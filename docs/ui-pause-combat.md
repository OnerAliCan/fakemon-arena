# Pause en combat — #44

Pendant le choix d'une action, **Échap** ou **Pause** ouvre le menu de pause.
**Reprendre** ou Échap réaffiche le même combat, avec ses PV, PP et son journal.
Le raccourci ne peut pas interrompre le choix d'un remplaçant.

**Volume** ouvre la même fenêtre que **Options** dans le menu principal.
Les niveaux de musique et d'effets sont partagés entre les parties de la
session. Les lecteurs audio seront reliés à ces réglages dans #28.

**Abandonner** transmet une action `Forfeit` à la démonstration. L'écran affiche
alors le bilan de défaite à la réception de `BattleEndedEvent`. Le vrai moteur
accepte également `Forfeit` pendant le choix d'une action ; son branchement
à l'écran de combat reste prévu dans #26.

Cette branche s'appuie sur #19 pour le panneau d'équipe et réutilise
`BattleEndScreen` de #15. Si #15 et #19 sont fusionnées avant #44, vérifier
l'intégration avant d'ouvrir ou de fusionner la PR de #44.
