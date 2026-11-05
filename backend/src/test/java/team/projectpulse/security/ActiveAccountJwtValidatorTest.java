package team.projectpulse.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jwt.Jwt;
import team.projectpulse.student.Student;
import team.projectpulse.user.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ActiveAccountJwtValidatorTest {
    private static final int STUDENT_ID = 17;
    private static final String USERNAME = "student@example.edu";

    private UserRepository users;
    private ActiveAccountJwtValidator validator;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        validator = new ActiveAccountJwtValidator(users);
    }

    @Test
    void alreadyIssuedTokenStopsWorkingWhenAccountIsDisabled() {
        Student student = new Student();
        student.setUsername(USERNAME);
        student.setEnabled(true);
        when(users.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        Jwt token = token((long) STUDENT_ID, USERNAME);

        assertThat(validator.validate(token).hasErrors()).isFalse();

        student.setEnabled(false);
        assertThat(validator.validate(token).hasErrors()).isTrue();
    }

    @Test
    void rejectsDeletedAccount() {
        when(users.findById(STUDENT_ID)).thenReturn(Optional.empty());

        assertThat(validator.validate(token(STUDENT_ID, USERNAME)).hasErrors()).isTrue();
    }

    @Test
    void rejectsTokenWhoseSubjectDoesNotMatchAccount() {
        Student student = new Student();
        student.setUsername(USERNAME);
        student.setEnabled(true);
        when(users.findById(STUDENT_ID)).thenReturn(Optional.of(student));

        assertThat(validator.validate(token(STUDENT_ID, "another@example.edu")).hasErrors()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, 2147483648L})
    void rejectsOutOfRangeAccountIds(long id) {
        assertThat(validator.validate(token(id, USERNAME)).hasErrors()).isTrue();
        verifyNoInteractions(users);
    }

    @Test
    void rejectsNonNumericAccountId() {
        assertThat(validator.validate(token("17", USERNAME)).hasErrors()).isTrue();
        verifyNoInteractions(users);
    }

    @Test
    void rejectsMissingAccountId() {
        Jwt token = Jwt.withTokenValue("token").header("alg", "RS256").subject(USERNAME).build();

        assertThat(validator.validate(token).hasErrors()).isTrue();
        verifyNoInteractions(users);
    }

    private Jwt token(Object accountId, String subject) {
        return Jwt.withTokenValue("token").header("alg", "RS256")
                .claim("userId", accountId).subject(subject).build();
    }
}
