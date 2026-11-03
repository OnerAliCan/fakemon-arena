# -*- coding: utf-8 -*-
"""Met en place tout le projet Fakemon Arena d'un coup, sur Windows ou macOS.

Premier lancement, par une seule personne : installe les outils, crée le dépôt GitHub avec
le squelette Maven JavaFX (#1) et la CI (#7), règle le dépôt (squash-merge, main protégée),
invite le binôme, crée le backlog (labels, milestones, 35 issues) et le tableau GitHub Projects.

Sur le poste du binôme, ou pour relancer : installe les outils, accepte l'invitation, clone
le dépôt, assigne ses issues et vérifie que le projet compile. Les étapes déjà faites sont
sautées : on peut relancer le script après une coupure.

Usage :
    setup-windows.bat                       (double-clic)
    bash setup-mac.command                  (dans le Terminal)
    python scripts/setup_project.py [--repo owner/nom] [--dir dossier] [--role A|B] [--binome login]
    python scripts/setup_project.py --local # essai sans GitHub : outils, squelette et build
"""
import argparse
import glob
import hashlib
import io
import json
import os
import re
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path
from urllib.parse import quote

HERE = Path(__file__).resolve().parent      # scripts/
ROOT = HERE.parent                           # dossier des lanceurs (racine du dépôt une fois cloné)
IS_WIN = os.name == "nt"
IS_MAC = sys.platform == "darwin"

PACKAGE = "fr.epitech.fakemon"
DEFAULT_REPO_NAME = "fakemon-arena"
DESCRIPTION = "Combats de fakemon au tour par tour · Java 21 + JavaFX · projet Epitech"
BOARD_TITLE = "Fakemon Arena"
PLAN_PDF = "Fakemon_Arena_plan_projet.pdf"
JDK_MIN = 21
MAVEN_VERSION = "3.9.11"
WRAPPER_VERSION = "3.3.2"
CENTRAL = "https://repo.maven.apache.org/maven2"
REQUIRED_SCOPES = ("repo", "workflow", "project")

# outil -> (nom affiché, id winget, commande Homebrew)
TOOLS = {
    "git": ("Git", "Git.Git", ["brew", "install", "git"]),
    "jdk": ("Java 21 (JDK)", "Microsoft.OpenJDK.21", ["brew", "install", "--cask", "temurin@21"]),
    "gh": ("GitHub CLI", "GitHub.cli", ["brew", "install", "gh"]),
}

ENV = dict(os.environ)
ASSUME_YES = False


# --------------------------------------------------------------------------
# Affichage et questions
# --------------------------------------------------------------------------
def step(title):
    print(f"\n=== {title} ===")


def ok(msg):
    print(f"  [ok] {msg}")


def warn(msg):
    print(f"  [!]  {msg}")


def fail(msg):
    sys.exit(f"\n[ERREUR] {msg}")


def ask(question, default=""):
    if ASSUME_YES:
        return default
    hint = f" [{default}]" if default else ""
    try:
        answer = input(f"  ? {question}{hint} : ").strip()
    except EOFError:
        answer = ""
    return answer or default


def confirm(question, default=True):
    if ASSUME_YES:
        return default
    answer = ask(f"{question} ({'O/n' if default else 'o/N'})").lower()
    return default if not answer else answer in ("o", "oui", "y", "yes")


# --------------------------------------------------------------------------
# Commandes
# --------------------------------------------------------------------------
def which(name):
    return shutil.which(str(name), path=ENV.get("PATH"))


def run(cmd, cwd=None, capture=False, check=True, data=None):
    """Lance une commande avec le PATH et le JAVA_HOME du script ; sans capture, elle reste interactive."""
    cmd = [str(c) for c in cmd]
    exe = which(cmd[0]) or cmd[0]
    try:
        res = subprocess.run([exe, *cmd[1:]], cwd=cwd, env=ENV, input=data, capture_output=capture)
    except FileNotFoundError:
        fail(f"Commande introuvable : {cmd[0]}")
    if check and res.returncode != 0:
        err = res.stderr.decode("utf-8", "replace").strip() if capture else ""
        fail(f"« {' '.join(cmd[:4])} » a échoué (code {res.returncode}). {err}")
    return res


def out(res):
    return (res.stdout or b"").decode("utf-8", "replace").strip()


def err_text(res):
    return (res.stderr or b"").decode("utf-8", "replace").strip()


def gh(*args, payload=None, check=True, interactive=False):
    """Appelle GitHub CLI. Le JSON passe par stdin pour éviter tout souci d'accents."""
    cmd = ["gh", *args]
    data = None
    if payload is not None:
        cmd += ["--input", "-"]
        data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    return run(cmd, capture=not interactive, check=check, data=data)


def gh_json(*args, check=True):
    res = gh(*args, check=check)
    text = out(res)
    return json.loads(text) if res.returncode == 0 and text else None


def download(url):
    res = run(["curl", "-fsSL", "--retry", "2", url], capture=True, check=False)
    if res.returncode != 0:
        fail(f"Téléchargement impossible : {url} (connexion internet ?)")
    return res.stdout


