package be.winnetrie.mod.simplestages.stage.data;

import be.winnetrie.mod.simplestages.SimpleStages;
import be.winnetrie.mod.simplestages.stage.StageManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Native Simple Stages persistence. Datapacks are intentionally not part of
 * the runtime configuration model anymore.
 *
 * <p>Modpacks ship defaults in defaultconfigs/simplestages/stages.json.
 * Each world receives its own editable copy in serverconfig/simplestages/stages.json.</p>
 */
public final class StageDefinitionStorage {

    public static final int SCHEMA = 1;

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private StageDefinitionStorage() {
    }

    public static void initialize(MinecraftServer server) {
        Path worldFile = getWorldFile(server);
        Path defaultsFile = getPackDefaultsFile();

        try {
            Files.createDirectories(worldFile.getParent());

            if (!Files.exists(worldFile)) {
                if (Files.isRegularFile(defaultsFile)) {
                    Files.copy(defaultsFile, worldFile, StandardCopyOption.COPY_ATTRIBUTES);
                    SimpleStages.LOGGER.info("Copied Simple Stages modpack defaults to {}", worldFile);
                } else {
                    writeFile(worldFile, List.of());
                    SimpleStages.LOGGER.info("Created empty Simple Stages world stage store at {}", worldFile);
                }
            }

            reload(server);
        } catch (Exception exception) {
            SimpleStages.LOGGER.error("Failed to initialize Simple Stages stage storage", exception);
            StageDefinitionManager.clear();
        }
    }

    public static void reload(MinecraftServer server) throws IOException {
        List<StageDefinition> definitions = readFile(getWorldFile(server));
        StageDefinitionManager.replaceDefinitions(definitions);
        syncDefinitionsToPlayers(server);
        SimpleStages.LOGGER.info("Loaded {} Simple Stages definitions from world storage", definitions.size());
    }

    public static void saveAll(MinecraftServer server, Collection<StageDefinition> definitions) throws IOException {
        List<StageDefinition> copy = List.copyOf(definitions);
        StageDefinitionManager.validateDefinitions(copy);
        writeFile(getWorldFile(server), copy);
        StageDefinitionManager.replaceDefinitions(copy);
        syncDefinitionsToPlayers(server);
    }

    public static void upsert(MinecraftServer server, StageDefinition definition) throws IOException {
        LinkedHashMap<String, StageDefinition> definitions = new LinkedHashMap<>(StageDefinitionManager.getDefinitionsById());
        definitions.put(definition.stage(), definition);
        saveAll(server, definitions.values());
    }

    public static boolean delete(MinecraftServer server, String stage) throws IOException {
        LinkedHashMap<String, StageDefinition> definitions = new LinkedHashMap<>(StageDefinitionManager.getDefinitionsById());
        if (definitions.remove(stage) == null) {
            return false;
        }
        saveAll(server, definitions.values());
        return true;
    }

    /**
     * Writes the current world definitions to the location a modpack author can
     * ship. Existing worlds are never overwritten by this export.
     */
    public static Path exportPackDefaults(MinecraftServer server) throws IOException {
        Path path = getPackDefaultsFile();
        writeFile(path, StageDefinitionManager.getDefinitions());
        SimpleStages.LOGGER.info("Exported Simple Stages modpack defaults to {}", path);
        return path;
    }

