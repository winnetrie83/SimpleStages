package be.winnetrie.mod.simplestages.network;


import net.neoforged.bus.api.SubscribeEvent;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;


public class NetworkEvents {

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToClient(
                        SyncStagesPayload.TYPE,
                        SyncStagesPayload.STREAM_CODEC,
                        SyncStagesPayload::handle
                );
    }
}