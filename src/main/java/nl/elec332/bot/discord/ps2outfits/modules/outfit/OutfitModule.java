package nl.elec332.bot.discord.ps2outfits.modules.outfit;

import com.google.gson.Gson;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import nl.elec332.bot.discord.ps2outfits.PS2BotConfigurator;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.commands.*;
import nl.elec332.discord.bot.core.api.ICommand;
import nl.elec332.discord.bot.core.util.AbstractGSONModule;
import nl.elec332.discord.bot.core.util.BotHelper;
import nl.elec332.planetside2.ps2api.util.NetworkUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

/**
 * Created by Elec332 on 22/05/2021
 */
public class OutfitModule extends AbstractGSONModule<OutfitConfig> {

    public OutfitModule() {
        super("outfit");
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void initialize() {
        File sm = BotHelper.getFile("servermapping.pbm");
        if (sm.exists()) {
            try {
                GZIPInputStream gis = new GZIPInputStream(new FileInputStream(sm));
                Map<String, String> m = (Map<String, String>) new ObjectInputStream(gis).readObject();
                gis.close();
                m.forEach((s, o) -> getInstanceFor(Long.parseUnsignedLong(s)).setOutfit(PS2BotConfigurator.API.getOutfitManager().get(Long.parseLong(o))));
                saveSettingsFile();
                if (sm.delete()) {
                    System.out.println("Successfully ported legacy settings!");
                } else {
                    throw new IOException();
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to load legacy settings.", e);
            }
        }
    }

    @Override
    public boolean canRunCommand(MessageChannel channel, Member member, OutfitConfig config, ICommand<OutfitConfig> command) {
        if (config.getOutfit() != null || command instanceof SetOutfitCommand) {
            return true;
        }
        channel.sendMessage("You need to set an outfit for this server before you can use this command!").submit();
        return false;
    }

    @Override
    protected Gson createGson() {
        return NetworkUtil.GSON;
    }

    @Override
    protected Class<OutfitConfig> getConfigType() {
        return OutfitConfig.class;
    }

    @Override
    public void registerCommands(Consumer<ICommand<OutfitConfig>> registry) {
        registry.accept(new SetOutfitCommand(this::saveSettingsFile));
        registry.accept(new DailyKdCommand());
        registry.accept(new WeeklyKdCommand());
        registry.accept(new MonthlyKdCommand());
        registry.accept(new DailyStatsCommand());
        registry.accept(new ExportMembersCommand());
        registry.accept(new OnlineMembersCommand());
        registry.accept(new TrackAlertsCommand(this::saveSettingsFile));
    }

}