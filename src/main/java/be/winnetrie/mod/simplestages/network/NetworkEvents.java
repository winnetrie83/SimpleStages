package be.winnetrie.mod.simplestages.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NetworkEvents {
    private static final String NETWORK_VERSION = "stage_editor_4";

    private NetworkEvents() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToClient(
                SyncStagesPayload.TYPE,
                SyncStagesPayload.STREAM_CODEC,
                SyncStagesPayload::handle
        );
        registrar.playToClient(
                SyncStageDefinitionsPayload.TYPE,
                SyncStageDefinitionsPayload.STREAM_CODEC,
                SyncStageDefinitionsPayload::handle
        );
        registrar.playToClient(
                SyncStonecutterRecipesPayload.TYPE,
                SyncStonecutterRecipesPayload.STREAM_CODEC,
                SyncStonecutterRecipesPayload::handle
        );
        StageEditorNetwork.register(registrar);
    }
}
