package app.service;

import app.model.dto.RegistrationDTO;
import app.model.dto.UserCreateDTO;
import app.model.dto.UserEditDTO;
import app.model.dto.UserProfileDTO;
import app.model.entity.TestPurchase;
import app.model.entity.User;
import app.model.enums.Country;
import app.model.enums.Role;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface UserService {

    User register(RegistrationDTO dto);

    long countUsers();

    boolean usernameExists(String username);

    User createUser(UserCreateDTO userCreateDTO);

    UserProfileDTO getProfile(String username);

    void updateProfile(String username, UserProfileDTO dto, String imageUrl);

    void changePassword(User user, String newPassword);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    long countActiveUsers();

    long countInactiveUsers();

    Map<Role, Long> countUsersByRole();

    Map<Country, Long> countUsersByCountry();

    List<TestPurchase> findAllForCurrentUser();

    User findById(UUID id);

    void updateUserFromDto(UUID id, UserEditDTO dto);

    User findByIdWithManagedCustomers(UUID id);

}
