package app.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserSessionInvalidationService {

    private final SessionRegistry sessionRegistry;

    public void invalidateAllSessions(String username) {
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (!principalMatchesUsername(principal, username)) {
                continue;
            }
            for (SessionInformation session : sessionRegistry.getAllSessions(principal, false)) {
                session.expireNow();
            }
        }
    }

    private static boolean principalMatchesUsername(Object principal, String username) {
        if (principal instanceof UserDetails userDetails) {
            return username.equals(userDetails.getUsername());
        }
        return username.equals(String.valueOf(principal));
    }
}
