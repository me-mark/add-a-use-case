package team.projectpulse.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import team.projectpulse.user.UserRepository;

@Component
public class ActiveAccountJwtValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2Error INVALID_ACCOUNT =
            new OAuth2Error("invalid_token", "The token does not identify an active account.", null);

    private final UserRepository users;

    public ActiveAccountJwtValidator(UserRepository users) {
        this.users = users;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        Object idClaim = token.getClaims().get("userId");
        Object subjectClaim = token.getClaims().get("sub");
        if (!(idClaim instanceof Integer || idClaim instanceof Long)
                || !(subjectClaim instanceof String username) || username.isBlank()) {
            return OAuth2TokenValidatorResult.failure(INVALID_ACCOUNT);
        }
        long id = ((Number) idClaim).longValue();
        if (id < 1 || id > Integer.MAX_VALUE) {
            return OAuth2TokenValidatorResult.failure(INVALID_ACCOUNT);
        }

        return users.findById((int) id)
                .filter(user -> user.isEnabled() && username.equals(user.getUsername()))
                .map(user -> OAuth2TokenValidatorResult.success())
                .orElseGet(() -> OAuth2TokenValidatorResult.failure(INVALID_ACCOUNT));
    }
}
