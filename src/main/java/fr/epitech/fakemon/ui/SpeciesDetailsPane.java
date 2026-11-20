package fr.epitech.fakemon.ui;

import fr.epitech.fakemon.core.model.Species;
import java.text.NumberFormat;
import java.util.Locale;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/** Affiche les informations du catalogue pour une espèce de Fakemon. */
final class SpeciesDetailsPane extends ScrollPane {
    private final ReadOnlyDoubleProperty effectsVolumePercent;
    private final Label audioStatus = new Label();
    private MediaPlayer player;

    /**
     * Présente la fiche et masque les informations facultatives absentes.
     *
     * @param species l'espèce sélectionnée dans le Mondex
     * @param effectsVolumePercent le volume partagé des effets sonores, de 0 à 100
     */
    SpeciesDetailsPane(Species species, ReadOnlyDoubleProperty effectsVolumePercent) {
        this.effectsVolumePercent = effectsVolumePercent;
        setFitToWidth(true);
        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        Label name = new Label(String.format("%03d — %s", species.getDexNumber(), species.getFrenchName()));
        name.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        name.setWrapText(true);
        content.getChildren().addAll(name, text("Nom d'origine — " + species.getOriginalName()),
                BattleAssets.sprite(species, false, 128),
                TypeBadges.row(species.getTypes()));

        var stats = species.getStats();
        NumberFormat numbers = NumberFormat.getNumberInstance(Locale.FRANCE);
        GridPane statistics = new GridPane();
        statistics.setHgap(24);
        statistics.setVgap(6);
        statistics.addRow(0, text("PV"), text(numbers.format(stats.getHp())));
        statistics.addRow(1, text("Attaque"), text(numbers.format(stats.getAttack())));
        statistics.addRow(2, text("Défense"), text(numbers.format(stats.getDefense())));
        statistics.addRow(3, text("Attaque spéciale"), text(numbers.format(stats.getSpecialAttack())));
        statistics.addRow(4, text("Défense spéciale"), text(numbers.format(stats.getSpecialDefense())));
        statistics.addRow(5, text("Vitesse"), text(numbers.format(stats.getSpeed())));
        content.getChildren().add(statistics);

        String talent = switch (species.getTalent()) {
            case CARAPACE -> "Carapace";
            case DERNIER_SOUFFLE -> "Dernier Souffle";
            case FRACAS -> "Fracas";
            case RECUPERATION -> "Récupération";
        };
        content.getChildren().add(text("Talent — " + talent));
        Button cry = new Button("Écouter le cri");
        String cryPath = species.getCry();
        boolean missingCry = cryPath == null || cryPath.isBlank();
        cry.setDisable(missingCry);
        audioStatus.setText(missingCry ? "Cri indisponible." : "");
        audioStatus.setWrapText(true);
        audioStatus.visibleProperty().bind(audioStatus.textProperty().isNotEmpty());
        audioStatus.managedProperty().bind(audioStatus.visibleProperty());
        cry.setOnAction(event -> playCry(cryPath));
        content.getChildren().addAll(cry, audioStatus);
        String designer = species.getDesigner();
        content.getChildren().add(text("Créé par : "
                + (designer == null || designer.isBlank() ? "Inconnu(e)" : designer)));
        String concept = species.getConcept();
        if (concept != null && !concept.isBlank()) {
            content.getChildren().add(text("Concept — " + concept));
        }
        setContent(content);
        sceneProperty().addListener((property, previous, current) -> {
            if (previous != null && current == null) {
                stopCry();
            }
        });
    }

    private void playCry(String path) {
        stopCry();
        audioStatus.setText("");
        try {
            Media media = new Media(BattleAssets.source(path));
            MediaPlayer candidate = new MediaPlayer(media);
            player = candidate;
            candidate.volumeProperty().bind(effectsVolumePercent.divide(100));
            media.setOnError(() -> audioFailed(candidate));
            candidate.setOnError(() -> audioFailed(candidate));
            candidate.setOnEndOfMedia(() -> {
                if (player == candidate) {
                    stopCry();
                }
            });
            if (media.getError() != null || candidate.getError() != null) {
                audioFailed(candidate);
            } else {
                candidate.play();
            }
        } catch (RuntimeException error) {
            stopCry();
            audioStatus.setText("Impossible de lire le cri de ce Fakemon.");
        }
    }

    private void audioFailed(MediaPlayer candidate) {
        if (player == candidate) {
            stopCry();
            audioStatus.setText("Impossible de lire le cri de ce Fakemon.");
        }
    }

    private void stopCry() {
        if (player != null) {
            MediaPlayer previous = player;
            player = null;
            previous.volumeProperty().unbind();
            previous.setOnEndOfMedia(null);
            previous.setOnError(null);
            previous.getMedia().setOnError(null);
            previous.dispose();
        }
    }

    private Label text(String value) {
        Label label = new Label(value);
        label.setWrapText(true);
        return label;
    }
}
