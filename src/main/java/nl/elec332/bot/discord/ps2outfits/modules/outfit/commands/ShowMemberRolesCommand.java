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

/**
 * Created by Elec332 on 20/05/2022
 */
public class ShowMemberRolesCommand extends SimpleCommand<OutfitConfig> {

    public ShowMemberRolesCommand() {
        super("showServerRoles", "Shows the role mapping for this server, can only used by server admins!");
    }

    @Override
    public boolean executeCommand(MessageChannel channel, Message message, Member member, OutfitConfig config, String arg) {
        if (member.hasPermission(Permission.ADMINISTRATOR)) {
            AsyncExecutor.executeAsync(() -> {
                Message msg = channel.sendMessage(".").submit().join();
                StringBuilder mss = new StringBuilder("Server role mapping:\n");
                for (OutfitRoleTypes t : OutfitRoleTypes.values()) {
                    mss.append(" -")
                            .append(t.getRoleNamePP())
                            .append(" (")
                            .append(t.getRoleName())
                            .append("): ");
                    Role r = config.getOutfitRole(t, member.getGuild());
                    if (r != null) {
                        mss.append(r.getAsMention());
                    } else {
                        mss.append("Unmapped");
                    }
                    mss.append("\n");
                }
                msg.editMessage(mss).submit();
            });
        } else {
            channel.sendMessage("You can only use this command as an administrator!").submit();
        }
        return true;
    }

}
