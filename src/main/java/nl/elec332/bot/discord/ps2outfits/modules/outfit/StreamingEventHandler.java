package nl.elec332.bot.discord.ps2outfits.modules.outfit;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import nl.elec332.bot.discord.ps2outfits.PS2BotConfigurator;
import nl.elec332.planetside2.ps2api.api.objects.player.IOutfit;
import nl.elec332.planetside2.ps2api.api.objects.player.IOutfitMember;
import nl.elec332.planetside2.ps2api.api.objects.player.ISlimPlayer;
import nl.elec332.planetside2.ps2api.api.streaming.IStreamingService;
import nl.elec332.planetside2.ps2api.api.streaming.event.IPlayerFacilityEvent;
import nl.elec332.planetside2.ps2api.api.streaming.event.base.IPlayerStreamingEvent;
import nl.elec332.planetside2.ps2api.api.streaming.request.IEventServiceFactory;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Elec332 on 21/09/2021
 */
public class StreamingEventHandler {

    private static transient final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static transient final NumberFormat NUMBER_FORMAT = new DecimalFormat("##0.##");

    public static void setup(IStreamingService streamingService, Supplier<Stream<TextChannel>> streamChannels, BooleanSupplier hasNoChannelListeners, Supplier<IOutfit> outfitGetter, Supplier<TimeZone> tzSupplier) {
        IEventServiceFactory eventServiceFactory = PS2BotConfigurator.API_ACCESSOR.getEventServiceFactory();
        streamingService.addListener(eventServiceFactory.getFacilityControlType(), event -> {
            IOutfit outfit = event.getOutfit();
            if (event.getFacility() == null || outfit == null) {
                return;
            }
            if (event.getOutfitId() != outfitGetter.get().getId()) {
                return;
            }
            if (hasNoChannelListeners.getAsBoolean() || event.getOldFaction().getId() == event.getNewFaction().getId()) {
                return;
            }
            TimeZone timeZone = tzSupplier.get();
            Collection<IPlayerFacilityEvent> events = PS2BotConfigurator.API_ACCESSOR.getStreamEventPoller().getPlayerEvents(event);
            Collection<ISlimPlayer> players = events.isEmpty() ? Collections.emptyList() : PS2BotConfigurator.API.getPlayerRequestHandler().getSlimPlayers(events.stream().map(IPlayerStreamingEvent::getPlayerId).collect(Collectors.toList()));
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
            streamChannels.get().forEach(channel -> channel.sendMessageEmbeds(builder.build()).submit());
        });
        streamingService.addListener(eventServiceFactory.getMetaGameEventType(), event -> {
            if (event.getEvent() == null) {
                System.out.println("Null event: " + event);
                return;
            }
            EmbedBuilder builder = new EmbedBuilder()
                    .setTitle(event.getEvent().getName() + " event " + event.getEventState().getName() + " on " + (event.getContinent() != null ? event.getContinent().getName() : "Koltyr"))
                    .addField("NC territory", NUMBER_FORMAT.format(event.getNCTerritory()) + "%", true)
                    .addField("TR territory", NUMBER_FORMAT.format(event.getTRTerritory()) + "%", true)
                    .addField("VS territory", NUMBER_FORMAT.format(event.getVSTerritory()) + "%", true);
            streamChannels.get().forEach(channel -> channel.sendMessageEmbeds(builder.build()).submit());
        });
        streamingService.addListener(eventServiceFactory.getContinentLockType(), event -> {
            EmbedBuilder builder = new EmbedBuilder()
                    .setTitle(event.getContinent().getName() + " was locked by " + event.getTriggeringFaction().getName())
                    .addField("NC population", event.getNCPopulation() + "%", true)
                    .addField("TR population", event.getTRPopulation() + "%", true)
                    .addField("VS population", event.getVSPopulation() + "%", true);
            streamChannels.get().forEach(channel -> channel.sendMessageEmbeds(builder.build()).submit());
        });
        streamingService.addListener(eventServiceFactory.getContinentUnlockType(), event -> {
            EmbedBuilder builder = new EmbedBuilder()
                    .setTitle(event.getContinent().getName() + " was unlocked by " + event.getTriggeringFaction().getName())
                    .addField("NC population", event.getNCPopulation() + "%", true)
                    .addField("TR population", event.getTRPopulation() + "%", true)
                    .addField("VS population", event.getVSPopulation() + "%", true);
            streamChannels.get().forEach(channel -> channel.sendMessageEmbeds(builder.build()).submit());
        });
        streamingService.setExceptionHandler(t -> streamChannels.get().forEach(channel -> channel.sendMessage("Received garbage from streaming API, causing " + t.getMessage()).submit()));
    }

}
