package posting.web.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public final class NewPostRequest {

    @NotNull
    @Size(max = 140)
    private final String post;

    @JsonCreator
    public NewPostRequest(@JsonProperty("post") String post) {
        this.post = post;
    }

    public String getPost() {
        return post;
    }
}