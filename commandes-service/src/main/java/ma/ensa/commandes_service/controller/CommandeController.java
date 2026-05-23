package ma.ensa.commandes_service.controller;


import ma.ensa.commandes_service.entity.Commande;
import ma.ensa.commandes_service.repository.CommandeRepository;
import ma.ensa.commandes_service.service.CommandeService;
import ma.ensa.commandes_service.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/Commandes")
public class CommandeController {

    @Autowired
    CommandeRepository commandeRepository;


    @Autowired
    ProductService productService;
    @Autowired
    CommandeService commandeService;

    @GetMapping
    public List<Commande> findAll(){
        return commandeRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getCommandeById(@PathVariable Long id){
        return ResponseEntity.ok(commandeService.getCommande(id));
    }

    @PostMapping("/addcommande/{id}")
    public ResponseEntity<Object> addCommande(@PathVariable Long id, @RequestBody Commande commande){
        return ResponseEntity.ok(commandeRepository.save(commande));
    }

}
