package posting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import posting.web.request.NewPostRequest;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PostingApiRestTemplate {

    private static final String BASE_REQUEST_TEMPLATE = "/users/%s";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    public PostingApiRestTemplate(MockMvc mockMvc, Jackson2ObjectMapperBuilder objectMapperBuilder) {
        this.mockMvc = mockMvc;
        objectMapper = objectMapperBuilder.build();
    }

    EnhancedResultActions newPost(String username, String postContent) {
        String value;

        try {
            value = objectMapper.writeValueAsString(new NewPostRequest(postContent));
        } catch (JsonProcessingException exception) {
            throw new RuntimeException("Failed to serialize request body", exception);
        }

        return perform(MockMvcRequestBuilders.post(baseRequestUrl(username) + "/post")
                .content(value)
                .contentType(MediaType.APPLICATION_JSON));
    }

    EnhancedResultActions follow(String requestingUsername, String followedUsername) {
        return perform(MockMvcRequestBuilders
                .put(baseRequestUrl(requestingUsername) + "/follow")
                .param("followedUserName", followedUsername));
    }

    EnhancedResultActions getCompleteWall(String username) {
        return perform(MockMvcRequestBuilders.get(baseRequestUrl(username) + "/completeWall"));
    }

    EnhancedResultActions getWall(String username, int page, int size) {
        return perform(MockMvcRequestBuilders
                .get(baseRequestUrl(username) + "/wall")
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size)));
    }

    EnhancedResultActions getCompleteTimeline(String username) {
        return perform(MockMvcRequestBuilders.get(baseRequestUrl(username) + "/completeTimeline"));
    }

    EnhancedResultActions getTimeline(String username, int page, int size) {
        return perform(MockMvcRequestBuilders
                .get(baseRequestUrl(username) + "/timeline")
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size)));
    }

    private EnhancedResultActions perform(MockHttpServletRequestBuilder requestBuilder) {
        try {
            return new EnhancedResultActions(mockMvc.perform(requestBuilder));
        } catch (Exception exception) {
            throw new RuntimeException("Failed to execute REST request", exception);
        }
    }

    private String baseRequestUrl(String username) {
        return String.format(BASE_REQUEST_TEMPLATE, username);
    }

    class EnhancedResultActions {

        private final ResultActions resultActions;

        private EnhancedResultActions(ResultActions resultActions) {
            this.resultActions = resultActions;
        }

        EnhancedResultActions andPrint() {
            return andDo(print());
        }

        EnhancedResultActions expectIsOk() {
            return andExpect(status().isOk());
        }

        EnhancedResultActions expectIsCreated() {
            return andExpect(status().isCreated());
        }

        EnhancedResultActions expectIsBadRequest() {
            return andExpect(status().isBadRequest());
        }

        private EnhancedResultActions andDo(ResultHandler resultHandler) {
            try {
                resultActions.andDo(resultHandler);
            } catch (Exception exception) {
                throw new RuntimeException("Result operation failure", exception);
            }

            return this;
        }

        private EnhancedResultActions andExpect(ResultMatcher resultMatcher) {
            try {
                resultActions.andExpect(resultMatcher);
            } catch (Exception exception) {
                throw new RuntimeException("Result operation failure", exception);
            }

            return this;
        }

        <T> T andGetResponseBody(TypeReference<T> bodyType) {
            try {
                String content = andReturn().getResponse().getContentAsString();

                return objectMapper.readValue(content, bodyType);
            } catch (Exception exception) {
                throw new RuntimeException("Failed to deserialize response body", exception);
            }
        }

        MvcResult andReturn() {
            return resultActions.andReturn();
        }

        ResultActions getResultActions() {
            return resultActions;
        }
    }
}