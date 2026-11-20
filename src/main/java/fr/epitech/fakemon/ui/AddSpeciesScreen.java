package fr.epitech.fakemon.ui;

import fr.epitech.fakemon.core.api.InvalidSpeciesException;
import fr.epitech.fakemon.core.api.SpeciesRepository;
import fr.epitech.fakemon.core.model.BaseStats;
import fr.epitech.fakemon.core.model.Move;
import fr.epitech.fakemon.core.model.Talent;
import fr.epitech.fakemon.core.model.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

/** Formulaire de création d'une espèce, avec aperçus, sélection d'attaques et validation du catalogue. */
final class AddSpeciesScreen extends BorderPane {
    private final SpeciesRepository repository;
    private final TextField original = field("original-name", "Nom d'origine");
    private final TextField french = field("french-name", "Nom français affiché dans le jeu");
    private final TextField designer = field("designer", "Facultatif");
    private final TextArea concept = new TextArea();
    private final ImageView frontPreview = preview("front-preview");
    private final ImageView backPreview = preview("back-preview");
    private final Label frontName = text("Aucun sprite avant");
    private final Label backName = text("Face retournée si aucun fichier arrière");
    private final Label cryName = text("Aucun cri choisi (facultatif)");
    private final Label audioMessage = text("");
    private final Label message = new Label();
    private final Label total = text("");
    private final Label moveCount = text("");
    private final TextField filter = field("moves-filter", "Rechercher une attaque ou un type");
    private final Map<Type, CheckBox> types = new LinkedHashMap<>();
    private final Map<Move, CheckBox> moves = new LinkedHashMap<>();
    private final List<Slider> stats = new ArrayList<>();
    private final ComboBox<Talent> talent = new ComboBox<>();
    private final Button listen = new Button("Écouter");
    private final Button save = new Button("Enregistrer le fakemon");
    private final Button another = new Button("Ajouter un autre fakemon");
    private final CryPreview audio;
    private Path front;
    private Path back;
    private Path cry;

    AddSpeciesScreen(SpeciesRepository repository, ReadOnlyDoubleProperty volume, Runnable menu) {
        this.repository = repository;
        audio = new CryPreview(volume, audioMessage::setText);
        setPadding(new Insets(16));
        setStyle("-fx-background-color: #19233a;");
        var heading = section("Ajouter un fakemon");
        heading.setStyle(heading.getStyle() + "-fx-font-size: 24px;");
        setTop(new VBox(5, heading, text("Crée sa fiche, choisis ses médias et ajoute-le au catalogue.")));

        VBox identity = new VBox(10, section("Identité"), labeled("Nom d'origine *", original),
                labeled("Nom français *", french), section("Sprites"), sprites(), cryControls(),
                section("Création"), labeled("Designer", designer));
        concept.setId("concept");
        concept.setPromptText("Concept du fakemon (facultatif)");
        concept.setPrefRowCount(3);
        concept.setWrapText(true);
        identity.getChildren().add(labeled("Concept", concept));
        VBox abilities = new VBox(10, section("Types (1 ou 2) *"), typeControls(), section("Statistiques"),
                text("Chaque valeur : 50 à 150. Total maximum : 600."), statControls(), total,
                section("Talent *"), talentControls(), section("Attaques disponibles *"),
                text("Coche au moins quatre attaques. Le filtre conserve tes choix."), filter, moveControls(), moveCount);
        GridPane form = new GridPane();
        form.setHgap(28);
        form.setPadding(new Insets(16, 4, 16, 0));
        for (int index = 0; index < 2; index++) {
            var column = new ColumnConstraints();
            column.setPercentWidth(50);
            form.getColumnConstraints().add(column);
        }
        identity.setMinWidth(0);
        abilities.setMinWidth(0);
        form.add(identity, 0, 0);
        form.add(abilities, 1, 0);
        var scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #19233a; -fx-background-color: #19233a;");
        setCenter(scroll);

        message.setId("form-message");
        message.setWrapText(true);
        message.setMaxWidth(Double.MAX_VALUE);
        message.setMinHeight(24);
        save.setId("save-species");
        save.setStyle("-fx-base: #f6c443; -fx-font-weight: bold;");
        MenuSounds.action(save, MenuSounds.Sound.NONE);
        save.setOnAction(event -> save());
        another.setVisible(false);
        another.setManaged(false);
        another.setOnAction(event -> reset());
        Button backButton = new Button("Retour au menu");
        MenuSounds.action(backButton, MenuSounds.Sound.CLOSE);
        backButton.setOnAction(event -> { audio.close(); menu.run(); });
        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        var actions = new HBox(10, backButton, spacer, another, save);
        actions.setAlignment(Pos.CENTER_LEFT);
        setBottom(new VBox(8, message, actions));
        sceneProperty().addListener((property, previous, current) -> {
            if (previous != null && current == null) {
                audio.close();
            }
        });
    }

