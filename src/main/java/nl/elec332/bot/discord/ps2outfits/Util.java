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
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Elec332 on 03/04/2021
 */
public class Util {

    public static final String RETURNED = "returned";
    public static Gson GSON = new GsonBuilder().setLenient().setPrettyPrinting().create();
    public static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#0.00");

    /**
     * Invokes a call to the Planetside2 Census API
     */
    public static JsonObject invokeAPI(String root, String command) {
        return readJsonFromURL("https://census.daybreakgames.com/" + Main.PS2_SID + "/get/ps2:v2/" + root + "/?" + command.replace(" ", "%20"));
    }

    @SuppressWarnings("all")
    public static JsonObject readJsonFromURL(String url) {
        try {
            InputStream is = new URL(url).openStream();
            try {
                BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                return GSON.fromJson(rd, JsonObject.class);
            } finally {
                is.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to poke API!", e);
        }
    }

    /**
     * Returns the root JSON object if the expected array size is 1
     */
    public static JsonObject getRootArray(JsonObject jo) {
        if (jo.has(RETURNED) && jo.get(RETURNED).getAsInt() == 1) {
            return jo.getAsJsonArray(jo.keySet().stream().filter(s -> !s.equals(RETURNED)).findFirst().orElseThrow(NullPointerException::new)).get(0).getAsJsonObject();
        }
        throw new RuntimeException("API didn't return expected result!");
    }

    public static JsonObject getOutfitObject(String outfitId) {
        return Util.getRootArray(Util.invokeAPI("outfit", "outfit_id=" + outfitId + "&c:resolve=member_character(times)"));
    }

    public static JsonArray getPlayerData(Collection<String> uidList, String... requests) {
        if (requests.length == 0) {
            throw new UnsupportedOperationException();
        }
        String uid = String.join(",", uidList);
        JsonObject root = invokeAPI("character", "character_id=" + uid + "&c:resolve=" + String.join(",", requests));
        if (root.has(RETURNED) && root.get(RETURNED).getAsInt() == uidList.size()) {
            return root.getAsJsonArray("character_list");
        }
        throw new RuntimeException();
    }

    public static Map<String, JsonArray> getPlayerObject(Collection<String> uidList, String request, String object) {
        Map<String, JsonArray> ret = new HashMap<>();
        JsonArray array = getPlayerData(uidList, request);
        String[] obj = object.split("\\.");
        for (int i = 0; i < array.size(); i++) {
            JsonObject jo = array.get(i).getAsJsonObject();
            String name = jo.getAsJsonObject("name").get("first").getAsString();
            for (int j = 0; j < obj.length - 1; j++) {
                jo = jo.getAsJsonObject(obj[j]);
            }
            JsonArray stats = jo.getAsJsonArray(obj[obj.length - 1]);
            ret.put(name, stats);
        }
        return ret;
    }

    /**
     * Gets the Kill and Death JSON objects for the provided List with player UID's
     */
    public static Map<String, Map.Entry<JsonObject, JsonObject>> getKDInfoHistory(Map<String, JsonArray> historyStats) {
        return historyStats.entrySet().stream()
                .map(e -> {
                    JsonObject kills = null, deaths = null;
                    for (int j = 0; j < e.getValue().size(); j++) {
                        JsonObject so = e.getValue().get(j).getAsJsonObject();
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
                    return new AbstractMap.SimpleEntry<>(e.getKey(), new AbstractMap.SimpleEntry<>(kills, deaths));
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
