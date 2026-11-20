package fr.epitech.fakemon.data;

import com.google.gson.JsonArray;
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

    /**
     * Valide un Fakemon, copie ses sprites et son cri éventuel, puis écrit sa fiche JSON
     * dans le dossier utilisateur. Un chemin de dos vide utilise une copie miroir de la face.
     * Le Fakemon rejoint le catalogue après l'enregistrement.
     *
     * @param species la fiche à ajouter, avec les chemins des fichiers source
     * @throws InvalidSpeciesException si la fiche est invalide ou si la sauvegarde échoue
     */
    @Override
    public void add(Species species) {
        String originalName = species.getOriginalName();
        if (findByOriginalName(originalName).isPresent()) {
            throw new InvalidSpeciesException(
                originalName + " : ce nom d'origine existe déjà dans le catalogue."
            );
        }
        validateSpecies(species);

        requireReadableFile(species.getFrontSprite(), originalName, "sprite de face");
        if (species.getBackSprite() != null && !species.getBackSprite().isBlank()) {
            requireReadableFile(species.getBackSprite(), originalName, "sprite de dos");
        }

        String cry = species.getCry();
        if (cry != null && !cry.isBlank()) {
            requireReadableFile(cry, originalName, "cri");
        }
        List<String> moveIds = new ArrayList<>();
        for (Move move : species.getPossibleMoves()) {
            String id = movesById.entrySet().stream()
                .filter(entry -> entry.getValue() == move)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new InvalidSpeciesException(
                    originalName + " : l'attaque « " + move.getName() + " » ne vient pas du catalogue."
                ));
            moveIds.add(id);
        }
        var types = species.getTypes();
        var stats = species.getStats();
        JsonObject json = new JsonObject();
        json.addProperty("originalName", originalName);
        json.addProperty("frenchName", species.getFrenchName());
        json.addProperty("dexNumber", species.getDexNumber());
        json.addProperty("talent", species.getTalent().name());

        if (species.getCoverageType() != null) {
            json.addProperty("coverageType", species.getCoverageType().name());
        }
        json.addProperty("concept", species.getConcept());
        json.addProperty("designer", species.getDesigner());

        JsonArray typesJson = new JsonArray();
        types.forEach(type -> typesJson.add(type.name()));
        json.add("types", typesJson);

        JsonArray movesJson = new JsonArray();
        moveIds.forEach(movesJson::add);
        json.add("possibleMoves", movesJson);

        JsonObject statsJson = new JsonObject();
        statsJson.addProperty("hp", stats.getHp());
        statsJson.addProperty("attack", stats.getAttack());
        statsJson.addProperty("defense", stats.getDefense());
        statsJson.addProperty("specialAttack", stats.getSpecialAttack());
        statsJson.addProperty("specialDefense", stats.getSpecialDefense());
        statsJson.addProperty("speed", stats.getSpeed());
        json.add("stats", statsJson);
        Path assetFolder = null;
        Path temporaryJson = null;
        try {
            Path speciesFolder = Files.createDirectories(userFolder.resolve("species"));
            Path assetsRoot = Files.createDirectories(userFolder.resolve("assets"));
            assetFolder = Files.createTempDirectory(assetsRoot, "fakemon-");

            copyAssets(species, assetFolder, json);
            Species savedSpecies = factory.createSpecies(json, movesById);

            String fileName = assetFolder.getFileName() + ".json";
            temporaryJson = Files.createTempFile(speciesFolder, "save-", ".tmp");
            Files.writeString(temporaryJson, json.toString(), StandardCharsets.UTF_8);
            Files.move(temporaryJson, speciesFolder.resolve(fileName));

            speciesList.add(savedSpecies);
        } catch (IOException | InvalidSpeciesException exception) {
            cleanFailedSave(temporaryJson, assetFolder, exception);
            throw new InvalidSpeciesException(
                originalName + " : enregistrement impossible. " + exception.getMessage()
            );
        }
    }

    private void validateSpecies(Species species) {
        String originalName = species.getOriginalName();
        var types = species.getTypes();
        if (types.isEmpty() || types.size() > 2 || types.stream().distinct().count() != types.size()) {
            throw new InvalidSpeciesException(
                originalName + " : le Fakemon doit avoir un ou deux types distincts."
            );
        }
        if (species.getPossibleMoves().stream().distinct().count() < 4) {
            throw new InvalidSpeciesException(
                originalName + " : le Fakemon doit avoir au moins quatre attaques différentes."
            );
        }
        var stats = species.getStats();
        double[] values = {
            stats.getHp(),
            stats.getAttack(),
            stats.getDefense(),
            stats.getSpecialAttack(),
            stats.getSpecialDefense(),
            stats.getSpeed()
        };

        double total = 0;
        for (double value : values) {
            if (!Double.isFinite(value) || value < 50 || value > 150) {
                throw new InvalidSpeciesException(
                    originalName + " : chaque statistique doit être comprise entre 50 et 150."
                );
            }
            total += value;
        }

        if (total > 600) {
            throw new InvalidSpeciesException(
                originalName + " : le total des statistiques ne doit pas dépasser 600."
            );
        }
    }

    private void cleanFailedSave(Path temporaryJson, Path assetFolder, Exception saveError) {
        deleteAfterFailedSave(temporaryJson, saveError);

        if (assetFolder == null) {
            return;
        }

        try (DirectoryStream<Path> files = Files.newDirectoryStream(assetFolder)) {
            for (Path file : files) {
                deleteAfterFailedSave(file, saveError);
            }
        } catch (IOException exception) {
            saveError.addSuppressed(exception);
        }

        deleteAfterFailedSave(assetFolder, saveError);
    }

    private void deleteAfterFailedSave(Path path, Exception saveError) {
        if (path == null) {
            return;
        }

        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            saveError.addSuppressed(exception);
        }
    }

    private void copyAssets(Species species, Path assetFolder, JsonObject json) throws IOException {
        JsonObject sprites = new JsonObject();
        sprites.addProperty("front", copyAsset(species.getFrontSprite(), assetFolder, "front"));
        String back = species.getBackSprite();
        if (back == null || back.isBlank()) {
            var front = javax.imageio.ImageIO.read(Path.of(species.getFrontSprite()).toFile());
            if (front == null) {
                throw new InvalidSpeciesException("Le sprite avant n'est pas une image lisible.");
            }
            var mirrored = new java.awt.image.BufferedImage(front.getWidth(), front.getHeight(),
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < front.getHeight(); y++) {
                for (int x = 0; x < front.getWidth(); x++) {
                    mirrored.setRGB(front.getWidth() - 1 - x, y, front.getRGB(x, y));
                }
            }
            Path destination = assetFolder.resolve("back.png");
            javax.imageio.ImageIO.write(mirrored, "png", destination.toFile());
            sprites.addProperty("back", userFolder.relativize(destination).toString().replace('\\', '/'));
        } else {
            sprites.addProperty("back", copyAsset(back, assetFolder, "back"));
        }
        json.add("sprites", sprites);

        String cry = species.getCry();
        if (cry != null && !cry.isBlank()) {
            json.addProperty("cry", copyAsset(cry, assetFolder, "cry"));
        }
    }

    private String copyAsset(String sourcePath, Path assetFolder, String prefix) throws IOException {
        Path source = Path.of(sourcePath);
        Path destination = assetFolder.resolve(prefix + "-" + source.getFileName());
        Files.copy(source, destination);
        return userFolder.relativize(destination).toString().replace('\\', '/');
    }

    private Path requireReadableFile(String value, String originalName, String label) {
        if (value == null || value.isBlank()) {
            throw new InvalidSpeciesException(originalName + " : " + label + " manquant.");
        }

        Path path;
        try {
            path = Path.of(value);
        } catch (java.nio.file.InvalidPathException exception) {
            throw new InvalidSpeciesException(originalName + " : chemin invalide pour " + label + ".");
        }

        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new InvalidSpeciesException(
                originalName + " : fichier absent ou illisible pour " + label + " : " + path
            );
        }
        return path;
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
            Species species = factory.createSpecies(json, movesById);
            validateSpecies(species);
            String originalName = species.getOriginalName();

            requireReadableFile(
                userFolder.resolve(species.getFrontSprite()).toString(), originalName, "sprite de face"
            );
            requireReadableFile(
                userFolder.resolve(species.getBackSprite()).toString(), originalName, "sprite de dos"
            );

            String cry = species.getCry();
            if (cry != null && !cry.isBlank()) {
                requireReadableFile(userFolder.resolve(cry).toString(), originalName, "cri");
            }

            addLoaded(species);
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