# --------------------------------------------------------------------------
# Outils : Git, JDK 21, GitHub CLI
# --------------------------------------------------------------------------
def refresh_path():
    """Relit le PATH : après une installation, la console garde l'ancien jusqu'à sa fermeture."""
    if IS_MAC:
        ENV["PATH"] = os.pathsep.join(["/opt/homebrew/bin", "/usr/local/bin", ENV.get("PATH", "")])
    if not IS_WIN:
        return
    import winreg
    parts = []
    for root, key_path in ((winreg.HKEY_LOCAL_MACHINE, r"SYSTEM\CurrentControlSet\Control\Session Manager\Environment"),
                           (winreg.HKEY_CURRENT_USER, "Environment")):
        try:
            with winreg.OpenKey(root, key_path) as key:
                parts.append(os.path.expandvars(winreg.QueryValueEx(key, "Path")[0]))
        except OSError:
            pass
    parts += [r"C:\Program Files\GitHub CLI", r"C:\Program Files\Git\cmd", ENV.get("PATH", "")]
    ENV["PATH"] = os.pathsep.join(p for p in parts if p)


def install(tool):
    label, winget_id, brew_cmd = TOOLS[tool]
    if not confirm(f"{label} est introuvable. L'installer maintenant ?"):
        fail(f"{label} est nécessaire. Installez-le puis relancez le script.")
    if IS_WIN:
        if not which("winget"):
            fail("winget est introuvable : installez « App Installer » depuis le Microsoft Store, puis relancez.")
        run(["winget", "install", "--id", winget_id, "--exact", "--source", "winget"], check=False)
    elif IS_MAC:
        if not which("brew"):
            fail(f"Homebrew est nécessaire pour installer {label} : suivez https://brew.sh puis relancez.")
        run(brew_cmd, check=False)
    else:
        fail(f"Installez {label} avec le gestionnaire de paquets de votre distribution, puis relancez.")
    refresh_path()


def tool_works(cmd):
    return bool(which(cmd[0])) and run(cmd, capture=True, check=False).returncode == 0


def ensure_tool(tool, cmd):
    if not tool_works(cmd):
        install(tool)
        if not tool_works(cmd):
            fail(f"{TOOLS[tool][0]} reste introuvable. Fermez ce terminal, rouvrez-en un et relancez le script.")
    ok((out(run(cmd, capture=True)).splitlines() or [TOOLS[tool][0]])[0])


def jdk_major(home):
    javac = Path(home) / "bin" / ("javac.exe" if IS_WIN else "javac")
    if not javac.is_file():
        return None
    res = run([javac, "-version"], capture=True, check=False)
    found = re.search(r"javac (\d+)", out(res) + err_text(res))
    return int(found.group(1)) if found and res.returncode == 0 else None


def jdk_candidates():
    homes = [ENV.get("JAVA_HOME", "")]
    if IS_MAC and os.path.exists("/usr/libexec/java_home"):
        for spec in (["-v", str(JDK_MIN)], []):
            res = run(["/usr/libexec/java_home", *spec], capture=True, check=False)
            if res.returncode == 0:
                homes.append(out(res))
    if IS_WIN:
        for vendor in ("Microsoft", "Eclipse Adoptium", "Java", "Zulu", "BellSoft", "Amazon Corretto"):
            homes += sorted(glob.glob(rf"C:\Program Files\{vendor}\jdk*"), reverse=True)
    javac = which("javac")
    if javac:
        homes.append(str(Path(javac).resolve().parent.parent))
    return [h for h in homes if h]


def find_jdk():
    for home in jdk_candidates():
        major = jdk_major(home)
        if major and major >= JDK_MIN:
            return home, major
    return None, None


def ensure_jdk():
    home, major = find_jdk()
    if not home:
        install("jdk")
        home, major = find_jdk()
        if not home:
            fail("Aucun JDK 21 trouvé après l'installation. Fermez ce terminal, rouvrez-en un et relancez.")
    ENV["JAVA_HOME"] = str(home)
    ENV["PATH"] = str(Path(home) / "bin") + os.pathsep + ENV.get("PATH", "")
    ok(f"Java {major} : {home}")
    if major != JDK_MIN:
        warn(f"La CI compile avec Java {JDK_MIN} : évitez les nouveautés de Java {major}.")


def ensure_gh():
    ensure_tool("gh", ["gh", "--version"])


# --------------------------------------------------------------------------
# Compte GitHub et identité git
# --------------------------------------------------------------------------
def token_scopes():
    res = gh("api", "-i", "user", check=False)
    for line in out(res).splitlines():
        if line.lower().startswith("x-oauth-scopes:"):
            return {s.strip() for s in line.split(":", 1)[1].split(",") if s.strip()}
    return None  # jeton sans scopes affichés : rien à vérifier


def github_login():
    if gh("auth", "status", "-h", "github.com", check=False).returncode != 0:
        print("  Connexion à GitHub : un code va s'afficher, collez-le dans la page qui s'ouvre.")
        gh("auth", "login", "-h", "github.com", "-p", "https", "-w", "-s", "workflow,project", interactive=True)
    scopes = token_scopes()
    missing = [s for s in REQUIRED_SCOPES if scopes is not None and s not in scopes]
    if missing:
        print(f"  Droits GitHub manquants ({', '.join(missing)}) : GitHub va demander une confirmation.")
        gh("auth", "refresh", "-h", "github.com", "-s", ",".join(missing), interactive=True)
    gh("auth", "setup-git", check=False)
    user = gh_json("api", "user")
    ok(f"Connecté à GitHub : {user['login']}")
    return user


