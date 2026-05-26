package yeonatano.steganography_system.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.FileUploadCallback;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.server.streams.UploadMetadata;
import com.vaadin.flow.dom.Element;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

import yeonatano.steganography_system.datamodels.User;
import yeonatano.steganography_system.services.StegnoService;
import yeonatano.steganography_system.services.StegnoService.EmbedTaskCallback;
import yeonatano.steganography_system.services.StegnoService.ExtractTaskCallback;

/**
 * מחלקת התצוגה המרכזית (View) של מערכת הסטגנוגרפיה.
 * משמשת כנקודת הממשק העיקרית (UI Layer) מול המשתמש לביצוע פעולות הטמעה וחילוץ של מסרים.
 * המחלקה מיישמת את BeforeEnterObserver כדי להבטיח בקרת גישה (Access Control) ולוודא שרק משתמשים מחוברים ניגשים לדף.
 */
@Route(value = "stagno", layout = MainLayout.class)
public class StegnoView extends VerticalLayout implements BeforeEnterObserver 
{
    // הזרקת תלויות (Dependency Injection) לשירות הלוגיקה העסקית.
    // שומר על הפרדת רשויות (Separation of Concerns) בין שכבת התצוגה לשכבת הלוגיקה.
    private final StegnoService stgnoService;
    
    // שמירת המופע הנוכחי של ה-UI. קריטי לעדכון הממשק מתוך תהליכי רקע (Background Threads) בצורה בטוחה.
    private final UI ui;
    
    // רכיבי ממשק משתמש (Stateful UI Components)
    private Upload upload;
    private TextField msgField;
    private Notification currentNotification = new Notification();
    private final VerticalLayout resultsContainer = new VerticalLayout();
    
    // ניהול מצב (State Management) של הקובץ המועלה באופן זמני
    private File uploadedFile;
    private String uploadedMimeType;

    /**
     * בנאי המחלקה. מופעל על ידי שלד התוכנה (Spring/Vaadin) בעת ניתוב לדף.
     * 
     * @param stgnoService מופע (Singleton/Bean) של שירות הסטגנוגרפיה, מוזרק אוטומטית.
     */
    public StegnoView(StegnoService stgnoService) 
    {   
        this.stgnoService = stgnoService;
        this.ui = UI.getCurrent();

        // הגדרת פריסת הבסיס של הדף (Flexbox מאחורי הקלעים)
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        // בניית הממשק באמצעות פונקציות עזר לשמירה על קוד מודולרי, קריא וקל לתחזוקה
        UploadComponent();
        setupMessageField();
        HorizontalLayout buttonsLayout = setupButtons();
        VerticalLayout cardLayout = buildMainCard(buttonsLayout);

        // אתחול הקונטיינר הדינמי שיציג את התוצאות (יאוכלס רק לאחר סיום עיבוד)
        resultsContainer.setWidthFull();
        resultsContainer.setAlignItems(Alignment.CENTER);
        
        add(cardLayout, resultsContainer);
    }

    // --- 2. תתי פונקציות ליצירת הממשק (UI Component Builders) ---

    /**
     * מאתחל את רכיב העלאת הקבצים.
     * משתמש ב-UploadHandler ליצירת קובץ זמני בשרת (Temp File) כדי לא להעמיס על זיכרון ה-RAM במקרה של קבצים גדולים.
     */
    private void UploadComponent() 
    {
        upload = new Upload(UploadHandler.toTempFile(new FileUploadCallback() {
            @Override
            public void complete(UploadMetadata metadata, File file) throws IOException {
                uploadedFile = file;
                uploadedMimeType = metadata.contentType();
                
                // עדכון הממשק חייב להתבצע דרך ui.access מכיוון שאירוע ההעלאה מנוהל ב-Thread נפרד של השרת
                ui.access(() -> showNotification("קובץ הועלה זמנית בהצלחה", NotificationVariant.LUMO_SUCCESS));
            }
        }));

        

        // מניעת דליפות זיכרון (Memory Leaks) וקוד זבל: ניקוי הקובץ הזמני כשהמשתמש מסיר אותו מהרכיב
        upload.getElement().addEventListener("file-remove", event -> clearUploadData());
        upload.setMaxFileSize(15 * 1024 * 1024); // הגבלת גודל ל-15MB למניעת מתקפות DoS והצפת שרת

        upload.setAcceptedFileTypes("image/*", "audio/*");

        upload.setWidthFull();
        upload.setMaxFiles(1);
        upload.addFileRejectedListener(event -> 
            showNotification(event.getErrorMessage(), NotificationVariant.LUMO_ERROR)
        );
    }

