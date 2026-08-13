# Simple Stages

Simple Stages is a lightweight progression mod for Minecraft modpacks and servers running NeoForge.

Version **1.3.x** introduces a fully in-game, server-authoritative Stage Manager. Stage definitions no longer require datapacks: admins can create and edit progression directly while playing, then export the finished setup as modpack defaults.

## Version 1.3.1

Simple Stages 1.3.1 builds on the 1.3.0 release with fixes and improvements to behavioural block masks and furnace recipe staging.

### Behavioural block-mask improvements

Block masks now act as a temporary, player-relative effective block state during normal vanilla/NeoForge interaction pipelines.

Example:

```text
Real block: minecraft:stone
Mask:       minecraft:dirt
Stage:      not unlocked
```

For the locked player:

- mining uses the mask block's mining behaviour;
- hit sound and terrain feedback use the mask;
- right-click block/item logic reads the mask;
- a hoe can turn a dirt mask into real farmland;
- a shovel can turn a dirt mask into a real dirt path;
- dirt conversions such as mud can write their real resulting block;
- once an interaction genuinely changes the real block, the mask no longer hides the new result;
- interactions that only affect a neighbouring block, such as placing a torch against the masked block, do not materialize the mask into the world.

Existing mask mining-speed and server break/drop logic is retained as a compatibility/safety fallback.

### Furnace world-load crash fix

A furnace-family block entity can begin ticking before a player stage snapshot has been attached to it.

Version 1.3.1 treats a missing stage snapshot as an empty visible-stage set instead of allowing a null access. This prevents the world-load crash while keeping staged furnace recipes unavailable until the appropriate stage is visible.

### Current block-mask limitation

Behavioural masking virtualizes `BlockState` reads, not block entities.

A mask that itself requires a block entity — for example disguising an ordinary block as a chest — may require a separate virtual block-entity layer. Normal non-block-entity masks such as dirt, farmland, dirt path and mud are supported by the current behavioural system.

---

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

---

# Release notes

## Simple Stages 1.3.1

### Fixed

- Fixed a world-load crash caused by furnace-family machines ticking before their visible-stage snapshot had been initialized.
- Made furnace stage visibility null-safe by treating a missing snapshot as an empty visible-stage set.

### Improved

- Behavioural block masks now participate in normal block interaction logic, not only rendering/mining.
- Masked blocks can use mask-appropriate transformations such as dirt → farmland, dirt path or mud.
- Real block transformations remain permanent after they occur.
- Neighbour-only interactions such as placing a torch do not convert the hidden real block into its mask.

## Simple Stages 1.3.0

Simple Stages 1.3.0 was a major usability and progression update focused on making stage configuration practical directly in-game.

### Highlights

- Added a full OP/admin **Stage Manager GUI**.
- Datapack stage definitions are no longer required.
- Added searchable visual browsers for items, blocks, mobs, dimensions and structures.
- Added a result-first recipe browser: choose an item, then select the loaded recipes that create it.
- Added live create, edit, rename, duplicate and delete support for stages.
- Added world-local stage storage with modpack defaults through `defaultconfigs/simplestages/stages.json`.
- Added an export workflow so a tested in-game setup can be shipped directly in a modpack.
- Expanded recipe staging across crafting, stonecutting, smelting, blasting, smoking, campfire cooking and smithing.
- Stonecutter recipes hidden by a stage are removed from the visible recipe choices for that player.
- Furnace, Blast Furnace and Smoker cooking recipes behave as unavailable when locked instead of using output-slot rollback logic.
- Added behavioural block masks: hidden blocks can visually and mechanically act like another block for players who have not unlocked the stage, including mining behaviour and drops.
- Improved client/server synchronization for stage definitions and block masks.
- Added stronger server-side validation for editor changes.
- Removed obsolete datapack loader and experimental recipe-automation code.
- Removed development-only client logging and compatibility shims.

### Modpack workflow

Create and test stages in-game, then use:

```text
/stage definitions export-pack
```

The exported file can be included in:

```text
defaultconfigs/simplestages/stages.json
```

New worlds receive their own editable copy automatically.

---

# Technical notes

## Behavioural mask implementation

The 1.3.1 behavioural-mask work introduces a temporary player-relative interaction context.

The implementation adds:

- `stage/MaskedBlockInteractionContext.java`
- `mixin/MaskedLevelBlockStateMixin.java`
- `mixin/MaskedServerInteractionMixin.java`
- `mixin/MaskedClientInteractionMixin.java`

and updates:

- `src/main/resources/simplestages.mixins.json`

The existing `MaskedBlockStateMixin` and `MaskedBlockBreakMixin` are deliberately retained as compatibility/safety fallbacks.

## Runtime test used for 1.3.1

The behavioural-mask fix was tested with a stage that locks `minecraft:stone` and masks it as `minecraft:dirt`.

Expected behaviour:

1. Existing worlds load without the previous furnace NPE.
2. Masked stone uses dirt-like mining feedback and drops.
3. Right-clicking with a hoe can produce real farmland.
4. Right-clicking with a shovel can produce a real dirt path.
5. Dirt → mud conversions remain real mud.
6. Placing a torch against untouched masked stone does not alter the hidden stone.
7. Granting the stage reveals untouched stone, while already transformed farmland/path/mud remains transformed.

## Historical 1.3.0 release-cleanup notes

The 1.3.0 release cleanup was originally applied over the tested `1.3.0-dev3.8` source.

The cleanup process was:

1. Copy the patch contents into the project root and overwrite matching files.
2. Delete every path listed in `REMOVE-OLD-FILES.txt` if it still exists, optionally using `./DELETE-OBSOLETE.ps1`.
3. Optionally run `./VERIFY-CLEANUP.ps1` from PowerShell in the project root.
4. Run the normal Gradle build.
5. Launch a client and, ideally, a dedicated server once before release.

That cleanup intentionally did not change gameplay behaviour from the tested dev3.8 design. It removed obsolete development paths, temporary compatibility code/debug output, updated documentation/metadata, and set the release version to 1.3.0.

At that point:

- network protocol remained `stage_editor_4`;
- stage storage schema remained `1`.
