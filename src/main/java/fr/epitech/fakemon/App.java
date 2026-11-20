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
    private SceneManager sceneManager;

    @Override
    public void start(Stage stage) {
        Scene scene = new Scene(new StackPane(), 960, 540);
        sceneManager = new SceneManager(scene);
        sceneManager.show(new MainMenu(sceneManager, getHostServices()::showDocument));

        stage.setTitle(TITLE);
        stage.setScene(scene);
        stage.show();
    }

    /** Libère les lecteurs audio quand la fenêtre du jeu se ferme. */
    @Override
    public void stop() {
        if (sceneManager != null) {
            sceneManager.close();
        }
    }
}
