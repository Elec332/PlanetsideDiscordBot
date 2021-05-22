package nl.elec332.bot.discord.ps2outfits.core;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import nl.elec332.bot.discord.ps2outfits.api.IBotModule;
import nl.elec332.bot.discord.ps2outfits.api.ICommand;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Elec332 on 21-10-2020
 */
public class ChatHandler extends ListenerAdapter {

    public ChatHandler(Map<IBotModule<?>, Set<ICommand<?>>> modules, Collection<String> helpNames) {
        this.modules = modules;
        this.helpNames = helpNames.stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toList());
    }

    private final Map<IBotModule<?>, Set<ICommand<?>>> modules;
    private final List<String> helpNames;

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
            processCommand(textChannel, command, args, member, guild.getIdLong());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void processCommand(TextChannel channel, String command, String args, Member member, long serverId) {
        for (String s : helpNames) {
            if (command.equals(s)) {
                executeHelpCommand(channel);
                return;
            }
        }
        try {
            for (Map.Entry<IBotModule<?>, Set<ICommand<?>>> e : this.modules.entrySet()) {
                IBotModule module = e.getKey();
                Set<ICommand<?>> commands = e.getValue();
                Object cfg = module.getConfigFor(serverId);
                for (ICommand cmd : commands) {
                    if (!command.equals(cmd.toString().toLowerCase(Locale.ROOT)) && !cmd.getAliases().contains(command)) {
                        continue;
                    }
                    if (!module.canRunCommand(channel, member, cfg, cmd)) {
                        continue;
                    }
                    if (cmd.executeCommand(channel, member, cfg, args)) {
                        return;
                    }
                }
            }
        } catch (InsufficientPermissionException e) {
            channel.sendMessage("The bot has insufficient permissions to perform this command!\n Please re-invite the bot with the following link. (Settings will be saved)\n" + Main.INVITE_URL).submit();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            channel.sendMessage("Failed to process command, (Type: " + e.getClass().getName() + ") message: " + e.getMessage()).submit();
        }
    }

    private void executeHelpCommand(TextChannel channel) {
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle("Help info")
                .setDescription("All available commands & descriptions\n\n");

        builder.addField("!" + String.join(", !", helpNames), " Shows info about all available commands.", false);

        modules.forEach((m, c) -> {
            builder.addBlankField(false);
            builder.addField(m.getModuleName() + " commands", "", false);
            for (ICommand<?> cmd : c) {
                String extra = " ";
                if (!cmd.getAliases().isEmpty()) {
                    extra = " (!" + String.join(", !", cmd.getAliases()) + ") ";
                }
                String argsDesc = cmd.getArgs().isEmpty() ? "" : "<" + cmd.getArgs() + ">";
                builder.addField("!" + cmd.toString().toLowerCase(Locale.ROOT) + extra + argsDesc, cmd.getHelpText(), false);
            }
        });

        channel.sendMessage(builder.build()).submit();
    }

//    private void setOutfitCommand(TextChannel channel, Member member, String args, String serverId) {
//        if (member.hasPermission(Permission.ADMINISTRATOR)) {
//            for (char c : BANNED.toCharArray()) {
//                if (args.contains("" + c)) {
//                    channel.sendMessage("An outfit name or tag cannot contain any of the following characters: \"" + BANNED + "\"").submit();
//                    return;
//                }
//            }
//            IOutfit outfit = Main.API.getOutfitManager().getByName(args);
//            if (outfit == null) {
//                channel.sendMessage("Failed to find outfit: \"" + args + "\"\nPlease make sure you give the bot your full outfit name or outfit tag.").submit();
//                return;
//            }
//
//            serverMapping.put(serverId, outfit.getId() + "");
//            save.run();
//            channel.sendMessage("Successfully set outfit ID to: " + outfit.getId() + " (" + outfit.getName() + ") for this server!").submit();
//        } else {
//            channel.sendMessage("You can only use this command as an administrator!").submit();
//        }
//    }

}
