package fr.epitech.fakemon.ui;

import java.util.List;
import java.util.Locale;
import java.util.Random;

import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Species;
import java.util.Objects;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Aperçu de deux équipes de démonstration de six fakemon avant le combat. */
public class TeamPreviewScreen extends BorderPane {
    /**
     * Construit l'aperçu et les actions de navigation.
     * @param sceneManager gestionnaire des changements d'écran
     * @param mainMenu menu existant à retrouver au retour
     */
    public TeamPreviewScreen(SceneManager sceneManager, MainMenu mainMenu) {
        setPadding(new Insets(16));
        Label title = new Label("Aperçu des équipes — démonstration 6 contre 6");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        setTop(title);
        BorderPane.setMargin(title, new Insets(0, 0, 12, 0));

        BattleDemo demo = new BattleDemo(new Random());
        VBox playerTeamPanel = createTeamPanel("Mon équipe", demo.playerTeam);
        VBox opponentTeamPanel = createTeamPanel("Équipe adverse", demo.opponentTeam);
        HBox teamsPanel = new HBox(32, playerTeamPanel, opponentTeamPanel);
        HBox.setHgrow(playerTeamPanel, Priority.ALWAYS);
        HBox.setHgrow(opponentTeamPanel, Priority.ALWAYS);
        ScrollPane teamsScroll = new ScrollPane(teamsPanel);
        teamsScroll.setFitToWidth(true);
        setCenter(teamsScroll);

        Button backButton = new Button("Retour au menu");
        backButton.setOnAction(event -> sceneManager.show(mainMenu));
        Button fightButton = new Button("Combattre");
        fightButton.setOnAction(event -> sceneManager.show(new BattleScreen(sceneManager, mainMenu, demo)));
        HBox actions = new HBox(12, backButton, fightButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        BorderPane.setMargin(actions, new Insets(12, 0, 0, 0));
        setBottom(actions);
    }

    /** Construit une colonne ; les deux équipes utilisent la même présentation. */
    private VBox createTeamPanel(String title, List<Fakemon> members) {
        Label heading = new Label(title);
        heading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        VBox panel = new VBox(8, heading);
        panel.setPadding(new Insets(8));
        for (Fakemon member : members) {
            panel.getChildren().add(createMemberRow(member));
        }
        return panel;
    }

    /** Regroupe le sprite de face, le nom français et les types d'un fakemon. */
    private HBox createMemberRow(Fakemon member) {
        Species species = member.getSpecies();
        String spritePath = "/sprites/front/" + species.getOriginalName().toLowerCase(Locale.ROOT) + ".png";
        Image spriteImage = new Image(Objects.requireNonNull(
                getClass().getResourceAsStream(spritePath), "Sprite introuvable : " + spritePath));
        ImageView spriteView = new ImageView(spriteImage);
        spriteView.setFitWidth(48);
        spriteView.setFitHeight(48);
        spriteView.setPreserveRatio(true);
        spriteView.setSmooth(false);
        Label frenchName = new Label(species.getFrenchName());
        frenchName.setStyle("-fx-font-weight: bold;");
        VBox memberInfo = new VBox(4, frenchName, new Label("Types : " + species.getTypes()));
        HBox memberRow = new HBox(12, spriteView, memberInfo);
        memberRow.setAlignment(Pos.CENTER_LEFT);
        memberRow.setMinHeight(48);
        return memberRow;
    }
}
