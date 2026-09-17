package dev.ftb.mods.ftbteams.api.faction;

import dev.ftb.mods.ftbteams.api.property.TeamProperties;

import java.util.Base64;

/**
 * The faction logo's portable representation. It stores full ARGB pixels, allowing a faction to use the same
 * colour picker as the rest of FTB Teams. The old 4-bit palette format is migrated on read.
 */
public final class FactionLogo {
	public static final int SIZE = TeamProperties.FACTION_LOGO_SIZE;
	public static final int PIXEL_COUNT = SIZE * SIZE;
	public static final int BYTE_COUNT = PIXEL_COUNT * Integer.BYTES;
	private static final int LEGACY_BYTE_COUNT = PIXEL_COUNT / 2;
	private static final int[] LEGACY_PALETTE = {
			0x00000000, 0xFFF9FFFE, 0xFFF9801D, 0xFFC74EBD, 0xFF3AB3DA, 0xFFFED83D,
			0xFF80C71F, 0xFFF38BAA, 0xFF474F52, 0xFF169C9C, 0xFF8932B8, 0xFF3C44AA,
			0xFF835432, 0xFF5E7C16, 0xFFB02E26, 0xFF1D1D21
	};

	private final byte[] pixels;

	public FactionLogo() {
		this(new byte[BYTE_COUNT]);
	}

	private FactionLogo(byte[] pixels) {
		this.pixels = pixels;
	}

	public static FactionLogo decode(String encoded) {
		try {
			byte[] bytes = Base64.getUrlDecoder().decode(encoded);
			if (bytes.length == BYTE_COUNT) {
				return new FactionLogo(bytes);
			} else if (bytes.length == LEGACY_BYTE_COUNT) {
				FactionLogo migrated = new FactionLogo();
				for (int index = 0; index < PIXEL_COUNT; index++) {
					int value = Byte.toUnsignedInt(bytes[index / 2]);
					int paletteIndex = (index & 1) == 0 ? value >>> 4 : value & 0xF;
					migrated.set(index % SIZE, index / SIZE, LEGACY_PALETTE[paletteIndex]);
				}
				return migrated;
			}
			return new FactionLogo();
		} catch (IllegalArgumentException ignored) {
			return new FactionLogo();
		}
	}

	public String encode() {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(pixels);
	}

	public int get(int x, int y) {
		int index = y * SIZE + x;
		int byteIndex = index * Integer.BYTES;
		return Byte.toUnsignedInt(pixels[byteIndex]) << 24
				| Byte.toUnsignedInt(pixels[byteIndex + 1]) << 16
				| Byte.toUnsignedInt(pixels[byteIndex + 2]) << 8
				| Byte.toUnsignedInt(pixels[byteIndex + 3]);
	}

	public void set(int x, int y, int color) {
		int index = y * SIZE + x;
		int byteIndex = index * Integer.BYTES;
		pixels[byteIndex] = (byte) (color >>> 24);
		pixels[byteIndex + 1] = (byte) (color >>> 16);
		pixels[byteIndex + 2] = (byte) (color >>> 8);
		pixels[byteIndex + 3] = (byte) color;
	}

	public void clear() {
		java.util.Arrays.fill(pixels, (byte) 0);
	}
}
