# Spike réseau (#34)

## Essai

Lancer `NetworkSpikeServer`, puis `NetworkSpikeClient` dans un second terminal
ou avec les boutons Run de VS Code. Le client se connecte à `localhost:5000`,
convertit un `TurnStartedEvent(1)` en JSON avec Gson et l'envoie sur une ligne.
Le serveur lit la ligne, la convertit en `TurnStartedEvent`, puis ferme la connexion.

Le test sur une seule machine a donné le résultat suivant.

```text
JSON reçu : {"turnNumber":1}
Tour reconstruit : 1
```

La suite du projet passe également avec 97 tests réussis.

## Suite pour le LAN (#35)

Vérifier la connexion entre deux postes sur le réseau local. Définir un format
qui identifie le type de chaque message, puis gérer les actions de l'invité et
le retour des événements depuis le moteur de l'hôte. Vérifier aussi comment
signaler une déconnexion et transmettre les événements à l'écran JavaFX.

L'échange JSON est validé en local pour un type d'événement connu. Ce résultat
sert au Go / No-Go, avec un essai sur deux postes et le branchement de l'écran
sur le moteur.
