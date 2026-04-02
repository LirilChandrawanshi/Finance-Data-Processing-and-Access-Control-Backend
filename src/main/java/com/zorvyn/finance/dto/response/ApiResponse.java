package com.zorvyn.finance.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Canonical API response envelope.
 *
 * <p>Every HTTP response from this API is wrapped in this structure:
 * <pre>
 * {
 *   "success"  : true | false,
 *   "data"     : { ... } | null,
 *   "error"    : "message" | null,
 *   "timestamp": "2024-03-15T10:30:00Z"
 * }
 * </pre>
 *
 * <p>{@code @JsonInclude(NON_NULL)} suppresses the {@code "data"} key entirely on
 * error responses and the {@code "error"} key on success responses — keeping
 * the payload clean without coupling the serialization to the Java model.
 *
 * @param <T> the type of the {@code data} payload
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private String error;
    private String timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(Instant.now().toString())
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(message)
                .timestamp(Instant.now().toString())
                .build();
    }
}
