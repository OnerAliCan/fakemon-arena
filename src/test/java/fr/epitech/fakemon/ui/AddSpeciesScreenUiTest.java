package fr.epitech.fakemon.ui;

import static org.junit.jupiter.api.Assertions.*;
import fr.epitech.fakemon.core.model.Talent;
import fr.epitech.fakemon.data.JsonSpeciesRepository;
import fr.epitech.fakemon.data.SpeciesFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

/** Vérifie la saisie réelle, le filtrage, les aperçus et les erreurs du dépôt. */
@EnabledIfSystemProperty(named = "fakemon.ui.tests", matches = "true")
class AddSpeciesScreenUiTest {
    @TempDir Path folder;

    @Test
    void validatesAndSavesWhileFilteringKeepsCheckedMoves() throws Exception {
        var front = folder.resolve("face.png");
        try (var stream = getClass().getResourceAsStream("/sprites/front/trantima.png")) {
            Files.copy(stream, front);
        }
        FxTestSupport.run(() -> {
            var repository = new JsonSpeciesRepository(new SpeciesFactory(), folder.resolve("user"));
            var screen = new AddSpeciesScreen(repository, new SimpleDoubleProperty(50), () -> {});
            var scene = new Scene(screen, 960, 540);
            screen.applyCss(); screen.layout();
            var save = (Button) screen.lookup("#save-species");
            var message = (Label) screen.lookup("#form-message");
            save.fire();
            assertTrue(message.getText().contains("nom"));
            ((TextField) screen.lookup("#original-name")).setText("AjoutUI");
            ((TextField) screen.lookup("#french-name")).setText("Créature test");
            screen.selectSprite(front, false);
            var preview = (ImageView) screen.lookup("#back-preview");
            assertNotNull(preview.getImage());
            assertEquals(-1, preview.getScaleX());
            ((CheckBox) screen.lookup("#type-FIRE")).fire();
            ((CheckBox) screen.lookup("#type-WATER")).fire();
            assertTrue(screen.lookup("#type-ROCK").isDisabled());
            ((CheckBox) screen.lookup("#type-WATER")).fire();
            @SuppressWarnings("unchecked") var talent = (ComboBox<Talent>) screen.lookup("#talent");
            talent.setValue(Talent.CARAPACE);
            for (int index = 0; index < 4; index++) {
                ((CheckBox) screen.lookup("#move-" + index)).fire();
            }
            ((TextField) screen.lookup("#moves-filter")).setText("introuvable");
            assertFalse(screen.lookup("#move-0").isVisible());
            assertTrue(((CheckBox) screen.lookup("#move-0")).isSelected());
            var hp = (Slider) screen.lookup("#stat-hp");
            assertEquals(50, hp.getMin()); assertEquals(150, hp.getMax());
            hp.setValue(150);
            assertEquals("Total : 650 / 600", ((Label) screen.lookup("#stat-total")).getText());
            save.fire();
            assertTrue(message.getText().contains("600"));
            assertEquals(30, repository.findAll().size());
            assertEquals("AjoutUI", ((TextField) screen.lookup("#original-name")).getText());
            hp.setValue(100);
            save.fire();
            assertTrue(message.getText().contains("a rejoint"));
            assertTrue(save.isDisabled());
            var reloaded = new JsonSpeciesRepository(new SpeciesFactory(), folder.resolve("user"));
            assertEquals(31, reloaded.findAll().size());
            assertEquals(4, reloaded.findByOriginalName("AjoutUI").orElseThrow().getPossibleMoves().size());
            scene.setRoot(new StackPane());
            return null;
        });
    }

    @Test
    void unreadableCryHasAVisibleErrorAndCanBeReplaced() throws Exception {
        Path invalid = Files.writeString(folder.resolve("invalid.wav"), "invalide");
        var scene = FxTestSupport.run(() -> {
            var screen = new AddSpeciesScreen(new JsonSpeciesRepository(new SpeciesFactory(), folder.resolve("user")),
                    new SimpleDoubleProperty(0), () -> {});
            var result = new Scene(screen, 960, 540);
            screen.applyCss(); screen.layout();
            screen.selectCry(invalid);
            var listen = (Button) screen.lookup("#listen-cry");
            assertFalse(listen.isDisabled());
            listen.fire();
            return result;
        });
        FxTestSupport.await(() -> !((Label) scene.getRoot().lookup("#cry-message")).getText().isBlank());
        FxTestSupport.run(() -> {
            var screen = (AddSpeciesScreen) scene.getRoot();
            assertTrue(((Label) screen.lookup("#cry-message")).getText().contains("Impossible"));
            screen.selectCry(invalid);
            assertEquals("", ((Label) screen.lookup("#cry-message")).getText());
            scene.setRoot(new StackPane());
            return null;
        });
    }

    @Test
    void invalidSpriteIsExplainedWithoutBreakingTheForm() throws Exception {
        Path invalid = Files.writeString(folder.resolve("invalid.png"), "invalide");
        FxTestSupport.run(() -> {
            var screen = new AddSpeciesScreen(new JsonSpeciesRepository(new SpeciesFactory(), folder.resolve("user")),
                    new SimpleDoubleProperty(0), () -> {});
            new Scene(screen, 960, 540);
            screen.applyCss(); screen.layout();
            screen.selectSprite(invalid, false);
            assertTrue(((Label) screen.lookup("#form-message")).getText().contains("Impossible"));
            assertNull(((ImageView) screen.lookup("#front-preview")).getImage());
            return null;
        });
    }
}
