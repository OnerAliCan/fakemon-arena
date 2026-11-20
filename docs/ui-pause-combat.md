# Pause en combat — #44

Pendant le choix d'une action, **Échap** ou **Pause** ouvre le menu de pause.
**Reprendre** ou Échap réaffiche le même combat, avec ses PV, statuts et son journal.
Le raccourci ne peut pas interrompre le choix d'un remplaçant.

**Volume** ouvre la même fenêtre que **Options** dans le menu principal.
Les niveaux de musique et d'effets sont partagés entre les parties de la
session. Le Mondex utilise déjà le niveau des effets pour les cris disponibles ;
la musique et les effets du combat seront reliés aux réglages dans #28.

**Abandonner** transmet une action `Forfeit` au moteur, directement contre l’IA
ou par la session réseau en LAN. L'écran affiche le bilan de défaite à réception
de `BattleEndedEvent`. La pause est locale en LAN : elle ne bloque pas les
commandes de l’autre joueur.
