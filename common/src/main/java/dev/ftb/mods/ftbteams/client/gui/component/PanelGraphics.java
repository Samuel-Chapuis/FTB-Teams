package dev.ftb.mods.ftbteams.client.gui.component;

import dev.ftb.mods.ftbteams.api.faction.FactionLogo;
import net.minecraft.client.gui.GuiGraphics;

/** Reusable drawing primitives shared by the enclosure and minion interfaces. */
public final class PanelGraphics {
	private static final int SHADOW = 0xFF101713;
	private static final int LOWER_EDGE = 0xFF19211C;

	private PanelGraphics() {
	}

	/** Draws a framed panel with a four-pixel inner border. */
	public static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height, int edge, int fill) {
		graphics.fill(x, y, x + width, y + height, SHADOW);
		graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, edge);
		graphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, fill);
		graphics.fill(x + 4, y + height - 4, x + width - 3, y + height - 2, LOWER_EDGE);
	}

	/** Draws one inventory slot at the requested outer size. */
	public static void drawSlot(GuiGraphics graphics, int x, int y, int size, int fill) {
		drawSlot(graphics, x, y, size, fill, 1);
	}

	/** Draws one slot with a configurable inset for its fill color. */
	public static void drawSlot(GuiGraphics graphics, int x, int y, int size, int fill, int inset) {
		graphics.fill(x - 1, y - 1, x + size, y + size, 0xFF261F1B);
		graphics.fill(x, y, x + size, y + size, 0xFFD2BDAE);
		graphics.fill(x + inset, y + inset, x + size - 1, y + size - 1, fill);
	}

	/** Draws a discrete value bar such as health or food. */
	public static void drawSegmentedBar(GuiGraphics graphics, int x, int y, int width, float value, int filled, int segments) {
		int segmentWidth = width / segments;
		int filledSegments = (int) Math.ceil(Math.clamp(value, 0F, 1F) * segments);
		for (int segment = 0; segment < segments; segment++) {
			int color = segment < filledSegments ? filled : 0xFF4B403B;
			graphics.fill(x + segment * segmentWidth, y, x + segment * segmentWidth + segmentWidth - 2, y + 9, color);
		}
	}

	/** Draws the encoded faction logo with nearest-neighbour scaling. */
	public static void drawFactionLogo(GuiGraphics graphics, String encodedLogo, int x, int y, int size) {
		FactionLogo logo = FactionLogo.decode(encodedLogo);
		for (int py = 0; py < size; py++) {
			for (int px = 0; px < size; px++) {
				int color = logo.get(px * FactionLogo.SIZE / size, py * FactionLogo.SIZE / size);
				if ((color >>> 24) != 0) {
					graphics.fill(x + px, y + py, x + px + 1, y + py + 1, color);
				}
			}
		}
	}
}
