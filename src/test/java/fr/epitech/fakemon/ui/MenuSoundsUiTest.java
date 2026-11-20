package fr.epitech.fakemon.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Vérifie le routage JavaFX, y compris les boutons désactivés qui ne déclenchent pas d'action. */
@EnabledIfSystemProperty(named = "fakemon.ui.tests", matches = "true")
class MenuSoundsUiTest {
    @Test
    void routesActionsDisabledClicksHoverAndKeyboardWithoutDuplicates() throws Exception {
        FxTestSupport.run(() -> {
            Stage stage = new Stage();
            try {
                var open = new Button("Ouvrir", new Label("Icône"));
                var back = new Button("Retour");
                var confirm = new ToggleButton("Choisir");
                var disabled = new Button("Indisponible");
                disabled.setDisable(true);
                var launch = new Button("Lancer");
                MenuSounds.action(open, MenuSounds.Sound.OPEN);
                MenuSounds.action(back, MenuSounds.Sound.CLOSE);
                MenuSounds.action(launch, MenuSounds.Sound.NONE);
                launch.setOnAction(event -> MenuSounds.play(launch, MenuSounds.Sound.LOAD_SAVE));
                var root = new VBox(12, open, back, confirm, disabled, launch);
                var scene = new Scene(root, 480, 400);
                var sounds = new RecordingSounds();
                sounds.install(scene);
                sounds.install(scene);
                stage.setScene(scene);
                stage.show();

                open.fire();
                back.fire();
                confirm.fire();
                launch.fire();
                assertEquals(List.of(MenuSounds.Sound.OPEN, MenuSounds.Sound.CLOSE,
                        MenuSounds.Sound.CONFIRM, MenuSounds.Sound.LOAD_SAVE), sounds.played);
                sounds.played.clear();

                var center = disabled.localToScene(disabled.getBoundsInLocal().getCenterX(),
                        disabled.getBoundsInLocal().getCenterY());
                // Le clic est livré au parent, comme lorsqu'un bouton désactivé est ignoré par JavaFX.
                Event.fireEvent(root, mouse(MouseEvent.MOUSE_PRESSED, root, center.getX(), center.getY()));
                disabled.fire();
                assertEquals(List.of(MenuSounds.Sound.ERROR), sounds.played);
                sounds.played.clear();

                Event.fireEvent(open, mouse(MouseEvent.MOUSE_MOVED, open, 1, 1));
                Event.fireEvent(open.getGraphic(), mouse(MouseEvent.MOUSE_MOVED, open.getGraphic(), 1, 1));
                Event.fireEvent(back, mouse(MouseEvent.MOUSE_MOVED, back, 1, 1));
                Event.fireEvent(disabled, mouse(MouseEvent.MOUSE_MOVED, disabled, 1, 1));
                assertEquals(List.of(MenuSounds.Sound.CURSOR, MenuSounds.Sound.CURSOR), sounds.played);
                sounds.played.clear();

                open.requestFocus();
                back.requestFocus();
                assertEquals(List.of(), sounds.played);
                open.requestFocus();
                Event.fireEvent(open, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN,
                        false, false, false, false));
                assertEquals(back, scene.getFocusOwner());
                assertEquals(List.of(MenuSounds.Sound.CURSOR), sounds.played);
                sounds.played.clear();

                var dynamic = new Button("Nouveau bouton");
                scene.setRoot(new VBox(dynamic));
                dynamic.fire();
                assertEquals(List.of(MenuSounds.Sound.CONFIRM), sounds.played);
                sounds.played.clear();

                try (var scenes = new SceneManager(scene)) {
                    scenes.show(new MainMenu(scenes, link -> {}));
                    button(scene, "Crédits").fire();
                    assertEquals(List.of(MenuSounds.Sound.OPEN), sounds.played);
                    Event.fireEvent(scene.getRoot(), new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE,
                            false, false, false, false));
                    assertEquals(List.of(MenuSounds.Sound.OPEN, MenuSounds.Sound.CLOSE), sounds.played);
                    sounds.played.clear();
                }

                var dialogClosed = new CompletableFuture<Void>();
                Platform.runLater(() -> {
                    try {
                        var window = javafx.stage.Window.getWindows().stream()
                                .filter(candidate -> candidate != stage && candidate.isShowing()).findFirst().orElseThrow();
                        var pane = (javafx.scene.control.DialogPane) window.getScene().getRoot();
                        ((Button) pane.lookupButton(ButtonType.CLOSE)).fire();
                        dialogClosed.complete(null);
                    } catch (Throwable error) {
                        dialogClosed.completeExceptionally(error);
                        stage.close();
                    }
                });
                new VolumeSettings().show(stage, sounds);
                dialogClosed.join();
                assertEquals(List.of(MenuSounds.Sound.CLOSE), sounds.played);
                return null;
            } finally {
                stage.close();
            }
        });
    }

    private Button button(Scene scene, String text) {
        return scene.getRoot().lookupAll(".button").stream().filter(Button.class::isInstance)
                .map(Button.class::cast).filter(button -> button.getText().equals(text)).findFirst().orElseThrow();
    }

    private MouseEvent mouse(javafx.event.EventType<MouseEvent> type, Node target, double x, double y) {
        return new MouseEvent(type, x, y, x, y, MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, false, false, false,
                new PickResult(target, x, y));
    }

    private static final class RecordingSounds extends MenuSounds {
        private final List<Sound> played = new ArrayList<>();

        RecordingSounds() {
            super(new SimpleDoubleProperty(0));
        }

        @Override
        void play(Sound sound) {
            if (sound != Sound.NONE) {
                played.add(sound);
            }
        }
    }
}
