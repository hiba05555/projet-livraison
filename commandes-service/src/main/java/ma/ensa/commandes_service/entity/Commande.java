package ma.ensa.commandes_service.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor

public class Commande {
    @Id
    @GeneratedValue
    private Long id;
    private LocalDateTime dateTime;
    @ElementCollection
    private List<Long> productsIds;


}
