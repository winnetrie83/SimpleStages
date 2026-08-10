package be.winnetrie.mod.simplestages.admin;

import com.google.gson.Gson;

import java.util.List;

/** Small on-demand server catalog used by the visual stage pickers. */
public record StageEditorCatalog(
        String catalog,
        String context,
        List<Entry> entries,
        String notice,
        boolean noticeError
) {
    private static final Gson GSON = new Gson();

    public StageEditorCatalog {
        catalog = catalog == null ? "" : catalog;
        context = context == null ? "" : context;
        entries = entries == null ? List.of() : List.copyOf(entries);
        notice = notice == null ? "" : notice;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static StageEditorCatalog fromJson(String json) {
        StageEditorCatalog catalog = GSON.fromJson(json, StageEditorCatalog.class);
        if (catalog == null) {
            throw new IllegalArgumentException("Missing stage editor catalog");
        }
        List<Entry> entries = catalog.entries == null
                ? List.of()
                : catalog.entries.stream().map(Entry::normalized).toList();
        return new StageEditorCatalog(catalog.catalog, catalog.context, entries, catalog.notice, catalog.noticeError);
    }

    public record Entry(String id, String detail) {
        public Entry normalized() {
            return new Entry(id == null ? "" : id, detail == null ? "" : detail);
        }
    }
}
