package fr.epitech.fakemon.ui;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.beans.property.ReadOnlyDoubleProperty;

/**
 * Centralise la navigation entre les écrans du jeu.
 */
public final class SceneManager implements AutoCloseable {

    private final Scene scene;
    private final VolumeSettings volumeSettings = new VolumeSettings();
    private final MenuSounds sounds = new MenuSounds(volumeSettings.effectsPercent());

    /** Donne le niveau de musique partagé par les écrans. */
    ReadOnlyDoubleProperty musicVolumePercent() {
        return volumeSettings.musicPercent();
    }

    /** Donne le niveau des effets sonores partagé par les écrans. */
    ReadOnlyDoubleProperty effectsVolumePercent() {
        return volumeSettings.effectsPercent();
    }

    /** Ouvre les réglages audio de la session. */
    public void showVolumeSettings() {
        volumeSettings.show(scene.getWindow(), sounds);
    }

    /**
     * Crée le gestionnaire pour la scène du jeu.
     *
     * @param scene scène dont le contenu sera remplacé
     */
    public SceneManager(Scene scene) {
        this.scene = scene;
        sounds.preload();
        sounds.install(scene);
    }

    /** Libère les sons de la session à la fermeture du jeu. */
    @Override
    public void close() {
        sounds.close();
    }

    /**
     * Affiche un écran dans la scène du jeu.
     *
     * @param screen conteneur principal de l'écran à afficher
     */
    public void show(Parent screen) {
        scene.setRoot(screen);
    }
}
