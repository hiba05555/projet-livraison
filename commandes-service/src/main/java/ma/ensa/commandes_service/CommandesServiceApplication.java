package ma.ensa.commandes_service;

import ma.ensa.commandes_service.entity.Commande;
import ma.ensa.commandes_service.repository.CommandeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class CommandesServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CommandesServiceApplication.class, args);}

	@Bean
	CommandLineRunner initDatabase(CommandeRepository commandeRepository) {

		return args -> {

			Commande c1 = new Commande();
			c1.setDateTime(LocalDateTime.now());
			c1.setProductsIds(List.of(1L, 2L));

			Commande c2 = new Commande();
			c2.setDateTime(LocalDateTime.now().minusDays(1));
			c2.setProductsIds(List.of(2L, 3L, 4L));

			Commande c3 = new Commande();
			c3.setDateTime(LocalDateTime.now().minusHours(5));
			c3.setProductsIds(List.of(1L, 4L));

			commandeRepository.saveAll(
					List.of(c1, c2, c3)
			);

			System.out.println("✅ Données H2 initialisées");
		};
	}
}
