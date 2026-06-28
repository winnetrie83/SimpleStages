# Simple Stages

Simple Stages is a lightweight, highly configurable progression system for Minecraft modpacks built for NeoForge.

It allows modpack creators to lock recipes, items, blocks, dimensions, mobs, structures, and more behind progression stages using simple JSON files.

---

# Features

* Recipe staging
* Item staging
* Block staging
* Dimension staging
* Mob staging
* Structure staging
* Loot staging
* Multiplayer support
* Custom messages
* Data-driven JSON configuration

---

# Supported Stage Systems

## Recipe Staging

Recipes can be hidden and locked until the required stage is unlocked.

Example:

* Lock iron pickaxe recipe until `iron_age`

---

## Item Staging

Items can be locked from usage.

Locked items:

* show as **Unidentified Item**
* display required stage in tooltip
* cannot be used until unlocked

Examples:

* tools
* weapons
* armor
* resources

---

## Block Staging

Blocks can be locked behind stages.

Players without the required stage:

* cannot mine staged blocks
* cannot interact with staged blocks

Examples:

* ores
* machines
* storage blocks

---

## Dimension Staging

Dimensions can be restricted.

Players without the required stage:

* cannot enter staged dimensions
* are teleported back if they somehow enter

Examples:

* Nether
* End
* modded dimensions

---

## Mob Staging

Mobs can be stage-locked.

Players without the required stage:

* cannot attack staged mobs
* cannot interact with staged mobs
* staged mobs cannot spawn near them

Each mob supports configurable spawn radius.

---

## Structure Staging

Structures can be locked behind progression stages.

Players without the required stage:

* cannot enter staged structures
* are teleported to the nearest safe location outside the structure

Each structure supports configurable buffer zones.

Examples:

* Villages
* Pillager Outposts
* Bastions
* Strongholds
* Modded structures

---

## Loot Staging

Staged items are removed from loot before loot generation for players who have not unlocked the required stage.

Works with:

* chests
* structures
* loot tables

---

# Stage Definitions

Stage definitions are JSON files placed in:

```text
data/<namespace>/stages/
```

Example:

```json
{
  "stage": "iron_age",
  "display_name": "Iron Age",

  "recipes": [
    "minecraft:iron_pickaxe"
  ],

  "items": [
    "minecraft:iron_pickaxe",
    "minecraft:diamond"
  ],

  "blocks": [
    {
      "block": "minecraft:iron_ore"
    }
  ],

  "dimensions": [
    "minecraft:the_nether"
  ],

  "mobs": [
    {
      "mob": "minecraft:blaze",
      "radius": 128
    }
  ],

  "structures": [
    {
      "structure": "minecraft:pillager_outpost",
      "buffer": 6
    },
    "minecraft:swamp_hut"
  ]
}
```

---

# JSON Fields

## stage

Unique stage identifier.

---

## display_name

Readable stage name shown to players.

---

## messages

Custom messages shown when stage restrictions apply.

Supported keys:

* item_use
* block_use
* dimension
* mob_use

---

## recipes

List of locked recipes.

---

## items

List of staged items.

---

## blocks

List of staged blocks.

---

## dimensions

List of staged dimensions.

---

## mobs

List of staged mobs.

Each mob supports:

* `mob`
* `radius`

Default radius:

```text
64 blocks
```

---

## structures

List of staged structures.

Supports both simple and advanced syntax.

Simple:

```json
"minecraft:swamp_hut"
```

Advanced:

```json
{
  "structure": "minecraft:pillager_outpost",
  "buffer": 6
}
```

Default structure buffer:

```text
3 blocks
```

---

# Multiplayer Support

Simple Stages fully supports multiplayer.

Stage definitions and player progression are synchronized between server and clients to ensure:

* proper tooltip rendering
* correct item masking
* consistent stage behavior

---

# Planned Features

* Trading staging
* Bartering staging
* Stage dependencies
* API for mod integration

---

# Requirements

* Minecraft 1.21.x
* NeoForge 26.2+
* Java 21+

---

# License

All Rights Reserved
