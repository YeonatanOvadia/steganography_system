package yeonatano.steganography_system.services;

import java.util.ArrayList;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import yeonatano.steganography_system.datamodels.User;
import yeonatano.steganography_system.repositories.UserRepository;

@Service
public class UserService implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) 
    {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean addUserToDB(User user)
    {
        if (userRepository.existsById(user.getUsername()))
            return false;

        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        
        userRepository.insert(user);
        return true;
    }

    public ArrayList<User> getAllUsers() 
    {
        return (ArrayList<User>)userRepository.findAll();
    }


    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // מציאת המשתמש במסד הנתונים בעזרת הפונקציה החדשה בריפוזיטורי (לפי השם ולא לפי ה-ID המובנה של מונגו)
        User myUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // המרה לפורמט של Spring Security
        return org.springframework.security.core.userdetails.User.builder()
                .username(myUser.getUsername())
                .password(myUser.getPassword()) // זו הסיסמה המוצפנת שהוצאנו מהדאטה-בייס
                .roles("USER")
                .build();
    }
    
}