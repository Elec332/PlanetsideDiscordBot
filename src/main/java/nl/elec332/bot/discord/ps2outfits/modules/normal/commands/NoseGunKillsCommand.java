package nl.elec332.bot.discord.ps2outfits.modules.normal.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import nl.elec332.bot.discord.ps2outfits.CommandHelper;
import nl.elec332.bot.discord.ps2outfits.PS2BotConfigurator;
import nl.elec332.bot.discord.ps2outfits.modules.normal.NormalModule;
import nl.elec332.discord.bot.core.api.util.SimpleCommand;
import nl.elec332.planetside2.ps2api.api.objects.player.IPlayer;
import nl.elec332.planetside2.ps2api.api.objects.player.IPlayerRequestList;
import nl.elec332.planetside2.ps2api.api.objects.player.request.IFactionWeaponStat;
import nl.elec332.planetside2.ps2api.util.PS2ItemSets;

import java.util.Collections;

/**
 * Created by Elec332 on 22/05/2021
 */
public class NoseGunKillsCommand extends SimpleCommand<NormalModule> {

    public NoseGunKillsCommand() {
        super("NoseGunKills", "Shows kills with the non-AI ESF noseguns\n Without parameters it runs for yourself", "playername");
    }

    @Override
    public boolean executeCommand(MessageChannel channel, Member member, NormalModule config, String... args) {
        IPlayer player = CommandHelper.getPlayer(member, args.length == 0 ? null : args[0]);
        if (player == null) {
            channel.sendMessage("Failed to find player").submit();
            return true;
        }
        IPlayerRequestList<IFactionWeaponStat> stats = PS2BotConfigurator.API.getPlayerRequestHandler().getSlimCharacterWeaponStats(Collections.singleton(player.getId()), PS2ItemSets.ALL_AA_NOSE_GUNS.without(PS2ItemSets.AI_NOSE_GUNS)).getAsList().get(0);
        if (stats == null) {
            throw new RuntimeException("Null stats");
        }
        if (stats.getResponse().isEmpty()) {
            channel.sendMessage("Couldn't find any nosegun kills for " + player.getName()).submit();
            return true;
        }
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(player.getName() + "'s nosegun kills")
                .setDescription("(Kills made with A2G guns do not count)");

        stats.getResponseByName("weapon_kills").forEach(s -> {
            builder.addField(PS2BotConfigurator.API.getItems().getCached(s.getItemId()).getName(), "Kills: " + s.getEnemyKills(player.getFaction()), false);
        });

        builder.addField("Total", "Kills: " + stats.getResponseByName("weapon_kills").mapToInt(IFactionWeaponStat::getTotal).sum(), false);
        channel.sendMessage(builder.build()).submit();
        return true;
    }

}
