package nl.elec332.bot.discord.ps2outfits.modules.outfit.commands;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import nl.elec332.bot.discord.ps2outfits.modules.CommandHelper;
import nl.elec332.bot.discord.ps2outfits.api.util.SimpleCommand;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.OutfitConfig;

import java.util.Calendar;

/**
 * Created by Elec332 on 22/05/2021
 */
public class WeeklyKdCommand extends SimpleCommand<OutfitConfig> {

    public WeeklyKdCommand() {
        super("WeeklyKD", "Shows weekly KD stats for everyone in your outfit.");
    }

    @Override
    public boolean executeCommand(TextChannel channel, Member member, OutfitConfig config, String... args) {
        CommandHelper.postKDInfo(channel, config.getOutfit(), "this week", CommandHelper.fromCal(cal -> cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek())), h -> h.getWeek(1));
        return true;
    }

}