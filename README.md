# Projet Livraison — Plateforme VRP

Plateforme web de gestion et d'optimisation des livraisons.

## Équipe
| Personne | Service |
|---|---|
| Mariem | Auth Service + API Gateway |
| Faik | Commande Service |
| Aya | VRP Service |
| Fati | Livraison Service |
| Fath | Tracking Service + Notification Service |
| Hiba | Frontend React |

## Stack Technique
- **Backend** : Java 17 + Spring Boot 3
- **Frontend** : React + Leaflet.js
- **Base de données** : PostgreSQL + Redis
- **Conteneurisation** : Docker + Docker Compose

## Lancer le projet
```bash
docker-compose up --build
```

## Services
| Service | Port |
|---|---|
| API Gateway | 8080 |
| Auth Service | 8081 |
| Commande Service | 8082 |
| Livraison Service | 8083 |
| VRP Service | 8084 |
| Tracking Service | 8085 |
| Notification Service | 8086 |
| Frontend | 3000 |
