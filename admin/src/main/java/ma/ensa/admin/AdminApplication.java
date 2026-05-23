package ma.ensa.admin;

import ma.ensa.admin.entity.Product;
import ma.ensa.admin.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;

@EnableDiscoveryClient
@SpringBootApplication
public class AdminApplication {

	public static void main(String[] args) {
		SpringApplication.run(AdminApplication.class, args);
	}
	@Bean
	CommandLineRunner initializeH2(ProductRepository productRepository){
		return args -> {productRepository.save(new Product(null,"lait",11,"laitier"));};
	}

}
