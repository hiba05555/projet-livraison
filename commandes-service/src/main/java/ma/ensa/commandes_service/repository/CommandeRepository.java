package ma.ensa.commandes_service.repository;

import ma.ensa.commandes_service.entity.Commande;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommandeRepository extends JpaRepository<Commande, Long> {
}
