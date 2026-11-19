package fr.epitech.fakemon.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.epitech.fakemon.core.api.InvalidSpeciesException;
import fr.epitech.fakemon.core.model.BaseStats;
import fr.epitech.fakemon.core.model.Move;
import fr.epitech.fakemon.core.model.MoveCategory;
import fr.epitech.fakemon.core.model.Species;
import fr.epitech.fakemon.core.model.Talent;
import fr.epitech.fakemon.core.model.StatusType;
import fr.epitech.fakemon.core.model.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Transforme le JSON du catalogue en objets du jeu : les attaques, puis les espèces.
 * C'est le patron Factory : le reste du jeu ne connaît pas le format JSON.
 */
public class SpeciesFactory {
    /** Crée une fabrique pour les fiches d'espèces et les attaques du catalogue. */
    public SpeciesFactory() {}


    /**
     * Crée une attaque à partir de son JSON.
     *
     * @param json le JSON d'une attaque
     * @return l'attaque
     * @throws InvalidSpeciesException s'il manque une information ou si une valeur est inconnue
     */
    public Move createMove(JsonObject json) {
        String id = readString(json, "id");
        try {
            JsonElement effect = json.get("status");
            String status = null;
            if (effect != null && !effect.isJsonNull()) {
                if (!effect.isJsonPrimitive() || !effect.getAsJsonPrimitive().isString()) {
                    throw new IllegalArgumentException("Le statut doit être identifié par son nom.");
                }
                status = effect.getAsString();
            }
            int statusChance = 0;
            if (json.has("statusChance")) {
                JsonElement value = json.get("statusChance");
                if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
                    throw new IllegalArgumentException("La chance de statut doit être un entier.");
                }
                statusChance = value.getAsBigDecimal().intValueExact();
            }
            return new Move(
                readString(json, "name"),
                readEnum(Type.class, json, "type", id),
                readEnum(MoveCategory.class, json, "category", id),
                readInt(json, "power", id),
                readInt(json, "accuracy", id),
                readInt(json, "priority", id),
                status == null ? null : toEnum(StatusType.class, status, "status", id),
                statusChance
            );
        } catch (IllegalArgumentException | ArithmeticException error) {
            throw new InvalidSpeciesException(id + " : " + error.getMessage());
        }
    }

    /**
     * Crée une espèce à partir de son JSON. Ses attaques possibles sont retrouvées par leur identifiant.
     *
     * @param json le JSON d'une espèce
     * @param movesById toutes les attaques déjà créées, rangées par leur identifiant
     * @return l'espèce
     * @throws InvalidSpeciesException s'il manque une information, si une valeur est inconnue
     *     ou si une attaque possible n'existe pas
     */
    public Species createSpecies(JsonObject json, Map<String, Move> movesById) {
        String name = readString(json, "originalName");

        List<Type> types = new ArrayList<>();
        for (JsonElement type : readArray(json, "types", name)) {
            types.add(toEnum(Type.class, type.getAsString(), "types", name));
        }

        List<Move> possibleMoves = new ArrayList<>();
        for (JsonElement moveId : readArray(json, "possibleMoves", name)) {
            Move move = movesById.get(moveId.getAsString());
            if (move == null) {
                throw new InvalidSpeciesException(
                    name + " : l'attaque « " + moveId.getAsString() + " » n'existe pas."
                );
            }
            possibleMoves.add(move);
        }

        JsonObject stats = readObject(json, "stats", name);
        JsonObject sprites = readObject(json, "sprites", name);

        Species.Builder builder = new Species.Builder()
            .originalName(name)
            .frenchName(readString(json, "frenchName", name))
            .types(types)
            .stats(new BaseStats(
                readInt(stats, "hp", name),
                readInt(stats, "attack", name),
                readInt(stats, "defense", name),
                readInt(stats, "specialAttack", name),
                readInt(stats, "specialDefense", name),
                readInt(stats, "speed", name)
            ))
            .possibleMoves(possibleMoves)
            .dexNumber(readInt(json, "dexNumber", name))
            .talent(readEnum(Talent.class, json, "talent", name))
            .frontSprite(readString(sprites, "front", name))
            .backSprite(readString(sprites, "back", name))
            .concept(readOptionalString(json, "concept"))
            .designer(readOptionalString(json, "designer"))
            .cry(readOptionalString(json, "cry"));

        String coverageType = readOptionalString(json, "coverageType");
        if (coverageType != null) {
            builder.coverageType(toEnum(Type.class, coverageType, "coverageType", name));
        }
        return builder.build();
    }

    /** Lit le texte obligatoire qui sert de nom dans les messages d'erreur. */
    private String readString(JsonObject json, String key) {
        return readString(json, key, "?");
    }

    private String readString(JsonObject json, String key, String owner) {
        JsonElement value = json.get(key);
        if (value == null || value.isJsonNull()) {
            throw missing(owner, key);
        }
        return value.getAsString();
    }

    private String readOptionalString(JsonObject json, String key) {
        JsonElement value = json.get(key);
        if (value == null || value.isJsonNull() || value.getAsString().isBlank()) {
            return null;
        }
        return value.getAsString();
    }

    private int readInt(JsonObject json, String key, String owner) {
        JsonElement value = json.get(key);
        if (value == null || value.isJsonNull()) {
            throw missing(owner, key);
        }
        try {
            return value.getAsInt();
        } catch (NumberFormatException exception) {
            throw new InvalidSpeciesException(owner + " : « " + key + " » doit être un nombre entier.");
        }
    }

    private JsonArray readArray(JsonObject json, String key, String owner) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonArray()) {
            throw missing(owner, key);
        }
        return value.getAsJsonArray();
    }

    private JsonObject readObject(JsonObject json, String key, String owner) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonObject()) {
            throw missing(owner, key);
        }
        return value.getAsJsonObject();
    }

    private <E extends Enum<E>> E readEnum(Class<E> enumType, JsonObject json, String key, String owner) {
        return toEnum(enumType, readString(json, key, owner), key, owner);
    }

    private <E extends Enum<E>> E toEnum(Class<E> enumType, String value, String key, String owner) {
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException exception) {
            throw new InvalidSpeciesException(
                owner + " : « " + value + " » n'est pas une valeur possible pour « " + key + " »."
            );
        }
    }

    private InvalidSpeciesException missing(String owner, String key) {
        return new InvalidSpeciesException(owner + " : l'information « " + key + " » est manquante.");
    }
}
