package be.winnetrie.mod.simplestages.admin;

import be.winnetrie.mod.simplestages.stage.data.BlockMaskEntry;
import be.winnetrie.mod.simplestages.stage.data.MobStageEntry;
import be.winnetrie.mod.simplestages.stage.data.StageDefinition;
import be.winnetrie.mod.simplestages.stage.data.StageDefinitionManager;
import be.winnetrie.mod.simplestages.stage.data.StructureStageEntry;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Server-authoritative stage definitions sent to the OP/admin editor. */
public record StageEditorSnapshot(
        List<StageEntry> stages,
        String notice,
        boolean noticeError
) {
    private static final Gson GSON = new Gson();

    public StageEditorSnapshot {
        stages = stages == null ? List.of() : List.copyOf(stages);
        notice = notice == null ? "" : notice;
    }

    public static StageEditorSnapshot fromServer(String notice, boolean noticeError) {
        List<StageEntry> entries = StageDefinitionManager.getDefinitions().stream()
                .map(StageEntry::fromDefinition)
                .sorted(Comparator.comparing(StageEntry::stage))
                .toList();
        return new StageEditorSnapshot(entries, notice, noticeError);
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static StageEditorSnapshot fromJson(String json) {
        StageEditorSnapshot snapshot = GSON.fromJson(json, StageEditorSnapshot.class);
        if (snapshot == null) {
            throw new IllegalArgumentException("Missing stage editor snapshot");
        }
        List<StageEntry> entries = snapshot.stages == null
                ? List.of()
                : snapshot.stages.stream().map(StageEntry::normalized).toList();
        return new StageEditorSnapshot(entries, snapshot.notice, snapshot.noticeError);
    }

    public static StageEntry entryFromJson(JsonElement element) {
        StageEntry entry = GSON.fromJson(element, StageEntry.class);
        if (entry == null) {
            throw new IllegalArgumentException("Missing stage definition");
        }
        return entry.normalized();
    }

    public static JsonElement entryToJson(StageEntry entry) {
        return GSON.toJsonTree(entry.normalized());
    }

    public record StageEntry(
            String stage,
            String displayName,
            Map<String, String> messages,
            List<String> recipes,
            List<String> items,
            List<BlockEntry> blocks,
            List<String> dimensions,
            List<MobEntry> mobs,
            List<StructureEntry> structures
    ) {
        public StageEntry normalized() {
            Map<String, String> safeMessages = messages == null
                    ? Map.of()
                    : new LinkedHashMap<>(messages);
            return new StageEntry(
                    stage == null ? "" : stage,
                    displayName == null ? "" : displayName,
                    safeMessages,
                    recipes == null ? List.of() : List.copyOf(recipes),
                    items == null ? List.of() : List.copyOf(items),
                    blocks == null ? List.of() : blocks.stream().map(BlockEntry::normalized).toList(),
                    dimensions == null ? List.of() : List.copyOf(dimensions),
                    mobs == null ? List.of() : mobs.stream().map(MobEntry::normalized).toList(),
                    structures == null ? List.of() : structures.stream().map(StructureEntry::normalized).toList()
            );
        }

        public static StageEntry fromDefinition(StageDefinition definition) {
            List<BlockEntry> blockEntries = definition.blocks() == null ? List.of() : definition.blocks().stream()
                    .map(entry -> new BlockEntry(
                            entry.block().toString(),
                            entry.mask().map(Identifier::toString).orElse("")
                    ))
                    .toList();
            List<MobEntry> mobEntries = definition.mobs() == null ? List.of() : definition.mobs().stream()
                    .map(entry -> new MobEntry(entry.mob().toString(), entry.radius()))
                    .toList();
            List<StructureEntry> structureEntries = definition.structures() == null ? List.of() : definition.structures().stream()
                    .map(entry -> new StructureEntry(entry.structure().toString(), entry.bufferOrDefault()))
                    .toList();

            return new StageEntry(
                    definition.stage(),
                    definition.displayName() == null ? "" : definition.displayName(),
                    definition.messages() == null ? Map.of() : new LinkedHashMap<>(definition.messages()),
                    identifiers(definition.recipes()),
                    identifiers(definition.items()),
                    blockEntries,
                    identifiers(definition.dimensions()),
                    mobEntries,
                    structureEntries
            );
        }

        public StageDefinition toDefinition() {
            StageEntry safe = normalized();
            Map<String, String> cleanMessages = new LinkedHashMap<>();
            safe.messages.forEach((key, value) -> {
                if (key != null && !key.isBlank() && value != null && !value.isBlank()) {
                    cleanMessages.put(key, value);
                }
            });

            List<BlockMaskEntry> blockEntries = new ArrayList<>();
            for (BlockEntry block : safe.blocks) {
                Identifier blockId = parseId(block.block, "block");
                Optional<Identifier> mask = block.mask == null || block.mask.isBlank()
                        ? Optional.empty()
                        : Optional.of(parseId(block.mask, "block mask"));
                blockEntries.add(new BlockMaskEntry(blockId, mask));
            }

            List<MobStageEntry> mobEntries = new ArrayList<>();
            for (MobEntry mob : safe.mobs) {
                mobEntries.add(new MobStageEntry(parseId(mob.mob, "mob"), mob.radius));
            }

            List<StructureStageEntry> structureEntries = new ArrayList<>();
            for (StructureEntry structure : safe.structures) {
                structureEntries.add(new StructureStageEntry(
                        parseId(structure.structure, "structure"),
                        structure.buffer
                ));
            }

            return new StageDefinition(
                    safe.stage.trim(),
                    safe.displayName.trim(),
                    cleanMessages,
                    parseIds(safe.recipes, "recipe"),
                    parseIds(safe.items, "item"),
                    blockEntries,
                    parseIds(safe.dimensions, "dimension"),
                    mobEntries,
                    structureEntries
            );
        }

        private static List<String> identifiers(List<Identifier> identifiers) {
            return identifiers == null ? List.of() : identifiers.stream().map(Identifier::toString).toList();
        }

        private static List<Identifier> parseIds(List<String> values, String what) {
            List<Identifier> result = new ArrayList<>();
            if (values == null) {
                return result;
            }
            for (String value : values) {
                result.add(parseId(value, what));
            }
            return result;
        }

        private static Identifier parseId(String value, String what) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Missing " + what + " id");
            }
            try {
                return Identifier.parse(value.trim());
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("Invalid " + what + " id '" + value + "'", exception);
            }
        }
    }

    public record BlockEntry(String block, String mask) {
        public BlockEntry normalized() {
            return new BlockEntry(block == null ? "" : block, mask == null ? "" : mask);
        }
    }

    public record MobEntry(String mob, double radius) {
        public MobEntry normalized() {
            return new MobEntry(mob == null ? "" : mob, radius <= 0.0D ? 64.0D : radius);
        }
    }

    public record StructureEntry(String structure, int buffer) {
        public StructureEntry normalized() {
            return new StructureEntry(
                    structure == null ? "" : structure,
                    buffer <= 0 ? StructureStageEntry.DEFAULT_BUFFER : buffer
            );
        }
    }
}
