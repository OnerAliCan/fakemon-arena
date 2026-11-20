package fr.epitech.fakemon.ui;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.property.DoubleProperty;
import javafx.scene.Node;
import javafx.util.Duration;

/** Animations du combat ; aucune ne modifie les données du moteur. */
final class BattleAnimations {
    private BattleAnimations() { }

    static Animation banner(Node banner) {
        banner.setOpacity(0);
        FadeTransition in = new FadeTransition(Duration.millis(160), banner);
        in.setFromValue(0);
        in.setToValue(1);
        FadeTransition out = new FadeTransition(Duration.millis(160), banner);
        out.setFromValue(1);
        out.setToValue(0);
        return new SequentialTransition(in, new PauseTransition(Duration.millis(650)), out);
    }

    static Animation health(Node sprite, DoubleProperty displayedHp, int remaining, boolean impact) {
        Timeline hp = new Timeline(new KeyFrame(Duration.millis(550),
                new KeyValue(displayedHp, remaining, Interpolator.EASE_BOTH)));
        if (!impact) {
            return hp;
        }
        FadeTransition flash = new FadeTransition(Duration.millis(75), sprite);
        flash.setFromValue(1);
        flash.setToValue(.2);
        flash.setCycleCount(4);
        flash.setAutoReverse(true);
        return new ParallelTransition(hp, flash);
    }

    static Animation faint(Node sprite) {
        FadeTransition fade = new FadeTransition(Duration.millis(350), sprite);
        fade.setToValue(0);
        TranslateTransition fall = new TranslateTransition(Duration.millis(350), sprite);
        fall.setToY(24);
        return new ParallelTransition(fade, fall);
    }

    static Animation enter(Node sprite, boolean outgoing, boolean player, Runnable replace) {
        double offset = player ? -70 : 70;
        SequentialTransition sequence = new SequentialTransition();
        if (outgoing) {
            FadeTransition exit = new FadeTransition(Duration.millis(250), sprite);
            exit.setToValue(0);
            TranslateTransition slide = new TranslateTransition(Duration.millis(250), sprite);
            slide.setToX(offset);
            sequence.getChildren().add(new ParallelTransition(exit, slide));
        }
        PauseTransition change = new PauseTransition(Duration.millis(1));
        change.setOnFinished(event -> {
            replace.run();
            sprite.setOpacity(0);
            sprite.setTranslateX(offset);
            sprite.setTranslateY(0);
        });
        FadeTransition arrival = new FadeTransition(Duration.millis(350), sprite);
        arrival.setFromValue(0);
        arrival.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(350), sprite);
        slide.setFromX(offset);
        slide.setToX(0);
        sequence.getChildren().addAll(change, new ParallelTransition(arrival, slide));
        return sequence;
    }
}
