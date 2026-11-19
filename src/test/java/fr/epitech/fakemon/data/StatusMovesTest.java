package fr.epitech.fakemon.data;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.epitech.fakemon.core.api.InvalidSpeciesException;
import fr.epitech.fakemon.core.model.*;
import fr.epitech.fakemon.net.NetworkMessage;
import fr.epitech.fakemon.net.NetworkTeamCodec;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Vérifie les attaques de statut dans le catalogue, les sauvegardes réseau et la fabrique. */
class StatusMovesTest {
    private final SpeciesFactory factory = new SpeciesFactory();

    @ParameterizedTest
    @EnumSource(value = StatusType.class, names = {"SLEEP", "CONFUSION"})
    void theExtraPureStatusMoveIsAssignedOnlyToSpeciesLearningItsType(StatusType status) {
        String base = status == StatusType.SLEEP ? "Lueur" : "Murmure";
        for (var species : BundledRoster.load().findAll()) {
            var moves = species.getPossibleMoves();
            var sleep = moves.stream().filter(move -> move.getStatus() == status).toList();
            boolean ether = moves.stream().anyMatch(move -> move.getName().equals(base));
            assertEquals(ether ? 1 : 0, sleep.size(), species.getFrenchName());
            if (ether) {
                var lullaby = sleep.getFirst();
                assertEquals(status == StatusType.SLEEP ? "Berceuse" : "Désorientation", lullaby.getName());
                assertEquals(typeFor(status), lullaby.getType());
                assertEquals(MoveCategory.STATUS, lullaby.getCategory());
                assertEquals(0, lullaby.getPower());
                assertEquals(100, lullaby.getAccuracy());
                assertEquals(100, lullaby.getStatusChance());
                assertEquals(0, lullaby.getPriority());
            }
        }
    }

    @ParameterizedTest
    @EnumSource(value = StatusType.class, names = {"BURN", "POISON", "PARALYSIS"})
    void onlySpeciesWithMatchingMovesReceiveEachStatusPair(StatusType status) {
        Type type = typeFor(status);
        String base = switch (status) {
            case BURN -> "Tison";
            case POISON -> "Aiguillon";
            case PARALYSIS -> "Grésillement";
            case SLEEP -> "Lueur";
            case CONFUSION -> "Murmure";
        };
        String damaging = switch (status) {
            case BURN -> "Braise tenace";
            case POISON -> "Toxine";
            case PARALYSIS -> "Décharge statique";
            case SLEEP -> throw new IllegalArgumentException("Le sommeil n'a pas d'attaque à dégâts.");
            case CONFUSION -> throw new IllegalArgumentException("La confusion n'a pas d'attaque à dégâts.");
        };
        String guaranteed = switch (status) {
            case BURN -> "Marque ardente";
            case POISON -> "Spores toxiques";
            case PARALYSIS -> "Cage électrique";
            case SLEEP -> "Berceuse";
            case CONFUSION -> "Désorientation";
        };
        var roster = BundledRoster.load();
        for (Species species : roster.findAll()) {
            var moves = species.getPossibleMoves();
            boolean learnsType = moves.stream().anyMatch(move -> move.getName().equals(base));
            var effects = moves.stream().filter(move -> move.getStatus() == status).toList();
            assertEquals(learnsType ? 2 : 0, effects.size(), species.getFrenchName());
            if (learnsType) {
                var ember = effects.stream().filter(move -> move.getName().equals(damaging)).findFirst().orElseThrow();
                assertEquals(status, ember.getStatus());
                assertEquals(MoveCategory.SPECIAL, ember.getCategory());
                assertEquals(50, ember.getPower());
                assertEquals(20, ember.getStatusChance());
                var mark = effects.stream().filter(move -> move.getName().equals(guaranteed)).findFirst().orElseThrow();
                assertEquals(MoveCategory.STATUS, mark.getCategory());
                assertEquals(0, mark.getPower());
                assertEquals(100, mark.getStatusChance());
                for (Move move : effects) {
                    assertEquals(type, move.getType());
                    assertEquals(100, move.getAccuracy());
                    assertEquals(0, move.getPriority());
                }
            }
        }
    }

