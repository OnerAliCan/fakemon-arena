# -*- coding: utf-8 -*-
"""Crée le backlog Fakemon Arena sur GitHub : labels, milestones (sprints + bonus) et les 35 issues.

Prérequis : GitHub CLI installé (winget install --id GitHub.cli) et connecté (gh auth login).

Usage :
    python scripts/create_backlog.py --dry-run                 # aperçu, aucun appel à GitHub
    python scripts/create_backlog.py --dry-run --preview 17    # affiche le corps complet de l'issue #17
    python scripts/create_backlog.py --repo owner/nom --dev-a loginA --dev-b loginB

Les issues sont créées dans l'ordre du plan de projet pour que leurs numéros (#1 à #35)
correspondent au PDF. Le script refuse donc de démarrer si le dépôt contient déjà d'autres
issues ou PR. Il peut être relancé après une coupure : les issues déjà créées sont ignorées.
"""
import argparse
import json
import shutil
import subprocess
import sys
import unicodedata
from urllib.parse import quote

# --------------------------------------------------------------------------
# Sprints → milestones : (titre, échéance, dates, objectif)
# --------------------------------------------------------------------------
SPRINTS = {
    "S0": ("Sprint 0 · Conception", "2026-11-04", "mar. 3 → mer. 4 nov.",
           "GDD validé, contrat d'API défini à deux, dépôt prêt. "
           "Aucune feature n'est codée avant la validation du GDD."),
    "S1": ("Sprint 1 · Fondations", "2026-11-10", "jeu. 5 → mar. 10 nov.",
           "Modèle, types et dégâts testés ; la CI tourne ; menu, aperçu, combat et fin "
           "s'affichent sur des données factices."),
    "S2": ("Sprint 2 · Moteur & dashboard", "2026-11-16", "jeu. 12 → lun. 16 nov.",
           "Le moteur résout un combat complet en tests (switchs, KO, victoire) ; le dashboard "
           "permet de saisir un fakemon ; les 30 sprites existent."),
    "S3": ("Sprint 3 · Intégration & release", "2026-11-20", "mar. 17 → ven. 20 nov.",
           "Un combat 6 contre 6 jouable contre l'IA sur le vrai roster, avec talents, objets, "
           "animations et sons ; documentation et soutenance prêtes ; v1.0.0 vendredi 20."),
    "BX": ("Bonus · si temps libéré", "2026-11-19", "après le Go / No-Go du lun. 16 nov.",
           "Démarré seulement si tout le P0 des Sprints 1 et 2 est mergé. "
           "Ne s'ajoute jamais à la charge : utilise le temps libéré, ou remplace des P1 sur décision du PO."),
}

# --------------------------------------------------------------------------
# Labels : (nom, couleur, description)
# --------------------------------------------------------------------------
LABELS = [
    ("core", "1d76db", "Modèle, moteur de combat, talents, objets"),
    ("ai", "5319e7", "Intelligence artificielle adverse"),
    ("data", "0052cc", "Données JSON, repository, roster"),
    ("ui", "f9d0c4", "Écrans JavaFX"),
    ("anim", "fef2c0", "Animations de combat"),
    ("audio", "c2e0c6", "Musiques, effets sonores, cris"),
    ("art", "d4c5f9", "Sprites et icônes"),
    ("net", "bfd4f2", "Réseau LAN (bonus)"),
    ("infra", "ededed", "Build, dépôt, CI"),
    ("docs", "0075ca", "GDD, UML, Javadoc, README, soutenance"),
    ("test", "c5def5", "Tests et couverture"),
    ("bug", "d73a4a", "Correctif"),
    ("retro-action", "bfdadc", "Action décidée en rétrospective"),
    ("dev:A", "2a78d6", "Dev A · cœur du jeu & données"),
    ("dev:B", "eb6834", "Dev B · expérience joueur & présentation"),
    ("P0", "b60205", "Indispensable"),
    ("P1", "fbca04", "Important, saute en premier en cas de retard"),
    ("P2", "0e8a16", "Bonus, seulement avec du temps libéré"),
    ("sp:1", "e4e4e4", "1 point ≈ 2 h"),
    ("sp:2", "d4d4d4", "2 points ≈ 4 h"),
    ("sp:3", "c4c4c4", "3 points ≈ 6 h"),
    ("sp:5", "b4b4b4", "5 points ≈ 10 h"),
    ("sp:8", "a4a4a4", "8 points : à découper"),
]