    /**
     * מאתחל את שדה הזנת המסר הסודי.
     */
    private void setupMessageField() 
    {
        msgField = new TextField();
        msgField.setLabel("Message to embed");
        msgField.setPlaceholder("Enter your secret message here...");
        msgField.setClearButtonVisible(true);
        msgField.setWidthFull();

        msgField.setHelperText("מגבלת מסר סודי: עד 65,535 תווים (כ-65KB)");
    }

    /**
     * מאגד את כפתורי הפעולות הראשיות (הטמעה וחילוץ).
     * מקשר בין אירועי הלחיצה (Click Listeners) לפונקציות הלוגיקה העסקית.
     */
    private HorizontalLayout setupButtons() 
    {
        Button btnEmbed = new Button("Embed & save To DB", e -> embedMsgAndAddImgToDB(msgField.getValue()));
        btnEmbed.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button btnExtract = new Button("Extract msg", e -> extractMsg());
        btnExtract.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        HorizontalLayout layout = new HorizontalLayout(btnEmbed, btnExtract);
        layout.setWidthFull();
        layout.setJustifyContentMode(JustifyContentMode.CENTER);
        return layout;
    }

    /**
     * אורז את רכיבי הקלט לתוך מעטפת ויזואלית של כרטיס (Card Pattern) לשיפור חווית המשתמש (UX/UI).
     */
    private VerticalLayout buildMainCard(HorizontalLayout buttonsLayout) 
    {
        Span uploadHint = new Span("💡 פורמטים נתמכים: PNG, JPG, WAV | רזולוציה מרבית: 2000x2000 | מקסימום 15MB");
        uploadHint.getStyle()
                .set("font-size", "13px")
                .set("color", "var(--lumo-secondary-text-color)") // משתמש בצבע האפור הסטנדרטי של Vaadin
                .set("text-align", "center");

        VerticalLayout card = new VerticalLayout(upload, msgField, buttonsLayout, uploadHint);
        card.setAlignItems(Alignment.CENTER);
        card.setMaxWidth("500px");
        
        // שימוש במשתני CSS של Lumo Theme (מערכת העיצוב של Vaadin) לשמירה על אחידות עיצובית
        card.getStyle().set("box-shadow", "var(--lumo-box-shadow-m)");
        card.getStyle().set("border-radius", "var(--lumo-border-radius-l)");
        card.getStyle().set("padding", "var(--lumo-space-xl)");
        card.getStyle().set("background-color", "var(--lumo-base-color)");
        
        return card;
    }

    /**
     * יוצר תצוגה חזותית להשוואה בין קובץ המקור לקובץ המכיל את המסר הנסתר (Stego-object).
     */
    private HorizontalLayout createComparisonView(byte[] original, byte[] stego, String mimeType) 
    {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setJustifyContentMode(JustifyContentMode.CENTER);
        layout.setSpacing(true);

        VerticalLayout originalSection = createMediaCard("הקובץ המקורי", original, mimeType);
        VerticalLayout stegoSection = createMediaCard("לאחר הטמעה", stego, mimeType);

        layout.add(originalSection, stegoSection);
        return layout;
    }

    /**
     * בונה כרטיסיית מדיה דינמית. 
     * משתמש בטכניקת Data URI (קידוד Base64) כדי להזרים את המדיה ישירות ל-DOM של הדפדפן
     * ללא צורך בשמירת קבצים סטטיים על השרת, מה שמגביר את אבטחת המידע וחוסך ב-I/O.
     */
    private VerticalLayout createMediaCard(String label, byte[] data, String mimeType) 
    {
        VerticalLayout card = new VerticalLayout();
        card.setAlignItems(Alignment.CENTER);
        card.add(new H3(label));

        // קידוד מערך הבייטים למחרוזת Base64 כדי שהדפדפן יוכל לפענח ולהציג כקובץ
        String base64Data = Base64.getEncoder().encodeToString(data);
        String dataUri = "data:" + mimeType + ";base64," + base64Data;

        if (mimeType.startsWith("image/")) 
        {
            // שימוש בנתיב מלא כדי למנוע התנגשות (Namespace Collision) עם מודל נתונים מקומי בשם Image
            Image image = new Image(dataUri, label);
            image.setMaxWidth("350px"); 
            image.getStyle().set("border", "1px solid #ddd").set("border-radius", "8px");
            card.add(image);
        } 
        else if (mimeType.startsWith("audio/")) 
        {
            // ייצור דינמי של תגית <audio> ב-HTML5
            Element audio = new Element("audio");            
            audio.setAttribute("controls", "true");
            audio.setAttribute("src", dataUri);
            card.getElement().appendChild(audio);
        }
        
        return card;
    }
    
