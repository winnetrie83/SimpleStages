package be.winnetrie.mod.simplestages.recipe;

import be.winnetrie.mod.simplestages.mixin.AbstractFurnaceBlockEntityAccessor;
import be.winnetrie.mod.simplestages.stage.StageManager;
import be.winnetrie.mod.simplestages.stage.data.StageDefinitionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

/**
 * Recipe-visibility helper for furnace, blast furnace and smoker.
 *
 * Unlike the old dev3.6 approach, ingredients are never rejected and output
 * pickup is never cancelled. The machine is allowed to contain input + fuel,
 * but a staged cooking recipe is treated as unavailable until the player using
 * the menu has the required stage. Vanilla therefore simply cannot cook it.
 */
public final class FurnaceRecipeStageHelper {
    private FurnaceRecipeStageHelper() {
    }

    public static void refreshForPlayer(ServerPlayer player, AbstractFurnaceBlockEntity furnace) {
        if (player == null || furnace == null) {
            return;
        }

        ((FurnaceStageVisibility) (Object) furnace)
                .simplestages$setVisibleStages(StageManager.getStages(player));
    }

    public static void refreshOpenFurnace(ServerPlayer player) {
        if (player == null || !(player.containerMenu instanceof AbstractFurnaceMenu menu)) {
            return;
        }
        if (menu.slots.isEmpty()) {
            return;
        }

        var inputSlot = menu.getSlot(0);
        if (inputSlot.container instanceof AbstractFurnaceBlockEntity furnace) {
            refreshForPlayer(player, furnace);
        }
    }

    /**
     * Returns whether the recipe matching the machine's current input is visible
     * to the stage snapshot currently attached to the machine.
     *
     * Unstaged recipes are always available. A staged recipe with no player
     * snapshot is hidden, which also makes a freshly-loaded machine safe until a
     * player actually uses it.
     */
    public static boolean isCurrentRecipeAvailable(AbstractFurnaceBlockEntity furnace) {
        if (furnace == null) {
            return true;
        }
        if (!(furnace.getLevel() instanceof ServerLevel level)) {
            return true;
        }

        ItemStack inputStack = furnace.getItem(0);
        if (inputStack.isEmpty()) {
            return true;
        }

        RecipeType<? extends AbstractCookingRecipe> recipeType =
                ((AbstractFurnaceBlockEntityAccessor) (Object) furnace).simplestages$getRecipeType();

        var recipe = level.getServer().getRecipeManager().getRecipeFor(
                recipeType,
                new SingleRecipeInput(inputStack),
                level
        );

        if (recipe.isEmpty()) {
            return true;
        }

        var recipeId = recipe.get().id().identifier();
        String requiredStage = StageDefinitionManager.getRequiredStageForRecipe(recipeId);
        if (requiredStage == null) {
            return true;
        }

        return ((FurnaceStageVisibility) (Object) furnace)
                .simplestages$hasVisibleStage(requiredStage);
    }

}
