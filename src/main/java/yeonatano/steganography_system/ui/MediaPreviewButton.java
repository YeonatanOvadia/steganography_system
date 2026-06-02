package yeonatano.steganography_system.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.component.Component;

import yeonatano.steganography_system.datamodels.Files;
import yeonatano.steganography_system.services.StegnoService;

import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * כפתור רב-שימושי המנהל את כל מחזור החיים של תצוגת מדיה מתוך מסד הנתונים:
 * 1. טעינה עצלה (Lazy Loading) - הקובץ (BLOB) נמשך ממסד הנתונים אסינכרונית רק כאשר המשתמש לוחץ על הכפתור, כדי לחסוך בזיכרון.
 * 2. יצירת ממשק תצוגה (Dialog) דינמי המזרים את המדיה ישירות לדפדפן (באמצעות Base64 Data URI).
 * 3. ניהול חילוץ סטגנוגרפי אסינכרוני מול שירותי הרקע, כולל טיפול בשגיאות.
 */
// [הוספת הסבר:] המחלקה יורשת מ-Button, ולכן היא קומפוננטת UI לכל דבר שניתן להוסיף לכל Layout או Grid ב-Vaadin.
public class MediaPreviewButton extends Button 
{
    // שירות ההזרקה לביצוע פעולות חילוץ נתונים מתוך תמונות או אודיו
    private final StegnoService steganographyService;

