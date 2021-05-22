package nl.elec332.bot.discord.ps2outfits.api;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.Collection;

/**
 * Created by Elec332 on 17/05/2021
 */
public interface ICommand<C> {

    String getHelpText();

    String getArgs();

    Collection<String> getAliases();

    boolean executeCommand(TextChannel channel, Member member, C config, String... args);

}
