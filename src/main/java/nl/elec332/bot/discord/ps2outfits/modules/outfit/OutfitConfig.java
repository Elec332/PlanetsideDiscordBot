package nl.elec332.bot.discord.ps2outfits.modules.outfit;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import nl.elec332.bot.discord.ps2outfits.CommandHelper;
import nl.elec332.bot.discord.ps2outfits.PS2BotConfigurator;
import nl.elec332.discord.bot.core.api.util.JDAConsumer;
import nl.elec332.planetside2.ps2api.api.objects.player.IOutfit;
import nl.elec332.planetside2.ps2api.api.objects.player.IOutfitMember;
import nl.elec332.planetside2.ps2api.api.objects.player.IPlayer;
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
import java.util.stream.Stream;

/**
 * Created by Elec332 on 22/05/2021
 */
public class OutfitConfig implements LongConsumer, Serializable, JDAConsumer {

    public OutfitConfig() {
        this.outfit = PS2BotConfigurator.API.getOutfitManager().getReference(0);
        this.facilityEventChannels = new HashSet<>();
        this.memberToPlayerMap = new HashMap<>();
        this.memberToPlayerAltMap = new HashMap<>();
    }

    private static transient final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static transient final NumberFormat NUMBER_FORMAT = new DecimalFormat("##0.##");

    private static boolean checkName(String member, String playerName) {
        if (member.equalsIgnoreCase(playerName)) {
            return true;
        }
        if (member.length() >= 6 && ((playerName.contains(member) || member.contains(playerName) || (playerName.contains(member.replace(" ", "")) || member.replace(" ", "").contains(playerName))))) {
            return true;
        }
        return false;
    }

    private transient long serverId;
    private transient IStreamingService streamingService;

    private IPS2ObjectReference<? extends IOutfit> outfit;
    private final Set<Long> facilityEventChannels;
    private final Map<Long, Long> memberToPlayerMap;
    private final Map<Long, Set<Long>> memberToPlayerAltMap;

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

    public void addPlayerMapping(Member member, String playerName, TextChannel channel, Runnable save) {
        if (playerName.trim().isEmpty()) {
            channel.sendMessage("Removed mapping for " + member.getEffectiveName()).submit();
            memberToPlayerMap.remove(member.getIdLong());
            save.run();
            return;
        }
        IPlayer player = PS2BotConfigurator.API.getPlayerManager().getByName(playerName);
        if (!player.getName().equalsIgnoreCase(playerName)) {
            channel.sendMessage("No player found matching name \"" + playerName + "\"").submit();
            return;
        } else {
            channel.sendMessage("Successfully mapped " + member.getEffectiveName() + " to PS2 player " + player.getName()).submit();
        }
        memberToPlayerMap.put(member.getIdLong(), player.getId());
        save.run();
    }

    public void addPlayerAltMapping(Member member, String playerName, TextChannel channel, Runnable save) {
        if (playerName.trim().isEmpty()) {
            channel.sendMessage("Removed alt mapping for " + member.getEffectiveName()).submit();
            memberToPlayerAltMap.remove(member.getIdLong());
            save.run();
            return;
        }
        IPlayer player = PS2BotConfigurator.API.getPlayerManager().getByName(playerName);
        if (!player.getName().equalsIgnoreCase(playerName)) {
            channel.sendMessage("No player found matching name \"" + playerName + "\"").submit();
            return;
        } else {
            channel.sendMessage("Successfully mapped " + member.getEffectiveName() + " to PS2 alt account " + player.getName()).submit();
        }
        memberToPlayerAltMap.computeIfAbsent(member.getIdLong(), l -> new HashSet<>()).add(player.getId());
        save.run();
    }

    public IPlayer getPlayer(Member member) {
        if (memberToPlayerMap.containsKey(member.getIdLong())) {
            return PS2BotConfigurator.API.getPlayerManager().get(memberToPlayerMap.get(member.getIdLong()));
        }
        String name = CommandHelper.trimPlayerName(member.getEffectiveName()).toLowerCase(Locale.ROOT);
        String[] parts = name.split("/");
        for (String s : parts) {
            s = s.trim();
            if (s.isEmpty()) {
                continue;
            }
            IPlayer player = PS2BotConfigurator.API.getPlayerManager().getByName(s);
            if (player == null) {
                return null;
            }
            String pn = player.getName().toLowerCase(Locale.ROOT);
            if (!checkName(s, pn)) {
                return null;
            }
            return player;
        }
        return null;
    }

