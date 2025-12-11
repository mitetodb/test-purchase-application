package app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableFeignClients(basePackages = "app.client")
@EnableAsync
@EnableScheduling
@EnableCaching
@SpringBootApplication
public class TestPurchaseApplication {

	public static void main(String[] args) {
		SpringApplication.run(TestPurchaseApplication.class, args);
	}

}
