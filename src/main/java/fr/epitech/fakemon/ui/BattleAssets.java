package fr.epitech.fakemon.ui;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

import fr.epitech.fakemon.core.model.Species;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/** Résout les décors et les médias du catalogue embarqué ou du dossier utilisateur. */
final class BattleAssets {
    private static final List<String> BACKGROUNDS = List.of(
            "/backgrounds/battle-grassland.png",
            "/backgrounds/battle-forest.png",
            "/backgrounds/battle-beach.png",
            "/backgrounds/battle-desert.png",
            "/backgrounds/battle-snow.png");

    private BattleAssets() {}

    /**
     * Tire un décor parmi les cinq paysages embarqués, une fois au début du combat.
     *
     * @param random source de hasard visuel, distincte de celle du moteur
     * @return l'adresse du décor choisi dans les ressources du jeu
     */
    static URL randomBackground(RandomGenerator random) {
        String path = BACKGROUNDS.get(random.nextInt(BACKGROUNDS.size()));
        return Objects.requireNonNull(BattleAssets.class.getResource(path), "Décor absent : " + path);
    }

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
