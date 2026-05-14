package yeonatano.steganography_system.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.dom.Element;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

import yeonatano.steganography_system.datamodels.User;
import yeonatano.steganography_system.services.StgnoService;
import yeonatano.steganography_system.services.StgnoService.EmbedTaskCallback;
import yeonatano.steganography_system.services.StgnoService.ExtractTaskCallback;

/**
 * תצוגת הסטגנוגרפיה המרכזית של המערכת.
 * נתיב (Route): "stagno" תחת הפריסה הראשית (MainLayout).
 * בדף זה המשתמש יכול להעלות קבצי מדיה, להטמיע בתוכם מסרים סודיים או לחלץ מסרים מקבצים קיימים.
 */
@Route(value = "stagno", layout = MainLayout.class)
public class StgnoView extends VerticalLayout implements BeforeEnterObserver 
{
    
    // שירות הסטגנוגרפיה המכיל את לוגיקת ההצפנה והחילוץ
    private final StgnoService stgnoService;
    // אובייקט ה-UI הנוכחי של Vaadin, משמש לעדכון הממשק מתוך תהליכים אסינכרוניים (Threads)
    private final UI ui;
    
    // רכיבי UI שנשמרים כמשתני מחלקה כדי שתהיה גישה אליהם מכל הפונקציות
    private Upload upload;
    private TextField msgField;
    private Notification currentNotification = new Notification();
    private final VerticalLayout resultsContainer = new VerticalLayout();
    
    // משתנים לשמירת הקובץ הזמני שהועלה
    private File uploadedFile;
    private String uploadedMimeType;

    /**
     * בנאי המחלקה. מאתחל את כל רכיבי הממשק (UI) ומסדר אותם על המסך.
     *
     * @param stgnoService שירות הסטגנוגרפיה (מוזרק על ידי Spring)
     */
    public StgnoView(StgnoService stgnoService) 
    {   
        this.stgnoService = stgnoService;
        this.ui = UI.getCurrent();

        // הגדרות עיצוב למסך הראשי (יישור למרכז ופריסה על כל המסך)
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        // יצירת הרכיבים השונים בעזרת תתי-פונקציות לשמירה על קוד קריא ונקי
        UploadComponent();
        setupMessageField();
        HorizontalLayout buttonsLayout = setupButtons();
        VerticalLayout cardLayout = buildMainCard(buttonsLayout);

        // הגדרת אזור התוצאות (למטה) שיתמלא אחרי פעולת הטמעה או חילוץ
        resultsContainer.setWidthFull();
        resultsContainer.setAlignItems(Alignment.CENTER);
        
        // הוספת הכרטיס המרכזי ואזור התוצאות למסך הראשי
        add(cardLayout, resultsContainer);
    }

    // --- 2. תתי פונקציות ליצירת הממשק (UI Builders) ---

    /**
     * אתחול רכיב העלאת הקבצים (Upload).
     * מגדיר את סוגי הקבצים המותרים, גודל מקסימלי, ומטפל באירועי העלאה ומחיקה של קבצים.
     */
    private void UploadComponent() 
    {
        // יצירת קובץ זמני בשרת בעת העלאה ושמירת פרטי הקובץ במשתני המחלקה
        upload = new Upload(UploadHandler.toTempFile((metadata, file) -> {
            this.uploadedFile = file;
            this.uploadedMimeType = metadata.contentType();
            
            // עדכון ה-UI בעזרת ui.access מכיוון שההעלאה מתבצעת ברקע
            ui.access(() -> showNotification("קובץ הועלה זמנית בהצלחה", NotificationVariant.LUMO_SUCCESS));
        }));

        // מחיקת נתוני הקובץ הזמני אם המשתמש לוחץ על כפתור המחיקה (X) ברכיב
        upload.getElement().addEventListener("file-remove", event -> clearUploadData());
        upload.setMaxFileSize(15 * 1024 * 1024); // הגבלת גודל ל-15MB
        upload.setWidthFull();
        // upload.setAcceptedFileTypes(".png", ".jpg", ".jpeg",".wav"); // סוגי קבצים נתמכים

        // הצגת הודעת שגיאה אם הועלה קובץ לא חוקי
        upload.addFileRejectedListener(event -> 
            showNotification(event.getErrorMessage(), NotificationVariant.LUMO_ERROR)
        );
    }

