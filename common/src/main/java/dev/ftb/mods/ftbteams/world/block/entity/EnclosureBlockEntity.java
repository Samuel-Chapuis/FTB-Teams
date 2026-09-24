package dev.ftb.mods.ftbteams.world.block.entity;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.world.block.EnclosureBlock;
import dev.ftb.mods.ftbteams.world.block.enclosure.EnclosurePreview;
import dev.ftb.mods.ftbteams.world.block.enclosure.EnclosureScanner;
import dev.ftb.mods.ftbteams.world.block.enclosure.EnclosureValidator;
import dev.ftb.mods.ftbteams.world.entity.minion.MinionHousing;
import dev.ftb.mods.ftbteams.world.entity.minion.MinionPopulationData;
import dev.ftb.mods.ftbteams.world.inventory.EnclosureMenu;
import dev.ftb.mods.ftbteams.world.faction.FactionAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/** Persistent controller state shared by every {@link EnclosureBlock} implementation. */
public class EnclosureBlockEntity extends BaseContainerBlockEntity implements ExtendedMenuProvider {
	private NonNullList<ItemStack> items;
	private EnclosureScanner.Status status = EnclosureScanner.Status.UNCHECKED;
	private int volume;
	private MinionHousing.Status housingStatus = MinionHousing.Status.NONE;
	private EnclosurePreview preview = EnclosurePreview.EMPTY;
	private UUID owner;
	private UUID faction;
	private long nextCheckTick;
	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			return switch (index) {
				case 0 -> status.ordinal();
				case 1 -> volume;
				case 2 -> getHousingStatus().ordinal();
				case 3 -> getWorkingMinions();
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {
			if (index == 0) {
				status = EnclosureScanner.Status.values()[value];
			} else if (index == 1) {
				volume = value;
			} else if (index == 2) {
				housingStatus = MinionHousing.Status.values()[value];
			}
		}

		@Override
		public int getCount() {
			return 4;
		}
	};

	public EnclosureBlockEntity(BlockPos pos, BlockState state) {
		this(FTBTeams.ENCLOSURE_BLOCK_ENTITY.get(), pos, state);
	}

	/** Other building types can reuse this entity with their own registered block entity type. */
	protected EnclosureBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		EnclosureBlock block = (EnclosureBlock) state.getBlock();
		// Only the controller half owns items; the foot exists solely for rendering.
		items = NonNullList.withSize(block.hasInventory() && block.getControllerPos(state, pos).equals(pos)
				? EnclosureBlock.STORAGE_SIZE : 0, ItemStack.EMPTY);
	}

	public boolean checkEnclosure(ServerPlayer player) {
		if (level == null || level.isClientSide || level.getGameTime() < nextCheckTick) {
			return false;
		}
		nextCheckTick = level.getGameTime() + 10;
		EnclosureBlock block = (EnclosureBlock) getBlockState().getBlock();
		var validation = EnclosureValidator.validate((ServerLevel) level, block, getBlockState(), worldPosition);
		var result = validation.scan();
		status = result.status();
		volume = result.volume();
		preview = validation.preview();
		setChanged();
		if (status == EnclosureScanner.Status.SEALED) {
			block.claimOwnership(this, player);
		}
		block.onEnclosureChecked(this, player, result);
		return true;
	}

	private MinionHousing.Status getHousingStatus() {
		if (level instanceof ServerLevel server && (housingStatus == MinionHousing.Status.NONE || housingStatus == MinionHousing.Status.ASSIGNED)) {
			return MinionPopulationData.get(server).resident(worldPosition) != null ? MinionHousing.Status.ASSIGNED : MinionHousing.Status.NONE;
		}
		return housingStatus;
	}

	public EnclosurePreview getPreview() {
		return preview;
	}

	public int getWorkingMinions() {
		return level instanceof ServerLevel server && getBlockState().getBlock() instanceof EnclosureBlock block
				? block.getWorkerCount(server, worldPosition) : 0;
	}

	/** Updates the housing result produced by a specialized enclosure block. */
	public void setHousingStatus(MinionHousing.Status status) {
		housingStatus = status;
		setChanged();
	}

	/** The player who first validated this sealed building. */
	public UUID getOwnerId() {
		return owner;
	}

	/** The faction of the owner when the building was claimed. */
	public UUID getFactionId() {
		return faction;
	}

	public boolean canAccess(ServerPlayer player) {
		return FactionAccess.canAccess(owner, faction, player);
	}

	/** Claims an unowned controller for the supplied player and faction. */
	public boolean claimOwnership(ServerPlayer player, UUID playerFaction) {
		if (owner != null) {
			return canAccess(player);
		}
		if (playerFaction == null) {
			return false;
		}
		owner = player.getUUID();
		faction = playerFaction;
		setChanged();
		return true;
	}

	@Override
	public void saveExtraData(FriendlyByteBuf buf) {
		buf.writeVarInt(getContainerSize());
		buf.writeBlockPos(worldPosition);
		buf.writeBoolean(getBlockState().getBlock() instanceof EnclosureBlock block && block.showsWorkerStatus());
	}

	@Override
	protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
		return new EnclosureMenu(id, inventory, this, data);
	}

	@Override
	protected Component getDefaultName() {
		return getBlockState().getBlock().getName();
	}

	@Override
	public int getContainerSize() {
		return items.size();
	}

	/** Output slots are extract-only. This also protects insertion through item handlers and pipes. */
	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot >= 0 && slot < EnclosureBlock.INPUT_SLOTS && super.canPlaceItem(slot, stack);
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		ContainerHelper.saveAllItems(tag, items, registries);
		tag.putBoolean("EnclosureSealed", status == EnclosureScanner.Status.SEALED);
		if (owner != null) {
			tag.putUUID("EnclosureOwner", owner);
		}
		if (faction != null) {
			tag.putUUID("EnclosureFaction", faction);
		}
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(tag, items, registries);
		owner = tag.hasUUID("EnclosureOwner") ? tag.getUUID("EnclosureOwner") : null;
		faction = tag.hasUUID("EnclosureFaction") ? tag.getUUID("EnclosureFaction") : null;
		// A scan is a snapshot, not proof that the building is still closed after a reload.
		status = tag.getBoolean("EnclosureSealed") ? EnclosureScanner.Status.SEALED : EnclosureScanner.Status.UNCHECKED;
		housingStatus = MinionHousing.Status.NONE;
		volume = 0;
		preview = EnclosurePreview.EMPTY;
	}
}
