package be.winnetrie.mod.simplestages.mixin;

import be.winnetrie.mod.simplestages.stage.MaskedBlockInteractionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Opens the mask interaction context before NeoForge fires block interaction and
 * break hooks. This makes the event pipeline, vanilla blocks/items and compatible
 * modded items read the mask state instead of the hidden real state.
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class MaskedServerInteractionMixin {
    @Shadow
    protected ServerPlayer player;

    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void simplestages$beginUseItemOn(
            ServerPlayer player,
            Level level,
            ItemStack itemStack,
            InteractionHand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        BlockPos pos = hitResult.getBlockPos();
        BlockState realState = level.getBlockState(pos);
        MaskedBlockInteractionContext.push(player, pos, realState);
    }

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void simplestages$endUseItemOn(
            ServerPlayer player,
            Level level,
            ItemStack itemStack,
            InteractionHand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        MaskedBlockInteractionContext.pop();
    }

    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"))
    private void simplestages$beginBlockBreakAction(
            BlockPos pos,
            ServerboundPlayerActionPacket.Action action,
            Direction direction,
            int maxY,
            int sequence,
            CallbackInfo ci
    ) {
        Level level = this.player.level();
        MaskedBlockInteractionContext.push(this.player, pos, level.getBlockState(pos));
    }

    @Inject(method = "handleBlockBreakAction", at = @At("RETURN"))
    private void simplestages$endBlockBreakAction(
            BlockPos pos,
            ServerboundPlayerActionPacket.Action action,
            Direction direction,
            int maxY,
            int sequence,
            CallbackInfo ci
    ) {
        MaskedBlockInteractionContext.pop();
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void simplestages$beginDestroyBlock(
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Level level = this.player.level();
        MaskedBlockInteractionContext.push(this.player, pos, level.getBlockState(pos));
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void simplestages$endDestroyBlock(
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        MaskedBlockInteractionContext.pop();
    }
}