    /**
     * אתחול שדה הטקסט להזנת המסר הסודי להטמעה.
     */
    private void setupMessageField() 
    {
        msgField = new TextField();
        msgField.setLabel("Message to embed");
        msgField.setPlaceholder("Enter your secret message here...");
        msgField.setClearButtonVisible(true);
        msgField.setWidthFull();
    }

    /**
     * יצירת כפתורי הפעולה: "הטמעה ושמירה" ו-"חילוץ מסר".
     * 
     * @return רכיב HorizontalLayout המכיל את הכפתורים מסודרים בשורה
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
     * הרכבת הכרטיס המרכזי המכיל את העלאת הקובץ, שדה המסר והכפתורים בעיצוב של כרטיסיה.
     *
     * @param buttonsLayout פאנל הכפתורים
     * @return רכיב VerticalLayout מעוצב ככרטיס
     */
    private VerticalLayout buildMainCard(HorizontalLayout buttonsLayout) 
    {
        VerticalLayout card = new VerticalLayout(upload, msgField, buttonsLayout);
        card.setAlignItems(Alignment.CENTER);
        card.setMaxWidth("500px");
        
        // שימוש ב-CSS (משתני עיצוב של Lumo) ליצירת מראה של כרטיס מרחף
        card.getStyle().set("box-shadow", "var(--lumo-box-shadow-m)");
        card.getStyle().set("border-radius", "var(--lumo-border-radius-l)");
        card.getStyle().set("padding", "var(--lumo-space-xl)");
        card.getStyle().set("background-color", "var(--lumo-base-color)");
        
        return card;
    }

    /**
     * יצירת תצוגת "לפני ואחרי" שמוצגת לאחר ביצוע הטמעה מוצלחת.
     *
     * @param original הקובץ המקורי כמעט בייטים
     * @param stego הקובץ לאחר ההטמעה כמערך בייטים
     * @param mimeType סוג הקובץ
     * @return פאנל אופקי המכיל שתי כרטיסיות (מקור מול תוצאה)
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
     * פונקציית עזר ליצירת כרטיסיית מדיה בודדת להצגת תמונה או השמעת אודיו.
     * מקודדת את הנתונים הבינאריים ל-Base64 כדי להציגם ישירות בדפדפן (Data URI).
     *
     * @param label כותרת לתצוגה
     * @param data נתוני הקובץ כמערך בייטים
     * @param mimeType סוג הקובץ כדי לדעת איך להציג אותו (תמונה או שמע)
     * @return פאנל אנכי המכיל את כותרת המדיה והמדיה עצמה
     */
    private VerticalLayout createMediaCard(String label, byte[] data, String mimeType) 
    {
        VerticalLayout card = new VerticalLayout();
        card.setAlignItems(Alignment.CENTER);
        card.add(new H3(label));

        // המרת נתוני הבייטים למחרוזת Base64 עבור הדפדפן
        String base64Data = Base64.getEncoder().encodeToString(data);
        String dataUri = "data:" + mimeType + ";base64," + base64Data;

        // הצגת תמונה אם מדובר בסוג קובץ תמונה
        if (mimeType.startsWith("image/")) 
        {
            // שמתי את הנתיב ישירות כי אם הייתי עושה IMPORT זה היה מתבלבל לי עם המודל IMAGE שלי
            com.vaadin.flow.component.html.Image image = new com.vaadin.flow.component.html.Image(dataUri, label);
            image.setMaxWidth("350px"); 
            image.getStyle().set("border", "1px solid #ddd").set("border-radius", "8px");
            card.add(image);
        } 
        // השמעת קובץ שמע אם מדובר באודיו (יצירת אלמנט HTML של audio)
        else if (mimeType.startsWith("audio/")) 
        {
            Element audio = new Element("audio");            
            audio.setAttribute("controls", "true");
            audio.setAttribute("src", dataUri);
            card.getElement().appendChild(audio);
        }
        
        return card;
    }
    
