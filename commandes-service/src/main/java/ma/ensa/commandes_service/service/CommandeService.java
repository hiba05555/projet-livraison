package ma.ensa.commandes_service.service;

import ma.ensa.commandes_service.dto.CommandeResponse;
import ma.ensa.commandes_service.dto.ProductDto;
import ma.ensa.commandes_service.entity.Commande;
import ma.ensa.commandes_service.repository.CommandeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
@Service

public class CommandeService {
    @Autowired
    ProductService productService;
    @Autowired
    CommandeRepository commandeRepository;


    public CommandeResponse getCommande(long id){
        Commande commande  = commandeRepository.findById(id).orElseThrow();
        List<ProductDto> products = commande.getProductsIds()
                .stream()
                .map(productService::getProductById)
                .toList();

        return new CommandeResponse(commande.getId(),commande.getDateTime(),products);
    }






}
