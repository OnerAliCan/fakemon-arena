package fr.epitech.fakemon;

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
