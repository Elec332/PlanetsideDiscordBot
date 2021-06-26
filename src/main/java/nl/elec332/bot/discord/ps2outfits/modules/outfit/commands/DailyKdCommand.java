package nl.elec332.bot.discord.ps2outfits.modules.outfit.commands;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import nl.elec332.bot.discord.ps2outfits.CommandHelper;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.OutfitConfig;
import nl.elec332.discord.bot.core.api.util.SimpleCommand;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Created by Elec332 on 22/05/2021
 */
public class DailyKdCommand extends SimpleCommand<OutfitConfig> {

    public DailyKdCommand() {
        super("DailyKD", "Shows daily KD stats for everyone in your outfit.");
    }

    @Override
    public boolean executeCommand(MessageChannel channel, Member member, OutfitConfig config, String... args) {
        CommandHelper.postKDInfo(channel, config.getOutfit(), "today", Instant.now().minus(1, ChronoUnit.DAYS), h -> h.getDay(1));
        return true;
    }

}
