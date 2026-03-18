package yeonatano.steganography_system.services;

import org.springframework.stereotype.Service;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import yeonatano.steganography_system.repositories.StgnoRepository;

@Service
public class StgnoService
{

    public interface EmbedTaskCallback
   {public void onComplete(boolean isSuccess, MemoryBuffer imgFile);}

   public interface ExtractTaskCallback
   {public void onComplete(boolean isSuccess, String msg);}


    private StgnoRepository stgnoRepository;
    private Thread StgnoTask;

    public StgnoService(StgnoRepository stgnoRepository)
    {
        this.stgnoRepository = stgnoRepository;
    }

    //_________________________________________הטמעה_________________________________________

    public void embedMsg(MemoryBuffer imgFile, String msg, EmbedTaskCallback embedTaskCallback)
    {
        StgnoTask =  new Thread(() ->
        {
            if(checkValid(imgFile))
                embedTaskCallback.onComplete(false, null);
            
            else
            {
                embed(imgFile);
                System.out.println("msg " + msg);

                embedTaskCallback.onComplete(true, imgFile);
            }
        });

        StgnoTask.start();
    }

    private void embed(MemoryBuffer imgFile)
    {
        String mimeType = checkType(imgFile);

        MemoryBuffer embedFile = new MemoryBuffer();

        switch (mimeType)
        {
            case "image/jpg":
            case "image/jpeg":
                System.out.println("f5 jpeg");
                embedFile = embedF5(imgFile);
                break;

            case "image/png":
                System.out.println("PVD PNG");
                embedFile = embedPVD(imgFile);
                break;
            
            case "audio/wav":
                embedFile = embedDSSS(imgFile);
        }
    }

    private MemoryBuffer embedDSSS(MemoryBuffer imgFile)
    {
        System.out.println("embedDSSS");
        return imgFile;
    }

    private MemoryBuffer embedPVD(MemoryBuffer imgFile)
    {
        System.out.println("embedPVD");
        return imgFile;
    }

    private MemoryBuffer embedF5(MemoryBuffer imgFile)
    {
    
        System.out.println("embedF5");
        return imgFile;
    }

    //______________________________________פענוח______________________________________________

    public void extractMsg(MemoryBuffer imgFile, ExtractTaskCallback extractTaskCallback)
    {
        System.out.println("Enter ExtractMsg");

        StgnoTask =  new Thread(() -> 
        {
            System.out.println("Enter Thread ExtractMsg");
           

            if(checkValid(imgFile))
            {
                extractTaskCallback.onComplete(false, null);
            }

            else
            {
                String msg = extract(imgFile);
                extractTaskCallback.onComplete(true, msg);
            }
        });

        StgnoTask.start();

        System.out.println("the task extractMsg & Thread is end");

    }

    private String extract(MemoryBuffer imgFile) {
        String mimeType = checkType(imgFile);
        String msg = new String();
        switch (mimeType)
        {
            case "image/jpg":
            case "image/jpeg":
                System.out.println("f5 jpeg");
                msg = extractF5(imgFile);
                break;

            case "image/png":
                System.out.println("PVD PNG");
                msg = extractPVD(imgFile);
                break;
            
            case "audio/wav":
                msg = extractDSSS(imgFile);
        }

        return msg;
        
    }   
   
    private String extractDSSS(MemoryBuffer imgFile)
    {
        return null;

    }

    private String extractPVD(MemoryBuffer imgFile)
    {
        return null;

    }

    private String extractF5(MemoryBuffer imgFile)
    {
        return null;

    }

//_________________________________________פונקציות עזר_________________________________________
    public boolean checkValid(MemoryBuffer imgFile)
    {
        if (!checkType(imgFile).equals("image/jpg") || !checkType(imgFile).equals("image/jpeg"))
        {
            System.out.println("not Valid");
            return false;
        }

        System.out.println("Valid");
        return true;
    }

    public String checkType(MemoryBuffer imgFile)
    {
        return imgFile.getFileData().getMimeType();
    }

}
