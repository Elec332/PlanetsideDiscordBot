package nl.elec332.bot.discord.ps2outfits;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Locale;
import java.util.Map;

/**
 * Created by Elec332 on 21-10-2020
 */
public class ChatHandler extends ListenerAdapter {

    public ChatHandler(Map<String, String> serverMapping, Runnable save) {
        this.serverMapping = serverMapping;
        this.save = save;
    }

    private final Map<String, String> serverMapping;
    private final Runnable save;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        String msg = message.getContentDisplay();

        if (event.isFromType(ChannelType.TEXT)) {
            Guild guild = event.getGuild();
            TextChannel textChannel = event.getTextChannel();
            Member member = event.getMember();

            if (!msg.startsWith("!")) {
                return;
            }
            msg = msg.substring(1);

            String command = msg.split(" ")[0].toLowerCase(Locale.ROOT);
            String args = msg.replace(command, "").trim();

            if (member == null || event.isWebhookMessage()) {
                textChannel.sendMessage("You cannot send commands from a webhook!");
                return;
            }

            System.out.printf("(%s)[%s]<%s>: %s\n", guild.getName(), textChannel.getName(), member.getEffectiveName(), msg);
            processCommand(textChannel, command, args, member, guild.getId());
        }
    }

    private void processCommand(TextChannel channel, String command, String args, Member member, String serverId) {
        try {
            if (command.equals("setoutfit")) {
                setOutfitCommand(channel, member, args, serverId);
                return;
            }
            String outfit = serverMapping.get(serverId);
            if (outfit == null || outfit.isEmpty()) {
                channel.sendMessage("Please set an outfit before sending commands!").submit();
                return;
            }
            for (Commands cmd : Commands.values()) {
                if (command.equals(cmd.toString().toLowerCase(Locale.ROOT))) {
                    cmd.executeCommand(channel, outfit);
                    return;
                }
            }

        } catch (Exception e) {
            channel.sendMessage("Failed to process command, message: " + e.getMessage()).submit();
        }
    }

    private void setOutfitCommand(TextChannel channel, Member member, String args, String serverId) {
        if (member.hasPermission(Permission.ADMINISTRATOR)) {
            JsonObject jo = Util.invokeAPI("outfit", "name=" + args);
            JsonElement je = jo.get("returned");
            if (je == null || je.getAsInt() != 1) {
                jo = Util.invokeAPI("outfit", "alias=" + args);
                je = jo.get("returned");
                if (je == null || je.getAsInt() != 1) {
                    channel.sendMessage("Failed to find outfit: \"" + args + "\"").submit();
                    return;
                }
            }
            je = jo.get("outfit_list").getAsJsonArray().get(0);
            String outId = je.getAsJsonObject().get("outfit_id").getAsString();
            serverMapping.put(serverId, outId);
            save.run();
            channel.sendMessage("Successfully set outfit ID to: " + outId + " for this server!").submit();
        } else {
            channel.sendMessage("You can only use this command as an administrator!").submit();
        }
    }

}