    /**
     * בנאי הכפתור. מאתחל את נראות הכפתור ומגדיר את הפעולה שתתרחש בעת לחיצה עליו.
     * @param buttonText הטקסט שיופיע על הכפתור.
     * @param fileSupplier פונקציה (Supplier) המספקת את קובץ המדיה ממסד הנתונים בעת קריאה. שימוש ב-Supplier מאפשר דחיית שליפת הנתונים עד לרגע הלחיצה.
     * @param stgnoService שירות הסטגנוגרפיה המוזרק לצורך ביצוע פעולות חילוץ.
     */
    public MediaPreviewButton(String buttonText, Supplier<Files> fileSupplier, StegnoService stgnoService) 
    {
        // שמירת הרפרנס לשירות הסטגנוגרפיה לשימוש עתידי בפונקציות המחלקה
        this.steganographyService = stgnoService;
        
        // הגדרת הטקסט שיוצג על גבי הכפתור בממשק
        setText(buttonText);
        
        // עיצוב הכפתור: כפתור ראשי (בולט) ובגודל קטן להתאמה לטבלאות או רשימות
        addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        // הגדרת פעולת הלחיצה: התחלת תהליך טעינת הקובץ מהמסד
        addClickListener(event -> 
        {
            // [הוספת הסבר:] חובה לשמור את ה-UI הנוכחי לפני כניסה ל-Thread חדש, כי בתוך ה-Thread
            // הפקודה UI.getCurrent() תחזיר null (אין לה קשר ישיר לבקשת ה-HTTP המקורית).
            UI currentUI = UI.getCurrent();
            
            // דגל בטיחות רב-תהליכוני (Thread-Safe) שמסמן האם המשתמש לחץ על כפתור "ביטול" במהלך ההמתנה
            // [הוספת הסבר:] AtomicBoolean הוא אובייקט בטוח לשימוש בסביבה מרובת-תהליכונים, 
            // המבטיח שקריאה וכתיבה אליו מ-Threads שונים לא יתנגשו ויגרמו למידע שגוי (Race Condition).
            AtomicBoolean isCancelled = new AtomicBoolean(false);
            
            // 1. בניית חלון ספינר (Loading) חוסם להצגת סטטוס ההורדה מהמסד
            Dialog spinnerDialog = new Dialog();
            // מניעת סגירת החלון בטעות על ידי לחיצה בחוץ או על מקש ESC, כדי להבטיח תהליך מסודר
            spinnerDialog.setCloseOnEsc(false);
            spinnerDialog.setCloseOnOutsideClick(false);
            
            // יצירת טקסט הסבר לחלון הטעינה ועיצובו (מודגש ובצבע הראשי של המערכת)
            Span text = new Span("שולף קובץ ממסד הנתונים...");
            text.getStyle().set("font-weight", "bold").set("color", "var(--lumo-primary-color)");
            
            // יצירת פס התקדמות (ProgressBar) לויזואליזציה של ההמתנה
            ProgressBar pb = new ProgressBar();
            // הגדרת מד ההתקדמות למצב "לא מוגדר" (פס שזז ימינה ושמאלה) כיוון שאיננו יודעים בדיוק כמה זמן תארך השליפה
            pb.setIndeterminate(true);
            
            // כפתור ביטול המאפשר הפסקת ההמתנה לקובץ ושינוי הדגל ל-true
            Button cancelBtn = new Button("ביטול הטעינה", e -> 
            {
                // עדכון הדגל כך שחוט הרקע ידע לעצור
                isCancelled.set(true); 
                // סגירת חלון הטעינה באופן מיידי
                spinnerDialog.close();
                // הקפצת הודעה למשתמש שהתהליך נעצר
                Notification.show("הפעולה בוטלה על ידי המשתמש", 2000, Position.MIDDLE);
            });
            
            // עיצוב כפתור הביטול: אדום ופחות בולט (Tertiary) כדי לא למשוך יותר מדי תשומת לב
            cancelBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            
            // איגוד הרכיבים (טקסט, מד התקדמות וכפתור) בתוך פריסה אנכית ומרכוז שלהם
            VerticalLayout layout = new VerticalLayout(text, pb, cancelBtn);
            layout.setAlignItems(Alignment.CENTER);
            
            // הוספת הפריסה לדיאלוג והצגתו למשתמש
            spinnerDialog.add(layout);
            spinnerDialog.open(); 
            
            // 2. הרצת השליפה (I/O) בתהליכון רקע למניעת הקפאת השרת או הדפדפן
            // [הוספת הסבר:] פעולות I/O (כמו קריאה ממסד נתונים) עלולות לארוך זמן. הרצתן בחוט הראשי 
            // תנעל את מסך המשתמש ותמנע ממנו ללחוץ על "ביטול". לכן פותחים Thread נפרד.
            new Thread(() -> 
            {
                try 
                {
                    // קריאה למסד הנתונים באמצעות פונקציית ה-Supplier (מושך את הנתונים בפועל)
                    Files file = fileSupplier.get();
                    
                    // במידה והמשתמש ביטל את הפעולה בזמן שהשרת עבד - עוצרים כאן ומונעים את הצגת החלון.
                    if (isCancelled.get()) 
                        return;
                    
                    // 3. חזרה לחוט העדכון של ממשק המשתמש
                    // [הוספת הסבר:] currentUI.access נועל בצורה בטוחה את מצב ה-UI הנוכחי ומאפשר ל-Thread 
                    // לעדכן את הדפדפן (כמו לסגור את חלון הטעינה או להציג את התמונה).
                    currentUI.access(() -> 
                    {
                        // סגירת חלון הטעינה כי הפעולה הסתיימה
                        spinnerDialog.close();
                        
                        // בדיקת תקינות התוכן שנשלף (מוודאים שהקובץ קיים ויש בו דאטה בינארי)
                        if (file == null || file.getImageData() == null) 
                        {
                            Notification.show("שגיאה טכנית: נתוני הקובץ ריקים", 3000, Position.MIDDLE);
                        }
                        else 
                        {
                            // קריאה לפונקציה הפנימית שבונה ומציגה את החלון עם המדיה
                            openMediaDialog(file);
                        }
                    });
                } 
                catch (Exception ex) 
                {
                    // טיפול בשגיאות תקשורת או מסד נתונים במהלך חוט הרקע
                    currentUI.access(() -> 
                    {
                        // חובה לסגור את חלון הטעינה גם במקרה של שגיאה כדי לא לתקוע את המשתמש
                        spinnerDialog.close();
                        Notification.show("שגיאה בתקשורת עם השרת", 3000, Position.MIDDLE);
                    });
                }
            }).start();
        });
    }

    // ==========================================
    // בניית ממשק הדיאלוג להצגת המדיה
    // ==========================================

