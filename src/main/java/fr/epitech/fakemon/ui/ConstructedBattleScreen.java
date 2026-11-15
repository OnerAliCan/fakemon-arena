package fr.epitech.fakemon.ui;

import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Species;
import java.util.Locale;
import java.util.Objects;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Parcours de combat construit avec des tours de démonstration jusqu'à #26. */
final class ConstructedBattleScreen extends BorderPane {
    private final ConstructedMatch match;
    private final Runnable menu;
    private final SceneManager scenes;
    private final TextArea journal = new TextArea();

    ConstructedBattleScreen(ConstructedMatch match, SceneManager scenes, Runnable menu) {
        this.match = match;
        this.scenes = scenes;
        this.menu = menu;
        setPadding(new Insets(16));
        journal.setEditable(false);
        journal.setWrapText(true);
        journal.setPrefRowCount(4);
        showTeam(true);
    }

    private void showTeam(boolean required) {
        String title = match.active == null ? "Choisis ton premier fakemon"
                : required ? "Choisis un remplaçant" : "Mon équipe";
        VBox rows = new VBox(6, heading(title));
        for (Fakemon member : match.playerTeam) {
            var name = member.getSpecies().getFrenchName();
            var send = button(member.isFainted() ? "K.O." : member == match.active ? "En jeu" : "Envoyer", () -> {
                if (match.choose(member)) {
                    journal.appendText(name + " entre en jeu.\n");
                    showBattle();
                }
            });
            send.setDisable(member.isFainted() || member == match.active);
            send.setAccessibleText("Envoyer " + name);
            var row = new HBox(12, sprite(member.getSpecies(), "front", 40), new Label(name),
                    new Label("PV : " + member.getCurrentHp() + " / " + member.getMaxHp()), send);
            row.setAlignment(Pos.CENTER_LEFT);
            rows.getChildren().add(row);
        }
        setTop(null);
        setCenter(rows);
        setBottom(required ? null : button("Retour au combat", this::showBattle));
    }

    private void showBattle() {
        var opponent = match.opponent();
        if (opponent == null || match.playerTeam.stream().allMatch(Fakemon::isFainted)) {
            showResult(opponent == null);
        } else if (match.active.isFainted()) {
            showTeam(true);
        } else {
            setTop(new HBox(12, heading("Combat construit"),
                    button("Équipe", () -> showTeam(false)), button("Abandonner", () -> showResult(false))));
            var playerCard = combatant(match.active, match.playerLoadouts.get(match.playerTeam.indexOf(match.active)).item(), "back");
            var opponentCard = combatant(opponent, match.opponentLoadouts.get(match.opponentTeam.indexOf(opponent)).item(), "front");
            var arena = new HBox(100, playerCard, opponentCard);
            arena.setAlignment(Pos.CENTER);
            setCenter(arena);
            var moves = new FlowPane(8, 8);
            for (var move : match.active.getMoves()) {
                moves.getChildren().add(button(move.getName(), () -> {
                    journal.appendText("Tour " + (match.turn + 1) + "\n" + match.attack(move) + "\n");
                    showBattle();
                }));
            }
            setBottom(new VBox(8, moves, journal));
        }
    }

    private VBox combatant(Fakemon mon, String item, String direction) {
        var hp = new ProgressBar((double) mon.getCurrentHp() / mon.getMaxHp());
        hp.setPrefWidth(160);
        var card = new VBox(6, heading(mon.getSpecies().getFrenchName()), sprite(mon.getSpecies(), direction, 112),
                new Label("Objet : " + item), hp, new Label("PV : " + mon.getCurrentHp() + " / " + mon.getMaxHp()));
        card.setAlignment(Pos.CENTER);
        return card;
    }

    private void showResult(boolean victory) {
        var content = new VBox(16, heading(victory ? "Victoire !" : "Défaite"),
                new Label("Nombre de tours : " + match.turn),
                button("Revanche", () -> scenes.show(new ConstructedBattleScreen(match.rematch(), scenes, menu))),
                button("Menu", menu));
        content.setAlignment(Pos.CENTER);
        setTop(null);
        setBottom(null);
        setCenter(content);
    }

    static ImageView sprite(Species species, String direction, int size) {
        String path = "/sprites/" + direction + "/" + species.getOriginalName().toLowerCase(Locale.ROOT) + ".png";
        var image = new ImageView(new Image(Objects.requireNonNull(
                ConstructedBattleScreen.class.getResourceAsStream(path), path)));
        image.setFitWidth(size);
        image.setFitHeight(size);
        image.setPreserveRatio(true);
        image.setSmooth(false);
        return image;
    }

    static Button button(String text, Runnable action) {
        Button button = new Button(text);
        button.setOnAction(event -> action.run());
        return button;
    }

    static Label heading(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        return label;
    }
}