def ensure_git_identity(user):
    name = out(run(["git", "config", "--global", "user.name"], capture=True, check=False))
    email = out(run(["git", "config", "--global", "user.email"], capture=True, check=False))
    if not name:
        default = (user.get("name") or user["login"]) if user else ""
        name = ask("Nom affiché dans tes commits", default)
        if not name:
            fail("Un nom est nécessaire pour les commits git.")
        run(["git", "config", "--global", "user.name", name])
    if not email:
        default = f"{user['id']}+{user['login']}@users.noreply.github.com" if user else ""
        email = ask("E-mail de tes commits (l'adresse noreply garde la tienne privée)", default)
        if not email:
            fail("Un e-mail est nécessaire pour les commits git.")
        run(["git", "config", "--global", "user.email", email])
    ok(f"Auteur des commits : {name} <{email}>")


# --------------------------------------------------------------------------
# Dépôt : création, clonage
# --------------------------------------------------------------------------
def local_team():
    """scripts/team.json, présent quand le script tourne depuis un clone ou un ZIP du dépôt."""
    path = HERE / "team.json"
    return json.loads(path.read_text(encoding="utf-8")) if path.is_file() else None


def repo_info(repo):
    return gh_json("repo", "view", repo, "--json", "nameWithOwner,url,viewerPermission,isEmpty", check=False)


def remote_url(repo):
    return f"https://github.com/{repo}.git"


def accept_invitation(repo):
    for inv in gh_json("api", "user/repository_invitations", check=False) or []:
        if inv["repository"]["full_name"].lower() == repo.lower():
            inviter = (inv.get("inviter") or {}).get("login", "?")
            if confirm(f"{inviter} t'invite sur {repo}. Accepter l'invitation ?"):
                gh("api", "-X", "PATCH", f"user/repository_invitations/{inv['id']}")
                ok("Invitation acceptée")


def synced_by(target):
    """Nom du service qui synchronise ce dossier (OneDrive, iCloud…), ou None."""
    for key in ("OneDrive", "OneDriveConsumer", "OneDriveCommercial"):
        if ENV.get(key):
            try:
                target.relative_to(Path(ENV[key]).resolve())
                return "OneDrive"
            except ValueError:
                pass
    for part in target.parts:
        for service in ("OneDrive", "iCloud Drive", "Mobile Documents", "Dropbox", "Google Drive"):
            if part.lower().startswith(service.lower()):
                return service
    return None


def choose_dir(args, default):
    target = Path(args.dir or ask("Dossier du projet sur ce poste", str(default))).expanduser().resolve()
    service = synced_by(target)
    if service and not confirm(f"{target} est synchronisé par {service}, ce qui abîme souvent .git et target/. "
                               "Continuer quand même ?", default=False):
        fail(f"Relancez le script avec un dossier hors synchronisation, par exemple {Path.home() / 'dev' / target.name}.")
    return target


def default_dir(repo_name):
    if (ROOT / ".git").is_dir() and (ROOT / "pom.xml").is_file():
        return ROOT          # le script tourne depuis un clone du dépôt
    return Path.home() / "dev" / repo_name


def clone(repo, target):
    if (target / ".git").is_dir():
        origin = out(run(["git", "-C", target, "remote", "get-url", "origin"], capture=True, check=False))
        if repo.lower() not in origin.lower():
            fail(f"{target} est déjà un dépôt git, mais pas celui de {repo} (origin : {origin or 'aucun'}).")
        if run(["git", "-C", target, "pull", "--ff-only", "-q"], check=False).returncode != 0:
            warn("Mise à jour impossible (modifications locales ?) : on garde la version actuelle.")
        ok(f"Déjà cloné dans {target}")
        return
    if target.exists() and any(target.iterdir()):
        fail(f"{target} existe déjà et n'est pas vide. Choisissez un autre dossier avec --dir.")
    gh("repo", "clone", repo, str(target))
    ok(f"Cloné dans {target}")


def ask_team(args, user, repo):
    role = (args.role or ask("Ton rôle dans le plan : A (cœur du jeu & données) ou B (joueur & présentation, PO)",
                             "A")).upper()
    if role not in ("A", "B"):
        fail("Le rôle doit être A ou B.")
    binome = (args.binome or "").lstrip("@")
    while not binome:
        if ASSUME_YES:
            fail("Indiquez le login GitHub du binôme avec --binome.")
        binome = ask("Login GitHub de ton binôme").lstrip("@")
    if binome.lower() == user["login"].lower():
        fail("Le binôme doit avoir son propre compte GitHub.")
    if not gh_json("api", f"users/{binome}", check=False):
        fail(f"Le compte GitHub « {binome} » n'existe pas.")
    me = user["login"]
    return {"repo": repo, "dev_a": me if role == "A" else binome, "dev_b": binome if role == "A" else me}


