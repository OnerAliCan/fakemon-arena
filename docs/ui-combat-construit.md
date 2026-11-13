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

Le parcours construit simule des dégâts fixes en attendant l’intégration du
moteur. Les objets sont tirés et affichés, mais leurs effets relèvent de #24.
`BundledRoster` lit uniquement le catalogue embarqué ; il ne sauvegarde pas
les ajouts de l’utilisateur.

Pour vérifier : lancer `./mvnw javafx:run`, tester Valider à 0, 5 et 6 choix,
retirer une carte, valider, envoyer un premier fakemon puis abandonner pour
essayer Revanche. `./mvnw verify` couvre le tirage, les choix invalides et la
conservation de l’équipe à la revanche.
