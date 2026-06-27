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

    public static String getRequiredStageRaw(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return StageDefinitionManager.getRequiredStageForItem(itemId);
    }

    public static String getItemUseMessage(ItemStack stack) {
        String requiredStage = getRequiredStageRaw(stack);

        if (requiredStage == null) {
            return "You have not identified this item yet.";
        }

        String msg = StageDefinitionManager.getMessage(requiredStage, "item_use");

        if (msg == null || msg.isBlank()) {
            return "You need " + StageDefinitionManager.getDisplayName(requiredStage) + " to use this item.";
        }

        return msg
                .replace("{stage}", StageDefinitionManager.getDisplayName(requiredStage))
                .replace("{item}", stack.getHoverName().getString());
    }

    public static boolean canEnterDimension(ServerPlayer player, Identifier dimensionId) {
        String requiredStage = StageDefinitionManager.getRequiredStageForDimension(dimensionId);

        if (requiredStage == null) {
            return true;
        }

        return StageManager.hasStage(player, requiredStage);
    }

    public static String getDimensionMessage(String stage) {
        String msg = StageDefinitionManager.getMessage(stage, "dimension");

        if (msg == null || msg.isBlank()) {
            return "You need " + StageDefinitionManager.getDisplayName(stage) + " to enter this dimension.";
        }

        return msg.replace("{stage}", StageDefinitionManager.getDisplayName(stage));
    }

    public static String getBlockUseMessage(Identifier blockId) {
        String requiredStage = StageDefinitionManager.getRequiredStageForBlock(blockId);

        if (requiredStage == null) {
            return "You cannot use this block yet.";
        }

        String msg = StageDefinitionManager.getMessage(requiredStage, "block_use");

        if (msg == null || msg.isBlank()) {
            return "You need " + StageDefinitionManager.getDisplayName(requiredStage) + " to use this block.";
        }

        return msg
            .replace("{stage}", StageDefinitionManager.getDisplayName(requiredStage))
            .replace("{block}", blockId.toString());
    }

    public static boolean isMobLocked(Player player, Identifier mobId) {
        if (player == null || mobId == null) {
            return false;
        }

        String requiredStage = StageDefinitionManager.getRequiredStageForMob(mobId);

        if (requiredStage == null) {
            return false;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            return !StageManager.hasStage(serverPlayer, requiredStage);
        }

        return !ClientStageCache.hasStage(requiredStage);
    }

    public static boolean canMobExistNearPlayer(ServerPlayer player, Identifier mobId) {
        String requiredStage = StageDefinitionManager.getRequiredStageForMob(mobId);

        if (requiredStage == null) {
            return true;
        }

        return StageManager.hasStage(player, requiredStage);
    }

    public static String getMobUseMessage(Identifier mobId) {
        String requiredStage = StageDefinitionManager.getRequiredStageForMob(mobId);

        if (requiredStage == null) {
            return "You cannot interact with this mob yet.";
        }

        String msg = StageDefinitionManager.getMessage(requiredStage, "mob_use");

        if (msg == null || msg.isBlank()) {
            return "You need " + StageDefinitionManager.getDisplayName(requiredStage) + " to interact with this mob.";
        }

        return msg
            .replace("{stage}", StageDefinitionManager.getDisplayName(requiredStage))
            .replace("{mob}", mobId.toString());
    }
}