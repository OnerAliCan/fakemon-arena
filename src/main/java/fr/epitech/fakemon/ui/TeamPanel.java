package fr.epitech.fakemon.ui;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

import fr.epitech.fakemon.core.api.SendFakemon;
import fr.epitech.fakemon.core.model.Fakemon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Présente l'équipe et transmet la sélection au combat. */
final class TeamPanel extends VBox {
    TeamPanel(List<Fakemon> members, Fakemon active, boolean required,
            Consumer<SendFakemon> onSelect, Runnable onCancel) {
        super(6);
        setPadding(new Insets(16));
        setMaxSize(660, Region.USE_PREF_SIZE);
        setStyle("-fx-background-color: #262b45; -fx-background-radius: 12;"
                + "-fx-border-color: #3a4170; -fx-border-radius: 12;");
        Label title = lightLabel(required ? "Choisis un remplaçant" : "Mon équipe");
        title.setStyle("-fx-text-fill: #fff5dd; -fx-font-size: 21px; -fx-font-weight: bold;");
        Label hint = lightLabel(required ? active.getSpecies().getFrenchName() + " est K.O. Envoie un autre fakemon."
                : "Envoyer un autre fakemon utilise ton tour.");
        getChildren().addAll(title, hint);
        for (Fakemon member : members) {
            getChildren().add(createRow(member, member == active, onSelect));
        }
        if (!required) {
            Button back = new Button("Retour au combat");
            back.setOnAction(event -> onCancel.run());
            getChildren().add(back);
        }
    }

    private HBox createRow(Fakemon member, boolean active, Consumer<SendFakemon> onSelect) {
        boolean fainted = member.isFainted();
        String spritePath = "/sprites/front/" + member.getSpecies().getOriginalName().toLowerCase(Locale.ROOT) + ".png";
        ImageView sprite = new ImageView(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream(spritePath), "Sprite introuvable : " + spritePath)));
        sprite.setFitWidth(42);
        sprite.setFitHeight(42);
        sprite.setPreserveRatio(true);
        sprite.setSmooth(false);
        if (fainted || active) {
            ColorAdjust gray = new ColorAdjust();
            gray.setSaturation(-1);
            sprite.setEffect(gray);
            sprite.setOpacity(0.65);
        }
        Label name = lightLabel(member.getSpecies().getFrenchName());
        name.setStyle("-fx-text-fill: #fff5dd; -fx-font-weight: bold;");
        VBox identity = new VBox(3, name, lightLabel(member.getSpecies().getTypes().toString()));
        identity.setMinWidth(148);
        double ratio = (double) member.getCurrentHp() / member.getMaxHp();
        ProgressBar hpBar = new ProgressBar(ratio);
        hpBar.setPrefWidth(130);
        hpBar.setStyle("-fx-accent: " + (ratio < 0.2 ? "#df6262" : ratio < 0.5 ? "#f2b648" : "#61ca69") + ";");
        VBox health = new VBox(3, hpBar, lightLabel("PV : " + member.getCurrentHp() + " / " + member.getMaxHp()));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button send = new Button(fainted ? "K.O." : active ? "En jeu" : "Envoyer");
        send.setMinWidth(90);
        send.setDisable(fainted || active);
        send.setStyle("-fx-base: #f6c443; -fx-font-weight: bold;");
        send.setAccessibleText("Envoyer " + member.getSpecies().getFrenchName());
        send.setOnAction(event -> onSelect.accept(new SendFakemon(member)));
        HBox row = new HBox(12, sprite, identity, health, spacer, send);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(3, 0, 3, 0));
        row.setStyle("-fx-border-color: #3a4170 transparent transparent transparent;");
        return row;
    }

    private Label lightLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #d6dcf5;");
        return label;
    }
}
