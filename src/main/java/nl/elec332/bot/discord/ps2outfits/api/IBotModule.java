package nl.elec332.bot.discord.ps2outfits.api;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.function.Consumer;

/**
 * Created by Elec332 on 17/05/2021
 */
public interface IBotModule<C> {

    default void onBotConnected(JDA jda) {
    }

    String getModuleName();

    boolean canRunCommand(TextChannel channel, Member member, C config, ICommand<C> command);

    void registerCommands(Consumer<ICommand<C>> registry);

    C getConfigFor(long serverId);

}
