package hk.ust.csit5930.seachengine.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuggestResult implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Double score;

    private String text;
}
