package fr.epitech.fakemon;

import fr.epitech.fakemon.ui.MainMenu;
import fr.epitech.fakemon.ui.SceneManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/** Application JavaFX de Fakemon Arena. */
public class App extends Application {

    /** Titre de la fenêtre. */
    public static final String TITLE = "Fakemon Arena";

    @Override
    public void start(Stage stage) {
        Scene scene = new Scene(new StackPane(), 960, 540);
        SceneManager sceneManager = new SceneManager(scene);
        sceneManager.show(new MainMenu(sceneManager));

        stage.setTitle(TITLE);
        stage.setScene(scene);
        stage.show();
    }
}
