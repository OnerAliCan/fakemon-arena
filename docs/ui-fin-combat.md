# Écran de fin et revanche — #15

## Fonctionnement

`BattleScreen` reçoit les événements de `DefaultBattleEngine` contre l’IA, ou de
la session réseau en LAN. `BattleEndedEvent` ouvre `BattleEndScreen` avec la victoire
ou la défaite du joueur, le nombre de tours, la raison de fin et les six membres
de son équipe avec leurs PV. Les membres K.O. sont grisés.

Les boutons transmettent des actions de navigation :

- **Menu** revient au menu principal.
- **Revanche contre l’IA, mode aléatoire** tire de nouvelles équipes et leurs équipements.
- **Revanche contre l’IA, mode construit** conserve les espèces, les attaques et les
  objets du joueur, restaure les PV et tire une nouvelle équipe adverse. Le joueur
  choisit à nouveau son premier combattant.
- **Revanche en LAN** réutilise la connexion lorsque le résultat a été confirmé.
  Les deux joueurs repassent par la préparation : l’invité se déclare prêt et l’hôte
  choisit le mode puis lance la partie.

Les statuts sont effacés à la fin du combat. Le jeu ne comporte pas de PP.
En LAN, les erreurs de préparation et de connexion empêchent le lancement d’une revanche.
La validation complète de la revanche entre deux PC reste à effectuer.

## Vérification

1. Jouer un combat contre l’IA jusqu’au K.O. de la dernière équipe ; vérifier le bilan.
2. Choisir **Revanche** et vérifier les PV réinitialisés ainsi que les règles du mode choisi.
3. Abandonner un combat depuis la pause ; vérifier la défaite pour abandon.
4. En LAN, terminer une partie puis demander une revanche des deux côtés et lancer
   la nouvelle partie sans ressaisir l’adresse IP.
5. Choisir **Menu** pour revenir au menu principal.

`./mvnw verify` couvre la fin du combat, les abandons et la réutilisation de la connexion
réseau après confirmation du résultat.
