package nl.elec332.bot.discord.ps2outfits;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Elec332 on 03/04/2021
 */
public class Commands {

    private static final int MAX_CHARS = 5500;

    public static void commandKD(TextChannel channel, String outfit) {
        JsonObject jo = Util.getOneObject(Util.invokeAPI("outfit", "outfit_id=" + outfit + "&c:resolve=member_character(times)"));
        try {
            Instant yesterday = Instant.now().minus(1, ChronoUnit.DAYS);
            JsonArray array = jo.getAsJsonArray("members");
            int members = array.size();
            List<String> onlineMembers = new ArrayList<>();
            for (int i = 0; i < members; i++) {
                JsonObject member = array.get(i).getAsJsonObject();
                JsonObject times = member.getAsJsonObject("times");
                if (times == null) {
                    continue;
                }
                Instant lastLogon = Instant.ofEpochSecond(Long.parseLong(times.get("last_login").getAsString()));
                if (lastLogon.isAfter(yesterday)) {
                    onlineMembers.add(member.get("character_id").getAsString());
                }
            }
            String onlineTxt = "Out of " + members + " members, " + onlineMembers.size() + " have been online in the last 24 hours";
            List<String> info = Util.getDailyKDInfo(onlineMembers);
            if (info.size() != onlineMembers.size()) {
                onlineTxt += "\n (" + info.size() + " members were active today)";
            }
//            channel.sendMessage(onlineTxt).submit();
            EmbedBuilder builder = new EmbedBuilder()
                    .setTitle(jo.get("name").getAsString() + " KD info")
                    .setDescription(onlineTxt);
//            int part = 1;
            int idx = 0;
            while (idx < info.size()) {
                int counter = 0;
                while (idx < info.size()) {
                    String is = info.get(idx);
                    if (counter + is.length() > MAX_CHARS || builder.getFields().size() >= 25) {
                        break;
                    }
                    idx++;
                    String[] data = is.split("\\|");
                    builder.addField(data[0], data[1], false);
                    counter += is.length();
                }
                if (counter < 1) {
                    break;
                }
                channel.sendMessage(builder.build()).submit();
//                part++;
                builder = new EmbedBuilder()
                ;//.setTitle(jo.get("name").getAsString() + " KD info Part " + part);
            }
        } catch (Exception e) {
            System.out.println(jo);
            e.printStackTrace(System.out);
            throw new RuntimeException("Please poke Elec332 to fix his bot, as it's broke!");
        }
    }

}
