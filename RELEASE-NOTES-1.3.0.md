# Simple Stages 1.3.0

Simple Stages 1.3.0 is a major usability and progression update focused on making stage configuration practical directly in-game.

## Highlights

- Added a full OP/admin **Stage Manager GUI**.
- Datapack stage definitions are no longer required.
- Added searchable visual browsers for items, blocks, mobs, dimensions and structures.
- Added a result-first recipe browser: choose an item, then select the loaded recipes that create it.
- Added live create, edit, rename, duplicate and delete support for stages.
- Added world-local stage storage with modpack defaults through `defaultconfigs/simplestages/stages.json`.
- Added an export workflow so a tested in-game setup can be shipped directly in a modpack.
- Expanded recipe staging across crafting, stonecutting, smelting, blasting, smoking, campfire cooking and smithing.
- Stonecutter recipes hidden by a stage are removed from the visible recipe choices for that player.
- Furnace, Blast Furnace and Smoker cooking recipes now behave as unavailable when locked instead of using output-slot rollback logic.
- Added behavioural block masks: hidden blocks can visually and mechanically act like another block for players who have not unlocked the stage, including mining behaviour and drops.
- Improved client/server synchronization for stage definitions and block masks.
- Added stronger server-side validation for editor changes.
- Removed obsolete datapack loader and experimental recipe-automation code.
- Removed development-only client logging and compatibility shims.

## Modpack workflow

Create and test stages in-game, then use:

```text
/stage definitions export-pack
```

The exported file can be included in:

```text
defaultconfigs/simplestages/stages.json
```

New worlds receive their own editable copy automatically.
