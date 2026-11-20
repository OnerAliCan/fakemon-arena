package fr.epitech.fakemon.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.epitech.fakemon.core.api.FakemonFaintedEvent;
import fr.epitech.fakemon.core.api.HpLostEvent;
import fr.epitech.fakemon.core.api.HpRestoredEvent;
import fr.epitech.fakemon.core.api.ItemActivatedEvent;
import fr.epitech.fakemon.core.api.MoveUsedEvent;
import fr.epitech.fakemon.core.api.Side;
import fr.epitech.fakemon.core.api.StatusAppliedEvent;
import fr.epitech.fakemon.core.api.StatusRemovedEvent;
import fr.epitech.fakemon.core.api.TalentActivatedEvent;
import fr.epitech.fakemon.core.model.BaseStats;
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Item;
import fr.epitech.fakemon.core.model.Move;
import fr.epitech.fakemon.core.model.MoveCategory;
import fr.epitech.fakemon.core.model.Species;
import fr.epitech.fakemon.core.model.StatusType;
import fr.epitech.fakemon.core.model.Talent;
import fr.epitech.fakemon.core.model.Type;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Vérifie les données visibles, la confidentialité adverse et les règles d'efficacité. */
class BattleTooltipsTest {
    private final BattleTooltips tooltips = new BattleTooltips();

    @Test
    void allyShowsItsKnownEquipmentMovesAndActualCombatStats() {
        Fakemon mon = mon("Allié", Type.NATURE);
        mon.equip(Item.PLUME);
        tooltips.entered(mon, 75, StatusType.PARALYSIS);
        String text = tooltips.fakemon(mon, true);
        assertTrue(text.contains("Types : Nature"));
        assertTrue(text.contains("PV : 75 / 100 (75 %)"));
        assertTrue(text.contains("Objet : Plume"));
        assertTrue(text.contains("Talent : Carapace"));
        assertTrue(text.contains("Attaque 100 / Défense 90"));
        assertTrue(text.contains("Att. spé. 80 / Déf. spé. 70"));
        assertTrue(text.contains("Vitesse : 39"));
        mon.getMoves().forEach(move -> assertTrue(text.contains(move.getName())));
    }

    @Test
    void opponentDoesNotExposeItsEquipmentTalentOrUnusedMoves() {
        Fakemon mon = mon("Adverse", Type.WATER);
        mon.equip(Item.PLUME);
        String text = tooltips.fakemon(mon, false);
        assertTrue(text.contains("Talent : inconnu"));
        assertTrue(text.contains("Objet : inconnu"));
        assertTrue(text.contains("Attaques révélées : aucune"));
        assertTrue(text.contains("Vitesse : 65 (avant modificateurs)"));
        assertFalse(text.contains("Carapace"));
        assertFalse(text.contains("Plume"));
        mon.getMoves().forEach(move -> assertFalse(text.contains(move.getName())));
    }

    @Test
    void revelationsAreRetainedAcrossSwitchesAndBelongToTheIndividualFakemon() {
        Fakemon mon = mon("Adverse", Type.WATER);
        Fakemon other = new Fakemon(mon.getSpecies(), mon.getMoves());
        Move attack = mon.getMoves().getFirst();
        tooltips.observe(new ItemActivatedEvent(Side.OPPONENT, mon, Item.ECU));
        tooltips.observe(new TalentActivatedEvent(Side.OPPONENT, mon, Talent.CARAPACE));
        tooltips.observe(new MoveUsedEvent(Side.OPPONENT, mon, attack));
        tooltips.entered(mon, 60, null);
        String text = tooltips.fakemon(mon, false);
        assertTrue(text.contains("Objet : Écu"));
        assertTrue(text.contains("Talent : Carapace"));
        assertTrue(text.contains(attack.getName()));
        assertFalse(text.contains(mon.getMoves().get(1).getName()));
        assertTrue(tooltips.fakemon(other, false).contains("Objet : inconnu"));
        assertTrue(new BattleTooltips().fakemon(mon, false).contains("Talent : inconnu"));
    }

    @Test
    void visibleHealthAndStatusDoNotJumpToTheEnginesFutureState() {
        Fakemon mon = mon("Allié", Type.NATURE);
        tooltips.entered(mon, 100, null);
        mon.takeDamage(75);
        mon.applyStatus(StatusType.PARALYSIS.create());
        String initial = tooltips.fakemon(mon, true);
        assertTrue(initial.contains("PV : 100 / 100"));
        assertTrue(initial.contains("Vitesse : 65"));
        assertFalse(initial.contains("Paralysie"));
        tooltips.observe(new HpLostEvent(Side.PLAYER, mon, 30, 70));
        tooltips.observe(new StatusAppliedEvent(Side.PLAYER, mon, StatusType.PARALYSIS));
        String shown = tooltips.fakemon(mon, true);
        assertTrue(shown.contains("PV : 70 / 100"));
        assertTrue(shown.contains("Vitesse : 32,5"));
        tooltips.observe(new HpRestoredEvent(Side.PLAYER, mon, 10, 80));
        tooltips.observe(new StatusRemovedEvent(Side.PLAYER, mon, StatusType.PARALYSIS));
        assertTrue(tooltips.fakemon(mon, true).contains("PV : 80 / 100"));
        assertTrue(tooltips.fakemon(mon, true).contains("Vitesse : 65"));
        tooltips.observe(new FakemonFaintedEvent(Side.PLAYER, mon));
        assertTrue(tooltips.fakemon(mon, true).contains("PV : 0 / 100"));
    }