    // --- 3. לוגיקה עסקית (פעולות ההטמעה והחילוץ) ---

    /**
     * לוגיקת ההטמעה (Embed): קוראת את הקובץ שהועלה, מפעילה את שירות הסטגנוגרפיה אסינכרונית,
     * ומציגה את התוצאות הכוללות כפתור הורדה למשתמש.
     *
     * @param msg המסר הסודי שיוטמע
     */
    private void embedMsgAndAddImgToDB(String msg) 
    {
        // ולידציה לפני התחלת תהליך: חובה קובץ והודעה
        if (uploadedFile == null || !uploadedFile.exists() || msg.trim().isEmpty()) 
        {
            showNotification("חסרים פרטים: אנא העלה קובץ והזן הודעה", NotificationVariant.LUMO_ERROR);
            return;
        }

        showNotification("מעבד נתונים... אנא המתן", NotificationVariant.LUMO_PRIMARY);

        try {
            // שמירת הבייטים המקוריים לפני השליחה לסרוויס כדי שנוכל להשוות אליהם מאוחר יותר
            final byte[] originalBytes = Files.readAllBytes(uploadedFile.toPath());

            // קריאה לשירות ההטמעה שרץ ברקע ומחזיר תשובה דרך Callback
            stgnoService.embedMsg(originalBytes, uploadedMimeType, msg, getCurrentUsername(), new EmbedTaskCallback() {
                @Override
                public void onComplete(boolean isSuccess, byte[] resultBytes) 
                {
                    // חזרה ל-Thread של ה-UI כדי לעדכן את התצוגה בביטחה
                    ui.access(() -> 
                    {
                        if (isSuccess && resultBytes != null) 
                        {
                            // --- כאן קורה הקסם של הצגת התוצאה! ---
                            resultsContainer.removeAll(); // ניקוי תוצאות ישנות
                            
                            // יצירת תצוגת ה"לפני ואחרי"
                            HorizontalLayout comparison = createComparisonView(originalBytes, resultBytes, uploadedMimeType);
                            resultsContainer.add(comparison);

                            // יצירת קישור להורדה (Anchor) עם הסיומת המתאימה דרך Data URI
                            String base64Result = Base64.getEncoder().encodeToString(resultBytes);
                            String extension = uploadedMimeType.equals("audio/wav") ? ".wav" : ".png";
                            Anchor downloadLink = new Anchor("data:" + uploadedMimeType + ";base64," + base64Result, "הורד את הקובץ המוטמע");
                            downloadLink.getElement().setAttribute("download", "stego_output" + extension); // שם הקובץ שיירד
                            downloadLink.getStyle().set("font-weight", "bold").set("font-size", "18px").set("margin-top", "15px");
                            
                            resultsContainer.add(downloadLink);

                            // איפוס ניקיון הטופס להכנה לפעולה הבאה
                            upload.clearFileList();
                            msgField.setValue("");
                            clearUploadData();

                            showNotification("ההטמעה הסתיימה בהצלחה!", NotificationVariant.LUMO_SUCCESS);
                        } 
                        else 
                        {
                           showNotification("שגיאה בתהליך ההטמעה", NotificationVariant.LUMO_ERROR);
                        }
                    });
                }
            });
        } 
        catch (Exception ex) 
        {
            ex.printStackTrace();
            showNotification("שגיאה בקריאת הקובץ שהועלה", NotificationVariant.LUMO_ERROR);
        }
    }

