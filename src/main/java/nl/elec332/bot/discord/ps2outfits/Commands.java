package nl.elec332.bot.discord.ps2outfits;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import nl.elec332.bot.discord.ps2outfits.util.Outfit;
import nl.elec332.bot.discord.ps2outfits.util.PS2Server;

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
        public void executeCommand(TextChannel channel, String outfitID, String... args) {
            CommandHelper.postPlayerData(channel, outfitID, "today", Instant.now().minus(1, ChronoUnit.DAYS), uidList -> CommandHelper.getKDInfo(uidList, CommandHelper.DAILY));
        }

    },
    WEEKLYKD("Shows weekly KD stats for everyone in your outfit.") {
        @Override
        public void executeCommand(TextChannel channel, String outfitID, String... args) {
            CommandHelper.postPlayerData(channel, outfitID, "this week", Instant.now().minus(7, ChronoUnit.DAYS), uidList -> CommandHelper.getKDInfo(uidList, CommandHelper.WEEKLY));
        }

    },
    MONTHLYKD("Shows monthly KD stats for everyone in your outfit.") {
        @Override
        public void executeCommand(TextChannel channel, String outfitID, String... args) {
            CommandHelper.postPlayerData(channel, outfitID, "this month", Instant.now().minus(30, ChronoUnit.DAYS), uidList -> CommandHelper.getKDInfo(uidList, CommandHelper.MONTHLY));
        }

    },
    DAILYSTATS("Shows daily stats for your outfit.") {
        @Override
        public void executeCommand(TextChannel channel, String outfitID, String... args) {
            Outfit outfit = Outfit.getOutfit(outfitID);
            List<String> onlineMembers = outfit.getOnlinePlayersBefore(Instant.now().minus(1, ChronoUnit.DAYS));
            Map<String, JsonArray> allStats = Util.getPlayerObject(onlineMembers, "stat", "stats.stat");
            Map<String, JsonArray> historyStats = Util.getPlayerObject(onlineMembers, "stat_history", "stats.stat_history");
            List<Map.Entry<String, String>> info = CommandHelper.getKDInfo(historyStats, CommandHelper.DAILY);

            String title = outfit.getName() + " daily stats";
            String description = "Out of " + outfit.getMembers() + " members, " + onlineMembers.size() + " have been online today";
            if (info.size() != onlineMembers.size()) {
                description += "\n (" + info.size() + " members were active in that period)";
            }
            EmbedBuilder builder = new EmbedBuilder()
                    .setTitle(title)
                    .setDescription(description);

            String kdStr = CommandHelper.getKDInfo(historyStats, CommandHelper.DAILY).stream()
                    .limit(10)
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining("\n"));
            builder.addField("K/D Top-10", kdStr, false);

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
                    .collect(Collectors.joining("\n"));
            builder.addField("Assists Top-10", assistStr, false);

            String capDef = historyStats.entrySet().stream()
                    .map(e -> {
                        JsonObject cap = null;
                        JsonArray a = e.getValue();
                        for (int i = 0; i < a.size(); i++) {
                            JsonObject o = a.get(i).getAsJsonObject();
                            String sn = o.get("stat_name").getAsString();
                            if (sn.equals("facility_capture")) {
                                cap = o;
                                break;
                            }
                        }
                        if (cap == null) {
                            return null;
                        }
                        int c = Integer.parseInt(cap.getAsJsonObject("day").get("d01").getAsString());
                        return new AbstractMap.SimpleEntry<>(e.getKey(), c);
                    })
                    .filter(Objects::nonNull)
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .limit(10)
                    .collect(Collectors.joining("\n"));
            builder.addField("Facilities captured Top-10:", capDef, false);

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
        public void executeCommand(TextChannel channel, String outfit, String... args) {
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
                String argsDesc = cmd.args.isEmpty() ? "" : "<" + cmd.args + ">";
                builder.addField("!" + cmd.toString().toLowerCase(Locale.ROOT) + extra + argsDesc, cmd.help, false);
            }

            channel.sendMessage(builder.build()).submit();
        }

    }, COBALTSTATUS("Shows cobalt's status") {
        @Override
        void addAliases(Consumer<String> reg) {
            reg.accept("cobalt");
        }

        @Override
        public void executeCommand(TextChannel channel, String outfit, String... args) {
            CommandHelper.postServerData(channel, PS2Server.COBALT);
        }

    },
    SERVERSTATUS("Shows server status", "serverName") {
        @Override
        public void executeCommand(TextChannel channel, String outfit, String... args) {
            if (args.length == 1) {
                String name = args[0];
                PS2Server server = PS2Server.getServer(name);
                if (server == null) {
                    channel.sendMessage("Invalid server name: " + name).submit();
                }
                CommandHelper.postServerData(channel, server);
            } else {
                throw new UnsupportedOperationException("Too many args!");
            }
        }

    };

    Commands(String help) {
        this(help, "");
    }

    Commands(String help, String args) {
        this.help = Objects.requireNonNull(help);
        this.args = Objects.requireNonNull(args);
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

    public abstract void executeCommand(TextChannel channel, String outfit, String... args);

}
