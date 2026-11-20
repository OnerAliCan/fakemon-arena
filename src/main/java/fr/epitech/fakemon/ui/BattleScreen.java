package fr.epitech.fakemon.ui;

import java.util.ArrayDeque;
import java.util.Deque;

import fr.epitech.fakemon.core.api.BattleEndedEvent;
import fr.epitech.fakemon.core.api.BattleEngine;
import fr.epitech.fakemon.core.api.BattleEvent;
import fr.epitech.fakemon.core.api.BattleListener;
import fr.epitech.fakemon.core.api.BattleState;
import fr.epitech.fakemon.core.api.CriticalHitEvent;
import fr.epitech.fakemon.core.api.EffectivenessEvent;
import fr.epitech.fakemon.core.api.FakemonEnteredEvent;
import fr.epitech.fakemon.core.api.FakemonFaintedEvent;
import fr.epitech.fakemon.core.api.FakemonSelectionRequiredEvent;
import fr.epitech.fakemon.core.api.Forfeit;
import fr.epitech.fakemon.core.api.HpLostEvent;
import fr.epitech.fakemon.core.api.HpRestoredEvent;
import fr.epitech.fakemon.core.api.MoveMissedEvent;
import fr.epitech.fakemon.core.api.ItemActivatedEvent;
import fr.epitech.fakemon.core.api.MoveUsedEvent;
import fr.epitech.fakemon.core.api.PlayerAction;
import fr.epitech.fakemon.core.api.Side;
import fr.epitech.fakemon.core.api.TalentActivatedEvent;
import fr.epitech.fakemon.core.api.TurnStartedEvent;
import fr.epitech.fakemon.core.api.UseMove;
import fr.epitech.fakemon.core.api.StatusAppliedEvent;
import fr.epitech.fakemon.core.api.StatusActivatedEvent;
import fr.epitech.fakemon.core.api.AttackPreventedEvent;
import fr.epitech.fakemon.core.api.StatusRemovedEvent;
import fr.epitech.fakemon.core.model.StatusType;
import fr.epitech.fakemon.core.model.MoveCategory;
import fr.epitech.fakemon.core.model.Move;
import fr.epitech.fakemon.core.engine.DefaultBattleEngine;
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Item;
import javafx.animation.PauseTransition;
import javafx.animation.Animation;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
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

    private final BattleEngine engine;
    private final NetworkBattleController network;
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
    private final Button cancelChoice = new Button("Annuler mon choix");
    private final Label turnLabel = new Label();
    private final HBox controls;
    private final BattleTooltips tooltips = new BattleTooltips();
    private final FighterPanel player = new FighterPanel(true, tooltips);
    private final FighterPanel opponent = new FighterPanel(false, tooltips);
    private View view = View.RESOLVING;
    private int turnsPlayed;

    BattleScreen(SceneManager scenes, BattleSetup setup, Runnable menu) {
        this(scenes, setup, menu, null);
    }

    BattleScreen(SceneManager scenes, NetworkBattleController network, Runnable menu) {
        this(scenes, null, menu, network);
    }

    private BattleScreen(SceneManager scenes, BattleSetup setup, Runnable menu, NetworkBattleController network) {
        this.setup = setup;
        this.network = network;
        engine = network == null ? new DefaultBattleEngine() : null;
        this.scenes = scenes;
        this.menu = menu;
        setPadding(new Insets(12));
        var background = BattleScreen.class.getResource("/backgrounds/battle-grassland.png");
        arena.setStyle("-fx-background-image: url('" + background.toExternalForm()
                + "'); -fx-background-size: cover; -fx-background-position: center;");
        setFocusTraversable(true);
        pauseButton.setOnAction(event -> pause());
        teamButton.setOnAction(event -> showTeam(false));
        cancelChoice.setVisible(false);
        cancelChoice.setManaged(false);
        cancelChoice.setOnAction(event -> network.cancelAction());
        setTop(new HBox(16, pauseButton, teamButton, turnLabel, cancelChoice));
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
                if (network != null && view != View.FINISHED) {
                    network.close();
                }
            } else {
                requestFocus();
            }
        });
        if (network == null) {
            engine.addListener(this);
            setup.start(engine);
            renderNext();
        } else {
            turnLabel.setText("Connexion au combat…");
            network.start(this::onEvent, this::networkChoiceRequired, this::networkChoiceStateChanged,
                    this::networkFailure);
        }
    }

    private BattleState state() {
        return network == null ? engine.getState() : network.getState();
    }

    private void networkChoiceStateChanged() {
        boolean allowed = network.canCancel() && view != View.FINISHED;
        cancelChoice.setVisible(allowed);
        cancelChoice.setManaged(allowed);
        if (network.canChoose()) {
            networkChoiceRequired(network.needsReplacement());
        }
    }

    private void networkChoiceRequired(boolean replacement) {
        events.add(() -> {
            if (replacement) {
                showTeam(true);
            } else {
                ready();
            }
        });
        scheduleNetworkEvents();
    }

    private void scheduleNetworkEvents() {
        if (network != null && eventDelay.getStatus() != Animation.Status.RUNNING) {
            eventDelay.playFromStart();
        }
    }

    private void networkFailure(Throwable error) {
        network.close();
        eventDelay.stop();
        events.clear();
        setView(View.FINISHED);
        Label message = new Label("Le combat en LAN a été interrompu. " + error.getMessage());
        message.setWrapText(true);
        Button back = new Button("Retour au menu principal");
        back.setOnAction(event -> menu.run());
        setCenter(new VBox(12, message, back));
        setBottom(null);
    }

    private void submit(PlayerAction action) {
        if (view == View.RESOLVING || view == View.FINISHED) {
            return;
        }
        turnsPlayed = state().getTurnNumber();
        showArena();
        setView(View.RESOLVING);
        if (network == null) {
            engine.submitAction(action);
            renderNext();
        } else {
            try {
                network.submitAction(action);
            } catch (RuntimeException error) {
                networkFailure(error);
            }
        }
    }

    private void renderNext() {
        if (events.isEmpty()) {
            player.hideActivation();
            opponent.hideActivation();
            if (view == View.RESOLVING) {
                ready();
            }
            return;
        }
        player.hideActivation();
        opponent.hideActivation();
        eventDelay.setDuration(Duration.millis(110));
        events.removeFirst().run();
        if (!events.isEmpty()) {
            eventDelay.playFromStart();
        } else if (view == View.RESOLVING) {
            if (player.activation.isVisible() || opponent.activation.isVisible()) {
                eventDelay.playFromStart();
            } else {
                ready();
            }
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
            StatusType status = mon.getStatus() == null ? null : mon.getStatus().getType();
            events.add(() -> {
                tooltips.entered(mon, hp, status);
                panel(entered.getSide()).show(mon, hp, status);
                appendLog(name(mon) + " entre en jeu.");
                refreshTooltips();
            });
        } else {
            events.add(() -> render(event));
        }
        scheduleNetworkEvents();
    }

    private void render(BattleEvent event) {
        tooltips.observe(event);
        if (event instanceof TurnStartedEvent turn) {
            turnLabel.setText("Tour " + turn.getTurnNumber());
            appendLog("Tour " + turn.getTurnNumber());
        } else if (event instanceof MoveUsedEvent move) {
            appendLog(name(move.getFakemon()) + " utilise " + move.getMove().getName() + " !");
        } else if (event instanceof StatusAppliedEvent applied) {
            panel(applied.getSide()).status(applied.getStatus());
            appendLog(name(applied.getFakemon()) + " reçoit le statut " + applied.getStatus().getDisplayName() + ".");
        } else if (event instanceof StatusRemovedEvent removed) {
            panel(removed.getSide()).status(null);
            appendLog(removed.getStatus() == StatusType.SLEEP ? name(removed.getFakemon()) + " se réveille."
                    : removed.getStatus().getDisplayName() + " prend fin pour " + name(removed.getFakemon()) + ".");
        } else if (event instanceof StatusActivatedEvent activated) {
            appendLog(activated.getStatus() == StatusType.CONFUSION
                    ? name(activated.getFakemon()) + " se blesse dans sa confusion."
                    : activated.getStatus().getDisplayName() + " affecte " + name(activated.getFakemon()) + ".");
        } else if (event instanceof AttackPreventedEvent prevented) {
            appendLog(prevented.getStatus().getDisplayName() + " empêche "
                    + name(prevented.getFakemon()) + " d'attaquer.");
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
        } else if (event instanceof ItemActivatedEvent activated) {
            String text = name(activated.getFakemon()) + " bénéficie de " + activated.getItem().getDisplayName() + ".";
            panel(activated.getSide()).activate("Objet : " + activated.getItem().getDisplayName(),
                    name(activated.getFakemon()), activated.getItem());
            eventDelay.setDuration(Duration.millis(850));
            appendLog(text);
        } else if (event instanceof TalentActivatedEvent activated) {
            String talentName = BattleBadges.talentName(activated.getTalent());
            panel(activated.getSide()).activate("Talent : " + talentName, name(activated.getFakemon()), null);
            eventDelay.setDuration(Duration.millis(850));
            appendLog(name(activated.getFakemon()) + " active " + talentName + " !");
        } else if (event instanceof FakemonFaintedEvent fainted) {
            appendLog(name(fainted.getFakemon()) + " est K.O.");
        } else if (event instanceof FakemonSelectionRequiredEvent) {
            showTeam(true);
        } else if (event instanceof BattleEndedEvent result) {
            setView(View.FINISHED);
            Runnable replay = network == null
                    ? () -> scenes.show(new BattleScreen(scenes, setup.rematch(), menu)) : null;
            var endScreen = new BattleEndScreen(result.getWinner(), result.getReason(), turnsPlayed,
                    state().getTeam(Side.PLAYER), replay, menu);
            if (network != null) {
                endScreen.offerReplay(() -> network.rematch(scenes, menu, endScreen::replayFailed));
                endScreen.sceneProperty().addListener((property, previous, current) -> {
                    if (previous != null && current == null) {
                        network.close();
                    }
                });
            }
            scenes.show(endScreen);
        }
        refreshTooltips();
    }

    private void refreshTooltips() {
        player.refreshTooltip();
        opponent.refreshTooltip();
        attacks.getChildren().forEach(node -> {
            if (node instanceof Button button && button.getUserData() instanceof Move move) {
                button.getTooltip().setText(tooltips.move(move, opponent.mon));
            }
        });
    }

    private void ready() {
        if (network != null && !network.canChoose()) {
            setView(View.RESOLVING);
            turnLabel.setText(state() == null ? "Connexion au combat…"
                    : "Tour " + state().getTurnNumber() + " — En attente de l'adversaire");
            return;
        }
        if (network != null && network.needsReplacement()) {
            showTeam(true);
            return;
        }
        if (network != null) {
            turnLabel.setText("Tour " + state().getTurnNumber());
        }
        showArena();
        attacks.getChildren().clear();
        var moves = state().getActiveFakemon(Side.PLAYER).getMoves();
        for (int index = 0; index < moves.size(); index++) {
            var move = moves.get(index);
            String power = move.getCategory() == MoveCategory.STATUS ? "Sans dégâts" : "Puissance " + move.getPower();
            String effect = move.getStatus() == null ? ""
                    : "\n" + move.getStatus().getDisplayName() + " : " + move.getStatusChance() + "%";
            Button button = new Button(move.getName() + " — " + move.getType()
                    + "\n" + power + " · Précision " + move.getAccuracy() + "%" + effect);
            button.setPrefWidth(235);
            button.setMinHeight(48);
            button.setWrapText(true);
            button.setOnAction(event -> submit(new UseMove(move)));
            button.setUserData(move);
            BattleTooltips.install(button, () -> tooltips.move(move, opponent.mon));
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
        var state = state();
        setView(required ? View.REQUIRED : View.TEAM);
        setCenter(new StackPane(new TeamPanel(state.getTeam(Side.PLAYER).getFakemons(), state.getActiveFakemon(Side.PLAYER),
                required, this::submit, this::ready, tooltips)));
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
        teamButton.setDisable(next != View.ACTION || state() == null
                || state().getTeam(Side.PLAYER).getStandingFakemons().size() < 2);
        boolean cancelAllowed = network != null && network.canCancel() && next == View.RESOLVING;
        cancelChoice.setVisible(cancelAllowed);
        cancelChoice.setManaged(cancelAllowed);
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
        private final BattleTooltips details;
        private final Tooltip tooltip;
        private final Label heldItem = new Label("Objet : inconnu");
        private Fakemon mon;
        private Item displayedItem;
        private boolean displayedItemKnown;
        private final ProgressBar bar = new ProgressBar();
        private final Label hp = new Label();
        private final Label status = new Label();
        private final VBox activation = new VBox(2);

        FighterPanel(boolean back, BattleTooltips details) {
            super(4);
            this.back = back;
            this.details = details;
            tooltip = BattleTooltips.install(this, () -> details.fakemon(mon, back));
            heldItem.setId("held-item");
            setMaxSize(240, Region.USE_PREF_SIZE);
            bar.setPrefSize(200, 18);
            bar.setMinHeight(18);
            activation.setId(back ? "player-activation" : "opponent-activation");
            activation.setStyle("-fx-background-color: #262b45; -fx-background-radius: 6;"
                    + "-fx-padding: 8; -fx-border-color: #f6c443; -fx-border-radius: 6;");
            hideActivation();
        }

        void activate(String title, String owner, Item item) {
            Label heading = new Label(title);
            heading.setWrapText(true);
            heading.setStyle("-fx-text-fill: #f6c443; -fx-font-weight: bold;");
            if (item != null) {
                heading.setGraphic(BattleBadges.itemIcon(item));
            }
            Label name = new Label(owner + (back ? " · Votre camp" : " · Adversaire"));
            name.setStyle("-fx-text-fill: #fff5dd;");
            activation.getChildren().setAll(heading, name);
            activation.setVisible(true);
            activation.setManaged(true);
            activation.setAccessibleText(owner + " : " + title);
        }

        void hideActivation() {
            activation.setVisible(false);
            activation.setManaged(false);
        }

        void show(Fakemon mon, int currentHp, StatusType statusType) {
            this.mon = mon;
            Label name = new Label(BattleScreen.name(mon));
            name.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
            getChildren().setAll(name, TypeBadges.row(mon.getSpecies().getTypes()),
                    heldItem, status, bar, hp, BattleAssets.sprite(mon.getSpecies(), back, 104), activation);
            status(statusType);
            health(currentHp, mon.getMaxHp());
        }

        void refreshTooltip() {
            if (mon == null) {
                return;
            }
            tooltip.setText(details.fakemon(mon, back));
            boolean known = back || details.itemKnown(mon);
            Item item = back ? mon.getItem() : details.revealedItem(mon);
            if (known != displayedItemKnown || item != displayedItem) {
                heldItem.setText(!known ? "Objet : inconnu" : item == null ? "Aucun objet" : item.getDisplayName());
                heldItem.setGraphic(known ? BattleBadges.itemIcon(item) : null);
                displayedItem = item;
                displayedItemKnown = known;
            }
        }

        void status(StatusType type) {
            status.setText(type == null ? "" : "Statut : " + type.getDisplayName());
            status.setVisible(type != null);
            status.setManaged(type != null);
        }

        void health(int current, int max) {
            double ratio = (double) current / max;
            bar.setProgress(ratio);
            bar.setStyle("-fx-accent: " + (ratio < .2 ? "#d93636" : ratio < .5 ? "#e59522" : "#299b52") + ";");
            hp.setText("PV : " + current + " / " + max);
        }
    }
}
