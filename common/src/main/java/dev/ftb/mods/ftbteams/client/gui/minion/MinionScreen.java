package dev.ftb.mods.ftbteams.client.gui.minion;

import dev.ftb.mods.ftbteams.client.gui.component.PanelGraphics;
import dev.ftb.mods.ftbteams.world.entity.MinionEntity;
import dev.ftb.mods.ftbteams.world.inventory.MinionMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Visual profile for a minion. Gameplay state and authorization remain server-owned by {@link MinionMenu}. */
public class MinionScreen extends AbstractContainerScreen<MinionMenu> {
	private static final int WIDTH = 320;
	private static final int HEIGHT = 200;
	private MinionSkinEditBox skinIdBox;

	public MinionScreen(MinionMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = WIDTH;
		imageHeight = HEIGHT;
	}

	@Override
	protected void init() {
		super.init();
		skinIdBox = addRenderableWidget(new MinionSkinEditBox(font, leftPos + 169, topPos + 145, 124, 18,
				menu.getMinionId()));
		addRenderableWidget(Button.builder(Component.translatable("ftbteams.minion.choose_skin"), button -> skinIdBox.open())
				.bounds(leftPos + 169, topPos + 169, 124, 20)
				.tooltip(Tooltip.create(Component.translatable("ftbteams.minion.choose_skin_tooltip"))).build());
		addRenderableWidget(Button.builder(Component.literal("\u00D7"), button -> clearProfession())
				.bounds(leftPos + 294, topPos + 8, 18, 18)
				.tooltip(Tooltip.create(Component.translatable("ftbteams.minion.clear_profession_tooltip"))).build())
				.active = menu.getWorkstation() != null;
	}

	private void clearProfession() {
		if (minecraft != null && minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MinionMenu.CLEAR_PROFESSION_BUTTON);
		}
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.pose().pushPose();
		graphics.pose().translate(leftPos, topPos, 0);
		drawPanels(graphics);
		drawIcons(graphics);
		drawInventory(graphics);
		drawVitals(graphics);
		graphics.pose().popPose();

		MinionEntity minion = minion();
		if (minion != null) {
			InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, leftPos + 158, topPos + 56,
					leftPos + 304, topPos + 130, 55, 0.0625F, mouseX, mouseY, minion);
		}
	}

	private void drawPanels(GuiGraphics graphics) {
		PanelGraphics.drawPanel(graphics, 0, 0, 138, 45, 0xFF77797C, 0xFF303235);
		PanelGraphics.drawPanel(graphics, 0, 48, 138, 26, 0xFFEC7137, 0xFF713C27);
		PanelGraphics.drawPanel(graphics, 0, 77, 138, 123, 0xFFEC7137, 0xFF713C27);
		PanelGraphics.drawPanel(graphics, 142, 0, 178, 45, 0xFF4F86B8, 0xFF102D45);
		PanelGraphics.drawPanel(graphics, 142, 48, 178, 152, 0xFF36A45F, 0xFF082E22);
	}

	private void drawIcons(GuiGraphics graphics) {
		graphics.fill(13, 7, 45, 39, 0xFF101713);
		PanelGraphics.drawFactionLogo(graphics, menu.getFactionLogo(), 16, 10, 26);
		graphics.fill(151, 7, 183, 39, 0xFF101713);
		ItemStack professionStack = minion() == null ? menu.getProfessionStack() : minion().getProfessionStack();
		if (!professionStack.isEmpty()) {
			graphics.renderItem(professionStack, 159, 15);
		}
	}

	private static void drawInventory(GuiGraphics graphics) {
		for (int row = 0; row < 4; row++) {
			for (int column = 0; column < 5; column++) {
				PanelGraphics.drawSlot(graphics, 13 + column * 24, 86 + row * 21, 20, 0xFF725545);
			}
		}
	}

	private void drawVitals(GuiGraphics graphics) {
		MinionEntity minion = minion();
		float health = minion == null ? 0F : minion.getHealth() / minion.getMaxHealth();
		float food = minion == null ? 0F : (float) minion.getFood() / MinionEntity.MAX_FOOD;
		PanelGraphics.drawSegmentedBar(graphics, 30, 55, 43, health, 0xFFE53B3B, 5);
		PanelGraphics.drawSegmentedBar(graphics, 96, 55, 30, food, 0xFFE29A38, MinionEntity.MAX_FOOD);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(font, Component.translatable("ftbteams.minion.faction"), 52, 12, 0xFFF1F1F1);
		graphics.drawString(font, menu.getFactionName().isBlank() ? Component.translatable("ftbteams.minion.faction_none")
				: Component.literal(menu.getFactionName()), 52, 27, 0xFFBEBEBE);
		graphics.drawString(font, "\u2665", 13, 54, 0xFFE53B3B);
		graphics.drawString(font, Component.translatable("ftbteams.minion.health"), 13, 65, 0xFFFFD1A6);
		graphics.drawString(font, Component.translatable("ftbteams.minion.food", minion() == null ? 0 : minion().getFood(),
				MinionEntity.MAX_FOOD), 76, 65, 0xFFFFD1A6);
		graphics.drawCenteredString(font, Component.translatable(minion() != null && minion().isResting()
				? "ftbteams.minion.resting" : "ftbteams.minion.awake"), 231, 134, 0xFFB8D2BF);
		graphics.drawString(font, Component.translatable("ftbteams.minion.profession"), 192, 12, 0xFFF1F1F1);
		graphics.drawString(font, menu.getProfession().isBlank() ? Component.translatable("ftbteams.minion.profession_none")
				: Component.translatable(menu.getProfession()), 192, 27, 0xFFB9D9FF);
		drawCoordinates(graphics, "ftbteams.minion.home", menu.getHome(), 8, 174);
		drawCoordinates(graphics, "ftbteams.minion.workstation", menu.getWorkstation(), 8, 185);
	}

	private void drawCoordinates(GuiGraphics graphics, String key, BlockPos pos, int x, int y) {
		Component label = Component.translatable(key, pos == null ? "-" : pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
		graphics.pose().pushPose();
		graphics.pose().scale(0.68F, 0.68F, 1F);
		graphics.drawString(font, label, Math.round(x / 0.68F), Math.round(y / 0.68F), 0xFFEBC9B4);
		graphics.pose().popPose();
	}

	private MinionEntity minion() {
		return minecraft != null && minecraft.level != null
				&& minecraft.level.getEntity(menu.getMinionId()) instanceof MinionEntity minion ? minion : null;
	}
}