# --------------------------------------------------------------------------
# Issues : (numéro, sprint, label, titre, dev, points, priorité, dépendances, story, critères)
# 1 point ≈ 2 h de travail effectif, apprentissage compris.
# --------------------------------------------------------------------------
ISSUES = [
    # ---- Sprint 0 · Conception
    (1, "S0", "infra", "Squelette du dépôt : projet Maven JavaFX qui ouvre une fenêtre, JUnit 5, packages", "B", 1, "P0", [],
     "En tant que dev, je veux un projet Maven qui ouvre une fenêtre JavaFX, afin que nous partions tous les deux de la même base.",
     ["Projet créé comme dans le bootstrap JavaFX (VS Code : « Create Java Project » puis JavaFX)",
      "`mvn javafx:run` ouvre une fenêtre vide",
      "`mvn test` exécute un premier test JUnit 5",
      "Packages `core.api`, `core.model`, `core.engine`, `core.effects`, `core.ai`, `data`, `ui` créés",
      "`.gitignore` (target/, fichiers .class, dossiers d'IDE) et README minimal"]),
    (2, "S0", "docs", "GDD · vision, boucle de jeu, maquettes des écrans (dont le dashboard), direction artistique et sonore", "B", 3, "P0", [],
     "En tant que Product Owner, je veux un GDD qui décrit l'expérience joueur, afin que nous partagions la même vision avant de coder.",
     ["Vision, public visé et boucle de jeu (menu → tirage → combat → fin)",
      "Maquettes de chaque écran : menu, aperçu des équipes, combat, panneau d'équipe, fin, dashboard (photos de croquis acceptées)",
      "Direction artistique : palette, sprites 32×32 affichés ×3 ; sprite de dos par défaut = face retournée",
      "Son : musiques libres de droits, effets et cris générés avec jsfxr",
      "Relu et validé par Dev A à la review du mercredi 4"]),
    (3, "S0", "docs", "GDD · règles de combat, 10 types, formule de dégâts, talents, objets, liste des 30 fakemon", "A", 3, "P0", [],
     "En tant que joueur, je veux des règles claires et équilibrées, afin que chaque choix en combat compte.",
     ["Les 10 types et leur table d'efficacité",
      "Formule de dégâts simple, avec un exemple chiffré",
      "Ordre de résolution d'un tour : priorité, vitesse, égalité",
      "4 talents et 3 objets tenus, avec leurs effets",
      "Liste des 30 fakemon : nom, nom français, types, concept ; relue et validée par Dev B"]),
    (4, "S0", "core", "Contrat d'API : PlayerAction, BattleEvent, BattleListener, BattleEngine, AiController, SpeciesRepository", "A", 2, "P0", [2, 3],
     "En tant que devs, nous voulons définir ensemble les classes qui relient le cœur et l'interface, afin de coder chacun de notre côté sans nous bloquer.",
     ["Défini à deux pendant l'atelier du mardi 3 (2 h)",
      "`PlayerAction` (classe abstraite) avec `UseMove` et `Switch` ; `BattleEvent` (classe abstraite) avec une sous-classe par event : dégâts, KO, entrée en jeu, talent, objet, fin de combat",
      "`BattleEngine.playTurn(actionJoueur, actionIA)` et l'interface `BattleListener` (Observer, comme au jour 08)",
      "Interfaces `AiController` et `SpeciesRepository`, avec une implémentation en mémoire de `SpeciesRepository` pour démarrer",
      "Javadoc sur chaque classe ; `CODEOWNERS` : A et B sur `core.api`"]),
    (5, "S0", "docs", "UML v1 · cas d'utilisation et navigation entre les écrans", "B", 1, "P0", [2],
     "En tant qu'équipe, nous voulons modéliser les parcours du joueur, afin de savoir quels écrans construire.",
     ["Cas d'utilisation : jouer contre l'IA, ajouter un fakemon",
      "Diagramme d'états de la navigation entre les écrans",
      "Sources PlantUML dans `docs/uml/` et PNG générés"]),
    # ---- Sprint 1 · Fondations
    (6, "S1", "docs", "UML v1 · diagramme de classes du cœur + séquence d'un tour", "A", 1, "P0", [4],
     "En tant qu'équipe, nous voulons une vue d'ensemble du cœur, afin de valider la conception avant d'écrire le moteur.",
     ["Diagramme de classes : modèle, moteur, IA, avec les patterns annotés",
      "Diagramme de séquence d'un tour : clic du joueur → `playTurn` → events → écran",
      "Sources PlantUML dans `docs/uml/` et PNG générés"]),
    (7, "S1", "infra", "CI GitHub Actions : build et tests à chaque PR, rapport de couverture JaCoCo", "A", 2, "P0", [1],
     "En tant qu'équipe, nous voulons que les tests tournent seuls à chaque PR, afin de voir tout de suite si quelque chose casse.",
     ["Workflow créé à partir du modèle GitHub « Java with Maven », déclenché sur chaque PR et sur `main`",
      "La protection de `main` exige que ce workflow passe",
      "Rapport JaCoCo généré et publié en artefact",
      "Badge de la CI dans le README"]),
    (8, "S1", "core", "Modèle : Type, Stats, Species, Fakemon, Move, Team", "A", 3, "P0", [4],
     "En tant que dev du moteur, je veux séparer l'espèce (données fixes) du fakemon en combat (PV, PP), afin qu'un combat ne modifie jamais le roster.",
     ["`Species` : nom, nom français, types, stats, attaques possibles",
      "`Fakemon` : une espèce + PV courants + 4 attaques avec leurs PP",
      "`Type` en enum, comme `Animal.Type` au jour 04",
      "`Team` de 6 fakemon au plus, exception sinon",
      "Tests JUnit de ces règles"]),
    (9, "S1", "core", "Table des types + calcul des dégâts (STAB, critique, précision, aléatoire injectable)", "A", 3, "P0", [8],
     "En tant que joueur, je veux que le type des attaques compte, afin d'avoir des choix tactiques.",
     ["Efficacité calculée pour toute paire attaquant / défenseur, double type compris",
      "Formule de dégâts conforme au GDD (#3)",
      "Le `Random` est passé au constructeur : les tests fixent la graine et sont reproductibles",
      "Tests : bonus de même type, super efficace, immunité, critique, attaque ratée"]),
    (10, "S1", "core", "Générateur d'équipes : 2 × 6 tirés dans le roster, 4 attaques tirées, seed", "A", 1, "P0", [8],
     "En tant que joueur, je veux une équipe différente à chaque combat, afin que chaque partie soit nouvelle.",
     ["Deux équipes de 6 sans doublon, tirées dans tout le pool (roster + fakemon ajoutés)",
      "4 attaques tirées parmi les attaques possibles de chaque fakemon",
      "Même graine, mêmes équipes (test)",
      "Exception claire si le pool contient moins de 12 fakemon"]),
    (11, "S1", "ai", "IA aléatoire : une attaque au hasard parmi les coups possibles", "A", 1, "P0", [4, 8],
     "En tant que joueur, je veux un premier adversaire, afin de pouvoir jouer un combat de bout en bout.",
     ["Implémente `AiController`",
      "Choisit une attaque qui a encore des PP",
      "Après un KO, choisit un remplaçant encore apte",
      "Test : 1 000 choix sans action impossible"]),
    (12, "S1", "ui", "SceneManager + menu principal", "B", 2, "P0", [1],
     "En tant que joueur, je veux naviguer entre les écrans depuis un menu, afin d'accéder au combat et au dashboard.",
     ["`SceneManager` change d'écran ; aucun écran ne crée directement les autres",
      "Menu : Combat contre l'IA, Dashboard, Options, Quitter",
      "Retour au menu possible depuis chaque écran"]),
    (13, "S1", "ui", "Écran de combat sur données factices : sprites, barres de PV, 4 attaques, journal", "B", 5, "P0", [4, 12],
     "En tant que joueur, je veux voir les deux fakemon, leurs PV et mes attaques, afin de choisir mon action.",
     ["Sprite de face pour l'adversaire, de dos pour le joueur",
      "Cartouches : nom, types, barre de PV colorée (vert, orange, rouge)",
      "4 boutons d'attaque avec type et PP, désactivés à 0 PP",
      "Journal de combat qui défile",
      "Alimenté par une liste d'events factices créés avec les classes de #4 ; une méthode `onEvent(BattleEvent)` prête pour #26"]),
    (14, "S1", "ui", "Écran d'aperçu des équipes (6 contre 6)", "B", 1, "P0", [12],
     "En tant que joueur, je veux découvrir les deux équipes avant le combat, afin de préparer ma stratégie.",
     ["Les 6 fakemon de chaque camp : sprite, nom français, types",
      "Bouton « Combattre » qui ouvre l'écran de combat"]),
    (15, "S1", "ui", "Écran de fin de combat + revanche", "B", 1, "P0", [12],
     "En tant que joueur, je veux savoir clairement qui a gagné et pouvoir rejouer, afin d'enchaîner les parties.",
     ["Victoire ou défaite, avec le nombre de tours",
      "« Revanche » relance un tirage, « Menu » revient au menu"]),
    (16, "S1", "art", "Charte graphique + sprites lot 1 : 15 fakemon de face (32×32) + icônes des 10 types", "B", 2, "P0", [3],
     "En tant que joueur, je veux des fakemon au style cohérent, afin que le jeu paraisse soigné.",
     ["Charte dans `docs/art.md` : palette réduite, 32×32, affichage ×3 sans lissage",
      "15 fakemon de face, 20 minutes maximum par sprite",
      "Icônes des 10 types",
      "Convention de fichiers : `sprites/front/<id>.png`, `sprites/back/<id>.png` (facultatif)"]),
    # ---- Sprint 2 · Moteur & dashboard
    (17, "S2", "core", "Moteur de tour : playTurn, priorité puis vitesse, KO, victoire et défaite", "A", 5, "P0", [8, 9],
     "En tant que joueur, je veux que les actions des deux camps se résolvent dans un ordre juste, afin que la vitesse et la priorité des attaques comptent vraiment.",
     ["`playTurn(actionJoueur, actionIA)` résout le tour complet",
      "Priorité d'abord, puis vitesse ; en cas d'égalité, tirage avec la graine",
      "Un fakemon mis KO avant son tour n'agit pas",
      "Chaque étape envoie un `BattleEvent` aux `BattleListener`, dans l'ordre",
      "`BattleEnded` est émis quand une équipe n'a plus de fakemon apte ; tests pour chaque critère"]),
    (18, "S2", "core", "Switch volontaire et remplacement forcé après KO", "A", 3, "P0", [17],
     "En tant que joueur, je veux changer de fakemon en combat, afin de répondre à un mauvais matchup.",
     ["Un switch volontaire est résolu avant les attaques",
      "Après un KO, `needsReplacement()` puis `replace(slot)` avant le tour suivant",
      "Impossible de switcher vers un fakemon KO ou déjà en jeu",
      "Tests, double KO compris"]),
    (19, "S2", "ui", "Panneau d'équipe : switch et choix forcé après KO", "B", 2, "P0", [13],
     "En tant que joueur, je veux voir mon équipe et choisir qui envoyer, afin de gérer mes switchs.",
     ["Les 6 fakemon du joueur avec leurs PV",
      "Switch volontaire pendant un tour normal",
      "Choix forcé et non annulable après un KO",
      "Fakemon KO ou déjà en jeu grisés ; fonctionne sur données factices jusqu'à #26"]),
    (20, "S2", "ui", "Dashboard : nom, nom FR, sprites avant/arrière, cri, attaques disponibles, types, stats", "B", 5, "P0", [4, 12],
     "En tant que membre de l'équipe, je veux un écran pour ajouter un fakemon, afin d'enrichir le jeu sans toucher au code.",
     ["Champs nom et nom français",
      "Sprite avant et sprite arrière choisis avec un `FileChooser`, avec aperçu ; sans sprite arrière, la face retournée est utilisée",
      "Cri choisi avec un `FileChooser`, avec un bouton d'écoute",
      "Attaques disponibles dans une liste filtrable à cocher ; types (1 ou 2) et 6 stats avec le total",
      "Erreurs de validation (exception levée par `SpeciesRepository`) affichées à l'écran ; enregistrement en mémoire jusqu'à #22"]),
    (21, "S2", "art", "Sprites lot 2 : 15 fakemon de face", "B", 1, "P0", [16],
     "En tant que joueur, je veux un sprite pour chaque fakemon, afin qu'aucun ne reste en placeholder.",
     ["Les 15 fakemon restants (30 au total), selon la charte de #16"]),
    # ---- Sprint 3 · Intégration & release
    (22, "S3", "data", "SpeciesRepository JSON : lecture/écriture, Factory, validation, dossier utilisateur", "A", 5, "P0", [4, 8],
     "En tant que membre de l'équipe, je veux que les fakemon ajoutés soient sauvegardés et rechargés, afin qu'ils entrent dans le tirage.",
     ["Lecture avec Gson des espèces fournies (ressources) et du dossier utilisateur `~/.fakemon-arena/`",
      "`save()` copie les sprites et le cri, puis écrit le JSON",
      "Validation : noms uniques, 1 ou 2 types, au moins 4 attaques, total des stats ≤ 600, fichiers présents ; sinon `InvalidSpeciesException` avec un message clair",
      "Factory : création des `Species` et `Move` à partir du JSON, comme au jour 08",
      "Tests dans un dossier temporaire (`@TempDir`)"]),
    (23, "S3", "data", "Roster : 30 fakemon + ~30 attaques, saisis via le dashboard", "A", 2, "P0", [20, 22],
     "En tant que joueur, je veux 30 fakemon variés, afin que chaque tirage donne des combats différents.",
     ["30 fakemon conformes au GDD (#3)",
      "Environ 30 attaques, 3 par type",
      "Saisis via le dashboard ; repli : JSON écrit à la main si le dashboard n'est pas prêt",
      "Chargement sans erreur de validation"]),
    (24, "S3", "core", "Talents (4) et objets tenus (3), objets en Decorator", "A", 3, "P1", [17],
     "En tant que joueur, je veux que mes fakemon aient un talent et un objet, afin que deux fakemon du même type jouent différemment.",
     ["Classe abstraite `Ability` et une sous-classe par talent, appelées par le moteur à l'entrée en jeu, au calcul des dégâts et en fin de tour",
      "3 objets tenus en Decorator sur les stats, comme `Shield` et `FireSword` au jour 08",
      "Events `AbilityTriggered` et `ItemTriggered` émis",
      "Un test par talent et par objet"]),
    (25, "S3", "docs", "Javadoc du cœur + UML final + couverture ≥ 70 %", "A", 2, "P0", [],
     "En tant qu'évaluateur, je veux une documentation à jour du cœur, afin de comprendre la conception.",
     ["Javadoc sans avertissement sur `core` et `data`",
      "Diagrammes de classes et de séquence à jour avec le code final",
      "Couverture JaCoCo d'au moins 70 % sur `core`"]),
    (26, "S3", "ui", "Branchement de l'écran de combat sur le vrai moteur (BattleListener, IA, switchs)", "B", 3, "P0", [11, 17, 18, 19],
     "En tant que joueur, je veux jouer un vrai combat contre l'IA, afin de tester le jeu réel et non plus des données factices.",
     ["L'écran de combat s'abonne au moteur (`BattleListener`)",
      "Clic du joueur + choix de l'IA → `playTurn`",
      "Boutons bloqués pendant l'affichage des events",
      "Switch et remplacement forcé branchés (#18, #19)",
      "Un combat complet 6 contre 6 jouable contre l'IA"]),
    (27, "S3", "anim", "Animations : impact, PV qui descendent, KO, entrée et sortie", "B", 2, "P1", [26],
     "En tant que joueur, je veux que les actions soient animées, afin de suivre le combat sans lire tout le journal.",
     ["Flash à l'impact, PV qui descendent progressivement, disparition au KO, entrée et sortie",
      "Faites avec `Timeline` (comme dans le bootstrap JavaFX) et les transitions JavaFX",
      "Chaque animation se termine avant l'event suivant"]),
    (28, "S3", "audio", "Musiques, effets sonores et cris des fakemon (jsfxr), volume", "B", 2, "P1", [12],
     "En tant que joueur, je veux entendre le combat, afin qu'il soit plus vivant.",
     ["Musique du menu et du combat, libres de droits, crédités dans le README",
      "Effets : coup, super efficace, KO",
      "Cri joué à l'entrée en combat (30 cris générés avec jsfxr)",
      "Volume réglable dans les options"]),
    (29, "S3", "ui", "Bannières et icônes des talents et objets", "B", 1, "P1", [24, 26],
     "En tant que joueur, je veux voir quand un talent ou un objet s'active, afin de comprendre ce qui se passe.",
     ["Bannière à l'activation d'un talent ou d'un objet",
      "Icône de l'objet tenu sur la cartouche"]),
    (30, "S3", "docs", "README, Javadoc des écrans, UML final des écrans", "B", 2, "P0", [],
     "En tant que nouveau joueur, je veux savoir installer et jouer, afin de lancer une partie sans aide.",
     ["README : présentation, prérequis, build, lancement, tests, captures, crédits",
      "Javadoc sans avertissement sur `ui`",
      "Cas d'utilisation et diagramme d'états à jour"]),
    (31, "S3", "docs", "Support de soutenance + script de démo", "B", 2, "P0", [],
     "En tant qu'équipe, nous voulons une soutenance préparée, afin de montrer le projet sous son meilleur jour.",
     ["Support d'environ 20 minutes : pitch, conception, patterns, tests, démo, méthode",
      "Script de démo avec une graine fixée",
      "Répétition le vendredi 20"]),
    # ---- Bonus · seulement avec du temps libéré
    (32, "BX", "core", "Statuts : brûlure, poison, paralysie (pattern State)", "A", 3, "P2", [17],
     "En tant que joueur, je veux des statuts qui changent le cours d'un combat, afin d'avoir d'autres stratégies que frapper fort.",
     ["Classe abstraite `Status` et une sous-classe par statut",
      "Brûlure et poison : dégâts en fin de tour ; paralysie : vitesse réduite et chance de ne pas agir",
      "Events émis ; un test par statut"]),
    (33, "BX", "ai", "IA heuristique : choisit l'attaque aux dégâts attendus les plus élevés", "A", 2, "P2", [11, 17],
     "En tant que joueur, je veux une IA qui joue intelligemment, afin que le combat soit un vrai défi.",
     ["Choisit l'attaque aux dégâts attendus les plus élevés (efficacité, bonus de même type, précision)",
      "Gagne au moins 70 % de 1 000 combats simulés contre l'IA aléatoire"]),
    (34, "BX", "net", "Spike réseau (timebox 2 h) : deux programmes échangent un event par socket", "B", 1, "P2", [],
     "En tant que dev, je veux tester le réseau en petit, afin de décider en connaissance de cause si le LAN est faisable.",
     ["Deux programmes échangent un event sérialisé en JSON (Gson) via `ServerSocket` et `Socket`",
      "Compte rendu dans `docs/net-spike.md` : ce qui a marché, ce qui reste à apprendre",
      "Sert au Go / No-Go du lundi 16"]),
    (35, "BX", "net", "Combat en LAN : hôte, invité, lobby (timebox 10 h)", "B", 5, "P2", [26, 34],
     "En tant que joueur, je veux affronter un ami sur le réseau local, afin de jouer à deux.",
     ["Sur une branche à part, timebox de 10 h",
      "L'hôte fait tourner le moteur ; l'invité envoie ses actions et reçoit les events (un thread de lecture + `Platform.runLater`)",
      "Écran lobby : héberger ou rejoindre par adresse IP",
      "Mergé seulement si un combat complet passe avant le jeudi 19 à 18 h"]),
]

