package yeonatano.steganography_system.services;

import org.springframework.stereotype.Service;

import com.vaadin.flow.component.upload.receivers.MemoryBuffer;

import yeonatano.steganography_system.repositories.StgnoRepository;

@Service
public class StgnoService
{
    private StgnoRepository stgnoRepository;

    public StgnoService(StgnoRepository stgnoRepository)
    {
        this.stgnoRepository = stgnoRepository;
    }

    //_________________________________________הצפנות__________________________________________

    public boolean embedMsg(MemoryBuffer imgFile, String msg)
    {
        if(!checkValid(imgFile))
            return false;

        embed(imgFile);
        System.out.println("msg " + msg);

        return true;
    }

    private void embed(MemoryBuffer imgFile) {
        String mimeType = checkType(imgFile);
        switch (mimeType) {
            case "image/jpeg":
                System.out.println("f5 jpeg");
                embedF5(imgFile);
                break;

            case "image/png":
                System.out.println("PVD PNG");
                embedPVD(imgFile);
                break;
            
            case "audio/wav":
                embedDSSS(imgFile);
        }
    }

    private void embedDSSS(MemoryBuffer imgFile)
    {

    }

    private void embedPVD(MemoryBuffer imgFile)
    {

    }

    private void embedF5(MemoryBuffer imgFile)
    {

    }

    //____________________________________פענוחים_____________________________________________

    public String extractMsg(MemoryBuffer imgFile)
    {
        return null ;
        
    }

    public boolean extract(MemoryBuffer imgFile)
    {
        return false;
        
    }   
   
    private void extractDSSS(MemoryBuffer imgFile)
    {

    }

    private void extractPVD(MemoryBuffer imgFile)
    {

    }

    private void extractF5(MemoryBuffer imgFile)
    {

    }

//_________________________________________פונקציות עזר_________________________________________
    public boolean checkValid(MemoryBuffer imgFile)
    {
        if (!checkType(imgFile).equals("image/jpeg"))
            return false;

        return true;
    }

    public String checkType(MemoryBuffer imgFile)
    {
        return imgFile.getFileData().getMimeType();
    }

}
