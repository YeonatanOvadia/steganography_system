package yeonatano.steganography_system.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.UploadHandler;
import yeonatano.steganography_system.datamodels.User;
import yeonatano.steganography_system.services.MessageService;
import yeonatano.steganography_system.services.StgnoService;

import java.io.File;

/**
 * חלון ליצירת ושליחת הודעות חדשות.
 * מממש שליחה אסינכרונית ברקע (Fire and Forget) כדי לא לתקוע את הממשק.
 */
public class NewMessageDialog extends Dialog 
{
    private final MessageService messageService;
    private final StgnoService stegoService;
    
    // הפעולה שתתבצע אחרי שההודעה נשלחה בהצלחה (למשל: רענון הטבלה)
    private final Runnable afterSendAction;

    public NewMessageDialog(MessageService messageService, StgnoService stegoService, Runnable afterSendAction) 
    {
        this.messageService = messageService;
        this.stegoService = stegoService;
        this.afterSendAction = afterSendAction;

        setHeaderTitle("כתיבת הודעה חדשה");
        buildWindow();
    }

    private void buildWindow() 
    {
        TextField toField = new TextField("אל (שם משתמש)");
        // 1. סימון ויזואלי למשתמש שזהו שדה חובה (מוסיף כוכבית)
        toField.setRequiredIndicatorVisible(true);
        
        TextField bodyField = new TextField("הודעה גלויה");
        
        Checkbox embedCheckbox = new Checkbox("האם להטמיע מסר סודי?");
        TextField secretField = new TextField("המסר הסודי");
        
        secretField.setVisible(false);
        embedCheckbox.addValueChangeListener(e -> secretField.setVisible(e.getValue()));

        File[] uploadedFile = new File[1];
        String[] fileType = new String[1];
        
        Upload uploadComponent = new Upload(UploadHandler.toTempFile((meta, file) -> 
        {
            uploadedFile[0] = file;
            fileType[0] = meta.contentType();
            
        }));

        uploadComponent.setMaxFiles(1);
        
        Button sendButton = new Button("שלח", e -> 
        {
            UI currentUI = UI.getCurrent(); 
            
            User currentUser = (User) VaadinSession.getCurrent().getAttribute("user");
            if (currentUser == null) 
            {
                Notification.show("שגיאה: משתמש לא מחובר", 4000, Position.MIDDLE);
                return;
            }
            String senderName = currentUser.getUsername();
            String toUser = toField.getValue();
            
            // בדיקת תקינות נמען
            if (toUser == null || toUser.trim().isEmpty()) 
            {
                toField.setInvalid(true); 
                toField.setErrorMessage("חובה להזין נמען");
                Notification.show("חובה להזין את שם המשתמש של הנמען", 3000, Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return; 
            }
            toField.setInvalid(false); 

            String bodyText = bodyField.getValue();
            boolean doEmbed = embedCheckbox.getValue();
            String secretText = secretField.getValue();

            // חיווי מהיר ראשון וסגירת החלון מיד
            Notification.show("ההודעה נשלחת ברקע...", 3000, Position.BOTTOM_START);
            this.close();

            // פתיחת תהליכון הרקע
            new Thread(() -> 
            {
                try 
                {
                    byte[] fileData = null;
                    if (uploadedFile[0] != null && uploadedFile[0].exists()) 
                    {
                        fileData = java.nio.file.Files.readAllBytes(uploadedFile[0].toPath());
                    }

                    // מסלול א': הפעלת הסטגנוגרפיה
                    if (doEmbed && fileData != null) 
                    {
                        stegoService.embedMsg(fileData, fileType[0], secretText, senderName, (success, resultData) -> 
                        {
                            messageService.sendMessage(senderName, toUser, bodyText, resultData, fileType[0], "Embed");
                            
                            // חזרה ל-UI וכפיית דחיפה (Push) של הודעת ההצלחה
                            currentUI.access(() -> { 
                                if (afterSendAction != null) afterSendAction.run(); 
                                Notification successNotif = 
                                Notification.show("ההודעה הסודית נשלחה בהצלחה!", 4000, Position.BOTTOM_END);
                                successNotif.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                                
                                currentUI.push(); // <-- שורת הקסם: מכריח את הדפדפן להציג את ההודעה מיד!
                            });
                        });
                    } 
                    // מסלול ב': שליחה רגילה
                    else 
                    {
                        messageService.sendMessage(senderName, toUser, bodyText, fileData, fileType[0], "Upload");
                        
                        currentUI.access(() -> {
                            if (afterSendAction != null) afterSendAction.run(); 
                            Notification successNotif = 
                            Notification.show("ההודעה נשלחה בהצלחה!", 4000, Position.BOTTOM_END);
                            successNotif.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                            
                            currentUI.push(); // <-- שורת הקסם: מכריח את הדפדפן להציג את ההודעה מיד!
                        });
                    }
                } 
                catch (Exception ex) 
                { 
                    // במקרה של שגיאה ברקע - דוחפים הודעת שגיאה אדומה
                    currentUI.access(() -> {
                        Notification errorNotif = 
                        Notification.show("שגיאה בשליחת ההודעה ברקע: " + ex.getMessage(), 6000, Position.MIDDLE);
                        errorNotif.addThemeVariants(NotificationVariant.LUMO_ERROR);
                        
                        currentUI.push(); // דחיפה מיידית של השגיאה למסך
                    });
                }
            }).start();
        });
        
        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout layout = new VerticalLayout(toField, bodyField, uploadComponent, embedCheckbox, secretField);
        add(layout);
        getFooter().add(sendButton);
    }
}