package nl.elec332.bot.discord.ps2outfits;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import nl.elec332.planetside2.api.objects.player.IOutfit;

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

    private static final String BANNED = "<>[]";
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
                textChannel.sendMessage("You cannot send commands from a webhook!").submit();
                return;
            }

            System.out.printf("(%s {%s})[%s {%s}]<%s>: %s\n", guild.getName(), guild.getId(), textChannel.getName(), textChannel.getId(), member.getEffectiveName(), msg);
            processCommand(textChannel, command, args, member, guild.getId());
        }
    }

    private void processCommand(TextChannel channel, String command, String args, Member member, String serverId) {
        try {
            if (command.equals("setoutfit")) {
                setOutfitCommand(channel, member, args, serverId);
                return;
            }
            for (Commands cmd : Commands.values()) {
                if (command.equals(cmd.toString().toLowerCase(Locale.ROOT)) || cmd.getAliases().contains(command)) {
                    cmd.executeCommand(channel, args);
                    return;
                }
            }
            for (OutfitCommands cmd : OutfitCommands.values()) {
                if (command.equals(cmd.toString().toLowerCase(Locale.ROOT)) || cmd.getAliases().contains(command)) {
                    String outfit = serverMapping.get(serverId);
                    if (outfit == null || outfit.isEmpty()) {
                        channel.sendMessage("You need to set an outfit for this server before you can use this command!").submit();
                        return;
                    }
                    IOutfit outfitInstance = Main.API.getOutfitManager().getCached(Long.parseLong(outfit));
                    if (outfitInstance == null) {
                        channel.sendMessage("Outfit not found!").submit();
                        return;
                    }
                    cmd.executeCommand(channel, outfitInstance, args);
                }
            }
        } catch (InsufficientPermissionException e) {
            channel.sendMessage("The bot has insufficient permissions to perform this command!\n Please re-invite the bot with the following link. (Settings will be saved)\n" + Main.INVITE_URL).submit();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            channel.sendMessage("Failed to process command, (Type: " + e.getClass().getName() + ") message: " + e.getMessage()).submit();
        }
    }

    private void setOutfitCommand(TextChannel channel, Member member, String args, String serverId) {
        if (member.hasPermission(Permission.ADMINISTRATOR)) {
            for (char c : BANNED.toCharArray()) {
                if (args.contains("" + c)) {
                    channel.sendMessage("An outfit name or tag cannot contain any of the following characters: \"" + BANNED + "\"").submit();
                    return;
                }
            }
            IOutfit outfit = Main.API.getOutfitManager().getByName(args);
            if (outfit == null) {
                channel.sendMessage("Failed to find outfit: \"" + args + "\"\nPlease make sure you give the bot your full outfit name or outfit tag.").submit();
                return;
            }

            serverMapping.put(serverId, outfit.getId() + "");
            save.run();
            channel.sendMessage("Successfully set outfit ID to: " + outfit.getId() + " (" + outfit.getName() + ") for this server!").submit();
        } else {
            channel.sendMessage("You can only use this command as an administrator!").submit();
        }
    }

}
