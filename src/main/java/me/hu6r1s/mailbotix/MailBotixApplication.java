package me.hu6r1s.mailbotix;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "MailBotix API 명세서", description = "OAuth 및 Mail API 명세서", version = "v1"))
public class MailBotixApplication {

	public static void main(String[] args) {
		SpringApplication.run(MailBotixApplication.class, args);
	}

}
