package be.winnetrie.mod.simplestages.stage.data;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.resources.Identifier;

public class StageDefinitionManager {

    private static final Map<Identifier, String> RECIPE_LOCKS = new HashMap<>();
    private static final Map<Identifier, ItemMaskEntry> ITEM_LOCKS = new HashMap<>();
    private static final Map<Identifier, BlockMaskEntry> BLOCK_LOCKS = new HashMap<>();
    private static final Map<Identifier, String> DIMENSION_STAGE_MAP = new HashMap<>();
    private static final Map<Identifier, MobStageEntry> MOB_LOCKS = new HashMap<>();
    private static final Map<String, StageDefinition> STAGES = new HashMap<>();
    private static final Map<String, String> DISPLAY_NAMES = new HashMap<>();

    public static void clear() {
        RECIPE_LOCKS.clear();
        ITEM_LOCKS.clear();
        BLOCK_LOCKS.clear();
        DIMENSION_STAGE_MAP.clear();
        MOB_LOCKS.clear();
        STAGES.clear();
        DISPLAY_NAMES.clear();
    }

    public static void addDefinition(StageDefinition definition) {
        String stage = definition.stage();

        STAGES.put(stage, definition);
        DISPLAY_NAMES.put(stage, definition.displayNameOrStage());

        for (Identifier recipe : definition.recipes()) {
            RECIPE_LOCKS.put(recipe, stage);
        }

        for (Identifier item : definition.items()) {
            ITEM_LOCKS.put(item, new ItemMaskEntry(item, stage));
        }

        for (BlockMaskEntry block : definition.blocks()) {
            BLOCK_LOCKS.put(block.block(), block);
        }

        for (Identifier dimension : definition.dimensions()) {
            DIMENSION_STAGE_MAP.put(dimension, stage);
        }

        for (MobStageEntry mob : definition.mobs()) {
            MOB_LOCKS.put(mob.mob(), mob);
        }
    }

    public static String getRequiredStageForRecipe(Identifier recipe) {
        return RECIPE_LOCKS.get(recipe);
    }

    public static String getRequiredStageForItem(Identifier item) {
        ItemMaskEntry entry = ITEM_LOCKS.get(item);
        return entry == null ? null : entry.stage();
    }

    public static String getRequiredStageForBlock(Identifier block) {
        BlockMaskEntry entry = BLOCK_LOCKS.get(block);

        if (entry == null) {
            return null;
        }

        for (StageDefinition definition : STAGES.values()) {
            for (BlockMaskEntry blockEntry : definition.blocks()) {
                if (blockEntry.block().equals(block)) {
                    return definition.stage();
                }
            }
        }

        return null;
    }

    public static String getRequiredStageForDimension(Identifier dimensionId) {
        return DIMENSION_STAGE_MAP.get(dimensionId);
    }

    public static String getRequiredStageForMob(Identifier mobId) {
        MobStageEntry entry = MOB_LOCKS.get(mobId);

        if (entry == null) {
            return null;
        }

        for (StageDefinition definition : STAGES.values()) {
            for (MobStageEntry mobEntry : definition.mobs()) {
                if (mobEntry.mob().equals(mobId)) {
                    return definition.stage();
                }
            }
        }

        return null;
    }

    public static double getMobSpawnRadius(Identifier mobId) {
        MobStageEntry entry = MOB_LOCKS.get(mobId);

        if (entry == null) {
            return 64.0D;
        }

        return entry.radius();
    }

    public static boolean hasMobLocked(Identifier mob) {
        return MOB_LOCKS.containsKey(mob);
    }

    public static String getDisplayName(String stageId) {
        String displayName = DISPLAY_NAMES.get(stageId);

        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }

        StageDefinition definition = STAGES.get(stageId);

        if (definition == null) {
            return stageId;
        }

        return definition.displayNameOrStage();
    }

    public static ItemMaskEntry getItemMask(Identifier item) {
        return ITEM_LOCKS.get(item);
    }

    public static BlockMaskEntry getBlockMask(Identifier block) {
        return BLOCK_LOCKS.get(block);
    }

    public static boolean hasRecipeLocked(Identifier recipe) {
        return RECIPE_LOCKS.containsKey(recipe);
    }

    public static boolean hasItemMasked(Identifier item) {
        return ITEM_LOCKS.containsKey(item);
    }

    public static boolean hasBlockMasked(Identifier block) {
        return BLOCK_LOCKS.containsKey(block);
    }

    public static StageDefinition getStageDefinition(String stage) {
        return STAGES.get(stage);
    }

    public static String getMessage(String stage, String key) {
        StageDefinition definition = STAGES.get(stage);

        if (definition == null) {
            return "";
        }

        return definition.getMessage(key);
    }

    public static Map<String, String> getItemStagesForSync() {
        Map<String, String> result = new HashMap<>();

        ITEM_LOCKS.forEach((item, entry) ->
                result.put(item.toString(), entry.stage())
        );

        return result;
    }

    public static Map<String, String> getDisplayNamesForSync() {
        return new HashMap<>(DISPLAY_NAMES);
    }

    public static void applyClientSync(Map<String, String> itemStages, Map<String, String> displayNames) {
        ITEM_LOCKS.clear();
        DISPLAY_NAMES.clear();

        displayNames.forEach(DISPLAY_NAMES::put);

        itemStages.forEach((itemId, stage) -> {
            Identifier id = Identifier.parse(itemId);
            ITEM_LOCKS.put(id, new ItemMaskEntry(id, stage));
        });
    }
}