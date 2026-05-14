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

@Route(value = "inbox", layout = MainLayout.class)
public class InboxView extends VerticalLayout implements BeforeEnterObserver 
{

    private MessageService msgService;
    private StgnoService stgnoService;
    
    private Grid<Message> grid = new Grid<>(Message.class, false);

    public InboxView(MessageService msgService, StgnoService stgnoService) 
    {
        this.msgService = msgService;
        this.stgnoService = stgnoService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setSpacing(true);

        add(new H1("תיבת דואר נכנס"));

        Button composeBtn = new Button("הודעה חדשה", e -> openComposeDialog());
        
        // קריאה מסודרת לפונקציות העמודות בדיוק כמו ב-HistoryView
        showSenderColumn();
        showBodyColumn();
        showAttachmentColumn();
        showDownloadColumn();
        showDeleteColumn();
        
        refreshGrid();
        grid.setSizeFull();
        
        add(composeBtn, grid);
    }

    private void showSenderColumn() 
    {
        grid.addColumn(Message::getSender).setHeader("מאת");
    }

    private void showBodyColumn() 
    {
        grid.addColumn(Message::getBody).setHeader("תוכן גלוי");
    }

    private void showAttachmentColumn() 
    {
        grid.addComponentColumn(msg -> {
            if (!msg.hasFile()) return new Span("-");
            
            // שליפת הקובץ מתוך מסד הנתונים בעזרת ה-ID שלו שיש בהודעה
            yeonatano.steganography_system.datamodels.Files attachedFile = msgService.getFileById(msg.getFileId());
            
            if (attachedFile == null) return new Span("הקובץ הוסר");

            String mimeType = attachedFile.getMediaType();
            byte[] fileData = attachedFile.getImageData();
            
            if (mimeType != null && fileData != null) 
            {
                String base64String = Base64.getEncoder().encodeToString(fileData);
                String dataUri = "data:" + mimeType + ";base64," + base64String;

                if (mimeType.startsWith("image/")) 
                {
                    com.vaadin.flow.component.html.Image uiImage = new com.vaadin.flow.component.html.Image(dataUri, "תצוגה מקדימה");
                    uiImage.setHeight("60px"); 
                    uiImage.getStyle().set("border-radius", "8px");
                    uiImage.getStyle().set("cursor", "pointer");

                    // לחיצה שמאלית להגדלה
                    uiImage.addClickListener(event -> 
                    {
                        Dialog dialog = new Dialog();
                        com.vaadin.flow.component.html.Image enlargedImage = new com.vaadin.flow.component.html.Image(dataUri, "תמונה מוגדלת");
                        enlargedImage.getStyle().set("max-width", "90vw");
                        enlargedImage.getStyle().set("max-height", "90vh");
                        dialog.add(enlargedImage);
                        dialog.open();
                    });

                    // לחיצה ימנית לחילוץ
                    attachContextMenuForExtraction(uiImage, fileData, mimeType);
                    return uiImage;
                }
                else if (mimeType.startsWith("audio/")) 
                {
                    com.vaadin.flow.dom.Element audioElement = new com.vaadin.flow.dom.Element("audio");
                    audioElement.setAttribute("controls", "true");
                    audioElement.setAttribute("src", dataUri);
                    audioElement.getStyle().set("width", "180px");
                    audioElement.getStyle().set("height", "40px");

                    Span audioContainer = new Span();
                    audioContainer.getElement().appendChild(audioElement);

                    // לחיצה ימנית על אודיו
                    attachContextMenuForExtraction(audioContainer, fileData, mimeType);
                    return audioContainer;
                }
            }
            return new Span(); 
        }).setHeader("קובץ מצורף").setWidth("200px");
    }