def prepare_local(args, user, repo, info):
    """Renvoie le dossier local du dépôt : cloné s'il existe déjà, sinon généré puis poussé."""
    target = choose_dir(args, default_dir(repo.split("/", 1)[1]))
    if info and not info["isEmpty"]:
        clone(repo, target)
        return target
    if info and info["viewerPermission"] != "ADMIN":
        fail("Le dépôt est encore vide : attends que ton binôme ait fini son setup, puis relance.")
    if (target / "pom.xml").is_file():
        ok(f"Squelette déjà présent dans {target}")
    else:
        write_skeleton(target, ask_team(args, user, repo))
        first_commit(target)
    if info is None:
        gh("repo", "create", repo, "--public" if args.public else "--private", "--description", DESCRIPTION)
        ok(f"Dépôt créé : https://github.com/{repo}")
    remotes = out(run(["git", "-C", target, "remote"], capture=True)).split()
    run(["git", "-C", target, "remote", "set-url" if "origin" in remotes else "add", "origin", remote_url(repo)])
    run(["git", "-C", target, "push", "-q", "-u", "origin", "main"])
    ok("Squelette poussé sur main")
    return target


# --------------------------------------------------------------------------
# Squelette du projet (#1) et CI (#7)
# --------------------------------------------------------------------------
PACKAGE_DOCS = [
    ("core.api", "A", "Contrat partagé entre le cœur et les écrans : actions, events, interfaces.\n"
                      " * Toute modification est approuvée par les deux devs (CODEOWNERS)."),
    ("core.model", "A", "Modèle du jeu : Type, Stats, Species, Fakemon, Move, Team."),
    ("core.engine", "A", "Moteur de combat : tours, dégâts, switchs, KO, tirage des équipes."),
    ("core.effects", "A", "Talents et objets tenus (statuts en bonus)."),
    ("core.ai", "A", "Adversaire contrôlé par l'ordinateur : aléatoire, heuristique en bonus."),
    ("data", "A", "Fakemon en JSON : SpeciesRepository, lecture, écriture et validation."),
    ("ui", "B", "Écrans JavaFX, dashboard d'ajout de fakemon, animations et son."),
    ("net", "B", "Combat en réseau local (bonus)."),
]

POM = """<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>fr.epitech</groupId>
  <artifactId>fakemon-arena</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <name>Fakemon Arena</name>

  <properties>
    <maven.compiler.release>21</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
    <javafx.version>21.0.8</javafx.version>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.openjfx</groupId>
      <artifactId>javafx-controls</artifactId>
      <version>${javafx.version}</version>
    </dependency>
    <!-- AudioClip et MediaPlayer : musiques, effets et cris -->
    <dependency>
      <groupId>org.openjfx</groupId>
      <artifactId>javafx-media</artifactId>
      <version>${javafx.version}</version>
    </dependency>
    <dependency>
      <groupId>com.google.code.gson</groupId>
      <artifactId>gson</artifactId>
      <version>2.11.0</version>
    </dependency>
    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <version>5.11.3</version>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.13.0</version>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>3.5.2</version>
      </plugin>
      <!-- mvnw javafx:run lance le jeu -->
      <plugin>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-maven-plugin</artifactId>
        <version>0.0.8</version>
        <configuration>
          <mainClass>{{PKG}}.App</mainClass>
        </configuration>
      </plugin>
      <!-- Couverture des tests : target/site/jacoco/index.html après mvnw verify -->
      <plugin>
        <groupId>org.jacoco</groupId>
        <artifactId>jacoco-maven-plugin</artifactId>
        <version>0.8.12</version>
        <executions>
          <execution>
            <id>prepare-agent</id>
            <goals><goal>prepare-agent</goal></goals>
          </execution>
          <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals><goal>report</goal></goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
"""

APP = """package {{PKG}};

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Application JavaFX de Fakemon Arena.
 * Pour l'instant, elle ouvre seulement la fenêtre du jeu : le menu arrive avec #12.
 */
public class App extends Application {

    /** Titre de la fenêtre. */
    public static final String TITLE = "Fakemon Arena";

    @Override
    public void start(Stage stage) {
        Scene scene = new Scene(new StackPane(new Label(TITLE)), 960, 540);
        stage.setTitle(TITLE);
        stage.setScene(scene);
        stage.show();
    }
}
"""

LAUNCHER = """package {{PKG}};

import javafx.application.Application;

/**
 * Point d'entrée à lancer depuis l'IDE (bouton Run sur cette classe).
 * Lancer {@link App} directement échoue avec « JavaFX runtime components are missing » :
 * une classe qui n'hérite pas d'Application contourne le problème.
 */
public final class Launcher {

    private Launcher() {
    }

    /**
     * Démarre le jeu.
     *
     * @param args arguments de la ligne de commande, transmis à JavaFX
     */
    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
"""

APP_TEST = """package {{PKG}};

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Premier test : vérifie que JUnit 5 tourne, en local comme dans la CI. */
class AppTest {

    @Test
    void titleIsFakemonArena() {
        assertEquals("Fakemon Arena", App.TITLE);
    }
}
"""

PACKAGE_INFO = """/**
 * {{DOC}}
 * <p>Responsable : Dev {{OWNER}}.</p>
 */
package {{PKG}}.{{NAME}};
"""

GITIGNORE = """# Build Maven
target/

# IDE
.idea/
*.iml
out/
bin/
.classpath
.project
.settings/
.vscode/*
!.vscode/extensions.json

# Système
.DS_Store
Thumbs.db

# Scripts Python
__pycache__/
"""

GITATTRIBUTES = """* text=auto
mvnw text eol=lf
*.sh text eol=lf
*.command text eol=lf
*.cmd text eol=crlf
*.bat text eol=crlf
*.png binary
*.jpg binary
*.gif binary
*.wav binary
*.ogg binary
*.mp3 binary
*.pdf binary
*.jar binary
"""

