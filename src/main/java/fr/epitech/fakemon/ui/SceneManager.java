package fr.epitech.fakemon.ui;

import fr.epitech.fakemon.core.api.Side;
import fr.epitech.fakemon.core.api.BattleEndReason;
import fr.epitech.fakemon.core.model.Team;
import javafx.scene.Parent;
import javafx.scene.Scene;

/**
 * Centralise la navigation entre les écrans du jeu.
 */
public final class SceneManager {

    private final Scene scene;

    /**
     * Crée le gestionnaire pour la scène du jeu.
     *
     * @param scene scène dont le contenu sera remplacé
     */
    public SceneManager(Scene scene) {
        this.scene = scene;
    }

    /**
     * Affiche un écran dans la scène du jeu.
     *
     * @param screen conteneur principal de l'écran à afficher
     */
    public void show(Parent screen) {
        scene.setRoot(screen);
    }
    /**
     * Lance une nouvelle démonstration avec deux équipes fraîchement tirées.
     * @param mainMenu menu existant à retrouver à la fin
     */
    public void startDemoBattle(MainMenu mainMenu) {
        show(new BattleScreen(this, mainMenu));
    }

    /**
     * Affiche le bilan et centralise les destinations de ses boutons.
     * @param winner camp vainqueur
     * @param reason raison de fin
     * @param turns nombre de tours
     * @param playerTeam équipe finale du joueur
     * @param mainMenu menu existant
     */
    public void showBattleResult(Side winner,
            BattleEndReason reason, int turns,
            Team playerTeam, MainMenu mainMenu) {
        show(new BattleEndScreen(winner, reason, turns, playerTeam,
                () -> startDemoBattle(mainMenu), () -> show(mainMenu)));
    }
}