package yeonatano.steganography_system.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import yeonatano.steganography_system.datamodels.Files;
import yeonatano.steganography_system.services.StegnoService;

import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * כפתור רב-שימושי המנהל את כל מחזור החיים של תצוגת המדיה:
 * 1. טעינה עצלה (Lazy Loading) אסינכרונית עם אפשרות ביטול.
 * 2. בנייה והצגה של חלון מודאלי (Dialog) המכיל את המדיה, אפשרויות הורדה וחילוץ סטגנוגרפי.
 */
public class MediaPreviewButton extends Button 
{
    private final StegnoService steganographyService;

    public MediaPreviewButton(String buttonText, Supplier<Files> fileSupplier, StegnoService stgnoService) 
    {
        this.steganographyService = stgnoService;
        
        setText(buttonText);
        addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        addClickListener(event -> 
        {
            UI currentUI = UI.getCurrent();
            
            // דגל בטיחות שמסמן האם המשתמש לחץ על ביטול
            AtomicBoolean isCancelled = new AtomicBoolean(false);
            
            // בניית ספינר מקומי עם כפתור ביטול
            Dialog spinnerDialog = new Dialog();
            spinnerDialog.setCloseOnEsc(false);
            spinnerDialog.setCloseOnOutsideClick(false);
            
            Span text = new Span("שולף קובץ ממסד הנתונים...");
            text.getStyle().set("font-weight", "bold").set("color", "var(--lumo-primary-color)");
            
            ProgressBar pb = new ProgressBar();
            pb.setIndeterminate(true);
            
            // כפתור הביטול שיעצור את הכל
            Button cancelBtn = new Button("ביטול הטעינה", e -> {
                isCancelled.set(true); 
                spinnerDialog.close();
                Notification.show("הפעולה בוטלה על ידי המשתמש", 2000, Notification.Position.MIDDLE);
            });
            cancelBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            
            VerticalLayout layout = new VerticalLayout(text, pb, cancelBtn);
            layout.setAlignItems(Alignment.CENTER);
            spinnerDialog.add(layout);
            spinnerDialog.open(); 
            
            // הרצת השליפה הכבדה ברקע
            new Thread(() -> {
                try {
                    Files file = fileSupplier.get();
                    
                    if (isCancelled.get()) {
                        return;
                    }
                    
                    currentUI.access(() -> {
                        spinnerDialog.close();
                        if (file == null || file.getImageData() == null) {
                            Notification.show("שגיאה טכנית: נתוני הקובץ ריקים", 3000, Notification.Position.MIDDLE);
                        } else {
                            // קריאה לפונקציה הפנימית שבונה את הדיאלוג
                            openMediaDialog(file);
                        }
                    });
                } catch (Exception ex) {
                    currentUI.access(() -> {
                        spinnerDialog.close();
                        Notification.show("שגיאה בתקשורת עם השרת", 3000, Notification.Position.MIDDLE);
                    });
                }
            }).start();
        });
    }

    // ==========================================
    // בניית ממשק הדיאלוג (הועבר מ-MediaViewerDialog)
    // ==========================================

    private void openMediaDialog(Files attachedFile) 
    {
        Dialog mediaDialog = new Dialog();
        mediaDialog.setWidth("auto");
        mediaDialog.setHeight("auto");

        String mimeType = attachedFile.getMediaType();
        byte[] fileData = attachedFile.getImageData();
        String dataUri = convertBytesToDataUri(fileData, mimeType);

        com.vaadin.flow.component.Component mediaElement = createMediaElement(mimeType, dataUri);
        com.vaadin.flow.component.Component downloadSection = createDownloadLink(attachedFile, mimeType, dataUri);

        VerticalLayout mainLayout = new VerticalLayout(mediaElement, downloadSection);
        mainLayout.setAlignItems(Alignment.CENTER);
        
        if ("Embed".equals(attachedFile.getActionType())) {
            addExtractionTools(mainLayout, mediaElement, fileData, mimeType);
        } else {
            addRegularFileIndication(mainLayout, mediaElement);
        }

        mediaDialog.add(mainLayout);
        mediaDialog.open();
    }

    private String convertBytesToDataUri(byte[] fileData, String mimeType) 
    {
        String base64String = Base64.getEncoder().encodeToString(fileData);
        return "data:" + mimeType + ";base64," + base64String;
    }