CI = """name: CI

on:
  push:
    branches: [main]
  pull_request:

jobs:
  build:
    name: build
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven
      - name: Compiler et lancer les tests
        run: ./mvnw -B -ntp verify
      - name: Publier le rapport de couverture
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: couverture-jacoco
          path: target/site/jacoco/
          if-no-files-found: ignore
"""

CODEOWNERS = """# Le contrat partagé entre le cœur et les écrans : toute modification est relue par l'autre dev.
/src/main/java/{{PKG_PATH}}/core/api/ @{{DEV_A}} @{{DEV_B}}
"""

PR_TEMPLATE = """Closes #

## Ce que fait la PR


## Comment tester


## Definition of Done
- [ ] Critères d'acceptation de l'issue vérifiés
- [ ] Tests de la logique ajoutée, CI verte
- [ ] Javadoc des classes et méthodes publiques
- [ ] Démontrable depuis `main` ; UML mis à jour si l'architecture a changé
"""

STORY_TEMPLATE = """---
name: Story
about: Nouvelle fonctionnalité pour le backlog
---
**Sprint** :
**Responsable** : Dev A / Dev B · **Points** : 1, 2, 3, 5 ou 8 · **Priorité** : P0 / P1 / P2
**Dépend de** : aucune

### Story
En tant que joueur, je veux … afin de …

### Critères d'acceptation
- [ ]

### Definition of Done
- [ ] Branche `feat/<numéro>-<nom>`, PR liée (« Closes #<numéro> »)
- [ ] Tests de la logique ajoutée, CI verte
- [ ] Javadoc des classes et méthodes publiques
- [ ] PR approuvée par l'autre dev, squash-merge sur `main`
"""

BUG_TEMPLATE = """---
name: Bug
about: Quelque chose ne marche pas comme prévu
labels: bug
---
### Ce qui se passe


### Ce qui devrait se passer


### Pour reproduire
1.

### Capture ou message d'erreur
"""

RETRO_TEMPLATE = """---
name: Action de rétro
about: Une amélioration décidée en rétrospective (2 au maximum par sprint)
labels: retro-action
---
**Rétro du sprint** :
**Porteur** : Dev A / Dev B

### Problème constaté


### Action


### Comment on saura que c'est fait
"""

VSCODE_EXTENSIONS = """{
  "recommendations": ["vscjava.vscode-java-pack"]
}
"""

README = """# Fakemon Arena

Combats de fakemon au tour par tour, façon Pokémon Showdown : deux équipes tirées au hasard parmi
30 fakemon originaux s'affrontent. Projet Epitech en Java 21 et JavaFX.

## Installer son poste

Le setup installe les outils (Git, Java 21, GitHub CLI), clone le dépôt et vérifie que tout compile :

- Windows : double-clic sur `setup-windows.bat`
- macOS : `bash setup-mac.command` dans le Terminal

## Lancer le jeu et les tests

| | Windows | macOS |
|---|---|---|
| Jouer | `mvnw.cmd javafx:run` | `./mvnw javafx:run` |
| Tests et couverture | `mvnw.cmd verify` | `./mvnw verify` |

Maven n'a pas besoin d'être installé : `mvnw` le télécharge la première fois.
Le rapport de couverture JaCoCo est dans `target/site/jacoco/index.html`.
Dans VS Code, lancez la classe `Launcher` (pas `App`).

## Équipe

| Rôle | Notions | GitHub |
|---|---|---|
| Dev A · Scrum Master | cœur du jeu, moteur, IA, données JSON, tests, CI | @{{DEV_A}} |
| Dev B · Product Owner | écrans JavaFX, dashboard, animations, son, soutenance | @{{DEV_B}} |

## Façon de travailler

- Le plan complet : [docs/Fakemon_Arena_plan_projet.pdf](docs/Fakemon_Arena_plan_projet.pdf)
- Une issue = une branche = une PR, relue par l'autre dev puis squash-merge sur `main`.
- `main` est protégée : pas de push direct, CI verte obligatoire.
"""


def fill(text, **values):
    for key, value in values.items():
        text = text.replace("{{" + key + "}}", value)
    return text


def skeleton_files(team):
    pkg_path = PACKAGE.replace(".", "/")
    values = {"PKG": PACKAGE, "PKG_PATH": pkg_path, "DEV_A": team["dev_a"], "DEV_B": team["dev_b"]}
    files = {
        "pom.xml": POM,
        f"src/main/java/{pkg_path}/App.java": APP,
        f"src/main/java/{pkg_path}/Launcher.java": LAUNCHER,
        f"src/test/java/{pkg_path}/AppTest.java": APP_TEST,
        ".gitignore": GITIGNORE,
        ".gitattributes": GITATTRIBUTES,
        ".github/workflows/ci.yml": CI,
        ".github/CODEOWNERS": CODEOWNERS,
        ".github/pull_request_template.md": PR_TEMPLATE,
        ".github/ISSUE_TEMPLATE/story.md": STORY_TEMPLATE,
        ".github/ISSUE_TEMPLATE/bug.md": BUG_TEMPLATE,
        ".github/ISSUE_TEMPLATE/retro-action.md": RETRO_TEMPLATE,
        ".vscode/extensions.json": VSCODE_EXTENSIONS,
        "README.md": README,
        "scripts/team.json": json.dumps(team, indent=2) + "\n",
    }
    for name, owner, doc in PACKAGE_DOCS:
        files[f"src/main/java/{pkg_path}/{name.replace('.', '/')}/package-info.java"] = \
            fill(PACKAGE_INFO, DOC=doc, OWNER=owner, NAME=name)
    # dossiers vides gardés par git : données de A, sprites, sons et styles de B, diagrammes PlantUML
    for folder in ("src/main/resources/data", "src/main/resources/sprites", "src/main/resources/audio",
                   "src/main/resources/css", "docs/uml"):
        files[f"{folder}/.gitkeep"] = ""
    return {rel: fill(text, **values) for rel, text in files.items()}


