package fr.epitech.fakemon.ui;

import fr.epitech.fakemon.core.api.BattleEndReason;
import fr.epitech.fakemon.core.api.BattleEndedEvent;
import fr.epitech.fakemon.core.api.BattleEvent;
import fr.epitech.fakemon.core.api.Forfeit;
import fr.epitech.fakemon.core.api.FakemonEnteredEvent;
import fr.epitech.fakemon.core.api.FakemonFaintedEvent;
import fr.epitech.fakemon.core.api.FakemonSelectionRequiredEvent;
import fr.epitech.fakemon.core.api.HpLostEvent;
import fr.epitech.fakemon.core.api.MoveUsedEvent;
import fr.epitech.fakemon.core.api.SelectionReason;
import fr.epitech.fakemon.core.api.SendFakemon;
import fr.epitech.fakemon.core.api.Side;
import fr.epitech.fakemon.core.api.TurnStartedEvent;
import fr.epitech.fakemon.core.engine.TeamDrafter;
import fr.epitech.fakemon.core.model.BaseStats;
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Move;
import fr.epitech.fakemon.core.model.MoveCategory;
import fr.epitech.fakemon.core.model.Species;
import fr.epitech.fakemon.core.model.Talent;
import fr.epitech.fakemon.core.model.Type;
import fr.epitech.fakemon.data.InMemorySpeciesRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Scénario de démonstration de l'écran de combat. */
final class BattleDemo {
    private static final int MAX_PP = 4;

    final List<Move> moves = List.of(
            new Move("Vague", Type.WATER, MoveCategory.SPECIAL, 60, 100, 0),
            new Move("Ombre vive", Type.SHADOW, MoveCategory.PHYSICAL, 40, 100, 1),
            new Move("Frappe", Type.FORCE, MoveCategory.PHYSICAL, 70, 95, 0),
            new Move("Éclat", Type.ETHER, MoveCategory.SPECIAL, 80, 90, 0));
    final List<Fakemon> playerTeam;
    final List<Fakemon> opponentTeam;
    final Fakemon opponent;

    private final int[][] pp;
    private int activeSlot;
    private int turn;
    private boolean ended;

    BattleDemo() {
        playerTeam = List.of(
                createFakemon("Koaninja", "Koanobi", 100, Type.NATURE, Type.FORCE),
                createFakemon("Anu", "Nubis", 85, Type.SHADOW),
                createFakemon("Lumaxi", "Lumax", 90, Type.LIGHTNING, Type.ETHER),
                createFakemon("Crator", "Héphaïstodon", 110, Type.FIRE, Type.ROCK),
                createFakemon("Tekagon", "Séismodonte", 120, Type.ROCK, Type.FORCE),
                createFakemon("Nymfusha", "Nymphaline", 80, Type.ETHER, Type.WIND));
        opponent = createFakemon("Forbiddron", "Mormite", 150, Type.VENOM, Type.SHADOW);
        opponentTeam = List.of(opponent);
        pp = createPp();
    }

    BattleDemo(Random random) {
        TeamDrafter drafter = new TeamDrafter(new InMemorySpeciesRepository(), random);
        List<Fakemon> drawnPlayers = drafter.randomTeam(List.of()).getFakemons();
        List<Species> excluded = drawnPlayers.stream().map(Fakemon::getSpecies).toList();
        List<Fakemon> drawnOpponents = drafter.randomTeam(excluded).getFakemons();

        playerTeam = drawnPlayers.stream().map(this::toDemoFakemon).toList();
        opponentTeam = drawnOpponents.stream().map(this::toDemoFakemon).toList();
        opponent = opponentTeam.get(0);
        pp = createPp();
    }

    private int[][] createPp() {
        int[][] memberPp = new int[playerTeam.size()][moves.size()];
        for (int[] row : memberPp) {
            for (int index = 0; index < row.length - 1; index++) {
                row[index] = MAX_PP;
            }
        }
        return memberPp;
    }

