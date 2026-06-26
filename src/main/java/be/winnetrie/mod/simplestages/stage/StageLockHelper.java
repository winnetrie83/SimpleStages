package be.winnetrie.mod.simplestages.stage;

import be.winnetrie.mod.simplestages.stage.data.StageDefinitionManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class StageLockHelper {

    public static boolean isLocked(Player player, ItemStack stack) {
        if (player == null || stack.isEmpty()) {
            return false;
        }

        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String requiredStage = StageDefinitionManager.getRequiredStageForItem(itemId);

        if (requiredStage == null) {
            return false;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            return !StageManager.hasStage(serverPlayer, requiredStage);
        }

        return !ClientStageCache.hasStage(requiredStage);
    }

    public static String getRequiredStage(ItemStack stack) {
        if (stack.isEmpty()) {
            return "";
        }

        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String requiredStage = StageDefinitionManager.getRequiredStageForItem(itemId);

        if (requiredStage == null) {
            return "";
        }

        return StageDefinitionManager.getDisplayName(requiredStage);
    }

    public static boolean isBlockLocked(Player player, Identifier blockId) {

        if (player == null || blockId == null) {
            return false;
        }
        
        String requiredStage = StageDefinitionManager.getRequiredStageForBlock(blockId);

        if (requiredStage == null) {
            return false;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            return !StageManager.hasStage(serverPlayer, requiredStage);
        }

        return !ClientStageCache.hasStage(requiredStage);
    }

    public static String getRequiredStageForBlock(Identifier blockId) {
        String requiredStage = StageDefinitionManager.getRequiredStageForBlock(blockId);

        if (requiredStage == null) {
            return "";
        }

        return StageDefinitionManager.getDisplayName(requiredStage);
    }
}