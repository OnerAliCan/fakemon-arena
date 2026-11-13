package fr.epitech.fakemon.ui;

import java.util.Objects;

import fr.epitech.fakemon.core.api.BattleEvent;
import fr.epitech.fakemon.core.api.BattleListener;
import fr.epitech.fakemon.core.api.HpLostEvent;
import fr.epitech.fakemon.core.api.MoveUsedEvent;
import fr.epitech.fakemon.core.api.Side;
import fr.epitech.fakemon.core.api.TurnStartedEvent;
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Move;
import fr.epitech.fakemon.core.model.Type;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Écran de démonstration : affiche les événements du contrat sans moteur de combat. */
public class BattleScreen extends BorderPane implements BattleListener {
    private final TextArea battleLog = new TextArea();
    private final ProgressBar playerHpBar = new ProgressBar();
    private final ProgressBar opponentHpBar = new ProgressBar();
    private final Label playerHpLabel = new Label();
    private final Label opponentHpLabel = new Label();
    private final BattleDemo demo = new BattleDemo();

    /**
     * Construit l'écran de combat sur des données factices.
     * @param sceneManager gestionnaire de navigation
     * @param mainMenu menu à retrouver au retour
     */
    public BattleScreen(SceneManager sceneManager, MainMenu mainMenu) {
        setPadding(new Insets(12));
        Button backButton = new Button("Retour au menu");
        backButton.setOnAction(event -> sceneManager.show(mainMenu));
        HBox header = new HBox(20, backButton, new Label("Combat de démonstration — données factices"));
        header.setAlignment(Pos.CENTER_LEFT);
        setTop(header);

        // Les panneaux partagent la hauteur disponible au lieu de s'empiler.
        StackPane arena = new StackPane();
        VBox opponentPanel = createFakemonPanel(demo.opponent, "front/forbiddron.png",
                opponentHpBar, opponentHpLabel, 96);
        VBox playerPanel = createFakemonPanel(demo.player, "back/koaninja.png",
                playerHpBar, playerHpLabel, 112);
        StackPane.setAlignment(opponentPanel, Pos.TOP_RIGHT);
        StackPane.setAlignment(playerPanel, Pos.BOTTOM_LEFT);
        arena.getChildren().addAll(opponentPanel, playerPanel);
        arena.setPadding(new Insets(12));
        setCenter(arena);

        GridPane attacksPanel = new GridPane();
        attacksPanel.setHgap(8);
        attacksPanel.setVgap(8);
        for (int index = 0; index < demo.moves.size(); index++) {
            final int moveIndex = index;
            Button button = new Button();
            updateAttackButton(button, index);
            button.setPrefWidth(250);
            button.setMinHeight(68);
            button.setWrapText(true);
            button.setStyle("-fx-font-size: 11px; -fx-base: " + typeColor(demo.moves.get(index).getType()) + ";");
            button.setOnAction(event -> {
                for (BattleEvent battleEvent : demo.play(moveIndex)) {
                    onEvent(battleEvent);
                }
                updateAttackButton(button, moveIndex);
            });
            attacksPanel.add(button, index % 2, index / 2);
        }

        battleLog.setEditable(false);
        battleLog.setWrapText(true);
        battleLog.setPrefWidth(350);
        battleLog.setMinWidth(0);
        battleLog.setPrefHeight(120);
        HBox controls = new HBox(12, attacksPanel, battleLog);
        HBox.setHgrow(battleLog, Priority.ALWAYS);
        setBottom(controls);
        updateHp(Side.PLAYER, demo.player.getCurrentHp(), demo.player.getMaxHp());
        updateHp(Side.OPPONENT, demo.opponent.getCurrentHp(), demo.opponent.getMaxHp());
        appendLog("Choisissez une attaque. La quatrième illustre une attaque sans PP.");
    }

    private VBox createFakemonPanel(Fakemon fakemon, String spritePath,
            ProgressBar bar, Label hpLabel, int spriteSize) {
        Image image = new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("/sprites/" + spritePath), "Sprite introuvable : " + spritePath));
        ImageView sprite = new ImageView(image);
        sprite.setFitWidth(spriteSize);
        sprite.setPreserveRatio(true);
        sprite.setSmooth(false);
        bar.setMinSize(200, 18);
        bar.setPrefSize(200, 18);
        bar.setMaxSize(200, 18);
        Label name = new Label(fakemon.getSpecies().getFrenchName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        VBox panel = new VBox(4, name, new Label("Type : " + fakemon.getSpecies().getTypes()), hpLabel, bar, sprite);
        panel.setMaxSize(240, Region.USE_PREF_SIZE);
        return panel;
    }

    private void updateAttackButton(Button button, int index) {
        Move move = demo.moves.get(index);
        button.setText(move.getName() + " — " + move.getType() + " — PP " + demo.pp[index] + "/" + demo.maxPp[index]
                + "\n" + move.getCategory() + " • Puissance " + move.getPower()
                + " • Précision " + move.getAccuracy() + "%" + (move.getPriority() > 0 ? " • rapide" : ""));
        button.setDisable(demo.pp[index] == 0);
    }

    private void updateHp(Side side, int hp, int maxHp) {
        int displayedHp = Math.max(0, Math.min(hp, maxHp));
        double ratio = (double) displayedHp / maxHp;
        ProgressBar bar = side == Side.PLAYER ? playerHpBar : opponentHpBar;
        Label label = side == Side.PLAYER ? playerHpLabel : opponentHpLabel;
        bar.setProgress(ratio);
        bar.setStyle("-fx-accent: " + (ratio < 0.2 ? "#d93636" : ratio < 0.5 ? "#e59522" : "#299b52") + ";");
        label.setText("PV : " + displayedHp + " / " + maxHp);
    }

    /**
     * Affiche un événement reçu dans l'ordre sur le thread JavaFX.
     * Cette entrée pourra être abonnée au moteur lors de l'issue #26.
     * @param event événement à afficher
     */
    @Override
    public void onEvent(BattleEvent event) {
        if (event instanceof TurnStartedEvent) {
            appendLog("Tour " + ((TurnStartedEvent) event).getTurnNumber());
        } else if (event instanceof MoveUsedEvent) {
            MoveUsedEvent move = (MoveUsedEvent) event;
            appendLog(move.getFakemon().getSpecies().getFrenchName() + " utilise " + move.getMove().getName() + " !");
        } else if (event instanceof HpLostEvent) {
            HpLostEvent damage = (HpLostEvent) event;
            updateHp(damage.getSide(), damage.getRemainingHp(), damage.getFakemon().getMaxHp());
            appendLog(damage.getFakemon().getSpecies().getFrenchName() + " perd " + damage.getHpLost() + " PV.");
        }
    }

    private void appendLog(String message) {
        battleLog.appendText(message + "\n");
        battleLog.positionCaret(battleLog.getLength());
        battleLog.setScrollTop(Double.MAX_VALUE);
    }

    private String typeColor(Type type) {
        switch (type) {
            case WATER: return "#91c7eb";
            case SHADOW: return "#c2addb";
            case FORCE: return "#ebba91";
            default: return "#e3d694";
        }
    }
}
