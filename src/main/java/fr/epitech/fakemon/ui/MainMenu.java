package fr.epitech.fakemon.ui;

import fr.epitech.fakemon.App;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Menu principal du jeu et accès aux écrans provisoires. */
public class MainMenu extends VBox {

    /**
     * Construit le menu et les actions de ses boutons.
     *
     * @param sceneManager gestionnaire utilisé pour changer d'écran
     */
    public MainMenu(SceneManager sceneManager) {
        setSpacing(15);
        setAlignment(Pos.CENTER);

        Button combatOnlineButton = new Button("Combat en ligne");
        Button combatButton = new Button("Combat contre l’IA");
        Button mondexButton = new Button("Mondex");
        Button addMon = new Button("Ajouter un Mon");
        Button optionsButton = new Button("Options");
        Button creditsButton = new Button("Crédits");
        Button quitButton = new Button("Quitter");
        getChildren().addAll(new Label(App.TITLE), combatOnlineButton, combatButton, mondexButton, addMon,
                optionsButton, creditsButton,
                quitButton);

        VBox onlineMenu = new VBox(15);
        onlineMenu.setAlignment(Pos.CENTER);
        Button onlineBackButton = new Button("Retour");
        onlineMenu.getChildren().addAll(new Label("Combat en ligne"), onlineBackButton,
                new Label("Mode bonus prévu ultérieurement"));

        combatOnlineButton.setOnAction(event -> {
            sceneManager.show(onlineMenu);
        });
        onlineBackButton.setOnAction(event -> {
            sceneManager.show(this);
        });

        VBox combatMenu = new VBox(15);
        combatMenu.setAlignment(Pos.CENTER);
        Button backButton = new Button("Retour");
        combatMenu.getChildren().addAll(new Label("Combat contre l’IA"), backButton);

        combatButton.setOnAction(event -> {
            sceneManager.show(combatMenu);
        });
        backButton.setOnAction(event -> {
            sceneManager.show(this);
        });

        VBox mondexMenu = new VBox(15);
        mondexMenu.setAlignment(Pos.CENTER);
        Button mondexBackButton = new Button("Retour");
        mondexMenu.getChildren().addAll(new Label("Mondex"), mondexBackButton);

        mondexButton.setOnAction(event -> {
            sceneManager.show(mondexMenu);
        });
        mondexBackButton.setOnAction(event -> {
            sceneManager.show(this);
        });

        VBox optionsMenu = new VBox(15);
        optionsMenu.setAlignment(Pos.CENTER);
        Button optionsBackButton = new Button("Retour");
        optionsMenu.getChildren().addAll(new Label("Options"), optionsBackButton);

        optionsButton.setOnAction(event -> {
            sceneManager.show(optionsMenu);
        });
        optionsBackButton.setOnAction(event -> {
            sceneManager.show(this);
        });

        VBox creditsMenu = new VBox(15);
        creditsMenu.setAlignment(Pos.CENTER);
        Button creditsBackButton = new Button("Retour");
        creditsMenu.getChildren().addAll(new Label("Crédits"), creditsBackButton);

        creditsButton.setOnAction(event -> {
            sceneManager.show(creditsMenu);
        });
        creditsBackButton.setOnAction(event -> {
            sceneManager.show(this);
        });

        VBox addMonMenu = new VBox(15);
        addMonMenu.setAlignment(Pos.CENTER);
        Button addMonBackButton = new Button("Retour");
        addMonMenu.getChildren().addAll(new Label("Ajouter un Mon"), addMonBackButton);

        addMon.setOnAction(event -> {
            sceneManager.show(addMonMenu);
        });
        addMonBackButton.setOnAction(event -> {
            sceneManager.show(this);
        });

        quitButton.setOnAction(event -> {
            Platform.exit();
        });
    }
}