    /**
     * פותחת את החלון המרכזי המציג את המדיה ומאפשר אינטראקציה עמה.
     * הפונקציה מחליטה אילו רכיבים להציג (תמונה/שמע, כלי חילוץ או התראת קובץ גלוי) בהתאם לסוג הקובץ.
     * @param attachedFile אובייקט הקובץ שנמשך ממסד הנתונים.
     */
    private void openMediaDialog(Files attachedFile) 
    {
        // יצירת חלון דיאלוג חדש לתצוגת המדיה
        Dialog mediaDialog = new Dialog();
        
        // הגדרת גודל דינמי המותאם אוטומטית לתוכן שבתוכו
        mediaDialog.setWidth("auto");
        mediaDialog.setHeight("auto");

        // שליפת המטא-דאטה והתוכן הבינארי מתוך האובייקט
        String mimeType = attachedFile.getMediaType();
        byte[] fileData = attachedFile.getImageData();
        
        // המרת המידע הבינארי לפורמט שניתן להזרים ישירות ל-DOM של הדפדפן
        String dataUri = convertBytesToDataUri(fileData, mimeType);

        // יצירת רכיבי התצוגה:
        // 1. המדיה עצמה (תמונה או נגן שמע)
        Component mediaElement = createMediaElement(mimeType, dataUri);
        // 2. אזור הכולל את כפתור ההורדה של הקובץ למחשב
        Component downloadSection = createDownloadLink(attachedFile, mimeType, dataUri);

        // סידור הרכיבים אחד מתחת לשני ומרכוזם
        VerticalLayout mainLayout = new VerticalLayout(mediaElement, downloadSection);
        mainLayout.setAlignItems(Alignment.CENTER);
        
        // אם מדובר בקובץ שעבר הטמעה (Stego-object) לפי הפעולה הרשומה במסד הנתונים, נוסיף כלי חילוץ
        if ("Embed".equals(attachedFile.getActionType())) 
        {
            // שליחה לפונקציית העזר שמוסיפה את כפתור החילוץ ותפריט הקליק-הימני
            addExtractionTools(mainLayout, mediaElement, fileData, mimeType);
        }
        // אחרת, מדובר בקובץ מדיה רגיל שהועבר גלוי ולכן אין טעם לאפשר חילוץ
        else 
        {
            // שליחה לפונקציית העזר שמוסיפה חיווי על כך שזהו קובץ גלוי
            addRegularFileIndication(mainLayout, mediaElement);
        }

        // הוספת פריסת התוכן אל תוך הדיאלוג והצגתו למשתמש
        mediaDialog.add(mainLayout);
        mediaDialog.open();
    }

    /**
     * ממיר מערך בייטים גולמי למחרוזת Base64 (Data URI Schema).
     * תבנית זו מאפשרת לדפדפן להציג או לנגן את הקובץ ישירות מהזיכרון, ללא צורך ביצירת קובץ פיזי בשרת (Stateless).
     * * @param fileData מערך הבייטים של הקובץ
     * @param mimeType סוג הקובץ (למשל: image/png או audio/wav)
     * @return מחרוזת בפורמט Data URI
     */
    private String convertBytesToDataUri(byte[] fileData, String mimeType) 
    {
        // [הוספת הסבר:] קידוד Base64 ממיר נתונים בינאריים לטקסט בטוח להעברה ברשת. 
        // חשוב לדעת שזה מגדיל את נפח המידע בכ-33% בזיכרון הדפדפן, אך מייתר את הצורך בראוטינג (Routing) נפרד לקובץ.
        
        // ביצוע ההמרה באמצעות המחלקה המובנית של Java
        String base64String = Base64.getEncoder().encodeToString(fileData);
        
        // שרשור המחרוזת לפורמט התקני שדפדפנים מצפים לקבל (data:[mime];base64,[data])
        return "data:" + mimeType + ";base64," + base64String;
    }

