package be.winnetrie.mod.simplestages.stage.data;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import net.minecraft.resources.Identifier;

/**
 * Runtime source of truth for all Simple Stages definitions.
 *
 * <p>The stage definitions themselves are authoritative. All fast lookup maps are
 * rebuilt as one immutable snapshot and swapped atomically, keeping live
 * create/edit/delete operations from the in-game editor consistent.</p>
 */
public final class StageDefinitionManager {

    private static final Pattern STAGE_ID_PATTERN = Pattern.compile("[a-z0-9_.-]+");

    private static volatile Snapshot snapshot = Snapshot.empty();

    private StageDefinitionManager() {
    }

    public static synchronized void clear() {
        snapshot = Snapshot.empty();
    }

    public static synchronized void replaceDefinitions(Collection<StageDefinition> definitions) {
        snapshot = buildSnapshot(definitions);
    }

    public static synchronized void upsertDefinition(StageDefinition definition) {
        Objects.requireNonNull(definition, "definition");

        LinkedHashMap<String, StageDefinition> definitions = new LinkedHashMap<>(snapshot.stages());
        definitions.put(definition.stage(), definition);
        snapshot = buildSnapshot(definitions.values());
    }

    public static synchronized boolean removeDefinition(String stage) {
        if (!snapshot.stages().containsKey(stage)) {
            return false;
        }

        LinkedHashMap<String, StageDefinition> definitions = new LinkedHashMap<>(snapshot.stages());
        definitions.remove(stage);
        snapshot = buildSnapshot(definitions.values());
        return true;
    }

    /**
     * Validates a complete definition set without changing runtime state.
     * Throws IllegalArgumentException with a user-facing explanation when invalid.
     */
    public static void validateDefinitions(Collection<StageDefinition> definitions) {
        buildSnapshot(definitions);
    }

    public static List<StageDefinition> getDefinitions() {
        return List.copyOf(snapshot.stages().values());
    }

