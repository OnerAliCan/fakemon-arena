package fr.epitech.fakemon.net;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import fr.epitech.fakemon.core.api.InvalidSpeciesException;
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Item;
import fr.epitech.fakemon.core.model.Move;
import fr.epitech.fakemon.core.model.Species;
import fr.epitech.fakemon.core.model.Team;
import fr.epitech.fakemon.data.SpeciesFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** Transporte une équipe initiale avec ses espèces, ses attaques et ses objets. */
public final class NetworkTeamCodec {
    private final Gson gson = new Gson();
    private final SpeciesFactory factory = new SpeciesFactory();

    /**
     * Prépare une équipe avant le début du combat.
     *
     * @param team l'équipe tirée par l'hôte
     * @return le message TEAM contenant les six membres dans leur ordre de jeu
     * @throws IllegalArgumentException si l'équipe est incomplète ou déjà blessée
     */
    public NetworkMessage encode(Team team) {
        if (team.getFakemons().size() != 6) {
            throw new IllegalArgumentException("Une équipe LAN doit contenir six Fakemon.");
        }
        JsonArray members = new JsonArray();
        for (Fakemon mon : team.getFakemons()) {
            if (mon.getCurrentHp() != mon.getMaxHp()) {
                throw new IllegalArgumentException("L'équipe initiale doit avoir tous ses PV.");
            }
            JsonObject member = new JsonObject();
            member.add("species", gson.toJsonTree(mon.getSpecies()));
            JsonArray chosen = new JsonArray();
            for (Move move : mon.getMoves()) {
                int index = mon.getSpecies().getPossibleMoves().indexOf(move);
                if (index < 0) {
                    throw new IllegalArgumentException("Une attaque est absente des attaques possibles de l'espèce.");
                }
                chosen.add(index);
            }
            member.add("chosenMoves", chosen);
            member.add("item", mon.getItem() == null ? JsonNull.INSTANCE : gson.toJsonTree(mon.getItem()));
            members.add(member);
        }
        JsonObject payload = new JsonObject();
        payload.add("members", members);
        return new NetworkMessage("TEAM", payload);
    }

    /**
     * Reconstruit une équipe à tous ses PV et recalcule les bonus de ses objets.
     *
     * @param message le message TEAM reçu
     * @return l'équipe avec des objets de combat propres à ce poste
     * @throws JsonParseException si une espèce, une attaque ou un objet est invalide
     */
    public Team decode(NetworkMessage message) {
        if (message == null || !"TEAM".equals(message.getType()) || message.getPayload() == null) {
            throw new JsonParseException("Un message TEAM est attendu.");
        }
        try {
            JsonArray members = message.getPayload().getAsJsonArray("members");
            if (members == null || members.size() != 6) {
                throw new JsonParseException("Une équipe LAN doit contenir six Fakemon.");
            }
            List<Fakemon> team = new ArrayList<>();
            for (JsonElement element : members) {
                team.add(decodeMember(element.getAsJsonObject()));
            }
            return new Team(team);
        } catch (InvalidSpeciesException | IllegalArgumentException | IllegalStateException
                | ArithmeticException | NullPointerException | ClassCastException error) {
            throw new JsonParseException("Équipe réseau invalide.", error);
        }
    }

    private Fakemon decodeMember(JsonObject member) {
        JsonObject speciesData = member.getAsJsonObject("species").deepCopy();
        readInt(speciesData.get("dexNumber"), 0, Integer.MAX_VALUE);
        JsonArray movesData = speciesData.getAsJsonArray("possibleMoves");
        var movesById = new HashMap<String, Move>();
        JsonArray moveIds = new JsonArray();
        for (int index = 0; index < movesData.size(); index++) {
            String id = Integer.toString(index);
            JsonObject moveData = movesData.get(index).getAsJsonObject();
            moveData.addProperty("id", id);
            validateMoveNumbers(moveData);
            movesById.put(id, factory.createMove(moveData));
            moveIds.add(id);
        }
        speciesData.add("possibleMoves", moveIds);
        JsonObject sprites = new JsonObject();
        sprites.add("front", speciesData.get("frontSprite"));
        sprites.add("back", speciesData.get("backSprite"));
        speciesData.add("sprites", sprites);
        validateStats(speciesData.getAsJsonObject("stats"));
        Species species = factory.createSpecies(speciesData, movesById);
        if (species.getTypes().isEmpty() || species.getTypes().size() > 2
                || species.getTypes().stream().distinct().count() != species.getTypes().size()) {
            throw new JsonParseException("Une espèce doit avoir un ou deux types distincts.");
        }
        JsonArray chosen = member.getAsJsonArray("chosenMoves");
        if (chosen == null || chosen.size() != 4) {
            throw new JsonParseException("Un Fakemon doit avoir quatre attaques.");
        }
        List<Move> moves = new ArrayList<>();
        for (JsonElement value : chosen) {
            int index = readInt(value, 0, species.getPossibleMoves().size() - 1);
            moves.add(species.getPossibleMoves().get(index));
        }
        Fakemon mon = new Fakemon(species, moves);
        if (!member.has("item")) {
            throw new JsonParseException("L'objet tenu doit être précisé, même s'il est absent.");
        }
        JsonElement item = member.get("item");
        if (!item.isJsonNull()) {
            if (!item.isJsonPrimitive() || !item.getAsJsonPrimitive().isString()) {
                throw new JsonParseException("L'objet tenu doit être identifié par son nom.");
            }
            mon.equip(Item.valueOf(item.getAsString()));
        }
        return mon;
    }

    private void validateMoveNumbers(JsonObject move) {
        readInt(move.get("power"), 0, Integer.MAX_VALUE);
        readInt(move.get("accuracy"), 0, 100);
        readInt(move.get("priority"), Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    private void validateStats(JsonObject stats) {
        for (String field : List.of("hp", "attack", "defense", "specialAttack", "specialDefense", "speed")) {
            readInt(stats.get(field), 1, Integer.MAX_VALUE);
        }
    }

    private int readInt(JsonElement value, int minimum, int maximum) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException("Un entier est attendu dans les données de l'équipe.");
        }
        int number = value.getAsBigDecimal().intValueExact();
        if (number < minimum || number > maximum) {
            throw new JsonParseException("Une valeur de l'équipe est hors limites.");
        }
        return number;
    }
}
