# Panneau d’équipe — #19

Le bouton **Équipe** affiche les six membres du joueur avec leur sprite, leur nom
français, leurs types et leurs PV. Le Fakemon actif et les membres K.O. sont grisés
et ne peuvent pas être envoyés. Un statut présent est indiqué dans sa ligne.

- Un changement volontaire consomme un tour. **Retour au combat** ferme le panneau
  sans jouer de tour.
- Après un K.O., le panneau impose un remplaçant valide et ne propose pas de retour.
  Ce remplacement ne consomme pas un tour supplémentaire.
- Chaque Fakemon conserve ses PV et son statut lorsqu’il quitte le terrain.
  Le jeu ne gère pas de PP.
- En combat construit contre l’IA, le panneau demande aussi le premier combattant.
  En LAN construit, ce choix a lieu dans la préparation des équipes.

## Organisation

`TeamPanel` affiche les choix et transmet un `SendFakemon` à `BattleScreen`.
Contre l’IA, `DefaultBattleEngine` valide l’action et produit les événements.
En LAN, l’hôte reçoit les choix et exécute le même moteur. L’écran ouvre le panneau
obligatoire sur `FakemonSelectionRequiredEvent` ou la demande réseau de remplacement,
et actualise le combattant sur `FakemonEnteredEvent`.

## Vérification

1. Lancer `./mvnw javafx:run`, puis un combat contre l’IA.
2. Cliquer sur **Équipe** et vérifier que le membre **En jeu** ne peut pas être envoyé.
3. Fermer avec **Retour au combat**, puis rouvrir et envoyer un autre membre.
   Vérifier le changement, la consommation du tour et les dégâts éventuels.
4. Jouer jusqu’à un K.O. avec des remplaçants encore debout. Vérifier que le choix
   est obligatoire et que les membres K.O. ne peuvent pas être sélectionnés.
5. Envoyer un remplaçant et vérifier la reprise du combat.

`./mvnw verify` couvre les changements, les choix invalides, les remplacements
obligatoires et la conservation des états de combat.