    private Fakemon toDemoFakemon(Fakemon drawn) {
        Species species = drawn.getSpecies();
        return createFakemon(
                species.getOriginalName(),
                species.getFrenchName(),
                150,
                species.getTypes().toArray(Type[]::new));
    }

    private Fakemon createFakemon(String originalName, String frenchName, int hp, Type... types) {
        String spriteName = originalName.toLowerCase(Locale.ROOT) + ".png";
        Species species = new Species.Builder()
            .originalName(originalName)
            .frenchName(frenchName)
            .types(List.of(types))
            .stats(new BaseStats(hp, 50, 50, 50, 50, 50))
            .possibleMoves(moves)
            .talent(Talent.CARAPACE)
            .frontSprite("/sprites/front/" + spriteName)
            .backSprite("/sprites/back/" + spriteName)
            .build();
        return new Fakemon(species, moves);
    }

    Fakemon activePlayer() {
        return playerTeam.get(activeSlot);
    }

    int remainingPp(int index) {
        return pp[activeSlot][index];
    }

    int getTurn() {
        return turn;
    }

    boolean canSelect(Fakemon candidate) {
        return playerTeam.contains(candidate) && candidate != activePlayer() && !candidate.isFainted();
    }

    boolean canAct() {
        return !ended && !activePlayer().isFainted() && !opponent.isFainted();
    }

    List<BattleEvent> play(int index) {
        if (!canAct() || remainingPp(index) == 0) {
            return List.of();
        }

        pp[activeSlot][index]--;
        List<BattleEvent> events = new ArrayList<>();
        events.add(new TurnStartedEvent(++turn));
        events.add(new MoveUsedEvent(Side.PLAYER, activePlayer(), moves.get(index)));
        loseHp(events, Side.OPPONENT, opponent, 30);

        if (opponent.isFainted()) {
            endBattle(events, Side.PLAYER);
        } else {
            retaliate(events);
        }
        return events;
    }

    List<BattleEvent> send(SendFakemon action) {
        Fakemon candidate = action.getFakemon();
        if (ended || opponent.isFainted() || !canSelect(candidate)) {
            return List.of();
        }

        boolean replacement = activePlayer().isFainted();
        activeSlot = playerTeam.indexOf(candidate);
        List<BattleEvent> events = new ArrayList<>();
        if (!replacement) {
            events.add(new TurnStartedEvent(++turn));
        }
        events.add(new FakemonEnteredEvent(Side.PLAYER, candidate));
        if (!replacement) {
            retaliate(events);
        }
        return events;
    }

    List<BattleEvent> submitAction(Forfeit action) {
        if (!canAct()) {
            return List.of();
        }
        ended = true;
        return List.of(new BattleEndedEvent(Side.OPPONENT, BattleEndReason.FORFEIT));
    }

    private void retaliate(List<BattleEvent> events) {
        events.add(new MoveUsedEvent(Side.OPPONENT, opponent, moves.get(1)));
        loseHp(events, Side.PLAYER, activePlayer(), 25);
        if (activePlayer().isFainted()) {
            if (playerTeam.stream().anyMatch(this::canSelect)) {
                events.add(new FakemonSelectionRequiredEvent(SelectionReason.REPLACEMENT));
            } else {
                endBattle(events, Side.OPPONENT);
            }
        }
    }

    private void endBattle(List<BattleEvent> events, Side winner) {
        ended = true;
        events.add(new BattleEndedEvent(winner, BattleEndReason.ALL_FAINTED));
    }

    private void loseHp(List<BattleEvent> events, Side side, Fakemon fakemon, int damage) {
        int lostHp = Math.min(damage, fakemon.getCurrentHp());
        fakemon.takeDamage(lostHp);
        events.add(new HpLostEvent(side, fakemon, lostHp, fakemon.getCurrentHp()));
        if (fakemon.isFainted()) {
            events.add(new FakemonFaintedEvent(side, fakemon));

        }
    }
}
