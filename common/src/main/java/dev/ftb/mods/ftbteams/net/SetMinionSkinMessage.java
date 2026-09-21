package dev.ftb.mods.ftbteams.net;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.world.entity.MinionEntity;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public record SetMinionSkinMessage(int entityId, String playerId) implements CustomPacketPayload {
	public static final Type<SetMinionSkinMessage> TYPE = new Type<>(FTBTeamsAPI.rl("set_minion_skin"));
	public static final StreamCodec<FriendlyByteBuf, SetMinionSkinMessage> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, SetMinionSkinMessage::entityId,
		ByteBufCodecs.stringUtf8(64), SetMinionSkinMessage::playerId,
		SetMinionSkinMessage::new);

	public static void handle(SetMinionSkinMessage message, NetworkManager.PacketContext context) {
		context.queue(() -> {
			if (!(context.getPlayer() instanceof ServerPlayer player) || !(player.containerMenu instanceof dev.ftb.mods.ftbteams.world.inventory.MinionMenu menu)
					|| menu.getMinionId() != message.entityId() || !(player.level().getEntity(message.entityId()) instanceof MinionEntity minion)
					|| !minion.canAccess(player)) return;
			String name = message.playerId().trim();
			if (name.length() < 1 || name.length() > 16 || !name.matches("[A-Za-z0-9_]+")) {
				minion.setCustomSkin("", null);
				return;
			}
			var onlineProfile = player.getServer().getPlayerList().getPlayers().stream()
					.filter(other -> other.getGameProfile().getName().equalsIgnoreCase(name))
					.findFirst().map(other -> other.getGameProfile());
			var profile = onlineProfile.or(() -> player.getServer().getProfileCache().get(name));
			profile.ifPresentOrElse(found -> minion.setCustomSkin(name, found.getId()),
					() -> minion.setCustomSkin("", null));
		});
	}

	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
