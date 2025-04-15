package hk.ust.csit5930.seachengine.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitResult {
    private Integer code;
    private String message;

    public static InitResult success() {
        return InitResult.builder()
                .code(1)
                .message("OK")
                .build();
    }

    public static InitResult error(String message) {
        return InitResult.builder()
                .code(0)
                .message(message)
                .build();
    }
}