    public Member getMemberFor(IOutfitMember player, Collection<Member> members) {
        if (members.isEmpty()) {
            return null;
        }
        if (memberToPlayerMap.containsValue(player.getPlayerId())) {
            return memberToPlayerMap.entrySet().stream()
                    .filter(e -> e.getValue() == player.getPlayerId())
                    .findFirst()
                    .map(e -> {
                        Member ret = members.stream()
                                .filter(m -> m.getIdLong() == e.getKey())
                                .findFirst()
                                .orElse(null);
                        if (ret == null) {
                            memberToPlayerMap.remove(e.getKey());
                        }
                        return ret;
                    })
                    .orElse(null);
        }
        Set<Long> l = memberToPlayerAltMap.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        if (l.contains(player.getPlayerId())) {
            return memberToPlayerAltMap.entrySet().stream()
                    .filter(e -> e.getValue().contains(player.getPlayerId()))
                    .findFirst()
                    .map(e -> {
                        Member ret = members.stream()
                                .filter(m -> m.getIdLong() == e.getKey())
                                .findFirst()
                                .orElse(null);
                        if (ret == null) {
                            memberToPlayerAltMap.remove(e.getKey());
                        }
                        return ret;
                    })
                    .orElse(null);
        }
        String pn = player.getPlayerName().toLowerCase(Locale.ROOT);
        return members.stream().filter(m -> !memberToPlayerMap.containsKey(m.getIdLong()) && !memberToPlayerAltMap.containsKey(m.getIdLong())).filter(m -> checkName(m.getEffectiveName().toLowerCase(Locale.ROOT), pn)).findFirst().orElse(null);
    }

    @Override
    public void accept(long value) {
        this.serverId = value;
    }

    @Override
    public void onJDAConnected(JDA jda) {
        TimeZone timeZone = TimeZone.getTimeZone("Europe/Amsterdam");
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
            streamChannels(jda)
                    .forEach(channel -> channel.sendMessageEmbeds(builder.build()).submit());
        });
        this.streamingService.addListener(eventServiceFactory.getMetaGameEventType(), event -> {
            System.out.println(event);
            EmbedBuilder builder = new EmbedBuilder()
                    .setTitle(event.getEvent().getName() + " event " + event.getEventState().getName() + " on " + event.getContinent().getName())
                    .addField("NC territory", NUMBER_FORMAT.format(event.getNCTerritory()) + "%", true)
                    .addField("TR territory", NUMBER_FORMAT.format(event.getTRTerritory()) + "%", true)
                    .addField("VS territory", NUMBER_FORMAT.format(event.getVSTerritory()) + "%", true);
            streamChannels(jda)
                    .forEach(channel -> channel.sendMessageEmbeds(builder.build()).submit());
        });
        this.streamingService.addListener(eventServiceFactory.getContinentLockType(), event -> {
            EmbedBuilder builder = new EmbedBuilder()
                    .setTitle(event.getContinent().getName() + " was locked by " + event.getTriggeringFaction().getName())
                    .addField("NC population", event.getNCPopulation() + "%", true)
                    .addField("TR population", event.getTRPopulation() + "%", true)
                    .addField("VS population", event.getVSPopulation() + "%", true);
            streamChannels(jda)
                    .forEach(channel -> channel.sendMessageEmbeds(builder.build()).submit());
        });
        this.streamingService.addListener(eventServiceFactory.getContinentUnlockType(), event -> {
            EmbedBuilder builder = new EmbedBuilder()
                    .setTitle(event.getContinent().getName() + " was unlocked by " + event.getTriggeringFaction().getName())
                    .addField("NC population", event.getNCPopulation() + "%", true)
                    .addField("TR population", event.getTRPopulation() + "%", true)
                    .addField("VS population", event.getVSPopulation() + "%", true);
            streamChannels(jda)
                    .forEach(channel -> channel.sendMessageEmbeds(builder.build()).submit());
        });
        this.streamingService.setExceptionHandler(t -> streamChannels(jda)
                .forEach(channel -> channel.sendMessage("Received garbage from streaming API, causing " + t.getMessage()).submit()));
    }

    private Stream<TextChannel> streamChannels(JDA jda) {
        return facilityEventChannels.stream()
                .map(jda::getTextChannelById)
                .filter(Objects::nonNull)
                .filter(t -> {
                    if (!getGuild(jda).getSelfMember().hasPermission(t, Permission.VIEW_CHANNEL)) {
                        System.out.println("No permission to view channel: " + t.getName() + " in " + getGuild(jda).getName());
                        return false;
                    }
                    return true;
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