    // --- 3. לוגיקה עסקית (Business Logic Integration) ---

    /**
     * מנהל את תהליך ההטמעה.
     * קורא את הקובץ, מעביר את הנתונים לשירות הסטגנוגרפיה לעיבוד אסינכרוני,
     * ומטפל בתגובה (Callback) לעדכון הממשק והצגת אפשרות הורדה.
     */
    private void embedMsgAndAddImgToDB(String msg) 
    {
        if (uploadedFile == null || !uploadedFile.exists() || msg.trim().isEmpty()) 
        {
            showNotification("חסרים פרטים: אנא העלה קובץ והזן הודעה", NotificationVariant.LUMO_ERROR);
            return;
        }

        // 1. יצירת דגל הביטול והעברתו לפונקציית הספינר
        AtomicBoolean isCancelled = new AtomicBoolean(false);
        Dialog spinner = createSpinner("מצפין ומטמיע את המסר... אנא המתן", isCancelled);
        spinner.open();

        try {
            final byte[] originalBytes = Files.readAllBytes(uploadedFile.toPath());

            stgnoService.embedMsg(originalBytes, uploadedMimeType, msg, getCurrentUsername(), new EmbedTaskCallback() {
                @Override
                public void onComplete(boolean isSuccess, byte[] resultBytes) 
                {
                    ui.access(() -> 
                    {
                        // 2. בדיקה: אם המשתמש לחץ ביטול בזמן שהשרת עבד - עוצרים פה ולא מעדכנים כלום!
                        if (isCancelled.get()) {
                            return; 
                        }

                        spinner.close(); // שחרור המסך רק אם התהליך סיים באופן טבעי
                        
                        if (isSuccess && resultBytes != null) 
                        {
                            resultsContainer.removeAll(); 
                            
                            HorizontalLayout comparison = createComparisonView(originalBytes, resultBytes, uploadedMimeType);
                            resultsContainer.add(comparison);

                            String base64Result = Base64.getEncoder().encodeToString(resultBytes);
                            String extension;
                            switch (uploadedMimeType) 
                            {
                                case "audio/wav":
                                    extension = ".wav";
                                    break;
                                case "image/jpeg":
                                case "image/jpg":
                                    extension = ".jpg";
                                    break;
                                case "image/png":
                                default:
                                    extension = ".png";
                                    break;
                            }

                            Anchor downloadLink = new Anchor("data:" + uploadedMimeType + ";base64," + base64Result, "הורד את הקובץ המוטמע");
                            downloadLink.getElement().setAttribute("download", "stego_output" + extension); 
                            downloadLink.getStyle().set("font-weight", "bold").set("font-size", "18px").set("margin-top", "15px");
                            
                            resultsContainer.add(downloadLink);

                            // ביצוע איפוס הממשק
                            upload.clearFileList();
                            msgField.setValue("");
                            clearUploadData();

                            showNotification("ההטמעה הסתיימה בהצלחה!", NotificationVariant.LUMO_SUCCESS);
                        } 
                        else 
                        {
                           showNotification("שגיאה בתהליך ההטמעה", NotificationVariant.LUMO_ERROR);
                        }
                        
                        ui.push(); // דחיפת העדכון לדפדפן
                    });
                }
            });
        } 
        catch (Exception ex) 
        {
            if (!isCancelled.get()) {
                spinner.close();
            }
            ex.printStackTrace(); 
            showNotification("שגיאה בקריאת הקובץ שהועלה", NotificationVariant.LUMO_ERROR);
        }
    }
    /**
     * מנהל את תהליך חילוץ המסר (Extraction) מתוך קובץ (Stego-object).
     */
    private void extractMsg() 
    {
        if (uploadedFile == null || !uploadedFile.exists()) 
        {
            showNotification("נא להעלות קובץ שמכיל מסר מוחבא", NotificationVariant.LUMO_ERROR);
            return;
        }

        // 1. חסימת המסך
        Dialog spinner = createSpinner("מחלץ מסר סודי... אנא המתן", null);
        spinner.open();
        
        resultsContainer.removeAll();
        
        try {
            byte[] fileBytes = Files.readAllBytes(uploadedFile.toPath());

            stgnoService.extractMsg(fileBytes, uploadedMimeType, new ExtractTaskCallback() 
            {
                @Override
                public void onComplete(boolean isSuccess, String msg) 
                {
                    ui.access(() -> 
                    {
                        spinner.close(); // שחרור המסך
                        
                        if (isSuccess && msg != null) 
                        {
                            showNotification("חילוץ הושלם בהצלחה!", NotificationVariant.LUMO_SUCCESS);
                            
                            H3 secretTitle = new H3("המסר הסודי שהתגלה:");
                            Span secretText = new Span(msg);
                            secretText.getStyle().set("font-size", "24px").set("color", "var(--lumo-primary-color)").set("font-weight", "bold");
                            
                            resultsContainer.add(secretTitle, secretText);
                            
                            // 2. ביצוע האיפוס
                            upload.clearFileList();
                            msgField.setValue("");
                            clearUploadData();
                        } 
                        else
                        {
                            showNotification("לא נמצא מסר או שהקובץ אינו נתמך", NotificationVariant.LUMO_ERROR);
                        }
                        
                        // 3. קריטי! דחיפת רענון רכיב ה-Upload לדפדפן באופן מיידי
                        ui.push(); 
                    });
                }
            });
        } 
        catch (Exception ex) 
        {
            spinner.close();
            ex.printStackTrace();
            showNotification("שגיאה בקריאת הקובץ", NotificationVariant.LUMO_ERROR);
        }
    }

