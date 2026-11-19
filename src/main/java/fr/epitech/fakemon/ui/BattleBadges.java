package fr.epitech.fakemon.ui;

import fr.epitech.fakemon.core.model.Item;
import fr.epitech.fakemon.core.model.Talent;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

/** Icônes vectorielles locales et noms lisibles des talents et objets du combat. */
final class BattleBadges {
    private BattleBadges() {}

    static String talentName(Talent talent) {
        return switch (talent) {
            case CARAPACE -> "Carapace";
            case DERNIER_SOUFFLE -> "Dernier souffle";
            case FRACAS -> "Fracas";
            case RECUPERATION -> "Récupération";
        };
    }

    /** Crée une nouvelle icône par cartouche, sans dépendre des polices système. */
    static StackPane itemIcon(Item item) {
        String name = item == null ? "Aucun objet" : item.getDisplayName();
        String drawing = item == null ? "M4 10 H16" : switch (item) {
            case ECU -> "M10 1 L18 4 V10 Q18 16 10 19 Q2 16 2 10 V4 Z";
            case PLUME -> "M2 18 L15 3 Q22 -1 18 9 L10 16 Z M4 16 L14 6 M8 12 L8 7 M11 10 L16 10";
            case GANTELET -> "M5 18 L2 11 L4 9 L7 12 V4 Q7 1 9 3 V9 V2 Q11 0 12 2 V9 V3 Q14 1 15 3 V10 V6 Q17 4 18 6 V13 L14 18 Z";
        };
        SVGPath shape = new SVGPath();
        shape.setContent(drawing);
        shape.setFill(item == null || item == Item.PLUME ? Color.TRANSPARENT : Color.web("#f6c443"));
        shape.setStroke(Color.web("#665022"));
        shape.setStrokeWidth(1.5);
        StackPane icon = new StackPane(shape);
        icon.setMinSize(24, 24);
        icon.setPrefSize(24, 24);
        icon.setMaxSize(24, 24);
        icon.setAccessibleRole(AccessibleRole.IMAGE_VIEW);
        icon.setAccessibleText(name);
        Tooltip.install(icon, new Tooltip(name));
        return icon;
    }

    static Label itemLabel(Item item) {
        Label label = new Label(item == null ? "Aucun objet" : item.getDisplayName(), itemIcon(item));
        label.setId("held-item");
        return label;
    }
}
