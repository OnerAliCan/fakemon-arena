package fr.epitech.fakemon.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import fr.epitech.fakemon.core.api.BattleEvent;
import fr.epitech.fakemon.core.api.ItemActivatedEvent;
import fr.epitech.fakemon.core.api.Side;
import fr.epitech.fakemon.core.api.TalentActivatedEvent;
import fr.epitech.fakemon.core.model.Item;
import fr.epitech.fakemon.core.model.Talent;
import fr.epitech.fakemon.data.JsonSpeciesRepository;
import fr.epitech.fakemon.data.SpeciesFactory;
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
        var scenes = FxTestSupport.run(() -> new SceneManager(new Scene(new StackPane(), 960, 540)));
        var setup = BattleSetup.random(new JsonSpeciesRepository(new SpeciesFactory(), userFolder), new Random(29));
        var screen = FxTestSupport.run(() -> {
            var battle = new BattleScreen(scenes, setup, () -> {});
            scenes.show(battle);
            return battle;
        });
        FxTestSupport.awaitBattle(screen);
        FxTestSupport.run(() -> {
            try {
                screen.applyCss();
                var render = BattleScreen.class.getDeclaredMethod("render", BattleEvent.class);
                render.setAccessible(true);
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
                var idle = BattleScreen.class.getDeclaredMethod("presentationFinished");
                idle.setAccessible(true);
                idle.invoke(screen);
                assertFalse(screen.lookup("#player-activation").isVisible());
                assertFalse(screen.lookup("#opponent-activation").isVisible());
            } finally {
                scenes.show(new StackPane());
            }
            return null;
        });
    }
}
