package yeonatano.steganography_system.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

// ייבוא המודל והסרוויס
import yeonatano.steganography_system.datamodels.User;
import yeonatano.steganography_system.services.UserService;

/**
 * תצוגת ההרשמה למערכת (Registration View).
 * שכבת התצוגה שאחראית על קליטת נתוני משתמש חדש, אימות ראשוני של הקלט (Client-side Validation),
 * והעברת הנתונים לשכבת הלוגיקה (Service) לשמירה במסד הנתונים.
 * 
 * מיישמת את BeforeEnterObserver כדי לשמש כ"שומר סף הפוך" (Reverse Auth Guard) - 
 * מניעת גישה לדף זה ממשתמשים שכבר מחוברים למערכת.
 */
@Route("register") 
@PageTitle("הרשמה | Steganography System") // הגדרת כותרת הטאב בדפדפן (SEO ו-UX)
public class RegisterView extends VerticalLayout implements BeforeEnterObserver 
{

    // הזרקת תלויות (Dependency Injection) של שירות המשתמשים. 
    // מאפשר ניתוק בין ממשק המשתמש לבין לוגיקת מסד הנתונים (DAO/Repository).
    private UserService userService;

    /**
     * בנאי המחלקה - בונה את עץ ה-DOM של ממשק ההרשמה.
     *
     * @param userService אובייקט השירות המוזרק אוטומטית על ידי הקונטיינר של Spring.
     */
    public RegisterView(UserService userService) 
    {
        this.userService = userService;

        // הגדרות עיצוב מבניות (Flexbox) - פריסת הטופס על פני כל המסך ומרכוז האלמנטים
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        H1 title = new H1("יצירת חשבון חדש");
        
        // --- 1. בניית רכיבי קלט (Input Fields) ---
        
        TextField usernameField = new TextField("שם משתמש");
        // הגדרת שדה חובה: Vaadin יוסיף אינדיקציה ויזואלית (כוכבית) ויתריע על שדה ריק ברמת ה-UI
        usernameField.setRequired(true); 
        usernameField.setWidth("300px");

        // שימוש ב-PasswordField מבטיח שההקלדה מוסתרת (***) ומונע חשיפת סיסמאות (Shoulder Surfing)
        PasswordField passwordField = new PasswordField("סיסמה");
        passwordField.setRequired(true);
        passwordField.setWidth("300px");

        PasswordField confirmPasswordField = new PasswordField("אימות סיסמה");
        confirmPasswordField.setRequired(true);
        confirmPasswordField.setWidth("300px");

        // --- 2. בניית רכיבי משוב (Feedback Components) ---
        
        // יצירת אלמנט Span להצגת שגיאות ולידציה בצורה דינמית מתחת לשדות, במקום להקפיץ חלוניות מציקות.
        Span errorMessage = new Span();
        errorMessage.getStyle().set("color", "var(--lumo-error-text-color)"); // שימוש במשתני העיצוב של Vaadin לאחידות
        errorMessage.setVisible(false); // מוסתר כברירת מחדל עד שתתגלה שגיאה

        // --- 3. לוגיקת ההרשמה (Event Handling) ---
        
        Button registerBtn = new Button("הרשם למערכת", e -> 
        {
            // Sanitization: שימוש ב-trim() כדי לנקות רווחים מיותרים בתחילת ובסוף שם המשתמש
            // מונע מצב של יצירת משתמש בשם "admin " (עם רווח) שיכול לגרום לבעיות התחברות
            String username = usernameField.getValue().trim();
            String password = passwordField.getValue();
            String confirmPassword = confirmPasswordField.getValue();

            // שלב א': Validation בסיסי (בדיקת קלט לפני פנייה לשרת/DB כדי לחסוך משאבים)
            if (username.isEmpty() || password.isEmpty()) 
            {
                showError(errorMessage, "נא למלא את כל השדות");
                return; // יציאה מוקדמת (Early Exit) למניעת המשך ביצוע
            }

            // שלב ב': אימות סיסמה נגד טעויות הקלדה
            if (!password.equals(confirmPassword)) 
            {
                showError(errorMessage, "הסיסמאות אינן תואמות");
                return;
            }

            // שלב ג': העברת הנתונים לשכבת ה-Service לניסיון יצירת רשומה במסד הנתונים
            User newUser = new User(username, password);
            boolean isAdded = userService.addUserToDB(newUser);

            if (isAdded) 
            {
                // מסלול הצלחה (Happy Path): הצגת חיווי והעברה לעמוד ההתחברות
                Notification.show("החשבון נוצר בהצלחה! נא להתחבר.", 4000, Notification.Position.MIDDLE);
                UI.getCurrent().navigate(LoginView.class);
            } 
            else 
            {
                // מסלול כישלון עסקי: ה-Service החזיר false, כלומר שם המשתמש כבר תפוס ב-DB
                showError(errorMessage, "שם המשתמש כבר קיים במערכת, אנא בחר שם אחר.");
            }
        });
        
        // הבלטת כפתור הפעולה הראשי (Call To Action)
        registerBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registerBtn.setWidth("300px");

        // --- 4. ניווט חלופי (Alternative Navigation) ---
        
        Button backToLoginBtn = new Button("כבר יש לך חשבון? התחבר כאן", e -> 
            UI.getCurrent().navigate(LoginView.class)
        );
        // עיצוב כפתור משני כטקסט בלבד (Tertiary) כדי לא להתחרות ויזואלית עם כפתור ההרשמה הראשי
        backToLoginBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        // הרכבת כל רכיבי ה-DOM לתוך הפריסה המרכזית
        add(title, usernameField, passwordField, confirmPasswordField, errorMessage, registerBtn, backToLoginBtn);
    }

    /**
     * פונקציית עזר (Helper Method) להצגת שגיאות ולידציה על המסך.
     * מרכזת את לוגיקת תצוגת השגיאות למקום אחד כדי למנוע כפילויות קוד (DRY - Don't Repeat Yourself).
     * 
     * @param errorSpan הרכיב הוויזואלי שיעודכן
     * @param message הודעת השגיאה הספציפית להצגה
     */
    private void showError(Span errorSpan, String message) 
    {
        errorSpan.setText(message);
        errorSpan.setVisible(true);
    }

    /**
     * בקרת גישה שמופעלת במחזור החיים (Lifecycle) של Vaadin לפני רנדור הדף.
     * במקרה זה מדובר ב"שומר סף הפוך": אנו מוודאים שמשתמש שכבר קיים לו Session פעיל 
     * לא יוכל לגשת לדף ההרשמה (לדוגמה על ידי הקלדת /register ב-URL).
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) 
    {
        // אם האובייקט 'user' קיים בסשן, המשתמש מחובר
        if (VaadinSession.getCurrent().getAttribute("user") != null) 
        {
            // הפניה שקטה (Forward) לעמוד הבית של המערכת
            event.forwardTo(HomeView.class);
        }
    }
}