    /**
     * מייצר את הרכיב הוויזואלי (תמונה או נגן שמע) שיוצג בחלון המדיה, בהתאם ל-MIME Type.
     * * @param mimeType סוג הקובץ
     * @param dataUri המידע המקודד שיוזן כמקור (src)
     * @return רכיב Vaadin המוכן להוספה לתצוגה
     */
    private Component createMediaElement(String mimeType, String dataUri) 
    {
        // בדיקה האם הקובץ הוא תמונה (יכול להיות png, jpg, jpeg וכו')
        if (mimeType.startsWith("image/")) 
        {
            // יצירת רכיב תמונה והזנת ה-Base64 בתור ה-URL שלו
            Image img = new Image(dataUri, "Media Image");
            // הגבלת גודל התמונה כדי שלא תגלוש מחוץ למסך (עד 80% מרוחב/גובה החלון)
            img.getStyle().set("max-width", "80vw").set("max-height", "80vh");
            return img;
        }
        else 
        {
            // עבור שמע (audio), אנו מייצרים דינמית תגית <audio> של HTML5
            // [הוספת הסבר:] Vaadin מאפשר הזרקה של תגיות HTML ישירות ל-DOM דרך מחלקת Element
            // כאשר אין קומפוננטה מובנית לנגן שמע.
            Element audioHtmlElement = new Element("audio");
            audioHtmlElement.setAttribute("controls", "true"); // מציג את כפתורי הניגון/השהייה המובנים של הדפדפן
            audioHtmlElement.setAttribute("src", dataUri);     // מזין את קובץ השמע לתגית
            
            // עטיפת ה-Element הגולמי ב-Component של Vaadin (כמו Span) כדי שנוכל להוסיף אותו ל-Layout
            Span audioContainer = new Span();
            audioContainer.getElement().appendChild(audioHtmlElement);
            
            // מחזירים את העטיפה שמכילה בתוכה את נגן האודיו
            return audioContainer;
        }
    }

    /**
     * מייצר קישור להורדת המדיה אל המחשב המקומי של המשתמש.
     * עוטף את מחרוזת ה-Data URI בתגית עוגן (Anchor) של Vaadin.
     * * @param attachedFile אובייקט הקובץ
     * @param mimeType סוג הקובץ (לצורך חישוב הסיומת)
     * @param dataUri המידע המקודד להורדה
     * @return רכיב קישור (Anchor) המכיל בתוכו כפתור הורדה
     */
    private Anchor createDownloadLink(Files attachedFile, String mimeType, String dataUri) 
    {
        // קביעת הסיומת לפי ה-MIME Type כדי שהקובץ יישמר נכון במחשב
        // שימוש בתנאי מקוצר (Ternary Operator) לבחירת הסיומת המתאימה
        String extension = mimeType.equals("audio/wav") ? ".wav" : (mimeType.equals("image/png") ? ".png" : ".jpg");
        
        // יצירת שם קובץ דינמי המורכב מהמזהה הייחודי שלו במסד והסיומת
        String fileName = "media_" + attachedFile.getId() + extension;
        
        // יצירת הקישור עצמו שמצביע על תוכן ה-Base64
        Anchor downloadLink = new Anchor(dataUri, "");
        
        // [הוספת הסבר:] התכונה "download" אומרת לדפדפן שלא לנסות לפתוח את הקישור בכרטיסייה חדשה,
        // אלא לשמור אותו ישר כדיסק קשיח, ומספקת לו את שם הקובץ הרצוי כברירת מחדל.
        downloadLink.getElement().setAttribute("download", fileName); // מאלץ את הדפדפן להוריד במקום לפתוח
        
        // בניית כפתור יפה שיישב בתוך הקישור
        Button downloadButton = new Button("הורדת קובץ", VaadinIcon.DOWNLOAD.create());
        downloadButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS); // צביעת הכפתור בירוק כחיווי חיובי
        
        // השמת הכפתור בתוך הקישור, כך שלחיצה על הכפתור מפעילה את ההורדה
        downloadLink.add(downloadButton);
        
