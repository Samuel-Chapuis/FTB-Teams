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
import net.minecraft.world.item.ItemStack;
import dev.ftb.mods.ftbteams.net.SetMinionSkinMessage;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.gui.components.EditBox;

/** Visual profile for a minion. It owns no gameplay state; MinionMenu authorizes and supplies its identity. */
public class MinionScreen extends AbstractContainerScreen<MinionMenu> {
	private static final int WIDTH = 320;
	private static final int HEIGHT = 200;
	private EditBox skinIdBox;

	public MinionScreen(MinionMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = WIDTH;
		imageHeight = HEIGHT;
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(Button.builder(Component.translatable("ftbteams.minion.choose_skin"), button -> {
			skinIdBox = addRenderableWidget(new EditBox(font, leftPos + 169, topPos + 145, 124, 18,
					Component.translatable("ftbteams.minion.choose_skin")));
			skinIdBox.setMaxLength(64);
			skinIdBox.setFocused(true);
		}).bounds(leftPos + 169, topPos + 169, 124, 20).tooltip(Tooltip.create(
				Component.translatable("ftbteams.minion.choose_skin_tooltip"))).build());
		addRenderableWidget(Button.builder(Component.literal("×"), button -> {
			if (minecraft != null && minecraft.gameMode != null) {
				minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MinionMenu.CLEAR_PROFESSION_BUTTON);
			}
		}).bounds(leftPos + 294, topPos + 8, 18, 18).tooltip(Tooltip.create(
				Component.translatable("ftbteams.minion.clear_profession_tooltip"))).build()).active = !menu.getProfession().isBlank();
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.pose().pushPose();
		graphics.pose().translate(leftPos, topPos, 0);
		panel(graphics, 0, 0, 138, 45, 0xFF77797C, 0xFF303235);
		panel(graphics, 0, 48, 138, 26, 0xFFEC7137, 0xFF713C27);
		panel(graphics, 0, 77, 138, 123, 0xFFEC7137, 0xFF713C27);
		panel(graphics, 142, 0, 178, 45, 0xFF4F86B8, 0xFF102D45);
		panel(graphics, 142, 48, 178, 152, 0xFF36A45F, 0xFF082E22);
		graphics.fill(13, 7, 45, 39, 0xFF101713);
		drawFactionLogo(graphics, 16, 10);
		graphics.fill(151, 7, 183, 39, 0xFF101713);
		ItemStack professionStack = minion() == null ? menu.getProfessionStack() : minion().getProfessionStack();
		if (!professionStack.isEmpty()) graphics.renderItem(professionStack, 159, 15);
		for (int row = 0; row < 4; row++) {
			for (int column = 0; column < 5; column++) drawSlot(graphics, 13 + column * 24, 86 + row * 21);
		}
		float health = minion() == null ? 0F : minion().getHealth() / minion().getMaxHealth();
		drawBar(graphics, 30, 55, 43, health, 0xFFE53B3B, 5);
		float food = minion() == null ? 0F : (float) minion().getFood() / MinionEntity.MAX_FOOD;
		drawBar(graphics, 96, 55, 30, food, 0xFFE29A38, MinionEntity.MAX_FOOD);
		graphics.pose().popPose();

		MinionEntity minion = minion();
		if (minion != null) {
			InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, leftPos + 158, topPos + 56, leftPos + 304, topPos + 130,
				55, 0.0625F, mouseX, mouseY, minion);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(font, Component.translatable("ftbteams.minion.faction"), 52, 12, 0xFFF1F1F1);
		graphics.drawString(font, menu.getFactionName().isBlank() ? Component.translatable("ftbteams.minion.faction_none")
				: Component.literal(menu.getFactionName()), 52, 27, 0xFFBEBEBE);
		graphics.drawString(font, "\u2665", 13, 54, 0xFFE53B3B);
		graphics.drawString(font, Component.translatable("ftbteams.minion.health"), 13, 65, 0xFFFFD1A6);
		graphics.drawString(font, Component.translatable("ftbteams.minion.food", minion() == null ? 0 : minion().getFood(), MinionEntity.MAX_FOOD),
				76, 65, 0xFFFFD1A6);
		graphics.drawCenteredString(font, Component.translatable(minion() != null && minion().isResting()
				? "ftbteams.minion.resting" : "ftbteams.minion.awake"), 231, 134, 0xFFB8D2BF);
		graphics.drawString(font, Component.translatable("ftbteams.minion.profession"), 192, 12, 0xFFF1F1F1);
		graphics.drawString(font, menu.getProfession().isBlank()
				? Component.translatable("ftbteams.minion.profession_none") : Component.translatable(menu.getProfession()), 192, 27, 0xFFB9D9FF);
		drawCoordinates(graphics, "ftbteams.minion.home", menu.getHome(), 8, 174);
		drawCoordinates(graphics, "ftbteams.minion.workstation", menu.getWorkstation(), 8, 185);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (skinIdBox != null && skinIdBox.isFocused() && keyCode == 257) {
			NetworkManager.sendToServer(new SetMinionSkinMessage(menu.getMinionId(), skinIdBox.getValue()));
			removeWidget(skinIdBox);
			skinIdBox = null;
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private void drawCoordinates(GuiGraphics graphics, String key, net.minecraft.core.BlockPos pos, int x, int y) {
		Component label = Component.translatable(key, pos == null ? "-" : pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
		graphics.pose().pushPose();
		graphics.pose().scale(0.68F, 0.68F, 1F);
		graphics.drawString(font, label, Math.round(x / 0.68F), Math.round(y / 0.68F), 0xFFEBC9B4);
		graphics.pose().popPose();
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