DEV_NAMES = {"A": "Dev A · cœur du jeu & données", "B": "Dev B · expérience joueur & présentation"}
BRANCH_PREFIX = {"infra": "chore", "docs": "docs", "test": "test", "bug": "fix"}


# --------------------------------------------------------------------------
# Construction des issues
# --------------------------------------------------------------------------
def slug(title, max_len=40):
    """Nom de branche lisible : sans accents, en minuscules, coupé entre deux mots."""
    head = title.split(":")[0].split("(")[0].replace("/", " ").replace("'", " ")
    head = head.lower().replace("œ", "oe").replace("æ", "ae")
    ascii_text = unicodedata.normalize("NFKD", head).encode("ascii", "ignore").decode()
    words = [w for w in ("".join(c for c in w if c.isalnum()) for w in ascii_text.split()) if w]
    out = ""
    for w in words:
        if out and len(out) + 1 + len(w) > max_len:
            break
        out = f"{out}-{w}" if out else w
    while "-" in out and out.rsplit("-", 1)[1] in {"de", "du", "des", "et", "la", "le", "les", "a", "en"}:
        out = out.rsplit("-", 1)[0]
    return out or "issue"


def issue_title(issue):
    num, sprint, label, title, *_ = issue
    return f"[{label.upper()}] {title}"


