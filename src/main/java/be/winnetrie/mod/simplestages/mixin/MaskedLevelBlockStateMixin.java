package be.winnetrie.mod.simplestages.mixin;

import be.winnetrie.mod.simplestages.stage.MaskedBlockInteractionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Supplies the temporary player-relative mask state while a block interaction is
 * executing. Outside an interaction context Level#getBlockState is untouched.
 */
@Mixin(Level.class)
public abstract class MaskedLevelBlockStateMixin {
    @Inject(method = "getBlockState", at = @At("RETURN"), cancellable = true)
    private void simplestages$resolveMaskedInteractionState(
            BlockPos pos,
            CallbackInfoReturnable<BlockState> cir
    ) {
        BlockState actualState = cir.getReturnValue();
        BlockState effectiveState = MaskedBlockInteractionContext.resolve(pos, actualState);
        if (effectiveState != actualState) {
            cir.setReturnValue(effectiveState);
        }
    }
}
