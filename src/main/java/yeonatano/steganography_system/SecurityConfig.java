package yeonatano.steganography_system;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import yeonatano.steganography_system.ui.LoginView;

@EnableWebSecurity
@Configuration
public class SecurityConfig extends VaadinWebSecurity {

    @Override
    protected void configure(HttpSecurity http) throws Exception 
    {
        super.configure(http);
        
        setLoginView(http, LoginView.class); 
    }

    @Bean
    public PasswordEncoder passwordEncoder() 
    {
        // הצפנה בסיסית ומהירה מאוד (Work Factor של 4)
        return new BCryptPasswordEncoder(4);
    }
}