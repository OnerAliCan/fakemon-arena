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
    private int turn;

    private Fakemon createFakemon(String name, Type type, int hp) {
        Species species = new Species(name, name, List.of(type),
                new BaseStats(hp, 50, 50, 50, 50, 50), moves);
        return new Fakemon(species, moves);
    }

    /** Produit une liste d'événements factices ; les dégâts fixes servent uniquement à la démo. */
    List<BattleEvent> play(int index) {
        if (ended || pp[index] == 0) {
            return List.of();
        }
        pp[index]--;
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
        }
        return events;
    }
}
