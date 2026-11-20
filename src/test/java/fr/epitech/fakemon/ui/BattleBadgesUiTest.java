package fr.epitech.fakemon.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import fr.epitech.fakemon.core.api.BattleEvent;
import fr.epitech.fakemon.core.api.ItemActivatedEvent;
import fr.epitech.fakemon.core.api.Side;
import fr.epitech.fakemon.core.api.TalentActivatedEvent;
import fr.epitech.fakemon.core.model.Item;
import fr.epitech.fakemon.core.model.Talent;
import fr.epitech.fakemon.data.JsonSpeciesRepository;
import fr.epitech.fakemon.data.SpeciesFactory;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

/** Vérification JavaFX optionnelle, à lancer sur un poste avec affichage. */
@EnabledIfSystemProperty(named = "fakemon.ui.tests", matches = "true")
class BattleBadgesUiTest {
    @TempDir java.nio.file.Path userFolder;

    @Test
    void bannersIdentifyBothSidesAndClearAfterTheirDisplay() throws Exception {
        var done = new CompletableFuture<Void>();
        Platform.startup(() -> {
            try {
                var setup = BattleSetup.random(new JsonSpeciesRepository(new SpeciesFactory(), userFolder), new Random(29));
                var scenes = new SceneManager(new Scene(new StackPane(), 960, 540));
                var screen = new BattleScreen(scenes, setup, () -> {});
                screen.applyCss();
                var arena = screen.getCenter();
                String backgroundStyle = arena.getStyle();
                assertTrue(backgroundStyle.contains("/backgrounds/battle-"));
                var render = BattleScreen.class.getDeclaredMethod("render", BattleEvent.class);
                render.setAccessible(true);
                var next = BattleScreen.class.getDeclaredMethod("renderNext");
                next.setAccessible(true);
                for (int index = 0; index < 100; index++) {
                    next.invoke(screen);
                }
                for (Item item : Item.values()) {
                    render.invoke(screen, new ItemActivatedEvent(Side.PLAYER, setup.player.getFakemons().getFirst(), item));
                    assertTrue(screen.lookup("#player-activation").isVisible());
                    assertTrue(screen.lookup("#player-activation").getAccessibleText().contains(item.getDisplayName()));
                    assertEquals(item.getDisplayName(), BattleBadges.itemIcon(item).getAccessibleText());
                }
                for (Talent talent : Talent.values()) {
                    render.invoke(screen, new TalentActivatedEvent(Side.OPPONENT, setup.opponent.getFakemons().getFirst(), talent));
                    assertTrue(screen.lookup("#opponent-activation").isVisible());
                    assertTrue(screen.lookup("#opponent-activation").getAccessibleText().contains(BattleBadges.talentName(talent)));
                }
                next.invoke(screen);
                assertFalse(screen.lookup("#player-activation").isVisible());
                assertFalse(screen.lookup("#opponent-activation").isVisible());
                assertEquals(backgroundStyle, arena.getStyle());
                done.complete(null);
            } catch (Throwable error) {
                done.completeExceptionally(error);
            }
        });
        try {
            done.get(20, TimeUnit.SECONDS);
        } finally {
            Platform.exit();
        }
    }
}
