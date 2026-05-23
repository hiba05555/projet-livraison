package ma.ensa.admin.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Data

public class Product {
    @Id
    @GeneratedValue
    private Long id;
    private String name;
    private double price;
    private String category;

}