        return downloadLink;
    }

    /**
     * מוסיף כפתור חילוץ (מפורש) וכן תפריט קליק-ימני (Context Menu) על גבי המדיה עצמה.
     * פונקציה זו נקראת רק אם הקובץ זוהה כקובץ המכיל מסר (Embed).
     * * @param layout ה-Layout שאליו יתווספו הכפתורים
     * @param mediaElement רכיב המדיה (כדי להצמיד אליו את הקליק הימני)
     * @param fileData הנתונים הגולמיים של הקובץ לחילוץ
     * @param mimeType סוג הקובץ
     */
    private void addExtractionTools(VerticalLayout layout, Component mediaElement, byte[] fileData, String mimeType) 
    {
        // יצירת תפריט קליק ימני לחווית משתמש (UX) מתקדמת - מוצמד ישירות לתמונה או לנגן
        ContextMenu rightClickMenu = new ContextMenu(mediaElement);
        // הוספת פריט לתפריט שבלחיצה יפעיל את מתודת החילוץ עם הנתונים הרלוונטיים
        rightClickMenu.addItem("חלץ מסר סודי", event -> startExtractionProcess(fileData, mimeType));
        
        // יצירת כפתור חילוץ גלוי למשתמשים שלא מכירים את אפשרות הקליק הימני
        Button extractButton = new Button("חלץ מסר סודי", VaadinIcon.UNLOCK.create());
        extractButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY); // כפתור מודגש
        
        // חיבור אירוע הלחיצה לאותה מתודת חילוץ
        extractButton.addClickListener(event -> startExtractionProcess(fileData, mimeType));
        
        // טקסט עזר להדרכת המשתמש על האפשרויות שלו
        Span instructionHint = new Span("💡 ניתן גם ללחוץ קליק ימני על המדיה לחילוץ");
        instructionHint.getStyle().set("color", "var(--lumo-primary-color)").set("font-size", "12px");
        
        // הוספת הכפתור והטקסט לפריסת החלון
        layout.add(extractButton, instructionHint);
    }

    /**
     * מוסיף אינדיקציה ויזואלית ותפריט מנוטרל במקרה שהקובץ אינו מכיל מסר מוסתר (קובץ גלוי).
     * נועד למנוע בלבול של המשתמש שיחפש אופציה לחלץ מידע.
     * * @param layout ה-Layout של החלון
     * @param mediaElement רכיב המדיה עליו יוחל הקליק הימני המנוטרל
     */
    private void addRegularFileIndication(VerticalLayout layout, Component mediaElement) 
    {
        // יצירת תפריט קליק ימני המקושר למדיה
        ContextMenu disabledMenu = new ContextMenu(mediaElement);
        // יצירת פריט תפריט, הגדרת פעולה ריקה (למבדה ריקה) וכיבוי הפריט (Disabled)
        disabledMenu.addItem("קובץ גלוי (ללא מסר)", event -> {}).setEnabled(false);
        
        // טקסט אינפורמטיבי הממוקם מתחת למדיה
        Span infoMessage = new Span("קובץ גלוי (ללא מסר סודי מוחבא)");
        infoMessage.getStyle()
                   .set("color", "var(--lumo-secondary-text-color)") // צבע אפור/משני
                   .set("font-size", "14px")
                   .set("margin-top", "10px"); // ריווח מלמעלה
                   
        // הוספת הודעת המידע לפריסה
        layout.add(infoMessage);
    }

    // ==========================================
    // מנגנון חילוץ המסרים
    // ==========================================

    /**
     * מתחיל את תהליך החילוץ הסטגנוגרפי בצורה אסינכרונית.
     * מפעיל את חלון הטעינה וקורא לשירות הסטגנוגרפיה.
     * * @param fileData נתוני הקובץ לפענוח
     * @param mimeType סוג הקובץ
     */
    private void startExtractionProcess(byte[] fileData, String mimeType) 
    {
        // הצגת חלון טעינה למניעת אינטראקציה בזמן האלגוריתם (האלגוריתם עשוי לקחת כמה שניות)
        Dialog loadingSpinner = createLoadingSpinner("מפענח נתונים, אנא המתן...");
        loadingSpinner.open();

        // שמירת ה-UI הנוכחי לטובת עדכונים מהתהליכון
        UI currentUI = UI.getCurrent();

        // קריאה לשירות הסטגנוגרפיה. הפונקציה מצפה להחזר של הצלחה/כישלון, למסר (במקרה של הצלחה), או להודעת שגיאה
        // [הוספת הסבר:] כאן מועבר Callback (פונקציית צד-שלישי המיושמת כלמבדה), המאפשר לשירות הרקע
        // לאותת ל-UI מתי הוא סיים את העבודה, מבלי שה-UI יצטרך להמתין אקטיבית.
        steganographyService.extractMsg(fileData, mimeType, (isSuccess, secretMessage, errorMessage) -> 
        {
            // חזרה לחוט ה-UI באופן בטוח כדי לעדכן את המסך עם התוצאה
            // קריאה למתודת ה-Callback שלנו שמרכזת את הטיפול בסיום
            currentUI.access(() -> Callback(isSuccess, secretMessage, errorMessage, loadingSpinner));
        });
    }

    /**
     * מעבד ומציג את התוצאה של תהליך החילוץ (Callback Action).
     * פונקציה זו מופעלת על ידי שירות הסטגנוגרפיה ברגע שהוא מסיים לעבד את המידע.
     * * @param isSuccess האם האלגוריתם סיים ללא חריגות וחילץ מסר בהצלחה.
     * @param secretMessage המחרוזת המפוענחת (אם חולצה בהצלחה).
     * @param errorMessage השגיאה שהוחזרה מהאלגוריתם במידה והפענוח נכשל (למשל תמונה פגומה או ללא מסר).
     * @param loadingSpinner חלון הטעינה שיש לסגור כעת.
     */
    private void Callback(boolean isSuccess, String secretMessage, String errorMessage, Dialog loadingSpinner) 
    {
        // קודם כל, סוגרים את חלון החסימה של הטעינה כדי לשחרר את הממשק
        loadingSpinner.close(); 
        
        // בדיקה האם החילוץ הוגדר כמוצלח והמסר עצמו תקין ולא ריק
        if (isSuccess && secretMessage != null && !secretMessage.isEmpty()) 
        {
            // חילוץ מוצלח: מציג את המסר בחלון תוצאה חדש וייעודי הכולל כותרת ותוכן
            Dialog resultDialog = new Dialog(new H3("המסר הסודי שהתגלה:"), new Span(secretMessage));
            resultDialog.open();
        }
        else 
        {
            // כישלון בחילוץ: מציג התראת שגיאה (באדום) עם הסיבה הספציפית לכשלון (למשל, בעיה בחתימת הקובץ)
            // שימוש בתנאי מקוצר כדי להציג הודעת ברירת מחדל במידה ולא סופקה שגיאה מפורשת
            String errorDisplay = (errorMessage != null && !errorMessage.isEmpty()) ? errorMessage : "לא נמצא מסר חבוי, או שהפענוח נכשל.";
            
            // יצירת נוטיפיקציה והצגתה במרכז המסך למשך 5 שניות
            Notification errorNotif = Notification.show("שגיאה בפענוח: " + errorDisplay, 5000, Notification.Position.MIDDLE);
            errorNotif.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    /**
     * פונקציית עזר ליצירת ספינר (חלון טעינה) כללי שלא ניתן לסגור אותו ידנית.
     * משמש להשהיית אינטראקציית משתמש בזמן פעולות כבדות.
     * * @param messageText הטקסט שיוצג בחלון (למשל "מפענח נתונים...")
     * @return אובייקט Dialog מוכן להפעלה (open)
     */
    private Dialog createLoadingSpinner(String messageText) 
    {
        // יצירת אובייקט דיאלוג חדש
        Dialog spinnerDialog = new Dialog();
        
        // חסימת האפשרות לסגור את הדיאלוג באמצעות כפתור ה-ESC או לחיצה מחוץ לגבולותיו
        spinnerDialog.setCloseOnEsc(false);
        spinnerDialog.setCloseOnOutsideClick(false);

        // יצירת אלמנט הטקסט ועיצובו (הגדלה, הדגשה וצבע)
        Span textElement = new Span(messageText);
        textElement.getStyle().set("font-weight", "bold").set("font-size", "18px").set("color", "var(--lumo-primary-color)");

        // יצירת ועיצוב מד ההתקדמות
        ProgressBar progressBar = new ProgressBar();
        progressBar.setIndeterminate(true); // אנימציה מחזורית שלא תלויה באחוזים מדויקים אלא רק משדרת "עבודה ברקע"
        progressBar.setWidth("100%"); // פריסת מד ההתקדמות על כל רוחב הדיאלוג

        // איגוד הטקסט ומד ההתקדמות לפריסה אנכית ממורכזת
        VerticalLayout layout = new VerticalLayout(textElement, progressBar);
        layout.setAlignItems(Alignment.CENTER);
        
        // הוספת הפריסה לדיאלוג והחזרתו
        spinnerDialog.add(layout);
        return spinnerDialog;
    }
}