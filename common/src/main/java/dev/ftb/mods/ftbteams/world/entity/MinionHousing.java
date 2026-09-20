package dev.ftb.mods.ftbteams.world.entity;

import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.world.block.EnclosureScanner;
import dev.ftb.mods.ftbteams.world.block.PopBedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public final class MinionHousing {
	public enum Status { NONE, ASSIGNED, MULTIPLE_BEDS, NO_FACTION, OTHER_FACTION, NO_SPACE, SPAWN_FAILED, INVALID_ROOM }

	private MinionHousing() {
	}

	public static Status validate(ServerLevel level, BlockPos home, ServerPlayer player, EnclosureScanner.Result room) {
		if (room.status() != EnclosureScanner.Status.SEALED || !MinionEntity.isHomeBed(level, home)) return Status.INVALID_ROOM;
		Set<BlockPos> beds = new HashSet<>();
		for (var p : room.boundary()) {
			BlockPos pos = new BlockPos(p.x(), p.y(), p.z());
			var state = level.getBlockState(pos);
			if (state.getBlock() instanceof PopBedBlock || state.getBlock() instanceof BedBlock) {
				beds.add(state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD
						? pos : pos.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
			}
		}
		if (beds.stream().anyMatch(bed -> !bed.equals(home))) return Status.MULTIPLE_BEDS;
		var team = FTBTeamsAPI.api().getManager().getTeamForPlayer(player).filter(t -> t.isPartyTeam()).orElse(null);
		if (team == null) return Status.NO_FACTION;
		var population = MinionPopulationData.get(level);
		var resident = population.resident(home);
		if (resident != null) {
			return resident.faction().equals(team.getTeamId()) ? Status.ASSIGNED : Status.OTHER_FACTION;
		}
		MinionEntity minion = FTBTeams.MINION.get().create(level);
		if (minion == null) return Status.SPAWN_FAILED;
		if (!placeInside(level, home, room, minion)) return Status.NO_SPACE;
		minion.assignHome(home, team.getTeamId());
		if (!population.claim(home, minion.getUUID(), team.getTeamId())) return Status.ASSIGNED;
		if (!level.addFreshEntity(minion)) {
			population.release(home, minion.getUUID());
			return Status.SPAWN_FAILED;
		}
		return Status.ASSIGNED;
	}

	private static boolean placeInside(ServerLevel level, BlockPos home, EnclosureScanner.Result room, MinionEntity minion) {
		// Only air cells from this exact room, never an arbitrary position across a wall.
		var candidates = room.interior().stream().map(p -> new BlockPos(p.x(), p.y(), p.z()))
				.sorted(Comparator.comparingDouble(p -> p.distSqr(home))).toList();
		for (BlockPos pos : candidates) {
			var floor = level.getBlockState(pos.below());
			var shape = floor.getCollisionShape(level, pos.below());
			if (shape.isEmpty() || !floor.getFluidState().isEmpty()) continue;
			double y = pos.getY() - 1 + shape.max(Direction.Axis.Y);
			minion.moveTo(pos.getX() + 0.5, y, pos.getZ() + 0.5, level.random.nextFloat() * 360, 0);
			if (level.getWorldBorder().isWithinBounds(minion.getBoundingBox()) && level.noCollision(minion)
					&& !level.containsAnyLiquid(minion.getBoundingBox())) return true;
		}
		return false;
	}
}
