package yeonatano.steganography_system.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
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
import yeonatano.steganography_system.services.StegnoService;

import java.io.File;

/**
 * דיאלוג (חלון קופץ) המאפשר יצירה ושליחה של הודעות רגילות או סטגנוגרפיות.
 * המחלקה משתמשת בתהליכוני רקע (Background Threads) לצורך ביצוע משימות "כבדות" (כגון הטמעת המידע)
 * מבלי לתקוע או להקפיא את ממשק המשתמש (UI). 
 * בנוסף, היא מטפלת בהעלאת קבצים זמניים ובולידציה של קלט המשתמש.
 */
public class NewMessageDialog extends Dialog 
{
    // שירות לניהול ותיעוד ההודעות במסד הנתונים
    private MessageService messageService;
    
    // שירות המפעיל את אלגוריתם הסטגנוגרפיה להטמעת וחילוץ מסרים
    private StegnoService stegoService;
    
    // פעולת תגובה המופעלת בסיום השליחה (למשל כדי לרענן את תיבת הדואר הנכנס של המשתמש)
    // שימוש ב-Runnable מאפשר העברת פונקציה כפרמטר בצורה פשוטה
    private Runnable afterSendAction;

    private File uploadedFile;

    private String uploadedMimeType;


    /**
     * בנאי המחלקה. מקבל את שירותי המערכת (Services) הדרושים ואת פעולת התגובה, ומאתחל את הדיאלוג.
     * * @param messageService שירות לניהול ההודעות במסד הנתונים
     * @param stegoService שירות לביצוע הטמעת נתונים בקבצי מדיה
     * @param afterSendAction פעולה (Callback) לביצוע לאחר שההודעה נשלחה בהצלחה
     */
    public NewMessageDialog(MessageService messageService, StegnoService stegoService, Runnable afterSendAction) 
    {
        this.messageService = messageService;
        this.stegoService = stegoService;
        this.afterSendAction = afterSendAction;

        // הגדרת כותרת הדיאלוג שתוצג בחלקו העליון
        setHeaderTitle("כתיבת הודעה חדשה");
        
        // קריאה לפונקציית העזר שבונה את כל רכיבי הממשק בתוך הדיאלוג
        buildWindow();
    }

