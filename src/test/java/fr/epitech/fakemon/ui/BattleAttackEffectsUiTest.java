package fr.epitech.fakemon.ui;

import static org.junit.jupiter.api.Assertions.*;
import fr.epitech.fakemon.core.model.Type;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Vérifie les dix effets, leur trajectoire et leur suppression après lecture. */
@EnabledIfSystemProperty(named = "fakemon.ui.tests", matches = "true")
class BattleAttackEffectsUiTest {
    @Test
    void everyElementTravelsToTargetAndCleansUp() throws Exception {
        FxTestSupport.run(() -> effects(false)).get(5, TimeUnit.SECONDS);
    }

    @Test
    void statusMovesAnimateCastingWithoutTravellingToOpponent() throws Exception {
        FxTestSupport.run(() -> effects(true)).get(5, TimeUnit.SECONDS);
    }

    private static CompletableFuture<Void> effects(boolean status) {
        var source = new Circle(40, 200, 10);
        var target = new Circle(400, 40, 10);
        var layer = new Pane();
        new Pane(source, target, layer);
        var completions = new ArrayList<CompletableFuture<Void>>();
        for (Type type : Type.values()) {
            var result = new CompletableFuture<Void>();
            completions.add(result);
            var animation = BattleAttackEffects.attack(layer, source, target, type, status);
            var effect = layer.getChildren().getLast();
            assertFalse(effect.isManaged());
            assertTrue(effect.isMouseTransparent());
            assertEquals("Attaque " + TypeBadges.name(type), effect.getAccessibleText());
            animation.setOnFinished(event -> {
                try {
                    assertFalse(layer.getChildren().contains(effect));
                    assertEquals(status ? 0 : 360, effect.getTranslateX(), .001);
                    assertEquals(status ? 0 : -160, effect.getTranslateY(), .001);
                    result.complete(null);
                } catch (Throwable error) {
                    result.completeExceptionally(error);
                }
            });
            animation.play();
        }
        return CompletableFuture.allOf(completions.toArray(CompletableFuture[]::new));
    }
}
