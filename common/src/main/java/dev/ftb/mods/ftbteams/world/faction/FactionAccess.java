package dev.ftb.mods.ftbteams.world.faction;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;

/**
 * Centralizes faction ownership checks and the small immutable view sent to building and minion menus.
 */
public final class FactionAccess {
	private static final Profile UNCLAIMED = new Profile("", 0xFF333333, "");

	private FactionAccess() {
	}

	/** Returns the player's party identifier, excluding personal teams. */
	public static Optional<UUID> findPlayerFaction(ServerPlayer player) {
		return FTBTeamsAPI.api().getManager().getTeamForPlayer(player)
				.filter(team -> team.isPartyTeam())
				.map(team -> team.getTeamId());
	}

	/** Allows an unclaimed object, its owner, or a current member of its owning faction. */
	public static boolean canAccess(UUID owner, UUID faction, ServerPlayer player) {
		return owner == null || owner.equals(player.getUUID())
				|| faction != null && findPlayerFaction(player).filter(faction::equals).isPresent();
	}

	/** Resolves display properties in one team-manager lookup. */
	public static Profile resolve(UUID faction) {
		if (faction == null) {
			return UNCLAIMED;
		}
		return FTBTeamsAPI.api().getManager().getTeamByID(faction)
				.map(team -> new Profile(team.getName().getString(), team.getProperty(TeamProperties.COLOR).rgb(),
						team.getProperty(TeamProperties.FACTION_LOGO)))
				.orElse(UNCLAIMED);
	}

	/** Display-only faction data safe to serialize to a client menu. */
	public record Profile(String name, int color, String logo) {
	}
}
