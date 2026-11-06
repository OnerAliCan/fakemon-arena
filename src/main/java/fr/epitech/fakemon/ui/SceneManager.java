package fr.epitech.fakemon.ui;

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
}