def write_skeleton(target, team):
    if target.exists() and any(p.name != ".git" for p in target.iterdir()):
        fail(f"{target} n'est pas vide. Choisissez un autre dossier avec --dir.")
    for rel, text in skeleton_files(team).items():
        path = target / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write(text)
    copy_tools(target)
    install_wrapper(target)
    ok(f"Squelette écrit dans {target}")


def copy_tools(target):
    """Le dépôt embarque le setup et le backlog, pour le binôme et pour relancer plus tard."""
    for name in ("setup_project.py", "create_backlog.py"):
        shutil.copy2(HERE / name, target / "scripts" / name)
    for launcher in ("setup-windows.bat", "setup-mac.command"):
        if (ROOT / launcher).is_file():
            shutil.copy2(ROOT / launcher, target / launcher)
    for pdf in (ROOT / PLAN_PDF, ROOT / "docs" / PLAN_PDF):
        if pdf.is_file():
            shutil.copy2(pdf, target / "docs" / PLAN_PDF)
            break


def install_wrapper(target):
    """Maven Wrapper : mvnw télécharge Maven tout seul, personne n'a besoin de l'installer."""
    url = (f"{CENTRAL}/org/apache/maven/wrapper/maven-wrapper-distribution/{WRAPPER_VERSION}/"
           f"maven-wrapper-distribution-{WRAPPER_VERSION}-only-script.zip")
    data = download(url)
    if hashlib.sha1(data).hexdigest() != download(url + ".sha1").decode().split()[0]:
        fail("Le Maven Wrapper téléchargé est corrompu : relancez le script.")
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        for name in ("mvnw", "mvnw.cmd"):
            (target / name).write_bytes(archive.read(name))
    os.chmod(target / "mvnw", 0o755)
    props = target / ".mvn" / "wrapper" / "maven-wrapper.properties"
    props.parent.mkdir(parents=True, exist_ok=True)
    with open(props, "w", encoding="utf-8", newline="\n") as f:
        f.write(f"wrapperVersion={WRAPPER_VERSION}\n"
                "distributionType=only-script\n"
                f"distributionUrl={CENTRAL}/org/apache/maven/apache-maven/{MAVEN_VERSION}/"
                f"apache-maven-{MAVEN_VERSION}-bin.zip\n")


def first_commit(target):
    git = ["git", "-C", target]
    if not (target / ".git").is_dir():
        run([*git, "init", "-q", "-b", "main"])
    run([*git, "-c", "core.safecrlf=false", "add", "-A"])  # sans l'avertissement LF/CRLF de Windows
    executables = [name for name in ("mvnw", "setup-mac.command") if (target / name).is_file()]
    run([*git, "update-index", "--chmod=+x", *executables])
    run([*git, "commit", "-q",
         "-m", "chore: squelette Maven JavaFX, CI et outillage du dépôt",
         "-m", "Généré par scripts/setup_project.py : pom.xml (JavaFX, Gson, JUnit 5, JaCoCo), "
               "Maven Wrapper, packages du plan, CI GitHub Actions, CODEOWNERS, modèles d'issue et de PR."])
    ok("Premier commit créé")


# --------------------------------------------------------------------------
# Réglages GitHub, backlog, tableau
# --------------------------------------------------------------------------
def configure_repo(repo):
    res = gh("api", "-X", "PATCH", f"repos/{repo}", check=False, payload={
        "allow_squash_merge": True, "allow_merge_commit": False, "allow_rebase_merge": False,
        "delete_branch_on_merge": True, "has_wiki": False,
        "squash_merge_commit_title": "PR_TITLE", "squash_merge_commit_message": "PR_BODY",
    })
    if res.returncode == 0:
        ok("Squash-merge uniquement, branche supprimée après le merge")
    else:
        warn(f"Réglages des merges non appliqués : {err_text(res)}")


def invite(repo, login):
    if gh("api", f"repos/{repo}/collaborators/{login}", check=False).returncode == 0:
        ok(f"{login} a déjà accès au dépôt")
        return
    res = gh("api", "-X", "PUT", f"repos/{repo}/collaborators/{login}", payload={"permission": "push"}, check=False)
    if res.returncode == 0:
        ok(f"Invitation envoyée à {login}, qui l'acceptera en lançant le setup sur son poste")
    else:
        warn(f"Invitation de {login} impossible : {err_text(res)}")


