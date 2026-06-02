package yeonatano.steganography_system;

import com.vaadin.flow.component.page.AppShellConfigurator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.vaadin.flow.component.page.Push;

@Push
@SpringBootApplication
public class AppMain implements AppShellConfigurator
{

	public static void main(String[] args) 
	{
		SpringApplication.run(AppMain.class, args);
	}
}
