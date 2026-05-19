package yeonatano.steganography_system.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.server.VaadinSession;
import yeonatano.steganography_system.datamodels.User;

/**
 * פריסת הבסיס (Main Layout / Shell Component) של המערכת.
 * מחלקה זו יורשת מ-AppLayout של Vaadin ומשמשת כמעטפת גלובלית לכל דפי המערכת.
 * כל מחלקה אחרת המשתמשת באנוטציה @Route(layout = MainLayout.class) תוצג בתוך מעטפת זו.
 * בדרך זו אנו מיישמים את עקרון DRY (Don't Repeat Yourself) ומונעים שכפול של קוד התפריט העליון בכל דף.
 */
public class MainLayout extends AppLayout 
{

    /**
     * בנאי המחלקה.
     * מופעל בעת בניית ה-DOM של המערכת. מאתחל את העיצוב הגלובלי וקורא לבניית הניווט.
     */
    public MainLayout() 
    {
        // הפעלת מצב לילה (Dark Mode) באופן גלובלי לאפליקציה על ידי הזרקת Theme
        UI.getCurrent().getElement().getThemeList().add("dark");
        
        createHeader();
        
        // הגדרת עיצוב רקע גלובלי למערכת (Inline CSS). 
        // יצירת תבנית רשת (Grid) המעניקה למערכת מראה טכנולוגי/סייבר, המתאים למערכת סטגנוגרפיה.
        getStyle()
        .set("background-color", "#050505")
        .set("background-image", "linear-gradient(rgba(13, 227, 20, 0.04) 1px, transparent 1px), linear-gradient(90deg, rgba(13, 227, 20, 0.04) 1px, transparent 1px)")
        .set("background-size", "40px 40px")
        .set("color", "#e0e0e0"); // צבע טקסט בהיר כברירת מחדל לניגודיות טובה על הרקע הכהה
    }

    /**
     * פונקציה ליצירת התפריט העליון (Header / Navbar).
     * מיישמת רנדור מותנה (Conditional Rendering): הממשק נבנה בזמן ריצה (Runtime)
     * בהתאם למצב הסשן (Session) של המשתמש הנוכחי (מחובר מול אורח).
     */
    private void createHeader() 
    {
        // עיצוב הלוגו שיוצג קבוע בצד שמאל של התפריט העליון
        H1 logo = new H1("StegoSystem");
        logo.getStyle().set("font-size", "var(--lumo-font-size-l)")
              .set("margin", "0").set("padding-left", "16px");

        // בדיקת בקרת גישה (Access Control): שליפת אובייקט המשתמש מתוך ה-Session המנוהל של השרת
        User user = (User) VaadinSession.getCurrent().getAttribute("user");

        // יצירת קונטיינר אופקי (Flexbox) שיכיל את כל רכיבי ה-Header ויישר אותם למרכז הציר האנכי
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        
        // לוגיקת פיצול תצוגה: מחובר לעומת אורח
        if (user != null) 
        {
            // --- תצוגה למשתמש מחובר (Authorized State) ---
            
            // יצירת כפתורי ניווט לכל אחד מדפי המערכת המרכזיים.
            // שימוש ב-UI.getCurrent().navigate מבצע ניווט בצד הלקוח (SPA - Single Page Application) 
            // ללא טעינה מחדש של כל עמוד ה-HTML, מה שמשפר משמעותית את ביצועי המערכת.
            Button navHome = new Button("דף הבית", e -> UI.getCurrent().navigate(HomeView.class));
            Button navStgno = new Button("הצפנה וחילוץ", e -> UI.getCurrent().navigate(StgnoView.class));
            Button navHistory = new Button("היסטוריית קבצים", e -> UI.getCurrent().navigate(HistoryView.class));
            Button navUsers = new Button("ניהול משתמשים", e -> UI.getCurrent().navigate(UserView.class));
            Button navInbox = new Button("דואר נכנס", e -> UI.getCurrent().navigate(InboxView.class));
            Button navSent = new Button("דואר יוצא", e -> UI.getCurrent().navigate(SentMessagesView.class));

            // שימוש בוריאנט LUMO_TERTIARY מסיר את המסגרת והרקע מהכפתור, 
            // כך שהוא מתנהג וולחץ כמו כפתור, אך נראה כמו לינק תפריט מודרני ונקי.
            navHome.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            navStgno.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            navHistory.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            navUsers.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            navInbox.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            navSent.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            // איגוד כל כפתורי הניווט יחד לפאנל אופקי אחד עם רווחים (Spacing) מובנים
            HorizontalLayout navigationMenu = new HorizontalLayout(navStgno, navHistory, navUsers, navInbox, navSent, navHome);
            navigationMenu.setSpacing(true);

            // כפתור התנתקות מנוהל (Managed Logout)

            Button logoutBtn = new Button("התנתק " + user.getUsername() , e -> 
            {
                // קריטי לאבטחה: השמדת הסשן בצד השרת (Session Invalidation)
                // פעולה זו מנקה את הזיכרון ומונעת מתקפות Session Hijacking
                VaadinSession.getCurrent().getSession().invalidate();
                
                // רענון מלא של הדף (Hard Refresh) וניתוב לרוט ("/") 
                // כדי לנקות את ה-DOM מצד הלקוח ולהכריח טעינה של מסך ההתחברות
                UI.getCurrent().getPage().setLocation("/");
            });

            // עיצוב כפתור ההתנתקות באדום (LUMO_ERROR) כדי לסמן פעולה דסטרקטיבית (הרסנית לסשן)
            logoutBtn.addThemeVariants(ButtonVariant.LUMO_ERROR); 
            logoutBtn.getStyle().set("margin-right", "16px");

            // אריזת התפריט וכפתור ההתנתקות יחד
            HorizontalLayout menuLayout = new HorizontalLayout(navigationMenu, logoutBtn);
            // דחיפת כל הרכיבים בתוך ה-Flex container לצד ימין (Justify-Content: End)
            menuLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END); 
            menuLayout.setWidthFull();
            menuLayout.setAlignItems(FlexComponent.Alignment.CENTER);

            header.add(logo, menuLayout);
        } 
        
        else 
        {
            // --- תצוגה לאורח (Unauthorized State) ---
            
            Button loginBtn = new Button("התחברות", e -> UI.getCurrent().navigate(LoginView.class));
            loginBtn.getStyle().set("margin-right", "16px");
            
            HorizontalLayout loginLayout = new HorizontalLayout(loginBtn);
            loginLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
            loginLayout.setWidthFull();

            Button btnRegister = new Button("יצירת חשבון חדש", e -> UI.getCurrent().navigate(RegisterView.class));
            
            header.add(logo, loginLayout, btnRegister);
        }

        // הזרקת ה-Header שבנינו לתוך רצועת הניווט (Navbar) המובנית של רכיב ה-AppLayout
        addToNavbar(header);
    }
}