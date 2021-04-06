package nl.elec332.bot.discord.ps2outfits.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import nl.elec332.bot.discord.ps2outfits.Util;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Elec332 on 06/04/2021
 */
public class Outfit {

    public static Outfit getOutfit(String oid) {
        return new Outfit(Util.getOutfitObject(oid));
    }

    private Outfit(JsonObject outfit) {
        this.users = outfit.getAsJsonArray("members");
        this.name = outfit.get("name").getAsString();
    }

    private final String name;
    private final JsonArray users;

    public String getName() {
        return this.name;
    }

    public int getMembers() {
        return users.size();
    }

    public List<String> getOnlinePlayersBefore(Instant since) {
        List<String> onlineMembers = new ArrayList<>();
        for (int i = 0; i < this.users.size(); i++) {
            JsonObject member = this.users.get(i).getAsJsonObject();
            JsonObject times = member.getAsJsonObject("times");
            if (times == null) {
                continue;
            }
            Instant lastLogon = Instant.ofEpochSecond(Long.parseLong(times.get("last_login").getAsString()));
            if (lastLogon.isAfter(since)) {
                onlineMembers.add(member.get("character_id").getAsString());
            }
        }
        return onlineMembers;
    }

}
