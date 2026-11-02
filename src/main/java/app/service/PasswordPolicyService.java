package app.service;

public interface PasswordPolicyService {

    void validateOrThrow(String password);

    boolean isValid(String password);
}
