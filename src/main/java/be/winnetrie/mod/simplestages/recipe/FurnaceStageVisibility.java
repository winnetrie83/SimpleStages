package be.winnetrie.mod.simplestages.recipe;

import java.util.Collection;

/**
 * Per-machine snapshot of the stages visible to the player currently using a
 * furnace-family menu.
 *
 * This is deliberately NOT ownership. No UUID is stored and the snapshot is
 * not persisted. It only gives an autonomous furnace tick enough context to
 * decide whether a staged cooking recipe should currently exist for normal
 * player-driven use.
 */
public interface FurnaceStageVisibility {
    void simplestages$setVisibleStages(Collection<String> stages);

    boolean simplestages$hasVisibleStage(String stage);
}
