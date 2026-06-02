package yeonatano.steganography_system.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import yeonatano.steganography_system.datamodels.Message;
import yeonatano.steganography_system.datamodels.User;
import yeonatano.steganography_system.services.MessageService;
import yeonatano.steganography_system.services.StegnoService;


/**
 * תצוגת "הודעות יוצאות" של המערכת.
 * מחלקה זו מציגה טבלה (Grid) דינמית של כל ההודעות שהמשתמש הנוכחי שלח.
 * משלבת מנגנוני אבטחה (חסימת גישה לאורחים), ריענון אוטומטי (Polling), 
 * ופעולות אסינכרוניות (כמו מחיקה ברקע) כדי לספק חווית משתמש חלקה ורציפה (SPA - Single Page Application).
 */
@Route(value = "sent", layout = MainLayout.class)
public class SentMessagesView extends VerticalLayout implements BeforeEnterObserver 
{
    private MessageService msgService;
    private StegnoService stgnoService;
    
    // יצירת טבלת הנתונים (Grid). ה-false מונע יצירה אוטומטית של עמודות, כדי שנוכל להגדיר אותן ידנית
    private Grid<Message> grid = new Grid<>(Message.class, false);

    /**
     * בנאי המחלקה. 
     * מאתחל את ממשק המשתמש ובונה את עמודות הטבלה.
     */
    public SentMessagesView(MessageService msgService, StegnoService stgnoService) 
    {
        this.msgService = msgService;
        this.stgnoService = stgnoService;

        // הגדרות עיצוב בסיסיות לפריסת העמוד
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setSpacing(true);

        add(new H1("הודעות שנשלחו"));

        // כפתור יצירת הודעה חדשה הפותח את חלון הדיאלוג שבנינו
        // מועבר אליו Callback () -> refreshGrid() כדי שהטבלה תתרענן מיד אחרי שליחה מוצלחת
        Button composeBtn = new Button("הודעה חדשה", e -> {
            NewMessageDialog dialog = new NewMessageDialog(msgService, stgnoService, () -> refreshGrid());
            dialog.open();
        });

        // הרכבת עמודות הטבלה באופן מודולרי
        showRecipientColumn();
        showBodyColumn();
        showAttachmentColumn();
        showDeleteColumn();
        
        // טעינה ראשונית של הנתונים מהמסד
        refreshGrid();
        grid.setSizeFull();
        
        add(composeBtn, grid);
    }

    /**
     * עמודת הנמען - מציגה למי נשלחה ההודעה.
     */
    private void showRecipientColumn() 
    {
        grid.addColumn(Message::getReceiver).setHeader("אל");
    }

    /**
     * עמודת התוכן הגלוי.
     * מיישמת תצוגה חכמה: חותכת טקסטים ארוכים כדי לא "לשבור" את עיצוב הטבלה,
     * והופכת אותם ללחיצים כדי שהמשתמש יוכל לקרוא את הכל בחלון מודאלי.
     */
    private void showBodyColumn() 
    {
        grid.addComponentColumn(msg -> 
        {
            String fullText = msg.getBody();
            
            // טיפול בקצה (Edge case): הודעה ללא תוכן טקסטואלי
            if (fullText == null || fullText.isEmpty()) {
                return new Span("-");
            }
            
            // חיתוך הטקסט אם הוא ארוך מדי (מעל 30 תווים) לטובת נראות נקייה (Clean UI)
            int maxLength = 30;
            String displayText = fullText.length() > maxLength ? 
                                 fullText.substring(0, maxLength) + "..." : 
                                 fullText;
            
            Span textSpan = new Span(displayText);
            
            // אם הטקסט נחתך, נספק חיווי ויזואלי (קו תחתון, סמן עכבר) שמדובר ברכיב לחיץ
            if (fullText.length() > maxLength) 
            {
                textSpan.getStyle().set("cursor", "pointer");
                textSpan.getStyle().set("color", "var(--lumo-primary-text-color)");
                textSpan.getStyle().set("text-decoration", "underline");
                
                // לחיצה פותחת את הדיאלוג עם הטקסט המלא
                textSpan.addClickListener(event -> showFullTextDialog(fullText));
            }
            
            return textSpan;
        }).setHeader("תוכן גלוי");
    }

