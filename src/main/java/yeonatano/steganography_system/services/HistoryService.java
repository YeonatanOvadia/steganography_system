package yeonatano.steganography_system.services;

import org.springframework.stereotype.Service;
import yeonatano.steganography_system.datamodels.Files;
import yeonatano.steganography_system.repositories.StgnoRepository;

import java.util.List;

@Service
public class HistoryService 
{

    private final StgnoRepository stgnoRepository;

    public HistoryService(StgnoRepository stgnoRepository) 
    {
        this.stgnoRepository = stgnoRepository;
    }

    public List<Files> getUserHistory(String userId) 
    {
        return stgnoRepository.findAllByUserId(userId);
    }
}