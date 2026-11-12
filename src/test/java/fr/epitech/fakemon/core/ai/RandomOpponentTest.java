package fr.epitech.fakemon.core.ai;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.epitech.fakemon.core.api.PlayerAction;
import fr.epitech.fakemon.core.api.SendFakemon;
import fr.epitech.fakemon.core.api.UseMove;
import fr.epitech.fakemon.core.model.BaseStats;
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Move;
import fr.epitech.fakemon.core.model.MoveCategory;
import fr.epitech.fakemon.core.model.Species;
import fr.epitech.fakemon.core.model.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Vérifie sur 1 000 tirages à graine fixe que l'IA aléatoire ne renvoie jamais d'action impossible. */
public class RandomOpponentTest {
    private static final long SEED = 50;
    private static final int DRAWS = 1000;

    @Test
    void chooseActionAlwaysUsesOneOfTheGivenMoves() {
        RandomOpponent randomOpponent = new RandomOpponent(new Random(SEED));
        List<Move> moveList = createMoveList();
        List<Fakemon> standingFakemons = new ArrayList<>();

        for (int i = 0; i < DRAWS; i++) {
            PlayerAction chosenAction = randomOpponent.chooseAction(moveList, standingFakemons);
            UseMove useMove = assertInstanceOf(UseMove.class, chosenAction);
            assertTrue(moveList.contains(useMove.getMove()));
        }
    }

    @Test
    void chooseFakemonAlwaysSendsAStandingFakemon() {
        RandomOpponent randomOpponent = new RandomOpponent(new Random(SEED));
        List<Fakemon> standingFakemons = new ArrayList<>();
        standingFakemons.add(createFakemon("Nubis", Type.SHADOW));
        standingFakemons.add(createFakemon("Zarlou", Type.FORCE));
        standingFakemons.add(createFakemon("Stoillone", Type.VENOM));

        for (int i = 0; i < DRAWS; i++) {
            PlayerAction chosenAction = randomOpponent.chooseFakemon(standingFakemons);
            SendFakemon sendFakemon = assertInstanceOf(SendFakemon.class, chosenAction);
            assertTrue(standingFakemons.contains(sendFakemon.getFakemon()));
        }
    }

    private List<Move> createMoveList() {
        List<Move> moveList = new ArrayList<>();
        moveList.add(new Move("Tison", Type.FIRE, MoveCategory.PHYSICAL, 40, 100, 1));
        moveList.add(new Move("Ressac", Type.WATER, MoveCategory.PHYSICAL, 65, 100, 0));
        moveList.add(new Move("Arc électrique", Type.LIGHTNING, MoveCategory.SPECIAL, 65, 100, 0));
        moveList.add(new Move("Éclipse", Type.SHADOW, MoveCategory.SPECIAL, 95, 80, 0));
        return moveList;
    }

    private Fakemon createFakemon(String name, Type type) {
        BaseStats stats = new BaseStats(100, 100, 100, 100, 100, 100);
        Species species = new Species(name, name, List.of(type), stats, List.of());
        return new Fakemon(species, createMoveList());
    }
}
