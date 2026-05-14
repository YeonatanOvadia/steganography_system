package yeonatano.steganography_system.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import yeonatano.steganography_system.datamodels.Files;
import yeonatano.steganography_system.datamodels.Message;
import yeonatano.steganography_system.datamodels.User;
import yeonatano.steganography_system.services.MessageService;
import yeonatano.steganography_system.services.StgnoService;

import java.util.Base64;

@Route(value = "sent", layout = MainLayout.class)
public class SentMessagesView extends VerticalLayout implements BeforeEnterObserver 
{

    private MessageService msgService;
    private StgnoService stgnoService;
    
    private Grid<Message> grid = new Grid<>(Message.class, false);

    public SentMessagesView(MessageService msgService, StgnoService stgnoService) 
    {
        this.msgService = msgService;
        this.stgnoService = stgnoService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setSpacing(true);

        add(new H1("הודעות שנשלחו"));

        // קריאה מסודרת לפונקציות העמודות
        showRecipientColumn(); // השינוי המרכזי: "אל" במקום "מאת"
        showBodyColumn();
        showAttachmentColumn();
        showDownloadColumn();
        showDeleteColumn();
        
        refreshGrid();
        grid.setSizeFull();
        
        add(grid);
    }

    /*
     * עמודת הנמען (למי נשלחה ההודעה)
     */
    private void showRecipientColumn() 
    {
        // הערה: החלף את getReceiver() בשם המדויק של פונקציית ה-Getter במודל Message שלך (למשל getTo או getRecipient)
        grid.addColumn(msg -> msg.getReceiver()).setHeader("אל");
    }

    private void showBodyColumn() 
    {
        grid.addColumn(Message::getBody).setHeader("תוכן גלוי");
    }

    private void showAttachmentColumn() 
    {
        grid.addComponentColumn(msg -> {
            if (!msg.hasFile()) return new Span("-");
            
            Files attachedFile = msgService.getFileById(msg.getFileId());
            
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

                    uiImage.addClickListener(event -> 
                    {
                        Dialog dialog = new Dialog();
                        Image enlargedImage = new com.vaadin.flow.component.html.Image(dataUri, "תמונה מוגדלת");
                        enlargedImage.getStyle().set("max-width", "90vw");
                        enlargedImage.getStyle().set("max-height", "90vh");
                        dialog.add(enlargedImage);
                        dialog.open();
                    });

                    attachContextMenuForExtraction(uiImage, fileData, mimeType);
                    return uiImage;
                }
                else if (mimeType.startsWith("audio/")) {
                    com.vaadin.flow.dom.Element audioElement = new com.vaadin.flow.dom.Element("audio");
                    audioElement.setAttribute("controls", "true");
                    audioElement.setAttribute("src", dataUri);
                    audioElement.getStyle().set("width", "180px");
                    audioElement.getStyle().set("height", "40px");

                    Span audioContainer = new Span();
                    audioContainer.getElement().appendChild(audioElement);

                    attachContextMenuForExtraction(audioContainer, fileData, mimeType);
                    return audioContainer;
                }
            }
            return new Span(); 
        }).setHeader("קובץ מצורף").setWidth("200px");
    }

    private void showDownloadColumn() 
    {
        grid.addComponentColumn(msg -> 
        {
            if (!msg.hasFile()) return new Span();

            yeonatano.steganography_system.datamodels.Files attachedFile = msgService.getFileById(msg.getFileId());
            if (attachedFile == null || attachedFile.getMediaType() == null || attachedFile.getImageData() == null) return new Span();

            String mimeType = attachedFile.getMediaType();
            byte[] fileData = attachedFile.getImageData();

            String extension = mimeType.equals("audio/wav") ? ".wav" : (mimeType.equals("image/png") ? ".png" : ".jpg");
            String fileName = "sent_msg_" + msg.getId() + "_file" + extension;
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
                msgService.deleteMessage(msg.getId());
                refreshGrid();
            });
            
            return deleteBtn;
        }).setHeader("מחיקה");    
    }

    private void attachContextMenuForExtraction(com.vaadin.flow.component.Component uiComponent, byte[] fileData, String mimeType) 
    {
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

    private void refreshGrid() 
    {
        User user = (User) VaadinSession.getCurrent().getAttribute("user");
        if (user != null) 
        {
            // שים לב: תצטרך לוודא שיש לך פונקציה כזו ב-MessageService שמחזירה את ההודעות שהמשתמש *שלח*
            grid.setItems(msgService.getMySentMessages(user.getUsername())); 
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event)
    {
        if (VaadinSession.getCurrent().getAttribute("user") == null) 
            event.rerouteTo(LoginView.class);    
    }
}