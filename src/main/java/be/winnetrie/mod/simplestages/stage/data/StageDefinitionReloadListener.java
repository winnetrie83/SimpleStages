package be.winnetrie.mod.simplestages.stage.data;

import be.winnetrie.mod.simplestages.SimpleStages;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

public class StageDefinitionReloadListener extends SimpleJsonResourceReloadListener<StageDefinition> {

    public StageDefinitionReloadListener() {
        super(
                StageDefinition.CODEC,
                FileToIdConverter.json("stages")
        );
    }

    @Override
    protected void apply(
            Map<Identifier, StageDefinition> definitions,
            net.minecraft.server.packs.resources.ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        StageDefinitionManager.clear();

        definitions.forEach((id, definition) -> {
            StageDefinitionManager.addDefinition(definition);
            SimpleStages.LOGGER.info(
                    "Loaded Simple Stages definition '{}' for stage '{}'",
                    id,
                    definition.stage()
            );
        });

        SimpleStages.LOGGER.info("Loaded {} Simple Stages definitions", definitions.size());
    }
}