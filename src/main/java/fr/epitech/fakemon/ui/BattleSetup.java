package fr.epitech.fakemon.ui;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import fr.epitech.fakemon.core.ai.RandomOpponent;
import fr.epitech.fakemon.core.api.BattleEngine;
import fr.epitech.fakemon.core.api.BattleMode;
import fr.epitech.fakemon.core.api.SpeciesRepository;
import fr.epitech.fakemon.core.engine.TeamDrafter;
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Item;
import fr.epitech.fakemon.core.model.Species;
import fr.epitech.fakemon.core.model.Team;

/** Prépare les équipes et conserve les choix nécessaires à une revanche. */
final class BattleSetup {
    private static final List<Item> ITEMS = List.of(Item.values());
    private final SpeciesRepository roster;
    private final Random random;
    final BattleMode mode;
    final Team player;
    final Team opponent;

    static BattleSetup random(SpeciesRepository roster, Random random) {
        return new BattleSetup(roster, random, BattleMode.RANDOM, null);
    }

    static BattleSetup constructed(SpeciesRepository roster, List<Species> selected, Random random) {
        validateSelection(roster, selected);
        return new BattleSetup(roster, random, BattleMode.CONSTRUCTED,
                new TeamDrafter(roster, random).buildTeam(selected));
    }

    /** Construit les deux équipes validées par l'hôte, premiers combattants en tête. */
    static BattleSetup networkConstructed(SpeciesRepository roster, List<Species> host, List<Species> guest,
            Random random) {
        validateSelection(roster, host);
        validateSelection(roster, guest);
        var drafter = new TeamDrafter(roster, random);
        return new BattleSetup(roster, random, BattleMode.CONSTRUCTED,
                drafter.buildTeam(host), drafter.buildTeam(guest));
    }

    private static void validateSelection(SpeciesRepository roster, List<Species> selected) {
        if (selected.size() != 6 || selected.stream().map(Species::getOriginalName).distinct().count() != 6
                || !roster.findAll().containsAll(selected)) {
            throw new IllegalArgumentException("Choisis six fakemon différents du catalogue.");
        }
    }

    private BattleSetup(SpeciesRepository roster, Random random, BattleMode mode, Team chosen) {
        this(roster, random, mode, chosen, null);
    }

    private BattleSetup(SpeciesRepository roster, Random random, BattleMode mode, Team chosen, Team chosenOpponent) {
        this.roster = roster;
        this.random = random;
        this.mode = mode;
        var drafter = new TeamDrafter(roster, random);
        player = chosen == null ? shuffled(drafter.randomTeam(List.of())) : chosen;
        opponent = chosenOpponent == null
                ? shuffled(drafter.randomTeam(player.getFakemons().stream().map(Fakemon::getSpecies).toList()))
                : chosenOpponent;
        for (Team team : List.of(player, opponent)) {
            for (Fakemon mon : team.getFakemons()) {
                if (mon.getItem() == null) {
                    mon.equip(ITEMS.get(random.nextInt(ITEMS.size())));
                }
            }
        }
    }

    private Team shuffled(Team team) {
        var members = new java.util.ArrayList<>(team.getFakemons());
        Collections.shuffle(members, random);
        return new Team(members);
    }

    String item(Fakemon fakemon) {
        return fakemon.getItem().getDisplayName();
    }

    void start(BattleEngine engine) {
        engine.start(player, opponent, mode, new RandomOpponent(random), random);
    }

    BattleSetup rematch() {
        if (mode == BattleMode.RANDOM) {
            return random(roster, random);
        }
        List<Fakemon> fresh = player.getFakemons().stream().map(mon -> {
            Fakemon copy = new Fakemon(mon.getSpecies(), mon.getMoves());
            copy.equip(mon.getItem());
            return copy;
        }).toList();
        return new BattleSetup(roster, random, mode, new Team(fresh));
    }
}
