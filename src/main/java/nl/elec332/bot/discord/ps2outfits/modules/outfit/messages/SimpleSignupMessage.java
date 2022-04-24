package nl.elec332.bot.discord.ps2outfits.modules.outfit.messages;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import nl.elec332.bot.discord.ps2outfits.CommandHelper;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.MiscEmotes;
import nl.elec332.discord.bot.core.api.util.ISpecialMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by Elec332 on 19/09/2021
 */
public class SimpleSignupMessage implements ISpecialMessage {

    public SimpleSignupMessage(Map<MiscEmotes, Emote> miscEmotes) {
        this.miscEmotes = miscEmotes;
    }

    private final Map<MiscEmotes, Emote> miscEmotes;
    private List<Long> signups = new ArrayList<>();
    private String name = "Event";
    private String description = "";

    @Override
    public void init(String args) {
        String[] parts = args.split("\"");
        parts = Arrays.stream(parts)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        if (parts.length > 0) {
            this.name = parts[0];
        }
        if (parts.length > 1) {
            this.description = parts[1];
        }
        if (parts.length > 2) {
            System.out.println("Too many args: " + Arrays.toString(parts));
        }
    }

    @Override
    public void onReactionAdded(MessageReaction reaction, Member member, Runnable runnable) {
        if (member.getUser().isBot() || !reaction.getReactionEmote().isEmote()) {
            return;
        }
        synchronized (this) {
            long id = member.getIdLong();
            Emote emote = reaction.getReactionEmote().getEmote();
            if (emote.equals(miscEmotes.get(MiscEmotes.OUTFIT))) {
                if (signups.contains(id)) {
                    signups.remove(id);
                } else {
                    signups.add(id);
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
        Collection<Member> members = signups.stream().map(id -> CommandHelper.getGuildMember(id, guild)).filter(Objects::nonNull).collect(Collectors.toList());
        return new EmbedBuilder()
                .setTitle(this.name + " Signups")
                .setDescription(this.description + "\n\nReact with " + miscEmotes.get(MiscEmotes.OUTFIT).getAsMention() +
                        " to signup, react again to remove signup.\n")
                .addField("Signups (" + members.size() + "):", members.stream().map(IMentionable::getAsMention).collect(Collectors.joining("\n")), false);
    }

    @Override
    public void onMessagePosted(Message message, long instanceId, boolean sameGuild, Supplier<String> footer) {
        updateMessage(message, instanceId, sameGuild, footer);
        if (!sameGuild) {
            return;
        }
        message.getChannel().addReactionById(message.getIdLong(), miscEmotes.get(MiscEmotes.OUTFIT)).submit();
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
        objectOutputStream.writeUTF(this.name);
        objectOutputStream.writeObject(this.signups);
        objectOutputStream.writeUTF(this.description);
        return 2;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void deserialize(ObjectInputStream objectInputStream, int version) throws IOException {
        try {
            this.name = objectInputStream.readUTF();
            this.signups = (List<Long>) objectInputStream.readObject();
            if (version == 2) {
                this.description = objectInputStream.readUTF();
            }
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    @Override
    public String toString() {
        return "GenericSignupMessage{" +
                "miscEmotes=" + miscEmotes +
                ", signups=" + signups +
                ", name='" + name + '\'' +
                '}';
    }
}
