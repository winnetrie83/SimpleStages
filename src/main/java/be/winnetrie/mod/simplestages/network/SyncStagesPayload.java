package be.winnetrie.mod.simplestages.network;

import be.winnetrie.mod.simplestages.SimpleStages;
import be.winnetrie.mod.simplestages.stage.ClientStageCache;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public record SyncStagesPayload(List<String> stages) implements CustomPacketPayload {

    public static final Type<SyncStagesPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SimpleStages.MODID, "sync_stages"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncStagesPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeVarInt(payload.stages.size());
                        for (String stage : payload.stages) {
                            buf.writeUtf(stage);
                        }
                    },
                    buf -> {
                        int size = buf.readVarInt();
                        List<String> stages = new ArrayList<>();

                        for (int i = 0; i < size; i++) {
                            stages.add(buf.readUtf());
                        }

                        return new SyncStagesPayload(stages);
                    }
            );

    public static void handle(SyncStagesPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> ClientStageCache.setStages(payload.stages()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}