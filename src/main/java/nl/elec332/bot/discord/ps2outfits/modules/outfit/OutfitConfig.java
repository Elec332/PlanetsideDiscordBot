package nl.elec332.bot.discord.ps2outfits.modules.outfit;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import nl.elec332.bot.discord.ps2outfits.PS2BotConfigurator;
import nl.elec332.discord.bot.core.api.util.JDAConsumer;
import nl.elec332.planetside2.ps2api.api.objects.player.IOutfit;
import nl.elec332.planetside2.ps2api.api.objects.player.IOutfitMember;
import nl.elec332.planetside2.ps2api.api.objects.player.ISlimPlayer;
import nl.elec332.planetside2.ps2api.api.objects.registry.IPS2ObjectReference;
import nl.elec332.planetside2.ps2api.api.objects.world.IServer;
import nl.elec332.planetside2.ps2api.api.streaming.IStreamingService;
import nl.elec332.planetside2.ps2api.api.streaming.event.IPlayerFacilityEvent;
import nl.elec332.planetside2.ps2api.api.streaming.event.base.IPlayerStreamingEvent;
import nl.elec332.planetside2.ps2api.api.streaming.request.IEventServiceFactory;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.LongConsumer;
import java.util.stream.Collectors;

/**
 * Created by Elec332 on 22/05/2021
 */
public class OutfitConfig implements LongConsumer, Serializable, JDAConsumer {

    public OutfitConfig() {
        this.outfit = PS2BotConfigurator.API.getOutfitManager().getReference(0);
        this.facilityEventChannels = new HashSet<>();
    }

    private static transient final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static transient final NumberFormat NUMBER_FORMAT = new DecimalFormat("##0.##");

    private transient long serverId;
    private transient IStreamingService streamingService;

    private IPS2ObjectReference<? extends IOutfit> outfit;
    private final Set<Long> facilityEventChannels;

    public void setOutfit(IOutfit outfit) {
        this.outfit = PS2BotConfigurator.API.getOutfitManager().getReference(Objects.requireNonNull(outfit).getId());
        setupStreaming();
    }

    public IOutfit getOutfit() {
        return this.outfit.getObject();
    }

    public Guild getGuild(JDA jda) {
        return jda.getGuildById(serverId);
    }

    public Set<Long> getFacilityEventChannels() {
        return facilityEventChannels;
    }

    @Override
    public void accept(long value) {
        this.serverId = value;
    }

