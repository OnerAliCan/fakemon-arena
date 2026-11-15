package fr.epitech.fakemon.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Optional;
import fr.epitech.fakemon.core.api.SpeciesRepository;
import fr.epitech.fakemon.core.api.BattleEndedEvent;
import fr.epitech.fakemon.core.api.BattleEndReason;
import fr.epitech.fakemon.core.engine.TeamDrafter;
import fr.epitech.fakemon.core.model.Team;

import fr.epitech.fakemon.core.api.BattleEvent;
import fr.epitech.fakemon.core.api.FakemonEnteredEvent;
import fr.epitech.fakemon.core.api.FakemonFaintedEvent;
import fr.epitech.fakemon.core.api.FakemonSelectionRequiredEvent;
import fr.epitech.fakemon.core.api.HpLostEvent;
import fr.epitech.fakemon.core.api.MoveUsedEvent;
import fr.epitech.fakemon.core.api.SelectionReason;
import fr.epitech.fakemon.core.api.SendFakemon;
import fr.epitech.fakemon.core.api.Side;
import fr.epitech.fakemon.core.api.TurnStartedEvent;
import fr.epitech.fakemon.core.model.BaseStats;
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Move;
import fr.epitech.fakemon.core.model.MoveCategory;
import fr.epitech.fakemon.core.model.Species;
import fr.epitech.fakemon.core.model.Type;

/** Scénario déterministe pour tester l'interface sans moteur de combat. */
final class BattleDemo {
    final List<Move> moves = List.of(
            new Move("Vague", Type.WATER, MoveCategory.SPECIAL, 60, 100, 0),
            new Move("Ombre vive", Type.SHADOW, MoveCategory.PHYSICAL, 40, 100, 1),
            new Move("Frappe", Type.FORCE, MoveCategory.PHYSICAL, 70, 95, 0),
            new Move("Éclat", Type.ETHER, MoveCategory.SPECIAL, 80, 90, 0));
    final int[] maxPp = {4, 4, 4, 4};
    final int[] pp = {4, 4, 4, 0};
    final Fakemon player;
    final Fakemon opponent;
    final Team playerTeam;
    final Team opponentTeam;
    private boolean ended;

    BattleDemo() {
        player = createFakemon("Koaninja", Type.WATER, 100);
        opponent = createFakemon("Forbiddron", Type.SHADOW, 150);
        playerTeam = new Team(List.of(player));
        opponentTeam = new Team(List.of(opponent));
    }

    /** Le catalogue de démo reste distinct du futur catalogue JSON de #22. */
    BattleDemo(Random random) {
        List<Species> catalog = new ArrayList<>();
        catalog.add(species("Koaninja", "Koanobi", List.of(Type.NATURE, Type.FORCE)));
        catalog.add(species("Forbiddron", "Mormite", List.of(Type.VENOM, Type.SHADOW)));
        catalog.add(species("Anu", "Nubis", List.of(Type.SHADOW)));
        catalog.add(species("Lumaxi", "Lumax", List.of(Type.LIGHTNING, Type.ETHER)));
        catalog.add(species("Crator", "Héphaïstodon", List.of(Type.FIRE, Type.ROCK)));
        catalog.add(species("Tekagon", "Séismodonte", List.of(Type.ROCK, Type.FORCE)));
        catalog.add(species("Trantima", "Tarentemps", List.of(Type.VENOM, Type.ROCK)));
        catalog.add(species("Webruiser", "Stoillone", List.of(Type.VENOM, Type.FORCE)));
        catalog.add(species("Lizquent", "Zarlou", List.of(Type.SHADOW, Type.FORCE)));
        catalog.add(species("Nemesusa", "Némésuse", List.of(Type.SHADOW, Type.WATER)));
        catalog.add(species("Firomenis", "Pyrale", List.of(Type.FIRE, Type.VENOM)));
        catalog.add(species("Dynabit", "Dynacell", List.of(Type.LIGHTNING)));
        SpeciesRepository repository = new SpeciesRepository() {
            public List<Species> findAll() { return new ArrayList<>(catalog); }
            public Optional<Species> findByOriginalName(String name) {
                return catalog.stream().filter(s -> s.getOriginalName().equals(name)).findFirst();
            }
            public void add(Species species) { catalog.add(species); }
        };
        TeamDrafter drafter = new TeamDrafter(repository, random);
        playerTeam = drafter.randomTeam(List.of());
        List<Species> excluded = new ArrayList<>();
        for (Fakemon fakemon : playerTeam.getFakemons()) {
            excluded.add(fakemon.getSpecies());
        }
        opponentTeam = drafter.randomTeam(excluded);
        player = playerTeam.getFakemons().get(0);
        opponent = opponentTeam.getFakemons().get(0);
    }

