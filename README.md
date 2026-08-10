# Simple Stages

Simple Stages is a lightweight progression mod for Minecraft modpacks and servers running NeoForge.

Version 1.3 introduces a fully in-game, server-authoritative Stage Manager. Stage definitions no longer require datapacks: admins can create and edit progression directly while playing, then export the finished setup as modpack defaults.

## Main features

- In-game stage editor for OP/admin players
- Recipe staging
- Item staging
- Block staging with optional behavioural masks
- Dimension staging
- Mob staging with configurable spawn radius
- Structure staging with configurable buffer
- Loot filtering for staged items
- Multiplayer synchronization
- Custom lock messages
- Modpack-friendly default configuration

## In-game Stage Manager

Open the editor with:

```text
/stage editor
```

The editor supports creating, renaming, duplicating and deleting stages. Each stage can configure recipes, items, blocks, dimensions, mobs, structures and messages.

Registry IDs do not normally need to be typed manually. The editor provides searchable browsers for the supported categories. Manual ID fields remain available as an advanced fallback.

### Recipe browser

Recipe selection follows a result-first workflow:

1. Open the Recipes tab and choose **Browse...**.
2. Pick an item from the searchable item catalog.
3. Simple Stages asks the server for loaded recipes that produce that item.
4. Select the recipes that should require the stage.
5. Save the stage.

This keeps the editor practical in large modpacks where recipe IDs are difficult to know by memory.

## Recipe staging

Simple Stages supports the main vanilla progression paths, including:

- crafting
- stonecutting
- smelting
- blasting
- smoking
- campfire cooking
- smithing

For furnace-family machines, a locked cooking recipe behaves as unavailable: input and fuel can be inserted, but the machine does not make progress until the recipe is available for the player's current stage view. This avoids output-slot rollback and inventory desynchronization.

Simple Stages is designed as a progression system, not as an anti-cheat layer for every possible automation mod. Modpacks that introduce hoppers, pipes, automated crafters or similar systems are encouraged to stage those progression tools as well.

## Item staging

Staged items cannot be normally used by players who do not have the required stage. Item staging is intentionally based on the registered item type, so component-based variants such as individual potion effects remain grouped under their base item category.

Staging both important recipes and their resulting items is a useful way to make progression more robust.

## Block staging and masks

Blocks can either be hard-locked or disguised behind another block.

Without a mask, a staged block cannot be normally mined or interacted with before its stage is unlocked.

With a mask, the hidden block behaves like the configured mask for the locked player. For example:

```text
Real block: minecraft:iron_ore
Mask:       minecraft:stone
Stage:      iron_age
```

A player without `iron_age`:

- sees Stone instead of Iron Ore;
- mines it using the mask block's mining behaviour;
- receives the mask block's drops;
- does not get a lock message that reveals the hidden block.

A player with `iron_age` sees and mines the real Iron Ore normally.

This allows ores and other progression resources to exist in already-generated chunks without revealing them before the intended stage.

## Other stage types

### Dimensions
Players can be prevented from entering dimensions until the required stage is unlocked.

### Mobs
Mobs can be locked from interaction/attack and can use a configurable player proximity radius for spawn restrictions.

### Structures
Structures can be restricted with a configurable buffer around their bounds.

### Loot
Staged items are filtered from supported loot generation for players who do not yet have the required stage.

## Player stage commands

```text
/stage add <player> <stage>
/stage remove <player> <stage>
/stage check <player> <stage>
/stage list <player>
```

Admin/editor utilities:

```text
/stage editor
/stage definitions list
/stage definitions reload
/stage definitions export-pack
```

Stage administration requires gamemaster/OP permission.

## Storage

Simple Stages owns its stage configuration directly. Datapack stage definitions are no longer used.

Each world stores its editable configuration at:

```text
<world>/serverconfig/simplestages/stages.json
```

The file is managed by the mod and the in-game editor; manual editing is optional rather than required.

## Shipping stages in a modpack

A modpack can provide defaults at:

```text
defaultconfigs/simplestages/stages.json
```

When a world has no Simple Stages configuration yet, the defaults are copied into that world's `serverconfig` folder. Existing worlds are not overwritten by later changes to the pack defaults.

Recommended workflow for a pack author:

1. Build and test stages in-game.
2. Use **Export pack defaults** in the Stage Manager or run `/stage definitions export-pack`.
3. Ship the generated `defaultconfigs/simplestages/stages.json` with the modpack.

## Requirements

- Minecraft 26.2
- NeoForge 26.2.x
- Java 25

## License

All Rights Reserved
