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
 * פריסת הבסיס (Main Layout) של האפליקציה.
 * מחלקה זו יורשת מ-AppLayout של Vaadin ומשמשת כמעטפת (Shell) לכל דפי המערכת.
 * היא מכילה את התפריט העליון (Navbar) שמשתנה באופן דינאמי בהתאם למצב ההתחברות של המשתמש.
 */
public class MainLayout extends AppLayout 
{

    /**
     * בנאי המחלקה.
     * מופעל בעת יצירת הפריסה וקורא לפונקציה שבונה את תפריט הניווט העליון.
     */
    public MainLayout() 
    {
        createHeader();
    }

    /**
     * פונקציה ליצירת התפריט העליון (Header).
     * בודקת האם קיים משתמש מחובר (ב-Session) ומציגה את כפתורי הניווט בהתאם 
     * (תפריט מלא למחוברים, או כפתורי התחברות/הרשמה לאורחים).
     */
    private void createHeader() 
    {
        // עיצוב הלוגו שיוצג בצד שמאל של התפריט העליון
        H1 logo = new H1("StegoSystem");
        logo.getStyle().set("font-size", "var(--lumo-font-size-l)")
              .set("margin", "0").set("padding-left", "16px");

        // שליפת אובייקט המשתמש הנוכחי מתוך ה-Session של הדפדפן
        User user = (User) VaadinSession.getCurrent().getAttribute("user");

        // יצירת קונטיינר אופקי שיכיל את כל רכיבי ה-Header ויישר אותם לאמצע (אנכית)
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        
        // בדיקה: האם המשתמש מחובר למערכת?
        if (user != null) 
        {
            // --- שיטת הכפתורים (הדרך המוכרת והפשוטה) ---
            
            // יצירת כפתורי ניווט לכל אחד מדפי המערכת המרכזיים
            Button navHome = new Button("דף הבית", e -> UI.getCurrent().navigate(HomeView.class));
            Button navStgno = new Button("הצפנה וחילוץ", e -> UI.getCurrent().navigate(StgnoView.class));
            Button navHistory = new Button("היסטוריית קבצים", e -> UI.getCurrent().navigate(HistoryView.class));
            Button navUsers = new Button("ניהול משתמשים", e -> UI.getCurrent().navigate(UserView.class));
            Button navInbox = new Button("תיבת דואר", e -> UI.getCurrent().navigate(InboxView.class));
            Button navSent = new Button("דואר יוצא", e -> UI.getCurrent().navigate(SentMessagesView.class));


            // הפיכת הכפתורים ל"שקופים" (ללא רקע בולט) כדי שייראו כמו קישורים/תפריט מודרני
            navHome.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            navStgno.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            navHistory.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            navUsers.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            navInbox.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            navSent.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            // איגוד כל כפתורי הניווט יחד לפאנל אופקי אחד עם רווחים ביניהם
            HorizontalLayout navigationMenu = new HorizontalLayout(navHome, navStgno, navHistory, navUsers, navInbox, navSent);
            navigationMenu.setSpacing(true);

            // כפתור התנתקות
            Button logoutBtn = new Button("התנתק", e -> 
            {
                // מחיקת ה-Session הנוכחי (התנתקות בפועל מהמערכת)
                VaadinSession.getCurrent().getSession().invalidate();
                // רענון/העברה לנתיב הראשי של המערכת (יזרוק את המשתמש חזרה ללוגין)
                UI.getCurrent().getPage().setLocation("/");
            });

            // עיצוב כפתור ההתנתקות באדום כדי שיבלוט ויסמן פעולה קריטית
            logoutBtn.addThemeVariants(ButtonVariant.LUMO_ERROR); 
            logoutBtn.getStyle().set("margin-right", "16px");

            // מסדרים את התפריט באמצע ואת ההתנתקות בצד ימין
            HorizontalLayout menuLayout = new HorizontalLayout(navigationMenu, logoutBtn);
            menuLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END); // דחיפת הרכיבים ימינה
            menuLayout.setWidthFull();
            menuLayout.setAlignItems(FlexComponent.Alignment.CENTER);

            // הוספת הלוגו משמאל ותפריט הניווט (הכולל את כפתור ההתנתקות) מימין
            header.add(logo, menuLayout);
        } 
        else 
        {
            // --- תצוגה לאורח (משתמש שאינו מחובר) ---
            
            // כפתור מעבר לדף התחברות
            Button loginBtn = new Button("התחברות", e -> UI.getCurrent().navigate(LoginView.class));
            loginBtn.getStyle().set("margin-right", "16px");
            
            // פאנל אופקי להצמדת כפתור ההתחברות לצד ימין של המסך
            HorizontalLayout loginLayout = new HorizontalLayout(loginBtn);
            loginLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
            loginLayout.setWidthFull();

            // כפתור מעבר לדף יצירת חשבון חדש (הרשמה)
            Button btnRegister = new Button("יצירת חשבון חדש", e -> UI.getCurrent().navigate(RegisterView.class));
            
            // הוספת הלוגו משמאל, ופאנל כפתורי האורח מימין
            header.add(logo, loginLayout, btnRegister);
        }

        // פקודה של Vaadin AppLayout - הוספת ה-Header המוכן לרצועת הניווט העליונה של האפליקציה
        addToNavbar(header);
    }
}