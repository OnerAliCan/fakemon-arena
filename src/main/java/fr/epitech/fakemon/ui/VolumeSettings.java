package fr.epitech.fakemon.ui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/** Niveaux audio partagés pendant toute la session, indépendamment du combat. */
final class VolumeSettings {
    private final DoubleProperty music = new SimpleDoubleProperty(100);
    private final DoubleProperty effects = new SimpleDoubleProperty(100);

    DoubleProperty musicPercent() {
        return music;
    }

    DoubleProperty effectsPercent() {
        return effects;
    }

    /** Ouvre le même réglage depuis les Options et la pause. */
    void show(Window owner, MenuSounds sounds) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Volume");
        Slider musicSlider = slider("Musique", musicPercent());
        Slider effectsSlider = slider("Effets sonores", effectsPercent());
        dialog.getDialogPane().setContent(new VBox(8, new Label("Musique"), musicSlider,
                new Label("Effets sonores"), effectsSlider));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        MenuSounds.action((javafx.scene.control.ButtonBase) dialog.getDialogPane().lookupButton(ButtonType.CLOSE),
                MenuSounds.Sound.NONE);
        dialog.setOnShown(event -> sounds.install(dialog.getDialogPane().getScene()));
        dialog.setOnHidden(event -> {
            sounds.play(MenuSounds.Sound.CLOSE);
            musicSlider.valueProperty().unbindBidirectional(music);
            effectsSlider.valueProperty().unbindBidirectional(effects);
        });
        dialog.showAndWait();
    }

    private Slider slider(String name, DoubleProperty value) {
        Slider control = new Slider(0, 100, value.get());
        control.setAccessibleText(name);
        control.setShowTickLabels(true);
        control.valueProperty().bindBidirectional(value);
        return control;
    }
}
