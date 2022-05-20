package nl.elec332.bot.discord.ps2outfits.modules.outfit.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import nl.elec332.bot.discord.ps2outfits.CommandHelper;
import nl.elec332.bot.discord.ps2outfits.PS2BotConfigurator;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.OutfitConfig;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.OutfitRoleTypes;
import nl.elec332.discord.bot.core.api.util.SimpleCommand;
import nl.elec332.planetside2.ps2api.api.objects.player.IOutfit;

import java.util.List;

/**
 * Created by Elec332 on 22/05/2021
 */
public class SetOutfitCommand extends SimpleCommand<OutfitConfig> {

    public SetOutfitCommand(Runnable save) {
        super("setOutfit", "Sets the outfit for this Discord server, can only used by server admins!", "OutfitName|OutfitTag");
        this.save = save;
    }

    private final Runnable save;
    private static final String BANNED = "<>[]";

    @Override
    public boolean executeCommand(MessageChannel channel, Message message, Member member, OutfitConfig config, String arg) {
        if (member.hasPermission(Permission.ADMINISTRATOR)) {
            for (char c : BANNED.toCharArray()) {
                if (arg.contains("" + c)) {
                    channel.sendMessage("An outfit name or tag cannot contain any of the following characters: \"" + BANNED + "\"").submit();
                    return true;
                }
            }
            IOutfit outfit = PS2BotConfigurator.API.getOutfitManager().getByName(arg);
            if (outfit == null) {
                channel.sendMessage("Failed to find outfit: \"" + arg + "\"\nPlease make sure you give the bot your full outfit name or outfit tag.").submit();
                return true;
            }

            config.setOutfit(outfit);
            List<Role> roles = message.getGuild().getRolesByName(outfit.getTag(), true);
            if (roles.size() == 1) {
                config.setOutfitRole(OutfitRoleTypes.MEMBER, roles.get(0));
            }
            save.run();
            CommandHelper.addIcons(config.getGuild(channel.getJDA()), config.getOutfit(), config.getClassEmotes(), config.getMiscEmotes());
            channel.sendMessage("Successfully set outfit ID to: " + outfit.getId() + " (" + outfit.getName() + ") for this server!").submit();
        } else {
            channel.sendMessage("You can only use this command as an administrator!").submit();
        }
        return true;
    }

}
