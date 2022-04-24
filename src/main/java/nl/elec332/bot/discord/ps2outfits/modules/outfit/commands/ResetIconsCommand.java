package nl.elec332.bot.discord.ps2outfits.modules.outfit.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import nl.elec332.bot.discord.ps2outfits.CommandHelper;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.OutfitConfig;
import nl.elec332.discord.bot.core.api.util.SimpleCommand;

/**
 * Created by Elec332 on 11/09/2021
 */
public class ResetIconsCommand extends SimpleCommand<OutfitConfig> {

    public ResetIconsCommand() {
        super("resetIcons", "Resets all bot-added icons in case some are missing.");
    }

    @Override
    public boolean executeCommand(MessageChannel channel, Message message, Member member, OutfitConfig outfitConfig, String strings) {
        if (!member.hasPermission(Permission.ADMINISTRATOR)) {
            channel.sendMessage("You can only use this command as an administrator!").submit();
            return true;
        }
        CommandHelper.addIcons(outfitConfig.getGuild(channel.getJDA()), outfitConfig.getOutfit(), outfitConfig.getClassEmotes(), outfitConfig.getMiscEmotes());
        return true;
    }

}
