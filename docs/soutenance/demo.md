# Démonstration de Fakemon Arena

Le pitch dure 10 minutes, puis les questions durent 10 minutes. La démo occupe 2 min 25 s, de 04:45 à 07:10. Les notes de [Fakemon Arena](Fakemon%20Arena.key) donnent le texte et les repères de chaque diapositive.

## Avant le pitch

1. Depuis la racine du dépôt, avec le JDK 21, lancer le jeu avec la graine **31**.
   - macOS ou Linux avec affichage : `./scripts/demo-soutenance.sh`
   - Windows dans PowerShell : `$env:JAVA_TOOL_OPTIONS = ("$env:JAVA_TOOL_OPTIONS -Dfakemon.demo.seed=31").Trim()` puis `.\mvnw.cmd javafx:run`. À la fin, retirer l’option de démo de la session PowerShell.
2. Sur ce poste, ouvrir la présentation dans Keynote et laisser le jeu sur le **menu principal**. Vérifier l’affichage sur le projecteur, le volume et la bascule entre les deux apps.
3. Pour tester le départ, ouvrir **Combat contre l’IA**, puis **Combat aléatoire**. On doit voir **Lumax** contre **Négatôme**. Revenir ensuite au menu ou relancer le jeu.

La graine fixe le tirage à partir du catalogue embarqué et les résultats du combat à actions identiques. Le décor est tiré indépendamment. Pour rejouer la séquence, repasser par le menu et choisir **Combat aléatoire**. **Revanche** continue le tirage et donne un autre combat.

## Séquence à présenter

| Repère du pitch | Geste | Point à montrer |
| --- | --- | --- |
| 04:45–05:20 | **Combat contre l’IA** → **Combat aléatoire**. Survoler puis jouer **Lueur**. | Lumax dispose de quatre attaques. Lueur, de type Éther, exploite le type Ombre de Négatôme et lui retire 32 PV. Négatôme riposte avec Éclipse et met Lumax K.O. |
| 05:20–05:45 | Au panneau **Choisis un remplaçant**, cliquer sur **Envoyer** à la ligne **Nymphaline**. | La perte de Lumax force un choix. Nymphaline a aussi des attaques Éther. |
| 05:45–06:40 | Jouer **Comète** trois fois, en attendant le retour des boutons entre chaque tour. | Les deux premières tentatives échouent. Nymphaline tient le coup et la troisième Comète met Négatôme K.O. au quatrième tour. Le duel continue. |
| 06:40–07:10 | Revenir à Keynote et passer à la diapositive **Les premiers joueurs à convaincre**. | « Le premier combattant est tombé. La décision suivante nous a remis dans le duel. » |

La démo s’arrête au premier K.O. adverse. Si une question porte sur l’écran de résultat, ouvrir **Pause** puis **Abandonner** pour montrer l’écran **Défaite** et le bouton **Revanche**.

## Si le lancement échoue

La diapositive 6 montre le duel initial. Passer aux diapositives 10 et 11 pour montrer le remplacement après le K.O. de Lumax et la riposte de Nymphaline, puis reprendre à la diapositive 7. Ces trois captures proviennent du même scénario.

## Répétition à deux

Le vendredi 20 novembre figurait au planning de l’issue #31. Pour la prochaine répétition, démarrer le chronomètre à l’ouverture, commencer la démo à 04:45 et reprendre Keynote à 07:10. Finir la demande d’investissement avant 10:00 et consacrer les dix minutes suivantes aux questions. Rejouer une fois avec les captures de secours et une fois sur le matériel de projection.

[Issue de soutenance #31](https://github.com/OnerAliCan/fakemon-arena/issues/31)
