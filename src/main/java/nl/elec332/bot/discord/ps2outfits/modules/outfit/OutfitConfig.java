package nl.elec332.bot.discord.ps2outfits.modules.outfit;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import nl.elec332.bot.discord.ps2outfits.CommandHelper;
import nl.elec332.bot.discord.ps2outfits.PS2BotConfigurator;
import nl.elec332.discord.bot.core.api.util.JDAConsumer;
import nl.elec332.planetside2.ps2api.api.objects.player.IOutfit;
import nl.elec332.planetside2.ps2api.api.objects.player.IOutfitMember;
import nl.elec332.planetside2.ps2api.api.objects.player.IPlayer;
import nl.elec332.planetside2.ps2api.api.objects.registry.IPS2ObjectReference;
import nl.elec332.planetside2.ps2api.api.objects.world.IServer;
import nl.elec332.planetside2.ps2api.api.streaming.IStreamingService;
import nl.elec332.planetside2.ps2api.api.streaming.request.IEventServiceFactory;
import nl.elec332.planetside2.ps2api.util.PS2Class;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
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
        this.classEmotes = new EnumMap<>(PS2Class.class);
        this.miscEmotes = new EnumMap<>(MiscEmotes.class);
        this.messageRoleMap = new HashMap<>();
        this.confirms = new HashMap<>();
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
    private final transient Map<PS2Class, Emote> classEmotes;
    private final transient Map<MiscEmotes, Emote> miscEmotes;
    private final transient Map<Long, Consumer<Member>> confirms;

    private IPS2ObjectReference<? extends IOutfit> outfit;
    private final Set<Long> facilityEventChannels;
    private final Map<Long, Long> memberToPlayerMap;
    private final Map<Long, Set<Long>> memberToPlayerAltMap;
    private final Map<String, Long> messageRoleMap;

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

    public Map<PS2Class, Emote> getClassEmotes() {
        return this.classEmotes;
    }

    public Map<MiscEmotes, Emote> getMiscEmotes() {
        return this.miscEmotes;
    }

    public void addConfirm(long id, Consumer<Member> thing) {
        this.confirms.put(id, thing);
    }

    public boolean hasMappedAccount(Member member) {
        long id = member.getIdLong();
        return memberToPlayerMap.containsKey(id);// || memberToPlayerAltMap.containsKey(id);
    }

    public void removeMappings(Member member, Runnable save) {
        long id = member.getIdLong();
        memberToPlayerMap.remove(id);
        memberToPlayerAltMap.remove(id);
        save.run();
    }

    public void addPlayerMapping(Member member, String playerName, MessageChannel channel, Runnable save) {
        if (playerName.trim().isEmpty()) {
            channel.sendMessage("Removed mapping for " + member.getEffectiveName()).submit();
            memberToPlayerMap.remove(member.getIdLong());
            save.run();
            return;
        }
        IPlayer player = PS2BotConfigurator.API.getPlayerManager().getByName(playerName);
        if (player == null || !player.getName().equalsIgnoreCase(playerName)) {
            channel.sendMessage("No player found matching name \"" + playerName + "\"").submit();
            return;
        }
        addPlayerMapping(member, player, channel, save);
    }

    public void addPlayerMapping(Member member, IPlayer player, MessageChannel channel, Runnable save) {
        if (player == null) {
            return;
        }
        channel.sendMessage("Successfully mapped " + member.getEffectiveName() + " to PS2 player " + player.getName()).submit();
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
        if (player == null || !player.getName().equalsIgnoreCase(playerName)) {
            channel.sendMessage("No player found matching name \"" + playerName + "\"").submit();
            return;
        } else {
            channel.sendMessage("Successfully mapped " + member.getEffectiveName() + " to PS2 alt account " + player.getName()).submit();
        }
        memberToPlayerAltMap.computeIfAbsent(member.getIdLong(), l -> new HashSet<>()).add(player.getId());
        save.run();
    }

    private static final String[] SPLITS = {"/", "-"};

    public IPlayer getDirectMapping(Member member) {
        if (memberToPlayerMap.containsKey(member.getIdLong())) {
            return PS2BotConfigurator.API.getPlayerManager().get(memberToPlayerMap.get(member.getIdLong()));
        }
        return null;
    }

    public Collection<IPlayer> getAltMapping(Member member) {
        Set<Long> alts = memberToPlayerAltMap.get(member.getIdLong());
        if (alts != null && !alts.isEmpty()) {
            return alts.stream()
                    .map(PS2BotConfigurator.API.getPlayerManager()::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return null;
    }

    public IPlayer getPlayer(Member member) {
        IPlayer ret = getDirectMapping(member);
        if (ret != null) {
            return ret;
        }
        String name = CommandHelper.trimPlayerName(member.getEffectiveName()).toLowerCase(Locale.ROOT);
        Collection<String> parts = Collections.singleton(name);
        for (String spl : SPLITS) {
            parts = parts.stream()
                    .map(s -> s.split(spl))
                    .flatMap(Arrays::stream)
                    .collect(Collectors.toList());
        }
        for (String s : parts) {
            s = s.trim();
            if (s.isEmpty()) {
                continue;
            }
            IPlayer player = PS2BotConfigurator.API.getPlayerManager().getByName(s);
            if (player != null) {
                String pn = player.getName().toLowerCase(Locale.ROOT);
                if (checkName(s, pn)) {
                    return player;
                }
            }
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
        return members.stream().filter(m -> !memberToPlayerMap.containsKey(m.getIdLong())).filter(m -> checkName(m.getEffectiveName().toLowerCase(Locale.ROOT), pn)).findFirst().orElse(null);
    }

    @Override
    public void accept(long value) {
        this.serverId = value;
    }

    public Stream<Member> getListeners(String type, Guild server) {
        if (!messageRoleMap.containsKey(type)) {
            return Stream.empty();
        }
        Role role = server.getRoleById(messageRoleMap.get(type));
        if (role == null) {
            return Stream.empty();
        }
        try {
            return server.findMembersWithRoles(role).get().stream();
        } catch (Exception e) {
            return Stream.empty();
        }
    }

    @Override
    public void onJDAConnected(JDA jda) {
        long id = jda.getSelfUser().getIdLong();
        TimeZone timeZone = TimeZone.getTimeZone("Europe/Amsterdam");
        this.streamingService = PS2BotConfigurator.API_ACCESSOR.createStreamingService();
        setupStreaming();
        CommandHelper.addIcons(getGuild(jda), getOutfit(), getClassEmotes(), getMiscEmotes());
        StreamingEventHandler.setup(this.streamingService, () -> streamChannels(jda), facilityEventChannels::isEmpty, this::getOutfit, () -> timeZone);
        jda.addEventListener(new ListenerAdapter() {

            @Override
            public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
                if (event.getGuild().getIdLong() != serverId) {
                    return;
                }
                if (event.getUserIdLong() == id) {
                    return;
                }
                MessageReaction.ReactionEmote re = event.getReactionEmote();
                if (re.isEmoji()) {
                    if (re.getName().equals(MiscEmotes.RED_CROSS_EMOJI)) {
                        confirms.remove(event.getMessageIdLong());
                    } else if (re.getName().equals(MiscEmotes.CHECK_MARK_EMOJI)) {
                        Consumer<Member> c = confirms.remove(event.getMessageIdLong());
                        if (c != null) {
                            event.retrieveMember().queue(c);
                        }
                    }
                }
            }

        });
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
