package pixiv.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Picture {
    private String title;//
    private String id;//
    private String src;//
    private Integer pageCount;//
    private String userId;
    private String userName;

    public Picture(String id) {
        this.id = id;
    }

    public Picture(String title, String id, String src, Integer pageCount, String userName) {
        this.title = title;
        this.id = id;
        this.src = src;
        this.pageCount = pageCount;
        this.userName = userName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Picture picture = (Picture) o;
        return id.equals(picture.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
