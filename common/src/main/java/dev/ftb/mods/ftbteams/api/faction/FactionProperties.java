package dev.ftb.mods.ftbteams.api.faction;

import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Common, typed access to the RP data stored on party teams. */
public final class FactionProperties {
	private FactionProperties() {
	}

	public static boolean isFaction(Team team) {
		return team.isPartyTeam();
	}

	public static int level(Team team) {
		return team.getProperty(TeamProperties.FACTION_LEVEL);
	}

	public static FactionLogo logo(Team team) {
		return FactionLogo.decode(team.getProperty(TeamProperties.FACTION_LOGO));
	}

	public static @Nullable Capital capital(Team team) {
		String rawDimension = team.getProperty(TeamProperties.FACTION_CAPITAL_DIMENSION);
		ResourceLocation dimension = ResourceLocation.tryParse(rawDimension);
		if (dimension == null) {
			return null;
		}
		return new Capital(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimension), new BlockPos(
				team.getProperty(TeamProperties.FACTION_CAPITAL_X),
				team.getProperty(TeamProperties.FACTION_CAPITAL_Y),
				team.getProperty(TeamProperties.FACTION_CAPITAL_Z)
		));
	}

	public record Capital(ResourceKey<Level> dimension, BlockPos pos) {
	}
}
