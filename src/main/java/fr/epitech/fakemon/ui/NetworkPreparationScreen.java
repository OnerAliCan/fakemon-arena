package fr.epitech.fakemon.ui;

import fr.epitech.fakemon.core.api.BattleMode;
import fr.epitech.fakemon.core.api.SpeciesRepository;
import fr.epitech.fakemon.core.engine.TeamDrafter;
import fr.epitech.fakemon.core.model.Species;
import fr.epitech.fakemon.net.NetworkConnection;
import fr.epitech.fakemon.net.NetworkPreparationSession;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;

/** Prépare les équipes LAN et cède la connexion à l'écran de combat lorsque les deux joueurs ont validé. */
final class NetworkPreparationScreen extends VBox implements AutoCloseable {
    private final NetworkConnection connection;
    private final boolean hosting;
    private final SpeciesRepository roster;
    private final SceneManager scenes;
    private final Runnable menu;
    private final NetworkPreparationSession session;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(
            Thread.ofPlatform().daemon().name("lan-preparation").factory());
    private final Label status = new Label();
    private final Button back = new Button("Retour au menu principal");
    private boolean handedOver;
    private volatile boolean closed;

    /**
     * Construit la vue et commence l'attente du signal Prêt côté hôte.
     *
     * @param connection connexion déjà établie
     * @param hosting true pour l'hôte, false pour l'invité
     * @param roster catalogue local, utilisé comme référence par l'hôte
     * @param scenes gestionnaire d'écrans
     * @param menu retourne au menu principal
     */
    NetworkPreparationScreen(NetworkConnection connection, boolean hosting, SpeciesRepository roster,
            SceneManager scenes, Runnable menu) {
        super(14);
        this.connection = connection;
        this.hosting = hosting;
        this.roster = roster;
        this.scenes = scenes;
        this.menu = menu;
        session = new NetworkPreparationSession(connection, hosting);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(24));
        status.setWrapText(true);
        MenuSounds.action(back, MenuSounds.Sound.CLOSE);
        back.setOnAction(event -> {
            close();
            menu.run();
        });
        sceneProperty().addListener((property, previous, current) -> {
            if (previous != null && current == null && !handedOver) {
                close();
            }
        });
        Label title = new Label("Préparation du combat en LAN");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        getChildren().add(title);
        session.startMonitoring(error -> update(() -> fail(error)));

        if (hosting) {
            ToggleGroup mode = new ToggleGroup();
            RadioButton random = new RadioButton("Combat aléatoire");
            RadioButton constructed = new RadioButton("Combat construit");
            random.setToggleGroup(mode);
            constructed.setToggleGroup(mode);
            random.setSelected(true);
            HBox choices = new HBox(16, random, constructed);
            choices.setAlignment(Pos.CENTER);
            Button start = new Button("Lancer");
            MenuSounds.action(start, MenuSounds.Sound.NONE);
            start.setDisable(true);
            status.setText("Invité : pas encore prêt.");
            start.setOnAction(event -> {
                start.setDisable(true);
                choices.setDisable(true);
                BattleMode selected = random.isSelected() ? BattleMode.RANDOM : BattleMode.CONSTRUCTED;
                work(() -> {
                    session.start(selected);
                    update(() -> prepare(selected));
                });
            });
            getChildren().addAll(new Label("Choisissez le mode de combat"), choices, status, start, back);
            work(() -> {
                session.awaitReady();
                update(() -> {
                    status.setText("Invité : prêt !");
                    start.setDisable(false);
                });
            });
        } else {
            Button ready = new Button("Prêt");
            status.setText("Vous n'êtes pas encore prêt.");
            ready.setOnAction(event -> {
                ready.setDisable(true);
                status.setText("Prêt. En attente du lancement par l'hôte…");
                work(() -> {
                    session.ready();
                    BattleMode selected = session.awaitStart();
                    update(() -> prepare(selected));
                });
            });
            getChildren().addAll(new Label("L'hôte choisit le mode de combat."), status, ready, back);
        }
    }

    private void prepare(BattleMode mode) {
        if (mode == BattleMode.RANDOM) {
            work(() -> finishPreparation(hosting ? BattleSetup.random(roster, new Random()) : null));
            return;
        }
        var selection = new TeamSelectionScreen(roster, () -> {
            close();
            menu.run();
        }, this::chooseStarter);
        MenuSounds.play(this, MenuSounds.Sound.OPEN);
        getChildren().setAll(selection);
        VBox.setVgrow(selection, Priority.ALWAYS);
    }

    private void chooseStarter(List<Species> selected) {
        var team = new TeamDrafter(roster, new Random()).buildTeam(selected);
        var starters = new TeamPanel(team.getFakemons(), null, true, action -> {
            var ordered = new ArrayList<>(selected);
            ordered.remove(action.getFakemon().getSpecies());
            ordered.addFirst(action.getFakemon().getSpecies());
            submitSelection(List.copyOf(ordered));
        }, () -> { });
        getChildren().setAll(starters, back);
    }

    private void submitSelection(List<Species> selected) {
        status.setText("Équipe validée. En attente de l'autre joueur…");
        getChildren().setAll(status, back);
        work(() -> {
            if (hosting) {
                List<Species> guest = session.receiveSelection().stream().map(name -> roster.findAll().stream()
                        .filter(species -> species.getOriginalName().equals(name)).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Espèce absente du catalogue de l'hôte : " + name)))
                        .toList();
                var setup = BattleSetup.networkConstructed(roster, selected, guest, new Random());
                finishPreparation(setup);
            } else {
                session.sendSelection(selected.stream().map(Species::getOriginalName).toList());
                finishPreparation(null);
            }
        });
    }

    private void finishPreparation(BattleSetup setup) throws IOException {
        session.finish();
        update(() -> launch(setup));
    }

    private void launch(BattleSetup setup) {
        var controller = new NetworkBattleController(connection, setup);
        try {
            var screen = new BattleScreen(scenes, controller, menu);
            handedOver = true;
            scenes.show(screen);
            worker.shutdown();
        } catch (RuntimeException error) {
            controller.close();
            throw error;
        }
    }

    private void work(NetworkTask task) {
        worker.execute(() -> {
            try {
                task.run();
            } catch (IOException | RuntimeException error) {
                update(() -> fail(error));
            }
        });
    }

    private void update(Runnable action) {
        Platform.runLater(() -> {
            if (!closed) {
                try {
                    action.run();
                } catch (RuntimeException error) {
                    fail(error);
                }
            }
        });
    }

    private void fail(Throwable error) {
        close();
        String message = error instanceof EOFException
                ? hosting ? "L'invité a quitté le lobby." : "L'hôte a quitté la partie."
                : "Préparation interrompue. " + error.getMessage();
        status.setText(message);
        getChildren().setAll(status, back);
        if (!hosting) {
            var owner = getScene() == null ? null : getScene().getWindow();
            menu.run();
            Alert notification = new Alert(Alert.AlertType.INFORMATION);
            notification.setTitle("Combat en LAN");
            notification.setHeaderText("Préparation interrompue");
            notification.setContentText(message);
            notification.initOwner(owner);
            notification.show();
        }
    }

    /** Ferme la connexion et interrompt l'attente lorsque le joueur quitte la préparation. */
    @Override
    public void close() {
        if (!closed && !handedOver) {
            closed = true;
            worker.shutdownNow();
            try {
                session.close();
            } catch (IOException error) {
                status.setText("Impossible de fermer la connexion. " + error.getMessage());
            }
        }
    }

    @FunctionalInterface
    private interface NetworkTask {
        void run() throws IOException;
    }
}
