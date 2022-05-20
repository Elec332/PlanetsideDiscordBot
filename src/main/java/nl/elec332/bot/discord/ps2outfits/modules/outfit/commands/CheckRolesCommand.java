package nl.elec332.bot.discord.ps2outfits.modules.outfit.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.utils.concurrent.Task;
import nl.elec332.bot.discord.ps2outfits.CommandHelper;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.MiscEmotes;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.OutfitConfig;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.OutfitRoleTypes;
import nl.elec332.discord.bot.core.api.util.SimpleCommand;
import nl.elec332.discord.bot.core.util.AsyncExecutor;
import nl.elec332.planetside2.ps2api.api.objects.player.IOutfit;
import nl.elec332.planetside2.ps2api.api.objects.player.IOutfitMember;
import nl.elec332.planetside2.ps2api.api.objects.player.IPlayer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Elec332 on 20/05/2022
 */
public class CheckRolesCommand extends SimpleCommand<OutfitConfig> {

    public CheckRolesCommand() {
        super("checkServerRoles", "Shows the role mapping for this server, can only used by server admins!");
    }

    @Override
    public boolean executeCommand(MessageChannel channel, Message message, Member sender, OutfitConfig config, String arg) {
        if (!sender.hasPermission(Permission.ADMINISTRATOR)) {
            channel.sendMessage("You can only use this command as an administrator!").submit();
            return true;
        }
        AsyncExecutor.executeAsync(() -> {
            Guild guild = sender.getGuild();;
            Set<Role> reservedRoles = new HashSet<>();
            Map<Integer, Role> rankMap = new HashMap<>();
            for (OutfitRoleTypes rt : OutfitRoleTypes.values()) {
                reservedRoles.add(config.getOutfitRole(rt,guild));
            }
            IOutfit outfit = config.getOutfit();
            for (int i = 1; i < 9; i++) { //Ty DBG, im getting MatLab nightmares now (again)...
                String rankNameIg = outfit.getRankName(i).replace("-", " ").toLowerCase(Locale.ROOT);
                for (Role rr : guild.getRoles()) {
                    String s = rr.getName().replace("-", " ");
                    if (s.contains("[")) {
                        s = CommandHelper.trimPlayerName(s);
                    }
                    if (s.toLowerCase(Locale.ROOT).equals(rankNameIg)) {
                        reservedRoles.add(rr);
                        rankMap.put(i, rr);
                        break;
                    }
                }
            }
            reservedRoles.remove(null);

            Task<List<Member>> mt = ((TextChannel) channel).getGuild().loadMembers();
            mt.onSuccess(members -> {
                for (final Member member : members) {
                    boolean mapped = config.hasMappedAccount(member);
                    if (!mapped && member.getRoles().stream().anyMatch(reservedRoles::contains) && !"mapped".equals(arg)) {
                        final Set<Role> res = new HashSet<>(member.getRoles());
                        res.retainAll(reservedRoles);
                        Message msg = channel.sendMessage(".").submit().join();
                        msg.editMessage(member.getAsMention() + " is not mapped to a player, yet he has the following reserved roles: " + res.stream().map(IMentionable::getAsMention).collect(Collectors.joining(", "))).submit();
                        msg.addReaction(MiscEmotes.CHECK_MARK_EMOJI).queue();
                        msg.addReaction(MiscEmotes.RED_CROSS_EMOJI).queue();
                        config.addConfirm(msg.getIdLong(), mbr -> {
                            if (mbr.hasPermission(Permission.ADMINISTRATOR)) {
                                res.forEach(r -> guild.removeRoleFromMember(member, r).submit().join());
                            }
                        });
                    }
                    if (mapped) {
                        Set<IPlayer> players = new HashSet<>();
                        players.add(config.getDirectMapping(member));
                        players.addAll(config.getAltMapping(member));
                        boolean outfitM = false, faction = false, foreigner = false, iaO = false;
                        int highestRole = 0;
                        for (IPlayer player : players) {
                            if (player.getOutfit() != null && player.getOutfit().getId() == outfit.getId()) {
                                IOutfitMember om = outfit.getMemberInfo(player.getId());
                                if (om == null) {
                                    System.out.println(player.getName() + " couldn't be matched to outfit!");
                                } else {
                                    outfitM = true;
                                    highestRole = Math.max(highestRole, om.getRankIndex());
                                    continue;
                                }
                            }
                            if (player.getFaction().getId() == outfit.getFaction().getId()) {
                                faction = true;
                                if (player.getOutfit() != null) {
                                    iaO = true;
                                }
                            } else {
                                foreigner = true;
                            }
                        }
                        if (highestRole >= 8) {
                            continue;
                        }
                        String msgS = "";
                        String msgS2 = "";
                        Set<Role> supposedRoles = new HashSet<>();
                        if (outfitM) {
                            msgS2 = " (Outfit member)";
                            supposedRoles.add(config.getOutfitRole(OutfitRoleTypes.MEMBER, guild));
                        }
                        if (faction) {
                            if (iaO) {
                                msgS2 = " (In another outfit)";
                            } else {
                                msgS2 = " (Not in an outfit)";
                            }
                            supposedRoles.add(config.getOutfitRole(OutfitRoleTypes.SAME_FACTION, guild));
                        }
                        if (foreigner) {
                            supposedRoles.add(config.getOutfitRole(OutfitRoleTypes.OTHER_FACTION, guild));
                        }
                        if (highestRole > 0) {
                            supposedRoles.add(rankMap.get(highestRole));
                        }
                        supposedRoles.remove(null);
                        final Set<Role> supposed2 = new HashSet<>(supposedRoles);
                        final Set<Role> res = new HashSet<>(member.getRoles());
                        res.retainAll(reservedRoles);
                        supposed2.removeAll(res);
                        res.removeAll(supposedRoles);
                        if (!res.isEmpty()) {
                            msgS += "\n -The following roles will be removed: " + res.stream().map(IMentionable::getAsMention).collect(Collectors.joining(", "));
                        }
                        if (!supposed2.isEmpty()) {
                            msgS += "\n -The following roles will be added: " + supposed2.stream().map(IMentionable::getAsMention).collect(Collectors.joining(", "));
                        }
                        if (msgS.isEmpty()) {
                            continue;
                        }
                        Message msg = channel.sendMessage(".").submit().join();
                        msg.editMessage(member.getAsMention() + " is a mapped player" + msgS2 + ", yet the roles do not match: " + msgS).submit();
                        msg.addReaction(MiscEmotes.CHECK_MARK_EMOJI).queue();
                        msg.addReaction(MiscEmotes.RED_CROSS_EMOJI).queue();
                        config.addConfirm(msg.getIdLong(), mbr -> {
                            if (mbr.hasPermission(Permission.ADMINISTRATOR)) {
                                res.forEach(r -> guild.removeRoleFromMember(member, r).submit().join());
                                supposed2.forEach(r -> guild.addRoleToMember(member, r).submit().join());
                                channel.sendMessage(member.getEffectiveName() + "'s roles have been updated.").submit();
                            }
                        });
                    }
                }
                channel.sendMessage("Role check finished").submit();
            });
        });
        return true;
    }

}
