package fr.epitech.fakemon.ui;

import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    // Données provisoires du GDD, section 9, en attendant le catalogue JSON.
    // Les clés restent les noms d'origine pour retrouver les fichiers de sprites.
    private static final Map<String, String> TYPES_BY_NAME = Map.ofEntries(
            Map.entry("Trantima", "Venin / Roche"),
            Map.entry("Webruiser", "Venin / Force"),
            Map.entry("Anu", "Ombre"),
            Map.entry("Lizquent", "Ombre / Force"),
            Map.entry("Nemesusa", "Ombre / Eau"),
            Map.entry("Firomenis", "Feu / Venin"),
            Map.entry("Dynabit", "Foudre"),
            Map.entry("Chimbat", "Feu / Vent"),
            Map.entry("Varanitor", "Nature / Roche"),
            Map.entry("Forbiddron", "Venin / Ombre"),
            Map.entry("Tuffettry", "Éther"),
            Map.entry("Deember", "Feu / Foudre"),
            Map.entry("Kankersaur", "Roche / Venin"),
            Map.entry("Nuenflu", "Eau / Vent"),
            Map.entry("Marelstorm", "Eau / Force"),
            Map.entry("Pomprim", "Nature / Éther"),
            Map.entry("Negagen", "Ombre / Foudre"),
            Map.entry("Sillennium", "Roche / Éther"),
            Map.entry("Avipex", "Eau / Vent"),
            Map.entry("Hogriks", "Roche"),
            Map.entry("Koaninja", "Nature / Force"),
            Map.entry("Crator", "Feu / Roche"),
            Map.entry("Nymfusha", "Éther / Vent"),
            Map.entry("Enderbird", "Ombre / Vent"),
            Map.entry("Delugar", "Eau"),
            Map.entry("Lumaxi", "Foudre / Éther"),
            Map.entry("Efflutal", "Éther / Feu"),
            Map.entry("Baobaraffe", "Nature"),
            Map.entry("Tekagon", "Roche / Force"),
            Map.entry("Criminalis", "Vent")
    );
    private static final Map<String, String> FRENCH_NAMES = Map.ofEntries(
            Map.entry("Trantima", "Tarentemps"),
            Map.entry("Webruiser", "Stoillone"),
            Map.entry("Anu", "Nubis"),
            Map.entry("Lizquent", "Zarlou"),
            Map.entry("Nemesusa", "Némésuse"),
            Map.entry("Firomenis", "Pyrale"),
            Map.entry("Dynabit", "Dynacell"),
            Map.entry("Chimbat", "Chauve-Suie"),
            Map.entry("Varanitor", "Varaniteur"),
            Map.entry("Forbiddron", "Mormite"),
            Map.entry("Tuffettry", "Poufêtonne"),
            Map.entry("Deember", "Décendre"),
            Map.entry("Kankersaur", "Furonclodile"),
            Map.entry("Nuenflu", "Noisettousse"),
            Map.entry("Marelstorm", "Espadassin"),
            Map.entry("Pomprim", "Primelady"),
            Map.entry("Negagen", "Négatôme"),
            Map.entry("Sillennium", "Silennium"),
            Map.entry("Avipex", "Avipex"),
            Map.entry("Hogriks", "Porcteresse"),
            Map.entry("Koaninja", "Koanobi"),
            Map.entry("Crator", "Héphaïstodon"),
            Map.entry("Nymfusha", "Nymphaline"),
            Map.entry("Enderbird", "Trépassereau"),
            Map.entry("Delugar", "Délupard"),
            Map.entry("Lumaxi", "Lumax"),
            Map.entry("Efflutal", "Efflutal"),
            Map.entry("Baobaraffe", "Baobarafe"),
            Map.entry("Tekagon", "Séismodonte"),
            Map.entry("Criminalis", "Criminaile")
    );

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

        List<String> playerNames = List.of("Koaninja", "Anu", "Forbiddron", "Lumaxi", "Crator", "Tekagon");
        List<String> opponentNames = List.of("Forbiddron", "Lumaxi", "Crator", "Tekagon", "Koaninja", "Anu");
        VBox playerTeamPanel = createTeamPanel("Mon équipe", playerNames);
        VBox opponentTeamPanel = createTeamPanel("Équipe adverse", opponentNames);
        HBox teamsPanel = new HBox(32, playerTeamPanel, opponentTeamPanel);
        HBox.setHgrow(playerTeamPanel, Priority.ALWAYS);
        HBox.setHgrow(opponentTeamPanel, Priority.ALWAYS);
        ScrollPane teamsScroll = new ScrollPane(teamsPanel);
        teamsScroll.setFitToWidth(true);
        setCenter(teamsScroll);

        Button backButton = new Button("Retour au menu");
        backButton.setOnAction(event -> sceneManager.show(mainMenu));
        Button fightButton = new Button("Combattre");
        fightButton.setOnAction(event -> sceneManager.show(new BattleScreen(sceneManager, mainMenu)));
        HBox actions = new HBox(12, backButton, fightButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        BorderPane.setMargin(actions, new Insets(12, 0, 0, 0));
        setBottom(actions);
    }

    /** Construit une colonne ; les deux équipes utilisent la même présentation. */
    private VBox createTeamPanel(String title, List<String> names) {
        Label heading = new Label(title);
        heading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        VBox panel = new VBox(8, heading);
        panel.setPadding(new Insets(8));
        for (String name : names) {
            panel.getChildren().add(createMemberRow(name));
        }
        return panel;
    }

    /** Regroupe le sprite de face, le nom français et les types d'un fakemon. */
    private HBox createMemberRow(String name) {
        String spritePath = "/sprites/front/" + name.toLowerCase(Locale.ROOT) + ".png";
        Image spriteImage = new Image(Objects.requireNonNull(
                getClass().getResourceAsStream(spritePath), "Sprite introuvable : " + spritePath));
        ImageView spriteView = new ImageView(spriteImage);
        spriteView.setFitWidth(48);
        spriteView.setFitHeight(48);
        spriteView.setPreserveRatio(true);
        spriteView.setSmooth(false);
        Label frenchName = new Label(FRENCH_NAMES.get(name));
        frenchName.setStyle("-fx-font-weight: bold;");
        VBox memberInfo = new VBox(4, frenchName, new Label("Types : " + TYPES_BY_NAME.get(name)));
        HBox memberRow = new HBox(12, spriteView, memberInfo);
        memberRow.setAlignment(Pos.CENTER_LEFT);
        memberRow.setMinHeight(48);
        return memberRow;
    }
}