    public static Map<String, StageDefinition> getDefinitionsById() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(snapshot.stages()));
    }

    public static String getRequiredStageForRecipe(Identifier recipe) {
        return snapshot.recipeLocks().get(recipe);
    }

    public static String getRequiredStageForItem(Identifier item) {
        ItemMaskEntry entry = snapshot.itemLocks().get(item);
        return entry == null ? null : entry.stage();
    }

    public static String getRequiredStageForBlock(Identifier block) {
        return snapshot.blockStages().get(block);
    }

    public static String getRequiredStageForDimension(Identifier dimensionId) {
        return snapshot.dimensionStages().get(dimensionId);
    }

    public static String getRequiredStageForMob(Identifier mobId) {
        return snapshot.mobStages().get(mobId);
    }

    public static double getMobSpawnRadius(Identifier mobId) {
        MobStageEntry entry = snapshot.mobLocks().get(mobId);
        return entry == null ? 64.0D : entry.radius();
    }

    public static boolean hasMobLocked(Identifier mob) {
        return snapshot.mobLocks().containsKey(mob);
    }

    public static String getDisplayName(String stageId) {
        return snapshot.displayNames().getOrDefault(stageId, stageId);
    }

    public static ItemMaskEntry getItemMask(Identifier item) {
        return snapshot.itemLocks().get(item);
    }

    public static BlockMaskEntry getBlockMask(Identifier block) {
        return snapshot.blockLocks().get(block);
    }

    public static boolean hasRecipeLocked(Identifier recipe) {
        return snapshot.recipeLocks().containsKey(recipe);
    }

    public static boolean hasItemMasked(Identifier item) {
        return snapshot.itemLocks().containsKey(item);
    }

    public static boolean hasBlockMasked(Identifier block) {
        return snapshot.blockLocks().containsKey(block);
    }

    public static StageDefinition getStageDefinition(String stage) {
        return snapshot.stages().get(stage);
    }

    public static String getMessage(String stage, String key) {
        StageDefinition definition = snapshot.stages().get(stage);
        return definition == null ? "" : definition.getMessage(key);
    }

    public static Map<String, String> getItemStagesForSync() {
        Map<String, String> result = new HashMap<>();
        snapshot.itemLocks().forEach((item, entry) -> result.put(item.toString(), entry.stage()));
        return result;
    }

    public static Map<String, String> getDisplayNamesForSync() {
        return new HashMap<>(snapshot.displayNames());
    }

    /**
     * Block -> required stage, used by the client for immediate block interaction
     * checks and visual masks.
     */
    public static Map<String, String> getBlockStagesForSync() {
        Map<String, String> result = new HashMap<>();
        snapshot.blockStages().forEach((block, stage) -> result.put(block.toString(), stage));
        return result;
    }

    /**
     * Block -> replacement block. Entries without a configured mask are omitted.
     */
    public static Map<String, String> getBlockMasksForSync() {
        Map<String, String> result = new HashMap<>();
        snapshot.blockLocks().forEach((block, entry) ->
                entry.mask().ifPresent(mask -> result.put(block.toString(), mask.toString()))
        );
        return result;
    }

    /**
     * Applies the lightweight definition data needed by client-side masking and
     * interaction feedback. Runtime/server definitions remain authoritative.
     */
    public static synchronized void applyClientSync(
            Map<String, String> itemStages,
            Map<String, String> blockStages,
            Map<String, String> blockMasks,
            Map<String, String> displayNames
    ) {
        Map<Identifier, ItemMaskEntry> itemLocks = new HashMap<>();
        itemStages.forEach((itemId, stage) -> {
            Identifier id = Identifier.parse(itemId);
            itemLocks.put(id, new ItemMaskEntry(id, stage));
        });

        Map<Identifier, String> syncedBlockStages = new HashMap<>();
        Map<Identifier, BlockMaskEntry> blockLocks = new HashMap<>();
        blockStages.forEach((blockId, stage) -> {
            Identifier id = Identifier.parse(blockId);
            syncedBlockStages.put(id, stage);

            String maskId = blockMasks.get(blockId);
            Optional<Identifier> mask = maskId == null || maskId.isBlank()
                    ? Optional.empty()
                    : Optional.of(Identifier.parse(maskId));
            blockLocks.put(id, new BlockMaskEntry(id, mask));
        });

        Snapshot old = snapshot;
        snapshot = new Snapshot(
                old.stages(),
                old.recipeLocks(),
                immutable(itemLocks),
                immutable(blockLocks),
                immutable(syncedBlockStages),
                old.dimensionStages(),
                old.mobLocks(),
                old.mobStages(),
                immutable(new HashMap<>(displayNames)),
                old.structureLocks()
        );
    }

    public static StructureLockEntry getStructureLock(Identifier structureId) {
        return snapshot.structureLocks().get(structureId);
    }

    public static Map<Identifier, StructureLockEntry> getStructureLocks() {
        return new HashMap<>(snapshot.structureLocks());
    }

    private static Snapshot buildSnapshot(Collection<StageDefinition> definitions) {
        Objects.requireNonNull(definitions, "definitions");

        LinkedHashMap<String, StageDefinition> stages = new LinkedHashMap<>();
        Map<Identifier, String> recipeLocks = new HashMap<>();
        Map<Identifier, ItemMaskEntry> itemLocks = new HashMap<>();
        Map<Identifier, BlockMaskEntry> blockLocks = new HashMap<>();
        Map<Identifier, String> blockStages = new HashMap<>();
        Map<Identifier, String> dimensionStages = new HashMap<>();
        Map<Identifier, MobStageEntry> mobLocks = new HashMap<>();
        Map<Identifier, String> mobStages = new HashMap<>();
        Map<String, String> displayNames = new HashMap<>();
        Map<Identifier, StructureLockEntry> structureLocks = new HashMap<>();

        for (StageDefinition definition : definitions) {
            validateDefinition(definition);

            String stage = definition.stage();
            if (stages.putIfAbsent(stage, definition) != null) {
                throw new IllegalArgumentException("Duplicate stage id '" + stage + "'.");
            }

            displayNames.put(stage, definition.displayNameOrStage());

            for (Identifier recipe : safe(definition.recipes())) {
                putUnique(recipeLocks, recipe, stage, "recipe");
            }

            for (Identifier item : safe(definition.items())) {
                putUniqueStage(itemLocks, item, stage, "item");
                itemLocks.put(item, new ItemMaskEntry(item, stage));
            }

            for (BlockMaskEntry block : safe(definition.blocks())) {
                if (block == null || block.block() == null) {
                    throw new IllegalArgumentException("Stage '" + stage + "' contains an invalid block entry.");
                }
                putUnique(blockStages, block.block(), stage, "block");
                blockLocks.put(block.block(), block);
            }

            for (Identifier dimension : safe(definition.dimensions())) {
                putUnique(dimensionStages, dimension, stage, "dimension");
            }

            for (MobStageEntry mob : safe(definition.mobs())) {
                if (mob == null || mob.mob() == null) {
                    throw new IllegalArgumentException("Stage '" + stage + "' contains an invalid mob entry.");
                }
                if (mob.radius() <= 0.0D) {
                    throw new IllegalArgumentException("Mob '" + mob.mob() + "' in stage '" + stage + "' must have a radius greater than 0.");
                }
                putUnique(mobStages, mob.mob(), stage, "mob");
                mobLocks.put(mob.mob(), mob);
            }

            for (StructureStageEntry structure : safe(definition.structures())) {
                if (structure == null || structure.structure() == null) {
                    throw new IllegalArgumentException("Stage '" + stage + "' contains an invalid structure entry.");
                }
                StructureLockEntry previous = structureLocks.putIfAbsent(
                        structure.structure(),
                        new StructureLockEntry(stage, structure.bufferOrDefault())
                );
                if (previous != null && !previous.stage().equals(stage)) {
                    throw conflict("structure", structure.structure(), previous.stage(), stage);
                }
            }
        }

        return new Snapshot(
                immutableLinked(stages),
                immutable(recipeLocks),
                immutable(itemLocks),
                immutable(blockLocks),
                immutable(blockStages),
                immutable(dimensionStages),
                immutable(mobLocks),
                immutable(mobStages),
                immutable(displayNames),
                immutable(structureLocks)
        );
    }

    private static void validateDefinition(StageDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Stage definition cannot be null.");
        }

        String stage = definition.stage();
        if (stage == null || stage.isBlank()) {
            throw new IllegalArgumentException("Stage id cannot be empty.");
        }
        if (!STAGE_ID_PATTERN.matcher(stage).matches()) {
            throw new IllegalArgumentException(
                    "Invalid stage id '" + stage + "'. Use lowercase letters, numbers, '_', '-' or '.'."
            );
        }
    }

    private static <K> void putUnique(Map<K, String> map, K key, String stage, String type) {
        if (key == null) {
            throw new IllegalArgumentException("Stage '" + stage + "' contains a null " + type + " id.");
        }
        String previous = map.putIfAbsent(key, stage);
        if (previous != null && !previous.equals(stage)) {
            throw conflict(type, key, previous, stage);
        }
    }

    private static void putUniqueStage(Map<Identifier, ItemMaskEntry> map, Identifier key, String stage, String type) {
        ItemMaskEntry previous = map.get(key);
        if (previous != null && !previous.stage().equals(stage)) {
            throw conflict(type, key, previous.stage(), stage);
        }
    }

    private static IllegalArgumentException conflict(String type, Object id, String firstStage, String secondStage) {
        return new IllegalArgumentException(
                "The " + type + " '" + id + "' is assigned to both stage '" + firstStage + "' and '" + secondStage + "'."
        );
    }

    private static <T> List<T> safe(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static <K, V> Map<K, V> immutable(Map<K, V> map) {
        return Collections.unmodifiableMap(new HashMap<>(map));
    }

    private static <K, V> Map<K, V> immutableLinked(LinkedHashMap<K, V> map) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(map));
    }

    public record StructureLockEntry(String stage, int buffer) {
    }

    private record Snapshot(
            Map<String, StageDefinition> stages,
            Map<Identifier, String> recipeLocks,
            Map<Identifier, ItemMaskEntry> itemLocks,
            Map<Identifier, BlockMaskEntry> blockLocks,
            Map<Identifier, String> blockStages,
            Map<Identifier, String> dimensionStages,
            Map<Identifier, MobStageEntry> mobLocks,
            Map<Identifier, String> mobStages,
            Map<String, String> displayNames,
            Map<Identifier, StructureLockEntry> structureLocks
    ) {
        private static Snapshot empty() {
            return new Snapshot(
                    Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
                    Map.of(), Map.of(), Map.of(), Map.of(), Map.of()
            );
        }
    }
}
