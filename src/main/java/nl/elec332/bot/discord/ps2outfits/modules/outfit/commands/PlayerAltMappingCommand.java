package nl.elec332.bot.discord.ps2outfits.modules.outfit.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.OutfitConfig;
import nl.elec332.discord.bot.core.api.util.SimpleCommand;

import java.util.Objects;

/**
 * Created by Elec332 on 19/08/2021
 */
public class PlayerAltMappingCommand extends SimpleCommand<OutfitConfig> {

    public PlayerAltMappingCommand(Runnable save) {
        super("mapPlayerAlt", "Binds a discord member to one of his/her alt accounts", "PS2Name", "Member_ping");
        this.save = save;
    }

    private final Runnable save;

    @Override
    public boolean executeCommand(MessageChannel channel, Message message, Member member, OutfitConfig outfitConfig, String... strings) {
        if (!member.hasPermission(Permission.ADMINISTRATOR)) {
            channel.sendMessage("You can only use this command as an administrator!").submit();
            return true;
        }
        if (message.getMentionedMembers().size() > 1) {
            channel.sendMessage("Multiple players mentioned!").submit();
            return true;
        }
        Member mapped = Objects.requireNonNull(message.getMentionedMembers().isEmpty() ? member : message.getMentionedMembers().get(0));
        outfitConfig.addPlayerAltMapping(mapped, strings[0].split(" ")[0], (TextChannel) channel, save);
        return true;
    }

}
