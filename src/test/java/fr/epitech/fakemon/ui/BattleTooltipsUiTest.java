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
        FxTestSupport.run(() -> {
            var roster = new JsonSpeciesRepository(new SpeciesFactory(), userFolder);
            var setup = BattleSetup.random(roster, new Random(50));
            var scene = new Scene(new StackPane(), 960, 540);
            var scenes = new SceneManager(scene);
            var stage = new Stage();
            stage.setScene(scene);
            var screen = new BattleScreen(scenes, setup, () -> {});
            scenes.show(screen);
            stage.show();
            var next = BattleScreen.class.getDeclaredMethod("renderNext");
            next.setAccessible(true);
            try {
                for (int index = 0; index < 100; index++) {
                    next.invoke(screen);
                }
                var opponentField = BattleScreen.class.getDeclaredField("opponent");
                opponentField.setAccessible(true);
                var panel = (Parent) opponentField.get(screen);
                var tooltipField = panel.getClass().getDeclaredField("tooltip");
                tooltipField.setAccessible(true);
                var tooltip = (Tooltip) tooltipField.get(panel);
                String before = tooltip.getText();
                var oldOpponent = setup.opponent.getFakemons().getFirst();
                screen.onEvent(new HpLostEvent(Side.OPPONENT, oldOpponent, oldOpponent.getMaxHp(), 0));
                assertEquals(before, tooltip.getText(), "Les événements en attente ne doivent pas être révélés.");
                next.invoke(screen);
                assertTrue(tooltip.getText().contains("PV : 0 %"));

                Button attack = nodes(screen).stream().filter(Button.class::isInstance).map(Button.class::cast)
                        .filter(button -> button.getUserData() != null).findFirst().orElseThrow();
                assertTrue(attack.getTooltip().getText().contains(oldOpponent.getSpecies().getFrenchName()));
                var replacement = setup.opponent.getFakemons().get(1);
                screen.onEvent(new FakemonEnteredEvent(Side.OPPONENT, replacement));
                assertEquals(before.substring(0, before.indexOf('\n')),
                        tooltip.getText().substring(0, tooltip.getText().indexOf('\n')));
                next.invoke(screen);
                assertTrue(tooltip.getText().startsWith(replacement.getSpecies().getFrenchName()));
                assertTrue(attack.getTooltip().getText().contains(replacement.getSpecies().getFrenchName()));
                assertTrue(tooltip.getText().contains("Objet : inconnu"));
                assertTrue(nodes(panel).stream().anyMatch(node -> node instanceof Label label
                        && "Objet : inconnu".equals(label.getText())));

                screen.onEvent(new ItemActivatedEvent(Side.OPPONENT, replacement, Item.PLUME));
                next.invoke(screen);
                screen.onEvent(new TalentActivatedEvent(Side.OPPONENT, replacement, Talent.CARAPACE));
                next.invoke(screen);
                assertTrue(tooltip.getText().contains("Objet : Plume"));
                assertTrue(tooltip.getText().contains("Talent : Carapace"));
                tooltip.show(panel, stage.getX() + 20, stage.getY() + 20);
                assertTrue(tooltip.isShowing());
                scenes.show(new StackPane());
                assertFalse(tooltip.isShowing(), "Une infobulle ne doit pas survivre à son écran.");
            } finally {
                scenes.show(new StackPane());
                stage.close();
            }
            return null;
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
