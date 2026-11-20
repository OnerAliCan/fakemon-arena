package fr.epitech.fakemon.ui;

import static org.junit.jupiter.api.Assertions.*;
import fr.epitech.fakemon.core.api.FakemonEnteredEvent;
import fr.epitech.fakemon.core.api.FakemonFaintedEvent;
import fr.epitech.fakemon.core.api.HpLostEvent;
import fr.epitech.fakemon.core.api.Side;
import fr.epitech.fakemon.data.JsonSpeciesRepository;
import fr.epitech.fakemon.data.SpeciesFactory;
import java.nio.file.Path;
import java.util.Random;
import javafx.animation.Animation;
import javafx.beans.property.DoubleProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

/** Exerce la file complète : PV, KO, remplacement, puis fermeture pendant une animation. */
@EnabledIfSystemProperty(named = "fakemon.ui.tests", matches = "true")
class BattlePresentationUiTest {
    @TempDir Path userFolder;

    @Test
    void damageFinishesBeforeFaintAndEntryAndNavigationStopsPlayback() throws Exception {
        var setup = BattleSetup.random(new JsonSpeciesRepository(new SpeciesFactory(), userFolder), new Random(27));
        var scenes = FxTestSupport.run(() -> new SceneManager(new Scene(new StackPane(), 960, 540)));
        var screen = FxTestSupport.run(() -> {
            var battle = new BattleScreen(scenes, setup, () -> {});
            scenes.show(battle);
            return battle;
        });
        try {
            FxTestSupport.awaitBattle(screen);
            Object fighter = FxTestSupport.run(() -> field(screen, "opponent"));
            var old = setup.opponent.getFakemons().getFirst();
            var replacement = setup.opponent.getFakemons().get(1);
            var hp = FxTestSupport.run(() -> (DoubleProperty) field(fighter, "displayedHp"));
            var sprite = FxTestSupport.run(() -> (Node) field(fighter, "sprite"));
            double before = FxTestSupport.run(hp::get);
            FxTestSupport.run(() -> {
                screen.onEvent(new HpLostEvent(Side.OPPONENT, old, (int) before, 0));
                screen.onEvent(new FakemonFaintedEvent(Side.OPPONENT, old));
                screen.onEvent(new FakemonEnteredEvent(Side.OPPONENT, replacement));
                start(screen);
                return null;
            });
            FxTestSupport.await(() -> hp.get() > 0 && hp.get() < before);
            FxTestSupport.run(() -> {
                start(screen); // Même un nouveau réveil de la file ne saute pas les PV.
                assertSame(old, field(fighter, "mon"));
                assertTrue(hp.get() > 0);
                return null;
            });
            FxTestSupport.await(() -> sprite.getOpacity() == 0 && hp.get() == 0);
            FxTestSupport.run(() -> {
                assertSame(old, field(fighter, "mon"), "Le KO doit précéder le remplacement.");
                return null;
            });
            FxTestSupport.await(() -> field(fighter, "mon") == replacement
                    && field(screen, "presentation") == null);
            FxTestSupport.run(() -> {
                assertEquals(1, sprite.getOpacity(), .001);
                assertEquals(0, sprite.getTranslateY(), .001);
                screen.onEvent(new HpLostEvent(Side.OPPONENT, replacement, 1, replacement.getCurrentHp() - 1));
                start(screen);
                var active = (Animation) field(screen, "presentation");
                assertEquals(Animation.Status.RUNNING, active.getStatus());
                scenes.show(new StackPane());
                assertEquals(Animation.Status.STOPPED, active.getStatus());
                assertNull(field(screen, "presentation"));
                assertEquals(true, field(screen, "closed"));
                return null;
            });
        } finally {
            FxTestSupport.run(() -> {
                scenes.show(new StackPane());
                return null;
            });
        }
    }

    private static Object field(Object object, String name) throws Exception {
        var field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    private static void start(BattleScreen screen) throws Exception {
        var next = BattleScreen.class.getDeclaredMethod("renderNext");
        next.setAccessible(true);
        next.invoke(screen);
    }
}
