package nl.elec332.bot.discord.ps2outfits.modules.outfit.commands;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import nl.elec332.bot.discord.ps2outfits.CommandHelper;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.OutfitConfig;
import nl.elec332.discord.bot.core.api.util.SimpleCommand;

import java.util.Calendar;

/**
 * Created by Elec332 on 22/05/2021
 */
public class MonthlyKdCommand extends SimpleCommand<OutfitConfig> {

    public MonthlyKdCommand() {
        super("MonthlyKD", "Shows monthly KD stats for everyone in your outfit.");
    }

    @Override
    public boolean executeCommand(MessageChannel channel, Member member, OutfitConfig config, String... args) {
        CommandHelper.postKDInfo(channel, config.getOutfit(), "this month", CommandHelper.fromCal(cal -> cal.set(Calendar.DAY_OF_MONTH, 0)), h -> h.getMonth(1));
        return true;
    }

}