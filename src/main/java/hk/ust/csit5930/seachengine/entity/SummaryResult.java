package hk.ust.csit5930.seachengine.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummaryResult {
    private Integer code;
    private String message;

    public static SummaryResult success(String message) {
        return SummaryResult.builder()
                .code(1)
                .message(message)
                .build();
    }

    public static SummaryResult error(String message) {
        return SummaryResult.builder()
                .code(0)
                .message(message)
                .build();
    }
}
