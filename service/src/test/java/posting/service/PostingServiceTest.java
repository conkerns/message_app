package posting.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import posting.domain.exception.InvalidRequestException;
import posting.domain.model.PostDto;
import posting.persistence.entity.Post;
import posting.persistence.entity.User;
import posting.persistence.repository.PostRepository;
import posting.persistence.repository.UserRepository;
import posting.service.validation.PostingServiceValidator;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PostingServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostingServiceValidator validator;

    @InjectMocks
    private PostingService postingService;

    @Test
    public void testNewPostNewUser() {
        String username = "testUser";
        String postContent = "postContent";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        postingService.newPost(username, postContent);

        verify(userRepository).save(userCaptor.capture());
        User user = userCaptor.getValue();
        User exampleUser = new User();
        exampleUser.setUsername(username);
        assertThat(user)
                .isEqualToIgnoringGivenFields(exampleUser, "posts");
        assertThat(user.getPosts())
                .extracting(Post::getContent)
                .containsExactly(postContent);
    }

    @Test
    public void testNewPostExistingUserWithExistingPosts() {
        String username = "testUser";
        String postContent = "postContent";
        String existingPostContent = "existingPostContent";
        Long existingUserId = 1L;

        User existingUser = new User();
        existingUser.setId(existingUserId);
        existingUser.setUsername(username);
        Post existingPost = new Post();
        existingPost.setContent(existingPostContent);
        existingPost.setUser(existingUser);
        existingUser.getPosts().add(existingPost);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(existingUser));
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        postingService.newPost(username, postContent);

        verify(userRepository).save(userCaptor.capture());
        User user = userCaptor.getValue();
        assertThat(user)
                .isEqualToIgnoringGivenFields(existingUser, "posts");
        assertThat(user.getPosts())
                .contains(existingPost)
                .flatExtracting(Post::getContent, Post::getUser)
                .containsExactlyInAnyOrder(postContent, existingUser, existingPostContent, existingUser);
    }

    @Test
    public void testFollow() {
        String requestingUsername = "user1";
        String followedUsername = "user2";

        User existingFollowedUser = new User();
        User requestingUser = new User();
        requestingUser.getFollowed().add(existingFollowedUser);
        User followedUser = new User();

        when(userRepository.findByUsername(requestingUsername)).thenReturn(Optional.of(requestingUser));
        when(userRepository.findByUsername(followedUsername)).thenReturn(Optional.of(followedUser));

        postingService.follow(requestingUsername, followedUsername);

        verify(validator).validateFollowingUsernames(requestingUsername, followedUsername);
        assertThat(requestingUser.getFollowed())
                .containsExactlyInAnyOrder(existingFollowedUser, followedUser);
    }

    @Test
    public void testFollowRequestingUserNotFound() {
        String requestingUsername = "user1";
        String followedUsername = "user2";

        InvalidRequestException thrownException = new InvalidRequestException("test");
        when(userRepository.findByUsername(requestingUsername)).thenReturn(Optional.empty());
        when(validator.unknownUsernameException(requestingUsername)).thenReturn(thrownException);

        Throwable throwable = catchThrowable(() -> postingService.follow(requestingUsername, followedUsername));

        assertThat(throwable).isSameAs(thrownException);
    }

    @Test
    public void testFollowFollowedUserNotFound() {
        String requestingUsername = "user1";
        String followedUsername = "user2";
        User requestingUser = new User();

        InvalidRequestException thrownException = new InvalidRequestException("test");
        when(userRepository.findByUsername(requestingUsername)).thenReturn(Optional.of(requestingUser));
        when(userRepository.findByUsername(followedUsername)).thenReturn(Optional.empty());
        when(validator.unknownUsernameException(followedUsername)).thenReturn(thrownException);

        Throwable throwable = catchThrowable(() -> postingService.follow(requestingUsername, followedUsername));

        assertThat(throwable).isSameAs(thrownException);
    }

    @Test
    public void getCompleteWall() {
        LocalDateTime createdDate1 = LocalDateTime.now();
        LocalDateTime createdDate2 = createdDate1.plusMinutes(5);
        String username = "testUser";
        String testContent1 = "testContent1";
        String testContent2 = "testContent2";

        Post post1 = createPost(testContent1, createdDate1, username);
        Post post2 = createPost(testContent2, createdDate2, username);

        when(postRepository.findByUsernameOrderByCreatedDateDescending(username))
                .thenReturn(Arrays.asList(post1, post2));

        List<PostDto> wall = postingService.getCompleteWall(username);

        verify(validator).validateUserExists(username);
        testPostList(wall, testContent1, createdDate1, username, testContent2, createdDate2, username);
    }

    @Test
    public void getCompleteWallNoPosts() {
        String username = "testUser";
        when(postRepository.findByUsernameOrderByCreatedDateDescending(username)).thenReturn(Collections.emptyList());

        List<PostDto> wall = postingService.getCompleteWall(username);

        verify(validator).validateUserExists(username);
        assertThat(wall).isEmpty();
    }

    @Test
    public void getCompleteTimeline() {
        LocalDateTime createdDate1 = LocalDateTime.now();
        LocalDateTime createdDate2 = createdDate1.plusMinutes(5);
        String username = "testUser";
        String username1 = "testUser1";
        String username2 = "testUser2";
        String testContent1 = "testContent1";
        String testContent2 = "testContent2";

        Post post1 = createPost(testContent1, createdDate1, username1);
        Post post2 = createPost(testContent2, createdDate2, username2);

        when(postRepository.findByFollowedOrderByCreatedDateDescending(username))
                .thenReturn(Arrays.asList(post1, post2));

        List<PostDto> timeline = postingService.getCompleteTimeline(username);

        verify(validator).validateUserExists(username);
        testPostList(timeline, testContent1, createdDate1, username1, testContent2, createdDate2, username2);
    }

    @Test
    public void getCompleteTimelineNoPosts() {
        String username = "testUser";
        when(postRepository.findByFollowedOrderByCreatedDateDescending(username)).thenReturn(Collections.emptyList());

        List<PostDto> timeline = postingService.getCompleteTimeline(username);

        verify(validator).validateUserExists(username);
        assertThat(timeline).isEmpty();
    }

    @Test
    public void getWall() {
        LocalDateTime createdDate1 = LocalDateTime.now();
        LocalDateTime createdDate2 = createdDate1.plusMinutes(5);
        String username = "testUser";
        int page = 1;
        int size = 50;
        String testContent1 = "testContent1";
        String testContent2 = "testContent2";

        Post post1 = createPost(testContent1, createdDate1, username);
        Post post2 = createPost(testContent2, createdDate2, username);

        Page<Post> posts = new PageImpl<>(Arrays.asList(post1, post2), PageRequest.of(page, size), 200);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(postRepository.findByUsernameOrderByCreatedDateDescending(eq(username), pageableCaptor.capture()))
                .thenReturn(posts);

        List<PostDto> wall = postingService.getWall(username, page, size);

        verify(validator).validateUserExists(username);
        verify(validator).validatePageNumber(page, posts);
        testPageable(pageableCaptor.getValue(), page, size);
        testPostList(wall, testContent1, createdDate1, username, testContent2, createdDate2, username);
    }

    @Test
    public void getWallNoPosts() {
        String username = "testUser";
        int page = 1;
        int size = 50;
        Page<Post> posts = Page.empty();
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(postRepository.findByUsernameOrderByCreatedDateDescending(eq(username), pageableCaptor.capture()))
                .thenReturn(posts);

        List<PostDto> wall = postingService.getWall(username, page, size);

        verify(validator).validateUserExists(username);
        verify(validator).validatePageNumber(page, posts);
        testPageable(pageableCaptor.getValue(), page, size);
        assertThat(wall).isEmpty();
    }

    @Test
    public void getTimeline() {
        LocalDateTime createdDate1 = LocalDateTime.now();
        LocalDateTime createdDate2 = createdDate1.plusMinutes(5);
        String username = "testUser";
        String username1 = "testUser1";
        String username2 = "testUser2";
        int page = 1;
        int size = 50;
        String testContent1 = "testContent1";
        String testContent2 = "testContent2";

        Post post1 = createPost(testContent1, createdDate1, username1);
        Post post2 = createPost(testContent2, createdDate2, username2);

        Page<Post> posts = new PageImpl<>(Arrays.asList(post1, post2), PageRequest.of(page, size), 200);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(postRepository.findByFollowedOrderByCreatedDateDescending(eq(username), pageableCaptor.capture()))
                .thenReturn(posts);

        List<PostDto> timeline = postingService.getTimeline(username, page, size);

        verify(validator).validateUserExists(username);
        verify(validator).validatePageNumber(page, posts);
        testPageable(pageableCaptor.getValue(), page, size);
        testPostList(timeline, testContent1, createdDate1, username1, testContent2, createdDate2, username2);
    }

    @Test
    public void getTimelineNoPosts() {
        String username = "testUser";
        int page = 1;
        int size = 50;
        Page<Post> posts = Page.empty();
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(postRepository.findByFollowedOrderByCreatedDateDescending(eq(username), pageableCaptor.capture()))
                .thenReturn(posts);

        List<PostDto> timeline = postingService.getTimeline(username, page, size);

        verify(validator).validateUserExists(username);
        verify(validator).validatePageNumber(page, posts);
        testPageable(pageableCaptor.getValue(), page, size);
        assertThat(timeline).isEmpty();
    }

    private void testPostList(List<PostDto> posts, Object... expectedValues) {
        assertThat(posts)
                .flatExtracting("content", "createdDate", "username")
                .containsExactly(expectedValues);
    }

    private void testPageable(Pageable pageable, int expectedPage, int expectedSize) {
        assertThat(pageable)
                .extracting(Pageable::getPageNumber, Pageable::getPageSize)
                .containsExactly(expectedPage, expectedSize);
    }

    private Post createPost(String content, LocalDateTime createdDate, String username) {
        User user = new User();
        user.setUsername(username);
        Post post = new Post();
        post.setContent(content);
        post.setCreatedDate(createdDate);
        post.setUser(user);
        user.getPosts().add(post);

        return post;
    }
}