    public static Path getWorldFile(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT)
                .resolve("serverconfig")
                .resolve(SimpleStages.MODID)
                .resolve("stages.json");
    }

    public static Path getPackDefaultsFile() {
        return FMLPaths.GAMEDIR.get()
                .resolve("defaultconfigs")
                .resolve(SimpleStages.MODID)
                .resolve("stages.json");
    }

    private static List<StageDefinition> readFile(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            return List.of();
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) {
                throw new IOException("Simple Stages stage file root must be a JSON object: " + path);
            }

            JsonObject root = parsed.getAsJsonObject();
            int schema = root.has("schema") ? root.get("schema").getAsInt() : SCHEMA;
            if (schema != SCHEMA) {
                throw new IOException("Unsupported Simple Stages stage schema " + schema + " (expected " + SCHEMA + ")");
            }

            JsonArray stages = root.has("stages") ? root.getAsJsonArray("stages") : new JsonArray();
            List<StageDefinition> definitions = new ArrayList<>(stages.size());
            for (int i = 0; i < stages.size(); i++) {
                JsonElement element = stages.get(i);
                if (!element.isJsonObject()) {
                    throw new IOException("Stage entry #" + i + " must be a JSON object.");
                }
                definitions.add(readDefinition(element.getAsJsonObject(), i));
            }

            StageDefinitionManager.validateDefinitions(definitions);
            return definitions;
        } catch (RuntimeException exception) {
            throw new IOException("Invalid Simple Stages stage file " + path + ": " + exception.getMessage(), exception);
        }
    }

    private static StageDefinition readDefinition(JsonObject object, int index) throws IOException {
        String stage = requiredString(object, "stage", "stage #" + index);
        String displayName = optionalString(object, "display_name", "");

        Map<String, String> messages = new LinkedHashMap<>();
        if (object.has("messages")) {
            JsonObject messageObject = object.getAsJsonObject("messages");
            messageObject.entrySet().forEach(entry -> messages.put(entry.getKey(), entry.getValue().getAsString()));
        }

        List<Identifier> recipes = readIdentifierArray(object, "recipes");
        List<Identifier> items = readIdentifierArray(object, "items");
        List<Identifier> dimensions = readIdentifierArray(object, "dimensions");

        List<BlockMaskEntry> blocks = new ArrayList<>();
        if (object.has("blocks")) {
            for (JsonElement element : object.getAsJsonArray("blocks")) {
                if (element.isJsonPrimitive()) {
                    blocks.add(new BlockMaskEntry(parseId(element.getAsString(), "block"), Optional.empty()));
                    continue;
                }
                JsonObject block = element.getAsJsonObject();
                Identifier blockId = parseId(requiredString(block, "block", "block entry"), "block");
                Optional<Identifier> mask = block.has("mask")
                        ? Optional.of(parseId(block.get("mask").getAsString(), "block mask"))
                        : Optional.empty();
                blocks.add(new BlockMaskEntry(blockId, mask));
            }
        }

        List<MobStageEntry> mobs = new ArrayList<>();
        if (object.has("mobs")) {
            for (JsonElement element : object.getAsJsonArray("mobs")) {
                if (element.isJsonPrimitive()) {
                    mobs.add(new MobStageEntry(parseId(element.getAsString(), "mob"), 64.0D));
                    continue;
                }
                JsonObject mob = element.getAsJsonObject();
                Identifier mobId = parseId(requiredString(mob, "mob", "mob entry"), "mob");
                double radius = mob.has("radius") ? mob.get("radius").getAsDouble() : 64.0D;
                mobs.add(new MobStageEntry(mobId, radius));
            }
        }

        List<StructureStageEntry> structures = new ArrayList<>();
        if (object.has("structures")) {
            for (JsonElement element : object.getAsJsonArray("structures")) {
                if (element.isJsonPrimitive()) {
                    structures.add(new StructureStageEntry(
                            parseId(element.getAsString(), "structure"),
                            StructureStageEntry.DEFAULT_BUFFER
                    ));
                    continue;
                }
                JsonObject structure = element.getAsJsonObject();
                Identifier structureId = parseId(requiredString(structure, "structure", "structure entry"), "structure");
                int buffer = structure.has("buffer")
                        ? structure.get("buffer").getAsInt()
                        : StructureStageEntry.DEFAULT_BUFFER;
                structures.add(new StructureStageEntry(structureId, buffer));
            }
        }

        return new StageDefinition(
                stage,
                displayName,
                messages,
                recipes,
                items,
                blocks,
                dimensions,
                mobs,
                structures
        );
    }

    private static void writeFile(Path path, Collection<StageDefinition> definitions) throws IOException {
        Files.createDirectories(path.getParent());

        JsonObject root = new JsonObject();
        root.addProperty("schema", SCHEMA);
        JsonArray stageArray = new JsonArray();
        for (StageDefinition definition : definitions) {
            stageArray.add(writeDefinition(definition));
        }
        root.add("stages", stageArray);

        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }

        try {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static JsonObject writeDefinition(StageDefinition definition) {
        JsonObject object = new JsonObject();
        object.addProperty("stage", definition.stage());
        if (definition.displayName() != null && !definition.displayName().isBlank()) {
            object.addProperty("display_name", definition.displayName());
        }

        if (definition.messages() != null && !definition.messages().isEmpty()) {
            JsonObject messages = new JsonObject();
            definition.messages().forEach(messages::addProperty);
            object.add("messages", messages);
        }

        addIdentifierArray(object, "recipes", definition.recipes());
        addIdentifierArray(object, "items", definition.items());

        if (definition.blocks() != null && !definition.blocks().isEmpty()) {
            JsonArray blocks = new JsonArray();
            for (BlockMaskEntry entry : definition.blocks()) {
                JsonObject block = new JsonObject();
                block.addProperty("block", entry.block().toString());
                entry.mask().ifPresent(mask -> block.addProperty("mask", mask.toString()));
                blocks.add(block);
            }
            object.add("blocks", blocks);
        }

        addIdentifierArray(object, "dimensions", definition.dimensions());

        if (definition.mobs() != null && !definition.mobs().isEmpty()) {
            JsonArray mobs = new JsonArray();
            for (MobStageEntry entry : definition.mobs()) {
                JsonObject mob = new JsonObject();
                mob.addProperty("mob", entry.mob().toString());
                mob.addProperty("radius", entry.radius());
                mobs.add(mob);
            }
            object.add("mobs", mobs);
        }

        if (definition.structures() != null && !definition.structures().isEmpty()) {
            JsonArray structures = new JsonArray();
            for (StructureStageEntry entry : definition.structures()) {
                JsonObject structure = new JsonObject();
                structure.addProperty("structure", entry.structure().toString());
                structure.addProperty("buffer", entry.bufferOrDefault());
                structures.add(structure);
            }
            object.add("structures", structures);
        }

        return object;
    }

    private static List<Identifier> readIdentifierArray(JsonObject object, String key) throws IOException {
        List<Identifier> result = new ArrayList<>();
        if (!object.has(key)) {
            return result;
        }
        for (JsonElement element : object.getAsJsonArray(key)) {
            result.add(parseId(element.getAsString(), key));
        }
        return result;
    }

    private static void addIdentifierArray(JsonObject object, String key, List<Identifier> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        JsonArray array = new JsonArray();
        values.forEach(value -> array.add(value.toString()));
        object.add(key, array);
    }

    private static Identifier parseId(String value, String what) throws IOException {
        try {
            return Identifier.parse(value);
        } catch (RuntimeException exception) {
            throw new IOException("Invalid " + what + " id '" + value + "'.", exception);
        }
    }

    private static String requiredString(JsonObject object, String key, String context) throws IOException {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) {
            throw new IOException("Missing required '" + key + "' in " + context + ".");
        }
        String value = object.get(key).getAsString();
        if (value.isBlank()) {
            throw new IOException("'" + key + "' cannot be empty in " + context + ".");
        }
        return value;
    }

    private static String optionalString(JsonObject object, String key, String fallback) {
        return object.has(key) ? object.get(key).getAsString() : fallback;
    }

    private static void syncDefinitionsToPlayers(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(StageManager::syncDefinitions);
    }
}
