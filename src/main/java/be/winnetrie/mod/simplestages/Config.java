package be.winnetrie.mod.simplestages;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_DEBUG_LOGGING = BUILDER
            .comment("Enable debug logging")
            .define("enableDebugLogging", false);

    

    public static final ModConfigSpec SPEC = BUILDER.build();
}