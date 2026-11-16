package fr.epitech.fakemon.ui;

import fr.epitech.fakemon.core.api.BattleEndReason;
import fr.epitech.fakemon.core.api.BattleEndedEvent;
import fr.epitech.fakemon.core.api.Forfeit;
import fr.epitech.fakemon.core.api.Side;
import fr.epitech.fakemon.core.model.Team;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Parcours de combat construit avec des tours de démonstration jusqu'à #26. */
final class ConstructedBattleScreen extends BorderPane {
    private final ConstructedMatch match;
    private final Runnable menu;
    private final SceneManager scenes;
    private enum View { TEAM, ACTION, PAUSE }
    private View view;
    private final TextArea journal = new TextArea();

    ConstructedBattleScreen(ConstructedMatch match, SceneManager scenes, Runnable menu) {
        this.match = match;
        this.scenes = scenes;
        this.menu = menu;
        setPadding(new Insets(16));
        journal.setEditable(false);
        journal.setWrapText(true);
        journal.setPrefRowCount(4);
        setFocusTraversable(true);
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                event.consume();
                if (view == View.ACTION) {
                    pause();
                } else if (view == View.PAUSE) {
                    showBattle();
                }
            }
        });
        showTeam(true);
    }

    private void showTeam(boolean required) {
        view = View.TEAM;
        TeamPanel panel = new TeamPanel(match.playerTeam, match.active, required, action -> {
            var member = action.getFakemon();
            if (match.choose(member)) {
                journal.appendText(member.getSpecies().getFrenchName() + " entre en jeu.\n");
                showBattle();
            }
        }, this::showBattle);
        setTop(null);
        setCenter(new StackPane(panel));
        setBottom(null);
        requestFocus();
    }

    private void pause() {
        if (view != View.ACTION) {
            return;
        }
        view = View.PAUSE;
        BattlePausePanel panel = new BattlePausePanel(this::showBattle, scenes::showVolumeSettings,
                () -> showResult(match.submitAction(new Forfeit())));
        setTop(null);
        setCenter(panel);
        setBottom(null);
        panel.focusResume();
    }

    private void showBattle() {
        var opponent = match.opponent();
        if (opponent == null || match.playerTeam.stream().allMatch(Fakemon::isFainted)) {
            showResult(new BattleEndedEvent(opponent == null ? Side.PLAYER : Side.OPPONENT,
                    BattleEndReason.ALL_FAINTED));
        } else if (match.active.isFainted()) {
            showTeam(true);
        } else {
            view = View.ACTION;
            setTop(new HBox(12, heading("Combat construit"),
                    button("Équipe", () -> showTeam(false)), button("Pause", this::pause)));
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
            requestFocus();
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

    private void showResult(BattleEndedEvent result) {
        scenes.show(new BattleEndScreen(result.getWinner(), result.getReason(), match.turn,
                new Team(match.playerTeam),
                () -> scenes.show(new ConstructedBattleScreen(match.rematch(), scenes, menu)), menu));
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
