package fr.epitech.fakemon.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import fr.epitech.fakemon.core.api.SpeciesRepository;
import fr.epitech.fakemon.core.model.BaseStats;
import fr.epitech.fakemon.core.model.Move;
import fr.epitech.fakemon.core.model.MoveCategory;
import fr.epitech.fakemon.core.model.Species;
import fr.epitech.fakemon.core.model.Type;

/** Lit le catalogue livré avec le jeu, sans gérer les sauvegardes utilisateur. */
public final class BundledRoster {
    private BundledRoster() {}

    /** Charge les espèces et résout leurs références vers les attaques du catalogue. */
    public static SpeciesRepository load() {
        var stream = Objects.requireNonNull(BundledRoster.class.getResourceAsStream("/data/fakemons.json"));
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            var root = JsonParser.parseReader(reader).getAsJsonObject();
            var moves = new HashMap<String, Move>();
            for (var entry : root.getAsJsonArray("moves")) {
                var m = entry.getAsJsonObject();
                moves.put(text(m, "id"), new Move(text(m, "name"), Type.valueOf(text(m, "type")),
                        MoveCategory.valueOf(text(m, "category")), number(m, "power"),
                        number(m, "accuracy"), number(m, "priority")));
            }
            var species = new ArrayList<Species>();
            for (var entry : root.getAsJsonArray("species")) {
                var s = entry.getAsJsonObject();
                var stats = s.getAsJsonObject("stats");
                var types = s.getAsJsonArray("types").asList().stream()
                        .map(type -> Type.valueOf(type.getAsString())).toList();
                var attacks = s.getAsJsonArray("possibleMoves").asList().stream()
                        .map(id -> Objects.requireNonNull(moves.get(id.getAsString()), id.getAsString())).toList();
                species.add(new Species(text(s, "originalName"), text(s, "frenchName"), types,
                        new BaseStats(number(stats, "hp"), number(stats, "attack"), number(stats, "defense"),
                                number(stats, "specialAttack"), number(stats, "specialDefense"),
                                number(stats, "speed")), attacks));
            }
            return new InMemorySpeciesRepository(species);
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Impossible de lire le catalogue du jeu.", error);
        }
    }

    private static String text(JsonObject object, String key) { return object.get(key).getAsString(); }
    private static int number(JsonObject object, String key) { return object.get(key).getAsInt(); }
}
