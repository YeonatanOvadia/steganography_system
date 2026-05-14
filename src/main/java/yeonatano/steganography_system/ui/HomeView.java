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

// ייבוא המודל שלך
import yeonatano.steganography_system.datamodels.User;

@Route(value = "", layout = MainLayout.class)
@PageTitle("STEGO-CORE | Home")
public class HomeView extends VerticalLayout 
{

    public HomeView() 
    {
        // 1. הגדרות מסך ראשי ורקע סייברפאנק (הכל דרך Java Style API)
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);
        
        getStyle()
            .set("background-color", "#050505")
            .set("background-image", "linear-gradient(rgba(13, 227, 20, 0.04) 1px, transparent 1px), linear-gradient(90deg, rgba(13, 227, 20, 0.04) 1px, transparent 1px)")
            .set("background-size", "40px 40px")
            .set("color", "#e0e0e0")
            .set("padding-top", "4rem")
            .set("font-family", "'Urbanist', sans-serif"); 

        // 2. שליפת המשתמש
        User user = (User) VaadinSession.getCurrent().getAttribute("user");
        String currentUsername = (user != null) ? user.getUsername() : "אורח";

        // 3. כותרות
        H1 mainTitle = new H1("STEGO-CORE");
        mainTitle.getStyle()
            .set("font-family", "'Rajdhani', sans-serif")
            .set("color", "#0de314")
            .set("text-shadow", "0 0 20px rgba(13, 227, 20, 0.5)")
            .set("letter-spacing", "4px")
            .set("margin-bottom", "0")
            .set("font-size", "4rem");

        Span subTitle = new Span("האמנות שבהסתרת המידע // מערכת סטגנוגרפיה מתקדמת");
        subTitle.getStyle()
            .set("color", "#555")
            .set("text-transform", "uppercase")
            .set("letter-spacing", "2px")
            .set("font-size", "14px")
            .set("font-weight", "bold");

        VerticalLayout headerLayout = new VerticalLayout(mainTitle, subTitle);
        headerLayout.setAlignItems(Alignment.CENTER);
        headerLayout.setSpacing(false);
        headerLayout.setPadding(false);
        headerLayout.getStyle().set("margin-bottom", "2rem");
        
        H3 welcomeMessage = new H3("SYSTEM ACCESSED: " + currentUsername.toUpperCase());
        welcomeMessage.getStyle()
            .set("color", "#0de314")
            .set("margin", "0")
            .set("font-family", "'Rajdhani', sans-serif");
        
        HorizontalLayout greetingLayout = new HorizontalLayout(welcomeMessage);
        greetingLayout.setAlignItems(Alignment.CENTER);
        greetingLayout.getStyle().set("margin-bottom", "1rem");

        // 4. אזור ההסבר (בנוי מפסקאות Java בלבד)
        VerticalLayout descriptionLayout = new VerticalLayout();
        descriptionLayout.getStyle().set("text-align", "right").set("direction", "rtl");
        descriptionLayout.setPadding(false);
        descriptionLayout.setSpacing(false);

        Paragraph p1 = new Paragraph("המערכת שלנו מאפשרת לכם להחביא הודעות כתובות סודיות בתוך קבצי מדיה יומיומיים, כמו תמונות או שירים. בעזרת שימוש באלגוריתמי עומק (PVD, F5, DSSS), הפעולה שומרת על הקבצים כך שייראו ויישמעו תמימים לחלוטין למי שמסתכל מבחוץ.");
        p1.getStyle().set("font-size", "1.15em").set("line-height", "1.7").set("margin-top", "0");

        Paragraph p2 = new Paragraph("המשמעות היא שגם אם תיפלו קורבן לתקיפה, כמו האזנה להודעות שלכם, התוקף יראה רק קבצי מדיה רגילים ולא יחשוד כלל שמועבר מסר סמוי בתוך הקובץ.");
        p2.getStyle()
            .set("font-size", "1.2em")
            .set("line-height", "1.7")
            .set("font-weight", "bold")
            .set("color", "#0de314")
            .set("border-right", "3px solid #0de314")
            .set("padding-right", "15px")
            .set("margin-top", "15px")
            .set("margin-bottom", "15px");

        Paragraph p3 = new Paragraph("• אין צורך בהתקנה ציוד קצה.");
        p3.getStyle().set("font-size", "1.1em").set("line-height", "1.6").set("margin-bottom", "0").set("color", "#aaa");

        descriptionLayout.add(p1, p2, p3);

