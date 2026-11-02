package app.security;

import app.model.entity.User;
import app.repository.UserRepository;
import app.service.LoginSecurityService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Verifies passwords before revealing account lockout (constant-time path for locked users).
 */
public class LoginAwareDaoAuthenticationProvider extends DaoAuthenticationProvider {

    private final UserRepository userRepository;
    private final LoginSecurityService loginSecurityService;

    public LoginAwareDaoAuthenticationProvider(
            PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            LoginSecurityService loginSecurityService) {
        this.userRepository = userRepository;
        this.loginSecurityService = loginSecurityService;
        setPasswordEncoder(passwordEncoder);
        setHideUserNotFoundExceptions(true);
        setPreAuthenticationChecks(user -> {
            if (!user.isEnabled()) {
                throw new DisabledException("User is disabled");
            }
        });
    }

    @Override
    protected void additionalAuthenticationChecks(
            UserDetails userDetails,
            UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {

        if (authentication.getCredentials() == null) {
            throw new BadCredentialsException("Bad credentials");
        }
        String presentedPassword = authentication.getCredentials().toString();
        if (!getPasswordEncoder().matches(presentedPassword, userDetails.getPassword())) {
            throw new BadCredentialsException("Bad credentials");
        }

        User user = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        if (user != null && loginSecurityService.isCurrentlyLocked(user)) {
            throw new BadCredentialsException("Bad credentials");
        }
    }
}
