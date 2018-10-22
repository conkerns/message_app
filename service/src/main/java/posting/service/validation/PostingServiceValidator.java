package posting.service.validation;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import posting.domain.exception.InvalidRequestException;
import posting.persistence.entity.Post;
import posting.persistence.repository.UserRepository;

@Service
public class PostingServiceValidator {

    private static final String INVALID_FOLLOWING_MESSAGE = "Can't follow yourself, sorry";
    private static final String INVALID_PAGE_NUMBER_MESSAGE_TEMPLATE =
            "Page number too high, max value of the 'page' parameter is [%s]";
    private static final String INVALID_USERNAME_MESSAGE_TEMPLATE = "User [%s] does not exist";

    private final UserRepository userRepository;

    public PostingServiceValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void validateFollowingUsernames(String requestingUsername, String followedUsername) {
        if (requestingUsername.equals(followedUsername)) {
            throw new InvalidRequestException(INVALID_FOLLOWING_MESSAGE);
        }
    }

    public void validatePageNumber(int page, Page<Post> posts) {
        if (posts.getTotalPages() <= page) {
            throw new InvalidRequestException(
                    String.format(INVALID_PAGE_NUMBER_MESSAGE_TEMPLATE, (posts.getTotalPages() - 1)));
        }
    }

    public void validateUserExists(String username) {
        if (!userRepository.existsByUsername(username)) {
            throw unknownUsernameException(username);
        }
    }

    public InvalidRequestException unknownUsernameException(String username) {
        return new InvalidRequestException(String.format(INVALID_USERNAME_MESSAGE_TEMPLATE, username));
    }
}