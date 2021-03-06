package nl.elec332.bot.discord.ps2outfits.modules.outfit.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.OutfitConfig;
import nl.elec332.discord.bot.core.api.util.SimpleCommand;
import nl.elec332.planetside2.ps2api.api.objects.player.IOutfit;
import nl.elec332.planetside2.ps2api.api.objects.player.IOutfitMember;

import java.util.stream.Collectors;

/**
 * Created by Elec332 on 22/05/2021
 */
public class OnlineMembersCommand extends SimpleCommand<OutfitConfig> {

    public OnlineMembersCommand() {
        super("OnlineMembers", "Shows which outfit members are online");
    }

    @Override
    public boolean executeCommand(MessageChannel channel, Message message, Member member, OutfitConfig config, String args) {
        IOutfit outfit = config.getOutfit();
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(outfit.getName() + " online members")
                .setDescription(outfit.getOnlineMembers()
                        .map(IOutfitMember::getPlayerName)
                        .sorted()
                        .collect(Collectors.joining("\n")));
        channel.sendMessageEmbeds(builder.build()).submit();
        return true;
    }

}
