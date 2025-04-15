package hk.ust.csit5930.seachengine.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.TreeMap;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRecord implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Integer docId;

    private String url;

    private Long lastModified;

    private String title;

    private Long size;

    private HashSet<Integer> parents;

    private HashSet<Integer> children;

    private TreeMap<String, Integer> headFreq;

    private TreeMap<String, Integer> bodyFreq;

    private Integer headWordCount;

    private Integer bodyWordCount;

    public void addParent(int parentId) {
        if (parents == null) {
            parents = new HashSet<>();
        }
        parents.add(parentId);
    }

    public void addChild(int childId) {
        if (children == null) {
            children = new HashSet<>();
        }
        children.add(childId);
    }
}