    /**
     * לוגיקת החילוץ (Extract): קוראת את הקובץ שהועלה ומחלצת ממנו מסר סודי אסינכרונית.
     */
    private void extractMsg() 
    {
        // ולידציה: דורש קובץ מועלה
        if (uploadedFile == null || !uploadedFile.exists()) 
        {
            showNotification("נא להעלות קובץ שמכיל מסר מוחבא", NotificationVariant.LUMO_ERROR);
            return;
        }

        showNotification("מחלץ מסר... אנא המתן", NotificationVariant.LUMO_PRIMARY);
        resultsContainer.removeAll(); // מנקים תצוגות קודמות
        
        try {
            // המרת הקובץ למערך בייטים לשליחה לשירות
            byte[] fileBytes = Files.readAllBytes(uploadedFile.toPath());

            // קריאה אסינכרונית לשירות החילוץ
            stgnoService.extractMsg(fileBytes, uploadedMimeType, new ExtractTaskCallback() 
            {
                @Override
                public void onComplete(boolean isSuccess, String msg) 
                {
                    // חזרה ל-UI Thread
                    ui.access(() -> 
                    {
                        if (isSuccess && msg != null) 
                        {
                            showNotification("חילוץ הושלם בהצלחה!", NotificationVariant.LUMO_SUCCESS);
                            
                            // מציגים את המסר שחולץ בגדול באזור התוצאות למטה
                            H3 secretTitle = new H3("המסר הסודי שהתגלה:");
                            Span secretText = new Span(msg);
                            secretText.getStyle().set("font-size", "24px").set("color", "var(--lumo-primary-color)").set("font-weight", "bold");
                            
                            resultsContainer.add(secretTitle, secretText);
                            
                            // ניקוי טופס
                            upload.clearFileList();
                            msgField.setValue("");
                            clearUploadData();
                        } 
                        else
                        {
                            showNotification("לא נמצא מסר או שהקובץ אינו נתמך", NotificationVariant.LUMO_ERROR);
                        }
                    });
                }
            });
        } 
        catch (Exception ex) 
        {
            ex.printStackTrace();
            showNotification("שגיאה בקריאת הקובץ", NotificationVariant.LUMO_ERROR);
        }
    }

    // --- 4. פונקציות עזר ---

    /**
     * מחיקת הקובץ הזמני מהשרת וניקוי המשתנים השומרים את נתוני ההעלאה.
     */
    private void clearUploadData() 
    {
        if (this.uploadedFile != null && this.uploadedFile.exists()) {
            this.uploadedFile.delete();
        }

        this.uploadedFile = null;
        this.uploadedMimeType = null;
    }

    /**
     * הצגת חלונית התראה (Notification) למשתמש, תוך סגירת ההתראה הקודמת אם קיימת.
     *
     * @param message תוכן ההודעה
     * @param variant סוג ההתראה (לדוגמה שגיאה, הצלחה, רגיל)
     */
    private void showNotification(String message, NotificationVariant variant) 
    {
        currentNotification.close(); // סגירת הודעה קודמת אם קיימת למניעת הצפה במסך
        currentNotification = Notification.show(message, 5000, Notification.Position.MIDDLE);
        currentNotification.addThemeVariants(variant);
    }

    /**
     * פונקציה לבדיקת הרשאות (חלק מממשק BeforeEnterObserver).
     * מופעלת לפני כניסה לדף, ומוודאת שהמשתמש אכן מחובר (קיים ב-Session).
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) 
    {
        if (VaadinSession.getCurrent().getAttribute("user") == null) {
            // אם המשתמש לא מחובר, ניתוב מחדש אוטומטי למסך ההתחברות
            event.rerouteTo(LoginView.class);
        }
    }

    /**
     * שליפת שם המשתמש הנוכחי מתוך ה-Session של Vaadin.
     *
     * @return שם המשתמש המחובר, או "Anonymous" במקרה לא צפוי שבו האובייקט ריק
     */
    private String getCurrentUsername() 
    {
        User user = (User) VaadinSession.getCurrent().getAttribute("user");
        return (user != null) ? user.getUsername() : "Anonymous";
    }
}