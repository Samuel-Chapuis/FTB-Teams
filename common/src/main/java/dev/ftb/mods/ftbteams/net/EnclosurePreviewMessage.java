package dev.ftb.mods.ftbteams.net;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.client.FTBTeamsClient;
import dev.ftb.mods.ftbteams.world.block.EnclosurePreview;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record EnclosurePreviewMessage(int containerId, BlockPos origin, EnclosurePreview preview) implements CustomPacketPayload {
	public static final Type<EnclosurePreviewMessage> TYPE = new Type<>(FTBTeamsAPI.rl("enclosure_preview"));
	public static final StreamCodec<RegistryFriendlyByteBuf, EnclosurePreviewMessage> STREAM_CODEC = new StreamCodec<>() {
		@Override
		public EnclosurePreviewMessage decode(RegistryFriendlyByteBuf buf) {
			return new EnclosurePreviewMessage(buf.readVarInt(), buf.readBlockPos(), EnclosurePreview.read(buf));
		}

		@Override
		public void encode(RegistryFriendlyByteBuf buf, EnclosurePreviewMessage message) {
			buf.writeVarInt(message.containerId());
			buf.writeBlockPos(message.origin());
			message.preview().write(buf);
		}
	};

	public static void handle(EnclosurePreviewMessage message, NetworkManager.PacketContext context) {
		context.queue(() -> FTBTeamsClient.updateEnclosurePreview(message));
	}

	@Override
	public Type<EnclosurePreviewMessage> type() {
		return TYPE;
	}
}
