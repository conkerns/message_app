package posting.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import posting.domain.model.PostDto;
import posting.persistence.entity.Post;
import posting.persistence.entity.User;
import posting.persistence.repository.PostRepository;
import posting.persistence.repository.UserRepository;
import posting.service.validation.PostingServiceValidator;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class PostingService implements posting.domain.service.PostingService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostingServiceValidator validator;

    public PostingService(UserRepository userRepository,
                          PostRepository postRepository,
                          PostingServiceValidator validator) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.validator = validator;
    }

    @Override
    @Transactional
    public void newPost(String username, String postContent) {
        User user = userRepository.findByUsername(username).orElseGet(getUser(username));
        Post post = new Post();
        post.setContent(postContent);
        user.getPosts().add(post);
        post.setUser(user);
        userRepository.save(user);
    }

    private Supplier<User> getUser(String username) {
        return () -> buildUser(username);
    }

    private User buildUser(String username) {
        User user = new User();
        user.setUsername(username);

        return user;
    }

    @Override
    @Transactional
    public void follow(String requestingUsername, String followedUsername) {
        validator.validateFollowingUsernames(requestingUsername, followedUsername);
        User requestingUser = findExistingUser(requestingUsername);
        User followedUser = findExistingUser(followedUsername);
        requestingUser.getFollowed().add(followedUser);
    }

    private User findExistingUser(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> validator.unknownUsernameException(username));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostDto> getCompleteWall(String username) {
        return findAllPosts(username, postRepository::findByUsernameOrderByCreatedDateDescending);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostDto> getWall(String username, int page, int size) {
        return findPosts(username, page, size, postRepository::findByUsernameOrderByCreatedDateDescending);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostDto> getCompleteTimeline(String username) {
        return findAllPosts(username, postRepository::findByFollowedOrderByCreatedDateDescending);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostDto> getTimeline(String username, int page, int size) {
        return findPosts(username, page, size, postRepository::findByFollowedOrderByCreatedDateDescending);
    }

    private List<PostDto> findAllPosts(String username, Function<String, List<Post>> repositoryCall) {
        validator.validateUserExists(username);
        List<Post> posts = repositoryCall.apply(username);

        return posts.stream()
                .map(toDomainModel())
                .collect(Collectors.toList());
    }

    private List<PostDto> findPosts(String username, int page, int size,
                                    BiFunction<String, Pageable, Page<Post>> repositoryCall) {
        validator.validateUserExists(username);
        Pageable pageRequest = PageRequest.of(page, size);
        Page<Post> posts = repositoryCall.apply(username, pageRequest);
        validator.validatePageNumber(page, posts);

        return posts.map(toDomainModel()).getContent();
    }

    private Function<Post, PostDto> toDomainModel() {
        return post -> new PostDto(post.getUser().getUsername(), post.getContent(), post.getCreatedDate());
    }
}