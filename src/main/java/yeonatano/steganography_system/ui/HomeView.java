package yeonatano.steganography_system.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Route("") // מחרוזת ריקה אומרת שזה העמוד הראשי (localhost:8080/)
@PageTitle("Home | Steganography System")
@AnonymousAllowed // כל משתמש שעבר את מסך ה-Login מורשה לראות את עמוד הבית
public class HomeView extends VerticalLayout {

    public HomeView() {
        // עיצוב המסך שיהיה ממורכז
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        // 1. קסם האבטחה: שולפים את שם המשתמש הנוכחי ישירות מהזיכרון
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();

        // 2. יצירת כותרות
        H1 title = new H1("מערכת סטגנוגרפיה");
        H2 welcomeMessage = new H2("ברוך הבא, " + currentUsername + "!");

        // 3. כפתורי ניווט לעמודים האחרים
        Button btnStgno = new Button("הצפנה וחילוץ מסרים (Stagno)", e -> 
            UI.getCurrent().navigate(StgnoView.class)
        );
        btnStgno.addThemeVariants(ButtonVariant.LUMO_PRIMARY); // צובע את הכפתור בכחול בולט

        Button btnUsers = new Button("ניהול משתמשים", e -> 
            UI.getCurrent().navigate(UserView.class)
        );

        HorizontalLayout buttonsLayout = new HorizontalLayout(btnStgno, btnUsers);
        buttonsLayout.setSpacing(true);

        // 4. הוספת הרכיבים לתצוגה
        add(title, welcomeMessage, buttonsLayout);
    }
}