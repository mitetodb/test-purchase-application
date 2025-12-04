package app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableFeignClients(basePackages = "app.client")
@EnableAsync
@SpringBootApplication
public class TestPurchaseApplication {

	public static void main(String[] args) {
		SpringApplication.run(TestPurchaseApplication.class, args);
	}

}
