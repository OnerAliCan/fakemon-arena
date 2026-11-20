package fr.epitech.fakemon.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.epitech.fakemon.core.api.BattleMode;
import fr.epitech.fakemon.core.api.SpeciesRepository;
import fr.epitech.fakemon.data.JsonSpeciesRepository;
import fr.epitech.fakemon.data.SpeciesFactory;
import fr.epitech.fakemon.net.NetworkConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Vérifie les départs et le transfert au combat sur JavaFX et de vrais sockets locaux. */
@EnabledIfSystemProperty(named = "fakemon.ui.tests", matches = "true")
class NetworkPreparationScreenTest {
    @TempDir Path userFolder;

    @Test
    void hostLeavingBeforeReadinessReturnsTheGuestToTheMenuWithANotification() throws Exception {
        try (Pair pair = pair()) {
            FxTestSupport.run(() -> {
                button(pair.host, "Retour au menu principal").fire();
                return null;
            });
            await(() -> pair.guestMenuVisits == 1);
            FxTestSupport.run(() -> {
                assertTrue(Window.getWindows().stream().anyMatch(window -> nodes(window.getScene().getRoot()).stream()
                        .anyMatch(node -> node instanceof Label label
                                && "L'hôte a quitté la partie.".equals(label.getText()))));
                return null;
            });
        }
    }

    @Test
    void guestLeavingAfterReadinessRemovesTheHostsLaunchControls() throws Exception {
        try (Pair pair = pair()) {
            makeReady(pair);
            FxTestSupport.run(() -> {
                button(pair.guest, "Retour au menu principal").fire();
                return null;
            });
            await(() -> nodes(pair.host).stream().anyMatch(node -> node instanceof Label label
                    && "L'invité a quitté le lobby.".equals(label.getText())));
            FxTestSupport.run(() -> {
                assertFalse(nodes(pair.host).stream().anyMatch(node -> node instanceof Button button
                        && "Lancer".equals(button.getText())));
                return null;
            });
        }
    }

    @Test
    void hostLeavingWhileTeamsAreBeingChosenAlsoReturnsTheGuestToTheMenu() throws Exception {
        try (Pair pair = pair()) {
            start(pair, BattleMode.CONSTRUCTED);
            await(() -> nodes(pair.guest).stream().anyMatch(TeamSelectionScreen.class::isInstance));
            FxTestSupport.run(() -> {
                pair.host.close();
                return null;
            });
            await(() -> pair.guestMenuVisits == 1);
        }
    }

    @ParameterizedTest
    @EnumSource(BattleMode.class)
    void bothModesReachTheFirstTurnWithoutCompetingReaders(BattleMode mode) throws Exception {
        try (Pair pair = pair()) {
            start(pair, mode);
            if (mode == BattleMode.CONSTRUCTED) {
                await(() -> nodes(pair.host).stream().anyMatch(TeamSelectionScreen.class::isInstance)
                        && nodes(pair.guest).stream().anyMatch(TeamSelectionScreen.class::isInstance));
                FxTestSupport.run(() -> {
                    selectTeam(pair.host, 0);
                    selectTeam(pair.guest, 6);
                    button(pair.host, "Envoyer").fire();
                    button(pair.guest, "Envoyer").fire();
                    return null;
                });
            }
            await(() -> isFirstTurn(pair.hostScene) && isFirstTurn(pair.guestScene));
            FxTestSupport.run(() -> {
                assertEquals(0, pair.hostMenuVisits);
                assertEquals(0, pair.guestMenuVisits);
                return null;
            });
        }
    }

    private Pair pair() throws Exception {
        return new Pair(new JsonSpeciesRepository(new SpeciesFactory(), userFolder));
    }

    private static void makeReady(Pair pair) throws Exception {
        FxTestSupport.run(() -> {
            button(pair.guest, "Prêt").fire();
            return null;
        });
        await(() -> !button(pair.host, "Lancer").isDisabled());
    }

