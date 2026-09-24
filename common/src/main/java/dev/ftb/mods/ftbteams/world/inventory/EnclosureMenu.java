package dev.ftb.mods.ftbteams.world.inventory;

import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.net.EnclosurePreviewMessage;
import dev.ftb.mods.ftbteams.world.block.EnclosureBlock;
import dev.ftb.mods.ftbteams.world.block.enclosure.EnclosurePreview;
import dev.ftb.mods.ftbteams.world.block.enclosure.EnclosureScanner;
import dev.ftb.mods.ftbteams.world.block.entity.EnclosureBlockEntity;
import dev.ftb.mods.ftbteams.world.entity.minion.MinionHousing;
import dev.ftb.mods.ftbteams.world.inventory.slot.ExtractOnlySlot;
import dev.ftb.mods.ftblibrary.util.NetworkHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative menu for enclosure status, optional storage, and player inventory transfers. */
public class EnclosureMenu extends AbstractContainerMenu {
	public static final int CHECK_BUTTON = 0;
	public static final int STORAGE_X = 14;
	public static final int STORAGE_Y = 40;
	public static final int PLAYER_X = 79;
	public static final int PLAYER_Y = 152;
	private final Container container;
	private final ContainerData data;
	private final int storageSize;
	private final Player viewer;
	private BlockPos origin;
	private boolean workerStatus;
	private EnclosurePreview preview = EnclosurePreview.EMPTY;
	private EnclosurePreview lastSentPreview;

	public EnclosureMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
		this(id, inventory, new SimpleContainer(buf.readVarInt()), new SimpleContainerData(4), buf.readBlockPos(), buf.readBoolean());
	}

	private EnclosureMenu(int id, Inventory inventory, Container container, ContainerData data, BlockPos origin, boolean workerStatus) {
		this(id, inventory, container, data);
		this.origin = origin;
		this.workerStatus = workerStatus;
	}

	public EnclosureMenu(int id, Inventory inventory, Container container, ContainerData data) {
		super(FTBTeams.ENCLOSURE_MENU.get(), id);
		this.container = container;
		this.data = data;
		viewer = inventory.player;
		origin = container instanceof EnclosureBlockEntity enclosure ? enclosure.getBlockPos() : BlockPos.ZERO;
		storageSize = container.getContainerSize();
		if (storageSize != 0 && storageSize != EnclosureBlock.STORAGE_SIZE) {
			throw new IllegalArgumentException("Enclosure storage must have 0 or 25 slots");
		}
		checkContainerDataCount(data, 4);
		workerStatus = container instanceof EnclosureBlockEntity enclosure
				&& enclosure.getBlockState().getBlock() instanceof EnclosureBlock block && block.showsWorkerStatus();
		container.startOpen(inventory.player);
		for (int slot = 0; slot < storageSize; slot++) {
			int x = STORAGE_X + slot % 5 * 18;
			addSlot(slot < EnclosureBlock.INPUT_SLOTS
					? new Slot(container, slot, x, storageY(slot))
					: new ExtractOnlySlot(container, slot, x, storageY(slot)));
		}
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(inventory, col + row * 9 + 9, PLAYER_X + col * 18, PLAYER_Y + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			addSlot(new Slot(inventory, col, PLAYER_X + col * 18, PLAYER_Y + 58));
		}
		addDataSlots(data);
	}

	public BlockPos getOrigin() {
		return origin;
	}

	public EnclosurePreview getPreview() {
		return preview;
	}

	public void setPreview(EnclosurePreview preview) {
		this.preview = preview;
	}

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();
		if (viewer instanceof ServerPlayer player && container instanceof EnclosureBlockEntity enclosure
				&& stillValid(player) && enclosure.getPreview() != lastSentPreview) {
			lastSentPreview = enclosure.getPreview();
			NetworkHelper.sendTo(player, new EnclosurePreviewMessage(containerId, origin, lastSentPreview));
		}
	}

	public static int storageY(int slot) {
		return STORAGE_Y + slot / 5 * 18 + (slot >= EnclosureBlock.INPUT_SLOTS ? 4 : 0);
	}

	/** Input and output roles are shared by the UI and future building processing logic. */
	public boolean isInputSlot(int slot) {
		return hasStorage() && slot >= 0 && slot < EnclosureBlock.INPUT_SLOTS;
	}

	public boolean isOutputSlot(int slot) {
		return hasStorage() && slot >= EnclosureBlock.INPUT_SLOTS && slot < storageSize;
	}

	public boolean hasStorage() {
		return storageSize > 0;
	}

	public EnclosureScanner.Status getStatus() {
		return EnclosureScanner.Status.values()[data.get(0)];
	}

	public int getVolume() {
		return data.get(1);
	}

	public MinionHousing.Status getHousingStatus() {
		return MinionHousing.Status.values()[data.get(2)];
	}

	public boolean showsWorkerStatus() {
		return workerStatus;
	}

	public int getWorkingMinions() {
		return data.get(3);
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id == CHECK_BUTTON && player instanceof ServerPlayer serverPlayer && !player.isSpectator() && stillValid(player)
				&& container instanceof EnclosureBlockEntity enclosure && enclosure.checkEnclosure(serverPlayer)) {
			broadcastChanges();
			return true;
		}
		return false;
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		if (index < 0 || index >= slots.size()) {
			return ItemStack.EMPTY;
		}
		Slot slot = slots.get(index);
		if (!slot.hasItem() || !slot.mayPickup(player)) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (index < storageSize) {
			if (!moveItemStackTo(stack, storageSize, slots.size(), true)) return ItemStack.EMPTY;
		} else if (hasStorage()) {
			if (!moveItemStackTo(stack, 0, storageSize, false)) return ItemStack.EMPTY;
		} else if (index < storageSize + 27) {
			if (!moveItemStackTo(stack, storageSize + 27, slots.size(), false)) return ItemStack.EMPTY;
		} else if (!moveItemStackTo(stack, storageSize, storageSize + 27, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
		else slot.setChanged();
		slot.onTake(player, stack);
		return original;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		container.stopOpen(player);
	}
}
