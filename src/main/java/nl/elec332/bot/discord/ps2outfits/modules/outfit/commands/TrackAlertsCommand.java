package nl.elec332.bot.discord.ps2outfits.modules.outfit.commands;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.OutfitConfig;
import nl.elec332.discord.bot.core.api.util.SimpleCommand;

import java.util.Set;

/**
 * Created by Elec332 on 04/06/2021
 */
public class TrackAlertsCommand extends SimpleCommand<OutfitConfig> {

    public TrackAlertsCommand(Runnable save) {
        super("TrackAlerts", "Starts/Stops tracking alerts for your outfit");
        this.save = save;
    }

    private final Runnable save;

    @Override
    public boolean executeCommand(MessageChannel channel, Message message, Member member, OutfitConfig config, String... args) {
        Set<Long> c = config.getFacilityEventChannels();
        if (c.contains(channel.getIdLong())) {
            c.remove(channel.getIdLong());
            channel.sendMessage("Stopped tracking facility captures in this channel").submit();
        } else {
            c.add(channel.getIdLong());
            channel.sendMessage("Now tracking facility captures in this channel").submit();
        }
        save.run();
        return true;
    }

}
