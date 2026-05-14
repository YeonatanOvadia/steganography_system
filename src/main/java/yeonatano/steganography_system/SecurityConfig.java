package yeonatano.steganography_system;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig 
{

    /**
     * כאן אנחנו מגדירים את ה-Bean ש-Spring חיפש.
     * זה אומר ל-Spring: "בכל פעם שמישהו (כמו UserService) מבקש PasswordEncoder,
     * תן לו עותק של BCryptPasswordEncoder".
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}