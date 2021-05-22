package nl.elec332.bot.discord.ps2outfits.modules.outfit.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import nl.elec332.bot.discord.ps2outfits.api.util.SimpleCommand;
import nl.elec332.bot.discord.ps2outfits.modules.PS2BotConfigurator;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.OutfitConfig;
import nl.elec332.planetside2.api.objects.player.IOutfit;

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
    public boolean executeCommand(TextChannel channel, Member member, OutfitConfig config, String... arg) {
        String args = arg[0];
        if (member.hasPermission(Permission.ADMINISTRATOR)) {
            for (char c : BANNED.toCharArray()) {
                if (args.contains("" + c)) {
                    channel.sendMessage("An outfit name or tag cannot contain any of the following characters: \"" + BANNED + "\"").submit();
                    return true;
                }
            }
            IOutfit outfit = PS2BotConfigurator.API.getOutfitManager().getByName(args);
            if (outfit == null) {
                channel.sendMessage("Failed to find outfit: \"" + args + "\"\nPlease make sure you give the bot your full outfit name or outfit tag.").submit();
                return true;
            }

            config.setOutfit(outfit);
            save.run();
            channel.sendMessage("Successfully set outfit ID to: " + outfit.getId() + " (" + outfit.getName() + ") for this server!").submit();
        } else {
            channel.sendMessage("You can only use this command as an administrator!").submit();
        }
        return true;
    }

}