def issue_body(issue):
    num, sprint, label, title, dev, points, prio, deps, story, criteria = issue
    ms_title, _, dates, _ = SPRINTS[sprint]
    branch = f"{BRANCH_PREFIX.get(label, 'feat')}/{num}-{slug(title)}"
    deps_txt = ", ".join(f"#{d}" for d in deps) if deps else "aucune"
    lines = [
        f"**Sprint** : {ms_title} ({dates})",
        f"**Responsable** : {DEV_NAMES[dev]} · **Points** : {points} (≈ {points * 2} h) · **Priorité** : {prio}",
        f"**Dépend de** : {deps_txt}",
        "",
        "### Story",
        story,
        "",
        "### Critères d'acceptation",
        *[f"- [ ] {c}" for c in criteria],
        "",
        "### Definition of Done",
        f"- [ ] Branche `{branch}`, PR liée (« Closes #{num} »)",
        "- [ ] Tests de la logique ajoutée, CI verte",
        "- [ ] Javadoc des classes et méthodes publiques",
        "- [ ] PR approuvée par l'autre dev, squash-merge sur `main`",
        "- [ ] Démontrable depuis `main` ; UML mis à jour si l'architecture a changé",
    ]
    return "\n".join(lines)


def issue_labels(issue):
    num, sprint, label, title, dev, points, prio, *_ = issue
    return [label, f"dev:{dev}", prio, f"sp:{points}"]


