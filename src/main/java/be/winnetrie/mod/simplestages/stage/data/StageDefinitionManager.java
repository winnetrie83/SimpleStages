package be.winnetrie.mod.simplestages.stage.data;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.resources.Identifier;

public class StageDefinitionManager {

    private static final Map<Identifier, String> RECIPE_LOCKS = new HashMap<>();
    private static final Map<Identifier, ItemMaskEntry> ITEM_LOCKS = new HashMap<>();
    private static final Map<Identifier, BlockMaskEntry> BLOCK_LOCKS = new HashMap<>();
    private static final Map<String, StageDefinition> STAGES = new HashMap<>();

    public static void clear() {
        RECIPE_LOCKS.clear();
        ITEM_LOCKS.clear();
        BLOCK_LOCKS.clear();
        STAGES.clear();
    }

    public static void addDefinition(StageDefinition definition) {
        String stage = definition.stage();

        STAGES.put(stage, definition);

        for (Identifier recipe : definition.recipes()) {
            RECIPE_LOCKS.put(recipe, stage);
        }

        for (Identifier item : definition.items()) {
            ITEM_LOCKS.put(item, new ItemMaskEntry(item, stage));
        }

        for (BlockMaskEntry block : definition.blocks()) {
            BLOCK_LOCKS.put(block.block(), block);
        }
    }

    public static String getRequiredStageForRecipe(Identifier recipe) {
        return RECIPE_LOCKS.get(recipe);
    }

    public static String getRequiredStageForItem(Identifier item) {
        ItemMaskEntry entry = ITEM_LOCKS.get(item);
        return entry == null ? null : entry.stage();
    }

    public static String getDisplayName(String stageId) {
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

    public static String getRequiredStageForBlock(Identifier block) {
        BlockMaskEntry entry = BLOCK_LOCKS.get(block);

        if (entry == null) {
            return null;
        }

        for (Map.Entry<String, StageDefinition> entrySet : STAGES.entrySet()) {
            StageDefinition definition = entrySet.getValue();

            for (BlockMaskEntry blockEntry : definition.blocks()) {
                if (blockEntry.block().equals(block)) {
                    return definition.stage();
                }
            }
        }

        return null;
    }

    
}