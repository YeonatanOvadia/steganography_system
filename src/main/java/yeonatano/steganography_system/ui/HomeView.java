package yeonatano.steganography_system.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

// ייבוא המודל של המשתמש
import yeonatano.steganography_system.datamodels.User;

/**
 * תצוגת דף הבית (Landing Page / Dashboard) של מערכת STEGO-CORE.
 * נתיב (Route): "" (כתובת הבסיס של המערכת).
 * מחלקה זו מרכזת את חווית המשתמש הראשונית (UI/UX) ומשמשת כצומת ניווט מרכזי (Hub)
 * לכל שאר חלקי המערכת (תיבת דואר, הצפנה, ניהול משתמשים).
 * 
 * הדף מתאפיין בעיצוב "סייברפאנק" (Cyberpunk) הממומש כולו דרך Java Style API, 
 * ללא צורך בגיליונות CSS חיצוניים.
 */
@Route(value = "", layout = MainLayout.class)
@PageTitle("STEGO-CORE | Home")
public class HomeView extends VerticalLayout 
{

    public HomeView() 
    {
        // --- 1. הגדרות מעטפת ויזואלית (Layout Configuration) ---
        // הגדרת פריסת המסך (Flexbox): תופס את כל הגובה, ממרכז את התוכן אופקית, ומצמיד למעלה אנכית.
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);
        
        // --- 2. בקרת גישה ופרסונליזציה (Session Management) ---
        // שליפת המשתמש המחובר כעת ב-Session. אם לא קיים, המערכת תזהה אותו כ"אורח".
        User user = (User) VaadinSession.getCurrent().getAttribute("user");
        String currentUsername = (user != null) ? user.getUsername() : "אורח";

        // --- 3. בניית הטיפוגרפיה (Typography & Headers) ---
        
        H1 mainTitle = new H1("STEGNO-MAIL");
        // שימוש ב-Inline CSS דרך Java כדי לייצר אפקטים מתקדמים (כמו Text Shadow זוהר)
        mainTitle.getStyle()
            .set("font-family", "'Rajdhani', sans-serif")
            .set("color", "#0de314")
            .set("text-shadow", "1px 1px 2px rgba(0, 0, 0, 0.15)")
            .set("letter-spacing", "4px")
            .set("margin-bottom", "0")
            .set("font-size", "4rem");

        Span subTitle = new Span("האמנות שבהסתרת המידע // מערכת מייל סטגנוגרפית מתקדמת");
        subTitle.getStyle()
            .set("color", "#555")
            .set("text-transform", "uppercase")
            .set("letter-spacing", "2px")
            .set("font-size", "14px")
            .set("font-weight", "bold");

        // אריזת הכותרות בקונטיינר אנכי צמוד (ללא מרווחים פנימיים)
        VerticalLayout headerLayout = new VerticalLayout(mainTitle, subTitle);
        headerLayout.setAlignItems(Alignment.CENTER);
        headerLayout.setSpacing(false);
        headerLayout.setPadding(false);
        headerLayout.getStyle().set("margin-bottom", "2rem");
        
        // הצגת הודעת ברכה דינמית מבוססת זהות משתמש
        H3 welcomeMessage = new H3("SYSTEM ACCESSED: " + currentUsername.toUpperCase());
        welcomeMessage.getStyle()
        .set("color", "#059609")
            .set("margin", "0")
            .set("font-family", "'Rajdhani', sans-serif");
        
        HorizontalLayout greetingLayout = new HorizontalLayout(welcomeMessage);
        greetingLayout.setAlignItems(Alignment.CENTER);
        greetingLayout.getStyle().set("margin-bottom", "1rem");

        // --- 4. אזור התוכן וההסבר (Content Area) ---
        // בניית פסקאות טקסט (Paragraph) שמסבירות על האלגוריתמים שבבסיס המערכת
        
        VerticalLayout descriptionLayout = new VerticalLayout();
        descriptionLayout.getStyle().set("text-align", "right").set("direction", "rtl");
        descriptionLayout.setPadding(false);
        descriptionLayout.setSpacing(false);

