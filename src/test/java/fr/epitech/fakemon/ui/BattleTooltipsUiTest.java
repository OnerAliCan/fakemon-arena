package fr.epitech.fakemon.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.epitech.fakemon.core.api.FakemonEnteredEvent;
import fr.epitech.fakemon.core.api.HpLostEvent;
import fr.epitech.fakemon.core.api.ItemActivatedEvent;
import fr.epitech.fakemon.core.api.Side;
import fr.epitech.fakemon.core.api.TalentActivatedEvent;
import fr.epitech.fakemon.core.model.Item;
import fr.epitech.fakemon.core.model.Talent;
import fr.epitech.fakemon.data.JsonSpeciesRepository;
import fr.epitech.fakemon.data.SpeciesFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

/** Exerce les vraies infobulles JavaFX pendant les changements visibles du combat. */
@EnabledIfSystemProperty(named = "fakemon.ui.tests", matches = "true")
class BattleTooltipsUiTest {
    @TempDir Path userFolder;

    @Test
    void tooltipsFollowDisplayedEventsTargetsAndScreenLifetime() throws Exception {
        var scene = FxTestSupport.run(() -> new Scene(new StackPane(), 960, 540));
        var scenes = FxTestSupport.run(() -> new SceneManager(scene));
        var setup = BattleSetup.random(new JsonSpeciesRepository(new SpeciesFactory(), userFolder), new Random(50));
        var stage = FxTestSupport.run(() -> {
            var window = new Stage();
            window.setScene(scene);
            window.show();
            return window;
        });
        var screen = FxTestSupport.run(() -> {
            var battle = new BattleScreen(scenes, setup, () -> {});
            scenes.show(battle);
            return battle;
        });
        var next = BattleScreen.class.getDeclaredMethod("renderNext");
        next.setAccessible(true);
        try {
            FxTestSupport.awaitBattle(screen);
            var panel = FxTestSupport.run(() -> {
                var field = BattleScreen.class.getDeclaredField("opponent");
                field.setAccessible(true);
                return (Parent) field.get(screen);
            });
            var tooltip = FxTestSupport.run(() -> {
                var field = panel.getClass().getDeclaredField("tooltip");
                field.setAccessible(true);
                return (Tooltip) field.get(panel);
            });
            var oldOpponent = setup.opponent.getFakemons().getFirst();
            FxTestSupport.run(() -> {
                String before = tooltip.getText();
                screen.onEvent(new HpLostEvent(Side.OPPONENT, oldOpponent, oldOpponent.getMaxHp(), 0));
                assertEquals(before, tooltip.getText(), "Les événements en attente ne doivent pas être révélés.");
                next.invoke(screen);
                assertTrue(tooltip.getText().contains("PV : 0 %"));
                return null;
            });
            // Attendre la fin de la présentation plutôt que forcer la file à avancer.
            awaitPresentation(screen);
            var replacement = setup.opponent.getFakemons().get(1);
            FxTestSupport.run(() -> {
                String before = tooltip.getText();
                screen.onEvent(new FakemonEnteredEvent(Side.OPPONENT, replacement));
                assertEquals(before, tooltip.getText());
                next.invoke(screen);
                assertTrue(tooltip.getText().startsWith(oldOpponent.getSpecies().getFrenchName()),
                        "Le remplaçant reste caché pendant la sortie.");
                return null;
            });
            awaitPresentation(screen);
            FxTestSupport.run(() -> {
                Button attack = nodes(screen).stream().filter(Button.class::isInstance).map(Button.class::cast)
                        .filter(button -> button.getUserData() != null).findFirst().orElseThrow();
                assertTrue(tooltip.getText().startsWith(replacement.getSpecies().getFrenchName()));
                assertTrue(attack.getTooltip().getText().contains(replacement.getSpecies().getFrenchName()));
                assertTrue(tooltip.getText().contains("Objet : inconnu"));
                assertTrue(nodes(panel).stream().anyMatch(node -> node instanceof Label label
                        && "Objet : inconnu".equals(label.getText())));
                screen.onEvent(new ItemActivatedEvent(Side.OPPONENT, replacement, Item.PLUME));
                screen.onEvent(new TalentActivatedEvent(Side.OPPONENT, replacement, Talent.CARAPACE));
                next.invoke(screen);
                return null;
            });
            awaitPresentation(screen);
            FxTestSupport.run(() -> {
                assertTrue(tooltip.getText().contains("Objet : Plume"));
                assertTrue(tooltip.getText().contains("Talent : Carapace"));
                tooltip.show(panel, stage.getX() + 20, stage.getY() + 20);
                assertTrue(tooltip.isShowing());
                scenes.show(new StackPane());
                assertFalse(tooltip.isShowing(), "Une infobulle ne doit pas survivre à son écran.");
                return null;
            });
        } finally {
            FxTestSupport.run(() -> {
                scenes.show(new StackPane());
                stage.close();
                return null;
            });
        }
    }

    private static void awaitPresentation(BattleScreen screen) throws Exception {
        FxTestSupport.await(() -> {
            var field = BattleScreen.class.getDeclaredField("presentation");
            field.setAccessible(true);
            return field.get(screen) == null;
        });
    }

    private static List<Node> nodes(Node root) {
        var result = new ArrayList<Node>();
        result.add(root);
        if (root instanceof Parent parent) {
            parent.getChildrenUnmodifiable().forEach(child -> result.addAll(nodes(child)));
        }
        return result;
    }
}
