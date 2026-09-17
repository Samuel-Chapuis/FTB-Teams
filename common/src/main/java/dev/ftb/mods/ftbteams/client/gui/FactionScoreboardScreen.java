package dev.ftb.mods.ftbteams.client.gui;

import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.misc.NordColors;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.faction.FactionLogo;
import dev.ftb.mods.ftbteams.api.faction.FactionProperties;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.List;

/** Read-only public directory of party teams, presented as RP factions. */
public class FactionScoreboardScreen extends BaseScreen implements NordColors {
	private static final int ROW_HEIGHT = 36;

	public FactionScoreboardScreen() {
		setSize(530, 250);
	}

	@Override
	public void addWidgets() {
		// This screen is intentionally read-only; rows are rendered directly so each can contain a 32x32 logo.
	}

	@Override
	public void drawForeground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
		theme.drawString(graphics, Component.translatable("ftbteams.gui.faction_scoreboard"), x + w / 2, y + 8, SNOW_STORM_1, Theme.CENTERED);
		int headerY = y + 25;
		drawText(graphics, Component.translatable("ftbteams.faction.table.logo"), x + 8, headerY, 0xFF81A1C1);
		drawText(graphics, Component.translatable("ftbteams.faction.table.name"), x + 47, headerY, 0xFF81A1C1);
		drawText(graphics, Component.translatable("ftbteams.faction.table.level"), x + 190, headerY, 0xFF81A1C1);
		drawText(graphics, Component.translatable("ftbteams.faction.table.members"), x + 245, headerY, 0xFF81A1C1);
		drawText(graphics, Component.translatable("ftbteams.faction.table.capital"), x + 330, headerY, 0xFF81A1C1);

		List<Team> factions = FTBTeamsAPI.api().getClientManager().getTeams().stream()
				.filter(FactionProperties::isFaction)
				.sorted(Comparator.comparingInt(FactionProperties::level).reversed()
						.thenComparing(team -> team.getProperty(TeamProperties.DISPLAY_NAME), String.CASE_INSENSITIVE_ORDER))
				.toList();
		int maxRows = (h - 44) / ROW_HEIGHT;
		for (int i = 0; i < Math.min(factions.size(), maxRows); i++) {
			drawFaction(graphics, factions.get(i), x + 5, y + 40 + i * ROW_HEIGHT, w - 10);
		}
		if (factions.size() > maxRows) {
			drawText(graphics, Component.translatable("ftbteams.faction.table.truncated", factions.size() - maxRows), x + 8, y + h - 12, 0xFFEBCB8B);
		}
	}

	private void drawFaction(GuiGraphics graphics, Team faction, int x, int y, int width) {
		graphics.fill(x, y, x + width, y + ROW_HEIGHT - 2, 0xFF3B4252);
		drawLogo(graphics, FactionProperties.logo(faction), x + 2, y + 1);
		drawText(graphics, Component.literal(faction.getProperty(TeamProperties.DISPLAY_NAME)), x + 42, y + 5, 0xFFECEFF4);
		drawText(graphics, Component.literal(String.valueOf(FactionProperties.level(faction))), x + 202, y + 5, 0xFFECEFF4);
		drawText(graphics, Component.literal(String.valueOf(faction.getMembers().size())), x + 265, y + 5, 0xFFECEFF4);
		FactionProperties.Capital capital = FactionProperties.capital(faction);
		Component capitalText = capital == null
				? Component.translatable("ftbteams.faction.table.no_capital")
				: Component.literal(capital.dimension().location() + " " + capital.pos().getX() + ", " + capital.pos().getY() + ", " + capital.pos().getZ());
		drawText(graphics, capitalText, x + 330, y + 5, capital == null ? 0xFF81A1C1 : 0xFFECEFF4);
	}

	private static void drawLogo(GuiGraphics graphics, FactionLogo logo, int x, int y) {
		for (int py = 0; py < FactionLogo.SIZE; py++) {
			for (int px = 0; px < FactionLogo.SIZE; px++) {
				int color = logo.get(px, py);
				if ((color >>> 24) != 0) {
					graphics.fill(x + px, y + py, x + px + 1, y + py + 1, color);
				}
			}
		}
	}

	private static void drawText(GuiGraphics graphics, Component text, int x, int y, int color) {
		graphics.drawString(Minecraft.getInstance().font, text, x, y, color, false);
	}
}
