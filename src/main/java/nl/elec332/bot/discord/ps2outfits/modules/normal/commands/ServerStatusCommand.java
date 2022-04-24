package nl.elec332.bot.discord.ps2outfits.modules.normal.commands;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import nl.elec332.bot.discord.ps2outfits.CommandHelper;
import nl.elec332.bot.discord.ps2outfits.PS2BotConfigurator;
import nl.elec332.bot.discord.ps2outfits.modules.normal.NormalModule;
import nl.elec332.discord.bot.core.api.util.SimpleCommand;
import nl.elec332.planetside2.ps2api.api.objects.world.IServer;

/**
 * Created by Elec332 on 22/05/2021
 */
public class ServerStatusCommand extends SimpleCommand<NormalModule> {

    public ServerStatusCommand() {
        super("ServerStatus", "Shows server status", "serverName");
    }

    @Override
    public boolean executeCommand(MessageChannel channel, Message message, Member member, NormalModule config, String args) {
        IServer server = PS2BotConfigurator.API.getServers().getByName(args);
        if (server == null) {
            channel.sendMessage("Invalid server name: " + args).submit();
            return true;
        }
        CommandHelper.postServerData(channel, server);

        return true;
    }

}
