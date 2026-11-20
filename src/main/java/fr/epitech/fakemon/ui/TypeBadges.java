package fr.epitech.fakemon.ui;

import fr.epitech.fakemon.core.model.Type;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Locale;
import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Pattern;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.SVGPath;

/** Pictogrammes locaux des types, accompagnés de leur nom français. */
final class TypeBadges {
    // Format volontairement limité aux SVG du projet : un chemin et une couleur.
    private static final Pattern PATH = Pattern.compile("<path fill=\"([^\"]+)\" d=\"([^\"]+)\"");

    private TypeBadges() {}

    static String name(Type type) {
        return switch (type) {
            case FIRE -> "Feu";
            case WATER -> "Eau";
            case NATURE -> "Nature";
            case LIGHTNING -> "Foudre";
            case ROCK -> "Roche";
            case WIND -> "Vent";
            case VENOM -> "Venin";
            case FORCE -> "Force";
            case SHADOW -> "Ombre";
            case ETHER -> "Éther";
        };
    }

    private record Symbol(String color, String drawing) { }

    private static final Map<Type, Symbol> SYMBOLS = loadSymbols();

    private static Map<Type, Symbol> loadSymbols() {
        Map<Type, Symbol> symbols = new EnumMap<>(Type.class);
        for (Type type : Type.values()) {
            String resource = "/icons/types/" + type.name().toLowerCase(Locale.ROOT) + ".svg";
            try (var stream = TypeBadges.class.getResourceAsStream(resource)) {
                if (stream == null) {
                    throw new IllegalStateException("Icône de type absente : " + resource);
                }
                var path = PATH.matcher(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
                if (!path.find()) {
                    throw new IllegalStateException("Icône de type invalide : " + resource);
                }
                symbols.put(type, new Symbol(path.group(1), path.group(2)));
            } catch (IOException error) {
                throw new IllegalStateException("Lecture impossible : " + resource, error);
            }
        }
        return Map.copyOf(symbols);
    }

    static Color color(Type type) {
        return Color.web(SYMBOLS.get(type).color());
    }

    static String colorCss(Type type) {
        return SYMBOLS.get(type).color();
    }

    static SVGPath icon(Type type) {
        SVGPath icon = new SVGPath();
        icon.setContent(SYMBOLS.get(type).drawing());
        icon.setFill(color(type));
        icon.setFillRule(FillRule.EVEN_ODD);
        icon.setAccessibleRole(AccessibleRole.IMAGE_VIEW);
        icon.setAccessibleText(name(type));
        return icon;
    }

    static Label badge(Type type) {
        Label badge = new Label(name(type), icon(type));
        badge.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #222222; "
                + "-fx-padding: 3 7; -fx-background-radius: 6; -fx-font-size: 12px;");
        badge.setAccessibleText("Type " + name(type));
        return badge;
    }

    static FlowPane row(Collection<Type> types) {
        FlowPane row = new FlowPane(6, 4);
        row.setAlignment(Pos.CENTER_LEFT);
        for (Type type : types) {
            row.getChildren().add(badge(type));
        }
        return row;
    }
}