    private Node sprites() {
        Button frontButton = new Button("Choisir l'avant…");
        Button backButton = new Button("Choisir l'arrière…");
        frontButton.setOnAction(event -> chooseSprite(false));
        backButton.setOnAction(event -> chooseSprite(true));
        Button clearBack = new Button("Utiliser la face retournée");
        clearBack.setOnAction(event -> { back = null; updateBack(); });
        frontName.setMaxWidth(160);
        backName.setMaxWidth(180);
        var previews = new HBox(12, new VBox(6, imageFrame(frontPreview), frontButton, frontName),
                new VBox(6, imageFrame(backPreview), backButton, backName));
        return new VBox(8, previews, clearBack);
    }

    private Node cryControls() {
        Button choose = new Button("Choisir le cri…");
        Button clear = new Button("Retirer");
        choose.setOnAction(event -> {
            var chooser = new FileChooser();
            chooser.setTitle("Choisir le cri du fakemon");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio WAV ou MP3", "*.wav", "*.mp3"));
            var selected = chooser.showOpenDialog(getScene().getWindow());
            if (selected != null) {
                selectCry(selected.toPath());
            }
        });
        clear.setOnAction(event -> { audio.close(); cry = null; cryName.setText("Aucun cri choisi (facultatif)");
            audioMessage.setText(""); listen.setDisable(true); });
        listen.setId("listen-cry");
        audioMessage.setId("cry-message");
        listen.setDisable(true);
        MenuSounds.action(listen, MenuSounds.Sound.NONE);
        listen.setOnAction(event -> audio.play(cry));
        return new VBox(6, section("Cri"), new HBox(8, choose, listen, clear), cryName, audioMessage);
    }

    /** Remplace le fichier audio en arrêtant toute lecture précédente. */
    void selectCry(Path file) {
        audio.close();
        cry = file;
        cryName.setText(file.getFileName().toString());
        audioMessage.setText("");
        listen.setDisable(false);
    }

    private Node typeControls() {
        var choices = new FlowPane(12, 8);
        for (Type type : Type.values()) {
            var choice = new CheckBox(TypeBadges.name(type));
            choice.setId("type-" + type.name());
            choice.setStyle("-fx-text-fill: #fff5dd;");
            choice.setGraphic(TypeBadges.icon(type));
            types.put(type, choice);
            choice.selectedProperty().addListener((observable, previous, current) -> {
                long count = types.values().stream().filter(CheckBox::isSelected).count();
                types.values().forEach(box -> box.setDisable(count >= 2 && !box.isSelected()));
            });
            choices.getChildren().add(choice);
        }
        return choices;
    }

    private Node statControls() {
        var grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        String[] labels = {"PV", "Attaque", "Défense", "Attaque spéciale", "Défense spéciale", "Vitesse"};
        String[] ids = {"hp", "attack", "defense", "special-attack", "special-defense", "speed"};
        for (int index = 0; index < labels.length; index++) {
            var slider = new Slider(50, 150, 100);
            slider.setId("stat-" + ids[index]);
            slider.setAccessibleText(labels[index]);
            slider.setBlockIncrement(1);
            slider.setMajorTickUnit(50);
            slider.setMinorTickCount(49);
            slider.setSnapToTicks(true);
            slider.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(slider, Priority.ALWAYS);
            var value = text("100");
            value.setMinWidth(28);
            stats.add(slider);
            slider.valueProperty().addListener((observable, previous, current) -> {
                value.setText(Long.toString(Math.round(current.doubleValue())));
                updateTotal();
            });
            grid.addRow(index, text(labels[index]), slider, value);
        }
        total.setId("stat-total");
        updateTotal();
        return grid;
    }

    private void updateTotal() {
        int sum = stats.stream().mapToInt(slider -> (int) Math.round(slider.getValue())).sum();
        total.setText("Total : " + sum + " / 600");
        total.setStyle("-fx-font-weight: bold; -fx-text-fill: " + (sum > 600 ? "#ffb4a9" : "#f6c443") + ";");
    }

    private Node talentControls() {
        talent.setId("talent");
        talent.getItems().setAll(Talent.values());
        talent.setPromptText("Choisir un talent");
        talent.setMaxWidth(Double.MAX_VALUE);
        talent.setConverter(new StringConverter<>() {
            @Override public String toString(Talent value) { return value == null ? "" : BattleBadges.talentName(value); }
            @Override public Talent fromString(String value) { throw new UnsupportedOperationException(); }
        });
        return talent;
    }

    private Node moveControls() {
        VBox list = new VBox(6);
        int index = 0;
        for (Move move : SpeciesDraft.availableMoves(repository)) {
            var choice = new CheckBox(move.getName() + " (" + TypeBadges.name(move.getType()) + ")");
            choice.setId("move-" + index++);
            choice.setUserData(move);
            choice.setStyle("-fx-text-fill: #fff5dd;");
            choice.setWrapText(true);
            choice.managedProperty().bind(choice.visibleProperty());
            choice.selectedProperty().addListener((observable, previous, current) -> updateMoveCount());
            moves.put(move, choice);
            list.getChildren().add(choice);
        }
        filter.textProperty().addListener((observable, previous, current) ->
                moves.forEach((move, choice) -> choice.setVisible(SpeciesDraft.matches(move, current))));
        var scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(200);
        scroll.setMinHeight(140);
        scroll.setStyle("-fx-background: #262b45; -fx-background-color: #262b45; -fx-padding: 8;");
        updateMoveCount();
        return scroll;
    }

