package dev.ftb.mods.ftbteams.api.property;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;

import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * These are the standard team properties which are registered for every team. Other mods may add additional properties;
 * see {@link dev.ftb.mods.ftbteams.api.event.TeamCollectPropertiesEvent}.
 */
public class TeamProperties {
	/** The side length, in pixels, of a faction banner. */
	public static final int FACTION_LOGO_SIZE = 32;
	/** The original 4-bit-per-pixel format, retained only so existing saved logos can be migrated. */
	public static final int LEGACY_FACTION_LOGO_ENCODED_LENGTH = 683;
	/** A 32x32 ARGB image is 4096 bytes, or 5462 unpadded base64 characters. */
	public static final int FACTION_LOGO_ENCODED_LENGTH = 5462;
	private static final String EMPTY_FACTION_LOGO = "A".repeat(FACTION_LOGO_ENCODED_LENGTH);
    public static final StringProperty DISPLAY_NAME
            = (StringProperty) new StringProperty(FTBTeamsAPI.rl("display_name"), "", Pattern.compile(".{3,}"))
            .syncToAll();
    public static final StringProperty DESCRIPTION
            = new StringProperty(FTBTeamsAPI.rl("description"), "");
    public static final ColorProperty COLOR
            = (ColorProperty) new ColorProperty(FTBTeamsAPI.rl("color"), Color4I.WHITE)
            .syncToAll();
    public static final BooleanProperty FREE_TO_JOIN
            = new BooleanProperty(FTBTeamsAPI.rl("free_to_join"), false);
    public static final IntProperty MAX_MSG_HISTORY_SIZE
            = new IntProperty(FTBTeamsAPI.rl("max_msg_history_size"), 1000);
    public static final StringSetProperty TEAM_STAGES
            = (StringSetProperty) new StringSetProperty(FTBTeamsAPI.rl("team_stages"), new HashSet<>())
            .hidden()
            .notPlayerEditable();
    public static final IntProperty LIVES_REMAINING
            = (IntProperty) new IntProperty(FTBTeamsAPI.rl("lives_remaining"), 0, 0, Integer.MAX_VALUE)
            .hidden()
            .notPlayerEditable();
	/**
	 * Public faction progression value. It is intentionally server-managed: the population system will calculate it
	 * later, while administrators can adjust it with the faction command during the transition.
	 */
	public static final IntProperty FACTION_LEVEL
			= (IntProperty) new IntProperty(FTBTeamsAPI.rl("faction_level"), 1, 1, 100)
			.syncToAll()
			.notPlayerEditable();
	/** A compact, palette-indexed 32x32 faction logo. See {@code FactionLogo}. */
	public static final StringProperty FACTION_LOGO
			= (StringProperty) new StringProperty(FTBTeamsAPI.rl("faction_logo"), EMPTY_FACTION_LOGO,
					Pattern.compile("[A-Za-z0-9_-]{" + LEGACY_FACTION_LOGO_ENCODED_LENGTH + "}|[A-Za-z0-9_-]{" + FACTION_LOGO_ENCODED_LENGTH + "}"))
			.syncToAll()
			.hidden()
			.notPlayerEditable();
	public static final StringProperty FACTION_CAPITAL_DIMENSION
			= (StringProperty) new StringProperty(FTBTeamsAPI.rl("faction_capital_dimension"), "")
			.syncToAll()
			.hidden()
			.notPlayerEditable();
	public static final IntProperty FACTION_CAPITAL_X
			= (IntProperty) new IntProperty(FTBTeamsAPI.rl("faction_capital_x"), 0)
			.syncToAll()
			.hidden()
			.notPlayerEditable();
	public static final IntProperty FACTION_CAPITAL_Y
			= (IntProperty) new IntProperty(FTBTeamsAPI.rl("faction_capital_y"), 0)
			.syncToAll()
			.hidden()
			.notPlayerEditable();
	public static final IntProperty FACTION_CAPITAL_Z
			= (IntProperty) new IntProperty(FTBTeamsAPI.rl("faction_capital_z"), 0)
			.syncToAll()
			.hidden()
			.notPlayerEditable();
}
