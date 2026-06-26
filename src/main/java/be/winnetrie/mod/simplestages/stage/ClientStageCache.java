package be.winnetrie.mod.simplestages.stage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClientStageCache {

    private static final Set<String> STAGES = new HashSet<>();

    public static void setStages(List<String> stages) {
        STAGES.clear();
        STAGES.addAll(stages);
    }

    public static boolean hasStage(String stage) {
        return STAGES.contains(stage);
    }

    public static void clear() {
        STAGES.clear();
    }
}