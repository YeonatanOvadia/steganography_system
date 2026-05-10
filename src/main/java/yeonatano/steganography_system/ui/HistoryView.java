package yeonatano.steganography_system.ui;

import org.springframework.security.core.context.SecurityContextHolder;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.security.PermitAll;
import yeonatano.steganography_system.datamodels.Files;
import yeonatano.steganography_system.services.HistoryService;

import java.io.ByteArrayInputStream;

@Route("history")
@PageTitle("היסטוריה | מערכת סטגנוגרפיה")
@PermitAll
public class HistoryView extends VerticalLayout {

    private final HistoryService historyService;
    private Grid<Files> grid = new Grid<>(Files.class, false);

    public HistoryView(HistoryService historyService) {
        this.historyService = historyService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setSpacing(true);

        add(new H1("היסטוריית הקבצים שלי"));

        // 1. הגדרת עמודות הטקסט הרגילות (באמצעות למדה רגילה)
        grid.addColumn(Files -> Files.getTimestamp()).setHeader("תאריך ושעה").setSortable(true);
        grid.addColumn(Files -> Files.getMediaType()).setHeader("סוג קובץ");

        // 2. עמודת תצוגה מקדימה (Preview) - תומכת בתמונות קופצות ובנגן שמע
        grid.addComponentColumn(dbImage -> {
            String mimeType = dbImage.getMediaType();

            // טיפול בתמונות (תמונה ממוזערת + חלון קופץ בלחיצה)
            if (mimeType != null && mimeType.startsWith("image/")) {
                StreamResource resource = new StreamResource(dbImage.getId(),
                        () -> new ByteArrayInputStream(dbImage.getImageData()));

                // שימוש בשם המלא כדי למנוע התנגשות עם מודל הנתונים Image
                com.vaadin.flow.component.html.Image uiImage = new com.vaadin.flow.component.html.Image(resource, "תצוגה מקדימה");
                uiImage.setHeight("80px");
                uiImage.getStyle().set("border-radius", "8px");
                uiImage.getStyle().set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)");

                // פתיחת חלון קופץ (Dialog) בלחיצה
                uiImage.addClickListener(event -> {
                    Dialog dialog = new Dialog();
                    com.vaadin.flow.component.html.Image enlargedImage = new com.vaadin.flow.component.html.Image(resource, "תמונה מוגדלת");
                    enlargedImage.getStyle().set("max-width", "90vw");
                    enlargedImage.getStyle().set("max-height", "90vh");
                    dialog.add(enlargedImage);
                    dialog.open();
                });

                return uiImage;
            } 
            // טיפול בקבצי שמע (נגן אודיו מובנה)
            else if (mimeType != null && mimeType.startsWith("audio/")) {
                StreamResource audioResource = new StreamResource(dbImage.getId(),
                        () -> new ByteArrayInputStream(dbImage.getImageData()));

                com.vaadin.flow.dom.Element audioElement = new com.vaadin.flow.dom.Element("audio");
                audioElement.setAttribute("controls", "true");
                audioElement.setAttribute("src", audioResource);
                audioElement.getStyle().set("width", "180px");
                audioElement.getStyle().set("height", "40px");

                Span audioContainer = new Span();
                audioContainer.getElement().appendChild(audioElement);
                
                return audioContainer;
            }
            
            return new Span();
        }).setHeader("").setWidth("200px");

        // 3. עמודת הורדה (פעולות)
        grid.addComponentColumn(dbImage -> {
            StreamResource resource = new StreamResource(dbImage.getId(),
                    () -> new ByteArrayInputStream(dbImage.getImageData()));
            
            Anchor downloadLink = new Anchor(resource, "הורד קובץ");
            downloadLink.getElement().setAttribute("download", true);
            return downloadLink;
        }).setHeader("פעולות");

        // טעינת הנתונים לפי המשתמש המחובר
        refreshGrid();

        grid.setSizeFull();
        add(grid);
    }

    private void refreshGrid() {
        String currentUser = getCurrentUsername();
        grid.setItems(historyService.getUserHistory(currentUser));
    }

    // שליפת שם המשתמש מתוך הזיכרון של Spring Security
    private String getCurrentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : "Anonymous";
    }
}