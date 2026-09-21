package dev.ftb.mods.ftbteams.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientRawInputEvent;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.ftb.mods.ftblibrary.api.sidebar.SidebarButtonCreatedEvent;
import dev.ftb.mods.ftblibrary.config.manager.ConfigManager;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.ui.CustomClickEvent;
import dev.ftb.mods.ftblibrary.util.client.ClientUtils;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamMessage;
import dev.ftb.mods.ftbteams.api.client.KnownClientPlayer;
import dev.ftb.mods.ftbteams.api.faction.FactionLogo;
import dev.ftb.mods.ftbteams.api.faction.FactionProperties;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import dev.ftb.mods.ftbteams.api.property.TeamPropertyCollection;
import dev.ftb.mods.ftbteams.client.gui.MyTeamScreen;
import dev.ftb.mods.ftbteams.config.ClientConfig;
import dev.ftb.mods.ftbteams.config.ServerConfig;
import dev.ftb.mods.ftbteams.data.ClientTeamManagerImpl;
import dev.ftb.mods.ftbteams.data.PlayerPermissions;
import dev.ftb.mods.ftbteams.net.OpenGUIMessage;
import dev.ftb.mods.ftbteams.net.EnclosurePreviewMessage;
import dev.ftb.mods.ftbteams.world.inventory.EnclosureMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FTBTeamsClient {
	public static void updateEnclosurePreview(EnclosurePreviewMessage message) {
		var player = Minecraft.getInstance().player;
		if (player != null && player.containerMenu instanceof EnclosureMenu menu
				&& menu.containerId == message.containerId() && menu.getOrigin().equals(message.origin())) {
			menu.setPreview(message.preview());
		}
	}

	public static final ResourceLocation OPEN_GUI_ID = FTBTeamsAPI.rl("open_gui");
	public static final ResourceLocation TEAM_LIVES_ID = FTBTeamsAPI.rl("team_lives");
	private static final int FACTION_LOGO_SLOT_SIZE = 22;
	private static final int FACTION_LOGO_ICON_SIZE = 18;

	public static KeyMapping openTeamsKey;
	private static boolean chatRedirected = false;

	public static void init() {
		ConfigManager.getInstance().registerClientConfig(ClientConfig.CONFIG, FTBTeamsAPI.MOD_ID + ".config.client");
		registerKeys();

		CustomClickEvent.EVENT.register(event -> {
			if (event.id().equals(OPEN_GUI_ID)) {
				OpenGUIMessage.sendToServer();
				return EventResult.interruptTrue();
			}
			return EventResult.pass();
		});

		ClientRawInputEvent.KEY_PRESSED.register(FTBTeamsClient::keyPressed);
		ClientGuiEvent.RENDER_HUD.register(FTBTeamsClient::renderFactionLogo);

		SidebarButtonCreatedEvent.EVENT.register(FTBTeamsClient::onSidebarButtonCreated);
	}

	private static void onSidebarButtonCreated(SidebarButtonCreatedEvent event) {
		if (event.getButton().getId().equals(TEAM_LIVES_ID)) {
			event.getButton().addVisibilityCondition(() -> ServerConfig.limitedLives().isPresent() && FTBTeamsAPI.api().getClientManager().selfTeam().isPartyTeam());
			event.getButton().setTooltipOverride(FTBTeamsClient::addLivesIconTooltip);
			event.getButton().addOverlayRender(FTBTeamsClient::renderLivesIconOverlay);
		}
	}

	private static void registerKeys() {
		openTeamsKey = new KeyMapping("key.ftbteams.open_gui", InputConstants.Type.KEYSYM, -1, "key.categories.ftbteams");
		KeyMappingRegistry.register(openTeamsKey);
	}

	private static EventResult keyPressed(Minecraft client, int keyCode, int scanCode, int action, int modifiers) {
		if (openTeamsKey.isDown()) {
			OpenGUIMessage.sendToServer();
			return EventResult.interruptTrue();
		}
		return EventResult.pass();
	}

	public static void openMyTeamGui(TeamPropertyCollection properties, PlayerPermissions permissions) {
		new MyTeamScreen(properties, permissions).openGui();
	}

	public static void updateSettings(UUID id, TeamPropertyCollection properties) {
		ClientTeamManagerImpl.ifPresent(mgr -> mgr.getTeam(id).ifPresent(team -> team.updateProperties(properties)));
	}

	public static void sendMessage(UUID from, Component text) {
		ClientTeamManagerImpl.ifPresent(mgr -> {
			TeamMessage msg = FTBTeamsAPI.api().createMessage(from, text);
			mgr.selfTeam().addMessage(msg);

			MyTeamScreen screen = ClientUtils.getCurrentGuiAs(MyTeamScreen.class);
			if (screen != null) {
				screen.refreshChat();
			}
		});
	}

	public static void updatePresence(KnownClientPlayer update) {
		ClientTeamManagerImpl.ifPresent(mgr -> mgr.updatePresence(update));
	}

	public static void setChatRedirected(boolean chatRedirected) {
		FTBTeamsClient.chatRedirected = chatRedirected;
	}

	public static boolean isChatRedirected() {
		return chatRedirected;
	}

	public static List<Component> addLivesIconTooltip() {
		Team team = FTBTeamsAPI.api().getClientManager().selfTeam();
		List<Component> res = new ArrayList<>();
		int lives = team.getProperty(TeamProperties.LIVES_REMAINING);
		res.add(Component.translatable("ftbteams.message.limited_lives", lives, ServerConfig.LIMITED_LIVES.get()));
		if (lives == 0) {
			res.add(Component.translatable("ftbteams.message.limited_lives.warn").withStyle(ChatFormatting.RED));
		}
		return res;
	}

	public static void renderLivesIconOverlay(GuiGraphics graphics, Font font, int buttonSize) {
		String text = String.valueOf(FTBTeamsAPI.api().getClientManager().selfTeam().getProperty(TeamProperties.LIVES_REMAINING));
		if (!text.isEmpty()) {
			var nw = font.width(text);
			graphics.pose().pushPose();
			graphics.pose().translate(buttonSize - nw, buttonSize - font.lineHeight, 0);
			graphics.pose().scale(0.75f, 0.75f, 0.75f);
			Color4I.rgb(0xFF208020).draw(graphics, 0, 0, nw + 1, font.lineHeight);
			graphics.drawString(font, text, 1, 1, 0xFFFFFFFF);
			graphics.pose().popPose();
		}
	}

	public static boolean shouldShowFactionLogo() {
		return ClientConfig.SHOW_FACTION_HUD_LOGO.get();
	}

	public static void toggleFactionLogoVisibility() {
		ClientConfig.SHOW_FACTION_HUD_LOGO.toggle();
		ConfigManager.getInstance().save(ClientConfig.KEY);
	}

	/** Draw the player's faction mark next to the vanilla HUD without allocating a dynamic texture every frame. */
	private static void renderFactionLogo(GuiGraphics graphics, DeltaTracker tickDelta) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.options.hideGui || !shouldShowFactionLogo() || minecraft.player == null || ClientTeamManagerImpl.getInstance() == null) {
			return;
		}

		Team team = FTBTeamsAPI.api().getClientManager().selfTeam();
		if (!FactionProperties.isFaction(team)) {
			return;
		}

		int slotX = minecraft.getWindow().getGuiScaledWidth() / 2 + 91;
		int slotY = minecraft.getWindow().getGuiScaledHeight() - FACTION_LOGO_SLOT_SIZE;
		drawHotbarSlot(graphics, slotX, slotY);

		FactionLogo logo = FactionProperties.logo(team);
		for (int py = 0; py < FACTION_LOGO_ICON_SIZE; py++) {
			for (int px = 0; px < FACTION_LOGO_ICON_SIZE; px++) {
				int color = logo.get(px * FactionLogo.SIZE / FACTION_LOGO_ICON_SIZE, py * FactionLogo.SIZE / FACTION_LOGO_ICON_SIZE);
				if ((color >>> 24) != 0) {
					graphics.fill(slotX + 2 + px, slotY + 2 + py, slotX + 3 + px, slotY + 3 + py, color);
				}
			}
		}
	}

	private static void drawHotbarSlot(GuiGraphics graphics, int x, int y) {
		graphics.fill(x, y, x + FACTION_LOGO_SLOT_SIZE, y + FACTION_LOGO_SLOT_SIZE, 0xFF202020);
		graphics.fill(x + 1, y + 1, x + FACTION_LOGO_SLOT_SIZE - 1, y + FACTION_LOGO_SLOT_SIZE - 1, 0xFFB0B0B0);
		graphics.fill(x + 2, y + 2, x + FACTION_LOGO_SLOT_SIZE - 2, y + FACTION_LOGO_SLOT_SIZE - 2, 0xFF555555);
	}

}
