package posting.web.endpoint;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import posting.domain.model.PostDto;
import posting.domain.service.PostingService;
import posting.web.request.NewPostRequest;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/users")
public class PostingApi {

    private final PostingService postingService;

    public PostingApi(PostingService postingService) {
        this.postingService = postingService;
    }

    @PostMapping(path = "/{username}/post", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> post(@PathVariable String username, @Valid @RequestBody NewPostRequest newPostRequest) {
        postingService.newPost(username, newPostRequest.getPost());

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping(path = "/{username}/follow", params = "followedUserName")
    public void follow(@PathVariable String username, @RequestParam("followedUserName") String followedUsername) {
        postingService.follow(username, followedUsername);
    }

    @GetMapping(path = "/{username}/completeWall", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<PostDto> getCompleteWall(@PathVariable String username) {
        return postingService.getCompleteWall(username);
    }

    @GetMapping(
            path = "/{username}/wall",
            params = {"page", "size"},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<PostDto> getWall(@PathVariable String username, @RequestParam int page, @RequestParam int size) {
        return postingService.getWall(username, page, size);
    }

    @GetMapping(path = "/{username}/completeTimeline", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<PostDto> getCompleteTimeline(@PathVariable String username) {
        return postingService.getCompleteTimeline(username);
    }

    @GetMapping(
            path = "/{username}/timeline",
            params = {"page", "size"},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<PostDto> getTimeline(@PathVariable String username, @RequestParam int page, @RequestParam int size) {
        return postingService.getTimeline(username, page, size);
    }
}