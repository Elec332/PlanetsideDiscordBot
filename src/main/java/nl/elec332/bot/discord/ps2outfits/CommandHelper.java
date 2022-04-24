package nl.elec332.bot.discord.ps2outfits;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.MiscEmotes;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.OutfitConfig;
import nl.elec332.planetside2.ps2api.api.objects.IHasImage;
import nl.elec332.planetside2.ps2api.api.objects.player.*;
import nl.elec332.planetside2.ps2api.api.objects.player.request.ICharacterStatHistory;
import nl.elec332.planetside2.ps2api.api.objects.world.IServer;
import nl.elec332.planetside2.ps2api.util.NetworkUtil;
import nl.elec332.planetside2.ps2api.util.PS2Class;

import java.text.DecimalFormat;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/**
 * Created by Elec332 on 03/04/2021
 */
public class CommandHelper {

    private static final int MAX_CHARS = 5500;
    private static final int MAX_FIELDS = 25;

    public static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#0.00");

    public static User getUser(long id, JDA jda) {
        try {
            return jda.retrieveUserById(id).submit().join();
        } catch (Exception e) {
            return null;
        }
    }

    public static Member getGuildMember(long id, Guild guild) {
        try {
            return guild.getJDA().retrieveUserById(id).flatMap(guild::retrieveMember).submit().join();
        } catch (Exception e) {
            User user = getUser(id, guild.getJDA());
            System.out.println("Failed to retrieve member with ID: " + id + "  User: " + (user == null ? null : user.getName()));
            return null;
        }
    }

    public static void addIcons(Guild guild, IOutfit outfit, Map<PS2Class, Emote> emotes, Map<MiscEmotes, Emote> emotes2) {
        if (guild == null) {
            return;
        }
        List<ListedEmote> ets = guild.retrieveEmotes().submit().join();
        Arrays.stream(PS2Class.values()).forEach(clazz -> {
            IPlayerClass c = clazz.getClass(PS2BotConfigurator.API.getPlayerClasses());
            emotes.put(clazz, addIcon(guild, ets, c.getMonolithicName(), c));
        });
        Arrays.stream(MiscEmotes.values()).forEach(img -> {
            IHasImage obj = img.getImage(outfit, PS2BotConfigurator.API);
            if (obj != null) {
                emotes2.put(img, addIcon(guild, ets, Objects.requireNonNull(img.getName(outfit, obj)), obj));
            }
        });
    }

    private static Emote addIcon(Guild guild, List<ListedEmote> emotes, String name, IHasImage clazz) {
        return emotes.stream()
                .filter(le -> le.getName().equals(name))
                .findFirst()
                .map(le -> (Emote) le)
                .orElseGet(() -> {
                    try {
                        return guild.createEmote(name, Icon.from(PS2BotConfigurator.API_ACCESSOR.getCensusAPI().getImage(Objects.requireNonNull(clazz)), Icon.IconType.PNG)).submit().join();
                    } catch (Exception e) {
                        throw new RuntimeException(name + " in " + guild.getName(), e);
                    }
                });
    }

    public static Instant fromCal(Consumer<Calendar> mod) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);

        mod.accept(cal);

        return Instant.ofEpochMilli(cal.getTimeInMillis());
    }

    public static String trimPlayerName(String name) {
        String newName = name;
        do {
            name = newName;
            int i = name.indexOf("[");
            int j = name.indexOf("]");
            if (i >= 0 && j >= 0) {
                newName = name.replace(name.substring(i, j + 1), "").trim();
            }
        } while (!name.equalsIgnoreCase(newName));
        return name;
    }

    public static IPlayer getPlayer(OutfitConfig config, Member member, String name) {
        if (name != null && !name.isEmpty()) {
            return PS2BotConfigurator.API.getPlayerManager().getByName(name);
        }
        if (member == null) {
            return null;
        }
        IPlayer ret = config.getPlayer(member);
        if (ret == null) {
            return PS2BotConfigurator.API.getPlayerManager().getByName(trimPlayerName(member.getEffectiveName()));
        }
        return ret;
    }

    public static void postServerData(MessageChannel channel, IServer server) {
        JsonObject o = NetworkUtil.readJsonFromURL("https://ps2.fisu.pw/api/population/?world=" + server.getId(), false);
        o = o.getAsJsonArray("result").get(0).getAsJsonObject();
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle("Cobalt server status")
                .addField("NC", "" + o.get("nc").getAsInt(), false)
                .addField("TR", "" + o.get("tr").getAsInt(), false)
                .addField("VS", "" + o.get("vs").getAsInt(), false)
                .addField("NS", "" + o.get("ns").getAsInt(), false);

        channel.sendMessageEmbeds(builder.build()).submit();
    }

    public static Collection<Map.Entry<String, String>> getKDInfo(IPlayerResponseList<IPlayerRequestList<ICharacterStatHistory>> historyStats, ToIntFunction<ICharacterStatHistory> getter) {
        return historyStats.streamMappedResponse(p -> {
            ICharacterStatHistory kills = p.getFirstResponseByName("kills");
            ICharacterStatHistory deaths = p.getFirstResponseByName("deaths");
            if (kills == null || deaths == null) {
                return null;
            }
            int k = getter.applyAsInt(kills);
            if (k == 0) {
                return null;
            }
            float d = Math.max(1, getter.applyAsInt(deaths));
            float kdr = k / d;
            String kds = NUMBER_FORMAT.format(kdr);
            if (k < 15 || d < 2) {
                kdr = -1 + kdr / 100;
                kds = "---";
            }
            return new AbstractMap.SimpleEntry<>(kdr, "K/D: " + kds + "  Kills: " + k);
        }, Comparator.<Float>reverseOrder())
                .collect(Collectors.toList());
    }

    public static void postKDInfo(MessageChannel channel, IOutfit outfit, String sinceStr, Instant since, ToIntFunction<ICharacterStatHistory> getter) {
        Collection<Long> onlineMembers = outfit.getPlayerIds(p -> p.getLastPlayerActivity().isAfter(since));
        if (onlineMembers.isEmpty()) {
            channel.sendMessage("No members have been online!").submit();
            return;
        }
        IPlayerResponseList<IPlayerRequestList<ICharacterStatHistory>> historyStats = PS2BotConfigurator.API.getPlayerRequestHandler().getSlimCharacterStatHistory(onlineMembers, "deaths", "kills");
        String title = outfit.getName() + " KD info";
        String description = "Out of " + outfit.getMembers() + " members, " + onlineMembers.size() + " have been online " + sinceStr;
        Collection<Map.Entry<String, String>> kdInfo = CommandHelper.getKDInfo(historyStats, i -> i.getDay(1));
        if (kdInfo.size() != onlineMembers.size()) {
            description += "\n (" + kdInfo.size() + " members were active in that period)";
        }
        postPlayerData(channel, title, description, kdInfo);
    }

    public static void postPlayerData(MessageChannel channel, String title_, String description, Collection<Map.Entry<String, String>> fields) {
        try {
            EmbedBuilder builder = new EmbedBuilder()
                    .setTitle(title_)
                    .setDescription(description);
            Iterator<Map.Entry<String, String>> it = fields.iterator();
            while (it.hasNext()) {
                int counter = 0;
                while (it.hasNext()) {
                    Map.Entry<String, String> is = it.next();
                    String title = is.getKey();
                    String value = is.getValue();
                    int length = title.length() + value.length();
                    if (counter + length > MAX_CHARS || builder.getFields().size() >= MAX_FIELDS) {
                        break;
                    }
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

}
