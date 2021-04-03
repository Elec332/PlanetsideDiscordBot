package nl.elec332.bot.discord.ps2outfits;

import net.dv8tion.jda.api.entities.TextChannel;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Created by Elec332 on 03/04/2021
 */
public enum Commands {

    DAILYKD {
        @Override
        public void executeCommand(TextChannel channel, String outfitID) {
            CommandHelper.postPlayerKDData(channel, outfitID, "today", Instant.now().minus(1, ChronoUnit.DAYS), CommandHelper::getDailyKDInfo);
        }

    },
    WEEKLYKD {
        @Override
        public void executeCommand(TextChannel channel, String outfitID) {
            CommandHelper.postPlayerKDData(channel, outfitID, "this week", Instant.now().minus(7, ChronoUnit.DAYS), CommandHelper::getWeeklyKDInfo);
        }

    },
    MONTHLYKD {
        @Override
        public void executeCommand(TextChannel channel, String outfitID) {
            CommandHelper.postPlayerKDData(channel, outfitID, "this month", Instant.now().minus(30, ChronoUnit.DAYS), CommandHelper::getMonthlyKDInfo);
        }

    };

    public abstract void executeCommand(TextChannel channel, String outfit);

}
