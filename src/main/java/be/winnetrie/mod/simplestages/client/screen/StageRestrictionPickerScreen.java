package be.winnetrie.mod.simplestages.client.screen;

import be.winnetrie.mod.simplestages.admin.StageEditorCatalog;
import be.winnetrie.mod.simplestages.network.StageEditorPayload;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Simple, visual restriction picker used by the Stage Manager.
 * Raw ID fields stay available on the parent screen as an advanced fallback.
 */
public final class StageRestrictionPickerScreen extends Screen {
    private static final int PANEL_W = 680;
    private static final int PANEL_H = 370;
    private static final int TILE = 22;
    private static final int SIMPLE_COLS = 22;
    private static final int SIMPLE_ROWS = 10;
    private static final int SIMPLE_PER_PAGE = SIMPLE_COLS * SIMPLE_ROWS;
    private static final int RECIPE_ITEM_COLS = 11;
    private static final int RECIPE_ITEM_ROWS = 9;
    private static final int RECIPE_ITEMS_PER_PAGE = RECIPE_ITEM_COLS * RECIPE_ITEM_ROWS;
    private static final int TEXT_ROWS = 17;
    private static final int CATALOG_ROWS = 14;
    private static final int ROW_H = 16;

    private final StageEditorScreen parent;
    private final Mode mode;
    private final String stageName;
    private final LinkedHashSet<String> selected;

    private EditBox search;
    private int itemPage;
    private int textPage;
    private List<StageEditorCatalog.Entry> serverEntries = List.of();
    private String selectedRecipeResultItem = "";
    private boolean loading;
    private boolean requestedInitialCatalog;
    private String notice = "";
    private boolean noticeError;

    public StageRestrictionPickerScreen(StageEditorScreen parent, Mode mode, String stageName, List<String> currentSelection) {
        super(Component.literal(mode.title));
        this.parent = parent;
        this.mode = mode;
        this.stageName = stageName == null ? "" : stageName;
        this.selected = new LinkedHashSet<>(currentSelection == null ? List.of() : currentSelection);
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int top = panelTop();
        int searchWidth = mode == Mode.RECIPES ? 242 : 330;
        search = new EditBox(this.font, left + 12, top + 32, searchWidth, 18, Component.literal(searchHint()));
        search.setMaxLength(120);
        addRenderableWidget(search);

        if (!requestedInitialCatalog && (mode == Mode.DIMENSIONS || mode == Mode.STRUCTURES)) {
            requestedInitialCatalog = true;
            requestRegistryCatalog(mode == Mode.DIMENSIONS ? "dimensions" : "structures");
        }
    }

