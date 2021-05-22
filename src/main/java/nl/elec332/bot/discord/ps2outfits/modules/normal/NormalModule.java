package nl.elec332.bot.discord.ps2outfits.modules.normal;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import nl.elec332.bot.discord.ps2outfits.api.IBotModule;
import nl.elec332.bot.discord.ps2outfits.api.ICommand;
import nl.elec332.bot.discord.ps2outfits.modules.normal.commands.NoseGunKillsCommand;
import nl.elec332.bot.discord.ps2outfits.modules.normal.commands.ServerStatusCommand;

import java.util.function.Consumer;

/**
 * Created by Elec332 on 22/05/2021
 */
public class NormalModule implements IBotModule<NormalModule> {

    @Override
    public void registerCommands(Consumer<ICommand<NormalModule>> registry) {
        registry.accept(new ServerStatusCommand());
        registry.accept(new NoseGunKillsCommand());
    }

    @Override
    public String getModuleName() {
        return "normal";
    }

    @Override
    public boolean canRunCommand(TextChannel channel, Member member, NormalModule config, ICommand<NormalModule> command) {
        return true;
    }

    @Override
    public NormalModule getConfigFor(long serverId) {
        return this;
    }

}
