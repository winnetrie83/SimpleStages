package be.winnetrie.mod.simplestages.client.screen;

import be.winnetrie.mod.simplestages.admin.StageEditorSnapshot;
import be.winnetrie.mod.simplestages.network.StageEditorPayload;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * OP/admin Stage Manager. All writes are sent to and revalidated by the server.
 * The normal workflow uses visual browsers; raw namespaced-ID fields remain
 * available as an advanced fallback for unusual/modded entries.
 */
public final class StageEditorScreen extends Screen {
    private static final int PANEL_W = 700;
    private static final int PANEL_H = 390;
    private static final int LIST_W = 180;
    private static final int STAGE_ROWS = 16;
    private static final int ENTRY_ROWS = 11;
    private static final int ROW_H = 15;
    private static final Pattern STAGE_ID_PATTERN = Pattern.compile("[a-z0-9_.-]+");

    private StageEditorSnapshot snapshot;
    private MutableStage working;
    private String originalStageId;
    private String selectedStageId;
    private String pendingSelection;
    private boolean creatingNew;
    private boolean dirty;
    private boolean deleteArmed;

    private Tab tab = Tab.GENERAL;
    private int stagePage;
    private int entryPage;
    private int selectedEntry = -1;

    private EditBox stageSearch;
    private EditBox stageId;
    private EditBox displayName;
    private EditBox entryId;
    private EditBox entryAux;
    private EditBox itemUseMessage;
    private EditBox blockUseMessage;
    private EditBox dimensionMessage;
    private EditBox mobUseMessage;

    private String stageSearchText = "";
    private String localNotice = "";
    private boolean localNoticeError;

    public StageEditorScreen(StageEditorSnapshot snapshot) {
        super(Component.literal("Simple Stages Manager"));
        this.snapshot = snapshot;
        if (!snapshot.stages().isEmpty()) {
            load(snapshot.stages().get(0));
        } else {
            beginNewStage();
        }
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int top = panelTop();
        int x = left + LIST_W + 14;

        stageSearch = new EditBox(this.font, left + 8, top + 28, LIST_W - 16, 18, Component.literal("Stage search"));
        stageSearch.setMaxLength(100);
        stageSearch.setValue(stageSearchText);
        addRenderableWidget(stageSearch);

        if (working == null) {
            return;
        }

        switch (tab) {
            case GENERAL -> {
                stageId = new EditBox(this.font, x, top + 86, 300, 18, Component.literal("Stage id"));
                stageId.setMaxLength(96);
                stageId.setValue(working.stage);
                addRenderableWidget(stageId);

                displayName = new EditBox(this.font, x, top + 128, 360, 18, Component.literal("Display name"));
                displayName.setMaxLength(160);
                displayName.setValue(working.displayName);
                addRenderableWidget(displayName);
            }
            case MESSAGES -> {
                itemUseMessage = addMessageBox(x, top + 84, working.messages.getOrDefault("item_use", ""), "Item use message");
                blockUseMessage = addMessageBox(x, top + 132, working.messages.getOrDefault("block_use", ""), "Block use message");
                dimensionMessage = addMessageBox(x, top + 180, working.messages.getOrDefault("dimension", ""), "Dimension message");
                mobUseMessage = addMessageBox(x, top + 228, working.messages.getOrDefault("mob_use", ""), "Mob use message");
            }
            default -> {
                entryId = new EditBox(this.font, x, top + 274, 300, 18, Component.literal(entryLabel()));
                entryId.setMaxLength(180);
                addRenderableWidget(entryId);

                if (tab == Tab.BLOCKS || tab == Tab.MOBS || tab == Tab.STRUCTURES) {
                    entryAux = new EditBox(this.font, x + 308, top + 274, 120, 18, Component.literal(auxLabel()));
                    entryAux.setMaxLength(120);
                    addRenderableWidget(entryAux);
                }
            }
        }
    }

    private EditBox addMessageBox(int x, int y, String value, String hint) {
        EditBox box = new EditBox(this.font, x, y, 430, 18, Component.literal(hint));
        box.setMaxLength(512);
        box.setValue(value == null ? "" : value);
        addRenderableWidget(box);
        return box;
    }

    public void applyServerSnapshot(StageEditorSnapshot newSnapshot) {
        captureActiveFields();
        this.snapshot = newSnapshot;
        this.localNotice = "";
        this.deleteArmed = false;
        this.dirty = false;

        String keep = pendingSelection;
        pendingSelection = null;
        if (keep == null || keep.isBlank()) {
            keep = selectedStageId;
        }

        StageEditorSnapshot.StageEntry match = findStage(keep);
        if (match != null) {
            load(match);
        } else if (!newSnapshot.stages().isEmpty()) {
            load(newSnapshot.stages().get(0));
        } else {
            beginNewStage();
        }
        refreshWidgets();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int left = panelLeft();
        int top = panelTop();
        int right = left + Math.min(PANEL_W, this.width - 12);
        int bottom = top + Math.min(PANEL_H, this.height - 12);
        int x = left + LIST_W + 14;

        graphics.fill(0, 0, this.width, this.height, 0xB0000000);
        graphics.fill(left, top, right, bottom, 0xFF171717);
        graphics.outline(left, top, right - left, bottom - top, 0xFF6A6A6A);
        graphics.text(this.font, Component.literal("Simple Stages Manager"), left + 8, top + 8, 0xFFFFFFFF, false);
        graphics.text(this.font, Component.literal("Stages"), left + 8, top + 18, 0xFFBDBDBD, false);
        graphics.fill(left + LIST_W, top + 7, left + LIST_W + 1, bottom - 7, 0xFF454545);

        drawStageList(graphics, mouseX, mouseY, left, top);
        drawTabs(graphics, mouseX, mouseY, x, top);
        drawCurrentTab(graphics, mouseX, mouseY, x, top);
        drawGlobalActions(graphics, mouseX, mouseY, x, top);
        drawNotice(graphics, x, top, right);
    }

