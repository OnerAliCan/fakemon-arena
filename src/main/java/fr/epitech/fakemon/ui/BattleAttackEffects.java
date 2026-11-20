package fr.epitech.fakemon.ui;

import fr.epitech.fakemon.core.model.Type;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.geometry.Point2D;
import javafx.scene.AccessibleRole;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.util.Duration;

/** Effets élémentaires superposés à l'arène, sans influencer sa disposition ni le moteur. */
final class BattleAttackEffects {
    private BattleAttackEffects() { }

    static Animation attack(Pane layer, Node source, Node target, Type type, boolean status) {
        Point2D from = center(layer, source);
        Point2D to = status ? from : center(layer, target);
        Group effect = motif(type);
        effect.setManaged(false);
        effect.setMouseTransparent(true);
        effect.setLayoutX(from.getX());
        effect.setLayoutY(from.getY());
        effect.setAccessibleRole(AccessibleRole.IMAGE_VIEW);
        effect.setAccessibleText("Attaque " + TypeBadges.name(type));
        layer.getChildren().add(effect);

        Timeline travel = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(effect.opacityProperty(), 0),
                        new KeyValue(effect.scaleXProperty(), .5),
                        new KeyValue(effect.scaleYProperty(), .5)),
                new KeyFrame(Duration.millis(80), new KeyValue(effect.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(500),
                        new KeyValue(effect.translateXProperty(), to.getX() - from.getX(), Interpolator.EASE_BOTH),
                        new KeyValue(effect.translateYProperty(), to.getY() - from.getY(), Interpolator.EASE_BOTH),
                        new KeyValue(effect.scaleXProperty(), status ? 1.8 : 1),
                        new KeyValue(effect.scaleYProperty(), status ? 1.8 : 1),
                        new KeyValue(effect.rotateProperty(), type == Type.ROCK || type == Type.NATURE ? 180 : 0)));
        FadeTransition fade = new FadeTransition(Duration.millis(180), effect);
        fade.setToValue(0);
        ScaleTransition expand = new ScaleTransition(Duration.millis(180), effect);
        expand.setToX(2);
        expand.setToY(2);
        PauseTransition cleanup = new PauseTransition(Duration.millis(1));
        cleanup.setOnFinished(event -> layer.getChildren().remove(effect));
        return new SequentialTransition(travel, new ParallelTransition(fade, expand), cleanup);
    }

    private static Point2D center(Pane layer, Node sprite) {
        var bounds = sprite.getBoundsInLocal();
        return layer.sceneToLocal(sprite.localToScene(bounds.getCenterX(), bounds.getCenterY()));
    }

    private static Group motif(Type type) {
        Color color = TypeBadges.color(type);
        Group group = new Group(new Circle(22, color.deriveColor(0, 1, 1, .22)));
        switch (type) {
            case FIRE -> {
                group.getChildren().addAll(new Circle(-20, 4, 10, color),
                        new Circle(-31, 9, 6, color.brighter()), new Circle(-39, 3, 3, Color.GOLD));
            }
            case WATER -> {
                group.getChildren().addAll(ring(25, color), ring(32, color.deriveColor(0, 1, 1, .4)),
                        new Circle(-21, -12, 5, color), new Circle(20, 15, 4, color));
            }
            case NATURE -> {
                var leaf = TypeBadges.icon(type);
                leaf.setTranslateX(-32);
                leaf.setRotate(-35);
                group.getChildren().addAll(leaf, new Circle(22, -18, 4, color));
            }
            case LIGHTNING -> {
                group.getChildren().addAll(new Polygon(-32, -12, -22, -6, -29, 3, -15, -4),
                        new Polygon(18, 15, 31, 7, 25, 21, 36, 15));
                group.getChildren().stream().filter(Polygon.class::isInstance)
                        .map(Polygon.class::cast).forEach(polygon -> polygon.setFill(color));
            }
            case ROCK -> group.getChildren().addAll(
                    shard(-26, -14, color), shard(24, 10, color.brighter()), shard(-12, 24, color));
            case WIND -> {
                for (int index = 0; index < 3; index++) {
                    Arc gust = new Arc(0, index * 10 - 10, 30, 12, 15, 220);
                    gust.setFill(Color.TRANSPARENT);
                    gust.setStroke(color);
                    gust.setStrokeWidth(2);
                    group.getChildren().add(gust);
                }
            }
            case VENOM -> group.getChildren().addAll(new Circle(-25, -10, 7, color),
                    new Circle(23, 12, 6, color.brighter()), new Circle(-12, 26, 4, color));
            case FORCE -> group.getChildren().addAll(ring(27, color),
                    new Polygon(-30, -20, -25, -4, -39, -8), new Polygon(24, 12, 38, 20, 28, 0));
            case SHADOW -> group.getChildren().addAll(new Circle(-20, -10, 13, color.deriveColor(0, 1, .7, .6)),
                    new Circle(18, 8, 14, color.deriveColor(0, 1, .7, .6)));
            case ETHER -> {
                for (int index = 0; index < 4; index++) {
                    double angle = index * Math.PI / 2;
                    group.getChildren().add(new Circle(Math.cos(angle) * 28, Math.sin(angle) * 28, 3, color));
                }
                group.getChildren().add(ring(23, color));
            }
        }
        group.getChildren().stream().filter(Polygon.class::isInstance)
                .map(Polygon.class::cast).forEach(polygon -> polygon.setFill(color));
        var icon = TypeBadges.icon(type);
        icon.setTranslateX(-12);
        icon.setTranslateY(-12);
        icon.setScaleX(1.6);
        icon.setScaleY(1.6);
        group.getChildren().add(icon);
        return group;
    }

    private static Circle ring(double radius, Color color) {
        Circle ring = new Circle(radius, Color.TRANSPARENT);
        ring.setStroke(color);
        ring.setStrokeWidth(2.5);
        return ring;
    }

    private static Polygon shard(double x, double y, Color color) {
        Polygon rock = new Polygon(x - 7, y, x - 3, y - 8, x + 7, y - 4, x + 5, y + 6);
        rock.setFill(color);
        return rock;
    }
}
