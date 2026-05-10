package yeonatano.steganography_system.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import yeonatano.steganography_system.datamodels.Files;

@Repository
public interface StgnoRepository extends MongoRepository<Files, String>
{
    List<Files> findAllByUserId(String userId);
}
