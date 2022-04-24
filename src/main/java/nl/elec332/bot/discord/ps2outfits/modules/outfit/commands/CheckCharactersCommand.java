package nl.elec332.bot.discord.ps2outfits.modules.outfit.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.utils.concurrent.Task;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.MiscEmotes;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.OutfitConfig;
import nl.elec332.discord.bot.core.api.util.SimpleCommand;
import nl.elec332.discord.bot.core.util.AsyncExecutor;
import nl.elec332.planetside2.ps2api.api.objects.player.IPlayer;

import java.util.List;


/**
 * Created by Elec332 on 09/10/2021
 */
public class CheckCharactersCommand extends SimpleCommand<OutfitConfig> {

    public CheckCharactersCommand(Runnable save) {
        super("CheckCharacters", "Checks PS2 names and registers new ones if needed.");
        this.save = save;
    }

    private final Runnable save;

    @Override
    public boolean executeCommand(MessageChannel channel, Message message, Member member, OutfitConfig config, String args) {
        if (!member.hasPermission(Permission.ADMINISTRATOR)) {
            channel.sendMessage("You can only use this command as an administrator!").submit();
            return true;
        }
        Task<List<Member>> mt = ((TextChannel) channel).getGuild().loadMembers();
        System.out.println("CheckCharacters");
        mt.onSuccess(members -> {
            members.stream()
                    .filter(m -> !config.hasMappedAccount(m))
                    .filter(m -> !m.getUser().isBot())
                    .forEach(m -> {
                        IPlayer player;
                        try {
                            player = config.getPlayer(m);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return;
                        }
                        System.out.println(m.getEffectiveName() + "   " + player);
                        if (player != null && player.getOutfit() != null && player.getOutfit().getId() == config.getOutfit().getId()) {
                            AsyncExecutor.executeAsync(() -> {
                                Message msg = channel.sendMessage(".").submit().join();
                                msg.editMessage(m.getAsMention() + " to PS2 player: " + player.getName()).submit();
                                msg.addReaction(MiscEmotes.CHECK_MARK_EMOJI).queue();
                                msg.addReaction(MiscEmotes.RED_CROSS_EMOJI).queue();
                                config.addConfirm(msg.getIdLong(), mbr -> {
                                    synchronized (save) {
                                        if (mbr.hasPermission(Permission.ADMINISTRATOR)) {
                                            config.addPlayerMapping(m, player, channel, save);
                                        }
                                    }
                                });
                            });
                        }
                    });
            channel.sendMessage("Character check finished").submit();

        });
        return true;
    }

}
