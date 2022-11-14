package pixiv.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Picture {
    private String title;//
    private String id;//
    private String src;//
    private Integer pageCount;//
    private String userName;
}