def protect_main(repo):
    res = gh("api", "-X", "PUT", f"repos/{repo}/branches/main/protection", check=False, payload={
        "required_status_checks": {"strict": False, "contexts": ["build"]},
        "enforce_admins": True,
        "required_pull_request_reviews": {"required_approving_review_count": 1, "require_code_owner_reviews": True},
        "restrictions": None,
        "required_linear_history": True,
        "allow_force_pushes": False,
        "allow_deletions": False,
    })
    if res.returncode == 0:
        ok("main protégée : PR obligatoire, CI verte et relecture par l'autre dev")
    elif "upgrade" in err_text(res).lower() or "403" in err_text(res):
        warn("Protection de main refusée : un dépôt privé sur un compte gratuit n'y a pas droit. Activez le "
             "GitHub Student Developer Pack (GitHub Pro gratuit), puis relancez le setup.")
    else:
        warn(f"Protection de main non appliquée : {err_text(res)}")


def assignable(repo, login):
    return gh("api", f"repos/{repo}/assignees/{login}", check=False).returncode == 0


def run_backlog(repo, target, team):
    cmd = [sys.executable, target / "scripts" / "create_backlog.py", "--repo", repo]
    for flag, key in (("--dev-a", "dev_a"), ("--dev-b", "dev_b")):
        if assignable(repo, team[key]):
            cmd += [flag, team[key]]
    if run(cmd, cwd=target, check=False).returncode != 0:
        fail("La création du backlog s'est arrêtée. Corrigez l'erreur ci-dessus puis relancez le setup : "
             "les issues déjà créées sont gardées.")


def list_issues(repo, label=None):
    path = f"repos/{repo}/issues?state=all&per_page=100" + (f"&labels={quote(label)}" if label else "")
    return [i for i in gh_json("api", path, check=False) or [] if "pull_request" not in i]


def setup_board(repo):
    owner = repo.split("/", 1)[0]
    listing = gh_json("project", "list", "--owner", owner, "--format", "json", "--limit", "100", check=False) or {}
    board = next((p for p in listing.get("projects", []) if p.get("title") == BOARD_TITLE), None)
    if board is None:
        board = gh_json("project", "create", "--owner", owner, "--title", BOARD_TITLE, "--format", "json",
                        check=False)
        if not board:
            warn("Tableau GitHub Projects non créé (droit « project » manquant ?) : relancez le setup plus tard.")
            return None
    number = str(board["number"])
    gh("project", "link", number, "--owner", owner, "--repo", repo, check=False)
    items = gh_json("project", "item-list", number, "--owner", owner, "--format", "json", "--limit", "500",
                    check=False) or {}
    present = {(i.get("content") or {}).get("url") for i in items.get("items", [])}
    missing = [i for i in list_issues(repo) if i["html_url"] not in present]
    failed = sum(gh("project", "item-add", number, "--owner", owner, "--url", i["html_url"], check=False).returncode != 0
                 for i in missing)
    if failed:
        warn(f"{failed} issue(s) non ajoutée(s) au tableau : relancez le setup pour réessayer.")
    ok(f"Tableau {board['url']} ({len(missing) - failed} issue(s) ajoutée(s))")
    return board["url"]


def assign_my_issues(repo, team, login):
    letter = next((l for l, key in (("A", "dev_a"), ("B", "dev_b")) if team.get(key, "").lower() == login.lower()),
                  None)
    if letter is None:
        warn(f"{login} n'apparaît pas dans scripts/team.json : aucune issue assignée.")
        return
    todo = [i for i in list_issues(repo, f"dev:{letter}") if not i.get("assignees")]
    for issue in todo:
        gh("api", "-X", "POST", f"repos/{repo}/issues/{issue['number']}/assignees",
           payload={"assignees": [login]}, check=False)
    ok(f"Dev {letter} : {len(todo)} issue(s) assignée(s) à {login}" if todo
       else f"Dev {letter} : tes issues sont déjà assignées")


# --------------------------------------------------------------------------
# Build
# --------------------------------------------------------------------------
def mvnw(target):
    if IS_WIN:
        return [target / "mvnw.cmd"]
    os.chmod(target / "mvnw", 0o755)  # le bit exécutable se perd parfois (ZIP, copie)
    return [target / "mvnw"]


def build(target):
    print("  Le premier build télécharge Maven et les dépendances : quelques minutes.")
    if run([*mvnw(target), "-B", "-ntp", "verify"], cwd=target, check=False).returncode != 0:
        fail("La compilation ou les tests échouent : lisez les erreurs ci-dessus.")
    ok("Compilation et tests OK (couverture : target/site/jacoco/index.html)")
    if not ASSUME_YES and confirm("Ouvrir la fenêtre du jeu pour vérifier JavaFX ?"):
        print("  Fermez la fenêtre pour continuer.")
        run([*mvnw(target), "-B", "-q", "javafx:run"], cwd=target, check=False)


# --------------------------------------------------------------------------
# Programme principal
# --------------------------------------------------------------------------
def local_trial(args):
    step("Essai local, sans GitHub")
    target = Path(args.dir or ask("Dossier d'essai", str(Path.home() / "dev" / f"{DEFAULT_REPO_NAME}-essai")))
    target = target.expanduser().resolve()
    if (target / "pom.xml").is_file():
        ok(f"Squelette déjà présent dans {target}")
    else:
        ensure_git_identity(None)
        write_skeleton(target, {"repo": "", "dev_a": args.role_a or "dev-a", "dev_b": args.role_b or "dev-b"})
        first_commit(target)
    step("Compilation et tests")
    build(target)
    print(f"\nEssai terminé dans {target}. Ce dossier n'est relié à aucun dépôt : supprimez-le quand vous voulez.")


