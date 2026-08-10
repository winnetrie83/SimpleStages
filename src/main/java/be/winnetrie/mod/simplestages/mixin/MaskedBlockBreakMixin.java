package be.winnetrie.mod.simplestages.mixin;

import be.winnetrie.mod.simplestages.stage.BlockMaskHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Replaces the local BlockState used by the vanilla server break pipeline with
 * the player's active mask state.
 *
 * <p>The actual block position is still removed from the real world, but tool
 * harvesting, break callbacks, loot and experience are evaluated as the mask.
 * This is what makes (for example) hidden iron ore genuinely behave and drop as
 * stone for a player who has not unlocked it.</p>
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class MaskedBlockBreakMixin {

    @Shadow
    protected ServerPlayer player;

    @ModifyVariable(method = "destroyBlock", at = @At("STORE"), ordinal = 0)
    private BlockState simplestages$useMaskStateForBreak(BlockState realState) {
        return BlockMaskHelper.getEffectiveState(this.player, realState);
    }
}
