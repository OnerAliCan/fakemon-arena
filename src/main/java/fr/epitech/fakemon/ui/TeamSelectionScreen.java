package fr.epitech.fakemon.ui;

import fr.epitech.fakemon.core.api.SpeciesRepository;
import fr.epitech.fakemon.core.model.Species;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Permet de composer une équipe de six espèces distinctes à partir du catalogue. */
final class TeamSelectionScreen extends BorderPane {
    private final Set<Species> selected = new LinkedHashSet<>();
    private final Map<Species, ToggleButton> cards = new LinkedHashMap<>();
    private final Label count = new Label();
    private final Button confirm = new Button("Valider");

    TeamSelectionScreen(SpeciesRepository roster, SceneManager scenes, Runnable back, Runnable menu) {
        this(roster, back, selected -> scenes.show(new BattleScreen(scenes,
                BattleSetup.constructed(roster, selected, new Random()), menu)));
        MenuSounds.action(confirm, MenuSounds.Sound.NONE);
    }

    /** Réutilise la sélection du catalogue pour une préparation locale ou réseau. */
    TeamSelectionScreen(SpeciesRepository roster, Runnable back, Consumer<List<Species>> onConfirm) {
        setPadding(new Insets(16));
        setStyle("-fx-background-color: #19233a;");
        Label title = new Label("Choisissez vos fakemon");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        title.setStyle(title.getStyle() + "-fx-text-fill: #fff5dd;");
        Label help = new Label("Choisis six fakemon. Clique de nouveau sur une carte pour la retirer.");
        help.setStyle("-fx-text-fill: #d6dcf5;");
        count.setStyle("-fx-text-fill: #f6c443; -fx-font-size: 16px; -fx-font-weight: bold;");
        setTop(new VBox(5, title, help, count));

        var grid = new FlowPane(9, 9);
        grid.setPadding(new Insets(6));
        grid.setStyle("-fx-background-color: #19233a;");
        for (Species species : roster.findAll()) {
            var card = new ToggleButton(species.getFrenchName(), BattleAssets.sprite(species, false, 48));
            card.setContentDisplay(javafx.scene.control.ContentDisplay.TOP);
            card.setWrapText(true);
            card.setPrefSize(136, 96);
            card.setMinSize(136, 96);
            card.setOnAction(event -> {
                if (!selected.remove(species) && selected.size() < 6) {
                    selected.add(species);
                }
                updateSelection();
            });
            cards.put(species, card);
            grid.getChildren().add(card);
        }
        var scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: #19233a; -fx-background-color: #19233a;");
        BorderPane.setMargin(scroll, new Insets(10, 0, 10, 0));
        setCenter(scroll);

        confirm.setStyle("-fx-base: #f6c443; -fx-font-weight: bold;");
        MenuSounds.action(confirm, MenuSounds.Sound.OPEN);
        confirm.setOnAction(event -> onConfirm.accept(List.copyOf(selected)));
        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button backButton = new Button("Retour");
        MenuSounds.action(backButton, MenuSounds.Sound.CLOSE);
        backButton.setOnAction(event -> back.run());
        var actions = new HBox(12, backButton, spacer, confirm);
        actions.setAlignment(Pos.CENTER_LEFT);
        setBottom(actions);
        updateSelection();
    }

    private void updateSelection() {
        count.setText(selected.size() + " / 6 sélectionnés");
        confirm.setDisable(selected.size() != 6);
        cards.forEach((species, card) -> {
            boolean chosen = selected.contains(species);
            card.setSelected(chosen);
            card.setText(species.getFrenchName() + (chosen ? "\nSélectionné" : ""));
            card.setStyle(chosen ? "-fx-base: #f6c443;" : "-fx-base: #e8edfb;");
            card.setDisable(!chosen && selected.size() == 6);
        });
    }
}
