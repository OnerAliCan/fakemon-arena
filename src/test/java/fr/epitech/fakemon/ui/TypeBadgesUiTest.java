package fr.epitech.fakemon.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import fr.epitech.fakemon.core.model.Type;
import javafx.scene.shape.SVGPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Charge réellement les dix ressources pour détecter les fichiers absents ou invalides. */
@EnabledIfSystemProperty(named = "fakemon.ui.tests", matches = "true")
class TypeBadgesUiTest {
    @Test
    void everyTypeHasALoadableIconAndFrenchLabel() throws Exception {
        FxTestSupport.run(() -> {
            for (Type type : Type.values()) {
                var badge = TypeBadges.badge(type);
                assertEquals(TypeBadges.name(type), badge.getText());
                assertEquals("Type " + badge.getText(), badge.getAccessibleText());
                var icon = (SVGPath) badge.getGraphic();
                assertFalse(icon.getBoundsInLocal().isEmpty());
            }
            return null;
        });
    }
}
