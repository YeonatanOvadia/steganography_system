package yeonatano.steganography_system.services;

import org.springframework.stereotype.Service;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import yeonatano.steganography_system.repositories.StgnoRepository;

@Service
public class StgnoService
{

    public interface EmbedTaskCallback
    {
        public void onComplete(boolean isSuccess, byte[] resultBytes);
    }

    public interface ExtractTaskCallback
    {
        public void onComplete(boolean isSuccess, String msg);
    }


    private StgnoRepository stgnoRepository;
    private F5StegoService f5StegoService;
    private DSSSStegnoService dsssStegnoService;
    private PVDStegoService pvdStegoService;



    private Thread StgnoTask;

    public StgnoService(StgnoRepository stgnoRepository, F5StegoService f5StegoService, DSSSStegnoService dsssStegnoService, PVDStegoService pvdStegoService)
    {
        this.stgnoRepository = stgnoRepository;
        this.f5StegoService = f5StegoService;
        this.dsssStegnoService = dsssStegnoService;
        this.pvdStegoService = pvdStegoService;

    }

    //_________________________________________הטמעה_________________________________________

    public void embedMsg(MemoryBuffer imgFile, String msg, EmbedTaskCallback embedTaskCallback)
    {
        StgnoTask =  new Thread(() ->
        {
            if(checkValid(imgFile)) 
            {
                byte[] resultBytes = null;
                try 
                {
                    resultBytes = embed(imgFile , msg);
                } 
                catch (Exception e) 
                {
                    e.printStackTrace();
                }
                embedTaskCallback.onComplete(true, resultBytes);
            }
            else
                embedTaskCallback.onComplete(false, null);
            
        });
        StgnoTask.start();
    }

    private byte[] embed(MemoryBuffer imgFile, String msg) throws Exception
    {
        String mimeType = checkType(imgFile);

        byte[] embedFile = null;

        switch (mimeType)
        {
            case "image/jpg":
            case "image/jpeg":
                System.out.println("f5 jpeg");
                embedFile = embedF5(imgFile, msg);
                break;

            case "image/png":
                System.out.println("PVD PNG");
                embedFile = embedPVD(imgFile, msg);
                break;
            
            case "audio/wav":
                embedFile = embedDSSS(imgFile, msg);
        }
        return embedFile;
    }

    private byte[] embedDSSS(MemoryBuffer File, String msg)
    {
        System.out.println("embedDSSS");

        byte[] result = dsssStegnoService.embed(File, msg);

        return result;
    }

    private byte[] embedPVD(MemoryBuffer imgFile, String msg) throws Exception
    {
        System.out.println("embedPVD");

        byte[] result = pvdStegoService.embed(imgFile, msg);

        return result;
    }

    private byte[] embedF5(MemoryBuffer imgFile, String msg)
    {
        System.out.println("Sending to F5StegoService with message: " + msg);
        
        byte[] resultBytes = f5StegoService.embed(imgFile, msg);     

        return resultBytes;
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
                String msg = null;
                try 
                {
                    msg = extract(imgFile);
                } 
                catch (Exception e) 
                {
                    e.printStackTrace();
                }
                System.out.println(msg);
                extractTaskCallback.onComplete(true, msg);
            }

            else
                extractTaskCallback.onComplete(false, null);
            
            
        });

        StgnoTask.start();

        System.out.println("the task extractMsg & Thread is end");

    }

    private String extract(MemoryBuffer imgFile) throws Exception
    {
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

        String result = dsssStegnoService.extract(imgFile);
        
        return result;

    }

    private String extractPVD(MemoryBuffer imgFile) throws Exception
    {
        System.out.println("Sending to F5StegoService for extraction");
        String result = pvdStegoService.extract(imgFile);
        return result;    }

    private String extractF5(MemoryBuffer imgFile)
    {
        System.out.println("Sending to F5StegoService for extraction");
        String result = f5StegoService.extract(imgFile);
        return result;
    }

//_________________________________________פונקציות עזר_________________________________________
   public boolean checkValid(MemoryBuffer imgFile) {
    String type = checkType(imgFile);
    // בדיקה האם הסוג הוא אחד מהפורמטים הנתמכים
    if (type.equals("image/jpg") || type.equals("image/jpeg") || type.equals("audio/wav") ||type.equals("image/png"))
    {
        System.out.println("Valid type: " + type);
        return true;
    }

    System.out.println("Not valid: " + type);
    return false;
}

    public String checkType(MemoryBuffer imgFile)
    {
        return imgFile.getFileData().getMimeType();
    }

}