    private com.vaadin.flow.component.Component createMediaElement(String mimeType, String dataUri) 
    {
        if (mimeType.startsWith("image/")) {
            com.vaadin.flow.component.html.Image img = new com.vaadin.flow.component.html.Image(dataUri, "Media Image");
            img.getStyle().set("max-width", "80vw").set("max-height", "80vh");
            return img;
        } else {
            com.vaadin.flow.dom.Element audioHtmlElement = new com.vaadin.flow.dom.Element("audio");
            audioHtmlElement.setAttribute("controls", "true");
            audioHtmlElement.setAttribute("src", dataUri);
            
            Span audioContainer = new Span();
            audioContainer.getElement().appendChild(audioHtmlElement);
            return audioContainer;
        }
    }

    private Anchor createDownloadLink(Files attachedFile, String mimeType, String dataUri) 
    {
        String extension = mimeType.equals("audio/wav") ? ".wav" : (mimeType.equals("image/png") ? ".png" : ".jpg");
        String fileName = "media_" + attachedFile.getId() + extension;
        
        Anchor downloadLink = new Anchor(dataUri, "");
        downloadLink.getElement().setAttribute("download", fileName);
        
        Button downloadButton = new Button("הורדת קובץ", com.vaadin.flow.component.icon.VaadinIcon.DOWNLOAD.create());
        downloadButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        downloadLink.add(downloadButton);
        
        return downloadLink;
    }

    private void addExtractionTools(VerticalLayout layout, com.vaadin.flow.component.Component mediaElement, byte[] fileData, String mimeType) 
    {
        ContextMenu rightClickMenu = new ContextMenu(mediaElement);
        rightClickMenu.addItem("חלץ מסר סודי", event -> startExtractionProcess(fileData, mimeType));
        
        Button extractButton = new Button("חלץ מסר סודי", com.vaadin.flow.component.icon.VaadinIcon.UNLOCK.create());
        extractButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        extractButton.addClickListener(event -> startExtractionProcess(fileData, mimeType));
        
        Span instructionHint = new Span("💡 ניתן גם ללחוץ קליק ימני על המדיה לחילוץ");
        instructionHint.getStyle().set("color", "var(--lumo-primary-color)").set("font-size", "12px");
        
        layout.add(extractButton, instructionHint);
    }

    private void addRegularFileIndication(VerticalLayout layout, com.vaadin.flow.component.Component mediaElement) 
    {
        ContextMenu disabledMenu = new ContextMenu(mediaElement);
        disabledMenu.addItem("קובץ גלוי (ללא מסר)", event -> {}).setEnabled(false);
        
        Span infoMessage = new Span("קובץ גלוי (ללא מסר סודי מוחבא)");
        infoMessage.getStyle()
                   .set("color", "var(--lumo-secondary-text-color)")
                   .set("font-size", "14px")
                   .set("margin-top", "10px");
                   
        layout.add(infoMessage);
    }

    private void startExtractionProcess(byte[] fileData, String mimeType) 
    {
        Dialog loadingSpinner = createLoadingSpinner("מפענח נתונים, אנא המתן...");
        loadingSpinner.open();

        UI currentUI = UI.getCurrent();

        steganographyService.extractMsg(fileData, mimeType, (isSuccess, secretMessage) -> 
        {
            currentUI.access(() -> showExtractionResult(isSuccess, secretMessage, loadingSpinner));
        });
    }

    private void showExtractionResult(boolean isSuccess, String secretMessage, Dialog loadingSpinner) 
    {
        loadingSpinner.close(); 
        
        if (isSuccess && secretMessage != null && !secretMessage.isEmpty()) {
            Dialog resultDialog = new Dialog(new H3("המסר הסודי שהתגלה:"), new Span(secretMessage));
            resultDialog.open();
        } else {
            Notification.show("לא נמצא מסר או שהפענוח נכשל.", 4000, Notification.Position.MIDDLE);
        }
    }

    private Dialog createLoadingSpinner(String messageText) 
    {
        Dialog spinnerDialog = new Dialog();
        spinnerDialog.setCloseOnEsc(false);
        spinnerDialog.setCloseOnOutsideClick(false);

        Span textElement = new Span(messageText);
        textElement.getStyle().set("font-weight", "bold").set("font-size", "18px").set("color", "var(--lumo-primary-color)");

        ProgressBar progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setWidth("100%");

        VerticalLayout layout = new VerticalLayout(textElement, progressBar);
        layout.setAlignItems(Alignment.CENTER);
        
        spinnerDialog.add(layout);
        return spinnerDialog;
    }
}