package nl.elec332.bot.discord.ps2outfits;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import nl.elec332.planetside2.api.objects.player.IPlayer;
import nl.elec332.planetside2.api.objects.player.IPlayerRequest;
import nl.elec332.planetside2.api.objects.player.request.IFactionWeaponStat;
import nl.elec332.planetside2.api.objects.world.IServer;
import nl.elec332.planetside2.util.PS2ItemSets;

import java.util.*;
import java.util.function.Consumer;

/**
 * Created by Elec332 on 03/04/2021
 */
public enum Commands {

    HELP("Shows info about all available commands.") {
        @Override
        void addAliases(Consumer<String> reg) {
            reg.accept("outfithelp");
            reg.accept("outfitcommands");
            reg.accept("ps2commands");
            reg.accept("ps2commands");
        }

        @Override
        public void executeCommand(TextChannel channel, Member member, String... args) {
            EmbedBuilder builder = new EmbedBuilder()
                    .setTitle("PS2OutfitStats help info")
                    .setDescription("All available commands & descriptions\n\n")
                    .addBlankField(false);

            builder.addField("!setoutfit <OutfitName|OutfitTag>", "Sets the outfit for this Discord server, can only used by server admins!", false);
            for (Commands cmd : Commands.values()) {
                String extra = " ";
                if (!cmd.getAliases().isEmpty()) {
                    extra = " (!" + String.join(", !", cmd.getAliases()) + ") ";
                }
                String argsDesc = cmd.args.isEmpty() ? "" : "<" + cmd.getArgs() + ">";
                builder.addField("!" + cmd.toString().toLowerCase(Locale.ROOT) + extra + argsDesc, cmd.getHelpText(), false);
            }
            for (OutfitCommands cmd : OutfitCommands.values()) {
                String extra = " ";
                if (!cmd.getAliases().isEmpty()) {
                    extra = " (!" + String.join(", !", cmd.getAliases()) + ") ";
                }
                String argsDesc = cmd.getArgs().isEmpty() ? "" : "<" + cmd.getArgs() + ">";
                builder.addField("!" + cmd.toString().toLowerCase(Locale.ROOT) + extra + argsDesc, cmd.getHelpText(), false);
            }

            channel.sendMessage(builder.build()).submit();
        }

    }, COBALTSTATUS("Shows cobalt's status") {
        @Override
        void addAliases(Consumer<String> reg) {
            reg.accept("cobalt");
        }

        @Override
        public void executeCommand(TextChannel channel, Member member, String... args) {
            CommandHelper.postServerData(channel, Main.API.getServers().getByName("cobalt"));
        }

    },
    SERVERSTATUS("Shows server status", "serverName") {
        @Override
        public void executeCommand(TextChannel channel, Member member, String... args) {
            if (args.length == 1) {
                String name = args[0];
                IServer server = Main.API.getServers().getByName(name);
                if (server == null) {
                    channel.sendMessage("Invalid server name: " + name).submit();
                    return;
                }
                CommandHelper.postServerData(channel, server);
            } else {
                throw new UnsupportedOperationException("Too many args!");
            }
        }

    },
    NOSEGUNKILLS("Shows kills with the non-AI ESF noseguns\n Without parameters it runs for yourself", "playername") {
        @Override
        public void executeCommand(TextChannel channel, Member member, String... args) {
            String playerName = args.length == 0 ? null : args[0];
            if (playerName == null || playerName.isEmpty()) {
                playerName = member.getEffectiveName();
            }
            IPlayer player = Main.API.getPlayerManager().getByName(playerName);
            if (player == null) {
                return;
            }
            IPlayerRequest<IFactionWeaponStat> stats = Main.API.getPlayerRequestHandler().getSlimCharacterWeaponStats(Collections.singleton(player.getId()), PS2ItemSets.ALL_AA_NOSE_GUNS.without(PS2ItemSets.AI_NOSE_GUNS)).getAsList().get(0);
            if (stats == null) {
                return;
            }
            EmbedBuilder builder = new EmbedBuilder()
                    .setTitle(player.getName() + "'s nosegun kills")
                    .setDescription("(Kills made with A2G guns do not count)");

            stats.getResponseByName("weapon_kills").forEach(s -> {
                builder.addField(Main.API.getItems().getCached(s.getItemId()).getName(), "Kills: " + s.getEnemyKills(player.getFaction()), false);
            });

            builder.addField("Total", "Kills: " + stats.getResponseByName("weapon_kills").mapToInt(IFactionWeaponStat::getTotal).sum(), false);
            channel.sendMessage(builder.build()).submit();
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

    public abstract void executeCommand(TextChannel channel, Member member, String... args);

}
