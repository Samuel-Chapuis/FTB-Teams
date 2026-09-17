package dev.ftb.mods.ftbteams.net;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.TeamRank;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import dev.ftb.mods.ftbteams.data.AbstractTeam;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** Client request for an owner to replace their party's faction logo. */
public record UpdateFactionLogoMessage(String logo) implements CustomPacketPayload {
	public static final Type<UpdateFactionLogoMessage> TYPE = new Type<>(FTBTeamsAPI.rl("update_faction_logo"));
	public static final StreamCodec<RegistryFriendlyByteBuf, UpdateFactionLogoMessage> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(TeamProperties.FACTION_LOGO_ENCODED_LENGTH), UpdateFactionLogoMessage::logo,
			UpdateFactionLogoMessage::new
	);

	public static void handle(UpdateFactionLogoMessage message, NetworkManager.PacketContext context) {
		context.queue(() -> {
			ServerPlayer player = (ServerPlayer) context.getPlayer();
			FTBTeamsAPI.api().getManager().getTeamForPlayer(player).ifPresent(team -> {
				if (team instanceof AbstractTeam abstractTeam
						&& team.isPartyTeam()
						&& team.getRankForPlayer(player.getUUID()).isAtLeast(TeamRank.OWNER)
						&& TeamProperties.FACTION_LOGO.fromString(message.logo).isPresent()) {
					abstractTeam.setProperty(TeamProperties.FACTION_LOGO, message.logo);
					abstractTeam.syncOnePropertyToAll(player.server, TeamProperties.FACTION_LOGO, message.logo);
				}
			});
		});
	}

	@Override
	public Type<UpdateFactionLogoMessage> type() {
		return TYPE;
	}
}
