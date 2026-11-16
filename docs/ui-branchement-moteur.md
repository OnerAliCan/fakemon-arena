# Combat contre l’IA — #26

Les modes aléatoire et construit utilisent le même écran et le même arbitre.
Le catalogue JSON fournit les espèces et les attaques. L’aperçu aléatoire
montre les équipes qui seront réellement envoyées au moteur. En mode construit,
le moteur demande au joueur de choisir son premier fakemon.

`BattleScreen` s’abonne comme `BattleListener`. Un clic génère un `UseMove`,
un `SendFakemon` ou un `Forfeit`, transmis à `submitAction`. Le moteur demande
lui-même la réponse à `RandomOpponent`. L’écran déroule ensuite les événements
en ordre, sans accepter de nouvelle action pendant leur affichage. Un remplacement
obligatoire ouvre le panneau d’équipe ; le résultat ouvre l’écran de fin.

La revanche aléatoire tire les deux équipes. En mode construit, elle préserve
les espèces, attaques et objets du joueur, réinitialise leurs PV et tire une
nouvelle équipe adverse. Le moteur actuel ne gère pas de PP. Les objets restent
affichés sans effet jusqu’à l’implémentation de leurs règles.

## Vérifier

Lancer `./mvnw verify`, puis `./mvnw javafx:run`. Jouer un combat contre l’IA :
observer les dégâts et le journal, changer de membre, remplacer un membre K.O.
et vérifier la fin de combat. En mode construit, vérifier aussi le choix du
premier membre au début et après Revanche.
