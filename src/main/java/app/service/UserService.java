package app.service;

import app.model.entity.User;
import app.model.enums.Role;
import app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User registerUser(String username, String password, Role role, String email) {
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .role(role)
                .email(email)
                .build();

        return userRepository.save(user);
    }

    public long countUsers() {
        return userRepository.count();
    }
}
