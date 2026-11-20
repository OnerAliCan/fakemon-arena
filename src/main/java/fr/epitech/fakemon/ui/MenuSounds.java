package fr.epitech.fakemon.ui;

import java.util.EnumMap;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

/** Partage les sons des menus entre les écrans et les fenêtres de réglages. */
class MenuSounds {
    enum Sound {
        ERROR("Error"), OPEN("Open"), LOAD_SAVE("LoadSave"), CLOSE("Close"),
        CONFIRM("Confirm"), CURSOR("Cursor"), NONE(null);

        private final String file;

        Sound(String file) {
            this.file = file;
        }
    }

    private static final Object PLAYER = new Object();
    private static final Object ACTION = new Object();
    private final ReadOnlyDoubleProperty volumePercent;
    private final EnumMap<Sound, MediaPlayer> players = new EnumMap<>(Sound.class);

    MenuSounds(ReadOnlyDoubleProperty volumePercent) {
        this.volumePercent = volumePercent;
    }

    /** Prépare un lecteur par effet pour éviter de le recréer à chaque action. */
    void preload() {
        for (Sound sound : Sound.values()) {
            if (sound == Sound.NONE) {
                continue;
            }
            try {
                var source = MenuSounds.class.getResource("/sounds/ui/" + sound.file + ".wav");
                var player = new MediaPlayer(new Media(source.toExternalForm()));
                players.put(sound, player);
                player.volumeProperty().bind(volumePercent.divide(100));
                // Le premier démarrage du moteur audio est plus lent, même avec un lecteur prêt.
                player.setMute(true);
                player.setOnReady(player::play);
                player.setOnEndOfMedia(player::stop);
                player.setOnError(() -> logFailure(sound, player.getError()));
            } catch (RuntimeException error) {
                logFailure(sound, error);
            }
        }
    }

    /** Libère les lecteurs au moment où l'application se ferme. */
    void close() {
        for (MediaPlayer player : players.values()) {
            player.setOnReady(null);
            player.setOnEndOfMedia(null);
            player.setOnError(null);
            player.volumeProperty().unbind();
            player.dispose();
        }
        players.clear();
    }

    /** Associe un son à une action particulière ; les autres boutons confirment par défaut. */
    static void action(ButtonBase button, Sound sound) {
        button.getProperties().put(ACTION, sound);
    }

    /** Joue aussi les actions réalisées sans bouton, notamment Échap et le lancement LAN. */
    static void play(Node node, Sound sound) {
        if (node.getScene() != null && node.getScene().getProperties().get(PLAYER) instanceof MenuSounds sounds) {
            sounds.play(sound);
        }
    }

    /** Installe une seule fois les sons de clic, survol et navigation au clavier. */
    void install(Scene scene) {
        if (scene.getProperties().putIfAbsent(PLAYER, this) != null) {
            return;
        }
        var keyboardNavigation = new boolean[1];
        scene.addEventFilter(ActionEvent.ACTION, event -> {
            if (event.getTarget() instanceof ButtonBase button && !button.isDisabled()) {
                Sound sound = (Sound) button.getProperties().getOrDefault(ACTION, Sound.CONFIRM);
                if (!keyboardNavigation[0] || sound != Sound.CONFIRM) {
                    play(sound);
                }
            }
        });
        // Un contrôle désactivé ne reçoit pas de clic : on le repère depuis la scène.
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                ButtonBase button = buttonAt(scene.getRoot(), event.getSceneX(), event.getSceneY());
                if (button != null && button.isDisabled()) {
                    play(Sound.ERROR);
                    event.consume();
                }
            }
        });
        var hovered = new ButtonBase[1];
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, event -> {
            ButtonBase button = button(event.getTarget());
            if (button != hovered[0]) {
                hovered[0] = button;
                if (button != null && !button.isDisabled()) {
                    play(Sound.CURSOR);
                }
            }
        });
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            KeyCode key = event.getCode();
            keyboardNavigation[0] = key.isArrowKey() || key == KeyCode.TAB;
            Platform.runLater(() -> keyboardNavigation[0] = false);
        });
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> keyboardNavigation[0] = false);
        scene.focusOwnerProperty().addListener((property, previous, current) -> {
            if (keyboardNavigation[0] && current instanceof ButtonBase button && !button.isDisabled()) {
                play(Sound.CURSOR);
            }
        });
        scene.rootProperty().addListener((property, previous, current) -> {
            hovered[0] = null;
            keyboardNavigation[0] = false;
        });
    }

    void play(Sound sound) {
        MediaPlayer player = players.get(sound);
        if (player == null || volumePercent.get() <= 0) {
            return;
        }
        player.setOnReady(null);
        if (player.getStatus() == MediaPlayer.Status.PLAYING) {
            player.seek(Duration.ZERO);
        }
        player.setMute(false);
        player.play();
    }

    private void logFailure(Sound sound, Throwable error) {
        System.getLogger(MenuSounds.class.getName()).log(System.Logger.Level.WARNING,
                "Impossible de lire le son de menu " + sound + ".", error);
    }

    private static ButtonBase button(Object target) {
        for (Node node = target instanceof Node value ? value : null; node != null; node = node.getParent()) {
            if (node instanceof ButtonBase button) {
                return button;
            }
        }
        return null;
    }

    private static ButtonBase buttonAt(Node node, double x, double y) {
        if (!node.isVisible() || node.isMouseTransparent() || !node.contains(node.sceneToLocal(x, y))) {
            return null;
        }
        if (node instanceof ButtonBase button) {
            return button;
        }
        if (node instanceof Parent parent) {
            var children = parent.getChildrenUnmodifiable();
            for (int index = children.size() - 1; index >= 0; index--) {
                ButtonBase button = buttonAt(children.get(index), x, y);
                if (button != null) {
                    return button;
                }
            }
        }
        return null;
    }
}
