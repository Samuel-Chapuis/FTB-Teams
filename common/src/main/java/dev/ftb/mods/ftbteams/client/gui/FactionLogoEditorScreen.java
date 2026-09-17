package dev.ftb.mods.ftbteams.client.gui;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftblibrary.config.ColorConfig;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.ColorSelectorPanel;
import dev.ftb.mods.ftblibrary.ui.SimpleButton;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.ui.misc.NordColors;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.faction.FactionLogo;
import dev.ftb.mods.ftbteams.net.UpdateFactionLogoMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** A deliberately small, no-texture editor for the faction's 32x32 palette-indexed emblem. */
public class FactionLogoEditorScreen extends BaseScreen implements NordColors {
	private static final int PIXEL_SIZE = 6;
	private static final Icon PEN_ICON = Icon.getIcon(FTBTeamsAPI.rl("textures/pen.png"));
	private static final Icon ERASER_ICON = Icon.getIcon(FTBTeamsAPI.rl("textures/eraser.png"));

	private enum Tool {
		PENCIL,
		ERASER
	}

	private final FactionLogo logo;
	private Color4I selectedColor = Color4I.WHITE;
	private Tool activeTool = Tool.PENCIL;
	private int brushSize = 1;

	public FactionLogoEditorScreen(String currentLogo) {
		setSize(300, 235);
		logo = FactionLogo.decode(currentLogo);
	}

	@Override
	public void addWidgets() {
		add(new Canvas());
		add(new ToolButton(Tool.PENCIL, Component.translatable("ftbteams.gui.logo_tool.pencil"), PEN_ICON, button -> choosePencilColor())
				.setPosAndSize(205, 30, 76, 18));
		add(new ToolButton(Tool.ERASER, Component.translatable("ftbteams.gui.logo_tool.eraser"), ERASER_ICON, button -> activeTool = Tool.ERASER)
				.setPosAndSize(205, 54, 76, 18));

		add(new SimpleButton(this, Component.translatable("ftbteams.gui.clear_logo"), Icons.CANCEL, (button, mouseButton) -> logo.clear())
				.setPosAndSize(205, 84, 36, 18));
		add(new SimpleButton(this, Component.translatable("ftbteams.gui.save_logo"), Icons.ACCEPT, (button, mouseButton) -> {
			NetworkManager.sendToServer(new UpdateFactionLogoMessage(logo.encode()));
			closeGui(true);
		}).setPosAndSize(245, 84, 36, 18));
		add(new BrushSizeSlider().setPosAndSize(201, 116, 94, 28));
	}

