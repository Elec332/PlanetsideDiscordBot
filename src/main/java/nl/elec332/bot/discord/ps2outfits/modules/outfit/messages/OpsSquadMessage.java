package nl.elec332.bot.discord.ps2outfits.modules.outfit.messages;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import nl.elec332.bot.discord.ps2outfits.CommandHelper;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.MiscEmotes;
import nl.elec332.discord.bot.core.api.util.ISpecialMessage;
import nl.elec332.planetside2.ps2api.util.PS2Class;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by Elec332 on 12/09/2021
 */
public class OpsSquadMessage implements ISpecialMessage {

    public OpsSquadMessage(Map<PS2Class, Emote> classEmotes, Map<MiscEmotes, Emote> miscEmotes) {
        this.classEmotes = classEmotes;
        this.miscEmotes = miscEmotes;
    }

    private static final EnumSet<PS2Class> ALLOWED = EnumSet.of(PS2Class.HEAVY_ASSAULT, PS2Class.COMBAT_MEDIC, PS2Class.ENGINEER, PS2Class.INFILTRATOR, PS2Class.MAX);

    private final Map<PS2Class, Emote> classEmotes;
    private final Map<MiscEmotes, Emote> miscEmotes;
    private Map<Long, List<PS2Class>> signups = new LinkedHashMap<>();
    private Set<Long> reserve = new LinkedHashSet<>();
    private Set<Long> max = new LinkedHashSet<>();
    private Set<Long> router = new LinkedHashSet<>();
    private String date = "";


    private PS2Class getFromEmote(Emote emote) {
        return classEmotes.entrySet().stream()
                .filter(e -> e.getValue().getIdLong() == emote.getIdLong())
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    @Override
    public void init(String args) {
        this.date = args;
    }

    @Override
    public void onReactionAdded(MessageReaction reaction, Member member, Runnable runnable) {
        System.out.println(member);
        System.out.println(reaction.getReactionEmote());
        if (member == null || member.getUser().isBot() || !reaction.getReactionEmote().isEmote()) {
            return;
        }
        synchronized (this) {
            long id = member.getIdLong();
            Emote emote = reaction.getReactionEmote().getEmote();
            PS2Class clazz = getFromEmote(emote);
            if (clazz == null) {
                if (emote.equals(miscEmotes.get(MiscEmotes.FACTION))) {
                    if (reserve.contains(id)) {
                        reserve.remove(id);
                    } else if (signups.getOrDefault(id, Collections.emptyList()).isEmpty()) {
                        reserve.add(id);
                    }
                } else if (emote.equals(miscEmotes.get(MiscEmotes.ROUTER))) {
                    if (router.contains(id)) {
                        router.remove(id);
                    } else if (!signups.getOrDefault(id, Collections.emptyList()).isEmpty()) {
                        router.add(id);
                    }
                }
            } else {
                List<PS2Class> classes = this.signups.computeIfAbsent(id, l -> new ArrayList<>());
                if (classes.contains(clazz)) {
                    classes.remove(clazz);
                } else {
                    classes.add(clazz);
                }
                if (classes.isEmpty()) {
                    router.remove(id);
                } else {
                    reserve.remove(id);
                }
            }
            runnable.run();
        }
        reaction.removeReaction(member.getUser()).queue();
    }

    @Override
    public void onReactionRemoved(GenericMessageReactionEvent event, Runnable runnable) {
        System.out.println(event.retrieveMember().complete());
    }

    @Override
    public void onMessageQuoted(MessageReceivedEvent event, Runnable runnable) {
        System.out.println(event.getMember());
    }

    private EmbedBuilder createSignupBody(Guild guild) {
        Map<String, String> data = signups.entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>(guild.getJDA().retrieveUserById(e.getKey()).flatMap(guild::retrieveMember).submit().join(), e.getValue()))
                .filter(e -> e.getKey() != null && !e.getValue().isEmpty())
                .collect(Collectors.toMap(e -> e.getKey().getEffectiveName(), e -> e.getValue().stream().map(PS2Class::toString).collect(Collectors.joining(", "))));
        EmbedBuilder ret = new EmbedBuilder()
                .setTitle("Ops Signups " + date)
                .setDescription("React with a class to signup, react again with the same class to remove signup.\n" +
                        "Please sign up in order of class preference (e.g. Heavy > Medic > Engineer), we'll make a roster based on class preferences.\n" +
                        "To signup as a backup (if you're e.g. gonna be late): " + miscEmotes.get(MiscEmotes.FACTION).getAsMention() +
                        "\nTo signup with a router, react with " + miscEmotes.get(MiscEmotes.ROUTER).getAsMention() +
                        "\n\n React again to remove your signup.")
                .addField("Signups:", "", false)
                .addField("Name:", String.join("\n", data.keySet()), true)
                .addField("Classes:", String.join("\n", data.values()), true);
        if (!router.isEmpty()) {
            ret.addField("Router: ", router.stream().map(id -> CommandHelper.getGuildMember(id, guild)).filter(Objects::nonNull).map(Member::getEffectiveName).collect(Collectors.joining(", ")), false);
        }
        if (!reserve.isEmpty()) {
            ret.addField("Reserves:", reserve.stream().map(id -> CommandHelper.getGuildMember(id, guild)).filter(Objects::nonNull).map(Member::getEffectiveName).collect(Collectors.joining(", ")), false);
        }
        return ret;
    }

    @Override
    public void onMessagePosted(Message message, long instanceId, boolean sameGuild, Supplier<String> footer) {
        updateMessage(message, instanceId, sameGuild, footer);
        if (!sameGuild) {
            return;
        }
        for (PS2Class clazz : ALLOWED) {
            message.getChannel().addReactionById(message.getIdLong(), classEmotes.get(clazz)).submit();
        }
        message.getChannel().addReactionById(message.getIdLong(), miscEmotes.get(MiscEmotes.ROUTER)).submit();
        message.getChannel().addReactionById(message.getIdLong(), miscEmotes.get(MiscEmotes.FACTION)).submit();
    }

    @Override
    public void updateMessage(Message message, long id, boolean sameGuild, Supplier<String> footer) {
        message.editMessageEmbeds(createSignupBody(message.getGuild())
                .setFooter(footer.get())
                .build())
                .submit();
    }

    @Override
    public String getListenerMessage() {
        return "nein";
    }

    @Override
    public int serialize(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.writeObject(this.signups);
        objectOutputStream.writeObject(this.reserve);
        objectOutputStream.writeObject(this.max);
        objectOutputStream.writeObject(this.router);
        objectOutputStream.writeUTF(this.date);
        return 2;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void deserialize(ObjectInputStream objectInputStream, int version) throws IOException {
        try {
            this.signups = (Map<Long, List<PS2Class>>) objectInputStream.readObject();
            this.reserve = (Set<Long>) objectInputStream.readObject();
            this.max = (Set<Long>) objectInputStream.readObject();
            this.router = (Set<Long>) objectInputStream.readObject();
            if (version == 2) {
                this.date = objectInputStream.readUTF();
            }
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    @Override
    public String toString() {
        return "OpsSquadMessage{" +
                "signups=" + signups +
                ", reserve=" + reserve +
                ", max=" + max +
                ", router=" + router +
                '}';
    }
}
