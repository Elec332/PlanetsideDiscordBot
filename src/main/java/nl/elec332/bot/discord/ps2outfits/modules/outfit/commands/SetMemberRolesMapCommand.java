package nl.elec332.bot.discord.ps2outfits.modules.outfit.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.OutfitConfig;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.OutfitRoleTypes;
import nl.elec332.discord.bot.core.api.util.SimpleCommand;
import nl.elec332.discord.bot.core.util.AsyncExecutor;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Elec332 on 20/05/2022
 */
public class SetMemberRolesMapCommand extends SimpleCommand<OutfitConfig> {

    public SetMemberRolesMapCommand(Runnable save) {
        super("setMappedServerRole", "Maps a role type to a server role, can only used by server admins!", "type_name", "Role_ping");
        this.save = save;
    }

    private final Runnable save;

    @Override
    public boolean executeCommand(MessageChannel channel, Message message, Member member, OutfitConfig config, String arg) {
        if (member.hasPermission(Permission.ADMINISTRATOR)) {
            AsyncExecutor.executeAsync(() -> {
                String[] split = arg.split(" ");
                OutfitRoleTypes type = Arrays.stream(OutfitRoleTypes.values())
                        .filter(r -> r.getRoleName().equals(split[0]))
                        .findFirst()
                        .orElse(null);
                List<Role> mentioned = message.getMentionedRoles();
                Role role = null;
                if (mentioned.size() == 1) {
                    role = mentioned.get(0);
                } else if (split.length == 2) {
                    role = member.getGuild().getRolesByName(split[1], true).stream().findFirst().orElse(null);
                }
                if (type == null || role == null) {
                    channel.sendMessage("Invalid type or role!").submit().join();
                    return;
                }
                String txt = "Successfully mapped \"" + type.getRoleNamePP() + "\" to role: " + role.getAsMention();
                config.setOutfitRole(type, role);
                save.run();
                Message msg = channel.sendMessage(".").submit().join();
                msg.editMessage(txt).submit();
            });
        } else {
            channel.sendMessage("You can only use this command as an administrator!").submit();
        }
        return true;
    }

}
