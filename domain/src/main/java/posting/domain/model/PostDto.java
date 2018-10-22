package posting.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public final class PostDto {

    private final String username;
    private final String content;
    private final LocalDateTime createdDate;

    @JsonCreator
    public PostDto(@JsonProperty("username") String username,
                   @JsonProperty("content") String content,
                   @JsonProperty("createdDate") LocalDateTime createdDate) {
        this.username = username;
        this.content = content;
        this.createdDate = createdDate;
    }

    public String getUsername() {
        return username;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
}