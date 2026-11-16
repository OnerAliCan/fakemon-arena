package fr.epitech.fakemon.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Menu de pause partagé par les combats aléatoire et construit. */
final class BattlePausePanel extends VBox {
    private final Button resume = new Button("Reprendre");

    BattlePausePanel(Runnable onResume, Runnable onVolume, Runnable onForfeit) {
        super(14);
        setAlignment(Pos.CENTER);
        resume.setOnAction(event -> onResume.run());
        Button volume = new Button("Volume");
        volume.setOnAction(event -> onVolume.run());
        Button forfeit = new Button("Abandonner");
        forfeit.setOnAction(event -> onForfeit.run());
        getChildren().addAll(new Label("Combat en pause"), resume, volume, forfeit);
    }

    void focusResume() {
        resume.requestFocus();
    }
}
