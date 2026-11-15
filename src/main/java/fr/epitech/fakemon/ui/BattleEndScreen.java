package fr.epitech.fakemon.ui;

import java.util.Locale;
import java.util.Objects;

import fr.epitech.fakemon.core.api.BattleEndReason;
import fr.epitech.fakemon.core.api.Side;
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Team;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Résultat du combat, équipe du joueur et choix entre revanche et menu. */
public final class BattleEndScreen extends VBox {
    /**
     * Construit le bilan sans décider elle-même comment créer la prochaine partie.
     * @param winner camp vainqueur
     * @param reason raison de la fin du combat
     * @param turns nombre de tours joués
     * @param playerTeam équipe du joueur dans son état final
     * @param replay action de nouveau tirage
     * @param menu action de retour au menu existant
     */
    public BattleEndScreen(Side winner, BattleEndReason reason, int turns, Team playerTeam,
            Runnable replay, Runnable menu) {
        setAlignment(Pos.CENTER);
        setSpacing(16);
        setPadding(new Insets(24));
        Label title = new Label(winner == Side.PLAYER ? "Victoire !" : "Défaite");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: "
                + (winner == Side.PLAYER ? "#237d42" : "#b42c2c") + ";");
        Label turnLabel = new Label("Nombre de tours : " + turns);
        Label reasonLabel = new Label(reason == BattleEndReason.FORFEIT
                ? "Combat terminé par abandon." : "Combat terminé par K.O.");
        HBox teamRow = new HBox(16);
        teamRow.setAlignment(Pos.CENTER);
        for (Fakemon fakemon : playerTeam.getFakemons()) {
            String path = "/sprites/front/" + fakemon.getSpecies().getOriginalName().toLowerCase(Locale.ROOT) + ".png";
            ImageView sprite = new ImageView(new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream(path), "Sprite introuvable : " + path)));
            sprite.setFitWidth(64);
            sprite.setFitHeight(64);
            sprite.setPreserveRatio(true);
            sprite.setSmooth(false);
            if (fakemon.isFainted()) {
                javafx.scene.effect.ColorAdjust grayscale = new javafx.scene.effect.ColorAdjust();
                grayscale.setSaturation(-1);
                sprite.setEffect(grayscale);
                sprite.setOpacity(0.5);
            }
            Label name = new Label(fakemon.getSpecies().getFrenchName());
            name.setWrapText(true);
            VBox member = new VBox(6, sprite, name,
                    new Label(fakemon.isFainted() ? "K.O." : "PV : " + fakemon.getCurrentHp() + "/" + fakemon.getMaxHp()));
            member.setAlignment(Pos.CENTER);
            member.setPrefWidth(125);
            teamRow.getChildren().add(member);
        }
        Button replayButton = new Button("Revanche");
        replayButton.setOnAction(event -> replay.run());
        Button menuButton = new Button("Menu");
        menuButton.setOnAction(event -> menu.run());
        HBox actions = new HBox(16, replayButton, menuButton);
        actions.setAlignment(Pos.CENTER);
        getChildren().addAll(title, turnLabel, reasonLabel, new Label("Votre équipe"), teamRow,
                new Label("Démonstration : le moteur de combat sera branché dans #26."), actions);
    }
}
