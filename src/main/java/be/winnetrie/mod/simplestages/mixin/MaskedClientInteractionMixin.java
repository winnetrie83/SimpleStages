package be.winnetrie.mod.simplestages.mixin;

import be.winnetrie.mod.simplestages.stage.MaskedBlockInteractionContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client counterpart of the server interaction context. In particular this makes
 * the mining state used by hit sounds and terrain particles be the mask state,
 * matching what the locked player sees.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MaskedClientInteractionMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "performUseItemOn", at = @At("HEAD"))
    private void simplestages$beginPerformUseItemOn(
            LocalPlayer player,
            InteractionHand hand,
            BlockHitResult blockHit,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        push(player, blockHit.getBlockPos());
    }

    @Inject(method = "performUseItemOn", at = @At("RETURN"))
    private void simplestages$endPerformUseItemOn(
            LocalPlayer player,
            InteractionHand hand,
            BlockHitResult blockHit,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        MaskedBlockInteractionContext.pop();
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"))
    private void simplestages$beginStartDestroyBlock(
            BlockPos pos,
            Direction direction,
            CallbackInfoReturnable<Boolean> cir
    ) {
        push(this.minecraft.player, pos);
    }

    @Inject(method = "startDestroyBlock", at = @At("RETURN"))
    private void simplestages$endStartDestroyBlock(
            BlockPos pos,
            Direction direction,
            CallbackInfoReturnable<Boolean> cir
    ) {
        MaskedBlockInteractionContext.pop();
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"))
    private void simplestages$beginContinueDestroyBlock(
            BlockPos pos,
            Direction direction,
            CallbackInfoReturnable<Boolean> cir
    ) {
        push(this.minecraft.player, pos);
    }

    @Inject(method = "continueDestroyBlock", at = @At("RETURN"))
    private void simplestages$endContinueDestroyBlock(
            BlockPos pos,
            Direction direction,
            CallbackInfoReturnable<Boolean> cir
    ) {
        MaskedBlockInteractionContext.pop();
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void simplestages$beginDestroyBlock(
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        push(this.minecraft.player, pos);
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void simplestages$endDestroyBlock(
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        MaskedBlockInteractionContext.pop();
    }

    private void push(LocalPlayer player, BlockPos pos) {
        if (player == null || this.minecraft.level == null) {
            MaskedBlockInteractionContext.push(player, pos, null);
            return;
        }
        BlockState realState = this.minecraft.level.getBlockState(pos);
        MaskedBlockInteractionContext.push(player, pos, realState);
    }
}
