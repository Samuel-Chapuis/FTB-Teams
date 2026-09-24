package dev.ftb.mods.ftbteams.world.block;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.ftb.mods.ftbteams.world.block.enclosure.EnclosureScanner.*;

/** Dependency-free regression tests; also runnable without bootstrapping Minecraft. */
public final class EnclosureScannerTest {
	private static final Position ORIGIN = new Position(0, 0, 0);
	private static final List<Position> SEEDS = List.of(new Position(0, 1, 0));
	private static int assertions;

	public static void main(String[] args) {
		Result closed = scan(ORIGIN, SEEDS, 15, EnclosureScannerTest::room);
		expect(closed.status() == Status.SEALED && closed.volume() == 27, "Closed 3x3x3 room has 27 air blocks");
		expect(closed.boundary().size() == 54, "Preview contains precisely the six inner boundary surfaces");
		expect(closed.interior().size() == 27 && closed.interior().stream().allMatch(p -> room(p) == Cell.AIR
				&& Math.abs(p.x()) < 2 && Math.abs(p.z()) < 2 && p.y() > 0 && p.y() < 4),
				"Spawn candidates contain only air inside the validated house");
		expect(scan(ORIGIN, SEEDS, 15, p -> Cell.AIR).interior().isEmpty(), "An open room offers no spawn candidates");
		expect(closed.boundary().contains(new Position(0, 0, 0)) && closed.boundary().contains(new Position(0, 4, 0)),
				"Preview includes floor and roof for cutaway rendering");
		expect(!closed.boundary().contains(new Position(0, 1, 0)) && !closed.boundary().contains(new Position(5, 1, 0)),
				"Preview excludes air and unrelated exterior blocks");
		expect(scan(ORIGIN, SEEDS, 15, p -> Cell.AIR).boundary().isEmpty(), "Failed checks discard preview geometry");

		for (Position hole : List.of(new Position(2, 1, 0), new Position(-2, 1, 0),
				new Position(0, 0, 0), new Position(0, 4, 0), new Position(0, 1, 2), new Position(0, 1, -2))) {
			expect(scan(ORIGIN, SEEDS, 15, p -> p.equals(hole) ? Cell.AIR : room(p)).status() == Status.OUT_OF_RANGE,
					"A hole in each wall, the floor or the roof must fail: " + hole);
		}
		expect(scan(ORIGIN, SEEDS, 15, p -> p.equals(new Position(2, 4, 2)) ? Cell.AIR : room(p)).status() == Status.SEALED,
				"Diagonal contact does not create a six-neighbor opening");
		expect(scan(ORIGIN, List.of(SEEDS.getFirst(), SEEDS.getFirst(), new Position(1, 1, 0)), 15,
				EnclosureScannerTest::room).volume() == 27, "Overlapping seed regions are counted once");
		expect(scan(ORIGIN, SEEDS, 15, p -> Cell.SOLID).status() == Status.NO_INTERIOR, "Solid seeds are not a room");
		expect(scan(ORIGIN, SEEDS, 15, p -> p.equals(new Position(1, 1, 0)) ? Cell.UNAVAILABLE : room(p)).status() == Status.UNAVAILABLE,
				"Unloaded cells never produce a sealed result");
		expect(scan(ORIGIN, List.of(SEEDS.getFirst(), new Position(8, 1, 0)), 15, EnclosureScannerTest::room).status() == Status.OUT_OF_RANGE,
				"An exposed second half cannot be hidden by a closed first half");
		expect(scan(ORIGIN, SEEDS, 15, p -> maxDistance(p) == 15 ? Cell.SOLID : Cell.AIR).volume() == 29 * 29 * 29,
				"Walls at the exact radius are accepted");
		expect(scan(ORIGIN, SEEDS, 15, p -> maxDistance(p) == 16 ? Cell.SOLID : Cell.AIR).status() == Status.OUT_OF_RANGE,
				"Walls beyond the radius are not assumed to be closed");
		AtomicInteger reads = new AtomicInteger();
		expect(scan(ORIGIN, SEEDS, 15, p -> {
			expect(maxDistance(p) <= 15, "Never read outside the allowed radius");
			reads.incrementAndGet();
			return Cell.AIR;
		}).status() == Status.OUT_OF_RANGE && reads.get() <= 31 * 31 * 31, "Open-world search has a fixed upper bound");
		Position offset = new Position(-100, -50, 200);
		expect(scan(offset, List.of(offset.offset(0, 1, 0)), 15,
				p -> room(new Position(p.x() - offset.x(), p.y() - offset.y(), p.z() - offset.z()))).volume() == 27,
				"Negative coordinates and nonzero world origins work");
		expect(scan(ORIGIN, SEEDS, 15, p -> {
			if (p.equals(new Position(1, 2, 0))) return Cell.SOLID;
			return room(p);
		}).volume() == 26, "Furniture / any non-air cell is excluded from the air volume");
		System.out.println("Enclosure scanner regression tests passed (" + assertions + " assertions).");
	}

	private static Cell room(Position p) {
		boolean insideShell = Math.abs(p.x()) <= 2 && Math.abs(p.z()) <= 2 && p.y() >= 0 && p.y() <= 4;
		boolean wall = Math.abs(p.x()) == 2 || Math.abs(p.z()) == 2 || p.y() == 0 || p.y() == 4;
		return insideShell && wall ? Cell.SOLID : Cell.AIR;
	}

	private static int maxDistance(Position p) {
		return Math.max(Math.max(Math.abs(p.x()), Math.abs(p.y())), Math.abs(p.z()));
	}

	private static void expect(boolean condition, String message) {
		assertions++;
		if (!condition) throw new AssertionError(message);
	}
}
