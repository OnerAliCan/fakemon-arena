# Écran de fin et revanche — #15

## Fonctionnement

`BattleScreen.onEvent(BattleEvent)` conserve le numéro reçu dans `TurnStartedEvent`.
À réception de `BattleEndedEvent`, il demande à `SceneManager` d'afficher
`BattleEndScreen`. Un second événement après la fin est ignoré.

Le bilan montre victoire ou défaite du point de vue du joueur, le nombre de tours,
la raison de fin et les six membres de son équipe avec leurs PV. Un membre K.O.
est grisé. Les actions sont transmises sous forme de `Runnable` : l'écran de fin
ne construit pas lui-même les autres écrans.

- **Menu** réaffiche l'objet `MainMenu` existant.
- **Revanche** appelle `SceneManager.startDemoBattle` : nouvel écran et nouveau
  scénario, nouveaux objets de fakemon, PV et PP réinitialisés, tour à zéro.

## Données provisoires

Le catalogue de démonstration comprend douze espèces, aux noms et types du GDD,
avec des statistiques et des attaques de remplissage. `TeamDrafter` (#10) tire
deux équipes de six sans doublon entre les camps. Une revanche effectue un nouveau
tirage ; le hasard peut reprendre certaines espèces.

La démonstration n'est pas un moteur 6 contre 6 : seuls les premiers membres sont
affichés au combat. Le scénario synthétique se termine lorsque le mon adverse
a perdu ses 150 PV (cinq attaques à 30 PV), puis envoie un `BattleEndedEvent` de
victoire. Les autres membres ne sont pas combattus. **Abandonner** émet un résultat
de défaite, y compris avant le premier tour. Le vrai moteur décidera des résultats
lors du branchement #26 ; la méthode `onEvent` est prête à recevoir son résultat.

Cette branche part de main et ne dépend pas de la PR #14 encore en relecture.

## Vérification manuelle

1. Lancer `./mvnw javafx:run` et ouvrir Combat contre l'IA.
2. Utiliser cinq attaques disponibles : vérifier Victoire et 5 tours.
3. Choisir Revanche : vérifier le nouveau tirage, les PV pleins, les PP restaurés
   et un journal neuf ; la quatrième attaque reste volontairement à 0 PP.
4. Abandonner avant toute attaque : vérifier Défaite, abandon et 0 tour.
5. Choisir Menu : vérifier le retour au menu principal.

Les tests de `BattleDemoTest` couvrent la consommation des PP, l'émission unique
du résultat, le blocage après fin et le nouveau tirage de deux équipes complètes.
Commande de vérification du projet : `./mvnw verify`.
