package yeonatano.steganography_system.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.Route;

import yeonatano.steganography_system.services.StgnoService;

@Route("/stagno")
public class StgnoView extends VerticalLayout {
    private StgnoService stgnoService;
    private UI ui;
    private Upload upload;
    private MemoryBuffer imgFile;
    private TextField msgField;


    public StgnoView(StgnoService stgnoService) {
        this.stgnoService = stgnoService;
        imgFile = new MemoryBuffer();
        upload = new Upload(imgFile);


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
        
        add(upload, Embed, Extract, msgField); 

    }

    private Object extractMsg(MemoryBuffer imgFile) 
    {
        new Thread(() -> {
            String msg = stgnoService.extractMsg(imgFile);
                //מציג על המסך כאשר המשימה בוצעה 
            ui.access(() -> {
                if (msg != null)
                {
                    Notification success = Notification.show("f5 done", 5000, Notification.Position.MIDDLE);
                    success.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                    upload.clearFileList();
                    this.imgFile = new MemoryBuffer();
                }

                else
                {
                    Notification error = Notification.show("Not supported", 5000, Notification.Position.MIDDLE);
                    error.addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });

        }).start();

        return imgFile;

    }

    private int embedMsgAndAddImgToDB(MemoryBuffer imgFile, String msg) {
        if(imgFile.getFileData() == null || msg == "")
            Notification.show("Details are missing", 500, Notification.Position.MIDDLE);

        else
        {
            Notification.show("F5 runing", 5000, Notification.Position.MIDDLE);

            new Thread(() -> {
                boolean isSuccess = stgnoService.embedMsg(imgFile, msg);

                    
                //מציג על המסך כאשר המשימה בוצעה 
                ui.access(() -> {
                    if (isSuccess)
                    {
                        Notification success = Notification.show("f5 done", 5000, Notification.Position.MIDDLE);
                        success.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                        upload.clearFileList();
                        this.imgFile = new MemoryBuffer();
                        this.msgField.setValue("");

                    }
                    else
                    {
                        Notification error = Notification.show("Not supported", 5000, Notification.Position.MIDDLE);
                        error.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                });

            }).start();
        }

        return 0;

    }
}
