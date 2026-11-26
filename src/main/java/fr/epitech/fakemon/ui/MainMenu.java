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
        for (Button button : new Button[] {combatOnlineButton, combatButton, mondexButton, addMon,
                optionsButton, creditsButton}) {
            MenuSounds.action(button, MenuSounds.Sound.OPEN);
        }
        getChildren().addAll(new Label(App.TITLE), combatOnlineButton, combatButton, mondexButton, addMon,
                optionsButton, creditsButton,
                quitButton);

        combatOnlineButton.setOnAction(event -> {
            Runnable menu = () -> sceneManager.show(this);
            sceneManager.show(new NetworkLobbyScreen(menu, (connection, hosting) -> {
                var roster = new JsonSpeciesRepository(new SpeciesFactory(), BattleAssets.userFolder());
                sceneManager.show(new NetworkPreparationScreen(connection, hosting, roster, sceneManager, menu));
            }));
        });

        combatButton.setOnAction(event -> {
            VBox modes = new VBox(12);
            modes.setAlignment(Pos.CENTER);
            Runnable menu = () -> sceneManager.show(this);
            Button constructed = new Button("Combat construit");
            MenuSounds.action(constructed, MenuSounds.Sound.OPEN);
            constructed.setOnAction(click -> {
                var roster = new JsonSpeciesRepository(new SpeciesFactory(), BattleAssets.userFolder());
                sceneManager.show(new TeamSelectionScreen(roster, sceneManager, () -> sceneManager.show(modes), menu));
            });
            Button random = new Button("Combat aléatoire");
            MenuSounds.action(random, MenuSounds.Sound.NONE);
            random.setOnAction(click -> {
                var setup = BattleSetup.soloRandom(
                        () -> new JsonSpeciesRepository(new SpeciesFactory(), BattleAssets.userFolder()),
                        System.getProperty("fakemon.demo.seed"));
                sceneManager.show(new BattleScreen(sceneManager, setup, menu));
            });
            Button back = new Button("Retour au menu principal");
            MenuSounds.action(back, MenuSounds.Sound.CLOSE);
            back.setOnAction(click -> menu.run());
            modes.getChildren().addAll(new Label("Combat contre l’IA"), constructed, random, back);
            sceneManager.show(modes);
        });

        mondexButton.setOnAction(event -> {
            var roster = new JsonSpeciesRepository(new SpeciesFactory(), BattleAssets.userFolder());
            sceneManager.show(new MondexScreen(roster, () -> sceneManager.show(this),
                    sceneManager.effectsVolumePercent()));
        });

        optionsButton.setOnAction(event -> sceneManager.showVolumeSettings());

        creditsButton.setOnAction(event -> {
            CreditsScreen credits = new CreditsScreen(() -> sceneManager.show(this), openLink);
            sceneManager.show(credits);
            credits.requestFocus();
        });

        addMon.setOnAction(event -> {
            var roster = new JsonSpeciesRepository(new SpeciesFactory(), BattleAssets.userFolder());
            sceneManager.show(new AddSpeciesScreen(roster, sceneManager.effectsVolumePercent(),
                    () -> sceneManager.show(this)));
        });

        quitButton.setOnAction(event -> {
            Platform.exit();
        });
    }
}
