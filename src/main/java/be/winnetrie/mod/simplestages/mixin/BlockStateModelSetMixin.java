package be.winnetrie.mod.simplestages.mixin;

import be.winnetrie.mod.simplestages.stage.BlockMaskHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Player-relative block model substitution for Minecraft 26.2.
 *
 * <p>BlockRenderDispatcher was removed in 26.1. BlockStateModelSet is now the
 * lookup used to obtain a BlockStateModel for a BlockState.</p>
 */
@Mixin(targets = "net.minecraft.client.renderer.block.BlockStateModelSet")
public abstract class BlockStateModelSetMixin {

    @Unique
    private static final ThreadLocal<Boolean> SIMPLESTAGES_RESOLVING_MASK =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void simplestages$useMaskedBlockModel(
            BlockState state,
            CallbackInfoReturnable<BlockStateModel> cir
    ) {
        if (SIMPLESTAGES_RESOLVING_MASK.get()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || state == null) {
            return;
        }

        BlockState effective = BlockMaskHelper.getEffectiveState(minecraft.player, state);
        if (effective == state || effective.getBlock() == state.getBlock()) {
            return;
        }

        SIMPLESTAGES_RESOLVING_MASK.set(Boolean.TRUE);
        try {
            cir.setReturnValue(
                    minecraft.getModelManager()
                            .getBlockStateModelSet()
                            .get(effective)
            );
        } finally {
            SIMPLESTAGES_RESOLVING_MASK.set(Boolean.FALSE);
        }
    }
}
