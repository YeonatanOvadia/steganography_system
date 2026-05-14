package yeonatano.steganography_system.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import yeonatano.steganography_system.datamodels.User;
import yeonatano.steganography_system.services.UserService;

/**
 * תצוגת ניהול משתמשים (User Management View).
 * מסלול (Route): "users" תחת הפריסה הראשית (MainLayout).
 * מחלקה זו יורשת מ-VerticalLayout עבור סידור הרכיבים אנכית,
 * ומממשת את BeforeEnterObserver כדי לבדוק הרשאות גישה לפני טעינת הדף.
 */
@Route(value = "users", layout = MainLayout.class)
public class UserView extends VerticalLayout implements BeforeEnterObserver {
    
    // שירות לניהול משתמשים במערכת
    private UserService userService;
    
    // שדות קלט עבור שם משתמש וסיסמה
    private TextField txfUN, txfPW;
    
    // טבלה (Grid) להצגת רשימת המשתמשים במערכת
    private Grid<User> usersGrid;

    /**
     * בנאי המחלקה - מאתחל את ממשק המשתמש של הדף.
     * 
     * @param userService השירות (Service) המוזרק על ידי Spring לצורך ביצוע פעולות על משתמשים
     */
    public UserView(UserService userService) {
        this.userService = userService;

        // אתחול שדות הטקסט לקליטת נתונים
        txfUN = new TextField("Username");
        txfPW = new TextField("Password");
        
        // אתחול טבלת המשתמשים תוך ציון המודל שעליו היא מבוססת (User)
        usersGrid = new Grid<>(User.class);

        // יצירת כפתור להוספת משתמש. בלחיצה עליו תופעל הפונקציה addUserToDB עם הערכים מהשדות.
        Button btnAddUser = new Button("+ Add User", e -> addUserToDB(txfUN.getValue(), txfPW.getValue()));

        // יצירת פאנל אופקי לארגון שדות הקלט והכפתור באותה שורה
        HorizontalLayout fieldsPanel = new HorizontalLayout();
        fieldsPanel.setWidthFull(); // הגדרת רוחב מלא לפאנל
        fieldsPanel.setDefaultVerticalComponentAlignment(Alignment.BASELINE); // יישור הרכיבים לנקודת בסיס אחידה
        fieldsPanel.add(txfUN, txfPW, btnAddUser); // הוספת הרכיבים לפאנל האופקי

        // טעינת כלל המשתמשים מהשירות והצבתם בטבלה
        usersGrid.setItems(userService.getAllUsers());
        usersGrid.getStyle().setBorder("1px solid gray"); // הוספת גבול עיצובי לטבלה

        // הוספת הפאנל האופקי והטבלה לתצוגה המרכזית (האנכית) של הדף
        add(fieldsPanel, usersGrid);
    }

    /**
     * פונקציית עזר להוספת משתמש חדש למסד הנתונים ועדכון הממשק בהתאם.
     *
     * @param un שם המשתמש שהוזן
     * @param pw הסיסמה שהוזנה
     */
    private void addUserToDB(String un, String pw) {
        // יצירת אובייקט משתמש חדש עם הנתונים שהתקבלו
        User userToAdd = new User(un, pw);
        
        // קריאה לפונקציית ההוספה ב-UserService וקבלת משוב (הצלחה/כישלון)
        boolean res = userService.addUserToDB(userToAdd);

        // אם המשתמש נוסף בהצלחה למסד הנתונים
        if(res) {
            txfUN.clear(); // איפוס שדה שם המשתמש
            txfPW.clear(); // איפוס שדה הסיסמה
            usersGrid.setItems(userService.getAllUsers()); // רענון הטבלה כדי להציג את המשתמש החדש
        }
    }

    /**
     * פונקציה מובנית של ממשק BeforeEnterObserver.
     * מופעלת אוטומטית בכל פעם שיש ניסיון לגשת לנתיב של הדף הזה ("users").
     * משמשת לאבטחת הדף ומניעת גישה ממשתמשים שאינם מחוברים.
     *
     * @param beforeEnterEvent אירוע הניווט, מאפשר לנו לנתב את המשתמש למקום אחר במידת הצורך
     */
    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        /*
         * שינוי לוגיקה: 
         * במקום לבדוק אם יש "error" ב-URL (שמגיע מ-Spring),
         * נבדוק אם המשתמש כבר מחובר. אם הוא מחובר, אין טעם שיראה את דף הלוגין.
         */
        
        // בדיקה האם לא קיים אובייקט "user" ב-Session הנוכחי של Vaadin
        if (VaadinSession.getCurrent().getAttribute("user") == null) {
            // המשתמש אינו מחובר - העברה אוטומטית לדף ההתחברות (LoginView)
            beforeEnterEvent.forwardTo(LoginView.class);
        }
    }
}