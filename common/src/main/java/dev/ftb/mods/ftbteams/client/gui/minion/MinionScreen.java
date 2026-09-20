package dev.ftb.mods.ftbteams.client.gui.minion;

import dev.ftb.mods.ftbteams.world.entity.MinionEntity;
import dev.ftb.mods.ftbteams.world.inventory.MinionMenu;
import dev.ftb.mods.ftbteams.api.faction.FactionLogo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Visual profile for a minion. It owns no gameplay state; MinionMenu authorizes and supplies its identity. */
public class MinionScreen extends AbstractContainerScreen<MinionMenu> {
	private static final int WIDTH = 320;
	private static final int HEIGHT = 220;

	public MinionScreen(MinionMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = WIDTH;
		imageHeight = HEIGHT;
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(Button.builder(Component.translatable("ftbteams.minion.change_skin"), button -> {
		}).bounds(leftPos + 174, topPos + 189, 124, 20).tooltip(Tooltip.create(
				Component.translatable("ftbteams.minion.change_skin_tooltip"))).build()).active = false;
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.pose().pushPose();
		graphics.pose().translate(leftPos, topPos, 0);
		panel(graphics, 0, 0, 138, 62, 0xFF4F86B8, 0xFF102D45);
		panel(graphics, 0, 65, 138, 26, 0xFFEC7137, 0xFF713C27);
		panel(graphics, 0, 94, 138, 126, 0xFFEC7137, 0xFF713C27);
		panel(graphics, 142, 0, 178, 220, 0xFF36A45F, 0xFF082E22);
		graphics.fill(13, 7, 45, 39, 0xFF101713);
		drawFactionLogo(graphics, 16, 10);
		for (int row = 0; row < 4; row++) {
			for (int column = 0; column < 5; column++) drawSlot(graphics, 13 + column * 24, 103 + row * 24);
		}
		float health = minion() == null ? 0F : minion().getHealth() / minion().getMaxHealth();
		drawBar(graphics, 30, 72, 43, health, 0xFFE53B3B, 5);
		float food = minion() == null ? 0F : (float) minion().getFood() / MinionEntity.MAX_FOOD;
		drawBar(graphics, 96, 72, 30, food, 0xFFE29A38, MinionEntity.MAX_FOOD);
		graphics.pose().popPose();

		MinionEntity minion = minion();
		if (minion != null) {
			InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, leftPos + 158, topPos + 18, leftPos + 304, topPos + 159,
				55, 0.0625F, mouseX, mouseY, minion);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(font, Component.translatable("ftbteams.minion.faction"), 52, 12, 0xFFF1F1F1);
		graphics.drawString(font, menu.getFactionName().isBlank() ? Component.translatable("ftbteams.minion.faction_none")
				: Component.literal(menu.getFactionName()), 52, 27, 0xFFBEBEBE);
		if (!menu.getProfession().isBlank()) {
			graphics.drawString(font, Component.translatable(menu.getProfession()), 52, 39, 0xFFB9D9FF);
		}
		graphics.drawString(font, "?", 13, 71, 0xFFE53B3B);
		graphics.drawString(font, Component.translatable("ftbteams.minion.health"), 13, 82, 0xFFFFD1A6);
		graphics.drawString(font, Component.translatable("ftbteams.minion.food", minion() == null ? 0 : minion().getFood(), MinionEntity.MAX_FOOD),
				76, 82, 0xFFFFD1A6);
		graphics.drawCenteredString(font, Component.translatable(minion() != null && minion().isResting()
				? "ftbteams.minion.resting" : "ftbteams.minion.awake"), 231, 176, 0xFFB8D2BF);
	}

	private MinionEntity minion() {
		return minecraft != null && minecraft.level != null && minecraft.level.getEntity(menu.getMinionId()) instanceof MinionEntity minion ? minion : null;
	}

	private static void panel(GuiGraphics graphics, int x, int y, int width, int height, int edge, int fill) {
		graphics.fill(x, y, x + width, y + height, 0xFF101713);
		graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, edge);
		graphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, fill);
		graphics.fill(x + 4, y + height - 4, x + width - 3, y + height - 2, 0xFF19211C);
	}

	private static void drawSlot(GuiGraphics graphics, int x, int y) {
		graphics.fill(x - 1, y - 1, x + 20, y + 20, 0xFF261F1B);
		graphics.fill(x, y, x + 20, y + 20, 0xFFD2BDAE);
		graphics.fill(x + 1, y + 1, x + 19, y + 19, 0xFF725545);
	}

	private static void drawBar(GuiGraphics graphics, int x, int y, int width, float value, int filled, int segments) {
		int segmentWidth = width / segments;
		for (int segment = 0; segment < segments; segment++) {
			int color = segment < Math.ceil(value * segments) ? filled : 0xFF4B403B;
			graphics.fill(x + segment * segmentWidth, y, x + segment * segmentWidth + segmentWidth - 2, y + 9, color);
		}
	}

	private void drawFactionLogo(GuiGraphics graphics, int x, int y) {
		FactionLogo logo = FactionLogo.decode(menu.getFactionLogo());
		for (int py = 0; py < 26; py++) {
			for (int px = 0; px < 26; px++) {
				int color = logo.get(px * FactionLogo.SIZE / 26, py * FactionLogo.SIZE / 26);
				if ((color >>> 24) != 0) graphics.fill(x + px, y + py, x + px + 1, y + py + 1, color);
			}
		}
	}
}
