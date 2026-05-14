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
 * תצוגת ההתחברות (Login) של המערכת.
 * נתיב (Route): "login".
 * משמשת כשער הכניסה למערכת ומבצעת אימות משתמשים בצורה ידנית אל מול מסד הנתונים.
 */
@Route("login") 
@PageTitle("Login")
public class LoginView extends VerticalLayout {

    // רכיב טופס התחברות מוכן של Vaadin (כולל שדות שם משתמש, סיסמה וכפתור התחברות מובנים)
    private LoginForm login = new LoginForm();
    
    // שירות משתמשים המשמש לאימות פרטי ההתחברות
    private UserService userService;

    /**
     * בנאי המחלקה - מאתחל את תצוגת ההתחברות ומגדיר את פעולת כפתור הלוגין.
     * הזרקת ה-UserService מתבצעת על ידי Spring כדי שנוכל לבצע אימות ידני למול מסד הנתונים.
     *
     * @param userService השירות המטפל בלוגיקת המשתמשים
     */
    public LoginView(UserService userService) {
        this.userService = userService;
        
        // הגדרות עיצוב למסך הראשי (פריסה על כל המסך ומרכוז הטופס באמצע האתר)
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        /*
         * שינוי חשוב מהתנהגות ברירת המחדל של Vaadin:
         * במקור היה login.setAction("login"), שאומר לטופס להישלח החוצה כבקשת POST לשרת.
         * אנחנו הסרנו את זה כי אנחנו לא רוצים שהטופס יישלח כ-POST ל-URL של Spring Security.
         * במקום זאת, אנחנו מטפלים בלחיצה בעצמנו (בתוך קוד ה-Java) בגישת Single Page Application.
         */

        // הוספת מאזין (Listener) לאירוע הלחיצה על כפתור ה-Login בטופס
        login.addLoginListener(event -> {
            // קריאה לפונקציית האימות שבנית בסרוויס (השוואת שם משתמש וסיסמה מוצפנת)
            User authenticatedUser = userService.authenticate(event.getUsername(), event.getPassword());

            // אם האימות הצליח (חזר אובייקט משתמש קיים ותקין)
            if (authenticatedUser != null) {
                // הצלחה: שומרים את אובייקט המשתמש בסשן הידני של Vaadin.
                // שמירה זו תשמש אותנו כ-"מפתח" שיאפשר כניסה (Authorization) לדפים המאובטחים במערכת.
                VaadinSession.getCurrent().setAttribute("user", authenticatedUser);
                
                // העברה אוטומטית לעמוד הראשי של המערכת
                UI.getCurrent().navigate("/");
            } 
            else {
                // כישלון באימות (משתמש לא קיים או סיסמה שגויה): 
                // מפעילים את מצב השגיאה בטופס (מדליק הודעת שגיאה באדום מעל השדות)
                login.setError(true);
            }
        });

        // הוספת הכותרת הראשית של המערכת וטופס ההתחברות לתצוגה כדי שיופיעו על המסך
        add(new H1("Steganography System"), login);
    } 
}