    /**
     * בונה ומאכלס את רכיבי ממשק המשתמש (UI Components) בתוך הדיאלוג.
     * הפונקציה מגדירה שדות קלט, כפתורים, רכיב העלאת קבצים ואת לוגיקת השליחה.
     */
    private void buildWindow() 
    {
        // 1. הגדרת שדות הקלט (Input Fields)
        TextField toField = new TextField("אל (שם משתמש)");
        // סימון ויזואלי של כוכבית אדומה המעיד שזהו שדה חובה
        toField.setRequiredIndicatorVisible(true);
        
        TextField bodyField = new TextField("הודעה גלויה");
        
        Checkbox embedCheckbox = new Checkbox("האם להטמיע מסר סודי?");
        TextField secretField = new TextField("המסר הסודי");
        
        // הגדרת טקסט עזר שיוצג מתחת לשדה המסר הסודי להדרכת המשתמש
        secretField.setHelperText("מגבלת מסר סודי: עד 65,535 תווים");
        
        // כברירת מחדל, שדה המסר הסודי מוסתר עד שהמשתמש מסמן שהוא רוצה להטמיע
        secretField.setVisible(false);
        
        // הוספת מאזין (Listener) לתיבת הסימון: ברגע שמשנים את הערך (מסמנים/מורידים סימון), 
        // נראות השדה הסודי תתעדכן בהתאם לערך החדש של תיבת הסימון.
        embedCheckbox.addValueChangeListener(e -> secretField.setVisible(e.getValue()));
        
        // 2. הגדרת רכיב ההעלאה (Upload)
        // שומר את הקובץ באופן זמני בשרת כדי למנוע העמסה על הזיכרון הראשי (RAM) של השרת בזמן העלאת קבצים גדולים.
        Upload uploadComponent = new Upload(UploadHandler.toTempFile((meta, file) -> 
        {
            // שומרים את הקובץ הפיזי ואת סוג ה-MIME שלו (למשל: image/png) ברגע שההעלאה מסתיימת
            uploadedFile = file;
            uploadedMimeType = meta.contentType();
        }));

        // הגבלת כמות הקבצים שניתן להעלות במקביל להודעה בודדת
        uploadComponent.setMaxFiles(1);
        uploadComponent.setMaxFileSize(15 * 1024 * 1024); // הגבלת גודל: 15MB

        // הוספת טקסט עזר ויזואלי לרכיב ההעלאה עם הגדרות עיצוב מותאמות אישית (CSS)
        Span uploadHint = new Span("💡 פורמטים מומלצים להטמעה: PNG, JPG, WAV | מקסימום 15MB");
        uploadHint.getStyle()
                .set("font-size", "12px")
                .set("color", "var(--lumo-secondary-text-color)");

        // 3. הגדרת כפתור השליחה והלוגיקה המקושרת אליו (Event Listener)
        Button sendButton = new Button("שלח", e -> 
        {
            // שמירת המופע הנוכחי של ה-UI כדי שנוכל לגשת אליו מאוחר יותר מחוט הרקע
            UI currentUI = UI.getCurrent(); 
            
            // וידוא שהמשתמש עדיין מחובר במערכת (שליפת אובייקט המשתמש מה-Session של Vaadin)
            User currentUser = (User) VaadinSession.getCurrent().getAttribute("user");
            if (currentUser == null) 
            {
                Notification.show("שגיאה: משתמש לא מחובר", 4000, Position.MIDDLE);
                return; // עצירת תהליך השליחה אם המשתמש לא חוקי
            }
            
            String senderName = currentUser.getUsername();
            String toUser = toField.getValue();
            
            // ולידציה בסיסית לשדה הנמען - בדיקה שהשדה לא ריק ולא מכיל רק רווחים
            if (toUser == null || toUser.trim().isEmpty()) 
            {
                // סימון השדה באדום כדי להתריע למשתמש על שגיאה
                toField.setInvalid(true); 
                toField.setErrorMessage("חובה להזין נמען");
                
                // הצגת התראת שגיאה קופצת במרכז המסך
                Notification.show("חובה להזין את שם המשתמש של הנמען", 3000, Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return; // עצירת התהליך עד לתיקון השגיאה
            }
            // איפוס מצב השגיאה במידה וההזנה תקינה
            toField.setInvalid(false); 

            // איסוף שאר הנתונים שהוזנו בטופס
            String bodyText = bodyField.getValue();
            boolean doEmbed = embedCheckbox.getValue();
            String secretText = secretField.getValue();

            // ולידציה מקדימה במקרה של ניסיון הטמעה: האם הועלה קובץ והוזן מסר סודי?
            if (doEmbed) 
            {
                // בדיקה האם קובץ הועלה ונשמר בהצלחה כקובץ זמני
                if (uploadedFile == null || !uploadedFile.exists()) 
                {
                    Notification.show("שגיאה: בחרת להטמיע מסר, אך לא העלית קובץ מדיה!", 5000, Position.MIDDLE)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return; // עצירת השליחה
                }
                
                // בדיקה שהוזן תוכן בשדה המסר הסודי
                if (secretText == null || secretText.trim().isEmpty()) 
                {
                    secretField.setInvalid(true);
                    secretField.setErrorMessage("חובה להזין מסר סודי");
                    Notification.show("שגיאה: לא הזנת מסר סודי להטמעה!", 5000, Position.MIDDLE)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return; // עצירת השליחה
                }
            }
            secretField.setInvalid(false);

            // 4. סגירת הדיאלוג והצגת חיווי למשתמש שהתהליך מתרחש ברקע
            // פעולה זו משפרת את חווית המשתמש (UX) בכך שהיא לא משאירה אותו להמתין מול חלון קפוא
            Notification.show("ההודעה נשלחת ברקע...", 3000, Position.MIDDLE);
            this.close();

            // 5. הרצת תהליכון אסינכרוני (Thread) לעיבוד ושליחת הנתונים
            // מונע את חסימת ה-Main UI Thread בזמן הפעלת אלגוריתמים כבדים (כמו סטגנוגרפיה)
            new Thread(() ->
            {
                try 
                {
                    byte[] fileData = null;
                    // המרת הקובץ הפיזי למערך בתים (byte array) לצורך עיבוד ושמירה במסד הנתונים
                    if (uploadedFile != null && uploadedFile.exists()) 
                    {
                        fileData = java.nio.file.Files.readAllBytes(uploadedFile.toPath());
                    }

                    // מסלול א': שליחת הודעה עם הטמעת סטגנוגרפיה (נדרש קובץ ומסר סודי)
                    if (doEmbed && fileData != null) 
                    {
                        // הפעלת שירות הסטגנוגרפיה. משתמש ב-Callback (פונקציית תגובה) כדי להחזיר את התוצאה ל-UI
                        // מאחר וההטמעה עלולה לקחת זמן, היא פועלת אסינכרונית ומפעילה את הבלוק הבא בסיומה
                        stegoService.embedMsg(fileData, uploadedMimeType, secretText, senderName, (success, resultData, errorMessage) -> 
                        {
                            // עדכון ממשק משתמש מתהליכון רקע חייב להיעשות באמצעות currentUI.access
                            // אחרת נקבל שגיאת גישה של Vaadin (IllegalStateException)
                            currentUI.access(() -> 
                            { 
                                if (success && resultData != null) 
                                {
                                    // האלגוריתם הצליח: שומרים את ההודעה במסד הנתונים עם הקובץ המעובד (resultData)
                                    // מציינים את סוג ההודעה כ-"Embed"
                                    messageService.sendMessage(senderName, toUser, bodyText, resultData, uploadedMimeType, "Embed");
                                    
                                    // הפעלת פעולת הריענון (לדוגמה: עדכון הטבלה במסך הראשי) במידה והוגדרה
                                    if (afterSendAction != null) afterSendAction.run(); 
                                    
                                    Notification successNotif = Notification.show("ההודעה הסודית נשלחה בהצלחה!", 4000, Position.MIDDLE);
                                    successNotif.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                                }
                                else 
                                {
                                    // האלגוריתם נכשל (למשל: תמונה קטנה מדי למסר המבוקש):
                                    // מציגים למשתמש את הודעת השגיאה החכמה שהגיעה משכבת הלוגיקה, ולא שולחים כלום.
                                    String errorText = (errorMessage != null && !errorMessage.isEmpty()) ? errorMessage : "שגיאה לא ידועה בהטמעה.";
                                    Notification errorNotif = Notification.show("שגיאה בשליחה: " + errorText, 6000, Position.MIDDLE);
                                    errorNotif.addThemeVariants(NotificationVariant.LUMO_ERROR);
                                }
                                
                                // דחיפת העדכון למשתמש (Push) נדרשת כיוון שהפעולה מתבצעת ברקע ולא כחלק מבקשת HTTP רגילה.
                                // זה גורם לשרת לשלוח באופן אקטיבי את השינויים לדפדפן של המשתמש (Server-Sent Events/WebSockets).
                                currentUI.push(); 
                            });
                        });
                    } 
                    // מסלול ב': שליחת הודעה רגילה עם צירוף קובץ גלוי (או ללא קובץ כלל)
                    else 
                    {
                        // שמירת ההודעה הרגילה במסד הנתונים, מציינים את סוג ההודעה כ-"Upload"
                        messageService.sendMessage(senderName, toUser, bodyText, fileData, uploadedMimeType, "Upload");
                        
                        // גישה בטוחה לממשק המשתמש כדי להציג הודעת הצלחה ולרענן את התצוגה
                        currentUI.access(() -> 
                        {
                            if (afterSendAction != null) afterSendAction.run(); 
                            Notification successNotif = Notification.show("ההודעה נשלחה בהצלחה!", 4000, Position.MIDDLE);
                            successNotif.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                            
                            // דחיפת העדכון לדפדפן
                            currentUI.push(); 
                        });
                    }
                } 
                catch (Exception ex) 
                { 
                    // תפיסת שגיאות קריטיות בתהליכון (כגון שגיאות בקריאת הקובץ ממערכת ההפעלה או קריסות בלתי צפויות)
                    // יש לעדכן את המשתמש שניסיון השליחה כשל
                    currentUI.access(() -> 
                    {
                        Notification errorNotif = Notification.show("שגיאה בשליחת ההודעה ברקע: " + ex.getMessage(), 6000, Position.MIDDLE);
                        errorNotif.addThemeVariants(NotificationVariant.LUMO_ERROR);
                        currentUI.push(); 
                    });
                }
            }).start(); // התחלת ביצוע חוט הרקע בפועל
        });
        
        // צביעת כפתור השליחה ככפתור הראשי של הדיאלוג (לרוב צבע כחול מודגש ב-Vaadin)
        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // ארגון כלל רכיבי הממשק (שדות טקסט, העלאה, צ'קבוקס) בפריסה אנכית (אחד מתחת לשני)
        VerticalLayout layout = new VerticalLayout(toField, bodyField, uploadHint, uploadComponent, embedCheckbox, secretField);
        
        // הוספת הפריסה האנכית לגוף הדיאלוג
        add(layout);
        
        // הוספת כפתור השליחה לאזור התחתון (Footer) של הדיאלוג, לשמירה על חווית משתמש עקבית
        getFooter().add(sendButton);
    }
}