    private void drawStageList(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int left, int top) {
        List<StageEditorSnapshot.StageEntry> filtered = filteredStages();
        int maxPage = Math.max(0, (filtered.size() - 1) / STAGE_ROWS);
        stagePage = Math.min(stagePage, maxPage);
        int start = stagePage * STAGE_ROWS;
        int rowY = top + 50;
        for (int i = 0; i < STAGE_ROWS && start + i < filtered.size(); i++) {
            StageEditorSnapshot.StageEntry entry = filtered.get(start + i);
            int y = rowY + i * ROW_H;
            boolean selected = !creatingNew && entry.stage().equals(selectedStageId);
            boolean hovered = hit(mouseX, mouseY, left + 7, y, LIST_W - 14, ROW_H - 1);
            if (selected) {
                graphics.fill(left + 7, y, left + LIST_W - 7, y + ROW_H - 1, 0xFF3E5D7A);
            } else if (hovered) {
                graphics.fill(left + 7, y, left + LIST_W - 7, y + ROW_H - 1, 0xFF303030);
            }
            graphics.text(this.font, Component.literal(trim(entry.stage(), 26)), left + 10, y + 3, 0xFFE5E5E5, false);
        }

        drawButton(graphics, left + 7, top + 296, 78, 18, "New", true, mouseX, mouseY);
        drawButton(graphics, left + 92, top + 296, 80, 18, "Duplicate", selectedEntry() != null, mouseX, mouseY);
        drawButton(graphics, left + 7, top + 320, 78, 18,
                deleteArmed ? "Confirm" : "Delete", selectedEntry() != null, mouseX, mouseY,
                deleteArmed ? 0xFF713232 : 0xFF353535);
        drawButton(graphics, left + 92, top + 320, 80, 18, "Revert", selectedEntry() != null && dirty, mouseX, mouseY);

        drawButton(graphics, left + 7, top + 348, 28, 18, "<", stagePage > 0, mouseX, mouseY);
        graphics.text(this.font, Component.literal((stagePage + 1) + "/" + (maxPage + 1)), left + 72, top + 353, 0xFFBDBDBD, false);
        drawButton(graphics, left + 144, top + 348, 28, 18, ">", stagePage < maxPage, mouseX, mouseY);
    }

    private void drawTabs(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int top) {
        int tx = x;
        for (Tab candidate : Tab.values()) {
            int width = candidate.width;
            boolean active = candidate == tab;
            boolean hovered = hit(mouseX, mouseY, tx, top + 18, width, 20);
            int fill = active ? 0xFF3E5D7A : hovered ? 0xFF3A3A3A : 0xFF292929;
            graphics.fill(tx, top + 18, tx + width, top + 38, fill);
            graphics.outline(tx, top + 18, width, 20, active ? 0xFFFFFFFF : 0xFF666666);
            graphics.centeredText(this.font, Component.literal(candidate.label), tx + width / 2, top + 24, 0xFFFFFFFF);
            tx += width + 3;
        }
    }