# --------------------------------------------------------------------------
# Appels GitHub via gh
# --------------------------------------------------------------------------
class GhError(RuntimeError):
    pass


def gh(*args, payload=None):
    """Lance gh et renvoie stdout. Le JSON passe par stdin pour éviter tout souci d'accents."""
    cmd = ["gh", *args]
    data = None
    if payload is not None:
        cmd += ["--input", "-"]
        data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    res = subprocess.run(cmd, input=data, capture_output=True)
    if res.returncode != 0:
        raise GhError(f"{' '.join(args[:3])} : {res.stderr.decode('utf-8', 'replace').strip()}")
    return res.stdout.decode("utf-8")


def gh_lines(path, jq):
    out = gh("api", "--paginate", path, "--jq", jq)
    return [json.loads(line) for line in out.splitlines() if line.strip()]


def ensure_labels(repo):
    existing = {l["name"].lower(): l["name"] for l in gh_lines(f"repos/{repo}/labels?per_page=100", ".[] | {name}")}
    for name, color, desc in LABELS:
        payload = {"color": color, "description": desc}
        if name.lower() in existing:
            gh("api", "-X", "PATCH", f"repos/{repo}/labels/{quote(existing[name.lower()], safe='')}",
               payload={"new_name": name, **payload})
            print(f"  label mis à jour : {name}")
        else:
            gh("api", "-X", "POST", f"repos/{repo}/labels", payload={"name": name, **payload})
            print(f"  label créé       : {name}")


