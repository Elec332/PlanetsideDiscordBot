package nl.elec332.bot.discord.ps2outfits.modules.normal.commands;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import nl.elec332.bot.discord.ps2outfits.modules.CommandHelper;
import nl.elec332.bot.discord.ps2outfits.api.util.SimpleCommand;
import nl.elec332.bot.discord.ps2outfits.modules.PS2BotConfigurator;
import nl.elec332.bot.discord.ps2outfits.modules.normal.NormalModule;
import nl.elec332.planetside2.api.objects.world.IServer;

/**
 * Created by Elec332 on 22/05/2021
 */
public class ServerStatusCommand extends SimpleCommand<NormalModule> {

    public ServerStatusCommand() {
        super("ServerStatus", "Shows server status", "serverName");
    }

    @Override
    public boolean executeCommand(TextChannel channel, Member member, NormalModule config, String... args) {
        if (args.length == 1) {
            String name = args[0];
            IServer server = PS2BotConfigurator.API.getServers().getByName(name);
            if (server == null) {
                channel.sendMessage("Invalid server name: " + name).submit();
                return true;
            }
            CommandHelper.postServerData(channel, server);
        } else {
            throw new UnsupportedOperationException("Too many args!");
        }
        return true;
    }

}
