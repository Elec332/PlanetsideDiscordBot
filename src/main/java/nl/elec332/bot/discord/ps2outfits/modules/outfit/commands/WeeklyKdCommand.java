package nl.elec332.bot.discord.ps2outfits.modules.outfit.commands;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import nl.elec332.bot.discord.ps2outfits.CommandHelper;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.OutfitConfig;
import nl.elec332.discord.bot.core.api.util.SimpleCommand;

import java.util.Calendar;

/**
 * Created by Elec332 on 22/05/2021
 */
public class WeeklyKdCommand extends SimpleCommand<OutfitConfig> {

    public WeeklyKdCommand() {
        super("WeeklyKD", "Shows weekly KD stats for everyone in your outfit.");
    }

    @Override
    public boolean executeCommand(MessageChannel channel, Message message, Member member, OutfitConfig config, String args) {
        CommandHelper.postKDInfo(channel, config.getOutfit(), "this week", CommandHelper.fromCal(cal -> cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek())), h -> h.getWeek(1));
        return true;
    }

}