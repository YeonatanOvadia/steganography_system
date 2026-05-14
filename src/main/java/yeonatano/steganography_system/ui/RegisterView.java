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
 * תצוגת הרשמה למערכת.
 * נתיב (Route): "register".
 * מאפשרת למשתמשים חדשים ליצור חשבון במערכת.
 * כוללת בדיקת גישה (BeforeEnterObserver) כדי למנוע ממשתמשים מחוברים לגשת לדף זה.
 */
@Route("register") // כתובת העמוד
@PageTitle("הרשמה | Steganography System")
public class RegisterView extends VerticalLayout implements BeforeEnterObserver 
{

    // שירות לניהול משתמשים במערכת (הוספה למסד נתונים)
    private UserService userService;

    /**
     * בנאי המחלקה - מאתחל ומסדר את ממשק המשתמש של דף ההרשמה.
     *
     * @param userService השירות (Service) המוזרק על ידי Spring לצורך רישום המשתמש
     */
    public RegisterView(UserService userService) 
    {
        this.userService = userService;

        // הגדרות עיצוב כלליות למסך (פריסה על כל המסך ומרכוז אנכי ואופקי)
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        // כותרת העמוד
        H1 title = new H1("יצירת חשבון חדש");
        
        // הגדרת השדות לקליטת נתונים מהמשתמש
        TextField usernameField = new TextField("שם משתמש");
        usernameField.setRequired(true); // סימון שדה חובה (מוסיף כוכבית אדומה ויזואלית)
        usernameField.setWidth("300px");

        PasswordField passwordField = new PasswordField("סיסמה");
        passwordField.setRequired(true);
        passwordField.setWidth("300px");

        PasswordField confirmPasswordField = new PasswordField("אימות סיסמה");
        confirmPasswordField.setRequired(true);
        confirmPasswordField.setWidth("300px");

        // הודעת שגיאה ויזואלית (מוסתרת כברירת מחדל, תוצג רק במקרה של שגיאת ולידציה)
        Span errorMessage = new Span();
        errorMessage.getStyle().set("color", "var(--lumo-error-text-color)");
        errorMessage.setVisible(false);

        // יצירת כפתור ההרשמה והגדרת הפעולה שתתבצע בעת לחיצה עליו
        Button registerBtn = new Button("הרשם למערכת", e -> 
        {
            // קריאת הערכים מהשדות וניקוי רווחים משם המשתמש
            String username = usernameField.getValue().trim();
            String password = passwordField.getValue();
            String confirmPassword = confirmPasswordField.getValue();

            // 1. בדיקת תקינות בסיסית (שדות ריקים)
            if (username.isEmpty() || password.isEmpty()) 
            {
                showError(errorMessage, "נא למלא את כל השדות");
                return; // עצירת תהליך ההרשמה
            }

            // 2. בדיקה שהסיסמאות תואמות זו לזו
            if (!password.equals(confirmPassword)) 
            {
                showError(errorMessage, "הסיסמאות אינן תואמות");
                return;
            }

            // 3. ניסיון הוספה למסד הנתונים דרך הסרוויס
            User newUser = new User(username, password);
            boolean isAdded = userService.addUserToDB(newUser);

            if (isAdded) 
            {
                // הרשמה הצליחה! תצוגת הודעת הצלחה למשך 4 שניות
                Notification.show("החשבון נוצר בהצלחה! נא להתחבר.", 4000, Notification.Position.MIDDLE);
                // העברת המשתמש אוטומטית לדף ההתחברות (Login)
                UI.getCurrent().navigate(LoginView.class);
            } 
            
            else // ההרשמה נכשלה מכיוון ששם המשתמש תפוס
                showError(errorMessage, "שם המשתמש כבר קיים במערכת, אנא בחר שם אחר.");

        });
        
        // עיצוב כפתור ההרשמה ככפתור ראשי בולט (צבע מלא)
        registerBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registerBtn.setWidth("300px");

        // כפתור חזרה לדף ההתחברות (למי שהגיע בטעות וכבר יש לו חשבון)
        Button backToLoginBtn = new Button("כבר יש לך חשבון? התחבר כאן", e -> 
            UI.getCurrent().navigate(LoginView.class)
        );
        // עיצוב כפתור החזרה ככפתור משני (טקסט בלבד ללא רקע בולט)
        backToLoginBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        // הוספת כל הרכיבים שהגדרנו לתוך הפריסה האנכית של המסך כדי שיוצגו בפועל
        add(title, usernameField, passwordField, confirmPasswordField, errorMessage, registerBtn, backToLoginBtn);
    }

    /**
     * פונקציית עזר להצגת הודעות שגיאה בצורה ויזואלית על המסך.
     * 
     * @param errorSpan הרכיב הוויזואלי שיציג את השגיאה
     * @param message תוכן הודעת השגיאה להצגה
     */
    private void showError(Span errorSpan, String message) 
    {
        errorSpan.setText(message);
        errorSpan.setVisible(true); // הפיכת השגיאה לגלויה למשתמש
    }

    /**
     * "השומר ההפוך":
     * מופעל אוטומטית לפני טעינת הדף (ממשק BeforeEnterObserver).
     * אם משתמש כבר מחובר למערכת (קיים ב-Session), אין לו סיבה לראות את עמוד ההרשמה.
     * נזרוק אותו חזרה לעמוד הראשי (HomeView).
     *
     * @param event אירוע הניווט, מאפשר שליטה לאן לנתב את המשתמש
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) 
    {
        // בדיקה האם יש אובייקט 'user' ב-Session
        if (VaadinSession.getCurrent().getAttribute("user") != null) 
        {
            // המשתמש מחובר - העברה אוטומטית לדף הבית
            event.forwardTo(HomeView.class);
        }
    }
}