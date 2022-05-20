package nl.elec332.bot.discord.ps2outfits.modules.outfit.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.utils.concurrent.Task;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.OutfitConfig;
import nl.elec332.discord.bot.core.api.util.SimpleCommand;
import nl.elec332.discord.bot.core.util.AsyncExecutor;
import nl.elec332.planetside2.ps2api.api.objects.player.IPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Elec332 on 09/10/2021
 */
public class ListCharactersCommand extends SimpleCommand<OutfitConfig> {

    public ListCharactersCommand() {
        super("ListCharacters", "Lists all PS2 character mappings.");
    }

    @Override
    public boolean executeCommand(MessageChannel channel, Message message, Member member, OutfitConfig config, String args) {
        if (!member.hasPermission(Permission.ADMINISTRATOR)) {
            channel.sendMessage("You can only use this command as an administrator!").submit();
            return true;
        }
        AsyncExecutor.executeAsync(() -> {
            Task<List<Member>> mt = ((TextChannel) channel).getGuild().loadMembers();
            mt.onSuccess(members -> {
                List<String> s = new ArrayList<>();
                members.stream()
                        .filter(config::hasMappedAccount)
                        .forEach(m -> {
                            StringBuilder sb = new StringBuilder();
                            sb.append(m.getAsMention())
                                    .append(": ");
                            IPlayer main = config.getDirectMapping(m);
                            if (main != null) {
                                sb.append(main.getName());
                            }
                            Collection<IPlayer> alts = config.getAltMapping(m);
                            if (alts != null && !alts.isEmpty()) {
                                if (main != null) {
                                    sb.append(" (Main)");
                                }
                                alts.forEach(p -> {
                                    sb.append(", ").append(p.getName()).append(" (Alt)");
                                });
                            }
                            sb.append("\n");
                            s.add(sb.toString());
                        });
                List<String> msgp = new ArrayList<>();
                StringBuilder sb = new StringBuilder();
                for (String str : s) {
                    sb.append(str);
                    if (sb.length() > 900) {
                        msgp.add(sb.toString());
                        sb = new StringBuilder();
                    }
                }
                s.clear();
                if (sb.length() > 0) {
                    msgp.add(sb.toString());
                }
                for (int i = 0; i < msgp.size(); i++) {
                    EmbedBuilder b = new EmbedBuilder()
                            .setTitle("PS2 player mappings");

                    Message msg = channel.sendMessageEmbeds(b.build()).submit().join();
                    if (i != 0) {
                        b.setTitle(null);
                    }
                    b.addField("Mappings" + (i > 0 ? " Part " + (i + 1) : ""), msgp.get(i), false);
                    msg.editMessageEmbeds(b.build()).submit();
                }

            });
        });
        return true;
    }

}
