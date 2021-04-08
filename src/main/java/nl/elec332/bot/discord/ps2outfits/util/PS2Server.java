package nl.elec332.bot.discord.ps2outfits.util;

import java.util.Locale;

/**
 * Created by Elec332 on 08/04/2021
 */
public enum PS2Server {

    CONNERY(1),
    MILLER(10),
    COBALT(13),
    EMERALD(17),
    JAEGER(19),
    APEX(24),
    SOLTECH(40);

    PS2Server(int id) {
        this.id = id;
    }

    private final int id;

    public int getId() {
        return id;
    }

    public String getServerName() {
        return toString().toLowerCase(Locale.ROOT);
    }

    public static PS2Server getServer(String name) {
        name = name.toLowerCase(Locale.ROOT);
        for (PS2Server server : PS2Server.values()) {
            if (server.toString().toLowerCase(Locale.ROOT).equals(name)) {
                return server;
            }
        }
        return null;
    }

}
