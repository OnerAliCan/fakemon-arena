package fr.epitech.fakemon.data;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import fr.epitech.fakemon.core.api.SpeciesRepository;
import fr.epitech.fakemon.core.model.Move;
import fr.epitech.fakemon.core.model.Species;

/** Lit le catalogue livré avec le jeu, sans gérer les sauvegardes utilisateur. */
public final class BundledRoster {
    private BundledRoster() {}

    /** Charge les espèces et résout leurs références vers les attaques du catalogue. */
    public static SpeciesRepository load() {
        var stream = Objects.requireNonNull(BundledRoster.class.getResourceAsStream("/data/fakemons.json"));
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            var root = JsonParser.parseReader(reader).getAsJsonObject();
            var factory = new SpeciesFactory();
            var moves = new HashMap<String, Move>();
            for (var entry : root.getAsJsonArray("moves")) {
                var move = entry.getAsJsonObject();
                moves.put(move.get("id").getAsString(), factory.createMove(move));
            }

            var species = new ArrayList<Species>();
            for (var entry : root.getAsJsonArray("species")) {
                species.add(factory.createSpecies(entry.getAsJsonObject(), moves));
            }

            return new InMemorySpeciesRepository(species);
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Impossible de lire le catalogue du jeu.", error);
        }
    }
}