    @Test
    void legacyMovesKeepTheirOriginalConstructorAndJsonFormat() {
        Move legacy = factory.createMove(json());
        assertNull(legacy.getStatus());
        assertEquals(0, legacy.getStatusChance());
        assertNull(new Move("Ancienne", Type.FIRE, MoveCategory.SPECIAL, 50, 100, 0).getStatus());
    }

    @Test
    void invalidStatusIdentifiersAndProbabilitiesAreRejected() {
        for (Number chance : List.<Number>of(-1, 0, 0.5, 101)) {
            JsonObject json = json();
            json.addProperty("status", "BURN");
            json.addProperty("statusChance", chance);
            assertThrows(InvalidSpeciesException.class, () -> factory.createMove(json));
        }
        for (String effect : List.of("UNKNOWN", "")) {
            JsonObject json = json();
            json.addProperty("status", effect);
            json.addProperty("statusChance", 20);
            assertThrows(InvalidSpeciesException.class, () -> factory.createMove(json));
        }
        JsonObject wrongType = json();
        wrongType.add("status", new JsonObject());
        assertThrows(InvalidSpeciesException.class, () -> factory.createMove(wrongType));
        wrongType.addProperty("status", "BURN");
        wrongType.add("statusChance", JsonNull.INSTANCE);
        assertThrows(InvalidSpeciesException.class, () -> factory.createMove(wrongType));
        wrongType.addProperty("statusChance", "20");
        assertThrows(InvalidSpeciesException.class, () -> factory.createMove(wrongType));
        assertThrows(IllegalArgumentException.class,
                () -> new Move("Invalide", Type.FIRE, MoveCategory.STATUS, 50, 100, 0, StatusType.BURN, 100));
    }

    @ParameterizedTest
    @EnumSource(value = StatusType.class, names = {"BURN", "POISON", "PARALYSIS", "SLEEP", "CONFUSION"})
    void statusMovesSurviveTeamTransportAndInvalidReceivedChancesAreRejected(StatusType status) {
        Type type = typeFor(status);
        var roster = BundledRoster.load().findAll();
        Species fire = roster.stream().filter(species -> species.getTypes().contains(type)).findFirst().orElseThrow();
        List<Fakemon> members = new ArrayList<>();
        var chosen = new ArrayList<>(fire.getPossibleMoves().stream().filter(move -> move.getStatus() == status).toList());
        int effectCount = chosen.size();
        chosen.addAll(fire.getPossibleMoves().subList(0, 4 - effectCount));
        members.add(new Fakemon(fire, chosen));
        roster.stream().filter(species -> species != fire).limit(5)
                .forEach(species -> members.add(new Fakemon(species, species.getPossibleMoves().subList(0, 4))));
        NetworkTeamCodec codec = new NetworkTeamCodec();
        NetworkMessage message = codec.encode(new Team(members));
        var received = codec.decode(message).getFakemons().getFirst().getMoves();
        assertEquals(status == StatusType.SLEEP || status == StatusType.CONFUSION ? List.of(100) : List.of(20, 100),
                received.subList(0, effectCount).stream().map(Move::getStatusChance).toList());
        assertTrue(received.subList(0, effectCount).stream().allMatch(move -> move.getStatus() == status));
        var moves = message.getPayload().getAsJsonArray("members").get(0).getAsJsonObject()
                .getAsJsonObject("species").getAsJsonArray("possibleMoves");
        moves.get(moves.size() - 1).getAsJsonObject().addProperty("statusChance", 101);
        assertThrows(com.google.gson.JsonParseException.class, () -> codec.decode(message));
    }

    private JsonObject json() {
        return JsonParser.parseString("""
                {"id":"test","name":"Test","type":"FIRE","category":"SPECIAL", "power":50,"accuracy":100,"priority":0}
                """).getAsJsonObject();
    }

    private static Type typeFor(StatusType status) {
        return switch (status) {
            case BURN -> Type.FIRE;
            case POISON -> Type.VENOM;
            case PARALYSIS -> Type.LIGHTNING;
            case SLEEP -> Type.ETHER;
            case CONFUSION -> Type.SHADOW;
        };
    }
}
