package be.winnetrie.mod.simplestages.recipe;

import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;

/**
 * Small common-side bridge implemented by the StonecutterMenu mixin.
 *
 * The server owns the authoritative filtered recipe list. The client receives
 * the same list (without RecipeHolder data, matching vanilla's stonecutter
 * recipe synchronization format) so visible button indexes cannot drift apart.
 */
public interface StonecutterMenuStageBridge {
    void simplestages$applyVisibleRecipes(SelectableRecipe.SingleInputSet<StonecutterRecipe> recipes);
}