    @Test
    void attackingAChangedTargetUsesItsCurrentTypesIncludingDoubleTypes() {
        Move water = new Move("Ressac", Type.WATER, MoveCategory.PHYSICAL, 65, 100, 0);
        assertTrue(tooltips.move(water, mon("Feu", Type.FIRE)).contains("Super efficace contre Feu (×2)"));
        assertTrue(tooltips.move(water, mon("Nature", Type.NATURE)).contains("Peu efficace contre Nature (×0,5)"));
        Fakemon doubleType = mon("Volcan", Type.FIRE, Type.ROCK);
        assertTrue(tooltips.move(water, doubleType).contains("Super efficace contre Volcan (×4)"));
        Move fire = new Move("Tison", Type.FIRE, MoveCategory.PHYSICAL, 40, 100, 1);
        assertTrue(tooltips.move(fire, mon("Résistant", Type.FIRE, Type.WATER)).contains("(×0,25)"));
        Move lightning = new Move("Éclair", Type.LIGHTNING, MoveCategory.SPECIAL, 65, 100, 0);
        assertTrue(tooltips.move(lightning, mon("Roche", Type.ROCK)).contains("Aucun effet contre Roche (×0)"));
        assertTrue(tooltips.move(water, null).contains("Aucune cible active"));
    }

    @Test
    void statusMovesReportStatusImmunityRatherThanDamageEffectiveness() {
        Move burn = new Move("Marque ardente", Type.FIRE, MoveCategory.STATUS, 0, 90, 0, StatusType.BURN, 100);
        String immune = tooltips.move(burn, mon("Flamme", Type.FIRE));
        assertTrue(immune.contains("Puissance : sans dégâts"));
        assertTrue(immune.contains("Flamme est immunisé à ce statut"));
        assertFalse(immune.contains("Peu efficace"));
        Fakemon water = mon("Eau", Type.WATER);
        assertTrue(tooltips.move(burn, water).contains("Eau peut recevoir ce statut"));
        tooltips.observe(new StatusAppliedEvent(Side.OPPONENT, water, StatusType.POISON));
        assertTrue(tooltips.move(burn, water).contains("possède déjà un statut"));
    }

    @Test
    void aDamagingMoveSeparatesDamageEffectivenessFromSecondaryStatusImmunity() {
        Move burn = new Move("Flamme", Type.FIRE, MoveCategory.SPECIAL, 65, 100, 0, StatusType.BURN, 20);
        String text = tooltips.move(burn, mon("Feu", Type.FIRE));
        assertTrue(text.contains("Peu efficace contre Feu (×0,5)"));
        assertTrue(text.contains("Feu est immunisé à ce statut"));
    }

    @Test
    void readingTooltipsDoesNotAlterTheCombat() {
        Fakemon mon = mon("Test", Type.WATER);
        mon.equip(Item.GANTELET);
        String before = tooltips.fakemon(mon, false);
        tooltips.move(mon.getMoves().getFirst(), mon);
        assertEquals(before, tooltips.fakemon(mon, false));
        assertEquals(100, mon.getCurrentHp());
        assertEquals(120, mon.getStats().getAttack());
    }

    private static Fakemon mon(String name, Type... types) {
        List<Move> moves = List.of(
                new Move("Attaque révélable", Type.WATER, MoveCategory.SPECIAL, 65, 100, 0),
                new Move("Attaque secrète", Type.FIRE, MoveCategory.PHYSICAL, 40, 100, 1),
                new Move("Autre secret", Type.NATURE, MoveCategory.PHYSICAL, 95, 80, 0),
                new Move("Dernier secret", Type.ETHER, MoveCategory.SPECIAL, 65, 100, 0));
        Species species = new Species.Builder().originalName(name).frenchName(name).types(List.of(types))
                .stats(new BaseStats(100, 100, 90, 80, 70, 65)).possibleMoves(moves).dexNumber(1)
                .talent(Talent.CARAPACE).frontSprite("/sprites/front/anu.png").backSprite("/sprites/back/anu.png").build();
        return new Fakemon(species, moves);
    }
}
