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

import yeonatano.steganography_system.services.StgnoService;
import yeonatano.steganography_system.services.StgnoService.EmbedTaskCallback;
import yeonatano.steganography_system.services.StgnoService.ExtractTaskCallback;

@Route("/stagno")
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

        upload.addFileRejectedListener(event -> {
            String errorMessage = event.getErrorMessage();
            Notification notification = Notification.show(errorMessage, 5000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        });

        msgField = new TextField();
        msgField.setLabel("msg to embed");
        msgField.setClearButtonVisible(true);

        ui = UI.getCurrent();
        Button Embed = new Button("Embed & send To DB", e -> embedMsgAndAddImgToDB(imgFile , msgField.getValue()));
        Button Extract = new Button("Extract msg",e -> extractMsg(imgFile));

        add(upload, Embed, Extract, msgField, autoDownloadAnchor); 

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
            stgnoService.embedMsg(imgFile, msg, new EmbedTaskCallback()
            {
                
                @Override
                public void onComplete(boolean isSuccess, byte[] resultBytes) // <-- שינוי: מקבל מערך בייטים
                {
                    // רק כאן משתמשים ב-ui.access כדי לעדכן את המסך
                    ui.access(() -> {

                        if (isSuccess)
                        {
                            notification.close();

                            // 1. הופכים את מערך הבייטים לקובץ שניתן להוריד
                            StreamResource res = new StreamResource("stego_result.jpg", 
                                () -> new java.io.ByteArrayInputStream(resultBytes)); // <-- שינוי הצינור
                            
                            // 2. מעדכנים את הקישור הקיים (במקום ליצור חדש כל פעם)
                            autoDownloadAnchor.setHref(res);
                            autoDownloadAnchor.setText("לחץ כאן להורדת התמונה המוטמעת");
                            autoDownloadAnchor.getElement().getStyle().set("display", "block"); // חושף את הקישור

                            // 3. מנקים את השדות לקראת הפעולה הבאה
                            upload.clearFileList();
                            msgField.setValue("");
                            // StgnoView.this.imgFile = new MemoryBuffer();

                            Notification.show("ההטמעה הסתיימה בהצלחה!", 5000, Notification.Position.MIDDLE)
                                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        }

                        else
                        {
                            notification.close();
                            notification = Notification.show("Not supported", 5000, Notification.Position.MIDDLE);
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
}
