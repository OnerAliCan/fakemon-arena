#!/bin/bash
# Met en place le projet Fakemon Arena sur macOS : bash setup-mac.command dans le Terminal,
# avec des options si besoin, par exemple : bash setup-mac.command --repo alice/fakemon-arena
cd "$(dirname "$0")" || exit 1
export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"

if ! python3 --version >/dev/null 2>&1; then
    echo "Python 3 est introuvable : macOS va proposer les outils de développement Apple."
    echo "Acceptez l'installation, puis relancez : bash setup-mac.command"
    xcode-select --install
    exit 1
fi

python3 scripts/setup_project.py "$@"
