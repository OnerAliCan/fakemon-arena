package fr.epitech.fakemon.ui;


import fr.epitech.fakemon.core.api.BattleEndReason;
import fr.epitech.fakemon.core.api.Side;
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Team;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Résultat du combat, équipe du joueur et choix entre revanche et menu. */
public final class BattleEndScreen extends VBox {
    private final HBox actions = new HBox(16);
    private final Label replayStatus = new Label();
    /**
     * Construit le bilan sans décider elle-même comment créer la prochaine partie.
     * @param winner camp vainqueur
     * @param reason raison de la fin du combat
     * @param turns nombre de tours joués
     * @param playerTeam équipe du joueur dans son état final
     * @param replay action de nouveau tirage, ou null pour proposer le retour au menu
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
            ImageView sprite = BattleAssets.sprite(fakemon.getSpecies(), false, 64);
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
        Button menuButton = new Button("Menu");
        menuButton.setOnAction(event -> menu.run());
        if (replay != null) {
            offerReplay(replay);
        }
        actions.getChildren().add(menuButton);
        actions.setAlignment(Pos.CENTER);
        getChildren().addAll(title, turnLabel, reasonLabel, new Label("Votre équipe"), teamRow,
                actions, replayStatus);
        replayStatus.setWrapText(true);
        replayStatus.setVisible(false);
        replayStatus.setManaged(false);
    }

    /** Ajoute une proposition de revanche et empêche les demandes répétées. */
    void offerReplay(Runnable replay) {
        Button replayButton = new Button("Revanche");
        replayButton.setOnAction(event -> {
            replayButton.setDisable(true);
            replayStatus.setText("Préparation de la revanche…");
            replayStatus.setVisible(true);
            replayStatus.setManaged(true);
            replay.run();
        });
        actions.getChildren().addFirst(replayButton);
    }

    /** Affiche l'impossibilité de rejouer, en laissant le retour au menu disponible. */
    void replayFailed(Throwable error) {
        replayStatus.setText("Revanche impossible. " + error.getMessage());
        replayStatus.setVisible(true);
        replayStatus.setManaged(true);
    }
}
