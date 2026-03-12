package yeonatano.steganography_system.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import yeonatano.steganography_system.datamodels.User;


@Repository
public interface UserRepository extends MongoRepository<User, String>
{
   public List<User> findAllByUsername(String name);

   public List<User> findByUsernameLike(String name);

   public User findOneByUsernameAndPassword(String un, String pw);
}
