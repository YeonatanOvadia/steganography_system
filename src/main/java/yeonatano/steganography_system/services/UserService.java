package yeonatano.steganography_system.services;

import java.util.ArrayList;

import org.springframework.stereotype.Service;

import yeonatano.steganography_system.datamodels.User;
import yeonatano.steganography_system.repositories.UserRepository;

@Service
public class UserService {

    private UserRepository userRepository;

    public UserService(UserRepository userRepository)
   {
      this.userRepository = userRepository;
   }


    public boolean addUserToDB(User user)
    {
        if (userRepository.existsById(user.getUsername()))
            return false;
        
        userRepository.insert(user);
        return true;
    }

    public ArrayList<User> getAllUsers() {

        return (ArrayList<User>)userRepository.findAll();
    }


    
}