package yeonatano.steganography_system.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import yeonatano.steganography_system.datamodels.Files;
import yeonatano.steganography_system.datamodels.User;
import yeonatano.steganography_system.services.HistoryService;
import yeonatano.steganography_system.services.StegnoService;


/**
 * תצוגת היסטוריית הקבצים של המשתמש.
 * מחלקה זו מציגה טבלה (Grid) של כל הקבצים (תמונות ושמע) שהמשתמש העלה או עיבד במערכת.
 * היא מאפשרת צפייה מקדימה במדיה, הורדה מחדש, חילוץ מסרים (דרך קליק ימני) ומחיקה לוגית.
 */
@Route(value = "history", layout = MainLayout.class)
public class HistoryView extends VerticalLayout implements BeforeEnterObserver 
{

    // הזרקת שירותים (Services) לגישה לנתוני היסטוריה ולשירות החילוץ האלגוריתמי
    private HistoryService historyService;
    private StegnoService stgnoService;
    
    // יצירת טבלת הנתונים. השימוש ב-false מונע יצירת עמודות אוטומטית לפי שדות המודל
    // ומאפשר לנו שליטה מוחלטת על אילו עמודות יוצגו ואיך (Custom Rendering).
    private Grid<Files> grid = new Grid<>(Files.class, false);

    /**
     * בנאי המחלקה. מאתחל את ממשק המשתמש ואת עמודות הטבלה.
     *
     * @param historyService שירות לניהול ושליפת היסטוריית הקבצים של המשתמש.
     * @param stgnoService שירות הסטגנוגרפיה המאפשר חילוץ מסרים מקבצים קיימים.
     */
    public HistoryView(HistoryService historyService, StegnoService stgnoService) 
    {
        this.historyService = historyService;
        this.stgnoService = stgnoService;
        
        // הגדרות עיצוב מבניות
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setSpacing(true);

        add(new H1("היסטוריית הקבצים שלי"));

        // קריאה מסודרת לפונקציות העמודות. מבנה זה שומר על קוד קריא, מודולרי וקל לתחזוקה (Clean Code).
        showTimestamp();
        showMediaType();
        showDbImage();
        showDeleteColumn();

        // מילוי הטבלה בנתונים ראשוניים לאחר בניית העמודות
        refreshGrid();

        grid.setSizeFull();
        add(grid);
    }

    // --- הגדרות עמודות הטבלה ---

    /**
     * עמודת חותמת זמן.
     * מציגה את התאריך והשעה שבהם הקובץ נוצר/הועלה. העמודה הוגדרה כבר-מיון (Sortable).
     */
    private void showTimestamp() 
    {
        grid.addColumn(file -> file.getTimestamp()).setHeader("תאריך ושעה").setSortable(true);
    }

    /**
     * עמודת סוג המדיה.
     * מציגה את ה-MIME Type של הקובץ (למשל image/png או audio/wav).
     */
    private void showMediaType() 
    {
        grid.addColumn(file -> file.getMediaType()).setHeader("סוג קובץ");
    }

    /**
     * עמודת תצוגה מקדימה של הקובץ (Media Renderer).
     * עמודה מורכבת זו מייצרת רכיב UI חי (תמונה או נגן שמע) ישירות מהמידע הבינארי השמור במסד הנתונים.
     */
    private void showDbImage() 
    {
        grid.addComponentColumn(dbFile -> {
            return new MediaPreviewButton(
                "הצג מדיה", 
                () -> {
                    // מושכים את הבינאריות רק כשהמשתמש באמת לוחץ
                    byte[] fullData = historyService.getFileData(dbFile.getId());
                    dbFile.setImageData(fullData);
                    return dbFile;
                }, 
                stgnoService
            );
        }).setHeader("תצוגה").setWidth("200px");
    }