    // --- 4. פונקציות עזר (Utility & Lifecycle Methods) ---

    /**
     * מבצע "איסוף זבל" ידני עבור קבצים זמניים.
     * מונע התמלאות של כונן השרת בקבצי Temp שנשארים לאחר סיום העיבוד.
     */
    private void clearUploadData() 
    {
        if (this.uploadedFile != null && this.uploadedFile.exists()) {
            this.uploadedFile.delete(); // מחיקת הקובץ הפיזי ממערכת ההפעלה
        }

        this.uploadedFile = null;
        this.uploadedMimeType = null;
    }

    /**
     * עוטף את מערכת ההתראות של Vaadin כדי לספק חווית משתמש חלקה.
     * סוגר התראות קודמות לפני הצגת חדשה כדי למנוע הצפה ויזואלית (Notification Stacking).
     */
    private void showNotification(String message, NotificationVariant variant) 
    {
        currentNotification.close();
        currentNotification = Notification.show(message, 5000, Notification.Position.MIDDLE);
        currentNotification.addThemeVariants(variant);
    }

    /**
     * אירוע מחזור חיים של Vaadin (Lifecycle Event).
     * מתבצע *לפני* שהדפדפן מרנדר את הדף. משמש כאן כ-Security Filter.
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) 
    {
        // בדיקת Session: האם קיים אובייקט חוקי של משתמש מחובר?
        if (VaadinSession.getCurrent().getAttribute("user") == null) {
            // מנגנון הגנה: ניתוב מחדש עוקף (Reroute) למסך התחברות
            event.rerouteTo(RegisterView.class);
        }
    }

    /**
     * שולף את זהות המשתמש מהסשן המנוהל של Vaadin.
     * 
     * @return שם המשתמש, או "Anonymous" כ-Fallback בטיחותי למקרה של תקלה בשליפה.
     */
    private String getCurrentUsername() 
    {
        User user = (User) VaadinSession.getCurrent().getAttribute("user");
        return (user != null) ? user.getUsername() : "Anonymous";
    }

    /**
     * יוצר חלון ספינר מודאלי שחוסם התערבות משתמש בזמן עיבוד נתונים.
     */
    /**
     * יוצר חלון ספינר מודאלי. 
     * אם מועבר cancelFlag, יתווסף כפתור "ביטול" שמשנה את ערך הדגל ל-true וסוגר את החלון.
     */
    private Dialog createSpinner(String text, AtomicBoolean cancelFlag) 
    {
        Dialog spinner = new Dialog();
        spinner.setCloseOnEsc(false);
        spinner.setCloseOnOutsideClick(false);
        
        Span spinnerText = new Span(text);
        spinnerText.getStyle().set("font-weight", "bold").set("color", "var(--lumo-primary-color)");
        
        ProgressBar pb = new ProgressBar();
        pb.setIndeterminate(true);
        pb.setWidth("100%");
        
        VerticalLayout layout = new VerticalLayout(spinnerText, pb);
        layout.setAlignItems(Alignment.CENTER);

        // הוספת כפתור הביטול רק אם הוגדר דגל בטיחות
        if (cancelFlag != null) 
        {
            Button cancelBtn = new Button("ביטול", e -> {
                cancelFlag.set(true); // סימון ל-Callback להתעלם מהתוצאות
                spinner.close();
                showNotification("פעולת ההטמעה בוטלה על ידי המשתמש", NotificationVariant.LUMO_ERROR);
            });
            cancelBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            layout.add(cancelBtn);
        }

        spinner.add(layout);
        return spinner;
    }
}