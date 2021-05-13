package nl.elec332.bot.discord.ps2outfits;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import nl.elec332.planetside2.api.objects.world.IServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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
        public void executeCommand(TextChannel channel, String... args) {
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
        public void executeCommand(TextChannel channel, String... args) {
            CommandHelper.postServerData(channel, Main.API.getServers().getByName("cobalt"));
        }

    },
    SERVERSTATUS("Shows server status", "serverName") {
        @Override
        public void executeCommand(TextChannel channel, String... args) {
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

    public abstract void executeCommand(TextChannel channel, String... args);

}
