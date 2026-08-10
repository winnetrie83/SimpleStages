package be.winnetrie.mod.simplestages.stage;

import be.winnetrie.mod.simplestages.SimpleStages;
import be.winnetrie.mod.simplestages.network.SyncStageDefinitionsPayload;
import be.winnetrie.mod.simplestages.network.SyncStagesPayload;
import be.winnetrie.mod.simplestages.recipe.FurnaceRecipeStageHelper;
import be.winnetrie.mod.simplestages.stage.data.StageDefinitionManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class StageManager {

    private static final String DATA_KEY = SimpleStages.MODID + "_stages";

    private StageManager() {
    }

    public static boolean hasStage(ServerPlayer player, String stage) {
        return getStages(player).contains(stage);
    }

    public static boolean addStage(ServerPlayer player, String stage) {
        List<String> stages = getStages(player);
        if (stages.contains(stage)) {
            return false;
        }
        stages.add(stage);
        saveStages(player, stages);
        sync(player);
        return true;
    }

    public static boolean removeStage(ServerPlayer player, String stage) {
        List<String> stages = getStages(player);
        if (!stages.remove(stage)) {
            return false;
        }
        saveStages(player, stages);
        sync(player);
        return true;
    }

    public static List<String> getStages(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(DATA_KEY)) {
            return new ArrayList<>();
        }

        ListTag list = data.getList(DATA_KEY).orElse(new ListTag());
        List<String> stages = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            list.getString(i).ifPresent(stages::add);
        }
        return stages;
    }

    private static void saveStages(ServerPlayer player, List<String> stages) {
        ListTag list = new ListTag();
        for (String stage : stages) {
            list.add(StringTag.valueOf(stage));
        }
        player.getPersistentData().put(DATA_KEY, list);
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncStagesPayload(getStages(player)));

        // If the player currently has a furnace-family screen open, stage
        // grant/removal must affect the machine immediately without requiring
        // the player to move the ingredient out and back in.
        FurnaceRecipeStageHelper.refreshOpenFurnace(player);
    }

    public static void syncDefinitions(ServerPlayer player) {
        PacketDistributor.sendToPlayer(
                player,
                new SyncStageDefinitionsPayload(
                        StageDefinitionManager.getItemStagesForSync(),
                        StageDefinitionManager.getBlockStagesForSync(),
                        StageDefinitionManager.getBlockMasksForSync(),
                        StageDefinitionManager.getDisplayNamesForSync()
                )
        );
    }
}
