package hk.ust.csit5930.seachengine.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Integer docId;

    private Double score;

    private String title;

    private String url;

    private Long lastModified;

    private Long size;

    private List<String> keywords;

    private List<String> parents;

    private List<String> children;
}