    public void applyCatalog(StageEditorCatalog catalog) {
        if (catalog == null) {
            return;
        }
        if (mode == Mode.RECIPES && "recipes".equals(catalog.catalog())) {
            if (!selectedRecipeResultItem.equals(catalog.context())) {
                return;
            }
            serverEntries = catalog.entries();
            loading = false;
            textPage = 0;
            notice = catalog.notice();
            noticeError = catalog.noticeError();
            return;
        }
        if (mode == Mode.DIMENSIONS && "dimensions".equals(catalog.catalog())) {
            serverEntries = catalog.entries();
            loading = false;
            textPage = 0;
            notice = catalog.notice();
            noticeError = catalog.noticeError();
            return;
        }
        if (mode == Mode.STRUCTURES && "structures".equals(catalog.catalog())) {
            serverEntries = catalog.entries();
            loading = false;
            textPage = 0;
            notice = catalog.notice();
            noticeError = catalog.noticeError();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int left = panelLeft();
        int top = panelTop();
        int right = left + Math.min(PANEL_W, this.width - 12);
        int bottom = top + Math.min(PANEL_H, this.height - 12);

        graphics.fill(0, 0, this.width, this.height, 0xB0000000);
        graphics.fill(left, top, right, bottom, 0xFF171717);
        graphics.outline(left, top, right - left, bottom - top, 0xFF6A6A6A);
        graphics.text(this.font, Component.literal(mode.title), left + 12, top + 10, 0xFFFFFFFF, false);
        graphics.text(this.font,
                Component.literal("Stage: " + trim(stageName, 48) + "  •  selected: " + selected.size()),
                left + 180, top + 11, 0xFFAAAAAA, false);

        switch (mode) {
            case ITEMS -> drawItemGrid(graphics, mouseX, mouseY, left, top, false);
            case BLOCKS -> drawBlockGrid(graphics, mouseX, mouseY, left, top);
            case RECIPES -> drawRecipePicker(graphics, mouseX, mouseY, left, top);
            case MOBS -> drawMobList(graphics, mouseX, mouseY, left, top);
            case DIMENSIONS, STRUCTURES -> drawServerList(graphics, mouseX, mouseY, left, top);
        }

        drawBottomActions(graphics, mouseX, mouseY, left, top);
    }

    private void drawItemGrid(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int left, int top, boolean recipeMode) {
        List<Item> items = filteredItems();
        int perPage = recipeMode ? RECIPE_ITEMS_PER_PAGE : SIMPLE_PER_PAGE;
        int cols = recipeMode ? RECIPE_ITEM_COLS : SIMPLE_COLS;
        int maxPage = Math.max(0, (items.size() - 1) / perPage);
        itemPage = Math.min(itemPage, maxPage);
        int start = itemPage * perPage;
        int gridX = left + 12;
        int gridY = top + 58;
        int available = Math.min(perPage, items.size() - start);

        for (int i = 0; i < available; i++) {
            Item item = items.get(start + i);
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            ItemStack stack = new ItemStack(item);
            int col = i % cols;
            int row = i / cols;
            int x = gridX + col * TILE;
            int y = gridY + row * TILE;
            boolean active = recipeMode ? id.toString().equals(selectedRecipeResultItem) : selected.contains(id.toString());
            boolean hovered = hit(mouseX, mouseY, x, y, TILE - 2, TILE - 2);

            graphics.fill(x, y, x + TILE - 2, y + TILE - 2,
                    active ? 0xFF3E5D7A : hovered ? 0xFF3A3A3A : 0xFF262626);
            graphics.outline(x, y, TILE - 2, TILE - 2, active ? 0xFFFFFFFF : 0xFF555555);
            graphics.item(stack, x + 2, y + 2);
            if (hovered) {
                graphics.setTooltipForNextFrame(this.font, stack, mouseX, mouseY);
            }
        }

        int navY = recipeMode ? top + 264 : top + 286;
        drawButton(graphics, gridX, navY, 28, 18, "<", itemPage > 0, mouseX, mouseY);
        graphics.text(this.font, Component.literal((itemPage + 1) + "/" + (maxPage + 1)), gridX + 42, navY + 5, 0xFFBDBDBD, false);
        drawButton(graphics, gridX + 88, navY, 28, 18, ">", itemPage < maxPage, mouseX, mouseY);
    }

    private void drawBlockGrid(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int left, int top) {
        List<Block> blocks = filteredBlocks();
        int maxPage = Math.max(0, (blocks.size() - 1) / SIMPLE_PER_PAGE);
        itemPage = Math.min(itemPage, maxPage);
        int start = itemPage * SIMPLE_PER_PAGE;
        int gridX = left + 12;
        int gridY = top + 58;
        int available = Math.min(SIMPLE_PER_PAGE, blocks.size() - start);

        for (int i = 0; i < available; i++) {
            Block block = blocks.get(start + i);
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            ItemStack stack = new ItemStack(block.asItem());
            int col = i % SIMPLE_COLS;
            int row = i / SIMPLE_COLS;
            int x = gridX + col * TILE;
            int y = gridY + row * TILE;
            boolean active = selected.contains(id.toString());
            boolean hovered = hit(mouseX, mouseY, x, y, TILE - 2, TILE - 2);

            graphics.fill(x, y, x + TILE - 2, y + TILE - 2,
                    active ? 0xFF3E5D7A : hovered ? 0xFF3A3A3A : 0xFF262626);
            graphics.outline(x, y, TILE - 2, TILE - 2, active ? 0xFFFFFFFF : 0xFF555555);
            graphics.item(stack, x + 2, y + 2);
            if (hovered) {
                graphics.setTooltipForNextFrame(this.font, stack, mouseX, mouseY);
            }
        }

        int navY = top + 286;
        drawButton(graphics, gridX, navY, 28, 18, "<", itemPage > 0, mouseX, mouseY);
        graphics.text(this.font, Component.literal((itemPage + 1) + "/" + (maxPage + 1)), gridX + 42, navY + 5, 0xFFBDBDBD, false);
        drawButton(graphics, gridX + 88, navY, 28, 18, ">", itemPage < maxPage, mouseX, mouseY);
        graphics.text(this.font, Component.literal("Blocks without an item form can still be entered manually."),
                left + 150, navY + 5, 0xFF888888, false);
    }

    private void drawRecipePicker(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int left, int top) {
        graphics.text(this.font, Component.literal("1. Choose result item"), left + 12, top + 52, 0xFFBDBDBD, false);
        drawItemGrid(graphics, mouseX, mouseY, left, top, true);

        int dividerX = left + 274;
        graphics.fill(dividerX, top + 30, dividerX + 1, top + 316, 0xFF454545);
        graphics.text(this.font, Component.literal("2. Choose recipe(s)"), dividerX + 12, top + 34, 0xFFBDBDBD, false);

        if (selectedRecipeResultItem.isBlank()) {
            graphics.textWithWordWrap(this.font,
                    Component.literal("Select an item on the left. Simple Stages will ask the server for every loaded recipe that produces it."),
                    dividerX + 12, top + 60, 370, 0xFF8F8F8F);
            return;
        }

        graphics.text(this.font, Component.literal(trim(selectedRecipeResultItem, 48)), dividerX + 12, top + 51, 0xFFFFFFFF, false);
        if (loading) {
            graphics.text(this.font, Component.literal("Loading recipes from server..."), dividerX + 12, top + 72, 0xFFFFC56B, false);
            return;
        }

        List<StageEditorCatalog.Entry> entries = filteredServerEntries(false);
        drawCatalogRows(graphics, mouseX, mouseY, dividerX + 12, top + 70, 372, entries, true);
    }

    private void drawMobList(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int left, int top) {
        List<MobRow> rows = filteredMobs();
        int maxPage = Math.max(0, (rows.size() - 1) / TEXT_ROWS);
        textPage = Math.min(textPage, maxPage);
        int start = textPage * TEXT_ROWS;
        int listX = left + 12;
        int listY = top + 58;

        for (int i = 0; i < TEXT_ROWS && start + i < rows.size(); i++) {
            MobRow row = rows.get(start + i);
            int y = listY + i * ROW_H;
            boolean active = selected.contains(row.id());
            boolean hovered = hit(mouseX, mouseY, listX, y, 540, ROW_H - 1);
            if (active) {
                graphics.fill(listX, y, listX + 540, y + ROW_H - 1, 0xFF3E5D7A);
            } else if (hovered) {
                graphics.fill(listX, y, listX + 540, y + ROW_H - 1, 0xFF303030);
            }
            graphics.text(this.font, Component.literal(trim(row.name(), 32)), listX + 4, y + 4, 0xFFFFFFFF, false);
            graphics.text(this.font, Component.literal(trim(row.id(), 48)), listX + 180, y + 4, 0xFFAAAAAA, false);
        }

        drawTextNavigation(graphics, mouseX, mouseY, left, top, maxPage);
        graphics.text(this.font, Component.literal("New mobs use radius 64; adjust radius later in the main editor if needed."),
                left + 145, top + 334, 0xFF888888, false);
    }

    private void drawServerList(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int left, int top) {
        if (loading) {
            graphics.text(this.font, Component.literal("Loading from server..."), left + 12, top + 62, 0xFFFFC56B, false);
            return;
        }
        List<StageEditorCatalog.Entry> rows = filteredServerEntries(true);
        drawCatalogRows(graphics, mouseX, mouseY, left + 12, top + 58, 540, rows, false);
        if (mode == Mode.STRUCTURES) {
            graphics.text(this.font, Component.literal("New structures use buffer 3; adjust buffer later in the main editor if needed."),
                    left + 145, top + 334, 0xFF888888, false);
        }
    }

    private void drawCatalogRows(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y,
                                 int width, List<StageEditorCatalog.Entry> rows, boolean recipeRows) {
        int maxPage = Math.max(0, (rows.size() - 1) / CATALOG_ROWS);
        textPage = Math.min(textPage, maxPage);
        int start = textPage * CATALOG_ROWS;

        for (int i = 0; i < CATALOG_ROWS && start + i < rows.size(); i++) {
            StageEditorCatalog.Entry row = rows.get(start + i);
            int ry = y + i * ROW_H;
            boolean active = selected.contains(row.id());
            boolean hovered = hit(mouseX, mouseY, x, ry, width, ROW_H - 1);
            if (active) {
                graphics.fill(x, ry, x + width, ry + ROW_H - 1, 0xFF3E5D7A);
            } else if (hovered) {
                graphics.fill(x, ry, x + width, ry + ROW_H - 1, 0xFF303030);
            }
            graphics.text(this.font, Component.literal(trim(row.id(), recipeRows ? 45 : 68)), x + 4, ry + 4, 0xFFFFFFFF, false);
            if (recipeRows && !row.detail().isBlank()) {
                graphics.text(this.font, Component.literal(trim(row.detail(), 28)), x + 235, ry + 4, 0xFF999999, false);
            }
        }

        int navY = y + CATALOG_ROWS * ROW_H + 4;
        drawButton(graphics, x, navY, 28, 18, "<", textPage > 0, mouseX, mouseY);
        graphics.text(this.font, Component.literal((textPage + 1) + "/" + (maxPage + 1)), x + 42, navY + 5, 0xFFBDBDBD, false);
        drawButton(graphics, x + 88, navY, 28, 18, ">", textPage < maxPage, mouseX, mouseY);
    }

    private void drawTextNavigation(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int left, int top, int maxPage) {
        int y = top + 334;
        drawButton(graphics, left + 12, y, 28, 18, "<", textPage > 0, mouseX, mouseY);
        graphics.text(this.font, Component.literal((textPage + 1) + "/" + (maxPage + 1)), left + 54, y + 5, 0xFFBDBDBD, false);
        drawButton(graphics, left + 100, y, 28, 18, ">", textPage < maxPage, mouseX, mouseY);
    }

    private void drawBottomActions(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int left, int top) {
        int y = top + 334;
        drawButton(graphics, left + 560, y, 48, 20, "Cancel", true, mouseX, mouseY);
        drawButton(graphics, left + 614, y, 52, 20, "Done", true, mouseX, mouseY, 0xFF315C3A);

        if (mode != Mode.MOBS && mode != Mode.DIMENSIONS && mode != Mode.STRUCTURES) {
            drawButton(graphics, left + 470, y, 82, 20, "Clear all", !selected.isEmpty(), mouseX, mouseY, 0xFF553333);
        }

        if (!notice.isBlank()) {
            graphics.text(this.font, Component.literal(trim(notice, 70)), left + 150, top + 314,
                    noticeError ? 0xFFFF7070 : 0xFF80FF80, false);
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

        if (mode == Mode.ITEMS || mode == Mode.RECIPES) {
            List<Item> items = filteredItems();
            int perPage = mode == Mode.RECIPES ? RECIPE_ITEMS_PER_PAGE : SIMPLE_PER_PAGE;
            int cols = mode == Mode.RECIPES ? RECIPE_ITEM_COLS : SIMPLE_COLS;
            int start = itemPage * perPage;
            int gridX = left + 12;
            int gridY = top + 58;
            int available = Math.min(perPage, Math.max(0, items.size() - start));
            for (int i = 0; i < available; i++) {
                int x = gridX + (i % cols) * TILE;
                int y = gridY + (i / cols) * TILE;
                if (hit(mx, my, x, y, TILE - 2, TILE - 2)) {
                    Identifier id = BuiltInRegistries.ITEM.getKey(items.get(start + i));
                    if (mode == Mode.RECIPES) {
                        selectRecipeResult(id.toString());
                    } else {
                        toggle(id.toString());
                    }
                    return true;
                }
            }
            int navY = mode == Mode.RECIPES ? top + 264 : top + 286;
            int maxPage = Math.max(0, (items.size() - 1) / perPage);
            if (hit(mx, my, gridX, navY, 28, 18) && itemPage > 0) {
                itemPage--;
                return true;
            }
            if (hit(mx, my, gridX + 88, navY, 28, 18) && itemPage < maxPage) {
                itemPage++;
                return true;
            }
        } else if (mode == Mode.BLOCKS) {
            List<Block> blocks = filteredBlocks();
            int start = itemPage * SIMPLE_PER_PAGE;
            int gridX = left + 12;
            int gridY = top + 58;
            int available = Math.min(SIMPLE_PER_PAGE, Math.max(0, blocks.size() - start));
            for (int i = 0; i < available; i++) {
                int x = gridX + (i % SIMPLE_COLS) * TILE;
                int y = gridY + (i / SIMPLE_COLS) * TILE;
                if (hit(mx, my, x, y, TILE - 2, TILE - 2)) {
                    toggle(BuiltInRegistries.BLOCK.getKey(blocks.get(start + i)).toString());
                    return true;
                }
            }
            int maxPage = Math.max(0, (blocks.size() - 1) / SIMPLE_PER_PAGE);
            if (hit(mx, my, gridX, top + 286, 28, 18) && itemPage > 0) {
                itemPage--;
                return true;
            }
            if (hit(mx, my, gridX + 88, top + 286, 28, 18) && itemPage < maxPage) {
                itemPage++;
                return true;
            }
        }

        if (mode == Mode.RECIPES && !loading && !selectedRecipeResultItem.isBlank()) {
            List<StageEditorCatalog.Entry> rows = filteredServerEntries(false);
            int x = left + 286;
            int y = top + 70;
            if (clickCatalogRows(mx, my, x, y, 372, rows)) {
                return true;
            }
            if (clickCatalogNavigation(mx, my, x, y, rows)) {
                return true;
            }
        } else if (mode == Mode.MOBS) {
            List<MobRow> rows = filteredMobs();
            int start = textPage * TEXT_ROWS;
            int x = left + 12;
            int y = top + 58;
            for (int i = 0; i < TEXT_ROWS && start + i < rows.size(); i++) {
                int ry = y + i * ROW_H;
                if (hit(mx, my, x, ry, 540, ROW_H - 1)) {
                    toggle(rows.get(start + i).id());
                    return true;
                }
            }
            int maxPage = Math.max(0, (rows.size() - 1) / TEXT_ROWS);
            if (hit(mx, my, left + 12, top + 334, 28, 18) && textPage > 0) {
                textPage--;
                return true;
            }
            if (hit(mx, my, left + 100, top + 334, 28, 18) && textPage < maxPage) {
                textPage++;
                return true;
            }
        } else if ((mode == Mode.DIMENSIONS || mode == Mode.STRUCTURES) && !loading) {
            List<StageEditorCatalog.Entry> rows = filteredServerEntries(true);
            int x = left + 12;
            int y = top + 58;
            if (clickCatalogRows(mx, my, x, y, 540, rows)) {
                return true;
            }
            if (clickCatalogNavigation(mx, my, x, y, rows)) {
                return true;
            }
        }

        int bottomY = top + 334;
        if (hit(mx, my, left + 560, bottomY, 48, 20)) {
            MinecraftScreenSwitch.backTo(parent);
            return true;
        }
        if (hit(mx, my, left + 614, bottomY, 52, 20)) {
            parent.applyPickerSelection(mode, new ArrayList<>(selected));
            MinecraftScreenSwitch.backTo(parent);
            return true;
        }
        if (mode != Mode.MOBS && mode != Mode.DIMENSIONS && mode != Mode.STRUCTURES
                && hit(mx, my, left + 470, bottomY, 82, 20) && !selected.isEmpty()) {
            selected.clear();
            notice = "Selection cleared. Click Done to keep this change.";
            noticeError = false;
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    private boolean clickCatalogRows(double mx, double my, int x, int y, int width, List<StageEditorCatalog.Entry> rows) {
        int start = textPage * CATALOG_ROWS;
        for (int i = 0; i < CATALOG_ROWS && start + i < rows.size(); i++) {
            int ry = y + i * ROW_H;
            if (hit(mx, my, x, ry, width, ROW_H - 1)) {
                toggle(rows.get(start + i).id());
                return true;
            }
        }
        return false;
    }

    private boolean clickCatalogNavigation(double mx, double my, int x, int y, List<StageEditorCatalog.Entry> rows) {
        int maxPage = Math.max(0, (rows.size() - 1) / CATALOG_ROWS);
        int navY = y + CATALOG_ROWS * ROW_H + 4;
        if (hit(mx, my, x, navY, 28, 18) && textPage > 0) {
            textPage--;
            return true;
        }
        if (hit(mx, my, x + 88, navY, 28, 18) && textPage < maxPage) {
            textPage++;
            return true;
        }
        return false;
    }

    private void selectRecipeResult(String itemId) {
        selectedRecipeResultItem = itemId;
        serverEntries = List.of();
        textPage = 0;
        loading = true;
        notice = "";
        JsonObject root = new JsonObject();
        root.addProperty("action", "recipe_catalog");
        root.addProperty("item", itemId);
        ClientPacketDistributor.sendToServer(StageEditorPayload.action(root.toString()));
    }

    private void requestRegistryCatalog(String catalog) {
        loading = true;
        JsonObject root = new JsonObject();
        root.addProperty("action", "registry_catalog");
        root.addProperty("catalog", catalog);
        ClientPacketDistributor.sendToServer(StageEditorPayload.action(root.toString()));
    }

    private void toggle(String id) {
        if (!selected.remove(id)) {
            selected.add(id);
        }
        notice = "";
    }

    private List<Item> filteredItems() {
        String query = searchQuery();
        return BuiltInRegistries.ITEM.stream()
                .filter(item -> item != Items.AIR)
                .filter(item -> {
                    if (query.isEmpty()) {
                        return true;
                    }
                    Identifier id = BuiltInRegistries.ITEM.getKey(item);
                    ItemStack stack = new ItemStack(item);
                    return id.toString().toLowerCase(Locale.ROOT).contains(query)
                            || stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query);
                })
                .sorted(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()))
                .toList();
    }

    private List<Block> filteredBlocks() {
        String query = searchQuery();
        return BuiltInRegistries.BLOCK.stream()
                .filter(block -> block.asItem() != Items.AIR)
                .filter(block -> {
                    if (query.isEmpty()) {
                        return true;
                    }
                    Identifier id = BuiltInRegistries.BLOCK.getKey(block);
                    ItemStack stack = new ItemStack(block.asItem());
                    return id.toString().toLowerCase(Locale.ROOT).contains(query)
                            || stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query);
                })
                .sorted(Comparator.comparing(block -> BuiltInRegistries.BLOCK.getKey(block).toString()))
                .toList();
    }

    private List<MobRow> filteredMobs() {
        String query = searchQuery();
        return BuiltInRegistries.ENTITY_TYPE.stream()
                .map(type -> new MobRow(
                        BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(),
                        safeEntityName(type)
                ))
                .filter(row -> !"minecraft:player".equals(row.id()))
                .filter(row -> query.isEmpty()
                        || row.id().toLowerCase(Locale.ROOT).contains(query)
                        || row.name().toLowerCase(Locale.ROOT).contains(query))
                .sorted(Comparator.comparing(MobRow::id))
                .toList();
    }

    private List<StageEditorCatalog.Entry> filteredServerEntries(boolean useSearch) {
        if (!useSearch || searchQuery().isEmpty()) {
            return serverEntries;
        }
        String query = searchQuery();
        return serverEntries.stream()
                .filter(entry -> entry.id().toLowerCase(Locale.ROOT).contains(query)
                        || entry.detail().toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    private String safeEntityName(EntityType<?> type) {
        try {
            return type.getDescription().getString();
        } catch (RuntimeException ignored) {
            return BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
        }
    }

    private String searchQuery() {
        return search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
    }

    private String searchHint() {
        return switch (mode) {
            case RECIPES -> "Search result item";
            case ITEMS -> "Search items";
            case BLOCKS -> "Search blocks";
            case MOBS -> "Search mobs";
            case DIMENSIONS -> "Search dimensions";
            case STRUCTURES -> "Search structures";
        };
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

    @Override
    public void onClose() {
        MinecraftScreenSwitch.backTo(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public enum Mode {
        RECIPES("Recipe Browser"),
        ITEMS("Item Browser"),
        BLOCKS("Block Browser"),
        DIMENSIONS("Dimension Browser"),
        MOBS("Mob Browser"),
        STRUCTURES("Structure Browser");

        private final String title;

        Mode(String title) {
            this.title = title;
        }
    }

    private record MobRow(String id, String name) {
    }

    /** Keeps the screen switch in one place and avoids reinitializing parent state manually. */
    private static final class MinecraftScreenSwitch {
        private MinecraftScreenSwitch() {
        }

        private static void backTo(Screen parent) {
            net.minecraft.client.Minecraft.getInstance().setScreenAndShow(parent);
        }
    }
}
