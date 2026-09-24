package dev.ftb.mods.ftbteams.world.entity.minion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.UUID;

/** Dimension-level workstation associations remain authoritative while entities and chunks are unloaded. */
public final class MinionWorkplaceData extends SavedData {
	private static final Factory<MinionWorkplaceData> FACTORY = new Factory<>(MinionWorkplaceData::new,
			MinionWorkplaceData::load, null);
	private final MinionWorkAssignments assignments = new MinionWorkAssignments();

	public static MinionWorkplaceData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(FACTORY, "ftbteams_minion_workplaces");
	}

	public UUID workerAt(BlockPos position) {
		return assignments.workerAt(position.asLong());
	}

	public BlockPos positionOf(UUID worker) {
		Long position = assignments.positionOf(worker);
		return position == null ? null : BlockPos.of(position);
	}

	public boolean claim(BlockPos position, UUID worker) {
		boolean alreadyOwned = worker.equals(assignments.workerAt(position.asLong()));
		boolean claimed = assignments.claim(position.asLong(), worker);
		if (claimed && !alreadyOwned) {
			setDirty();
		}
		return claimed;
	}

	public void release(UUID worker) {
		if (assignments.release(worker)) {
			setDirty();
		}
	}

	public void remove(BlockPos position) {
		if (assignments.remove(position.asLong())) {
			setDirty();
		}
	}

	private static MinionWorkplaceData load(CompoundTag tag, HolderLookup.Provider registries) {
		MinionWorkplaceData data = new MinionWorkplaceData();
		ListTag workplaces = tag.getList("Workplaces", Tag.TAG_COMPOUND);
		for (int index = 0; index < workplaces.size(); index++) {
			CompoundTag workplace = workplaces.getCompound(index);
			if (workplace.hasUUID("Minion")) {
				data.assignments.claim(workplace.getLong("Pos"), workplace.getUUID("Minion"));
			}
		}
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		ListTag workplaces = new ListTag();
		assignments.entries().forEach((position, worker) -> {
			CompoundTag workplace = new CompoundTag();
			workplace.putLong("Pos", position);
			workplace.putUUID("Minion", worker);
			workplaces.add(workplace);
		});
		tag.put("Workplaces", workplaces);
		return tag;
	}
}