        // 5. חלונית סייברפאנק עוטפת
        VerticalLayout cardContainer = new VerticalLayout(greetingLayout, descriptionLayout);
        cardContainer.setMaxWidth("800px");
        cardContainer.getStyle()
            .set("background", "rgba(10, 10, 10, 0.85)")
            .set("border", "1px solid #222")
            .set("border-right", "4px solid #0de314")
            .set("border-left", "4px solid #0de314")
            .set("padding", "2.5rem")
            .set("border-radius", "2px")
            .set("box-shadow", "0 15px 35px rgba(0,0,0,0.9)")
            .set("backdrop-filter", "blur(5px)");

        // 6. כפתורי ניווט עם עיצוב מותאם
        Button btnStgno = new Button("הצפנה וחילוץ", VaadinIcon.SHIELD.create(), e -> UI.getCurrent().navigate(StgnoView.class));
        stylePrimaryButton(btnStgno);
        btnStgno.setHeight("50px");
        btnStgno.setWidth("200px");

        Button btnhistory = new Button("היסטוריית תקשורת", VaadinIcon.ARCHIVE.create(), e -> UI.getCurrent().navigate(HistoryView.class));
        styleSecondaryButton(btnhistory);
        btnhistory.setHeight("50px");
        btnhistory.setWidth("200px");

        Button btnUsers = new Button("ניהול זהויות", VaadinIcon.USERS.create(), e -> UI.getCurrent().navigate(UserView.class));
        styleSecondaryButton(btnUsers);

        Button btnRegister = new Button("הרשמת סוכן", VaadinIcon.USERS.create(), e -> UI.getCurrent().navigate(RegisterView.class));
        styleSecondaryButton(btnRegister);

        Button btnSentMessages = new Button("דואר יוצא", VaadinIcon.SHIELD.create(), e -> UI.getCurrent().navigate(StgnoView.class));
        stylePrimaryButton(btnSentMessages);
        btnStgno.setHeight("50px");
        btnStgno.setWidth("200px");

        Button btnInbox = new Button("דואר נכנס", VaadinIcon.SHIELD.create(), e -> UI.getCurrent().navigate(StgnoView.class));
        stylePrimaryButton(btnInbox);
        btnStgno.setHeight("50px");
        btnStgno.setWidth("200px");

        HorizontalLayout MailActionButtons = new HorizontalLayout(btnSentMessages, btnInbox);
        HorizontalLayout primaryActionButtons = new HorizontalLayout(btnStgno, btnhistory);
        HorizontalLayout secondaryActionButtons = new HorizontalLayout(btnUsers, btnRegister);
        
        VerticalLayout buttonsLayout = new VerticalLayout(primaryActionButtons, secondaryActionButtons, MailActionButtons);
        buttonsLayout.setAlignItems(Alignment.CENTER);
        buttonsLayout.getStyle().set("margin-top", "2rem");

        // 7. הוספת הכל למסך הראשי
        add(headerLayout, cardContainer, buttonsLayout);
    }

    // ==========================================
    // פונקציות עזר לעיצוב כפתורים (Java בלבד)
    // ==========================================

    private void stylePrimaryButton(Button btn) 
    {
        btn.getStyle()
            .set("background-color", "#0de314")
            .set("color", "#000")
            .set("font-weight", "bold")
            .set("border-radius", "0")
            .set("letter-spacing", "1px")
            .set("transition", "all 0.2s ease-in-out")
            .set("cursor", "pointer");

        // אפקט Hover דרך מאזיני אירועים של Java
        btn.getElement().addEventListener("mouseover", e -> 
        {
            btn.getStyle().set("box-shadow", "0 0 20px rgba(13, 227, 20, 0.7)");
            btn.getStyle().set("transform", "translateY(-2px) scale(1.02)");
        });
        btn.getElement().addEventListener("mouseout", e -> 
        {
            btn.getStyle().remove("box-shadow");
            btn.getStyle().remove("transform");
        });
    }

    private void styleSecondaryButton(Button btn) {
        btn.getStyle()
            .set("background-color", "transparent")
            .set("color", "#0de314")
            .set("border", "1px solid #0de314")
            .set("border-radius", "0")
            .set("transition", "all 0.2s ease-in-out")
            .set("cursor", "pointer");

        // אפקט Hover דרך מאזיני אירועים של Java
        btn.getElement().addEventListener("mouseover", e -> {
            btn.getStyle().set("background-color", "rgba(13, 227, 20, 0.1)");
            btn.getStyle().set("box-shadow", "0 0 15px rgba(13, 227, 20, 0.3)");
            btn.getStyle().set("transform", "translateY(-2px)");
        });
        btn.getElement().addEventListener("mouseout", e -> {
            btn.getStyle().set("background-color", "transparent");
            btn.getStyle().remove("box-shadow");
            btn.getStyle().remove("transform");
        });
    }
}