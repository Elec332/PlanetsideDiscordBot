package nl.elec332.bot.discord.ps2outfits.modules.outfit;

import com.google.gson.Gson;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import nl.elec332.bot.discord.ps2outfits.PS2BotConfigurator;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.commands.*;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.messages.OpsSquadMessage;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.messages.SimpleSignupMessage;
import nl.elec332.discord.bot.core.api.ICommand;
import nl.elec332.discord.bot.core.api.util.ISpecialMessage;
import nl.elec332.discord.bot.core.util.AbstractGSONModule;
import nl.elec332.discord.bot.core.util.BotHelper;
import nl.elec332.discord.bot.core.util.SpecialMessageHandler;
import nl.elec332.planetside2.ps2api.util.NetworkUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.LongFunction;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

/**
 * Created by Elec332 on 22/05/2021
 */
public class OutfitModule extends AbstractGSONModule<OutfitConfig> {

    public OutfitModule() {
        super("outfit");
    }

    private static final BiPredicate<Member, OutfitConfig> ADMIN_CHECK = (m, c) -> m.hasPermission(Permission.ADMINISTRATOR);

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
    protected Class<OutfitConfig> getInstanceType() {
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
        registry.accept(new PlayerMappingCommand(this::saveSettingsFile));
        registry.accept(new PlayerAltMappingCommand(this::saveSettingsFile));
        registry.accept(new NoseGunKillsCommand());
        registry.accept(new ResetIconsCommand());
        registry.accept(SpecialMessageHandler.postSpecialMessageCommand("OpsSignups", "ops_squad", ADMIN_CHECK, "Starts ops signups."));
        registry.accept(SpecialMessageHandler.repostSpecialMessageCommand("RepostMessage", ADMIN_CHECK));
        registry.accept(SpecialMessageHandler.fakeReactionCommand("FakeReact"));
        registry.accept(SpecialMessageHandler.postSpecialMessageCommand("SimpleSignups", "simple_signup", ADMIN_CHECK, "Starts a simple signup given a title and description between \"'s.", "title", "description"));
        registry.accept(SpecialMessageHandler.postSpecialMessageCommand("SimpleSignups", "simple_signup", ADMIN_CHECK, "Starts a simple signup given a title and description between \"'s.", "title", "description"));
        registry.accept(SpecialMessageHandler.deleteSpecialMessageCommand("DisableMessage", ADMIN_CHECK));
        registry.accept(new CheckCharactersCommand(this::saveSettingsFile));
        registry.accept(new ListCharactersCommand());
        registry.accept(new RemovePlayerMappingCommand(this::saveSettingsFile));
        registry.accept(new ShowMemberRolesCommand());
        registry.accept(new SetMemberRolesMapCommand(this::saveSettingsFile));
        registry.accept(new CheckRolesCommand());
    }

    @Override
    public void registerSpecialMessages(BiConsumer<String, LongFunction<ISpecialMessage>> registry) {
        registry.accept("ops_squad", l -> {
            OutfitConfig c = getInstanceFor(l);
            return new OpsSquadMessage(c.getClassEmotes(), c.getMiscEmotes());
        });
        registry.accept("simple_signup", l -> new SimpleSignupMessage(getInstanceFor(l).getMiscEmotes()));
    }

    @Override
    public Stream<Member> getMessageListeners(String type, Guild server) {
        return getInstanceFor(server.getIdLong()).getListeners(type, server);
    }

}