        Paragraph p1 = new Paragraph("המערכת שלנו מאפשרת לכם להחביא הודעות כתובות סודיות בתוך קבצי מדיה יומיומיים, כמו תמונות או שירים. בעזרת שימוש באלגוריתמי עומק (PVD, F5, DSSS), הפעולה שומרת על הקבצים כך שייראו ויישמעו תמימים לחלוטין למי שמסתכל מבחוץ.");
        p1.getStyle().set("font-size", "1.15em").set("line-height", "1.7").set("margin-top", "0");

        Paragraph p2 = new Paragraph("המשמעות היא שגם אם תיפלו קורבן לתקיפה, כמו האזנה להודעות שלכם, התוקף יראה רק קבצי מדיה רגילים ולא יחשוד כלל שמועבר מסר סמוי בתוך הקובץ.");
        // הוספת מסגרת צדדית להדגשת הפיסקה (Blockquote Style)
        p2.getStyle()
            .set("font-size", "1.2em")
            .set("line-height", "1.7")
            .set("font-weight", "bold")
            .set("color", "#059609") // ירוק כהה קריא
            .set("border-right", "3px solid #059609")
            .set("padding-right", "15px")
            .set("margin-top", "15px")
            .set("margin-bottom", "15px");

        Paragraph p3 = new Paragraph("מגיש: יהונתן אליעזר עובדה שנת 2026");
        p3.getStyle().set("font-size", "1.1em").set("line-height", "1.6").set("margin-bottom", "0").set("color", "#555555"); // אפור כהה וברור
        descriptionLayout.add(p1, p2, p3);

        // --- 5. עיצוב המעטפת המרכזית (Glassmorphism & Background) ---
        // עטיפת הברכה וההסבר בתוך כרטיסייה (Card) עם רקע תמונה משולב בגרדיאנט להחשכה
        
        VerticalLayout cardContainer = new VerticalLayout(greetingLayout, descriptionLayout);
        cardContainer.setMaxWidth("800px");
        cardContainer.getStyle()
            // שימוש בנתיב יחסי לתמונת הרקע (התמונה צריכה לשבת בתיקיית src/main/resources/META-INF/resources/)
            .set("background", "linear-gradient(rgba(255, 255, 255, 0.9), rgba(255, 255, 255, 0.9)), url('Images/background.jpg')")
            .set("background-size", "cover") 
            .set("background-position", "center") 
            .set("border", "1px solid #e0e0e0") // מסגרת בהירה ועדינה
            .set("border-right", "4px solid #0de314")
            .set("border-left", "4px solid #0de314")
            .set("padding", "2.5rem")
            .set("border-radius", "2px")
            .set("box-shadow", "0 15px 35px rgba(0,0,0,0.9)")
            // שימוש בפילטר Backdrop ליצירת טשטוש (Blur) מודרני מסביב לאזור הטקסט
            .set("backdrop-filter", "blur(5px)");
            

        // --- 6. ניתוב וכפתורי פעולה (Navigation & Actions) ---
        // יצירת כפתורים באמצעות וקטורים (Vaadin Icons) ואירועי לחיצה (Click Listeners) המבצעים ניווט SPA
        
        Button btnStgno = new Button("הצפנה וחילוץ", VaadinIcon.SHIELD.create(), e -> UI.getCurrent().navigate(StegnoView.class));
        stylePrimaryButton(btnStgno);
        btnStgno.setHeight("50px");
        btnStgno.setWidth("200px");

        Button btnhistory = new Button("היסטוריה", VaadinIcon.ARCHIVE.create(), e -> UI.getCurrent().navigate(HistoryView.class));
        stylePrimaryButton(btnhistory);
        btnhistory.setHeight("50px");
        btnhistory.setWidth("200px");


        Button btnRegister = new Button("הרשמת סוכן", VaadinIcon.USERS.create(), e -> UI.getCurrent().navigate(RegisterView.class));
        styleSecondaryButton(btnRegister);
        btnRegister.setHeight("50px");
        btnRegister.setWidth("200px");


        // תוקן: ניווט לדף SentMessagesView וקביעת גודל תקינה לכפתור
        Button btnSentMessages = new Button("דואר יוצא", VaadinIcon.PAPERPLANE.create(), e -> UI.getCurrent().navigate(SentMessagesView.class));
        stylePrimaryButton(btnSentMessages);
        btnSentMessages.setHeight("50px");
        btnSentMessages.setWidth("200px");