    private void updateMoveCount() {
        long selected = moves.values().stream().filter(CheckBox::isSelected).count();
        moveCount.setText(selected + " attaque(s) sélectionnée(s) — minimum 4");
    }

    private void chooseSprite(boolean rear) {
        var chooser = new FileChooser();
        chooser.setTitle(rear ? "Choisir le sprite arrière" : "Choisir le sprite avant");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images PNG, JPEG ou GIF", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        var selected = chooser.showOpenDialog(getScene().getWindow());
        if (selected != null) {
            selectSprite(selected.toPath(), rear);
        }
    }

    /** Valide l'image avant de remplacer le choix précédent. */
    void selectSprite(Path path, boolean rear) {
        try {
            Image image = new Image(path.toUri().toString());
            if (image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0) {
                throw new IllegalArgumentException("Choisis une image PNG, JPEG ou GIF lisible.");
            }
            if (rear) {
                back = path;
                backPreview.setImage(image);
                backPreview.setScaleX(1);
                backName.setText(path.getFileName().toString());
            } else {
                front = path;
                frontPreview.setImage(image);
                frontName.setText(path.getFileName().toString());
            }
            updateBack();
            message.setText("");
        } catch (RuntimeException error) {
            error("Impossible d'ouvrir l'image. Choisis un fichier PNG, JPEG ou GIF valide.");
        }
    }

    private void updateBack() {
        if (back == null) {
            backPreview.setImage(frontPreview.getImage());
            backPreview.setScaleX(-1);
            backName.setText("Face retournée automatiquement");
        }
    }

    private void save() {
        try {
            var selectedTypes = types.entrySet().stream().filter(entry -> entry.getValue().isSelected()).map(Map.Entry::getKey).toList();
            var selectedMoves = moves.entrySet().stream().filter(entry -> entry.getValue().isSelected()).map(Map.Entry::getKey).toList();
            int[] values = stats.stream().mapToInt(slider -> (int) Math.round(slider.getValue())).toArray();
            var draft = new SpeciesDraft(original.getText(), french.getText(), selectedTypes,
                    new BaseStats(values[0], values[1], values[2], values[3], values[4], values[5]), selectedMoves,
                    talent.getValue(), front, back, cry, designer.getText(), concept.getText());
            repository.add(draft.build(repository));
            audio.close();
            message.setStyle("-fx-text-fill: #b6e3a3;");
            message.setText(french.getText().strip() + " a rejoint le catalogue. Tu peux le retrouver dans le Mondex et composer une équipe avec lui.");
            save.setDisable(true);
            another.setVisible(true);
            another.setManaged(true);
        } catch (InvalidSpeciesException | IllegalStateException exception) {
            error(exception.getMessage());
        }
    }

    private void error(String explanation) {
        message.setStyle("-fx-text-fill: #ffb4a9;");
        message.setText(explanation);
    }

    private void reset() {
        audio.close();
        original.clear(); french.clear(); designer.clear(); concept.clear(); filter.clear();
        types.values().forEach(box -> box.setSelected(false));
        moves.values().forEach(box -> box.setSelected(false));
        stats.forEach(slider -> slider.setValue(100));
        talent.setValue(null);
        front = null; back = null; cry = null;
        frontPreview.setImage(null); backPreview.setImage(null);
        frontName.setText("Aucun sprite avant"); backName.setText("Face retournée si aucun fichier arrière");
        cryName.setText("Aucun cri choisi (facultatif)"); audioMessage.setText(""); listen.setDisable(true);
        save.setDisable(false); another.setVisible(false); another.setManaged(false); message.setText("");
        ((ScrollPane) getCenter()).setVvalue(0);
        original.requestFocus();
    }

    private static TextField field(String id, String prompt) {
        var field = new TextField();
        field.setId(id);
        field.setPromptText(prompt);
        return field;
    }

    private static ImageView preview(String id) {
        var image = new ImageView();
        image.setId(id);
        image.setFitWidth(96); image.setFitHeight(96); image.setPreserveRatio(true); image.setSmooth(false);
        return image;
    }

    private static StackPane imageFrame(ImageView image) {
        var frame = new StackPane(image);
        frame.setPrefSize(140, 120); frame.setMinSize(140, 120);
        frame.setStyle("-fx-background-color: #303b55; -fx-border-color: #65718d;");
        return frame;
    }

    private static VBox labeled(String label, Node control) {
        var caption = text(label);
        caption.setLabelFor(control);
        return new VBox(5, caption, control);
    }

    private static Label text(String value) {
        var label = new Label(value);
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: #d6dcf5;");
        return label;
    }

    private static Label section(String value) {
        var label = text(value);
        label.setStyle("-fx-text-fill: #fff5dd; -fx-font-size: 17px; -fx-font-weight: bold;");
        return label;
    }
}
