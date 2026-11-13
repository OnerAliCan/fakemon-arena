# Panneau d’équipe — #19

Le bouton **Équipe** affiche les six membres du joueur avec leur sprite, leur nom
français, leurs types et leurs PV. Le fakemon actif et les membres K.O. sont grisés
et ne peuvent pas être envoyés.

- Un changement volontaire consomme un tour. **Retour au combat** ferme le panneau
  sans jouer de tour.
- Après un K.O., le panneau s’ouvre automatiquement et impose un remplaçant valide.
  Le remplacement ne consomme pas de tour.
- Chaque fakemon conserve ses PV et ses PP lorsqu’il quitte le terrain.

## Organisation

`TeamPanel` affiche les choix et transmet un `SendFakemon` à `BattleScreen`.
`BattleDemo` valide le choix et produit les événements factices. L’écran ouvre le
panneau obligatoire sur `FakemonSelectionRequiredEvent(REPLACEMENT)` et actualise
le fakemon affiché sur `FakemonEnteredEvent`. La logique de démonstration sera
remplacée par le moteur lors de #26.

## Vérification manuelle

1. Lancer `./mvnw javafx:run`, puis ouvrir **Combat contre l’IA**.
2. Cliquer sur **Équipe**. Vérifier les six membres et le bouton **En jeu** désactivé.
3. Fermer avec **Retour au combat**, puis rouvrir et envoyer un autre membre.
   Vérifier le changement de sprite et les PV après la riposte fictive.
4. Repartir du menu et ouvrir un combat neuf. Jouer quatre fois **Vague** :
   Koanobi subit quatre ripostes de 25 PV et tombe K.O.
5. Vérifier l’ouverture automatique du panneau, l’absence de retour au combat et
   l’impossibilité de choisir Koanobi. Envoyer un remplaçant et vérifier que les
   attaques redeviennent disponibles.

`./mvnw verify` vérifie notamment la consommation du tour, la conservation des PP,
le rejet des choix invalides et la reprise après remplacement.