    private static void start(Pair pair, BattleMode mode) throws Exception {
        makeReady(pair);
        FxTestSupport.run(() -> {
            if (mode == BattleMode.CONSTRUCTED) {
                nodes(pair.host).stream().filter(node -> node instanceof RadioButton button
                        && "Combat construit".equals(button.getText())).map(node -> (RadioButton) node)
                        .findFirst().orElseThrow().fire();
            }
            button(pair.host, "Lancer").fire();
            return null;
        });
    }

    private static void selectTeam(Parent screen, int offset) {
        var cards = nodes(screen).stream().filter(ToggleButton.class::isInstance)
                .map(ToggleButton.class::cast).toList();
        cards.subList(offset, offset + 6).forEach(ToggleButton::fire);
        button(screen, "Valider").fire();
    }

    private static boolean isFirstTurn(Scene scene) {
        return scene.getRoot() instanceof BattleScreen && nodes(scene.getRoot()).stream()
                .filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> button.getText().contains("Précision") && !button.isDisabled()).count() == 4;
    }

    private static ButtonBase button(Parent root, String text) {
        return nodes(root).stream().filter(node -> node instanceof ButtonBase button && text.equals(button.getText()))
                .map(ButtonBase.class::cast).findFirst().orElseThrow();
    }

    private static List<Node> nodes(Node root) {
        var found = new ArrayList<Node>();
        found.add(root);
        if (root instanceof ScrollPane scroll && scroll.getContent() != null) {
            found.addAll(nodes(scroll.getContent()));
        } else if (root instanceof Parent parent) {
            parent.getChildrenUnmodifiable().forEach(child -> found.addAll(nodes(child)));
        }
        return found;
    }

    private static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
        while (System.nanoTime() < deadline) {
            if (FxTestSupport.run(condition::getAsBoolean)) {
                return;
            }
            Thread.sleep(20);
        }
        assertTrue(FxTestSupport.run(condition::getAsBoolean), "L'état attendu n'a pas été atteint.");
    }

    private static final class Pair implements AutoCloseable {
        private final NetworkConnection hostConnection;
        private final NetworkConnection guestConnection;
        private final Scene hostScene;
        private final Scene guestScene;
        private final NetworkPreparationScreen host;
        private final NetworkPreparationScreen guest;
        private int hostMenuVisits;
        private int guestMenuVisits;

        private Pair(SpeciesRepository roster) throws Exception {
            try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
                Socket client = new Socket(InetAddress.getLoopbackAddress(), listener.getLocalPort());
                hostConnection = new NetworkConnection(listener.accept());
                guestConnection = new NetworkConnection(client);
            }
            var scenes = FxTestSupport.run(() -> List.of(new Scene(new StackPane(), 960, 540),
                    new Scene(new StackPane(), 960, 540)));
            hostScene = scenes.get(0);
            guestScene = scenes.get(1);
            var screens = FxTestSupport.run(() -> {
                var hostView = new NetworkPreparationScreen(hostConnection, true, roster, new SceneManager(hostScene),
                        () -> { hostMenuVisits++; hostScene.setRoot(new StackPane()); });
                var guestView = new NetworkPreparationScreen(guestConnection, false, roster, new SceneManager(guestScene),
                        () -> { guestMenuVisits++; guestScene.setRoot(new StackPane()); });
                hostScene.setRoot(hostView);
                guestScene.setRoot(guestView);
                return List.of(hostView, guestView);
            });
            host = screens.get(0);
            guest = screens.get(1);
        }

        @Override
        public void close() throws Exception {
            try {
                FxTestSupport.run(() -> {
                    host.close();
                    guest.close();
                    hostScene.setRoot(new StackPane());
                    guestScene.setRoot(new StackPane());
                    List.copyOf(Window.getWindows()).forEach(Window::hide);
                    return null;
                });
            } finally {
                try (hostConnection; guestConnection) { }
            }
        }
    }
}