	@Override
	public void drawForeground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
		theme.drawString(graphics, Component.translatable("ftbteams.gui.logo_editor"), x + w / 2, y + 8, SNOW_STORM_1, Theme.CENTERED);
		Component mode = Component.translatable(activeTool == Tool.PENCIL ? "ftbteams.gui.logo_tool.pencil" : "ftbteams.gui.logo_tool.eraser");
		graphics.drawString(getMinecraft().font, mode, x + 5, y + 148, activeTool == Tool.PENCIL ? selectedColor.rgba() : SNOW_STORM_1.rgb(), false);
	}

	private void choosePencilColor() {
		activeTool = Tool.PENCIL;
		ColorConfig config = new ColorConfig();
		config.setValue(selectedColor);
		ColorSelectorPanel.popupAtMouse(getGui(), config, accepted -> {
			if (accepted) {
				selectedColor = config.getValue();
			}
		});
	}

	private class ToolButton extends SimpleButton {
		private final Tool tool;

		private ToolButton(Tool tool, Component title, Icon icon, java.util.function.Consumer<SimpleButton> action) {
			super(FactionLogoEditorScreen.this, title, icon, (button, mouseButton) -> action.accept(button));
			this.tool = tool;
		}

		@Override
		public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
			theme.drawButton(graphics, x, y, w, h, getWidgetType());
			int iconSize = Math.min(16, h - 2);
			icon.draw(graphics, x + 2, y + (h - iconSize) / 2, iconSize, iconSize);
			graphics.drawString(getMinecraft().font, title, x + iconSize + 5, y + (h - getMinecraft().font.lineHeight) / 2, SNOW_STORM_1.rgb(), false);
			if (activeTool == tool) {
				SNOW_STORM_1.draw(graphics, x, y, w, 1);
				SNOW_STORM_1.draw(graphics, x, y + h - 1, w, 1);
			}
		}
	}

	private class BrushSizeSlider extends Widget {
		private static final int MIN_SIZE = 1;
		private static final int MAX_SIZE = 8;
		private boolean dragging;

		private BrushSizeSlider() {
			super(FactionLogoEditorScreen.this);
		}

		@Override
		public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
			// Deliberately draw this as a conventional slider rather than an FTB button.
			POLAR_NIGHT_0.draw(graphics, x, y, w, h);
			SNOW_STORM_1.draw(graphics, x, y, w, 1);
			SNOW_STORM_1.draw(graphics, x, y + h - 1, w, 1);

			String label = Component.translatable("ftbteams.gui.logo_brush_size", brushSize).getString();
			int labelWidth = getMinecraft().font.width(label);
			graphics.drawString(getMinecraft().font, label, x + (w - labelWidth) / 2, y + 3, SNOW_STORM_1.rgb(), false);

			int trackStart = x + 8;
			int trackEnd = x + w - 8;
			int trackY = y + h - 8;
			int knobX = trackStart + Math.round((brushSize - MIN_SIZE) * (trackEnd - trackStart) / (float) (MAX_SIZE - MIN_SIZE));
			graphics.fill(trackStart, trackY, trackEnd, trackY + 4, 0xFF2E3440);
			graphics.fill(trackStart, trackY, knobX, trackY + 4, 0xFF88C0D0);
			for (int i = 0; i <= MAX_SIZE - MIN_SIZE; i++) {
				int tickX = trackStart + Math.round(i * (trackEnd - trackStart) / (float) (MAX_SIZE - MIN_SIZE));
				graphics.fill(tickX, trackY - 2, tickX + 1, trackY + 6, 0xFFD8DEE9);
			}
			graphics.fill(knobX - 4, trackY - 4, knobX + 5, trackY + 8, 0xFFECEFF4);
			graphics.fill(knobX - 2, trackY - 2, knobX + 3, trackY + 6, 0xFF5E81AC);
		}

		@Override
		public boolean mousePressed(MouseButton button) {
			if (!isMouseOver() || !button.isLeft()) {
				return false;
			}
			dragging = true;
			updateValue();
			return true;
		}

		@Override
		public boolean mouseDragged(int button, double dragX, double dragY) {
			if (dragging) {
				updateValue();
				return true;
			}
			return false;
		}

		@Override
		public void mouseReleased(MouseButton button) {
			dragging = false;
		}

		private void updateValue() {
			float fraction = Math.max(0F, Math.min(1F, (getMouseX() - getX() - 8F) / (width - 16F)));
			brushSize = MIN_SIZE + Math.round(fraction * (MAX_SIZE - MIN_SIZE));
		}
	}

	private class Canvas extends Widget {
		private Canvas() {
			super(FactionLogoEditorScreen.this);
			setPosAndSize(5, 28, FactionLogo.SIZE * PIXEL_SIZE, FactionLogo.SIZE * PIXEL_SIZE);
		}

		@Override
		public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
			graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, POLAR_NIGHT_0.rgb());
			for (int py = 0; py < FactionLogo.SIZE; py++) {
				for (int px = 0; px < FactionLogo.SIZE; px++) {
					int color = logo.get(px, py);
					if (color == 0) {
						color = ((px + py) & 1) == 0 ? 0xFF4C566A : 0xFF3B4252;
					}
					graphics.fill(x + px * PIXEL_SIZE, y + py * PIXEL_SIZE, x + (px + 1) * PIXEL_SIZE, y + (py + 1) * PIXEL_SIZE, color);
				}
			}
		}

		@Override
		public boolean mousePressed(MouseButton button) {
			return paint(button);
		}

		@Override
		public boolean mouseDragged(int button, double dragX, double dragY) {
			return paint(MouseButton.get(button));
		}

		private boolean paint(MouseButton button) {
			if (!isMouseOver()) {
				return false;
			}
			int pixelX = (getMouseX() - getX()) / PIXEL_SIZE;
			int pixelY = (getMouseY() - getY()) / PIXEL_SIZE;
			if (pixelX >= 0 && pixelX < FactionLogo.SIZE && pixelY >= 0 && pixelY < FactionLogo.SIZE) {
				int color = activeTool == Tool.ERASER ? 0 : selectedColor.rgba();
				int startOffset = -(brushSize - 1) / 2;
				for (int brushY = 0; brushY < brushSize; brushY++) {
					for (int brushX = 0; brushX < brushSize; brushX++) {
						int targetX = pixelX + startOffset + brushX;
						int targetY = pixelY + startOffset + brushY;
						if (targetX >= 0 && targetX < FactionLogo.SIZE && targetY >= 0 && targetY < FactionLogo.SIZE) {
							logo.set(targetX, targetY, color);
						}
					}
				}
			}
			return true;
		}
	}
}
