package fr.epitech.fakemon.ui;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import fr.epitech.fakemon.core.ai.RandomOpponent;
import fr.epitech.fakemon.core.api.BattleEngine;
import fr.epitech.fakemon.core.api.BattleMode;
import fr.epitech.fakemon.core.api.SpeciesRepository;
import fr.epitech.fakemon.core.engine.TeamDrafter;
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Species;
import fr.epitech.fakemon.core.model.Team;

/** Prépare les équipes et conserve les choix nécessaires à une revanche. */
final class BattleSetup {
    private static final List<String> ITEMS = List.of("Gantelet", "Écu", "Plume");
    private final SpeciesRepository roster;
    private final Random random;
    private final Map<Fakemon, String> items = new IdentityHashMap<>();
    final BattleMode mode;
    final Team player;
    final Team opponent;

    static BattleSetup random(SpeciesRepository roster, Random random) {
        return new BattleSetup(roster, random, BattleMode.RANDOM, null);
    }

    static BattleSetup constructed(SpeciesRepository roster, List<Species> selected, Random random) {
        if (selected.size() != 6 || selected.stream().map(Species::getOriginalName).distinct().count() != 6
                || !roster.findAll().containsAll(selected)) {
            throw new IllegalArgumentException("Choisis six fakemon différents du catalogue.");
        }
        return new BattleSetup(roster, random, BattleMode.CONSTRUCTED,
                new TeamDrafter(roster, random).buildTeam(selected));
    }

    private BattleSetup(SpeciesRepository roster, Random random, BattleMode mode, Team chosen) {
        this.roster = roster;
        this.random = random;
        this.mode = mode;
        var drafter = new TeamDrafter(roster, random);
        player = chosen == null ? shuffled(drafter.randomTeam(List.of())) : chosen;
        opponent = shuffled(drafter.randomTeam(player.getFakemons().stream().map(Fakemon::getSpecies).toList()));
        for (Team team : List.of(player, opponent)) {
            for (Fakemon mon : team.getFakemons()) {
                items.put(mon, ITEMS.get(random.nextInt(ITEMS.size())));
            }
        }
    }

    private Team shuffled(Team team) {
        var members = new java.util.ArrayList<>(team.getFakemons());
        Collections.shuffle(members, random);
        return new Team(members);
    }

    String item(Fakemon fakemon) {
        return items.get(fakemon);
    }

    void start(BattleEngine engine) {
        engine.start(player, opponent, mode, new RandomOpponent(random), random);
    }

    BattleSetup rematch() {
        if (mode == BattleMode.RANDOM) {
            return random(roster, random);
        }
        var original = player.getFakemons();
        var fresh = original.stream().map(mon -> new Fakemon(mon.getSpecies(), mon.getMoves())).toList();
        var result = new BattleSetup(roster, random, mode, new Team(fresh));
        for (int i = 0; i < original.size(); i++) {
            result.items.put(fresh.get(i), item(original.get(i)));
        }
        return result;
    }
}
