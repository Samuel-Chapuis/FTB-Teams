package dev.ftb.mods.ftbteams.config;

import dev.ftb.mods.ftblibrary.snbt.config.BooleanValue;
import dev.ftb.mods.ftblibrary.snbt.config.SNBTConfig;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;

/** Per-client visual preferences. These never affect server data or other players. */
public interface ClientConfig {
	String KEY = FTBTeamsAPI.MOD_ID + "-client";

	SNBTConfig CONFIG = SNBTConfig.create(KEY)
			.comment("Client-specific settings for FTB Teams");

	BooleanValue SHOW_FACTION_HUD_LOGO = CONFIG.addBoolean("show_faction_hud_logo", true)
			.comment("Show the faction logo as an extra slot next to the hotbar.");
}
