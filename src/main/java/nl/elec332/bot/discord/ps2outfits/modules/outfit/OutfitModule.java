package nl.elec332.bot.discord.ps2outfits.modules.outfit;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import nl.elec332.bot.discord.ps2outfits.api.ICommand;
import nl.elec332.bot.discord.ps2outfits.api.util.AbstractSimpleConfigurableBotModule;
import nl.elec332.bot.discord.ps2outfits.core.Main;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.commands.*;

import java.io.File;
import java.util.function.Consumer;

/**
 * Created by Elec332 on 22/05/2021
 */
public class OutfitModule extends AbstractSimpleConfigurableBotModule<OutfitConfig> {

    public OutfitModule() {
        super("outfit");
    }

    @Override
    public boolean canRunCommand(TextChannel channel, Member member, OutfitConfig config, ICommand<OutfitConfig> command) {
        if (config.getOutfit() != null || command instanceof SetOutfitCommand) {
            return true;
        }
        channel.sendMessage("You need to set an outfit for this server before you can use this command!").submit();
        return false;
    }

    @Override
    protected OutfitConfig createConfig(long sid) {
        return new OutfitConfig();
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
    }

}
