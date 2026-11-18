package fr.epitech.fakemon.ui;

import java.nio.file.Path;

import fr.epitech.fakemon.core.model.Species;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/** Charge les sprites du catalogue embarqué ou du dossier utilisateur. */
final class BattleAssets {
    private BattleAssets() {}

    static Path userFolder() {
        return Path.of(System.getProperty("user.home"), ".fakemon-arena");
    }

    static ImageView sprite(Species species, boolean back, int size) {
        Image front = back ? load(species.getBackSprite()) : null;
        boolean flip = back && front == null;
        var view = new ImageView(front == null ? load(species.getFrontSprite()) : front);
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(true);
        view.setSmooth(false);
        if (flip) {
            view.setScaleX(-1);
        }
        return view;
    }

    private static Image load(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        Image image = new Image(source(path));
        return image.isError() ? null : image;
    }

    /** Résout un média intégré au jeu ou enregistré dans le dossier utilisateur. */
    static String source(String path) {
        var resource = BattleAssets.class.getResource(path.startsWith("/") ? path : "/" + path);
        return resource != null ? resource.toExternalForm() : userFolder().resolve(path).toUri().toString();
    }
}
