package yeonatano.steganography_system.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import yeonatano.steganography_system.datamodels.Image;

@Repository
public interface StgnoRepository extends MongoRepository<Image, String>
{
    
}
