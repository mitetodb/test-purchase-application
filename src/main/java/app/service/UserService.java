package app.service;

import app.model.dto.RegistrationDTO;
import app.model.dto.UserCreateDTO;
import app.model.dto.UserProfileDTO;
import app.model.entity.User;
import app.model.enums.Country;
import app.model.enums.Role;
import app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User register(RegistrationDTO dto) {

        if (usernameExists(dto.getUsername())) {
            throw new IllegalArgumentException("Username already taken.");
        }

        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        User user = User.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(dto.getRole())
                .email(dto.getEmail())
                .country(dto.getCountry())
                .active(true)
                .build();

        return userRepository.save(user);
    }

    public long countUsers() {
        return userRepository.count();
    }

    public boolean usernameExists(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    public User createUser(UserCreateDTO userCreateDTO) {
        if (usernameExists(userCreateDTO.getUsername())) {
            throw new IllegalArgumentException("Username already taken.");
        }

        User user = User.builder()
                .username(userCreateDTO.getUsername())
                .password(passwordEncoder.encode(userCreateDTO.getPassword()))
                .email(userCreateDTO.getEmail())
                .role(userCreateDTO.getRole())
                .country(userCreateDTO.getCountry())
                .active(userCreateDTO.isActive())
                .build();

        return userRepository.save(user);
    }

    public UserProfileDTO getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserProfileDTO dto = new UserProfileDTO();
        dto.setUsername(user.getUsername());
        dto.setRole(user.getRole());
        dto.setCreatedOn(user.getCreatedOn());
        dto.setEmail(user.getEmail());
        dto.setImageUrl(user.getImageUrl());
        dto.setCountry(user.getCountry());

        return dto;
    }

    public void updateProfile(String username, UserProfileDTO dto, String imageUrl) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setEmail(dto.getEmail());
        user.setCountry(dto.getCountry());

        if (imageUrl != null) {
            user.setImageUrl(imageUrl);
        }

        userRepository.save(user);
    }

}
