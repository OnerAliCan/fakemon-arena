package fr.epitech.fakemon.ui;

import fr.epitech.fakemon.net.NetworkClient;
import fr.epitech.fakemon.net.NetworkConnection;
import fr.epitech.fakemon.net.NetworkHost;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Ouvre ou rejoint une connexion LAN puis la transmet à l'écran de combat. */
final class NetworkLobbyScreen extends VBox implements AutoCloseable {
    private static final int PORT = 5000;
    private final TextField address = new TextField();
    private final Button hostButton = new Button("Héberger");
    private final Button joinButton = new Button("Rejoindre");
    private final Button cancelButton = new Button("Annuler");
    private final Label status = new Label();
    private final Runnable menu;
    private final BiConsumer<NetworkConnection, Boolean> onConnected;
    private final ExecutorService connections = Executors.newCachedThreadPool(
            Thread.ofPlatform().daemon().name("lan-lobby-", 0).factory());
    private final AtomicLong attempt = new AtomicLong();
    private final AtomicReference<NetworkHost> listening = new AtomicReference<>();
    private boolean busy;
    private boolean closed;

    /**
     * Prépare les actions du lobby.
     *
     * @param menu retourne au menu principal
     * @param onConnected reçoit la connexion et true pour l'hôte, false pour l'invité, sur le thread JavaFX
     */
    NetworkLobbyScreen(Runnable menu, BiConsumer<NetworkConnection, Boolean> onConnected) {
        super(14);
        this.menu = Objects.requireNonNull(menu);
        this.onConnected = Objects.requireNonNull(onConnected);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(24));
        address.setPromptText("Adresse IP de l'hôte");
        address.setMaxWidth(280);
        address.setAccessibleText("Adresse IP de l'hôte");
        status.setWrapText(true);
        status.setMaxWidth(440);
        Button back = new Button("Retour au menu principal");
        HBox buttons = new HBox(12, hostButton, joinButton);
        buttons.setAlignment(Pos.CENTER);
        getChildren().addAll(new Label("Combat en LAN"), address, buttons, status, cancelButton, back);
        setBusy(false);
        hostButton.setOnAction(event -> connect(true));
        joinButton.setOnAction(event -> connect(false));
        address.setOnAction(event -> {
            if (!busy && !closed) {
                connect(false);
            }
        });
        cancelButton.setOnAction(event -> cancel());
        back.setOnAction(event -> returnToMenu());
        setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                if (busy) {
                    cancel();
                } else {
                    returnToMenu();
                }
                event.consume();
            }
        });
        sceneProperty().addListener((property, previous, current) -> {
            if (previous != null && current == null) {
                close();
            } else if (current != null) {
                Platform.runLater(address::requestFocus);
            }
        });
    }

    private void connect(boolean hosting) {
        if (closed || busy) {
            return;
        }
        String target = address.getText().strip();
        if (!hosting && target.isEmpty()) {
            status.setText("Renseigne l'adresse IP de l'hôte.");
            address.requestFocus();
            return;
        }
        long currentAttempt = attempt.incrementAndGet();
        setBusy(true);
        status.setText(hosting ? "En attente d'un invité sur le port " + PORT + "…"
                : "Connexion à " + target + "…");
        connections.execute(() -> {
            try {
                NetworkConnection connection = hosting ? acceptGuest(currentAttempt) : NetworkClient.connect(target, PORT);
                if (connection != null) {
                    Platform.runLater(() -> handOver(connection, hosting, currentAttempt));
                }
            } catch (IOException | RuntimeException error) {
                Platform.runLater(() -> {
                    if (!closed && attempt.get() == currentAttempt) {
                        setBusy(false);
                        status.setText("Connexion impossible. " + error.getMessage());
                    }
                });
            }
        });
    }

    private NetworkConnection acceptGuest(long currentAttempt) throws IOException {
        try (NetworkHost host = new NetworkHost(PORT)) {
            listening.set(host);
            try {
                if (attempt.get() != currentAttempt) {
                    return null;
                }
                return host.accept();
            } finally {
                listening.compareAndSet(host, null);
            }
        }
    }

    private void handOver(NetworkConnection connection, boolean hosting, long currentAttempt) {
        if (closed || attempt.get() != currentAttempt) {
            closeConnection(connection);
            return;
        }
        try {
            onConnected.accept(connection, hosting);
        } catch (RuntimeException error) {
            closeConnection(connection);
            setBusy(false);
            status.setText("Impossible d'ouvrir le combat. " + error.getMessage());
        }
    }

    private void cancel() {
        attempt.incrementAndGet();
        closeListener();
        setBusy(false);
        status.setText("Connexion annulée.");
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        hostButton.setDisable(busy);
        joinButton.setDisable(busy);
        address.setDisable(busy);
        cancelButton.setVisible(busy);
        cancelButton.setManaged(busy);
    }

    private void returnToMenu() {
        close();
        menu.run();
    }

    private void closeListener() {
        NetworkHost host = listening.getAndSet(null);
        if (host != null) {
            try {
                host.close();
            } catch (IOException error) {
                status.setText("Impossible de fermer le port d'écoute. " + error.getMessage());
            }
        }
    }

    private void closeConnection(NetworkConnection connection) {
        try {
            connection.close();
        } catch (IOException error) {
            status.setText("Impossible de fermer la connexion. " + error.getMessage());
        }
    }

    /** Annule les tentatives et ferme le port d'écoute en quittant cet écran. */
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            attempt.incrementAndGet();
            closeListener();
            connections.shutdownNow();
        }
    }
}