    private void showDownloadColumn() {
        grid.addComponentColumn(msg -> {
            if (!msg.hasFile()) return new Span();

            yeonatano.steganography_system.datamodels.Files attachedFile = msgService.getFileById(msg.getFileId());
            if (attachedFile == null || attachedFile.getMediaType() == null || attachedFile.getImageData() == null) return new Span();

            String mimeType = attachedFile.getMediaType();
            byte[] fileData = attachedFile.getImageData();

            String extension = mimeType.equals("audio/wav") ? ".wav" : (mimeType.equals("image/png") ? ".png" : ".jpg");
            String fileName = "msg_" + msg.getId() + "_file" + extension;
            String dataUri = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(fileData);

            Anchor downloadLink = new Anchor(dataUri, "הורד קובץ");
            downloadLink.getElement().setAttribute("download", fileName);
            return downloadLink;
        }).setHeader("הורדה");
    }

    private void showDeleteColumn() 
    {
        grid.addComponentColumn(msg -> 
        {
            Button deleteBtn = new Button("מחיקה");
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
            
            deleteBtn.addClickListener(event -> 
            {
                // מחיקה של כל ההודעה (נניח ויש לך פונקציה כזו ב-MessageService)
                msgService.deleteMessage(msg.getId());
                refreshGrid();
            });
            
            return deleteBtn;
        }).setHeader("מחיקה");    
    }

    // פונקציית עזר להוספת התפריט הימני
    private void attachContextMenuForExtraction(com.vaadin.flow.component.Component uiComponent, byte[] fileData, String mimeType) {
        ContextMenu menu = new ContextMenu(uiComponent);
        menu.addItem("חלץ מסר סודי", e -> 
        {
            UI currentUI = UI.getCurrent(); 
            stgnoService.extractMsg(fileData, mimeType, (success, secretMsg) -> 
            {
                currentUI.access(() -> Notification.show("המסר הסודי: " + secretMsg, 5000, Notification.Position.MIDDLE));
            });
        });
    }

    private void openComposeDialog() 
    {
        Dialog dialog = new Dialog();
        
        TextField to = new TextField("אל (שם משתמש)");
        TextField body = new TextField("הודעה גלויה");
        
        Checkbox doEmbed = new Checkbox("האם להטמיע מסר סודי?");
        TextField secret = new TextField("המסר הסודי");
        secret.setVisible(false); 
        
        doEmbed.addValueChangeListener(e -> secret.setVisible(e.getValue()));

        File[] uploaded = new File[1];
        String[] mime = new String[1];
        
        Upload upload = new Upload(UploadHandler.toTempFile((meta, file) -> 
        {
            uploaded[0] = file;
            mime[0] = meta.contentType();
        }));

        Button send = new Button("שלח", e -> 
        {
            UI currentUI = UI.getCurrent(); 
            try 
            {
                byte[] data = null;
                if (uploaded[0] != null && uploaded[0].exists()) 
                {
                    data = java.nio.file.Files.readAllBytes(uploaded[0].toPath());
                }
                
                User user = (User) VaadinSession.getCurrent().getAttribute("user");
                if (user == null) 
                {
                    Notification.show("שגיאה: משתמש לא מחובר");
                    return;
                }
                String sender = user.getUsername();

                if (doEmbed.getValue() && data != null) 
                {
                    stgnoService.embedMsg(data, mime[0], secret.getValue(), sender, (success, result) -> {
                        msgService.sendMessage(sender, to.getValue(), body.getValue(), result, mime[0], "Embed");
                        currentUI.access(() -> { 
                            dialog.close(); 
                            refreshGrid(); 
                        });
                    });
                } 

                else 
                {
                    msgService.sendMessage(sender, to.getValue(), body.getValue(), data, mime[0], "Upload");
                    dialog.close(); 
                    refreshGrid(); 
                }
            } 
            catch (Exception ex) 
            { 
                Notification.show("שגיאה בשליחה: " + ex.getMessage()); 
            }
        });

        dialog.add(new VerticalLayout(to, body, upload, doEmbed, secret), send);
        dialog.open();
    }

    private void refreshGrid() 
    {
        User user = (User) VaadinSession.getCurrent().getAttribute("user");
        if (user != null) 
            grid.setItems(msgService.getMyInbox(user.getUsername())); 
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) 
    {
        if (VaadinSession.getCurrent().getAttribute("user") == null) 
            event.rerouteTo(LoginView.class);    
    }
}