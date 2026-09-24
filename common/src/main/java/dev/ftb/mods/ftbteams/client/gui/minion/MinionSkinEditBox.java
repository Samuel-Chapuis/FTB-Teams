package dev.ftb.mods.ftbteams.client.gui.minion;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbteams.net.SetMinionSkinMessage;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/** Text input that submits a player name as a minion skin when Enter is pressed. */
final class MinionSkinEditBox extends EditBox {
	private static final int ENTER_KEY = 257;
	private final int minionId;

	MinionSkinEditBox(Font font, int x, int y, int width, int height, int minionId) {
		super(font, x, y, width, height, Component.translatable("ftbteams.minion.choose_skin"));
		this.minionId = minionId;
		setMaxLength(64);
		visible = false;
	}

	/** Reveals and focuses the input while preserving any text already entered. */
	void open() {
		visible = true;
		setFocused(true);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (visible && isFocused() && keyCode == ENTER_KEY) {
			NetworkManager.sendToServer(new SetMinionSkinMessage(minionId, getValue().trim()));
			setFocused(false);
			visible = false;
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}
}
