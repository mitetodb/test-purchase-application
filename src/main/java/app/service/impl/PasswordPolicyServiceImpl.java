package app.service.impl;

import app.config.PasswordPolicyProperties;
import app.service.PasswordPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordPolicyServiceImpl implements PasswordPolicyService {

    private final PasswordPolicyProperties properties;

    @Override
    public void validateOrThrow(String password) {
        if (!isValid(password)) {
            throw new IllegalArgumentException("password.policy.tooShort");
        }
    }

    @Override
    public boolean isValid(String password) {
        return password != null && password.length() >= properties.minLength();
    }
}
