package fr.epitech.fakemon.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import fr.epitech.fakemon.core.api.InvalidSpeciesException;
import fr.epitech.fakemon.core.api.SpeciesRepository;
import fr.epitech.fakemon.core.model.Move;
import fr.epitech.fakemon.core.model.Species;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Le catalogue des Fakemon, lu en JSON au démarrage.
 * Il contient les Fakemon fournis avec le jeu, puis ceux de l'utilisateur,
 * enregistrés à raison d'un fichier par Fakemon dans le sous-dossier {@code species} de son dossier.
 * Un Fakemon invalide empêche le démarrage : le message nomme le Fakemon, et le fichier s'il vient de l'utilisateur.
 */
public class JsonSpeciesRepository implements SpeciesRepository {

    /** Le fichier des Fakemon et des attaques fournis avec le jeu, dans les ressources. */
    private static final String BUILT_IN_FILE = "/data/fakemons.json";

    private final List<Species> speciesList = new ArrayList<>();
    private final Map<String, Move> movesById = new HashMap<>();
    private final SpeciesFactory factory;
    private final Path userFolder;

    /**
     * Crée le catalogue et le remplit tout de suite.
     *
     * @param factory la Factory qui transforme le JSON en objets du jeu
     * @param userFolder le dossier de l'utilisateur, par exemple {@code ~/.fakemon-arena}
     * @throws InvalidSpeciesException si un Fakemon, fourni ou de l'utilisateur, est invalide
     */
    public JsonSpeciesRepository(SpeciesFactory factory, Path userFolder) {
        this.factory = factory;
        this.userFolder = userFolder;
        load();
    }

    @Override
    public List<Species> findAll() {
        return new ArrayList<>(speciesList);
    }

    @Override
    public Optional<Species> findByOriginalName(String originalName) {
        for (Species species : speciesList) {
            if (species.getOriginalName().equals(originalName)) {
                return Optional.of(species);
            }
        }
        return Optional.empty();
    }

    @Override
    public void add(Species species) {
        throw new UnsupportedOperationException("L'ajout arrive avec la suite de #22.");
    }

    /** Lit les Fakemon fournis avec le jeu, puis ceux du dossier de l'utilisateur. */
    private void load() {
        JsonObject builtIn = readBuiltInFile();
        for (JsonElement moveJson : builtIn.getAsJsonArray("moves")) {
            JsonObject move = moveJson.getAsJsonObject();
            movesById.put(move.get("id").getAsString(), factory.createMove(move));
        }
        for (JsonElement speciesJson : builtIn.getAsJsonArray("species")) {
            addLoaded(factory.createSpecies(speciesJson.getAsJsonObject(), movesById));
        }
        loadUserSpecies();
    }

    private JsonObject readBuiltInFile() {
        InputStream stream = JsonSpeciesRepository.class.getResourceAsStream(BUILT_IN_FILE);
        if (stream == null) {
            throw new IllegalStateException("Le fichier " + BUILT_IN_FILE + " est introuvable.");
        }
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /** Lit un fichier par Fakemon dans le sous-dossier {@code species} de l'utilisateur, s'il existe. */
    private void loadUserSpecies() {
        Path folder = userFolder.resolve("species");
        if (!Files.isDirectory(folder)) {
            return;
        }
        try (DirectoryStream<Path> files = Files.newDirectoryStream(folder, "*.json")) {
            for (Path file : files) {
                loadUserFile(file);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private void loadUserFile(Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            addLoaded(factory.createSpecies(json, movesById));
        } catch (InvalidSpeciesException | IllegalStateException | JsonParseException exception) {
            throw new InvalidSpeciesException(
                exception.getMessage() + " Fichier à corriger ou supprimer : " + file.toAbsolutePath()
            );
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /** Ajoute une espèce lue, en refusant un nom d'origine déjà présent. */
    private void addLoaded(Species species) {
        if (findByOriginalName(species.getOriginalName()).isPresent()) {
            throw new InvalidSpeciesException(
                species.getOriginalName() + " : ce nom d'origine existe déjà dans le catalogue."
            );
        }
        speciesList.add(species);
    }
}
