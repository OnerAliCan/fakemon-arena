# Bannières et icônes — #29

La cartouche de chaque fakemon affiche le nom et une icône de son objet tenu.
Les dessins JavaFX du Gantelet, de l’Écu et de la Plume sont locaux : aucune
image externe et aucune police d’icônes ne sont nécessaires. Le nom reste
visible et disponible en infobulle et pour les outils d’accessibilité.

À réception d’un `ItemActivatedEvent` ou `TalentActivatedEvent`, l’écran
affiche une bannière sur la cartouche du camp concerné avec le nom du fakemon,
le type d’activation et son nom français. Elle est affichée pendant 850 ms
avant l’événement suivant. Les commandes restent bloquées pendant ce délai ;
le journal conserve une trace après disparition de la bannière.

Les deux modes contre l’IA et le LAN utilisent le même traitement d’événements.
Aucune règle de talent ou d’objet n’est recalculée dans l’interface.

## Vérification

- `./mvnw verify` : tests de logique et de réseau du projet.
- Sur un poste avec affichage : `./mvnw -Dtest=BattleBadgesUiTest -Dfakemon.ui.tests=true test`.
  Vérifie les trois objets, les quatre talents, les deux camps et le nettoyage
  des bannières. Ce test graphique est ignoré par défaut sur les CI sans écran.
- Jouer un combat, observer l’objet dans les cartouches et une activation
  (par exemple Fracas à l’entrée ou Récupération en fin de tour).