def ensure_milestones(repo):
    existing = {m["title"]: m["number"] for m in
                gh_lines(f"repos/{repo}/milestones?state=all&per_page=100", ".[] | {title, number}")}
    numbers = {}
    for sid, (title, due, dates, goal) in SPRINTS.items():
        payload = {"title": title, "description": f"{dates} · {goal}", "due_on": f"{due}T12:00:00Z"}
        if title in existing:
            numbers[sid] = existing[title]
            print(f"  milestone existant : {title}")
        else:
            numbers[sid] = json.loads(gh("api", "-X", "POST", f"repos/{repo}/milestones", payload=payload))["number"]
            print(f"  milestone créé     : {title}")
    return numbers


def check_assignee(repo, login):
    try:
        gh("api", f"repos/{repo}/assignees/{login}")
    except GhError:
        sys.exit(f"Erreur : {login} ne peut pas être assigné sur {repo} (pas collaborateur ?).")


def create_issues(repo, milestones, assignees):
    # issues et PR partagent la même numérotation : on les récupère toutes
    existing = gh_lines(f"repos/{repo}/issues?state=all&per_page=100", ".[] | {number, title}")
    by_title = {i["title"]: i["number"] for i in existing}
    planned = {issue_title(i): i[0] for i in ISSUES}

    for item in existing:
        expected = planned.get(item["title"])
        if expected != item["number"]:
            sys.exit(f"Erreur : #{item['number']} « {item['title']} » existe déjà et décalerait la numérotation.\n"
                     "Lancez le script sur un dépôt sans issue ni PR pour que les numéros suivent le plan.")

    last = max((i["number"] for i in existing), default=0)
    for issue in ISSUES:
        num, title = issue[0], issue_title(issue)
        if by_title.get(title) == num:
            print(f"  #{num:<2} déjà présente")
            continue
        if last != num - 1:
            sys.exit(f"Erreur : la prochaine issue aurait le numéro {last + 1}, le plan attend #{num}.")
        payload = {"title": title, "body": issue_body(issue), "labels": issue_labels(issue),
                   "milestone": milestones[issue[1]]}
        if assignees.get(issue[4]):
            payload["assignees"] = [assignees[issue[4]]]
        created = json.loads(gh("api", "-X", "POST", f"repos/{repo}/issues", payload=payload))
        if created["number"] != num:
            sys.exit(f"Erreur : l'issue « {title} » a reçu le numéro #{created['number']} au lieu de #{num} "
                     "(une issue supprimée consomme quand même son numéro). Arrêt.")
        last = num
        print(f"  #{num:<2} créée : {title}")


