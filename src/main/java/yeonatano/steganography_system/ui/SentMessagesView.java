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
 */
@Route(value = "sent", layout = MainLayout.class)
public class SentMessagesView extends VerticalLayout implements BeforeEnterObserver 
{
    private MessageService msgService;
    private StegnoService stgnoService;
    private Grid<Message> grid = new Grid<>(Message.class, false);

    public SentMessagesView(MessageService msgService, StegnoService stgnoService) 
    {
        this.msgService = msgService;
        this.stgnoService = stgnoService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setSpacing(true);

        add(new H1("הודעות שנשלחו"));

        Button composeBtn = new Button("הודעה חדשה", e -> {
        NewMessageDialog dialog = new NewMessageDialog(msgService, stgnoService, () -> refreshGrid());
        dialog.open();
        });

        showRecipientColumn();
        showBodyColumn();
        showAttachmentColumn();
        showDeleteColumn();
        
        refreshGrid();
        grid.setSizeFull();
        
        add(composeBtn, grid);
    }

    private void showRecipientColumn() 
    {
        grid.addColumn(Message::getReceiver).setHeader("אל");
    }

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
    /**
     * שימוש ברכיב הגלובלי MediaPreviewButton המבצע Lazy Loading.
     * הקובץ הבינארי נשלף מה-DB רק כאשר המשתמש לוחץ על הכפתור.
     */
    private void showAttachmentColumn() 
    {
        grid.addComponentColumn(msg -> 
        {
            if (!msg.hasFile()) return new Span("-");
            
            return new MediaPreviewButton(
                "הצג מדיה", 
                () -> msgService.getFileById(msg.getFileId()), 
                stgnoService
            );
        }).setHeader("קובץ מצורף").setWidth("200px");
    }

    /**
     * עמודת הורדת הקבצים.
     * עובדת באמצעות StreamResource כדי להזרים את הקובץ ישירות לדפדפן (ללא העמסת זיכרון).
     */
    

    /**
     * עמודת מחיקה אסינכרונית עם תצוגה אופטימית (Optimistic UI) וחלון אישור.
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

    private void refreshGrid() 
    {
        User user = (User) VaadinSession.getCurrent().getAttribute("user");
        if (user != null) 
        {
            grid.setItems(msgService.getMySentMessages(user.getUsername())); 
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event)
    {
        if (VaadinSession.getCurrent().getAttribute("user") == null) 
            event.rerouteTo(RegisterView.class);    
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) 
    {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        ui.setPollInterval(15000); 
        ui.addPollListener(e -> refreshGrid());
    }
}