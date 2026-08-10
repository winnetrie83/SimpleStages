package be.winnetrie.mod.simplestages.network;

import be.winnetrie.mod.simplestages.SimpleStages;
import be.winnetrie.mod.simplestages.client.ClientBlockMaskRenderHelper;
import be.winnetrie.mod.simplestages.stage.data.StageDefinitionManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public record SyncStageDefinitionsPayload(
        Map<String, String> itemStages,
        Map<String, String> blockStages,
        Map<String, String> blockMasks,
        Map<String, String> displayNames
) implements CustomPacketPayload {

    public static final Type<SyncStageDefinitionsPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SimpleStages.MODID, "sync_stage_definitions"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncStageDefinitionsPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        writeStringMap(buf, payload.itemStages);
                        writeStringMap(buf, payload.blockStages);
                        writeStringMap(buf, payload.blockMasks);
                        writeStringMap(buf, payload.displayNames);
                    },
                    buf -> new SyncStageDefinitionsPayload(
                            readStringMap(buf),
                            readStringMap(buf),
                            readStringMap(buf),
                            readStringMap(buf)
                    )
            );

    private static void writeStringMap(RegistryFriendlyByteBuf buf, Map<String, String> map) {
        buf.writeVarInt(map.size());
        map.forEach((key, value) -> {
            buf.writeUtf(key);
            buf.writeUtf(value);
        });
    }

    private static Map<String, String> readStringMap(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<String, String> result = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            result.put(buf.readUtf(), buf.readUtf());
        }
        return result;
    }

    public static void handle(
            SyncStageDefinitionsPayload payload,
            net.neoforged.neoforge.network.handling.IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            StageDefinitionManager.applyClientSync(
                    payload.itemStages(),
                    payload.blockStages(),
                    payload.blockMasks(),
                    payload.displayNames()
            );
            ClientBlockMaskRenderHelper.refreshWorld();
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
