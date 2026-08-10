package be.winnetrie.mod.simplestages.mixin;

import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Read-only access to NeoForge's furnace recipe type. NeoForge stores this on
 * AbstractFurnaceBlockEntity so the same logic works for furnace, blast
 * furnace and smoker without guessing from the concrete block-entity class.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public interface AbstractFurnaceBlockEntityAccessor {

    @Accessor("recipeType")
    RecipeType<? extends AbstractCookingRecipe> simplestages$getRecipeType();
}