    /**
     * עמודת מחיקה.
     * מבצעת מחיקה של הרשומה. מומלץ להסביר כי מדובר במחיקה לוגית.
     */
    private void showDeleteColumn() 
    {
        grid.addComponentColumn(file -> 
        {
            Button deleteBtn = new Button("מחיקה");
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
            
            deleteBtn.addClickListener(event -> 
            {
                // 1. יצירת חלון אישור מחיקה
                Dialog confirmDialog = new Dialog();
                confirmDialog.setHeaderTitle("אישור מחיקה");
                Span message = new Span("האם אתה בטוח שברצונך למחוק קובץ זה מההיסטוריה? (פעולה זו תסתיר את הקובץ)");
                
                Button cancelBtn = new Button("ביטול", e -> confirmDialog.close());
                
                Button confirmBtn = new Button("מחק", e -> {
                    // סגירת חלון האישור
                    confirmDialog.close();
                    
                    // השהיית הכפתור כדי למנוע לחיצות כפולות בזמן ההמתנה לשרת
                    deleteBtn.setEnabled(false);
                    Notification.show("ממתין לאישור מחיקה ממסד הנתונים...", 2000, com.vaadin.flow.component.notification.Notification.Position.BOTTOM_START);
                    
                    UI currentUI = UI.getCurrent();
                    
                    // 2. תהליכון רקע: קודם מוחקים ב-DB, ורק אז מעדכנים את המסך
                    new Thread(() -> {
                        try {
                            System.out.println("Attempting to logically delete file ID: " + file.getId());
                            
                            // קריאה לשירות לביצוע Soft Delete ב-MongoDB
                            historyService.softDeleteFile(file.getId());
                            
                            System.out.println("Successfully updated isDeleted=true for file ID: " + file.getId());
                            
                            // 3. הצלחה! חזרה ל-UI Thread כדי להעלים את השורה ולהציג הודעה
                            currentUI.access(() -> {
                                // רק עכשיו השורה באמת נעלמת מהעין
                                grid.getListDataView().removeItem(file); 
                                
                                Notification successNotif = 
                                    Notification.show(
                                        "הקובץ נמחק בהצלחה מהמערכת!", 
                                        4000, 
                                        Notification.Position.BOTTOM_END
                                    );
                                successNotif.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                            });
                            
                        } catch (Exception ex) {
                            System.err.println("Error during logical deletion: " + ex.getMessage());
                            ex.printStackTrace();
                            
                            // 4. שגיאה: המחיקה נכשלה. משחררים את הכפתור ומציגים הודעה אדומה
                            currentUI.access(() -> {
                                deleteBtn.setEnabled(true); // מאפשרים למשתמש לנסות שוב
                                Notification errorNotif = Notification.show(
                                        "שגיאה: המחיקה במסד הנתונים נכשלה ולא בוצעה.", 
                                        5000, 
                                        Notification.Position.MIDDLE
                                    );
                                errorNotif.addThemeVariants(NotificationVariant.LUMO_ERROR);
                            });
                        }
                    }).start();
                });
                
                confirmBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
                
                confirmDialog.add(new VerticalLayout(message));
                confirmDialog.getFooter().add(cancelBtn, confirmBtn);
                confirmDialog.open();
            });
            
            return deleteBtn;
        }).setHeader("מחיקה");    
    }
    // --- פונקציות עזר ולוגיקה ---

    /**
     * מרעננת את הנתונים בטבלה.
     * מבצעת שאילתה ל-DB כדי למשוך אך ורק את הקבצים הפעילים (שלא עברו Soft Delete) השייכים למשתמש הספציפי.
     */
    private void refreshGrid() 
    {
        User user = (User) VaadinSession.getCurrent().getAttribute("user");
        if (user != null) 
            // קריאה מתאימה לשירות המביאה היסטוריה פעילה בלבד
            grid.setItems(historyService.getActiveUserHistory(user.getUsername()));
    }

    /**
     * מנגנון אבטחה בסיסי: חוסם גישה ממשתמשים שאינם מחוברים.
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) 
    {
        if (VaadinSession.getCurrent().getAttribute("user") == null)
            event.rerouteTo(LoginView.class);
    }
}