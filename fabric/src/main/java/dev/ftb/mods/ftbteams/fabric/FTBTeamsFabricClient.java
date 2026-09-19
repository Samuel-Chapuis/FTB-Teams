package dev.ftb.mods.ftbteams.fabric;

import dev.architectury.registry.menu.MenuRegistry;
import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.client.gui.EnclosureScreen;
import net.fabricmc.api.ClientModInitializer;

public class FTBTeamsFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuRegistry.registerScreenFactory(FTBTeams.ENCLOSURE_MENU.get(), EnclosureScreen::new);
	}
}
