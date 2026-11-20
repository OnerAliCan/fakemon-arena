package fr.epitech.fakemon.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.animation.Animation;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Vérifie les valeurs intermédiaires et finales sur le véritable thread JavaFX. */
@EnabledIfSystemProperty(named = "fakemon.ui.tests", matches = "true")
class BattleAnimationsUiTest {
    @Test
    void bannerFadesWithoutChangingItsPosition() throws Exception {
        var completed = FxTestSupport.run(() -> {
            var banner = new StackPane();
            banner.setLayoutY(25);
            var appeared = new AtomicBoolean();
            banner.opacityProperty().addListener((observable, old, value) -> {
                if (value.doubleValue() > .9) {
                    appeared.set(true);
                }
            });
            return play(BattleAnimations.banner(banner), () -> {
                assertTrue(appeared.get());
                assertEquals(0, banner.getOpacity(), .001);
                assertEquals(25, banner.getLayoutY(), .001);
                assertEquals(0, banner.getTranslateY(), .001);
            });
        });
        completed.get(5, TimeUnit.SECONDS);
    }

    @Test
    void healthPassesThroughIntermediateValuesAndRestoresOpacity() throws Exception {
        var completed = FxTestSupport.run(() -> {
            var sprite = new StackPane();
            var hp = new SimpleDoubleProperty(100);
            var intermediate = new AtomicBoolean();
            hp.addListener((observable, old, value) -> {
                if (value.doubleValue() > 0 && value.doubleValue() < 100) {
                    intermediate.set(true);
                }
            });
            Animation animation = BattleAnimations.health(sprite, hp, 0, true);
            return play(animation, () -> {
                assertTrue(intermediate.get());
                assertEquals(0, hp.get(), .001);
                assertEquals(1, sprite.getOpacity(), .001);
            });
        });
        completed.get(5, TimeUnit.SECONDS);
    }

    @Test
    void replacementWaitsForExitAndResetsTheFaintedSprite() throws Exception {
        var completed = FxTestSupport.run(() -> {
            var sprite = new StackPane();
            sprite.setTranslateY(24);
            var replaced = new AtomicBoolean();
            Animation animation = BattleAnimations.enter(sprite, true, true, () -> {
                assertEquals(0, sprite.getOpacity(), .001);
                replaced.set(true);
            });
            var result = play(animation, () -> {
                assertTrue(replaced.get());
                assertEquals(1, sprite.getOpacity(), .001);
                assertEquals(0, sprite.getTranslateX(), .001);
                assertEquals(0, sprite.getTranslateY(), .001);
            });
            assertFalse(replaced.get());
            return result;
        });
        completed.get(5, TimeUnit.SECONDS);
    }

    @Test
    void faintDisappearsBeforeCompletion() throws Exception {
        var completed = FxTestSupport.run(() -> {
            var sprite = new StackPane();
            return play(BattleAnimations.faint(sprite), () -> {
                assertEquals(0, sprite.getOpacity(), .001);
                assertEquals(24, sprite.getTranslateY(), .001);
            });
        });
        completed.get(5, TimeUnit.SECONDS);
    }

    private static CompletableFuture<Void> play(Animation animation, Runnable assertions) {
        var result = new CompletableFuture<Void>();
        animation.setOnFinished(event -> {
            try {
                assertions.run();
                result.complete(null);
            } catch (Throwable error) {
                result.completeExceptionally(error);
            }
        });
        animation.play();
        return result;
    }
}