# --------------------------------------------------------------------------
def main():
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    except AttributeError:
        pass
    ap = argparse.ArgumentParser(description="Crée labels, milestones et issues du projet Fakemon Arena.")
    ap.add_argument("--repo", help="owner/nom (par défaut : le dépôt du dossier courant)")
    ap.add_argument("--dev-a", help="login GitHub de Dev A, assigné à ses issues")
    ap.add_argument("--dev-b", help="login GitHub de Dev B, assigné à ses issues")
    ap.add_argument("--dry-run", action="store_true", help="affiche ce qui serait créé, sans appeler GitHub")
    ap.add_argument("--preview", type=int, metavar="N", help="avec --dry-run : affiche le corps de l'issue #N")
    args = ap.parse_args()

    if args.dry_run:
        print(f"{len(LABELS)} labels : {', '.join(l[0] for l in LABELS)}\n")
        for sid, (title, due, dates, _) in SPRINTS.items():
            nums = [i[0] for i in ISSUES if i[1] == sid]
            print(f"{title} (échéance {due}) : #{nums[0]} à #{nums[-1]}")
        print()
        for issue in ISSUES:
            print(f"#{issue[0]:<2} {issue_title(issue)}\n     {', '.join(issue_labels(issue))}")
        if args.preview:
            issue = next((i for i in ISSUES if i[0] == args.preview), None)
            if issue is None:
                sys.exit(f"Pas d'issue #{args.preview} dans le plan.")
            print(f"\n----- corps de #{issue[0]} -----\n{issue_body(issue)}")
        return

    if not shutil.which("gh"):
        sys.exit("GitHub CLI introuvable. Installez-le (winget install --id GitHub.cli), puis lancez gh auth login.")
    try:
        repo = args.repo or json.loads(gh("repo", "view", "--json", "nameWithOwner"))["nameWithOwner"]
        assignees = {"A": args.dev_a, "B": args.dev_b}
        for login in filter(None, assignees.values()):
            check_assignee(repo, login)
        print(f"Dépôt : {repo}\n\nLabels")
        ensure_labels(repo)
        print("\nMilestones")
        milestones = ensure_milestones(repo)
        print("\nIssues")
        create_issues(repo, milestones, assignees)
    except GhError as err:
        sys.exit(f"Erreur gh : {err}")
    print(f"\nTerminé : {len(ISSUES)} issues dans {len(SPRINTS)} milestones sur {repo}.")


if __name__ == "__main__":
    main()
