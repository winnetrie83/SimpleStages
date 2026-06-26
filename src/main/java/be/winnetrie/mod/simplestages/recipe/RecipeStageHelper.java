package be.winnetrie.mod.simplestages.recipe;

import be.winnetrie.mod.simplestages.stage.StageManager;
import be.winnetrie.mod.simplestages.stage.data.StageDefinitionManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public class RecipeStageHelper {

    public static boolean canUseRecipe(ServerPlayer player, Identifier recipeId) {
        String requiredStage = StageDefinitionManager.getRequiredStageForRecipe(recipeId);

        if (requiredStage == null) {
            return true;
        }

        return StageManager.hasStage(player, requiredStage);
    }
}