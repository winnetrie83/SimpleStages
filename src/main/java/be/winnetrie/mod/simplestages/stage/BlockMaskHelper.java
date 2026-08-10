package be.winnetrie.mod.simplestages.stage;

import be.winnetrie.mod.simplestages.stage.data.BlockMaskEntry;
import be.winnetrie.mod.simplestages.stage.data.StageDefinitionManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Resolves the block state a particular player is allowed to perceive and
 * interact with.
 *
 * <p>A configured block mask is deliberately more than a visual replacement:
 * while the underlying block is locked for the player, the mask becomes that
 * player's effective block for rendering, mining speed/tool rules and the
 * server-side break/drop pipeline.</p>
 */
public final class BlockMaskHelper {
    private BlockMaskHelper() {
    }

    /**
     * Returns the configured mask state when the real block is currently locked
     * for this player. Otherwise returns {@code realState} unchanged.
     */
    public static BlockState getEffectiveState(Player player, BlockState realState) {
        if (player == null || realState == null) {
            return realState;
        }

        Identifier blockId = BuiltInRegistries.BLOCK.getKey(realState.getBlock());
        if (!StageLockHelper.isBlockLocked(player, blockId)) {
            return realState;
        }

        BlockMaskEntry entry = StageDefinitionManager.getBlockMask(blockId);
        if (entry == null || entry.mask().isEmpty()) {
            return realState;
        }

        Block maskBlock = BuiltInRegistries.BLOCK.getValue(entry.mask().get());
        if (maskBlock == null || maskBlock == realState.getBlock()) {
            return realState;
        }

        return maskBlock.defaultBlockState();
    }

    public static boolean hasActiveMask(Player player, BlockState realState) {
        BlockState effective = getEffectiveState(player, realState);
        return effective != null && effective != realState && effective.getBlock() != realState.getBlock();
    }
}
