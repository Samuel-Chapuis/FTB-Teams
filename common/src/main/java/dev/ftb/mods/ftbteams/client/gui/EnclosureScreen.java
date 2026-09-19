package dev.ftb.mods.ftbteams.client.gui;

import dev.ftb.mods.ftbteams.world.block.EnclosureScanner;
import dev.ftb.mods.ftbteams.world.block.EnclosureBlock;
import dev.ftb.mods.ftbteams.world.inventory.EnclosureMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/** Compact container layout following the grey controls / orange storage / green preview reference. */
public class EnclosureScreen extends AbstractContainerScreen<EnclosureMenu> {
	private static final int PREVIEW_X = 130, PREVIEW_Y = 24, PREVIEW_WIDTH = 184, PREVIEW_HEIGHT = 84;
	private final EnclosurePreviewRenderer previewRenderer = new EnclosurePreviewRenderer();
	private CutSlider cutSlider;
	private Button resetView;
	private boolean draggingPreview;
	private boolean panningPreview;
	public EnclosureScreen(EnclosureMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = 320;
		imageHeight = 238;
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(Button.builder(Component.translatable("ftbteams.enclosure.check"), button -> {
			if (minecraft != null && minecraft.gameMode != null) {
				minecraft.gameMode.handleInventoryButtonClick(menu.containerId, EnclosureMenu.CHECK_BUTTON);
			}
		}).bounds(leftPos + 7, topPos + 8, 67, 20)
				.tooltip(Tooltip.create(Component.translatable("ftbteams.enclosure.check_tooltip"))).build());
		cutSlider = addRenderableWidget(new CutSlider(leftPos + 132, topPos + 113));
		resetView = addRenderableWidget(Button.builder(Component.literal("R"), button -> {
			previewRenderer.reset();
			cutSlider.sync();
		}).bounds(leftPos + 294, topPos + 6, 18, 16)
				.tooltip(Tooltip.create(Component.translatable("ftbteams.enclosure.preview_controls"))).build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		cutSlider.active = resetView.active = !menu.getPreview().blocks().isEmpty();
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
		if (menu.hasStorage()) {
			boolean input = hoveredSlot != null && !hoveredSlot.hasItem() && menu.isInputSlot(hoveredSlot.index);
			boolean output = hoveredSlot != null && !hoveredSlot.hasItem() && menu.isOutputSlot(hoveredSlot.index);
			if (mouseX >= leftPos + 105 && mouseX < leftPos + 117) {
				input |= mouseY >= topPos + 40 && mouseY < topPos + 111;
				output |= mouseY >= topPos + 116 && mouseY < topPos + 133;
			}
			if (input || output) {
				graphics.renderTooltip(font, font.split(Component.translatable("ftbteams.enclosure." + (input ? "inputs" : "outputs")), 220), mouseX, mouseY);
			}
		}
		if (mouseX >= leftPos + 77 && mouseX < leftPos + 118 && mouseY >= topPos + 3 && mouseY < topPos + 32) {
			graphics.renderTooltip(font, font.split(statusDetail(), 220), mouseX, mouseY);
		}
		if (mouseX >= leftPos + 130 && mouseX < leftPos + 145 && mouseY >= topPos + 96 && mouseY < topPos + 109
				&& (previewRenderer.isSimplified() || previewRenderer.isTruncated())) {
			graphics.renderTooltip(font, font.split(Component.translatable(previewRenderer.isTruncated()
					? "ftbteams.enclosure.preview_limited" : "ftbteams.enclosure.preview_simplified"), 220), mouseX, mouseY);
		}
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.pose().pushPose();
		graphics.pose().translate(leftPos, topPos, 0);
		panel(graphics, 0, 0, 120, 33, 0xFF77797C, 0xFF303235);
		panel(graphics, 0, 35, 120, 102, 0xFFEC7137, 0xFF713C27);
		panel(graphics, 124, 0, 196, 137, 0xFF36A45F, 0xFF082E22);
		panel(graphics, 68, 138, 184, 100, 0xFF77797C, 0xFF303235);
		if (menu.hasStorage()) {
			for (int slot = 0; slot < EnclosureBlock.STORAGE_SIZE; slot++) {
				drawSlot(graphics, EnclosureMenu.STORAGE_X + slot % 5 * 18, EnclosureMenu.storageY(slot), 0xFF725545);
			}
			graphics.fill(9, 112, 110, 114, 0xFFC05D30);
		}
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				drawSlot(graphics, EnclosureMenu.PLAYER_X + col * 18, EnclosureMenu.PLAYER_Y + row * 18, 0xFF55585A);
			}
		}
		for (int col = 0; col < 9; col++) {
			drawSlot(graphics, EnclosureMenu.PLAYER_X + col * 18, EnclosureMenu.PLAYER_Y + 58, 0xFF55585A);
		}
		graphics.pose().popPose();
		previewRenderer.render(graphics, menu.getPreview(), menu.getOrigin(),
				leftPos + PREVIEW_X, topPos + PREVIEW_Y, PREVIEW_WIDTH, PREVIEW_HEIGHT);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawCenteredString(font, Component.translatable("ftbteams.enclosure.sealed"), 96, 6, 0xFFDDDDDD);
		EnclosureScanner.Status status = menu.getStatus();
		String key = switch (status) {
			case SEALED -> "yes";
			case UNCHECKED -> "unknown";
			default -> "no";
		};
		int color = status == EnclosureScanner.Status.SEALED ? 0xFF68EE77 : status == EnclosureScanner.Status.UNCHECKED ? 0xFFCCCCCC : 0xFFFFA15A;
		graphics.drawCenteredString(font, Component.translatable("ftbteams.enclosure." + key), 96, 19, color);
		graphics.drawString(font, title, 134, 10, 0xFFD7F4DF);
		if (menu.getPreview().blocks().isEmpty()) {
			graphics.drawWordWrap(font, menu.getStatus() == EnclosureScanner.Status.SEALED
					? Component.translatable("ftbteams.enclosure.preview_loading") : statusDetail(), 136, 40, 173, 0xFFB8D2BF);
		} else if (previewRenderer.isSimplified() || previewRenderer.isTruncated()) {
			graphics.drawString(font, "*", 134, 99, 0xFFFFD1A6);
		}
		if (!menu.hasStorage()) {
			graphics.drawWordWrap(font, Component.translatable("ftbteams.enclosure.no_storage"), 12, 68, 96, 0xFFEBC9B4);
		} else {
			graphics.pose().pushPose();
			graphics.pose().translate(105, 71, 0);
			graphics.pose().scale(0.6F, 0.6F, 1);
			graphics.drawString(font, "IN", 0, 0, 0xFFFFD1A6);
			graphics.drawString(font, "OUT", 0, 83, 0xFFFFD1A6);
			graphics.pose().popPose();
		}
		graphics.drawString(font, playerInventoryTitle, EnclosureMenu.PLAYER_X, 141, 0xFFDDDDDD);
	}

	private Component statusDetail() {
		return Component.translatable("ftbteams.enclosure.status." + menu.getStatus().name().toLowerCase(Locale.ROOT), menu.getVolume());
	}

	private boolean overPreview(double mouseX, double mouseY) {
		return !menu.getPreview().blocks().isEmpty() && mouseX >= leftPos + PREVIEW_X && mouseX < leftPos + PREVIEW_X + PREVIEW_WIDTH
				&& mouseY >= topPos + PREVIEW_Y && mouseY < topPos + PREVIEW_Y + PREVIEW_HEIGHT;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (overPreview(mouseX, mouseY) && (button == 0 || button == 1)) {
			draggingPreview = true;
			panningPreview = button == 1 || hasShiftDown();
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
		if (draggingPreview) {
			if (panningPreview) previewRenderer.pan(dx, dy);
			else previewRenderer.rotate(dx, dy);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dx, dy);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (draggingPreview) {
			draggingPreview = false;
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		if (overPreview(mouseX, mouseY)) {
			if (hasShiftDown()) {
				previewRenderer.setCut(previewRenderer.getCut() + (float) vertical * 0.03F);
				cutSlider.sync();
			} else previewRenderer.zoom(vertical);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
	}

	private final class CutSlider extends AbstractSliderButton {
		private CutSlider(int x, int y) {
			super(x, y, 180, 18, Component.empty(), previewRenderer.getCut());
			setTooltip(Tooltip.create(Component.translatable("ftbteams.enclosure.cut_tooltip")));
			updateMessage();
		}

		private void sync() {
			value = previewRenderer.getCut();
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.translatable("ftbteams.enclosure.cut", Math.round(value * 100)));
		}

		@Override
		protected void applyValue() {
			previewRenderer.setCut((float) value);
		}
	}

	private static void panel(GuiGraphics graphics, int x, int y, int width, int height, int edge, int fill) {
		graphics.fill(x, y, x + width, y + height, 0xFF101713);
		graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, edge);
		graphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, fill);
		graphics.fill(x + 4, y + height - 4, x + width - 3, y + height - 2, 0xFF19211C);
	}

	private static void drawSlot(GuiGraphics graphics, int x, int y, int fill) {
		graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF261F1B);
		graphics.fill(x, y, x + 17, y + 17, 0xFFD2BDAE);
		graphics.fill(x, y, x + 16, y + 16, fill);
	}
}
