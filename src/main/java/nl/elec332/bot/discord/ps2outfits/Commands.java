package nl.elec332.bot.discord.ps2outfits;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import nl.elec332.bot.discord.ps2outfits.util.Outfit;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by Elec332 on 03/04/2021
 */
public enum Commands {

    DAILYKD("Shows daily KD stats for everyone in your outfit.") {
        @Override
        public void executeCommand(TextChannel channel, String outfitID) {
            CommandHelper.postPlayerData(channel, outfitID, "today", Instant.now().minus(1, ChronoUnit.DAYS), CommandHelper::getDailyKDInfo);
        }

    },
    WEEKLYKD("Shows weekly KD stats for everyone in your outfit.") {
        @Override
        public void executeCommand(TextChannel channel, String outfitID) {
            CommandHelper.postPlayerData(channel, outfitID, "this week", Instant.now().minus(7, ChronoUnit.DAYS), CommandHelper::getWeeklyKDInfo);
        }

    },
    MONTHLYKD("Shows monthly KD stats for everyone in your outfit.") {
        @Override
        public void executeCommand(TextChannel channel, String outfitID) {
            CommandHelper.postPlayerData(channel, outfitID, "this month", Instant.now().minus(30, ChronoUnit.DAYS), CommandHelper::getMonthlyKDInfo);
        }

    },
    DAILYSTATS("Shows daily stats for your outfit.") {
        @Override
        public void executeCommand(TextChannel channel, String outfitID) {
            Outfit outfit = Outfit.getOutfit(outfitID);
            List<String> onlineMembers = outfit.getOnlinePlayersBefore(Instant.now().minus(1, ChronoUnit.DAYS));
            String title = outfit.getName() + " daily stats";
            String description = "Out of " + outfit.getMembers() + " members, " + onlineMembers.size() + " have been online today";
            List<Map.Entry<String, String>> info = CommandHelper.getDailyKDInfo(onlineMembers);
            if (info.size() != onlineMembers.size()) {
                description += "\n (" + info.size() + " members were active in that period)";
            }
            EmbedBuilder builder = new EmbedBuilder()
                    .setTitle(title)
                    .setDescription(description);

            StringBuilder kd = new StringBuilder();
            for (int i = 0; i < Math.min(10, info.size()); i++) {
                Map.Entry<String, String> data = info.get(i);
                kd.append(data.getKey()).append(": ")
                        .append(data.getValue())
                        .append("\n");
            }
            builder.addField("K/D Top-10", kd.toString(), false);

            Map<String, JsonArray> allStats = Util.getPlayerObject(onlineMembers, "stat", "stats.stat");
            String assistStr = allStats.entrySet().stream()
                    .map(e -> {
                        JsonObject assists = null;
                        JsonArray a = e.getValue();
                        for (int i = 0; i < a.size(); i++) {
                            JsonObject o = a.get(i).getAsJsonObject();
                            if (o.get("stat_name").getAsString().equals("assist_count")) {
                                assists = o;
                                break;
                            }
                        }
                        if (assists == null) {
                            return null;
                        }
                        return new AbstractMap.SimpleEntry<>(e.getKey(), Integer.parseInt(assists.get("value_daily").getAsString()));
                    })
                    .filter(Objects::nonNull)
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .limit(10)
                    .peek(System.out::println)
                    .collect(Collectors.joining("\n"));

            builder.addField("Assists Top-10", assistStr, false);

            builder.addField("Repair Ribbons Top-5", "//todo", false);

            builder.addField("Revive & Heal Ribbons Top-5", "//todo", false);

            channel.sendMessage(builder.build()).submit();
        }

    },
    HELP("Shows info about all available commands.") {
        @Override
        void addAliases(Consumer<String> reg) {
            reg.accept("outfithelp");
            reg.accept("outfitcommands");
        }

        @Override
        public void executeCommand(TextChannel channel, String outfit) {
            EmbedBuilder builder = new EmbedBuilder()
                    .setTitle("PS2OutfitStats help info")
                    .setDescription("All available commands & descriptions\n\n")
                    .addBlankField(false);

            builder.addField("!setoutfit <OutfitName|OutfitTag>", "Sets the outfit for this Discord server, can only used by server admins!", false);
            for (Commands cmd : Commands.values()) {
                String extra = " ";
                if (!cmd.aliases.isEmpty()) {
                    extra = " (!" + String.join(", !", cmd.aliases) + ") ";
                }
                builder.addField("!" + cmd.toString().toLowerCase(Locale.ROOT) + extra + cmd.args, cmd.help, false);
            }

            channel.sendMessage(builder.build()).submit();
        }

    };

    Commands(String help) {
        this(help, "");
    }

    Commands(String help, String args) {
        this.help = help;
        this.args = args;
        this.aliases = new ArrayList<>();
        addAliases(this.aliases::add);
    }

    private final String help;
    private final String args;
    private final List<String> aliases;

    void addAliases(Consumer<String> reg) {
    }

    public String getHelpText() {
        return help;
    }

    public String getArgs() {
        return args;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public abstract void executeCommand(TextChannel channel, String outfit);

}
