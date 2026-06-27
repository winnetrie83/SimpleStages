package be.winnetrie.mod.simplestages.network;

import be.winnetrie.mod.simplestages.SimpleStages;
import be.winnetrie.mod.simplestages.stage.data.StageDefinitionManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public record SyncStageDefinitionsPayload(
        Map<String, String> itemStages,
        Map<String, String> displayNames
) implements CustomPacketPayload {

    public static final Type<SyncStageDefinitionsPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SimpleStages.MODID, "sync_stage_definitions"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncStageDefinitionsPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeVarInt(payload.itemStages.size());
                        payload.itemStages.forEach((item, stage) -> {
                            buf.writeUtf(item);
                            buf.writeUtf(stage);
                        });

                        buf.writeVarInt(payload.displayNames.size());
                        payload.displayNames.forEach((stage, displayName) -> {
                            buf.writeUtf(stage);
                            buf.writeUtf(displayName);
                        });
                    },
                    buf -> {
                        Map<String, String> itemStages = new HashMap<>();
                        int itemSize = buf.readVarInt();

                        for (int i = 0; i < itemSize; i++) {
                            itemStages.put(buf.readUtf(), buf.readUtf());
                        }

                        Map<String, String> displayNames = new HashMap<>();
                        int displaySize = buf.readVarInt();

                        for (int i = 0; i < displaySize; i++) {
                            displayNames.put(buf.readUtf(), buf.readUtf());
                        }

                        return new SyncStageDefinitionsPayload(itemStages, displayNames);
                    }
            );

    public static void handle(SyncStageDefinitionsPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() ->
                StageDefinitionManager.applyClientSync(payload.itemStages(), payload.displayNames())
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}