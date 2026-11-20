# Combat construit — #46

Dans **Combat contre l’IA → Combat construit**, le joueur choisit six espèces
distinctes parmi les trente du catalogue fourni. Un second clic retire une
espèce ; à six, il faut retirer un choix avant d’en ajouter un autre.

**Valider** utilise `TeamDrafter` pour tirer quatre attaques compatibles avec
chaque espèce. Les deux camps reçoivent un objet par fakemon et l’adversaire est
tiré parmi les espèces non choisies. Le panneau d’équipe demande quel membre
envoyer en premier, avant le tour 1.

En combat, **Équipe** permet un changement volontaire. Un K.O. impose le choix
d’un remplaçant valide tant qu’il reste des membres debout. **Revanche** conserve
les espèces, attaques et objets du joueur, restaure les PV, retire l’équipe
adverse au sort et redemande le premier fakemon.

Le parcours utilise `DefaultBattleEngine` et ses règles de dégâts, de talents,
d’objets et de statuts. Le catalogue est fourni par `JsonSpeciesRepository`, qui
charge les espèces intégrées et les fiches du dossier utilisateur.

En LAN, la même sélection est proposée aux deux joueurs après le choix du mode
par l’hôte. Chacun choisit son premier combattant pendant la préparation ; l’hôte
construit les deux équipes validées avant de démarrer le moteur.

Pour vérifier : lancer `./mvnw javafx:run`, tester Valider à 0, 5 et 6 choix,
retirer une carte, valider, envoyer un premier fakemon puis abandonner pour
essayer Revanche. `./mvnw verify` couvre le tirage, les choix invalides et la
conservation de l’équipe à la revanche.
