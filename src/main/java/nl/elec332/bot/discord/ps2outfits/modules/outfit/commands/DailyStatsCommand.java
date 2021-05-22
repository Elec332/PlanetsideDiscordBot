package nl.elec332.bot.discord.ps2outfits.modules.outfit.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import nl.elec332.bot.discord.ps2outfits.api.util.SimpleCommand;
import nl.elec332.bot.discord.ps2outfits.modules.CommandHelper;
import nl.elec332.bot.discord.ps2outfits.modules.PS2BotConfigurator;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.OutfitConfig;
import nl.elec332.planetside2.api.objects.player.IOutfit;
import nl.elec332.planetside2.api.objects.player.IPlayerResponseList;
import nl.elec332.planetside2.api.objects.player.request.ICharacterStat;
import nl.elec332.planetside2.api.objects.player.request.ICharacterStatHistory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Elec332 on 22/05/2021
 */
public class DailyStatsCommand extends SimpleCommand<OutfitConfig> {

    public DailyStatsCommand() {
        super("DailyStats", "Shows daily stats for your outfit.");
    }

    @Override
    public boolean executeCommand(TextChannel channel, Member member, OutfitConfig config, String... args) {
        IOutfit outfit = config.getOutfit();
        Collection<Long> onlineMembers = outfit.getPlayerIds(p -> p.getLastPlayerActivity().isAfter(Instant.now().minus(1, ChronoUnit.DAYS)));
        if (onlineMembers.isEmpty()) {
            channel.sendMessage("No members have been online!").submit();
            return true;
        }
        String title = outfit.getName() + " daily stats";
        String description = "Out of " + outfit.getMembers() + " members, " + onlineMembers.size() + " have been online today";

        IPlayerResponseList<ICharacterStatHistory> historyStats = PS2BotConfigurator.API.getPlayerRequestHandler().getSlimCharacterStatHistory(onlineMembers, "facility_capture", "deaths", "kills");
        IPlayerResponseList<ICharacterStat> stats = PS2BotConfigurator.API.getPlayerRequestHandler().getSlimCharacterStats(onlineMembers, "assist_count");

        Collection<Map.Entry<String, String>> kdInfo = CommandHelper.getKDInfo(historyStats, i -> i.getDay(1));
        if (kdInfo.size() != onlineMembers.size()) {
            description += "\n (" + kdInfo.size() + " members were active in that period)";
        }

        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(title)
                .setDescription(description);

        String kdStr = kdInfo.stream()
                .limit(10)
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("\n"));
        builder.addField("K/D Top-10", kdStr, false);

        String assistStr = stats
                .streamResponse(p -> p.getFirstResponse().getDaily(), 1, Comparator.reverseOrder())
                .map(e -> e.getKey() + ": " + e.getValue())
                .limit(10)
                .collect(Collectors.joining("\n"));
        builder.addField("Assists Top-10", assistStr, false);

        String capDef = historyStats.streamResponse(p -> p.getFirstResponseByName("facility_capture").getDay(1), 0, Comparator.reverseOrder())
                .map(e -> e.getKey() + ": " + e.getValue())
                .limit(10)
                .collect(Collectors.joining("\n"));
        builder.addField("Facilities captured Top-10:", capDef, false);

        builder.addField("Repair Ribbons Top-5", "//todo", false);

        builder.addField("Revive & Heal Ribbons Top-5", "//todo", false);

        channel.sendMessage(builder.build()).submit();
        return true;
    }

}
