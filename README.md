# Fakemon Arena

[![CI](https://github.com/OnerAliCan/fakemon-arena/actions/workflows/ci.yml/badge.svg)](https://github.com/OnerAliCan/fakemon-arena/actions/workflows/ci.yml)

Combats de fakemon au tour par tour, façon Pokémon Showdown : deux équipes tirées au hasard parmi
30 fakemon originaux s'affrontent. Projet Epitech en Java 21 et JavaFX.

## Installer son poste

Le setup installe les outils (Git, Java 21, GitHub CLI), clone le dépôt et vérifie que tout compile :

- Windows : double-clic sur `setup-windows.bat`
- macOS : `bash setup-mac.command` dans le Terminal

## Lancer le jeu et les tests

| | Windows | macOS |
|---|---|---|
| Jouer | `.\mvnw.cmd javafx:run` | `./mvnw javafx:run` |
| Tests et couverture | `.\mvnw.cmd verify` | `./mvnw verify` |

Maven n'a pas besoin d'être installé : `mvnw` le télécharge la première fois.
Le rapport de couverture JaCoCo est dans `target/site/jacoco/index.html`.
Dans VS Code, lancez la classe `Launcher` (pas `App`).

## Équipe

| Rôle | Notions | GitHub |
|---|---|---|
| Dev A · Scrum Master | cœur du jeu, moteur, IA, données JSON, tests, CI | @OnerAliCan |
| Dev B · Product Owner | écrans JavaFX, dashboard, animations, son, soutenance | @matteobleuvialon |

## Façon de travailler

- Le plan complet : [docs/Fakemon_Arena_plan_projet.pdf](docs/Fakemon_Arena_plan_projet.pdf)
- Une issue = une branche = une PR, relue par l'autre dev puis squash-merge sur `main`.
- `main` est protégée : pas de push direct, CI verte obligatoire.
- Une fois par machine, pour activer la vérification de chaque commit (message, branche, fichiers, compilation et tests) : `git config core.hooksPath .githooks`.
