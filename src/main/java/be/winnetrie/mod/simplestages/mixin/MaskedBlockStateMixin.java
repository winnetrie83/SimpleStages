package be.winnetrie.mod.simplestages.mixin;

import be.winnetrie.mod.simplestages.stage.BlockMaskHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes a locked, masked block mine at the mask block's speed and with the mask
 * block's tool effectiveness for that player.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class MaskedBlockStateMixin {

    @Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
    private void simplestages$useMaskDestroyProgress(
            Player player,
            BlockGetter level,
            BlockPos pos,
            CallbackInfoReturnable<Float> cir
    ) {
        BlockState realState = (BlockState) (Object) this;
        BlockState effective = BlockMaskHelper.getEffectiveState(player, realState);
        if (effective == realState || effective.getBlock() == realState.getBlock()) {
            return;
        }

        cir.setReturnValue(effective.getDestroyProgress(player, level, pos));
    }
}
