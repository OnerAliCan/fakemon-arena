package fr.epitech.fakemon.ui;

import java.util.ArrayList;
import java.util.List;

import fr.epitech.fakemon.core.api.BattleEvent;
import fr.epitech.fakemon.core.api.HpLostEvent;
import fr.epitech.fakemon.core.api.MoveUsedEvent;
import fr.epitech.fakemon.core.api.Side;
import fr.epitech.fakemon.core.api.TurnStartedEvent;
import fr.epitech.fakemon.core.model.BaseStats;
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Move;
import fr.epitech.fakemon.core.model.MoveCategory;
import fr.epitech.fakemon.core.model.Species;
import fr.epitech.fakemon.core.model.Type;

/** Scénario déterministe pour vérifier l'écran ; ne représente pas les règles du moteur. */
final class BattleDemo {
    final List<Move> moves = List.of(
            new Move("Vague", Type.WATER, MoveCategory.SPECIAL, 60, 100, 0),
            new Move("Ombre vive", Type.SHADOW, MoveCategory.PHYSICAL, 40, 100, 1),
            new Move("Frappe", Type.FORCE, MoveCategory.PHYSICAL, 70, 95, 0),
            new Move("Éclat", Type.ETHER, MoveCategory.SPECIAL, 80, 90, 0));
    final int[] maxPp = {4, 4, 4, 4};
    final int[] pp = {4, 4, 4, 0};
    final Fakemon player = createFakemon("Koaninja", Type.WATER, 100);
    final Fakemon opponent = createFakemon("Forbiddron", Type.SHADOW, 150);
    private int turn;

    private Fakemon createFakemon(String name, Type type, int hp) {
        Species species = new Species(name, name, List.of(type),
                new BaseStats(hp, 50, 50, 50, 50, 50), moves);
        return new Fakemon(species, moves);
    }

    /** Produit une liste d'événements factices ; les dégâts fixes servent uniquement à la démo. */
    List<BattleEvent> play(int index) {
        if (pp[index] == 0) {
            return List.of();
        }
        pp[index]--;
        List<BattleEvent> events = new ArrayList<>();
        events.add(new TurnStartedEvent(++turn));
        events.add(new MoveUsedEvent(Side.PLAYER, player, moves.get(index)));
        int lostHp = Math.min(30, opponent.getCurrentHp());
        opponent.takeDamage(lostHp);
        events.add(new HpLostEvent(Side.OPPONENT, opponent, lostHp, opponent.getCurrentHp()));
        return events;
    }
}
