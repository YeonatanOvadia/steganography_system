package yeonatano.steganography_system.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import yeonatano.steganography_system.datamodels.User;
import yeonatano.steganography_system.services.UserService;

/**
 * תצוגת ההתחברות (Authentication Gateway) של המערכת.
 * מחלקה זו משמשת כשער הכניסה הראשי, ומונעת גישה לא מורשית לדפי המערכת הפנימיים.
 * 
 * בניגוד למערכות סטנדרטיות המסתמכות לחלוטין על Spring Security עבור ניתוב טפסים, 
 * תצוגה זו מממשת אימות משתמשים (Authentication) וניהול סשנים (Session Management) 
 * באופן ידני מבוסס-אירועים (Event-Driven), המותאם לארכיטקטורת Single Page Application (SPA).
 */
@Route("login")
@PageTitle("Login | StegoSystem") // הגדרת כותרת לשונית הדפדפן (SEO ו-UX)
public class LoginView extends VerticalLayout 
{

    // שימוש ברכיב ה-LoginForm המובנה של Vaadin המספק תבנית מאובטחת, 
    // רספונסיבית, ומוכנה מראש לקליטת שם משתמש וסיסמה.
    private LoginForm login = new LoginForm();
    
    // הזרקת תלויות (Dependency Injection) לשכבת הלוגיקה העסקית של המשתמשים.
    private UserService userService;

    /**
     * בנאי המחלקה - בונה את ממשק המשתמש ומגדיר את לוגיקת האימות.
     *
     * @param userService שירות המשתמשים (Singleton) המוזרק על ידי קונטיינר ה-IoC של Spring.
     */
    public LoginView(UserService userService) {
        this.userService = userService;
        
        // הגדרות עיצוב למסך הראשי (Flexbox)
        // פריסה על כל המסך ומרכוז אנכי ואופקי של טופס ההתחברות במרכז הצג.
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        /*
         * 💡 החלטה ארכיטקטונית קריטית (Custom Authentication Flow):
         * כברירת מחדל, רכיב ה-LoginForm של Vaadin מקנפג את עצמו לשלוח בקשת HTTP POST 
         * מסורתית לכתובת (action="login"). 
         * מכיוון שאנו מנהלים את האפליקציה כ-SPA (Single Page Application), ביטלנו את 
         * השליחה הסטנדרטית הזו. במקום לרענן את העמוד, אנו לוכדים את אירוע הלחיצה ב-Java
         * ומבצעים אימות מול ה-Service מבלי לעזוב את ה-Context הנוכחי של הדפדפן.
         */

        // האזנה לאירוע שליחת הטופס (Login Event)
        login.addLoginListener(event -> 
        {
            
            // שלב 1: אימות (Authentication)
            // העברת אישורי הגישה (Credentials) לשכבת הסרוויס לבדיקה מול מסד הנתונים.
            // מצופה שהסרוויס יבצע את השוואת הסיסמאות (רצוי לאחר גיבוב - Hashing).
            User authenticatedUser = userService.authenticate(event.getUsername(), event.getPassword());

            if (authenticatedUser != null) 
            {
                // שלב 2: הרשאה (Authorization) וניהול Session
                // האימות הצליח. אנו שומרים את אובייקט המשתמש בתוך ה-Session של Vaadin.
                // פעולה זו משמשת כ"אסימון גישה" (Access Token) וירטואלי.
                // מחלקות אחרות (המשתמשות ב-BeforeEnterObserver) יבדקו את קיום האובייקט הזה 
                // כדי להחליט אם לאפשר גישה לעמודים מוגנים.
                VaadinSession.getCurrent().setAttribute("user", authenticatedUser);
                
                // שלב 3: ניווט (Navigation)
                // ניתוב שקט (ללא טעינת דף מלאה מחדש) לעמוד הבית של המערכת.
                UI.getCurrent().navigate("/");
            } 

            else 
            {
                // מסלול כישלון (Failure Path): אישורים שגויים או משתמש לא קיים.
                // הדלקת דגל השגיאה בטופס ה-UI, אשר מציג למשתמש הודעת שגיאה ויזואלית (אדומה)
                // מבלי לחשוף אם הבעיה היא בשם המשתמש או בסיסמה (Security Best Practice).
                login.setError(true);
            }
        });

        // הרכבת ה-DOM הסופי: הוספת הכותרת הראשית ורכיב הטופס אל תצוגת העמוד.
        add(new H1("Steganography System"), login);
    } 
}