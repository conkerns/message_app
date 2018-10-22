package posting;

import org.hibernate.Hibernate;
import org.springframework.transaction.annotation.Transactional;
import posting.persistence.entity.User;
import posting.persistence.repository.UserRepository;

public class InitializingRepository {

    private final UserRepository userRepository;

    public InitializingRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User getInitializedUser(String username) {
        User user = userRepository.findByUsername(username).get();
        Hibernate.initialize(user.getPosts());
        Hibernate.initialize(user.getFollowed());

        return user;
    }
}
