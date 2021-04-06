package nl.elec332.bot.discord.ps2outfits;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import nl.elec332.bot.discord.ps2outfits.util.Outfit;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by Elec332 on 03/04/2021
 */
public class CommandHelper {

    private static final int MAX_CHARS = 5500;

    public static List<Map.Entry<String, String>> getDailyKDInfo(Collection<String> uidList) {
        return getKDInfo(uidList, o -> o.getAsJsonObject("day").get("d01"));
    }

    public static List<Map.Entry<String, String>> getWeeklyKDInfo(Collection<String> uidList) {
        return getKDInfo(uidList, o -> o.getAsJsonObject("week").get("w01"));
    }

    public static List<Map.Entry<String, String>> getMonthlyKDInfo(Collection<String> uidList) {
        return getKDInfo(uidList, o -> o.getAsJsonObject("month").get("m01"));
    }

    public static void postPlayerData(TextChannel channel, String outfitID, String timeString, Instant since, Function<List<String>, List<Map.Entry<String, String>>> kdGetter) {
        Outfit outfit = Outfit.getOutfit(outfitID);
        List<String> onlineMembers = outfit.getOnlinePlayersBefore(since);
        String title = outfit.getName() + " KD info";
        String description = "Out of " + outfit.getMembers() + " members, " + onlineMembers.size() + " have been online " + timeString;
        List<Map.Entry<String, String>> info = kdGetter.apply(onlineMembers);
        if (info.size() != onlineMembers.size()) {
            description += "\n (" + info.size() + " members were active in that period)";
        }
        CommandHelper.postPlayerData(channel, title, description, info);
    }

    public static void postPlayerData(TextChannel channel, String title_, String description, List<Map.Entry<String, String>> fields) {
        try {
            EmbedBuilder builder = new EmbedBuilder()
                    .setTitle(title_)
                    .setDescription(description);
            int idx = 0;
            while (idx < fields.size()) {
                int counter = 0;
                while (idx < fields.size()) {
                    Map.Entry<String, String> is = fields.get(idx);
                    String title = is.getKey();
                    String value = is.getValue();
                    int length = title.length() + value.length();
                    if (counter + length > MAX_CHARS || builder.getFields().size() >= 25) {
                        break;
                    }
                    idx++;
                    builder.addField(title, value, false);
                    counter += length;
                }
                if (counter < 1) {
                    break;
                }
                channel.sendMessage(builder.build()).submit();
                builder = new EmbedBuilder();
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw new RuntimeException("Please poke Elec332 to fix his bot, as it's broke!");
        }
    }

    private static List<Map.Entry<String, String>> getKDInfo(Collection<String> uidList, Function<JsonObject, JsonElement> getter) {
        Map<String, Map.Entry<JsonObject, JsonObject>> data = Util.getKDInfoHistory(uidList);
        Map<Float, String> map = new LinkedHashMap<>();
        data.forEach((name, kd) -> {
            int k = Integer.parseInt(getter.apply(kd.getKey()).getAsString());
            if (k == 0) {
                return;
            }
            float d = Math.max(1, Integer.parseInt(getter.apply(kd.getValue()).getAsString()));
            float kdr = k / d;
            String kds = Util.NUMBER_FORMAT.format(kdr);
            if (k < 15 || d < 2) {
                kdr = -1 + kdr / 100;
                kds = "---";
            }
            map.put(kdr, name + "|" + "K/D: " + kds + "  Kills: " + k);
        });
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
                .map(Map.Entry::getValue)
                .map(s -> {
                    String[] split = s.split("\\|");
                    return new AbstractMap.SimpleEntry<>(split[0], split[1]);
                })
                .collect(Collectors.toList());
    }

}