    @Override
    public void onJDAConnected(JDA jda) {
        this.streamingService = PS2BotConfigurator.API_ACCESSOR.createStreamingService();
        setupStreaming();
        IEventServiceFactory eventServiceFactory = PS2BotConfigurator.API_ACCESSOR.getEventServiceFactory();
        this.streamingService.addListener(eventServiceFactory.getFacilityControlType(), event -> {
            IOutfit outfit = event.getOutfit();
            if (event.getFacility() == null || outfit == null) {
                return;
            }
            if (event.getOutfitId() != this.outfit.getId()) {
                return;
            }
            if (facilityEventChannels.isEmpty() || event.getOldFaction().getId() == event.getNewFaction().getId()) {
                return;
            }
            Collection<IPlayerFacilityEvent> events = PS2BotConfigurator.API_ACCESSOR.getStreamEventPoller().getPlayerEvents(event);
            Collection<ISlimPlayer> players = events.isEmpty() ? Collections.emptyList() : PS2BotConfigurator.API.getPlayerRequestHandler().getSlimPlayers(events.stream().map(IPlayerStreamingEvent::getPlayerId).collect(Collectors.toList()));

            TimeZone timeZone = TimeZone.getTimeZone("Europe/Amsterdam");
            List<String> op = outfit.getOnlineMembers()
                    .filter(p -> events.stream().anyMatch(e -> p.getPlayerId() == e.getPlayerId()))
                    .map(IOutfitMember::getPlayerName)
                    .collect(Collectors.toList());
            EmbedBuilder builder = new EmbedBuilder()
                    .setTitle(outfit.getName() + " captured " + event.getFacility().getName())
                    .addField("Continent", event.getContinent().getName(), true)
                    .addField("Attackers", "" + players.size(), true)
                    .addField("Time", DATE_FORMAT.format(event.getTimeStamp().atZone(timeZone.toZoneId())) + " " + timeZone.getDisplayName(timeZone.inDaylightTime(new Date()), TimeZone.SHORT), true)
                    .addField("Outfit members present (" + op.size() + "/" + players.size() + " attackers):", String.join(", ", op), false);
            facilityEventChannels.stream()
                    .map(jda::getTextChannelById)
                    .filter(Objects::nonNull)
                    .forEach(channel -> channel.sendMessage(builder.build()).submit());
        });
        this.streamingService.addListener(eventServiceFactory.getMetaGameEventType(), event -> {
            EmbedBuilder builder = new EmbedBuilder()
                    .setTitle(event.getEvent().getName() + " event " + event.getEventState().getName() + " on " + event.getContinent().getName())
                    .addField("NC territory", NUMBER_FORMAT.format(event.getNCTerritory()) + "%", true)
                    .addField("TR territory", NUMBER_FORMAT.format(event.getTRTerritory()) + "%", true)
                    .addField("VS territory", NUMBER_FORMAT.format(event.getVSTerritory()) + "%", true);
            facilityEventChannels.stream()
                    .map(jda::getTextChannelById)
                    .filter(Objects::nonNull)
                    .forEach(channel -> channel.sendMessage(builder.build()).submit());
        });
        this.streamingService.addListener(eventServiceFactory.getContinentLockType(), event -> {
            EmbedBuilder builder = new EmbedBuilder()
                    .setTitle(event.getContinent().getName() + " was locked by " + event.getTriggeringFaction().getName())
                    .addField("NC population", event.getNCPopulation() + "%", true)
                    .addField("TR population", event.getTRPopulation() + "%", true)
                    .addField("VS population", event.getVSPopulation() + "%", true);
            facilityEventChannels.stream()
                    .map(jda::getTextChannelById)
                    .filter(Objects::nonNull)
                    .forEach(channel -> channel.sendMessage(builder.build()).submit());
        });
        this.streamingService.addListener(eventServiceFactory.getContinentUnlockType(), event -> {
            EmbedBuilder builder = new EmbedBuilder()
                    .setTitle(event.getContinent().getName() + " was unlocked by " + event.getTriggeringFaction().getName())
                    .addField("NC population", event.getNCPopulation() + "%", true)
                    .addField("TR population", event.getTRPopulation() + "%", true)
                    .addField("VS population", event.getVSPopulation() + "%", true);
            facilityEventChannels.stream()
                    .map(jda::getTextChannelById)
                    .filter(Objects::nonNull)
                    .forEach(channel -> channel.sendMessage(builder.build()).submit());
        });
    }

    private void setupStreaming() {
        this.streamingService.clearSubscriptions();
        IOutfit outfit = this.getOutfit();
        if (outfit == null) {
            return;
        }
        IEventServiceFactory eventServiceFactory = PS2BotConfigurator.API_ACCESSOR.getEventServiceFactory();
        IServer server = outfit.getServer().getObject();
        this.streamingService.subscribeToEvent(eventServiceFactory.getFacilityControlType(), server);
        this.streamingService.subscribeToEvent(eventServiceFactory.getMetaGameEventType(), server);
        this.streamingService.subscribeToEvent(eventServiceFactory.getContinentLockType(), server);
        this.streamingService.subscribeToEvent(eventServiceFactory.getContinentUnlockType(), server);
    }

    @Override
    public void onJDADisconnected() {
        if (streamingService != null) {
            streamingService.stop();
        }
    }

}
