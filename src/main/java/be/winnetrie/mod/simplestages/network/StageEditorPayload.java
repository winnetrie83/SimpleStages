package be.winnetrie.mod.simplestages.network;

import be.winnetrie.mod.simplestages.SimpleStages;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record StageEditorPayload(String kind, String json) implements CustomPacketPayload {
    public static final Type<StageEditorPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SimpleStages.MODID, "stage_editor"));

    public static final StreamCodec<ByteBuf, StageEditorPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            StageEditorPayload::kind,
            ByteBufCodecs.STRING_UTF8,
            StageEditorPayload::json,
            StageEditorPayload::new
    );

    public static StageEditorPayload snapshot(String json) {
        return new StageEditorPayload("snapshot", json);
    }

    public static StageEditorPayload action(String json) {
        return new StageEditorPayload("action", json);
    }

    public static StageEditorPayload catalog(String json) {
        return new StageEditorPayload("catalog", json);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
