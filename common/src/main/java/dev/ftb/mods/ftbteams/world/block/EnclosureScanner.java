package dev.ftb.mods.ftbteams.world.block;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Bounded six-neighbor flood fill. Only air is traversable, regardless of collision shapes. */
public final class EnclosureScanner {
	private static final int[][] DIRECTIONS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

	private EnclosureScanner() {
	}

	public enum Cell { AIR, SOLID, UNAVAILABLE }
	public enum Status { UNCHECKED, SEALED, OUT_OF_RANGE, UNAVAILABLE, NO_INTERIOR }
	public record Position(int x, int y, int z) {
		Position offset(int x, int y, int z) {
			return new Position(this.x + x, this.y + y, this.z + z);
		}
	}
	public record Result(Status status, int volume) {
	}
	@FunctionalInterface
	public interface CellLookup {
		Cell get(Position pos);
	}

	public static Result scan(Position origin, List<Position> seeds, int radius, CellLookup lookup) {
		if (radius < 1 || radius > 32) {
			throw new IllegalArgumentException("Enclosure radius must be between 1 and 32");
		}
		Set<Position> visited = new HashSet<>();
		ArrayDeque<Position> pending = new ArrayDeque<>(seeds);
		int volume = 0;
		while (!pending.isEmpty()) {
			Position pos = pending.removeFirst();
			if (!visited.add(pos)) {
				continue;
			}
			if (Math.abs(pos.x - origin.x) > radius || Math.abs(pos.y - origin.y) > radius || Math.abs(pos.z - origin.z) > radius) {
				return new Result(Status.OUT_OF_RANGE, 0);
			}
			Cell cell = lookup.get(pos);
			if (cell == Cell.UNAVAILABLE) {
				return new Result(Status.UNAVAILABLE, 0);
			}
			if (cell == Cell.AIR) {
				volume++;
				for (int[] direction : DIRECTIONS) {
					pending.addLast(pos.offset(direction[0], direction[1], direction[2]));
				}
			}
		}
		return new Result(volume == 0 ? Status.NO_INTERIOR : Status.SEALED, volume);
	}
}