def summary(target, repo, team, login, admin, board_url):
    run_cmd = "mvnw.cmd javafx:run" if IS_WIN else "./mvnw javafx:run"
    other = team["dev_b"] if team["dev_a"].lower() == login.lower() else team["dev_a"]
    print("\n=== Terminé ===")
    print(f"  Projet  : {target}")
    print(f"  Dépôt   : https://github.com/{repo}")
    if board_url:
        print(f"  Tableau : {board_url}")
    print("\nEnsuite :")
    print(f"  - Jouer : cd \"{target}\" puis {run_cmd}")
    if which("code"):
        print(f"  - Ouvrir dans VS Code : code \"{target}\" (acceptez l'extension Java proposée)")
    if admin:
        print(f"  - Pour {other} : accepter l'invitation GitHub reçue par e-mail, télécharger le ZIP du dépôt")
        print("    (bouton Code > Download ZIP), le dézipper et lancer le setup. Il clone alors le vrai dépôt.")
        if board_url:
            print(f"  - Tableau : ajoutez {other} dans Settings > Manage access, et créez une vue Board")
            print("    (New view > Board) pour avoir les colonnes Todo / In Progress / Done.")
        print("  - #1 (squelette) et #7 (CI) sont couverts par ce setup : relisez leurs critères ensemble et fermez-les.")


def parse_args():
    ap = argparse.ArgumentParser(description="Met en place le projet Fakemon Arena (Windows et macOS).")
    ap.add_argument("--repo", help="dépôt GitHub owner/nom (par défaut : <ton login>/fakemon-arena)")
    ap.add_argument("--dir", help="dossier du projet sur ce poste (par défaut : ~/dev/fakemon-arena)")
    ap.add_argument("--role", choices=["A", "B", "a", "b"], help="création : ton rôle dans le plan")
    ap.add_argument("--binome", help="création : login GitHub de ton binôme")
    ap.add_argument("--public", action="store_true", help="création : dépôt public au lieu de privé")
    ap.add_argument("--no-board", action="store_true", help="ne pas créer le tableau GitHub Projects")
    ap.add_argument("--local", action="store_true", help="essai sans GitHub : outils, squelette et build")
    ap.add_argument("--yes", action="store_true", help="répond oui / valeur par défaut à toutes les questions")
    ap.add_argument("--role-a", help=argparse.SUPPRESS)  # logins fictifs pour --local
    ap.add_argument("--role-b", help=argparse.SUPPRESS)
    return ap.parse_args()


def main():
    global ASSUME_YES
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", line_buffering=True)
    args = parse_args()
    ASSUME_YES = args.yes
    print(f"Fakemon Arena · mise en place du projet ({'Windows' if IS_WIN else 'macOS' if IS_MAC else sys.platform}, "
          f"Python {sys.version.split()[0]})")
    if not (HERE / "create_backlog.py").is_file():
        fail(f"create_backlog.py doit être à côté de ce script, dans {HERE}.")
    refresh_path()

    step("Outils")
    ensure_tool("git", ["git", "--version"])
    ensure_jdk()
    if args.local:
        local_trial(args)
        return
    ensure_gh()

    step("Compte GitHub")
    user = github_login()
    login = user["login"]
    ensure_git_identity(user)

    step("Dépôt")
    repo = args.repo or (local_team() or {}).get("repo") or ask("Dépôt GitHub (propriétaire/nom)",
                                                                f"{login}/{DEFAULT_REPO_NAME}")
    if not re.fullmatch(r"[\w.-]+/[\w.-]+", repo):
        fail("Format attendu : propriétaire/nom, par exemple alice/fakemon-arena.")
    accept_invitation(repo)
    info = repo_info(repo)
    if info is None:
        owner = repo.split("/", 1)[0]
        if owner.lower() != login.lower() and (gh_json("api", f"users/{owner}", check=False) or {}).get("type") != "Organization":
            fail(f"Pas d'accès à {repo}. Si c'est le dépôt de ton binôme, demande-lui l'invitation, puis relance.")
        if not confirm(f"{repo} n'existe pas encore. Le créer ({'public' if args.public else 'privé'}) avec le squelette ?"):
            fail("Rien n'a été créé.")
    target = prepare_local(args, user, repo, info)
    info = repo_info(repo)
    team = json.loads((target / "scripts" / "team.json").read_text(encoding="utf-8"))
    admin = info["viewerPermission"] == "ADMIN"

    board_url = None
    step("Réglages GitHub")
    if admin:
        configure_repo(repo)
        invite(repo, team["dev_b"] if team["dev_a"].lower() == login.lower() else team["dev_a"])
        protect_main(repo)
        step("Backlog")
        run_backlog(repo, target, team)
        if not args.no_board:
            step("Tableau GitHub Projects")
            board_url = setup_board(repo)
    else:
        ok("Faits par le propriétaire du dépôt")
    assign_my_issues(repo, team, login)

    step("Compilation et tests")
    build(target)
    summary(target, repo, team, login, admin, board_url)


if __name__ == "__main__":
    main()