    private void drawCurrentTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int top) {
        if (working == null) {
            graphics.text(this.font, Component.literal("No stage selected."), x, top + 64, 0xFFAAAAAA, false);
            return;
        }

        graphics.text(this.font,
                Component.literal(creatingNew ? "New stage" : "Editing: " + working.stage),
                x, top + 50, 0xFFFFFFFF, false);

        switch (tab) {
            case GENERAL -> drawGeneral(graphics, x, top);
            case MESSAGES -> drawMessages(graphics, x, top);
            default -> drawEntryEditor(graphics, mouseX, mouseY, x, top);
        }
    }

    private void drawGeneral(GuiGraphicsExtractor graphics, int x, int top) {
        graphics.text(this.font, Component.literal("Stage ID"), x, top + 74, 0xFFBDBDBD, false);
        graphics.text(this.font, Component.literal("Display name"), x, top + 116, 0xFFBDBDBD, false);
        graphics.textWithWordWrap(this.font,
                Component.literal("The stage ID is the internal progression key used by /stage add/remove. Display name is what players see."),
                x, top + 162, 430, 0xFF8F8F8F);
        if (creatingNew) {
            graphics.text(this.font, Component.literal("New stages are stored directly in this world's Simple Stages store."), x, top + 205, 0xFF8F8F8F, false);
        }
    }

    private void drawMessages(GuiGraphicsExtractor graphics, int x, int top) {
        graphics.text(this.font, Component.literal("Locked item use"), x, top + 72, 0xFFBDBDBD, false);
        graphics.text(this.font, Component.literal("Locked block use"), x, top + 120, 0xFFBDBDBD, false);
        graphics.text(this.font, Component.literal("Locked dimension"), x, top + 168, 0xFFBDBDBD, false);
        graphics.text(this.font, Component.literal("Locked mob interaction"), x, top + 216, 0xFFBDBDBD, false);
        graphics.textWithWordWrap(this.font,
                Component.literal("Placeholders: {stage}, plus {item}, {block} or {mob} where applicable. Leave blank for Simple Stages' default message."),
                x, top + 263, 430, 0xFF8F8F8F);
    }

    private void drawEntryEditor(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int top) {
        List<String> rows = displayRows();
        int maxPage = Math.max(0, (rows.size() - 1) / ENTRY_ROWS);
        entryPage = Math.min(entryPage, maxPage);
        int start = entryPage * ENTRY_ROWS;
        int rowY = top + 72;

        graphics.text(this.font, Component.literal(tab.description), x, top + 61, 0xFFBDBDBD, false);
        drawButton(graphics, x + 340, top + 42, 90, 18, "Browse...", true, mouseX, mouseY, 0xFF31506A);
        for (int i = 0; i < ENTRY_ROWS && start + i < rows.size(); i++) {
            int absoluteIndex = start + i;
            int y = rowY + i * ROW_H;
            boolean selected = absoluteIndex == selectedEntry;
            boolean hovered = hit(mouseX, mouseY, x, y, 430, ROW_H - 1);
            if (selected) {
                graphics.fill(x, y, x + 430, y + ROW_H - 1, 0xFF3E5D7A);
            } else if (hovered) {
                graphics.fill(x, y, x + 430, y + ROW_H - 1, 0xFF303030);
            }
            graphics.text(this.font, Component.literal(trim(rows.get(absoluteIndex), 65)), x + 4, y + 3, 0xFFE5E5E5, false);
        }

        drawButton(graphics, x, top + 243, 28, 18, "<", entryPage > 0, mouseX, mouseY);
        graphics.text(this.font, Component.literal((entryPage + 1) + "/" + (maxPage + 1)), x + 65, top + 248, 0xFFBDBDBD, false);
        drawButton(graphics, x + 118, top + 243, 28, 18, ">", entryPage < maxPage, mouseX, mouseY);
        graphics.text(this.font, Component.literal(entryLabel()), x, top + 263, 0xFFBDBDBD, false);
        if (entryAux != null) {
            graphics.text(this.font, Component.literal(auxLabel()), x + 308, top + 263, 0xFFBDBDBD, false);
        }
        drawButton(graphics, x, top + 300, 94, 18, selectedEntry >= 0 ? "Update" : "Add", canApplyEntry(), mouseX, mouseY);
        drawButton(graphics, x + 101, top + 300, 94, 18, "Clear", true, mouseX, mouseY);
        drawButton(graphics, x + 202, top + 300, 94, 18, "Remove", selectedEntry >= 0, mouseX, mouseY, 0xFF553333);

        String tip = switch (tab) {
            case BLOCKS -> "Mask is optional; it is the block players see instead of the locked block.";
            case MOBS -> "Radius controls how close an unstaged player may be before this mob spawn is blocked.";
            case STRUCTURES -> "Buffer is the extra protected distance around the structure.";
            default -> "Use Browse for the easy visual picker. Manual IDs remain available for advanced cases.";
        };
        graphics.textWithWordWrap(this.font, Component.literal(tip), x + 306, top + 301, 135, 0xFF888888);
    }

    private void drawGlobalActions(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int top) {
        drawButton(graphics, x + 326, top + 344, 104, 20, "Save stage", canSave(), mouseX, mouseY, 0xFF315C3A);
        drawButton(graphics, x, top + 344, 116, 20, "Reload from disk", true, mouseX, mouseY);
        drawButton(graphics, x + 123, top + 344, 128, 20, "Export pack defaults", true, mouseX, mouseY);

        if (dirty) {
            graphics.text(this.font, Component.literal("Unsaved changes"), x + 258, top + 350, 0xFFFFC56B, false);
        }
    }

    private void drawNotice(GuiGraphicsExtractor graphics, int x, int top, int right) {
        String notice = !localNotice.isBlank() ? localNotice : snapshot.notice();
        boolean error = !localNotice.isBlank() ? localNoticeError : snapshot.noticeError();
        if (!notice.isBlank()) {
            graphics.textWithWordWrap(this.font, Component.literal(notice), x, top + 369,
                    Math.max(120, right - x - 8), error ? 0xFFFF7070 : 0xFF80FF80);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        double mx = event.x();
        double my = event.y();
        int left = panelLeft();
        int top = panelTop();
        int x = left + LIST_W + 14;

        List<StageEditorSnapshot.StageEntry> stages = filteredStages();
        int stageStart = stagePage * STAGE_ROWS;
        int rowY = top + 50;
        for (int i = 0; i < STAGE_ROWS && stageStart + i < stages.size(); i++) {
            int y = rowY + i * ROW_H;
            if (hit(mx, my, left + 7, y, LIST_W - 14, ROW_H - 1)) {
                selectStage(stages.get(stageStart + i));
                return true;
            }
        }

        if (hit(mx, my, left + 7, top + 296, 78, 18)) {
            beginNewStage();
            refreshWidgets();
            return true;
        }
        if (hit(mx, my, left + 92, top + 296, 80, 18) && selectedEntry() != null) {
            duplicateSelected();
            refreshWidgets();
            return true;
        }
        if (hit(mx, my, left + 7, top + 320, 78, 18) && selectedEntry() != null) {
            if (!deleteArmed) {
                deleteArmed = true;
                localNotice = "Click Confirm to permanently delete stage '" + selectedStageId + "'.";
                localNoticeError = true;
            } else {
                sendDelete();
            }
            return true;
        }
        if (hit(mx, my, left + 92, top + 320, 80, 18) && selectedEntry() != null && dirty) {
            StageEditorSnapshot.StageEntry current = selectedEntry();
            if (current != null) {
                load(current);
                localNotice = "Reverted unsaved changes.";
                localNoticeError = false;
                refreshWidgets();
            }
            return true;
        }
        if (hit(mx, my, left + 7, top + 348, 28, 18) && stagePage > 0) {
            stagePage--;
            return true;
        }
        int maxStagePage = Math.max(0, (stages.size() - 1) / STAGE_ROWS);
        if (hit(mx, my, left + 144, top + 348, 28, 18) && stagePage < maxStagePage) {
            stagePage++;
            return true;
        }

        int tx = x;
        for (Tab candidate : Tab.values()) {
            if (hit(mx, my, tx, top + 18, candidate.width, 20)) {
                switchTab(candidate);
                return true;
            }
            tx += candidate.width + 3;
        }

        if (tab != Tab.GENERAL && tab != Tab.MESSAGES) {
            if (hit(mx, my, x + 340, top + 42, 90, 18)) {
                openPicker();
                return true;
            }
            List<String> rows = displayRows();
            int start = entryPage * ENTRY_ROWS;
            int entryRowY = top + 72;
            for (int i = 0; i < ENTRY_ROWS && start + i < rows.size(); i++) {
                int y = entryRowY + i * ROW_H;
                if (hit(mx, my, x, y, 430, ROW_H - 1)) {
                    selectListEntry(start + i);
                    return true;
                }
            }
            if (hit(mx, my, x, top + 243, 28, 18) && entryPage > 0) {
                entryPage--;
                return true;
            }
            int maxEntryPage = Math.max(0, (rows.size() - 1) / ENTRY_ROWS);
            if (hit(mx, my, x + 118, top + 243, 28, 18) && entryPage < maxEntryPage) {
                entryPage++;
                return true;
            }
            if (hit(mx, my, x, top + 300, 94, 18) && canApplyEntry()) {
                applyEntry();
                return true;
            }
            if (hit(mx, my, x + 101, top + 300, 94, 18)) {
                clearEntrySelection();
                return true;
            }
            if (hit(mx, my, x + 202, top + 300, 94, 18) && selectedEntry >= 0) {
                removeSelectedEntry();
                return true;
            }
        }

        if (hit(mx, my, x, top + 344, 116, 20)) {
            sendSimpleAction("reload");
            return true;
        }
        if (hit(mx, my, x + 123, top + 344, 128, 20)) {
            sendSimpleAction("export_pack");
            return true;
        }
        if (hit(mx, my, x + 326, top + 344, 104, 20) && canSave()) {
            sendSave();
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    private void switchTab(Tab newTab) {
        if (newTab == tab) {
            return;
        }
        captureActiveFields();
        tab = newTab;
        selectedEntry = -1;
        entryPage = 0;
        refreshWidgets();
    }

    private void selectStage(StageEditorSnapshot.StageEntry entry) {
        captureActiveFields();
        load(entry);
        localNotice = "";
        deleteArmed = false;
        tab = Tab.GENERAL;
        selectedEntry = -1;
        entryPage = 0;
        refreshWidgets();
    }

    private void load(StageEditorSnapshot.StageEntry entry) {
        this.working = MutableStage.from(entry);
        this.originalStageId = entry.stage();
        this.selectedStageId = entry.stage();
        this.creatingNew = false;
        this.dirty = false;
    }

    private void beginNewStage() {
        captureActiveFields();
        String id = nextFreeStageId("new_stage");
        this.working = MutableStage.empty(id);
        this.originalStageId = "";
        this.selectedStageId = null;
        this.creatingNew = true;
        this.dirty = true;
        this.deleteArmed = false;
        this.tab = Tab.GENERAL;
        this.selectedEntry = -1;
        this.entryPage = 0;
        this.localNotice = "Creating a new stage. Save when finished.";
        this.localNoticeError = false;
    }

    private void duplicateSelected() {
        StageEditorSnapshot.StageEntry entry = selectedEntry();
        if (entry == null) {
            return;
        }
        MutableStage copy = MutableStage.from(entry);
        copy.stage = nextFreeStageId(entry.stage() + "_copy");
        copy.displayName = entry.displayName().isBlank() ? "" : entry.displayName() + " Copy";
        this.working = copy;
        this.originalStageId = "";
        this.selectedStageId = null;
        this.creatingNew = true;
        this.dirty = true;
        this.deleteArmed = false;
        this.tab = Tab.GENERAL;
        this.selectedEntry = -1;
        this.entryPage = 0;
        this.localNotice = "Duplicated '" + entry.stage() + "'. Choose an ID and save it as a new stage.";
        this.localNoticeError = false;
    }

    private void captureActiveFields() {
        if (working == null) {
            return;
        }
        if (stageSearch != null) {
            stageSearchText = stageSearch.getValue();
        }
        if (tab == Tab.GENERAL && stageId != null && displayName != null) {
            String newId = stageId.getValue().trim();
            String newDisplay = displayName.getValue().trim();
            if (!newId.equals(working.stage) || !newDisplay.equals(working.displayName)) {
                dirty = true;
            }
            working.stage = newId;
            working.displayName = newDisplay;
        } else if (tab == Tab.MESSAGES && itemUseMessage != null) {
            dirty |= putMessage("item_use", itemUseMessage.getValue());
            dirty |= putMessage("block_use", blockUseMessage.getValue());
            dirty |= putMessage("dimension", dimensionMessage.getValue());
            dirty |= putMessage("mob_use", mobUseMessage.getValue());
        }
    }

    private boolean putMessage(String key, String value) {
        String clean = value == null ? "" : value.trim();
        String old = working.messages.getOrDefault(key, "");
        if (clean.isBlank()) {
            working.messages.remove(key);
        } else {
            working.messages.put(key, clean);
        }
        return !old.equals(clean);
    }

    private void selectListEntry(int index) {
        selectedEntry = index;
        if (entryId == null) {
            return;
        }
        switch (tab) {
            case RECIPES -> entryId.setValue(working.recipes.get(index));
            case ITEMS -> entryId.setValue(working.items.get(index));
            case DIMENSIONS -> entryId.setValue(working.dimensions.get(index));
            case BLOCKS -> {
                StageEditorSnapshot.BlockEntry entry = working.blocks.get(index);
                entryId.setValue(entry.block());
                entryAux.setValue(entry.mask());
            }
            case MOBS -> {
                StageEditorSnapshot.MobEntry entry = working.mobs.get(index);
                entryId.setValue(entry.mob());
                entryAux.setValue(Double.toString(entry.radius()));
            }
            case STRUCTURES -> {
                StageEditorSnapshot.StructureEntry entry = working.structures.get(index);
                entryId.setValue(entry.structure());
                entryAux.setValue(Integer.toString(entry.buffer()));
            }
            default -> {
            }
        }
    }

    private void clearEntrySelection() {
        selectedEntry = -1;
        if (entryId != null) {
            entryId.setValue("");
        }
        if (entryAux != null) {
            entryAux.setValue(tab == Tab.MOBS ? "64" : tab == Tab.STRUCTURES ? "3" : "");
        }
    }

    private void applyEntry() {
        try {
            String id = canonicalId(entryId.getValue());
            switch (tab) {
                case RECIPES -> setOrAdd(working.recipes, id);
                case ITEMS -> setOrAdd(working.items, id);
                case DIMENSIONS -> setOrAdd(working.dimensions, id);
                case BLOCKS -> {
                    String mask = entryAux.getValue().trim();
                    if (!mask.isBlank()) {
                        mask = canonicalId(mask);
                    }
                    setOrAddBlock(new StageEditorSnapshot.BlockEntry(id, mask));
                }
                case MOBS -> {
                    double radius = Double.parseDouble(entryAux.getValue().trim());
                    if (radius <= 0.0D) {
                        throw new IllegalArgumentException("Mob radius must be greater than 0.");
                    }
                    setOrAddMob(new StageEditorSnapshot.MobEntry(id, radius));
                }
                case STRUCTURES -> {
                    int buffer = Integer.parseInt(entryAux.getValue().trim());
                    if (buffer <= 0) {
                        throw new IllegalArgumentException("Structure buffer must be greater than 0.");
                    }
                    setOrAddStructure(new StageEditorSnapshot.StructureEntry(id, buffer));
                }
                default -> {
                    return;
                }
            }
            dirty = true;
            localNotice = selectedEntry >= 0 ? "Updated entry." : "Added entry.";
            localNoticeError = false;
            clearEntrySelection();
        } catch (RuntimeException exception) {
            localNotice = exception.getMessage() == null ? "Invalid entry." : exception.getMessage();
            localNoticeError = true;
        }
    }

    private void removeSelectedEntry() {
        if (selectedEntry < 0) {
            return;
        }
        switch (tab) {
            case RECIPES -> working.recipes.remove(selectedEntry);
            case ITEMS -> working.items.remove(selectedEntry);
            case BLOCKS -> working.blocks.remove(selectedEntry);
            case DIMENSIONS -> working.dimensions.remove(selectedEntry);
            case MOBS -> working.mobs.remove(selectedEntry);
            case STRUCTURES -> working.structures.remove(selectedEntry);
            default -> {
                return;
            }
        }
        dirty = true;
        clearEntrySelection();
        localNotice = "Removed entry. Save the stage to apply it.";
        localNoticeError = false;
    }

    private void setOrAdd(List<String> list, String value) {
        if (selectedEntry >= 0) {
            list.set(selectedEntry, value);
        } else if (!list.contains(value)) {
            list.add(value);
        } else {
            throw new IllegalArgumentException("That entry is already in this stage.");
        }
    }

    private void setOrAddBlock(StageEditorSnapshot.BlockEntry value) {
        if (selectedEntry >= 0) {
            working.blocks.set(selectedEntry, value);
        } else {
            ensureUnique(value.block(), working.blocks.stream().map(StageEditorSnapshot.BlockEntry::block).toList());
            working.blocks.add(value);
        }
    }

    private void setOrAddMob(StageEditorSnapshot.MobEntry value) {
        if (selectedEntry >= 0) {
            working.mobs.set(selectedEntry, value);
        } else {
            ensureUnique(value.mob(), working.mobs.stream().map(StageEditorSnapshot.MobEntry::mob).toList());
            working.mobs.add(value);
        }
    }

    private void setOrAddStructure(StageEditorSnapshot.StructureEntry value) {
        if (selectedEntry >= 0) {
            working.structures.set(selectedEntry, value);
        } else {
            ensureUnique(value.structure(), working.structures.stream().map(StageEditorSnapshot.StructureEntry::structure).toList());
            working.structures.add(value);
        }
    }

    private static void ensureUnique(String value, List<String> existing) {
        if (existing.contains(value)) {
            throw new IllegalArgumentException("That entry is already in this stage.");
        }
    }

    private boolean canApplyEntry() {
        if (entryId == null || entryId.getValue().isBlank()) {
            return false;
        }
        try {
            canonicalId(entryId.getValue());
            if (tab == Tab.BLOCKS && entryAux != null && !entryAux.getValue().isBlank()) {
                canonicalId(entryAux.getValue());
            } else if (tab == Tab.MOBS && entryAux != null) {
                return Double.parseDouble(entryAux.getValue().trim()) > 0.0D;
            } else if (tab == Tab.STRUCTURES && entryAux != null) {
                return Integer.parseInt(entryAux.getValue().trim()) > 0;
            }
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean canSave() {
        if (working == null) {
            return false;
        }
        String id = working.stage;
        if (tab == Tab.GENERAL && stageId != null) {
            id = stageId.getValue().trim();
        }
        return !id.isBlank() && STAGE_ID_PATTERN.matcher(id).matches();
    }

    private void sendSave() {
        captureActiveFields();
        try {
            validateWorking();
            StageEditorSnapshot.StageEntry entry = working.toEntry();
            JsonObject root = new JsonObject();
            root.addProperty("action", "save");
            root.addProperty("original_stage", originalStageId == null ? "" : originalStageId);
            root.add("stage", StageEditorSnapshot.entryToJson(entry));
            pendingSelection = entry.stage();
            ClientPacketDistributor.sendToServer(StageEditorPayload.action(root.toString()));
        } catch (RuntimeException exception) {
            localNotice = exception.getMessage() == null ? "Cannot save this stage." : exception.getMessage();
            localNoticeError = true;
        }
    }

    private void sendDelete() {
        if (selectedStageId == null) {
            return;
        }
        JsonObject root = new JsonObject();
        root.addProperty("action", "delete");
        root.addProperty("stage", selectedStageId);
        pendingSelection = null;
        ClientPacketDistributor.sendToServer(StageEditorPayload.action(root.toString()));
    }

    private void sendSimpleAction(String action) {
        captureActiveFields();
        JsonObject root = new JsonObject();
        root.addProperty("action", action);
        pendingSelection = selectedStageId;
        ClientPacketDistributor.sendToServer(StageEditorPayload.action(root.toString()));
    }

    private void openPicker() {
        captureActiveFields();
        StageRestrictionPickerScreen.Mode mode = pickerModeForTab();
        if (mode == null || working == null) {
            return;
        }
        String name = working.displayName == null || working.displayName.isBlank() ? working.stage : working.displayName;
        this.minecraft.setScreenAndShow(new StageRestrictionPickerScreen(this, mode, name, pickerSelection(mode)));
    }

    private StageRestrictionPickerScreen.Mode pickerModeForTab() {
        return switch (tab) {
            case RECIPES -> StageRestrictionPickerScreen.Mode.RECIPES;
            case ITEMS -> StageRestrictionPickerScreen.Mode.ITEMS;
            case BLOCKS -> StageRestrictionPickerScreen.Mode.BLOCKS;
            case DIMENSIONS -> StageRestrictionPickerScreen.Mode.DIMENSIONS;
            case MOBS -> StageRestrictionPickerScreen.Mode.MOBS;
            case STRUCTURES -> StageRestrictionPickerScreen.Mode.STRUCTURES;
            default -> null;
        };
    }

    private List<String> pickerSelection(StageRestrictionPickerScreen.Mode mode) {
        if (working == null) {
            return List.of();
        }
        return switch (mode) {
            case RECIPES -> List.copyOf(working.recipes);
            case ITEMS -> List.copyOf(working.items);
            case BLOCKS -> working.blocks.stream().map(StageEditorSnapshot.BlockEntry::block).toList();
            case DIMENSIONS -> List.copyOf(working.dimensions);
            case MOBS -> working.mobs.stream().map(StageEditorSnapshot.MobEntry::mob).toList();
            case STRUCTURES -> working.structures.stream().map(StageEditorSnapshot.StructureEntry::structure).toList();
        };
    }

    void applyPickerSelection(StageRestrictionPickerScreen.Mode mode, List<String> selectedIds) {
        if (working == null) {
            return;
        }
        List<String> clean = selectedIds == null ? List.of() : selectedIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .sorted()
                .toList();

        List<String> before = pickerSelection(mode).stream().distinct().sorted().toList();
        if (before.equals(clean)) {
            localNotice = "No selection changes.";
            localNoticeError = false;
            return;
        }

        switch (mode) {
            case RECIPES -> {
                working.recipes.clear();
                working.recipes.addAll(clean);
            }
            case ITEMS -> {
                working.items.clear();
                working.items.addAll(clean);
            }
            case DIMENSIONS -> {
                working.dimensions.clear();
                working.dimensions.addAll(clean);
            }
            case BLOCKS -> {
                Map<String, StageEditorSnapshot.BlockEntry> existing = new LinkedHashMap<>();
                for (StageEditorSnapshot.BlockEntry entry : working.blocks) {
                    existing.put(entry.block(), entry);
                }
                working.blocks.clear();
                for (String id : clean) {
                    working.blocks.add(existing.getOrDefault(id, new StageEditorSnapshot.BlockEntry(id, "")));
                }
            }
            case MOBS -> {
                Map<String, StageEditorSnapshot.MobEntry> existing = new LinkedHashMap<>();
                for (StageEditorSnapshot.MobEntry entry : working.mobs) {
                    existing.put(entry.mob(), entry);
                }
                working.mobs.clear();
                for (String id : clean) {
                    working.mobs.add(existing.getOrDefault(id, new StageEditorSnapshot.MobEntry(id, 64.0D)));
                }
            }
            case STRUCTURES -> {
                Map<String, StageEditorSnapshot.StructureEntry> existing = new LinkedHashMap<>();
                for (StageEditorSnapshot.StructureEntry entry : working.structures) {
                    existing.put(entry.structure(), entry);
                }
                working.structures.clear();
                for (String id : clean) {
                    working.structures.add(existing.getOrDefault(id, new StageEditorSnapshot.StructureEntry(id, 3)));
                }
            }
        }

        dirty = true;
        selectedEntry = -1;
        entryPage = 0;
        localNotice = "Updated " + mode.name().toLowerCase(Locale.ROOT) + " selection. Save the stage to apply it.";
        localNoticeError = false;
    }

    private void validateWorking() {
        if (working.stage == null || working.stage.isBlank() || !STAGE_ID_PATTERN.matcher(working.stage).matches()) {
            throw new IllegalArgumentException("Stage ID may only contain lowercase a-z, 0-9, underscore, dot and dash.");
        }
        for (String id : working.recipes) canonicalId(id);
        for (String id : working.items) canonicalId(id);
        for (String id : working.dimensions) canonicalId(id);
        for (StageEditorSnapshot.BlockEntry entry : working.blocks) {
            canonicalId(entry.block());
            if (!entry.mask().isBlank()) canonicalId(entry.mask());
        }
        for (StageEditorSnapshot.MobEntry entry : working.mobs) {
            canonicalId(entry.mob());
            if (entry.radius() <= 0.0D) throw new IllegalArgumentException("Mob radius must be greater than 0.");
        }
        for (StageEditorSnapshot.StructureEntry entry : working.structures) {
            canonicalId(entry.structure());
            if (entry.buffer() <= 0) throw new IllegalArgumentException("Structure buffer must be greater than 0.");
        }
    }

    private static String canonicalId(String value) {
        Identifier id = Identifier.parse(value.trim());
        return id.toString();
    }

    private List<String> displayRows() {
        if (working == null) {
            return List.of();
        }
        return switch (tab) {
            case RECIPES -> List.copyOf(working.recipes);
            case ITEMS -> List.copyOf(working.items);
            case DIMENSIONS -> List.copyOf(working.dimensions);
            case BLOCKS -> working.blocks.stream()
                    .map(entry -> entry.mask().isBlank() ? entry.block() : entry.block() + "  → mask " + entry.mask())
                    .toList();
            case MOBS -> working.mobs.stream()
                    .map(entry -> entry.mob() + "  • radius " + formatDouble(entry.radius()))
                    .toList();
            case STRUCTURES -> working.structures.stream()
                    .map(entry -> entry.structure() + "  • buffer " + entry.buffer())
                    .toList();
            default -> List.of();
        };
    }

    private String entryLabel() {
        return switch (tab) {
            case RECIPES -> "Manual recipe ID";
            case ITEMS -> "Manual item ID";
            case BLOCKS -> "Manual block ID";
            case DIMENSIONS -> "Manual dimension ID";
            case MOBS -> "Manual mob / entity ID";
            case STRUCTURES -> "Manual structure ID";
            default -> "ID";
        };
    }

    private String auxLabel() {
        return switch (tab) {
            case BLOCKS -> "Mask block (optional)";
            case MOBS -> "Spawn radius";
            case STRUCTURES -> "Buffer";
            default -> "";
        };
    }

    private StageEditorSnapshot.StageEntry selectedEntry() {
        return findStage(selectedStageId);
    }

    private StageEditorSnapshot.StageEntry findStage(String id) {
        if (id == null || snapshot == null) {
            return null;
        }
        return snapshot.stages().stream().filter(entry -> entry.stage().equals(id)).findFirst().orElse(null);
    }

    private List<StageEditorSnapshot.StageEntry> filteredStages() {
        stageSearchText = stageSearch == null ? stageSearchText : stageSearch.getValue();
        String query = stageSearchText.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return snapshot.stages();
        }
        return snapshot.stages().stream()
                .filter(entry -> entry.stage().toLowerCase(Locale.ROOT).contains(query)
                        || entry.displayName().toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    private String nextFreeStageId(String base) {
        String clean = base == null ? "new_stage" : base.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        if (clean.isBlank()) clean = "new_stage";
        String candidate = clean;
        int suffix = 2;
        while (findStage(candidate) != null) {
            candidate = clean + "_" + suffix++;
        }
        return candidate;
    }

    private void refreshWidgets() {
        if (stageSearch != null) {
            stageSearchText = stageSearch.getValue();
        }
        clearWidgets();
        init();
    }

    private int panelLeft() {
        return Math.max(6, (this.width - Math.min(PANEL_W, this.width - 12)) / 2);
    }

    private int panelTop() {
        return Math.max(6, (this.height - Math.min(PANEL_H, this.height - 12)) / 2);
    }

    private static boolean hit(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private void drawButton(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                            String label, boolean enabled, int mouseX, int mouseY) {
        drawButton(graphics, x, y, width, height, label, enabled, mouseX, mouseY, 0xFF353535);
    }

    private void drawButton(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                            String label, boolean enabled, int mouseX, int mouseY, int normalFill) {
        boolean hovered = enabled && hit(mouseX, mouseY, x, y, width, height);
        int fill = !enabled ? 0xFF252525 : hovered ? 0xFF4A4A4A : normalFill;
        int border = !enabled ? 0xFF3B3B3B : hovered ? 0xFFFFFFFF : 0xFF777777;
        int text = enabled ? 0xFFFFFFFF : 0xFF777777;
        graphics.fill(x, y, x + width, y + height, fill);
        graphics.outline(x, y, width, height, border);
        graphics.centeredText(this.font, Component.literal(trim(label, Math.max(4, width / 6))), x + width / 2, y + 6, text);
    }

    private static String trim(String value, int maxChars) {
        if (value == null || value.length() <= maxChars) {
            return value == null ? "" : value;
        }
        return value.substring(0, Math.max(1, maxChars - 1)) + "…";
    }

    private static String formatDouble(double value) {
        if (value == Math.rint(value)) {
            return Integer.toString((int) value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum Tab {
        GENERAL("General", "General stage settings", 58),
        RECIPES("Recipes", "Recipes locked behind this stage", 58),
        ITEMS("Items", "Items locked behind this stage", 48),
        BLOCKS("Blocks", "Blocks locked behind this stage", 52),
        DIMENSIONS("Dims", "Dimensions locked behind this stage", 44),
        MOBS("Mobs", "Mobs locked behind this stage", 44),
        STRUCTURES("Structs", "Structures locked behind this stage", 54),
        MESSAGES("Messages", "Custom locked messages", 66);

        private final String label;
        private final String description;
        private final int width;

        Tab(String label, String description, int width) {
            this.label = label;
            this.description = description;
            this.width = width;
        }
    }

    private static final class MutableStage {
        private String stage;
        private String displayName;
        private final Map<String, String> messages;
        private final List<String> recipes;
        private final List<String> items;
        private final List<StageEditorSnapshot.BlockEntry> blocks;
        private final List<String> dimensions;
        private final List<StageEditorSnapshot.MobEntry> mobs;
        private final List<StageEditorSnapshot.StructureEntry> structures;

        private MutableStage(String stage, String displayName, Map<String, String> messages,
                             List<String> recipes, List<String> items,
                             List<StageEditorSnapshot.BlockEntry> blocks,
                             List<String> dimensions, List<StageEditorSnapshot.MobEntry> mobs,
                             List<StageEditorSnapshot.StructureEntry> structures) {
            this.stage = stage;
            this.displayName = displayName;
            this.messages = messages;
            this.recipes = recipes;
            this.items = items;
            this.blocks = blocks;
            this.dimensions = dimensions;
            this.mobs = mobs;
            this.structures = structures;
        }

        private static MutableStage from(StageEditorSnapshot.StageEntry entry) {
            StageEditorSnapshot.StageEntry safe = entry.normalized();
            return new MutableStage(
                    safe.stage(), safe.displayName(), new LinkedHashMap<>(safe.messages()),
                    new ArrayList<>(safe.recipes()), new ArrayList<>(safe.items()),
                    new ArrayList<>(safe.blocks()), new ArrayList<>(safe.dimensions()),
                    new ArrayList<>(safe.mobs()), new ArrayList<>(safe.structures())
            );
        }

        private static MutableStage empty(String id) {
            return new MutableStage(
                    id, "", new LinkedHashMap<>(), new ArrayList<>(), new ArrayList<>(),
                    new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()
            );
        }

        private StageEditorSnapshot.StageEntry toEntry() {
            return new StageEditorSnapshot.StageEntry(
                    stage, displayName, new LinkedHashMap<>(messages), List.copyOf(recipes), List.copyOf(items),
                    List.copyOf(blocks), List.copyOf(dimensions), List.copyOf(mobs), List.copyOf(structures)
            ).normalized();
        }
    }
}
