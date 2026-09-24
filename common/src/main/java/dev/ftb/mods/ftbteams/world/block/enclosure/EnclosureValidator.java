package dev.ftb.mods.ftbteams.world.block.enclosure;

import dev.ftb.mods.ftbteams.world.block.EnclosureBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Performs a complete enclosure scan and builds the matching client preview snapshot. */
public final class EnclosureValidator {
	private EnclosureValidator() {
	}

	public static Result validate(ServerLevel level, EnclosureBlock block, BlockState state, BlockPos origin) {
		var seeds = block.getInteriorSeeds(state, origin).stream().map(EnclosureValidator::position).toList();
		var scan = EnclosureScanner.scan(position(origin), seeds, EnclosureBlock.SEARCH_RADIUS,
				candidate -> classify(level, candidate));
		return new Result(scan, EnclosurePreview.capture(level, origin, scan));
	}

	private static EnclosureScanner.Cell classify(ServerLevel level, EnclosureScanner.Position candidate) {
		BlockPos pos = new BlockPos(candidate.x(), candidate.y(), candidate.z());
		if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos) || !level.hasChunkAt(pos)) {
			return EnclosureScanner.Cell.UNAVAILABLE;
		}
		return level.getBlockState(pos).isAir() ? EnclosureScanner.Cell.AIR : EnclosureScanner.Cell.SOLID;
	}

	private static EnclosureScanner.Position position(BlockPos pos) {
		return new EnclosureScanner.Position(pos.getX(), pos.getY(), pos.getZ());
	}

	/** Immutable output of a validation pass. */
	public record Result(EnclosureScanner.Result scan, EnclosurePreview preview) {
	}
}
