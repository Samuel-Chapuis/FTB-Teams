package dev.ftb.mods.ftbteams.world.entity.minion;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** One-to-one association between persistent minion UUIDs and workstation positions. */
public final class MinionWorkAssignments {
	private final Map<Long, UUID> workersByPosition = new HashMap<>();
	private final Map<UUID, Long> positionsByWorker = new HashMap<>();

	public UUID workerAt(long position) {
		return workersByPosition.get(position);
	}

	public Long positionOf(UUID worker) {
		return positionsByWorker.get(worker);
	}

	/** Claims a free position, or confirms an association already owned by the same minion. */
	public boolean claim(long position, UUID worker) {
		UUID currentWorker = workersByPosition.get(position);
		Long currentPosition = positionsByWorker.get(worker);
		if (worker.equals(currentWorker) && currentPosition != null && currentPosition == position) {
			return true;
		}
		if (currentWorker != null || currentPosition != null) {
			return false;
		}
		workersByPosition.put(position, worker);
		positionsByWorker.put(worker, position);
		return true;
	}

	/** Releases the worker's position without affecting a later replacement. */
	public boolean release(UUID worker) {
		Long position = positionsByWorker.remove(worker);
		return position != null && workersByPosition.remove(position, worker);
	}

	/** Removes a destroyed workstation and its reverse worker association. */
	public boolean remove(long position) {
		UUID worker = workersByPosition.remove(position);
		return worker != null && positionsByWorker.remove(worker, position);
	}

	public Map<Long, UUID> entries() {
		return Map.copyOf(workersByPosition);
	}
}
