package fr.epitech.fakemon.ui;

import java.util.ArrayDeque;
import java.util.Deque;

import fr.epitech.fakemon.core.api.BattleEndedEvent;
import fr.epitech.fakemon.core.api.BattleEngine;
import fr.epitech.fakemon.core.api.BattleEvent;
import fr.epitech.fakemon.core.api.BattleListener;
import fr.epitech.fakemon.core.api.CriticalHitEvent;
import fr.epitech.fakemon.core.api.EffectivenessEvent;
import fr.epitech.fakemon.core.api.FakemonEnteredEvent;
import fr.epitech.fakemon.core.api.FakemonFaintedEvent;
import fr.epitech.fakemon.core.api.FakemonSelectionRequiredEvent;
import fr.epitech.fakemon.core.api.Forfeit;
import fr.epitech.fakemon.core.api.HpLostEvent;
import fr.epitech.fakemon.core.api.HpRestoredEvent;
import fr.epitech.fakemon.core.api.MoveMissedEvent;
import fr.epitech.fakemon.core.api.MoveUsedEvent;
import fr.epitech.fakemon.core.api.PlayerAction;
import fr.epitech.fakemon.core.api.Side;
import fr.epitech.fakemon.core.api.TurnStartedEvent;
import fr.epitech.fakemon.core.api.UseMove;
import fr.epitech.fakemon.core.engine.DefaultBattleEngine;
import fr.epitech.fakemon.core.model.Fakemon;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Affiche les événements de l'arbitre et transmet les choix du joueur. */
public final class BattleScreen extends BorderPane implements BattleListener {
    private enum View { ACTION, TEAM, REQUIRED, PAUSE, RESOLVING, FINISHED }

    private final BattleEngine engine = new DefaultBattleEngine();
    private final BattleSetup setup;
    private final SceneManager scenes;
    private final Runnable menu;
    private final Deque<Runnable> events = new ArrayDeque<>();
    private final PauseTransition eventDelay = new PauseTransition(Duration.millis(110));
    private final StackPane arena = new StackPane();
    private final GridPane attacks = new GridPane();
    private final TextArea battleLog = new TextArea();
    private final Button pauseButton = new Button("Pause");
    private final Button teamButton = new Button("Équipe");
    private final Label turnLabel = new Label();
    private final HBox controls;
    private final FighterPanel player = new FighterPanel(true);
    private final FighterPanel opponent = new FighterPanel(false);
    private View view = View.RESOLVING;
    private int turnsPlayed;

