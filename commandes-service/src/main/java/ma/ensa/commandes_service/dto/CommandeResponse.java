package ma.ensa.commandes_service.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class CommandeResponse {
    private Long id;
    private LocalDateTime dateTime;
    private List<ProductDto> products;
}
