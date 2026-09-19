package dev.ftb.mods.ftbteams.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Immutable, bounded snapshot of the solid boundary of the validated air volume. No inventories or block entity NBT. */
public record EnclosurePreview(List<Entry> blocks) {
	public static final EnclosurePreview EMPTY = new EnclosurePreview(List.of());
	public static final int RADIUS = EnclosureBlock.SEARCH_RADIUS;
	public static final int DIAMETER = RADIUS * 2 + 1;
	public static final int MAX_BLOCKS = DIAMETER * DIAMETER * DIAMETER;

	public EnclosurePreview {
		blocks = List.copyOf(blocks);
		if (blocks.size() > MAX_BLOCKS) throw new IllegalArgumentException("Enclosure preview exceeds search volume");
	}

	public record Entry(BlockPos offset, BlockState state) {
	}

	public static EnclosurePreview capture(Level level, BlockPos origin, EnclosureScanner.Result result) {
		if (result.status() != EnclosureScanner.Status.SEALED) return EMPTY;
		List<Entry> blocks = new ArrayList<>();
		for (EnclosureScanner.Position p : result.boundary()) {
			BlockPos pos = new BlockPos(p.x(), p.y(), p.z());
			// Never force-load a chunk for a preview.
			if (!level.hasChunkAt(pos)) return EMPTY;
			BlockState state = level.getBlockState(pos);
			if (!state.isAir()) blocks.add(new Entry(pos.subtract(origin), state));
		}
		return new EnclosurePreview(blocks);
	}

	public void write(RegistryFriendlyByteBuf buf) {
		buf.writeVarInt(blocks.size());
		for (Entry block : blocks) {
			BlockPos pos = block.offset();
			buf.writeShort(((pos.getX() + RADIUS) * DIAMETER + pos.getY() + RADIUS) * DIAMETER + pos.getZ() + RADIUS);
			buf.writeVarInt(Block.getId(block.state()));
		}
	}

	public static EnclosurePreview read(RegistryFriendlyByteBuf buf) {
		int count = buf.readVarInt();
		if (count < 0 || count > MAX_BLOCKS) throw new IllegalArgumentException("Invalid enclosure preview size");
		List<Entry> blocks = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			int packed = buf.readUnsignedShort();
			if (packed >= MAX_BLOCKS) throw new IllegalArgumentException("Invalid preview block offset");
			BlockPos offset = new BlockPos(packed / (DIAMETER * DIAMETER) - RADIUS,
					packed / DIAMETER % DIAMETER - RADIUS, packed % DIAMETER - RADIUS);
			blocks.add(new Entry(offset, Block.stateById(buf.readVarInt())));
		}
		return count == 0 ? EMPTY : new EnclosurePreview(blocks);
	}
}
