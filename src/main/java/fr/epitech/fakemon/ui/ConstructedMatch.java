package fr.epitech.fakemon.ui;

import fr.epitech.fakemon.core.api.SpeciesRepository;
import fr.epitech.fakemon.core.engine.TeamDrafter;
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Move;
import fr.epitech.fakemon.core.model.Species;
import java.util.List;
import java.util.Random;

/** Tirage et état provisoire d'un combat construit, avant le branchement du moteur. */
final class ConstructedMatch {
    record Loadout(Species species, List<Move> moves, String item) {
        Fakemon freshFakemon() { return new Fakemon(species, moves); }
    }

    private static final List<String> ITEMS = List.of("Gantelet", "Écu", "Plume");
    private final SpeciesRepository roster;
    private final Random random;
    final List<Loadout> playerLoadouts;
    final List<Loadout> opponentLoadouts;
    final List<Fakemon> playerTeam;
    final List<Fakemon> opponentTeam;
    Fakemon active;
    int turn;

    static ConstructedMatch start(SpeciesRepository roster, List<Species> selected, Random random) {
        if (selected.size() != 6 || selected.stream().map(Species::getOriginalName).distinct().count() != 6
                || !roster.findAll().containsAll(selected)) {
            throw new IllegalArgumentException("Choisis six fakemon différents du catalogue.");
        }
        var team = new TeamDrafter(roster, random).buildTeam(selected);
        return new ConstructedMatch(roster, random, drawItems(team.getFakemons(), random));
    }

    private ConstructedMatch(SpeciesRepository roster, Random random, List<Loadout> playerLoadouts) {
        this.roster = roster;
        this.random = random;
        this.playerLoadouts = playerLoadouts;
        var excluded = playerLoadouts.stream().map(Loadout::species).toList();
        opponentLoadouts = drawItems(new TeamDrafter(roster, random).randomTeam(excluded).getFakemons(), random);
        playerTeam = playerLoadouts.stream().map(Loadout::freshFakemon).toList();
        opponentTeam = opponentLoadouts.stream().map(Loadout::freshFakemon).toList();
    }

    private static List<Loadout> drawItems(List<Fakemon> team, Random random) {
        return team.stream().map(mon -> new Loadout(mon.getSpecies(), List.copyOf(mon.getMoves()),
                ITEMS.get(random.nextInt(ITEMS.size())))).toList();
    }

    /** Rejoue avec les mêmes espèces, attaques et objets côté joueur, avec des PV neufs. */
    ConstructedMatch rematch() { return new ConstructedMatch(roster, random, playerLoadouts); }

    Fakemon opponent() {
        return opponentTeam.stream().filter(mon -> !mon.isFainted()).findFirst().orElse(null);
    }

    boolean choose(Fakemon member) {
        if (!playerTeam.contains(member) || member.isFainted() || member == active) {
            return false;
        }
        boolean voluntary = active != null && !active.isFainted();
        active = member;
        if (voluntary && opponent() != null) {
            turn++;
            active.takeDamage(25);
        }
        return true;
    }

    String attack(Move move) {
        Fakemon target = opponent();
        if (active == null || active.isFainted() || target == null || !active.getMoves().contains(move)) {
            return "";
        }
        turn++;
        int loss = Math.min(30, target.getCurrentHp());
        target.takeDamage(loss);
        String message = active.getSpecies().getFrenchName() + " utilise " + move.getName()
                + " : " + target.getSpecies().getFrenchName() + " perd " + loss + " PV.";
        if (!target.isFainted()) {
            int reply = Math.min(25, active.getCurrentHp());
            active.takeDamage(reply);
            message += "\n" + target.getSpecies().getFrenchName() + " riposte : " + reply + " PV perdus.";
        }
        return message;
    }
}
