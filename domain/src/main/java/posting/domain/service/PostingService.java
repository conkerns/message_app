package posting.domain.service;

import posting.domain.model.PostDto;

import java.util.List;

public interface PostingService {

    void newPost(String username, String postContent);

    void follow(String username, String followedUsername);

    List<PostDto> getCompleteWall(String username);

    List<PostDto> getWall(String username, int page, int size);

    List<PostDto> getCompleteTimeline(String username);

    List<PostDto> getTimeline(String username, int page, int size);
}