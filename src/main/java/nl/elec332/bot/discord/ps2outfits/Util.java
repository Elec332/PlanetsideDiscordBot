package nl.elec332.bot.discord.ps2outfits;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Elec332 on 03/04/2021
 */
public class Util {

    public static final String RETURNED = "returned";
    public static Gson GSON = new GsonBuilder().setLenient().setPrettyPrinting().create();
    public static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#0.00");

    @SuppressWarnings("all")
    public static JsonObject invokeAPI(String root, String command) {
        try {
            InputStream is = new URL("https://census.daybreakgames.com/" + Main.PS2_SID + "/get/ps2:v2/" + root + "/?" + command.replace(" ", "%20")).openStream();
            try {
                BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                return GSON.fromJson(rd, JsonObject.class);
            } finally {
                is.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to poke API!");
        }
    }

    public static JsonObject getOneObject(JsonObject jo) {
        if (jo.has(RETURNED) && jo.get(RETURNED).getAsInt() == 1) {
            return jo.getAsJsonArray(jo.keySet().stream().filter(s -> !s.equals(RETURNED)).findFirst().orElseThrow(NullPointerException::new)).get(0).getAsJsonObject();
        }
        throw new RuntimeException("API didn't return expected result!");
    }

    public static List<String> getDailyKDInfo(Collection<String> uidList) {
        Map<String, Map.Entry<JsonObject, JsonObject>> data = getKDInfoHistory(uidList);
        Map<Float, String> map = new LinkedHashMap<>();
        data.forEach((name, kd) -> {
            int k = Integer.parseInt(kd.getKey().getAsJsonObject("day").get("d01").getAsString());
            if (k == 0) {
                return;
            }
            float d = Integer.parseInt(kd.getValue().getAsJsonObject("day").get("d01").getAsString());
            float kdr = k / d;
            String kds = k < 15 ? "---" : NUMBER_FORMAT.format(kdr);
            map.put(kdr, name + "|" + "K/D: " + kds + "  Kills: " + k);
        });
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    public static Map<String, Map.Entry<JsonObject, JsonObject>> getKDInfoHistory(Collection<String> uidList) {
        Map<String, Map.Entry<JsonObject, JsonObject>> ret = new HashMap<>();
        String uid = String.join(",", uidList);
        JsonObject root = invokeAPI("character", "character_id=" + uid + "&c:resolve=stat_history");
        if (root.has(RETURNED) && root.get(RETURNED).getAsInt() == uidList.size()) {
            JsonArray array = root.getAsJsonArray("character_list");
            for (int i = 0; i < array.size(); i++) {
                JsonObject jo = array.get(i).getAsJsonObject();
                String name = jo.getAsJsonObject("name").get("first").getAsString();
                JsonArray stats = jo.getAsJsonObject("stats").getAsJsonArray("stat_history");
                JsonObject kills = null, deaths = null;
                for (int j = 0; j < stats.size(); j++) {
                    JsonObject so = stats.get(j).getAsJsonObject();
                    String statName = so.get("stat_name").getAsString();
                    if (statName.equals("deaths")) {
                        deaths = so;
                    } else if (statName.equals("kills")) {
                        kills = so;
                    }
                    if (deaths != null && kills != null) {
                        break;
                    }
                }
                if (kills == null || deaths == null) {
                    throw new RuntimeException("Failed to fetch K/D history!");
                }
                ret.put(name, new AbstractMap.SimpleEntry<>(kills, deaths));
            }
        }
        return ret;
    }

}