    BattleScreen(SceneManager scenes, BattleSetup setup, Runnable menu) {
        this.setup = setup;
        this.scenes = scenes;
        this.menu = menu;
        setPadding(new Insets(12));
        setFocusTraversable(true);
        pauseButton.setOnAction(event -> pause());
        teamButton.setOnAction(event -> showTeam(false));
        setTop(new HBox(16, pauseButton, teamButton, turnLabel));
        StackPane.setAlignment(player, Pos.BOTTOM_LEFT);
        StackPane.setAlignment(opponent, Pos.TOP_RIGHT);
        arena.getChildren().addAll(player, opponent);
        arena.setPadding(new Insets(12));
        attacks.setHgap(8);
        attacks.setVgap(8);
        battleLog.setEditable(false);
        battleLog.setWrapText(true);
        battleLog.setPrefSize(340, 125);
        battleLog.setMinWidth(0);
        controls = new HBox(12, attacks, battleLog);
        HBox.setHgrow(battleLog, Priority.ALWAYS);
        showArena();
        setView(View.RESOLVING);
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                event.consume();
                if (view == View.PAUSE || view == View.TEAM) {
                    ready();
                } else if (view == View.ACTION) {
                    pause();
                }
            }
        });
        eventDelay.setOnFinished(event -> renderNext());
        sceneProperty().addListener((property, previous, current) -> {
            if (current == null) {
                eventDelay.stop();
                events.clear();
            } else {
                requestFocus();
            }
        });
        engine.addListener(this);
        setup.start(engine);
        renderNext();
    }

    private void submit(PlayerAction action) {
        if (view == View.RESOLVING || view == View.FINISHED) {
            return;
        }
        if (view == View.ACTION || view == View.TEAM) {
            turnsPlayed = engine.getState().getTurnNumber();
        }
        showArena();
        setView(View.RESOLVING);
        engine.submitAction(action);
        renderNext();
    }

    private void renderNext() {
        if (events.isEmpty()) {
            if (view == View.RESOLVING) {
                ready();
            }
            return;
        }
        events.removeFirst().run();
        if (!events.isEmpty()) {
            eventDelay.playFromStart();
        } else if (view == View.RESOLVING) {
            ready();
        }
    }

    /**
     * Capture les valeurs au moment de l'événement et les affiche dans l'ordre.
     * @param event fait annoncé par l'arbitre sur le thread JavaFX
     */
    @Override
    public void onEvent(BattleEvent event) {
        if (event instanceof FakemonEnteredEvent entered) {
            Fakemon mon = entered.getFakemon();
            int hp = mon.getCurrentHp();
            events.add(() -> {
                panel(entered.getSide()).show(mon, hp, setup.item(mon));
                appendLog(name(mon) + " entre en jeu.");
            });
        } else {
            events.add(() -> render(event));
        }
    }

    private void render(BattleEvent event) {
        if (event instanceof TurnStartedEvent turn) {
            turnLabel.setText("Tour " + turn.getTurnNumber());
            appendLog("Tour " + turn.getTurnNumber());
        } else if (event instanceof MoveUsedEvent move) {
            appendLog(name(move.getFakemon()) + " utilise " + move.getMove().getName() + " !");
        } else if (event instanceof HpLostEvent loss) {
            panel(loss.getSide()).health(loss.getRemainingHp(), loss.getFakemon().getMaxHp());
            appendLog(name(loss.getFakemon()) + " perd " + loss.getHpLost() + " PV.");
        } else if (event instanceof HpRestoredEvent gain) {
            panel(gain.getSide()).health(gain.getRemainingHp(), gain.getFakemon().getMaxHp());
            appendLog(name(gain.getFakemon()) + " récupère " + gain.getHpRestored() + " PV.");
        } else if (event instanceof MoveMissedEvent miss) {
            appendLog(name(miss.getFakemon()) + " rate son attaque.");
        } else if (event instanceof CriticalHitEvent) {
            appendLog("Coup critique !");
        } else if (event instanceof EffectivenessEvent effectiveness) {
            appendLog(switch (effectiveness.getEffectiveness()) {
                case SUPER_EFFECTIVE -> "C'est super efficace !";
                case NOT_VERY_EFFECTIVE -> "Ce n'est pas très efficace.";
                case NO_EFFECT -> "L'attaque n'a aucun effet.";
            });
        } else if (event instanceof FakemonFaintedEvent fainted) {
            appendLog(name(fainted.getFakemon()) + " est K.O.");
        } else if (event instanceof FakemonSelectionRequiredEvent) {
            showTeam(true);
        } else if (event instanceof BattleEndedEvent result) {
            setView(View.FINISHED);
            scenes.show(new BattleEndScreen(result.getWinner(), result.getReason(), turnsPlayed, setup.player,
                    () -> scenes.show(new BattleScreen(scenes, setup.rematch(), menu)), menu));
        }
    }

    private void ready() {
        showArena();
        attacks.getChildren().clear();
        var moves = engine.getState().getActiveFakemon(Side.PLAYER).getMoves();
        for (int index = 0; index < moves.size(); index++) {
            var move = moves.get(index);
            Button button = new Button(move.getName() + " — " + move.getType()
                    + "\nPuissance " + move.getPower() + " · Précision " + move.getAccuracy() + "%");
            button.setPrefWidth(235);
            button.setMinHeight(48);
            button.setWrapText(true);
            button.setOnAction(event -> submit(new UseMove(move)));
            attacks.add(button, index % 2, index / 2);
        }
        setView(View.ACTION);
        requestFocus();
    }

    private void showArena() {
        setCenter(arena);
        setBottom(controls);
    }

    private void showTeam(boolean required) {
        if (!required && view != View.ACTION) {
            return;
        }
        var state = engine.getState();
        setView(required ? View.REQUIRED : View.TEAM);
        setCenter(new StackPane(new TeamPanel(setup.player.getFakemons(), state.getActiveFakemon(Side.PLAYER),
                required, this::submit, this::ready)));
        setBottom(null);
        requestFocus();
    }

    private void pause() {
        if (view != View.ACTION) {
            return;
        }
        setView(View.PAUSE);
        var panel = new BattlePausePanel(this::ready, scenes::showVolumeSettings, () -> submit(new Forfeit()));
        setCenter(panel);
        setBottom(null);
        panel.focusResume();
    }

    private void setView(View next) {
        view = next;
        attacks.setDisable(next != View.ACTION);
        pauseButton.setDisable(next != View.ACTION);
        teamButton.setDisable(next != View.ACTION || setup.player.getStandingFakemons().size() < 2);
    }

    private FighterPanel panel(Side side) {
        return side == Side.PLAYER ? player : opponent;
    }

    private static String name(Fakemon mon) {
        return mon.getSpecies().getFrenchName();
    }

    private void appendLog(String message) {
        battleLog.appendText(message + "\n");
        battleLog.positionCaret(battleLog.getLength());
        battleLog.setScrollTop(Double.MAX_VALUE);
    }

    private static final class FighterPanel extends VBox {
        private final boolean back;
        private final ProgressBar bar = new ProgressBar();
        private final Label hp = new Label();

        FighterPanel(boolean back) {
            super(4);
            this.back = back;
            setMaxSize(240, Region.USE_PREF_SIZE);
            bar.setPrefSize(200, 18);
            bar.setMinHeight(18);
        }

        void show(Fakemon mon, int currentHp, String item) {
            Label name = new Label(BattleScreen.name(mon));
            name.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
            getChildren().setAll(name, new Label("Types : " + mon.getSpecies().getTypes()),
                    new Label("Objet : " + item), bar, hp, BattleAssets.sprite(mon.getSpecies(), back, 104));
            health(currentHp, mon.getMaxHp());
        }

        void health(int current, int max) {
            double ratio = (double) current / max;
            bar.setProgress(ratio);
            bar.setStyle("-fx-accent: " + (ratio < .2 ? "#d93636" : ratio < .5 ? "#e59522" : "#299b52") + ";");
            hp.setText("PV : " + current + " / " + max);
        }
    }
}
