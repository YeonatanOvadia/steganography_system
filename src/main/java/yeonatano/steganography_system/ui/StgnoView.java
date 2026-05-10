package yeonatano.steganography_system.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import jakarta.annotation.security.PermitAll;
import yeonatano.steganography_system.services.StgnoService;
import yeonatano.steganography_system.services.StgnoService.EmbedTaskCallback;
import yeonatano.steganography_system.services.StgnoService.ExtractTaskCallback;

@Route("/stagno")
@PermitAll
public class StgnoView extends VerticalLayout {
    private StgnoService stgnoService;
    private UI ui;
    private Upload upload;
    private MemoryBuffer imgFile;
    private TextField msgField;
    private Notification notification = new Notification();
    private Anchor autoDownloadAnchor = new Anchor(); // קישור להורדה

    public StgnoView(StgnoService stgnoService)
    {
     this.stgnoService = stgnoService;
        imgFile = new MemoryBuffer();
        upload = new Upload(imgFile);

        autoDownloadAnchor.getElement().getStyle().set("display", "none");
        autoDownloadAnchor.getElement().setAttribute("download", true);

        int maxFileSizeInBytes = 15 * 1024 * 1024; // 15MB
        upload.setMaxFileSize(maxFileSizeInBytes);
        upload.setWidthFull(); // פריסת אזור ההעלאה לכל הרוחב

        upload.addFileRejectedListener(event -> {
            String errorMessage = event.getErrorMessage();
            Notification notification = Notification.show(errorMessage, 5000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        });

        msgField = new TextField();
        msgField.setLabel("Message to embed");
        msgField.setPlaceholder("Enter your secret message here...");
        msgField.setClearButtonVisible(true);
        msgField.setWidthFull(); // פריסת שדה הטקסט לכל הרוחב

        ui = UI.getCurrent();

        // עיצוב הכפתורים
        Button Embed = new Button("Embed & send To DB", e -> embedMsgAndAddImgToDB(imgFile , msgField.getValue()));
        Embed.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY); // כפתור ראשי מודגש
        
        Button Extract = new Button("Extract msg", e -> extractMsg(imgFile));
        Extract.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_CONTRAST); // כפתור כהה ליצירת ניגודיות

        // סידור הכפתורים בשורה אחת
        com.vaadin.flow.component.orderedlayout.HorizontalLayout buttonsLayout = new com.vaadin.flow.component.orderedlayout.HorizontalLayout(Embed, Extract);
        buttonsLayout.setWidthFull();
        buttonsLayout.setJustifyContentMode(JustifyContentMode.CENTER); // מרכוז הכפתורים

        // יצירת "כרטיס" (Card) מעוצב שיעטוף את כל הרכיבים
        com.vaadin.flow.component.orderedlayout.VerticalLayout cardLayout = new com.vaadin.flow.component.orderedlayout.VerticalLayout(upload, msgField, buttonsLayout, autoDownloadAnchor);
        cardLayout.setAlignItems(Alignment.CENTER);
        cardLayout.setMaxWidth("500px");
        cardLayout.getStyle().set("box-shadow", "var(--lumo-box-shadow-m)"); // צללית של Vaadin
        cardLayout.getStyle().set("border-radius", "var(--lumo-border-radius-l)"); // פינות מעוגלות
        cardLayout.getStyle().set("padding", "var(--lumo-space-xl)");
        cardLayout.getStyle().set("background-color", "var(--lumo-base-color)");

        // הגדרות עיצוב למסך הראשי עצמו
        setSizeFull();
        setAlignItems(Alignment.CENTER); // יישור הכרטיס לאמצע המסך אופקית
        setJustifyContentMode(JustifyContentMode.CENTER); // יישור הכרטיס לאמצע המסך אנכית

        // הוספת הכרטיס למסך
        add(cardLayout);
    }

        private void embedMsgAndAddImgToDB(MemoryBuffer imgFile, String msg)
    {
        if(imgFile.getFileData() == null || msg.equals(""))
        {
            notification.close();
            notification = Notification.show("Details are missing", 5000, Notification.Position.MIDDLE);
        }

        else
        {
            notification.close();
            notification = Notification.show("running......", 5000, Notification.Position.MIDDLE);

            // קוראים לפעולה ב-Service ומוסרים לה את ה-Callback
            stgnoService.embedMsg(imgFile, msg, getCurrentUsername(), new EmbedTaskCallback()
            {
                
                @Override
                public void onComplete(boolean isSuccess, byte[] resultBytes) // <-- שינוי: מקבל מערך בייטים
                {
                    // רק כאן משתמשים ב-ui.access כדי לעדכן את המסך
                    ui.access(() -> {
                        if (isSuccess) {
                            notification.close();

                            // 1. חילוץ שם הקובץ המקורי כדי לשמור על הסיומת הנכונה (.wav או .jpg)
                            String originalFileName = imgFile.getFileName();
                            String resultFileName = "stego_" + originalFileName;

                            // 2. יצירת המשאב עם השם הדינמי והגדרת סוג תוכן בינארי
                            StreamResource res = new StreamResource(resultFileName, 
                                () -> new java.io.ByteArrayInputStream(resultBytes));
                            
                            // הכרחי: מונע מהדפדפן לנסות "לנחש" את סוג הקובץ וחוסם שגיאות רשת
                            res.setContentType("application/octet-stream");

                            // 3. עדכון הקישור והגדרת תכונת הורדה (download attribute)
                            autoDownloadAnchor.setHref(res);
                            autoDownloadAnchor.getElement().setAttribute("download", resultFileName);
                            autoDownloadAnchor.getElement().getStyle().set("display", "block");

                            // 4. ביצוע הורדה אוטומטית באמצעות JavaScript עם השהיה קלה לסנכרון ה-DOM
                            ui.getPage().executeJs("setTimeout(function() { $0.click(); }, 300);", autoDownloadAnchor.getElement());

                            // 5. ניקוי שדות הממשק
                            upload.clearFileList();
                            msgField.setValue("");

                            Notification.show("ההטמעה הסתיימה בהצלחה! ההורדה מתחילה...", 5000, Notification.Position.MIDDLE)
                                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        } else {
                            notification.close();
                            notification = Notification.show("Not supported or processing error", 5000, Notification.Position.MIDDLE);
                            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                        }
                    });
                }
            });
        }
    }

    private void extractMsg(MemoryBuffer imgFile) 
    {
        if(imgFile.getFileData() == null)
        {
            notification.close();
            notification = Notification.show("Details are missing", 5000, Notification.Position.MIDDLE);
            System.out.println("Details are missing");
        }

        else
        {
            notification.close();
            notification = Notification.show("running......", 5000, Notification.Position.MIDDLE);
            System.out.println("runing extract");
        

            stgnoService.extractMsg(imgFile, new ExtractTaskCallback()
            {

                @Override
                public void onComplete(boolean isSuccess, String msg)
                {
                    System.out.println("the task is finish");
                    ui.access(() -> {

                            if (isSuccess)
                            {
                                notification.close();
                                notification = Notification.show("Extract Done " + msg , 5000, Notification.Position.MIDDLE);
                                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                                upload.clearFileList();
                                // StgnoView.this.imgFile = new MemoryBuffer();
                                msgField.setValue("");
                            }

                            else
                            {
                                notification.close();
                                notification = Notification.show("We have a problem Try again", 5000, Notification.Position.MIDDLE);
                                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                            }
                        });
                    
            
                }

            });
        }

    }

    private String getCurrentUsername() 
    {
        return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
