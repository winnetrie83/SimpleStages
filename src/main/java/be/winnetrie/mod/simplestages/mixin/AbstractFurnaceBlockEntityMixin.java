package be.winnetrie.mod.simplestages.mixin;

import be.winnetrie.mod.simplestages.recipe.FurnaceRecipeStageHelper;
import be.winnetrie.mod.simplestages.recipe.FurnaceStageVisibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Makes staged furnace-family recipes behave as if they do not exist for the
 * current player stage view.
 *
 * We deliberately hook the machine's cooking decision instead of inventory
 * slots. Input and fuel can be inserted normally. If the matching recipe is
 * hidden, vanilla's canBurn path simply returns false: no cook progress, no
 * output, no rollback and therefore no inventory desync/duplication path.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin implements FurnaceStageVisibility {

    @Unique
    private Set<String> simplestages$visibleStages = Set.of();

    /**
     * canBurn is static and no longer receives the block entity in 26.2. Keep a
     * tick-local reference only while serverTick is evaluating this furnace.
     */
    @Unique
    private static final ThreadLocal<AbstractFurnaceBlockEntity> SIMPLESTAGES$ACTIVE_FURNACE =
            new ThreadLocal<>();

    @Override
    public void simplestages$setVisibleStages(Collection<String> stages) {
        if (stages == null || stages.isEmpty()) {
            this.simplestages$visibleStages = Set.of();
        } else {
            this.simplestages$visibleStages = Set.copyOf(new HashSet<>(stages));
        }
    }

    @Override
    public boolean simplestages$hasVisibleStage(String stage) {
        // Mixin instance-field initializers are not a safe lifecycle guarantee
        // for already-constructed target instances. A freshly loaded furnace can
        // therefore reach its first server tick before a player stage snapshot
        // has ever been attached. Treat that state as an empty stage snapshot.
        return stage != null
                && this.simplestages$visibleStages != null
                && this.simplestages$visibleStages.contains(stage);
    }

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void simplestages$beginFurnaceTick(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            AbstractFurnaceBlockEntity entity,
            CallbackInfo ci
    ) {
        SIMPLESTAGES$ACTIVE_FURNACE.set(entity);
    }

    @Inject(method = "serverTick", at = @At("RETURN"))
    private static void simplestages$endFurnaceTick(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            AbstractFurnaceBlockEntity entity,
            CallbackInfo ci
    ) {
        SIMPLESTAGES$ACTIVE_FURNACE.remove();
    }

    @Inject(method = "canBurn", at = @At("HEAD"), cancellable = true)
    private static void simplestages$hideLockedCookingRecipe(
            NonNullList<ItemStack> items,
            int maxStackSize,
            ItemStack burnResult,
            CallbackInfoReturnable<Boolean> cir
    ) {
        AbstractFurnaceBlockEntity furnace = SIMPLESTAGES$ACTIVE_FURNACE.get();
        if (furnace == null) {
            return;
        }

        if (!FurnaceRecipeStageHelper.isCurrentRecipeAvailable(furnace)) {
            cir.setReturnValue(false);
        }
    }
}
