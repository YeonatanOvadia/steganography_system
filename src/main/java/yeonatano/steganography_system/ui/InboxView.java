package yeonatano.steganography_system.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.UploadHandler;
import yeonatano.steganography_system.datamodels.Message;
import yeonatano.steganography_system.datamodels.User;
import yeonatano.steganography_system.services.MessageService;
import yeonatano.steganography_system.services.StgnoService;

import java.io.File;
import java.util.Base64;

/**
 * תצוגת "תיבת דואר נכנס" (Inbox).
 * מחלקה זו אחראית על הצגת ההודעות שהתקבלו למשתמש המחובר, וכן מספקת ממשק
 * מודאלי (Dialog) ליצירת ושליחת הודעות חדשות, כולל אופציה להטמעת מסר סטגנוגרפי בזמן אמת.
 */
@Route(value = "inbox", layout = MainLayout.class)
public class InboxView extends VerticalLayout implements BeforeEnterObserver 
{

    // הזרקת שירותים לגישה למסד הנתונים ולמנוע האלגוריתמיקה
    private MessageService msgService;
    private StgnoService stgnoService;
    
    // Grid להצגת אובייקטי Message ללא ייצור עמודות אוטומטי (שליטה מלאה בתצוגה)
    private Grid<Message> grid = new Grid<>(Message.class, false);

    /**
     * בנאי המחלקה. מרכיב את ממשק המשתמש הראשי.
     */
    public InboxView(MessageService msgService, StgnoService stgnoService) 
    {
        this.msgService = msgService;
        this.stgnoService = stgnoService;

        // הגדרת פריסת עמוד (Flexbox)
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setSpacing(true);

        add(new H1("תיבת דואר נכנס"));

        // כפתור Call To Action ראשי לפתיחת חלונית יצירת הודעה חדשה
        Button composeBtn = new Button("הודעה חדשה", e -> {
            NewMessageDialog dialog = new NewMessageDialog(msgService, stgnoService, () -> refreshGrid());
            dialog.open();
        });        
        // אתחול העמודות בצורה מודולרית
        showSenderColumn();
        showBodyColumn();
        showAttachmentColumn();
        showDeleteColumn();
        
        // טעינת הנתונים מהמסד אל הטבלה
        refreshGrid();
        grid.setSizeFull();
        
        add(composeBtn, grid);
    }

    // --- בניית עמודות הטבלה ---

    /**
     * עמודת השולח.
     */
    private void showSenderColumn() 
    {
        grid.addColumn(Message::getSender).setHeader("מאת");
    }

    /**
     * עמודת התוכן הגלוי.
     */
    private void showBodyColumn() 
    {
        grid.addComponentColumn(msg -> 
        {
            String fullText = msg.getBody();
            
            // טיפול במקרה של הודעה ריקה
            if (fullText == null || fullText.isEmpty()) {
                return new Span("-");
            }
            
            // חיתוך הטקסט אם הוא ארוך מדי (מעל 30 תווים)
            int maxLength = 30;
            String displayText = fullText.length() > maxLength ? 
                                 fullText.substring(0, maxLength) + "..." : 
                                 fullText;
            
            Span textSpan = new Span(displayText);
            
            // אם הטקסט נחתך, נהפוך אותו ללחיץ מבחינה עיצובית ונוסיף אירוע לחיצה
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
     * פונקציית עזר המייצרת חלון מודאלי להצגת טקסטים ארוכים בצורה נוחה
     */
    private void showFullTextDialog(String fullText) 
    {
        Dialog textDialog = new Dialog();
        textDialog.setHeaderTitle("תוכן ההודעה המלא");
        textDialog.setWidth("400px"); // רוחב שנוח לקריאה
        
        // שימוש ב-TextArea לקריאה בלבד מאפשר גלילה נוחה במקרה של מגילות טקסט
        com.vaadin.flow.component.textfield.TextArea textArea = new com.vaadin.flow.component.textfield.TextArea();
        textArea.setValue(fullText);
        textArea.setReadOnly(true);
        textArea.setWidthFull();
        textArea.getStyle().set("max-height", "50vh"); // מונע מהחלון לחרוג מגובה המסך
        
        Button closeBtn = new Button("סגור", e -> textDialog.close());
        
        textDialog.add(textArea);
        textDialog.getFooter().add(closeBtn);
        
        textDialog.open();
    }

    private void showAttachmentColumn() 
    {
        grid.addComponentColumn(msg -> {
            if (!msg.hasFile()) return new Span("-"); 

            return new MediaPreviewButton(
                "הצג מדיה", 
                () -> msgService.getFileById(msg.getFileId()), 
                stgnoService
            );
        }).setHeader("קובץ מצורף").setWidth("200px");
    }
    /**
     * עמודת מחיקת הודעה (קריאה ל-Service ורענון הממשק).
     */
    private void showDeleteColumn() 
    {
        grid.addComponentColumn(msg -> 
        {
            Button deleteBtn = new Button("מחיקה");
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
            
            deleteBtn.addClickListener(event -> 
            {
                // 1. פידבק מיידי על המסך ללא חלונית מודאלית חוסמת
                Notification.show("מוחק את ההודעה מהתיבה...", 2000, Notification.Position.BOTTOM_START);
                
                // שמירת ה-Context של ה-UI הנוכחי
                UI currentUI = UI.getCurrent();
                
                // 2. מעבר מיידי לרקע - ה-UI Thread משתחרר מיד ואפס תקיעות למסך
                new Thread(() -> {
                    try {
                        // מחיקת עצם ה-Message ממסד הנתונים (MongoDB)
                        msgService.deleteMessage(msg.getId());
                        
                        // 3. חזרה אסינכרונית בטוחה ל-UI לצורך העלמת השורה מהגריד
                        currentUI.access(() -> {
                            refreshGrid(); // השורה נעלמת בצורה חלקה מהמסך
                        });
                    } catch (Exception ex) {
                        // 4. במקרה של שגיאה במסד הנתונים, נקבל התראת שגיאה ברורה על המסך
                        currentUI.access(() -> {
                            Notification.show("שגיאה: המחיקה נכשלה. " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
                        });
                    }
                }).start();
            });
            
            return deleteBtn;
        }).setHeader("מחיקה");    
    }

    // --- לוגיקה עסקית ופונקציות עזר ---

    /**
     * רענון נתוני הטבלה תוך סינון לפי המשתמש המחובר כעת (אבטחת מידע ברמת האפליקציה).
     */
    private void refreshGrid() 
    {
        User user = (User) VaadinSession.getCurrent().getAttribute("user");
        if (user != null) 
            grid.setItems(msgService.getMyInbox(user.getUsername())); 
    }

    /**
     * בקרת הרשאות לפני טעינת המסך.
     * שים לב: כאן מוגדר ניתוב ל-RegisterView במקרה של משתמש לא מחובר.
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) 
    {
        if (VaadinSession.getCurrent().getAttribute("user") == null) 
            event.rerouteTo(RegisterView.class);    
    }
}