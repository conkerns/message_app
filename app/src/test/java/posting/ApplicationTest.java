package posting;


import com.fasterxml.jackson.core.type.TypeReference;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import posting.PostingApiRestTemplate.EnhancedResultActions;
import posting.domain.model.PostDto;
import posting.persistence.entity.Post;
import posting.persistence.entity.User;
import posting.persistence.repository.PostRepository;
import posting.persistence.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ApplicationTest {

    @Autowired
    private PostingApiRestTemplate postingApiRestTemplate;

    @Autowired
    private InitializingRepository initializingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Test
    public void testNewPostNewUser() {
        //given
        String username = "testUser";
        String postContent = "post content";
        LocalDateTime saveDate = LocalDateTime.now();

        //when
        EnhancedResultActions resultActions = postingApiRestTemplate.newPost(username, postContent);

        //then
        resultActions.expectIsCreated();

        long usersCount = userRepository.count();
        long postsCount = postRepository.count();
        assertThat(usersCount).isOne();
        assertThat(postsCount).isOne();

        User user = initializingRepository.getInitializedUser(username);
        assertThat(user.getId()).isNotNull();
        assertThat(user.getUsername()).isEqualTo(username);
        assertThat(user.getPosts())
                .hasSize(1)
                .extracting(Post::getContent)
                .containsExactly(postContent);
        assertThat(user.getPosts())
                .extracting(Post::getId)
                .doesNotContainNull();
        assertThat(user.getPosts())
                .extracting(Post::getCreatedDate)
                .allMatch(saveDate::isBefore);
    }

    @Test
    public void testNewPostExistingUser() {
        //given
        String username = "testUser";
        String postContent1 = "post content 1";
        String postContent2 = "post content 2";
        LocalDateTime firstSaveDate = LocalDateTime.now();
        postingApiRestTemplate.newPost(username, postContent1);
        User existingUser = initializingRepository.getInitializedUser(username);
        LocalDateTime secondSaveDate = LocalDateTime.now();

        //when
        EnhancedResultActions resultActions = postingApiRestTemplate.newPost(username, postContent2);

        //then
        resultActions.expectIsCreated();

        long usersCount = userRepository.count();
        long postsCount = postRepository.count();
        assertThat(usersCount).isOne();
        assertThat(postsCount).isEqualTo(2);

        User user = initializingRepository.getInitializedUser(username);
        assertThat(user)
                .extracting(User::getId, User::getUsername)
                .containsExactly(existingUser.getId(), existingUser.getUsername());
        assertThat(user.getPosts())
                .hasSize(2)
                .extracting(Post::getContent)
                .containsExactlyInAnyOrder(postContent1, postContent2);
        assertThat(user.getPosts())
                .extracting(Post::getId)
                .doesNotContainNull()
                .contains(existingUser.getPosts().get(0).getId());
        assertThat(user.getPosts())
                .extracting(Post::getCreatedDate)
                .anyMatch(firstSaveDate::isBefore)
                .anyMatch(secondSaveDate::isBefore);
    }

    @Test
    public void testNewPostNullContent() {
        //given
        String username = "testUser";

        //when
        EnhancedResultActions resultActions = postingApiRestTemplate.newPost(username, null);

        //then
        resultActions.expectIsBadRequest();

        long usersCount = userRepository.count();
        long postsCount = postRepository.count();
        assertThat(usersCount).isZero();
        assertThat(postsCount).isZero();
    }

    @Test
    public void testNewPostTooLargeContent() {
        //given
        String username = "testUser";
        String postContent = RandomString.make(141);

        //when
        EnhancedResultActions resultActions = postingApiRestTemplate.newPost(username, postContent);

        //then
        resultActions.expectIsBadRequest();

        long usersCount = userRepository.count();
        long postsCount = postRepository.count();
        assertThat(usersCount).isZero();
        assertThat(postsCount).isZero();
    }

    @Test
    public void testFollow() {
        //given
        String username1 = "testUser1";
        String username2 = "testUser2";
        String postContent1 = "post content 1";
        String postContent2 = "post content 2";
        postingApiRestTemplate.newPost(username1, postContent1);
        postingApiRestTemplate.newPost(username2, postContent2);
        User existingUser2 = initializingRepository.getInitializedUser(username2);

        //when
        EnhancedResultActions resultActions = postingApiRestTemplate.follow(username1, username2);

        //then
        resultActions.expectIsOk();

        User user1 = initializingRepository.getInitializedUser(username1);
        assertThat(user1.getFollowed())
                .flatExtracting(User::getId, User::getUsername)
                .containsExactly(existingUser2.getId(), existingUser2.getUsername());
        User user2 = initializingRepository.getInitializedUser(username2);
        assertThat(user2.getFollowed()).isEmpty();
    }

    @Test
    public void testFollowRequestingUserNotPresent() {
        //given
        String username1 = "testUser1";
        String username2 = "testUser2";
        String postContent = "post content";
        postingApiRestTemplate.newPost(username2, postContent);

        //when
        EnhancedResultActions resultActions = postingApiRestTemplate.follow(username1, username2);

        //then
        resultActions.expectIsBadRequest();

        User user2 = initializingRepository.getInitializedUser(username2);
        assertThat(user2.getFollowed()).isEmpty();
    }

    @Test
    public void testFollowFollowedUserNotPresent() {
        //given
        String username1 = "testUser1";
        String username2 = "testUser2";
        String postContent = "post content";
        postingApiRestTemplate.newPost(username1, postContent);

        //when
        EnhancedResultActions resultActions = postingApiRestTemplate.follow(username1, username2);

        //then
        resultActions.expectIsBadRequest();

        User user1 = initializingRepository.getInitializedUser(username1);
        assertThat(user1.getFollowed()).isEmpty();
    }

    @Test
    public void testGetCompleteWall() {
        //given
        String username = "testUser";
        String followedUsername = "followedUser";
        String postContent1 = "post content 1";
        String postContent2 = "post content 2";
        String postContent3 = "post content 3";
        String followedPostContent1 = "followed post content 1";
        String followedPostContent2 = "followed post content 2";

        postingApiRestTemplate.newPost(username, postContent3);
        postingApiRestTemplate.newPost(username, postContent1);
        postingApiRestTemplate.newPost(username, postContent2);
        postingApiRestTemplate.newPost(followedUsername, followedPostContent1);
        postingApiRestTemplate.newPost(followedUsername, followedPostContent2);
        postingApiRestTemplate.follow(username, followedUsername);

        //when
        EnhancedResultActions resultActions = postingApiRestTemplate.getCompleteWall(username);

        //then
        List<PostDto> posts = resultActions.expectIsOk().andGetResponseBody(new TypeReference<List<PostDto>>() {
        });

        User savedUser = initializingRepository.getInitializedUser(username);
        savedUser.getPosts().sort((p1, p2) -> p2.getCreatedDate().compareTo(p1.getCreatedDate()));
        Post post2 = savedUser.getPosts().get(0);
        Post post1 = savedUser.getPosts().get(1);
        Post post3 = savedUser.getPosts().get(2);

        User followedUser = initializingRepository.getInitializedUser(followedUsername);
        Post followedPost1 = followedUser.getPosts().get(0);
        Post followedPost2 = followedUser.getPosts().get(1);

        assertThat(posts)
                .flatExtracting(PostDto::getUsername, PostDto::getContent, PostDto::getCreatedDate)
                .containsExactly(
                        username, post2.getContent(), post2.getCreatedDate(),
                        username, post1.getContent(), post1.getCreatedDate(),
                        username, post3.getContent(), post3.getCreatedDate())
                .doesNotContain(
                        followedUsername, followedPost1.getContent(), followedPost1.getCreatedDate(),
                        followedUsername, followedPost2.getContent(), followedPost2.getCreatedDate());
    }

    @Test
    public void testGetCompleteWallUserNotPresent() {
        //given
        String username = "testUser";

        //when
        EnhancedResultActions resultActions = postingApiRestTemplate.getCompleteWall(username);

        //then
        resultActions.expectIsBadRequest();
    }

    @Test
    public void testGetCompleteTimeline() {
        //given
        String username = "testUser";
        String followedUsername1 = "followedUser1";
        String followedUsername2 = "followedUser2";
        String postContent1 = "post content 1";
        String postContent2 = "post content 2";
        String followedPostContent1 = "followed post content 1";
        String followedPostContent2 = "followed post content 2";
        String followedPostContent3 = "followed post content 3";
        String followedPostContent4 = "followed post content 4";

        postingApiRestTemplate.newPost(username, postContent1);
        postingApiRestTemplate.newPost(username, postContent2);
        postingApiRestTemplate.newPost(followedUsername2, followedPostContent3);
        postingApiRestTemplate.newPost(followedUsername1, followedPostContent2);
        postingApiRestTemplate.newPost(followedUsername2, followedPostContent4);
        postingApiRestTemplate.newPost(followedUsername1, followedPostContent1);
        postingApiRestTemplate.follow(username, followedUsername1);
        postingApiRestTemplate.follow(username, followedUsername2);

        //when
        EnhancedResultActions resultActions = postingApiRestTemplate.getCompleteTimeline(username);

        //then
        List<PostDto> posts = resultActions.expectIsOk().andGetResponseBody(new TypeReference<List<PostDto>>() {
        });

        User savedUser = initializingRepository.getInitializedUser(username);
        Post post1 = savedUser.getPosts().get(0);
        Post post2 = savedUser.getPosts().get(1);

        User followedUser1 = initializingRepository.getInitializedUser(followedUsername1);
        followedUser1.getPosts().sort((p1, p2) -> p2.getCreatedDate().compareTo(p1.getCreatedDate()));
        Post followedPost1 = followedUser1.getPosts().get(0);
        Post followedPost2 = followedUser1.getPosts().get(1);

        User followedUser2 = initializingRepository.getInitializedUser(followedUsername2);
        followedUser2.getPosts().sort((p1, p2) -> p2.getCreatedDate().compareTo(p1.getCreatedDate()));
        Post followedPost4 = followedUser2.getPosts().get(0);
        Post followedPost3 = followedUser2.getPosts().get(1);

        assertThat(posts)
                .flatExtracting(PostDto::getUsername, PostDto::getContent, PostDto::getCreatedDate)
                .containsExactly(
                        followedUsername1, followedPost1.getContent(), followedPost1.getCreatedDate(),
                        followedUsername2, followedPost4.getContent(), followedPost4.getCreatedDate(),
                        followedUsername1, followedPost2.getContent(), followedPost2.getCreatedDate(),
                        followedUsername2, followedPost3.getContent(), followedPost3.getCreatedDate())
                .doesNotContain(
                        username, post1.getContent(), post1.getCreatedDate(),
                        username, post2.getContent(), post2.getCreatedDate());
    }

    @Test
    public void testGetCompleteTimelineUserNotPresent() {
        //given
        String username = "testUser";

        //when
        EnhancedResultActions resultActions = postingApiRestTemplate.getCompleteTimeline(username);

        //then
        resultActions.expectIsBadRequest();
    }

    @Test
    public void testGetWall() {
        //given
        String username = "testUser";
        String followedUsername = "followedUser";
        String postContent1 = "post content 1";
        String postContent2 = "post content 2";
        String postContent3 = "post content 3";
        String postContent4 = "post content 4";
        String followedPostContent1 = "followed post content 1";
        String followedPostContent2 = "followed post content 2";

        postingApiRestTemplate.newPost(username, postContent2);
        postingApiRestTemplate.newPost(username, postContent3);
        postingApiRestTemplate.newPost(username, postContent1);
        postingApiRestTemplate.newPost(username, postContent4);
        postingApiRestTemplate.newPost(followedUsername, followedPostContent1);
        postingApiRestTemplate.newPost(followedUsername, followedPostContent2);
        postingApiRestTemplate.follow(username, followedUsername);

        //when
        EnhancedResultActions resultActionsPage0 = postingApiRestTemplate.getWall(username, 0, 2);
        EnhancedResultActions resultActionsPage1 = postingApiRestTemplate.getWall(username, 1, 2);

        //then
        List<PostDto> page0Posts = resultActionsPage0.expectIsOk().andGetResponseBody(
                new TypeReference<List<PostDto>>() {
                });
        List<PostDto> page1Posts = resultActionsPage1.expectIsOk().andGetResponseBody(
                new TypeReference<List<PostDto>>() {
                });

        User savedUser = initializingRepository.getInitializedUser(username);
        savedUser.getPosts().sort((p1, p2) -> p2.getCreatedDate().compareTo(p1.getCreatedDate()));
        Post post4 = savedUser.getPosts().get(0);
        Post post1 = savedUser.getPosts().get(1);
        Post post3 = savedUser.getPosts().get(2);
        Post post2 = savedUser.getPosts().get(3);

        User followedUser = initializingRepository.getInitializedUser(followedUsername);
        Post followedPost1 = followedUser.getPosts().get(0);
        Post followedPost2 = followedUser.getPosts().get(1);

        assertThat(page0Posts)
                .flatExtracting(PostDto::getUsername, PostDto::getContent, PostDto::getCreatedDate)
                .containsExactly(
                        username, post4.getContent(), post4.getCreatedDate(),
                        username, post1.getContent(), post1.getCreatedDate())
                .doesNotContain(
                        followedUsername, followedPost1.getContent(), followedPost1.getCreatedDate(),
                        followedUsername, followedPost2.getContent(), followedPost2.getCreatedDate());

        assertThat(page1Posts)
                .flatExtracting(PostDto::getUsername, PostDto::getContent, PostDto::getCreatedDate)
                .containsExactly(
                        username, post3.getContent(), post3.getCreatedDate(),
                        username, post2.getContent(), post2.getCreatedDate())
                .doesNotContain(
                        followedUsername, followedPost1.getContent(), followedPost1.getCreatedDate(),
                        followedUsername, followedPost2.getContent(), followedPost2.getCreatedDate());
    }

    @Test
    public void testGetWallUserNotPresent() {
        //given
        String username = "testUser";

        //when
        EnhancedResultActions resultActions = postingApiRestTemplate.getWall(username, 1, 10);

        //then
        resultActions.expectIsBadRequest();
    }

    @Test
    public void testGetWallPageTooHigh() {
        //given
        String username = "testUser";
        String followedUsername = "followedUser";
        String postContent1 = "post content 1";
        String postContent2 = "post content 2";
        String postContent3 = "post content 3";
        String postContent4 = "post content 4";
        String followedPostContent1 = "followed post content 1";
        String followedPostContent2 = "followed post content 2";

        postingApiRestTemplate.newPost(username, postContent1);
        postingApiRestTemplate.newPost(username, postContent2);
        postingApiRestTemplate.newPost(username, postContent3);
        postingApiRestTemplate.newPost(username, postContent4);
        postingApiRestTemplate.newPost(followedUsername, followedPostContent1);
        postingApiRestTemplate.newPost(followedUsername, followedPostContent2);
        postingApiRestTemplate.follow(username, followedUsername);

        //when
        EnhancedResultActions resultActions = postingApiRestTemplate.getWall(username, 2, 2);

        //then
        resultActions.expectIsBadRequest();
    }

    @Test
    public void testGetTimeline() {
        //given
        String username = "testUser";
        String followedUsername1 = "followedUser1";
        String followedUsername2 = "followedUser2";
        String postContent1 = "post content 1";
        String postContent2 = "post content 2";
        String followedPostContent1 = "followed post content 1";
        String followedPostContent2 = "followed post content 2";
        String followedPostContent3 = "followed post content 3";
        String followedPostContent4 = "followed post content 4";

        postingApiRestTemplate.newPost(username, postContent1);
        postingApiRestTemplate.newPost(username, postContent2);
        postingApiRestTemplate.newPost(followedUsername2, followedPostContent3);
        postingApiRestTemplate.newPost(followedUsername1, followedPostContent2);
        postingApiRestTemplate.newPost(followedUsername2, followedPostContent4);
        postingApiRestTemplate.newPost(followedUsername1, followedPostContent1);
        postingApiRestTemplate.follow(username, followedUsername1);
        postingApiRestTemplate.follow(username, followedUsername2);

        //when
        EnhancedResultActions resultActionsPage0 = postingApiRestTemplate.getTimeline(username, 0, 2);
        EnhancedResultActions resultActionsPage1 = postingApiRestTemplate.getTimeline(username, 1, 2);

        //then
        List<PostDto> page0Posts = resultActionsPage0.expectIsOk().andGetResponseBody(
                new TypeReference<List<PostDto>>() {
                });
        List<PostDto> page1Posts = resultActionsPage1.expectIsOk().andGetResponseBody(
                new TypeReference<List<PostDto>>() {
                });

        User savedUser = initializingRepository.getInitializedUser(username);
        Post post1 = savedUser.getPosts().get(0);
        Post post2 = savedUser.getPosts().get(1);

        User followedUser1 = initializingRepository.getInitializedUser(followedUsername1);
        followedUser1.getPosts().sort((p1, p2) -> p2.getCreatedDate().compareTo(p1.getCreatedDate()));
        Post followedPost1 = followedUser1.getPosts().get(0);
        Post followedPost2 = followedUser1.getPosts().get(1);

        User followedUser2 = initializingRepository.getInitializedUser(followedUsername2);
        followedUser2.getPosts().sort((p1, p2) -> p2.getCreatedDate().compareTo(p1.getCreatedDate()));
        Post followedPost4 = followedUser2.getPosts().get(0);
        Post followedPost3 = followedUser2.getPosts().get(1);

        assertThat(page0Posts)
                .flatExtracting(PostDto::getUsername, PostDto::getContent, PostDto::getCreatedDate)
                .containsExactly(
                        followedUsername1, followedPost1.getContent(), followedPost1.getCreatedDate(),
                        followedUsername2, followedPost4.getContent(), followedPost4.getCreatedDate())
                .doesNotContain(
                        username, post1.getContent(), post1.getCreatedDate(),
                        username, post2.getContent(), post2.getCreatedDate());

        assertThat(page1Posts)
                .flatExtracting(PostDto::getUsername, PostDto::getContent, PostDto::getCreatedDate)
                .containsExactly(
                        followedUsername1, followedPost2.getContent(), followedPost2.getCreatedDate(),
                        followedUsername2, followedPost3.getContent(), followedPost3.getCreatedDate())
                .doesNotContain(
                        username, post1.getContent(), post1.getCreatedDate(),
                        username, post2.getContent(), post2.getCreatedDate());
    }

    @Test
    public void testGetTimelineUserNotPresent() {
        //given
        String username = "testUser";

        //when
        EnhancedResultActions resultActions = postingApiRestTemplate.getTimeline(username, 1, 10);

        //then
        resultActions.expectIsBadRequest();
    }

    @Test
    public void testGetTimelinePageTooHigh() {
        //given
        String username = "testUser";
        String followedUsername1 = "followedUser1";
        String followedUsername2 = "followedUser2";
        String postContent1 = "post content 1";
        String postContent2 = "post content 2";
        String followedPostContent1 = "followed post content 1";
        String followedPostContent2 = "followed post content 2";
        String followedPostContent3 = "followed post content 3";
        String followedPostContent4 = "followed post content 4";

        postingApiRestTemplate.newPost(username, postContent1);
        postingApiRestTemplate.newPost(username, postContent2);
        postingApiRestTemplate.newPost(followedUsername2, followedPostContent3);
        postingApiRestTemplate.newPost(followedUsername1, followedPostContent2);
        postingApiRestTemplate.newPost(followedUsername2, followedPostContent4);
        postingApiRestTemplate.newPost(followedUsername1, followedPostContent1);
        postingApiRestTemplate.follow(username, followedUsername1);
        postingApiRestTemplate.follow(username, followedUsername2);

        //when
        EnhancedResultActions resultActions = postingApiRestTemplate.getTimeline(username, 2, 2);

        //then
        resultActions.expectIsBadRequest();
    }

    @TestConfiguration
    public static class SupportConfig {

        @Bean
        public PostingApiRestTemplate postingApiRestTemplate(MockMvc mockMvc,
                                                             Jackson2ObjectMapperBuilder objectMapperBuilder) {
            return new PostingApiRestTemplate(mockMvc, objectMapperBuilder);
        }

        @Bean
        public InitializingRepository initializingRepository(UserRepository userRepository) {
            return new InitializingRepository(userRepository);
        }
    }
}