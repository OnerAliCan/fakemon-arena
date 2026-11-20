# Animations du combat — issue #27

`BattleScreen` capture les événements du moteur, comme auparavant. Le moteur
résout le tour immédiatement ; seule sa présentation est étalée dans le temps.
Les PV et le statut d'une entrée restent capturés au moment de l'événement,
pour éviter de montrer trop tôt l'état final du tour.

## Animations

- Perte de PV : clignotement du sprite (quatre alternances de 75 ms) et
  `Timeline` de 550 ms. La barre, sa couleur et le nombre de PV suivent une
  propriété JavaFX contenant les PV affichés, sans modifier le Fakemon.
- Soin : même progression des PV, sans clignotement d'impact.
- KO : fondu et glissement vers le bas pendant 350 ms ; les informations
  et la barre à zéro restent visibles, le sprite disparaît.
- Entrée : fondu et glissement latéral pendant 350 ms, depuis le côté du camp.
- Changement volontaire : sortie du sprite actuel pendant 250 ms, remplacement
  de son contenu uniquement après la sortie, puis animation d'entrée.
  Après un KO, aucune seconde sortie n'est jouée. Les déplacements du sprite
  sont réinitialisés avant l'entrée du remplaçant.

`BattleAnimations` construit les `Timeline`, `FadeTransition`,
`TranslateTransition`, `ParallelTransition` et `SequentialTransition`.

## Ordre et cycle de vie

`BattleEventQueue` ne démarre qu'une présentation à la fois. Son callback de fin
est appelé par `onFinished`, après l'animation et une courte pause de 110 ms.
Les bannières d'objet et de talent conservent leur durée de 850 ms.
Un message réseau reçu entre-temps s'ajoute à la file et ne coupe pas l'animation.
Les commandes de combat restent verrouillées pendant la résolution d'un tour.

Quand on quitte l'écran ou que la connexion échoue, la présentation est arrêtée
et les événements restants sont supprimés. Un callback ancien ne peut ni
relancer la file, ni terminer une nouvelle présentation.

## Vérification

- `./mvnw verify` : moteur, réseau et tests de la file sans affichage.
- `./mvnw verify -Dfakemon.ui.tests=true` : ajoute les tests JavaFX, dont les
  valeurs intermédiaires des PV, le retour d'opacité après l'impact, le KO,
  le remplacement et l'arrêt lors de la navigation.
- En CI Linux, les tests d'animation et d'intégration tournent sous Xvfb.

Démonstration : lancer `./mvnw javafx:run`, choisir un combat aléatoire,
attaquer, puis utiliser Équipe pour changer de Fakemon. Vérifier les PV et
les sprites sans lire le journal ; poursuivre jusqu'à un KO. Vérifier aussi
le remplacement et l'écran de résultat, ainsi qu'un combat LAN à deux postes.

Les diagrammes `architecture.puml` et `sequence-tour.puml` décrivent désormais
la file de présentation et l'attente de fin d'animation.
