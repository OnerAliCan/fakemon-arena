package fr.epitech.fakemon.ui;

import fr.epitech.fakemon.core.api.SpeciesRepository;
import fr.epitech.fakemon.core.model.Species;
import java.util.Comparator;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;

/** Présente le catalogue des Fakemon dans l'ordre de leur numéro de Mondex. */
final class MondexScreen extends BorderPane {
    private final ListView<Species> list;
    private final Label title = new Label("Mondex");
    private final Button back = new Button("Retour au menu principal");
    private final Runnable onBack;
    private final ReadOnlyDoubleProperty effectsVolumePercent;
    private boolean showingDetails;

    /**
     * Charge la liste du catalogue et prépare le retour au menu.
     *
     * @param repository le catalogue des espèces intégrées et ajoutées par l'utilisateur
     * @param onBack l'action de retour au menu principal
     * @param effectsVolumePercent le volume partagé des effets sonores, de 0 à 100
     */
    MondexScreen(SpeciesRepository repository, Runnable onBack, ReadOnlyDoubleProperty effectsVolumePercent) {
        this.onBack = onBack;
        this.effectsVolumePercent = effectsVolumePercent;
        setPadding(new Insets(16));
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        setTop(title);
        BorderPane.setMargin(title, new Insets(0, 0, 12, 0));

        var orderedSpecies = repository.findAll().stream()
                .sorted(Comparator.comparingInt(Species::getDexNumber))
                .toList();
        list = new ListView<>(FXCollections.observableArrayList(orderedSpecies));
        list.setPlaceholder(new Label("Le catalogue est vide."));
        list.setCellFactory(view -> {
            ListCell<Species> cell = new ListCell<>() {
                @Override
                protected void updateItem(Species species, boolean empty) {
                    super.updateItem(species, empty);
                    if (empty || species == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(String.format("%03d — %s", species.getDexNumber(), species.getFrenchName()));
                        setGraphic(BattleAssets.sprite(species, false, 48));
                    }
                }
            };
            cell.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && !cell.isEmpty()) {
                    showDetails(cell.getItem());
                }
            });
            return cell;
        });
        list.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && list.getSelectionModel().getSelectedItem() != null) {
                event.consume();
                showDetails(list.getSelectionModel().getSelectedItem());
            }
        });
        setCenter(list);

        back.setOnAction(event -> goBack());
        MenuSounds.action(back, MenuSounds.Sound.NONE);
        setBottom(back);
        BorderPane.setMargin(back, new Insets(12, 0, 0, 0));
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                event.consume();
                goBack();
            }
        });
    }

    private void showDetails(Species species) {
        MenuSounds.play(this, MenuSounds.Sound.OPEN);
        showingDetails = true;
        title.setText("Détails du Fakemon");
        back.setText("Retour au Mondex");
        setCenter(new SpeciesDetailsPane(species, effectsVolumePercent));
        back.requestFocus();
    }

    private void goBack() {
        MenuSounds.play(this, MenuSounds.Sound.CLOSE);
        if (showingDetails) {
            showingDetails = false;
            title.setText("Mondex");
            back.setText("Retour au menu principal");
            setCenter(list);
            list.requestFocus();
        } else {
            onBack.run();
        }
    }
}
