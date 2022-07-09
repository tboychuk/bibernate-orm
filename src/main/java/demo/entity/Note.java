package demo.entity;

import bibernate.annotation.Column;
import bibernate.annotation.Id;
import bibernate.annotation.Table;
import lombok.Data;

@Data
@Table("notes")
public class Note {
    @Id
    private Long id;
    
    private String body;
}