    /**
     * פונקציית עזר המייצרת חלון מודאלי (Dialog) להצגת טקסטים ארוכים בצורה נוחה.
     * מופעלת בעת לחיצה על טקסט מקוצר בטבלה.
     */
    private void showFullTextDialog(String fullText) 
    {
        Dialog textDialog = new Dialog();
        textDialog.setHeaderTitle("תוכן ההודעה המלא");
        textDialog.setWidth("400px"); // רוחב קבוע למניעת שבירת שורות אגרסיבית
        
        // שימוש ב-TextArea לקריאה בלבד מאפשר גלילה (Scroll) נוחה במקרה של טקסטים ארוכים מאוד
        com.vaadin.flow.component.textfield.TextArea textArea = new com.vaadin.flow.component.textfield.TextArea();
        textArea.setValue(fullText);
        textArea.setReadOnly(true);
        textArea.setWidthFull();
        textArea.getStyle().set("max-height", "50vh"); // מונע מהחלון לחרוג מגובה המסך (Viewport)
        
        Button closeBtn = new Button("סגור", e -> textDialog.close());
        
        textDialog.add(textArea);
        textDialog.getFooter().add(closeBtn);
        
        textDialog.open();
    }

    /**
     * עמודת הצגת המדיה.
     * שימוש ברכיב הגלובלי MediaPreviewButton המבצע טעינה עצלה (Lazy Loading).
     * הקובץ הבינארי (BLOB) לא נשלף ממסד הנתונים בעת טעינת הטבלה, אלא *רק* * כאשר המשתמש לוחץ בפועל על הכפתור "הצג מדיה", מה שחוסך משאבי רשת וזיכרון.
     */
    private void showAttachmentColumn() 
    {
        grid.addComponentColumn(msg -> 
        {
            if (!msg.hasFile()) return new Span("-");
            
            return new MediaPreviewButton(
                "הצג מדיה", 
                () -> msgService.getFileById(msg.getFileId()), // פונקציית ספק (Supplier) לשליפה מאוחרת
                stgnoService
            );
        }).setHeader("קובץ מצורף").setWidth("200px");
    }

    /**
     * עמודת מחיקה.
     * מבצעת מחיקה אסינכרונית תוך שמירה על רספונסיביות הממשק.
     */
    private void showDeleteColumn() 
    {
        grid.addComponentColumn(msg -> 
        {
            Button deleteBtn = new Button("מחיקה");
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
            
            deleteBtn.addClickListener(event -> 
            {
                // 1. פידבק מיידי על המסך (UX) כדי שהמשתמש ידע שהפעולה נקלטה
                Notification.show("מוחק את ההודעה מהתיבה...", 2000, Notification.Position.MIDDLE);
                
                // שמירת ה-Context של ה-UI הנוכחי כדי שנוכל לחזור אליו מה-Thread
                UI currentUI = UI.getCurrent();
                
                // 2. מעבר מיידי לרקע - ה-UI Thread משתחרר מיד למניעת הקפאת המסך
                new Thread(() -> {
                    try {
                        // מחיקת עצם ה-Message ממסד הנתונים
                        msgService.deleteMessage(msg.getId());
                        
                        // 3. חזרה אסינכרונית בטוחה ל-UI לצורך רענון הטבלה והעלמת השורה שנמחקה
                        currentUI.access(() -> {
                            refreshGrid(); // השורה נעלמת בצורה חלקה מהמסך
                        });
                    } catch (Exception ex) {
                        // 4. במקרה של שגיאה במסד הנתונים, הלקוח יקבל התראת שגיאה ברורה
                        currentUI.access(() -> {
                            Notification.show("שגיאה: המחיקה נכשלה. " + ex.getMessage(), 5000, Position.MIDDLE);
                        });
                    }
                }).start();
            });
            
            return deleteBtn;
        }).setHeader("מחיקה");    
    }

    /**
     * מעדכנת את הנתונים המוצגים בטבלה.
     * שולפת את ההודעות העדכניות של המשתמש ממסד הנתונים.
     */
    private void refreshGrid() 
    {
        User user = (User) VaadinSession.getCurrent().getAttribute("user");
        if (user != null) 
        {
            grid.setItems(msgService.getMySentMessages(user.getUsername())); 
        }
    }

    /**
     * אירוע מחזור חיים (Lifecycle) של Vaadin המופעל לפני הכניסה לעמוד.
     * משמש כפילטר אבטחה (Route Guard) כדי לוודא שמשתמשים לא מחוברים (אורחים)
     * לא יוכלו לגשת לתיבת ההודעות היוצאות על ידי הקלדת ה-URL ישירות.
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event)
    {
        if (VaadinSession.getCurrent().getAttribute("user") == null) 
            event.rerouteTo(RegisterView.class);    
    }

    /**
     * אירוע מחזור חיים המופעל כאשר הרכיב מחובר ל-UI באופן מלא.
     * מפעיל מנגנון Polling (דגימה מחזורית) מול השרת.
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) 
    {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        
        // הגדרת Polling כל 15 שניות. 
        // מאפשר לטבלה להתרענן אוטומטית למקרה שהודעה נמחקה מחוץ לחלון הנוכחי,
        // או כדי לעדכן סטטוסים אם יש צורך בכך מבלי שהמשתמש ילחץ על רענון ידני.
        ui.setPollInterval(15000); 
        ui.addPollListener(e -> refreshGrid());
    }
}