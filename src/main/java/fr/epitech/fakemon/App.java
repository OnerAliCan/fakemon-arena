package fr.epitech.fakemon;

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
