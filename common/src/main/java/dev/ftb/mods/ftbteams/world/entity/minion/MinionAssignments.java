package dev.ftb.mods.ftbteams.world.entity.minion;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** One-to-one home/resident ledger. An unloaded resident still owns its home. */
public final class MinionAssignments {
	public record Resident(UUID minion, UUID owner, UUID faction) {
	}

	private final Map<Long, Resident> homes = new HashMap<>();
	private final Map<UUID, Long> residents = new HashMap<>();

	public Resident get(long home) {
		return homes.get(home);
	}

	public boolean claim(long home, UUID minion, UUID owner, UUID faction) {
		if (homes.containsKey(home) || residents.containsKey(minion)) {
			return false;
		}
		homes.put(home, new Resident(minion, owner, faction));
		residents.put(minion, home);
		return true;
	}

	public boolean release(long home, UUID minion) {
		Resident resident = homes.get(home);
		if (resident == null || !resident.minion().equals(minion)) {
			return false;
		}
		homes.remove(home);
		residents.remove(minion);
		return true;
	}

	public Map<Long, Resident> entries() {
		return Map.copyOf(homes);
	}
}