        // תוקן: ניווט לדף InboxView וקביעת גודל תקינה לכפתור
        Button btnInbox = new Button("דואר נכנס", VaadinIcon.ENVELOPE.create(), e -> UI.getCurrent().navigate(InboxView.class));
        stylePrimaryButton(btnInbox);
        btnInbox.setHeight("50px");
        btnInbox.setWidth("200px");

        // סידור הכפתורים בקבוצות אופקיות (שורות)
        HorizontalLayout MailActionButtons = new HorizontalLayout(btnSentMessages, btnInbox);
        HorizontalLayout primaryActionButtons = new HorizontalLayout(btnStgno, btnhistory);
        HorizontalLayout secondaryActionButtons = new HorizontalLayout(btnRegister);
        
        // איגוד כל השורות לפאנל אנכי אחד
        VerticalLayout buttonsLayout = new VerticalLayout(secondaryActionButtons, primaryActionButtons, MailActionButtons);
        buttonsLayout.setAlignItems(Alignment.CENTER);
        buttonsLayout.getStyle().set("margin-top", "2rem");

        // --- 7. הרכבת ה-DOM הסופי ---
        add(headerLayout, cardContainer, buttonsLayout);
    }

    // =========================================================================
    // פונקציות עזר: ניהול עיצוב ואירועים (CSS Injection & Event Listeners)
    // =========================================================================

    /**
     * מחילה עיצוב של "כפתור פעולה ראשי" (צבע מלא, בולט).
     * @param btn הכפתור לעיצוב
     */
    private void stylePrimaryButton(Button btn) 
    {
        btn.getStyle()
            .set("background-color", "transparent")
            .set("color", "#059609") // החלפה לירוק בעל ניגודיות טובה על לבן
            .set("border", "1px solid #059609")
            .set("font-weight", "bold")
            .set("border-radius", "0")
            .set("letter-spacing", "1px")
            .set("transition", "all 0.2s ease-in-out") // אנימציה חלקה בעת שינוי מצב
            .set("cursor", "pointer");

        /* 
         * הוספת אינטראקטיביות (Hover Effects) ברמת ה-DOM דרך קוד Java.
         * במקום לכתוב קובץ CSS עם פקודת :hover, אנחנו תופסים את אירועי העכבר 
         * של הדפדפן (mouseover, mouseout) ומשנים את הסטייל בזמן אמת.
         */
        btn.getElement().addEventListener("mouseover", e -> 
        {
            btn.getStyle().set("box-shadow", "0 0 20px rgba(13, 227, 20, 0.7)");
            btn.getStyle().set("transform", "translateY(-2px) scale(1.02)"); // הגדלה עדינה והרמה
        });
        btn.getElement().addEventListener("mouseout", e -> 
        {
            btn.getStyle().remove("box-shadow");
            btn.getStyle().remove("transform"); // החזרת הכפתור למצבו הטבעי
        });
    }

    /**
     * מחילה עיצוב של "כפתור פעולה משני" (שקוף עם מסגרת, Outline).
     * @param btn הכפתור לעיצוב
     */
    private void styleSecondaryButton(Button btn) 
    {
        btn.getStyle()
            .set("background-color", "transparent")
            .set("color", "#0de314")
            .set("border", "1px solid #0de314")
            .set("border-radius", "0")
            .set("transition", "all 0.2s ease-in-out")
            .set("cursor", "pointer");

        // אפקט Hover - צביעת הרקע בשקיפות כשהעכבר מעל הכפתור
        btn.getElement().addEventListener("mouseover", e -> 
        {
            btn.getStyle().set("background-color", "rgba(13, 227, 20, 0.1)");
            btn.getStyle().set("box-shadow", "0 0 15px rgba(13, 227, 20, 0.3)");
            btn.getStyle().set("transform", "translateY(-2px)");
        });
        
        btn.getElement().addEventListener("mouseout", e -> 
        {
            btn.getStyle().set("background-color", "transparent");
            btn.getStyle().remove("box-shadow");
            btn.getStyle().remove("transform");
        });
    }
}