    private Species species(String original, String french, List<Type> types) {
        return new Species(original, french, types, new BaseStats(150, 50, 50, 50, 50, 50), moves);
    }

    int getTurn() { return turn; }

    /** L'abandon permet aussi de vérifier le parcours défaite sans moteur réel. */
    List<BattleEvent> forfeit() {
        if (ended) { return List.of(); }
        ended = true;
        return List.of(new BattleEndedEvent(Side.OPPONENT, BattleEndReason.FORFEIT));
    }
    final List<Fakemon> playerTeam = List.of(
            createFakemon("Koaninja", "Koanobi", 100, Type.NATURE, Type.FORCE),
            createFakemon("Anu", "Nubis", 85, Type.SHADOW),
            createFakemon("Lumaxi", "Lumax", 90, Type.LIGHTNING, Type.ETHER),
            createFakemon("Crator", "Héphaïstodon", 110, Type.FIRE, Type.ROCK),
            createFakemon("Tekagon", "Séismodonte", 120, Type.ROCK, Type.FORCE),
            createFakemon("Nymfusha", "Nymphaline", 80, Type.ETHER, Type.WIND));
    final Fakemon opponent = createFakemon("Forbiddron", "Mormite", 150, Type.VENOM, Type.SHADOW);
    private final int[][] pp = new int[playerTeam.size()][moves.size()];
    private int activeSlot;
    private int turn;

    BattleDemo() {
        for (int[] memberPp : pp) {
            for (int index = 0; index < memberPp.length - 1; index++) {
                memberPp[index] = 4;
            }
        }
    }

    private Fakemon createFakemon(String originalName, String frenchName, int hp, Type... types) {
        Species species = new Species(originalName, frenchName, List.of(types),
                new BaseStats(hp, 50, 50, 50, 50, 50), moves);
        return new Fakemon(species, moves);
    }

    Fakemon activePlayer() {
        return playerTeam.get(activeSlot);
    }

    int remainingPp(int index) {
        return pp[activeSlot][index];
    }

    boolean canSelect(Fakemon candidate) {
        return playerTeam.contains(candidate) && candidate != activePlayer() && !candidate.isFainted();
    }

    boolean canAct() {
        return !activePlayer().isFainted() && !opponent.isFainted();
    }

    /** Joue un tour factice : le joueur attaque, puis l'adversaire réplique s'il est encore debout. */
    List<BattleEvent> play(int index) {
        if (ended || pp[index] == 0) {
        if (!canAct() || remainingPp(index) == 0) {
            return List.of();
        }
        pp[activeSlot][index]--;
        List<BattleEvent> events = new ArrayList<>();
        events.add(new TurnStartedEvent(++turn));
        events.add(new MoveUsedEvent(Side.PLAYER, player, moves.get(index)));
        int lostHp = Math.min(30, opponent.getCurrentHp());
        opponent.takeDamage(lostHp);
        events.add(new HpLostEvent(Side.OPPONENT, opponent, lostHp, opponent.getCurrentHp()));
        // Fin synthétique du scénario : le vrai moteur décidera de la victoire dans #26.
        if (opponent.isFainted()) {
            ended = true;
            events.add(new BattleEndedEvent(Side.PLAYER, BattleEndReason.ALL_FAINTED));
        events.add(new MoveUsedEvent(Side.PLAYER, activePlayer(), moves.get(index)));
        loseHp(events, Side.OPPONENT, opponent, 30);
        if (!opponent.isFainted()) {
            retaliate(events);
        }
        return events;
    }

    /** Un changement volontaire consomme un tour ; un remplacement après K.O. n'en consomme pas. */
    List<BattleEvent> send(SendFakemon action) {
        Fakemon candidate = action.getFakemon();
        if (opponent.isFainted() || !canSelect(candidate)) {
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

    private void retaliate(List<BattleEvent> events) {
        events.add(new MoveUsedEvent(Side.OPPONENT, opponent, moves.get(1)));
        loseHp(events, Side.PLAYER, activePlayer(), 25);
        if (activePlayer().isFainted() && playerTeam.stream().anyMatch(this::canSelect)) {
            events.add(new FakemonSelectionRequiredEvent(SelectionReason.REPLACEMENT));
        }
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
