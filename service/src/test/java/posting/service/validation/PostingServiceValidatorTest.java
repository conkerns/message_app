package posting.service.validation;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import posting.domain.exception.InvalidRequestException;
import posting.persistence.entity.Post;
import posting.persistence.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PostingServiceValidatorTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PostingServiceValidator validator;

    @Test
    public void testValidateFollowingUsernamesInvalid() {
        String requestingUsername = "user";
        String followedUsername = "user";

        Throwable thrownException = catchThrowable(
                () -> validator.validateFollowingUsernames(requestingUsername, followedUsername));

        assertThat(thrownException)
                .isExactlyInstanceOf(InvalidRequestException.class)
                .hasMessage("Can't follow yourself, sorry");
    }

    @Test
    public void testValidateFollowingUsernamesValid() {
        String requestingUsername = "user1";
        String followedUsername = "user2";

        validator.validateFollowingUsernames(requestingUsername, followedUsername);
    }

    @Test
    public void testValidatePageNumberTooLarge() {
        int pageNumber = 2;
        Page<Post> mockPage = (Page<Post>) mock(Page.class);
        when(mockPage.getTotalPages()).thenReturn(2);

        Throwable thrownException = catchThrowable(() -> validator.validatePageNumber(pageNumber, mockPage));

        assertThat(thrownException)
                .isExactlyInstanceOf(InvalidRequestException.class)
                .hasMessage("Page number too high, max value of the 'page' parameter is [1]");
    }

    @Test
    public void testValidatePageNumberIsMax() {
        int pageNumber = 1;
        Page<Post> mockPage = (Page<Post>) mock(Page.class);
        when(mockPage.getTotalPages()).thenReturn(2);

        validator.validatePageNumber(pageNumber, mockPage);
    }

    @Test
    public void testValidatePageNumberIsSmaller() {
        int pageNumber = 0;
        Page<Post> mockPage = (Page<Post>) mock(Page.class);
        when(mockPage.getTotalPages()).thenReturn(2);

        validator.validatePageNumber(pageNumber, mockPage);
    }

    @Test
    public void testValidateUserExistsWhenNot() {
        String username = "testUser";
        when(userRepository.existsByUsername(username)).thenReturn(Boolean.FALSE);

        Throwable thrownException = catchThrowable(() -> validator.validateUserExists(username));

        assertThat(thrownException)
                .isExactlyInstanceOf(InvalidRequestException.class)
                .hasMessage("User [testUser] does not exist");
    }

    @Test
    public void testValidateUserExistsWhenDoes() {
        String username = "testUser";
        when(userRepository.existsByUsername(username)).thenReturn(Boolean.TRUE);

        validator.validateUserExists(username);
    }

    @Test
    public void testUnknownUsernameException() {
        String username = "testUser";

        InvalidRequestException exception = validator.unknownUsernameException(username);

        assertThat(exception)
                .isExactlyInstanceOf(InvalidRequestException.class)
                .hasMessage("User [testUser] does not exist");
    }
}