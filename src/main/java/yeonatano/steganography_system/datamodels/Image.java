package yeonatano.steganography_system.datamodels;

import org.bson.types.Binary;
import org.springframework.data.mongodb.core.mapping.Document;

import com.vaadin.flow.component.template.Id;

@Document(collection = "Images")
public class Image
{
    @Id
    private String id = null;
    private Binary imageData;

    public Image(){}

    public Image(String id, Binary imageData)
    {
        this.id = id;
        this.imageData = imageData;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Binary getImageData() {
        return imageData;
    }

    public void setImageData(Binary imageData) {
        this.imageData = imageData;
    }



}
