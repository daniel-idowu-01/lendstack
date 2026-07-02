package ng.lendstack.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Uniform response envelope for every endpoint:
 * success: { success: true,  data, message, timestamp }
 * error:   { success: false, error: { code, message, details }, timestamp }
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String message;
    private final ErrorBody error;
    @Builder.Default
    private final Instant timestamp = Instant.now();

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder().success(true).data(data).build();
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return ApiResponse.<T>builder().success(true).data(data).message(message).build();
    }

    public static ApiResponse<Void> okMessage(String message) {
        return ApiResponse.<Void>builder().success(true).message(message).build();
    }

    public static <T> ApiResponse<T> error(String code, String message, Object details) {
        return ApiResponse.<T>builder()
            .success(false)
            .error(new ErrorBody(code, message, details))
            .build();
    }

    @Getter
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorBody {
        private final String code;
        private final String message;
        private final Object details;
    }
}
