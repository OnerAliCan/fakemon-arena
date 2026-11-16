package fr.epitech.fakemon.ui;

import fr.epitech.fakemon.App;
import fr.epitech.fakemon.data.JsonSpeciesRepository;
import fr.epitech.fakemon.data.SpeciesFactory;
import java.util.function.Consumer;
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
     * @param openLink action d’ouverture des liens des crédits
     */
    public MainMenu(SceneManager sceneManager, Consumer<String> openLink) {
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

        combatButton.setOnAction(event -> {
            VBox modes = new VBox(12);
            modes.setAlignment(Pos.CENTER);
            Runnable menu = () -> sceneManager.show(this);
            var roster = new JsonSpeciesRepository(new SpeciesFactory(), BattleAssets.userFolder());
            Button constructed = new Button("Combat construit");
            constructed.setOnAction(click -> sceneManager.show(new TeamSelectionScreen(
                    roster, sceneManager, () -> sceneManager.show(modes), menu)));
            Button random = new Button("Combat aléatoire");
            random.setOnAction(click -> sceneManager.show(new TeamPreviewScreen(sceneManager, roster, menu)));
            Button back = new Button("Retour au menu principal");
            back.setOnAction(click -> menu.run());
            modes.getChildren().addAll(new Label("Combat contre l’IA"), constructed, random, back);
            sceneManager.show(modes);
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

        optionsButton.setOnAction(event -> sceneManager.showVolumeSettings());

        creditsButton.setOnAction(event -> {
            CreditsScreen credits = new CreditsScreen(() -> sceneManager.show(this), openLink);
            sceneManager.show(credits);
            credits.requestFocus();
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
