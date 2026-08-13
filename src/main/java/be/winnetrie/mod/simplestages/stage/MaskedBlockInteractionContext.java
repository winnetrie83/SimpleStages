package be.winnetrie.mod.simplestages.stage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Temporary, thread-local view used while a player interacts with a masked block.
 *
 * <p>The real world state is never replaced just to create the disguise. Reads of
 * the clicked position are redirected to the configured mask only while the
 * original real state is still present. If vanilla or another mod genuinely
 * changes the block (for example dirt -> farmland/path/mud), the new real state is
 * returned immediately and therefore remains permanent after the interaction.</p>
 */
public final class MaskedBlockInteractionContext {
    private static final ThreadLocal<Deque<Frame>> STACK =
            ThreadLocal.withInitial(ArrayDeque::new);

    private MaskedBlockInteractionContext() {
    }

    /**
     * Pushes one interaction frame. A frame is always pushed, even when no mask is
     * active, so nested interaction methods can pair every {@link #push} with one
     * {@link #pop} safely.
     */
    public static void push(Player player, BlockPos pos, BlockState observedState) {
        BlockPos immutablePos = pos == null ? null : pos.immutable();
        BlockState effectiveState = BlockMaskHelper.getEffectiveState(player, observedState);
        boolean active = immutablePos != null
                && observedState != null
                && effectiveState != null
                && effectiveState != observedState
                && effectiveState.getBlock() != observedState.getBlock();

        STACK.get().push(new Frame(
                immutablePos,
                observedState,
                active ? effectiveState : null
        ));
    }

    /** Pops the most recent interaction frame. */
    public static void pop() {
        Deque<Frame> frames = STACK.get();
        if (!frames.isEmpty()) {
            frames.pop();
        }
        if (frames.isEmpty()) {
            STACK.remove();
        }
    }

    /**
     * Replaces an actual world read with the player's mask when a matching active
     * interaction frame exists.
     *
     * <p>The {@code actualState == frame.realState} check is intentional. As soon
     * as the interaction really writes another state at the position, that new
     * state must become visible to vanilla's own setBlock/notification pipeline
     * instead of being hidden behind the temporary mask.</p>
     */
    public static BlockState resolve(BlockPos pos, BlockState actualState) {
        if (pos == null || actualState == null) {
            return actualState;
        }

        Deque<Frame> frames = STACK.get();
        for (Frame frame : frames) {
            if (frame.effectiveState == null || frame.pos == null) {
                continue;
            }
            if (frame.pos.equals(pos) && actualState == frame.realState) {
                return frame.effectiveState;
            }
        }
        return actualState;
    }

    private record Frame(BlockPos pos, BlockState realState, BlockState effectiveState) {
    }
}
