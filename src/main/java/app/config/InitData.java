package app.config;

import app.model.enums.Role;
import app.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/*
@Component
@RequiredArgsConstructor
public class InitData implements CommandLineRunner {

    private final UserService userService;

    @Override
    public void run(String... args) {

        if (userService.countUsers() == 0) {
            userService.register(
                    "admin",
                    "123123",
                    Role.ADMIN,
                    "admin@example.com"
            );

            System.out.println("Admin account created.");
        }
    }
}
*/
