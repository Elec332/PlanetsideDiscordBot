package nl.elec332.bot.discord.ps2outfits.modules.outfit.commands;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.utils.concurrent.Task;
import nl.elec332.bot.discord.ps2outfits.CommandHelper;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.OutfitConfig;
import nl.elec332.discord.bot.core.api.util.SimpleCommand;
import nl.elec332.planetside2.ps2api.api.objects.player.IOutfit;
import nl.elec332.planetside2.ps2api.api.objects.player.IOutfitMember;
import nl.elec332.planetside2.ps2api.api.objects.player.IPlayer;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by Elec332 on 22/05/2021
 */
public class ExportMembersCommand extends SimpleCommand<OutfitConfig> {

    public ExportMembersCommand() {
        super("ExportMembers", "Exports the current members (with rank, activity and discord name) to a xlsx. Pass the \"full\" argument to export Discord members aswell");
    }

    @Override
    public boolean executeCommand(MessageChannel channel, Message message, Member member, OutfitConfig config, String args) {
        IOutfit outfit = config.getOutfit();
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Members");
        int col = 0;
        int name = col++;
        int lastOnline = col++;
        int rank = col++;
        col++;
        int discord = col++;
        int dcRanks = col++;
        Row main = sheet.createRow(0);
        main.createCell(name).setCellValue("Name:");
        main.createCell(discord).setCellValue("Discord Name:");
        main.createCell(lastOnline).setCellValue("Last Online:");
        main.createCell(rank).setCellValue("Current Rank:");
        XSSFCellStyle orange = workbook.createCellStyle();
        XSSFCellStyle red = workbook.createCellStyle();
        XSSFCellStyle green = workbook.createCellStyle();
        XSSFCellStyle blue = workbook.createCellStyle();
        XSSFCellStyle purple = workbook.createCellStyle();
        XSSFCellStyle gray = workbook.createCellStyle();
        red.setFillForegroundColor(new XSSFColor(Color.RED, null));
        red.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        orange.setFillForegroundColor(new XSSFColor(Color.ORANGE, null));
        orange.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        green.setFillForegroundColor(new XSSFColor(Color.GREEN, null));
        green.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        blue.setFillForegroundColor(new XSSFColor(Color.BLUE, null));
        blue.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        purple.setFillForegroundColor(new XSSFColor(new Color(128, 64, 128), null));
        purple.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        gray.setFillForegroundColor(new XSSFColor(Color.GRAY, null));
        gray.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Instant lastMonth = Instant.now().minus(30, ChronoUnit.DAYS);
        Instant twoMonths = Instant.now().minus(60, ChronoUnit.DAYS);

        AtomicInteger counter = new AtomicInteger(2);
        Task<List<Member>> mt = ((TextChannel) channel).getGuild().loadMembers();
        int col2 = col;

        mt.onSuccess(members -> {
            Set<Member> processed = new HashSet<>();
            outfit.getOutfitMemberInfo().stream()
                    .sorted(Comparator.comparing(IOutfitMember::getRankIndex).thenComparing(Collections.reverseOrder(Comparator.comparing(IOutfitMember::getLastPlayerActivity))))
                    .forEach(m -> {
                        Row row = sheet.createRow(counter.getAndIncrement());

                        row.createCell(name).setCellValue(m.getPlayerName());
                        Member dcMember = config.getMemberFor(m, members);
                        if (dcMember != null) {
                            processed.add(dcMember);
                        }
                        List<String> roles = dcMember == null ? Collections.emptyList() : dcMember.getRoles().stream().map(Role::getName).map(s -> s.replace("-", " ")).collect(Collectors.toList());
                        row.createCell(discord).setCellValue(dcMember == null ? "-" : dcMember.getEffectiveName());
                        row.createCell(dcRanks).setCellValue(String.join(",", roles));
                        Cell cell = row.createCell(lastOnline);
                        Instant active = m.getLastPlayerActivity();
                        cell.setCellValue(active.toString());
                        if (active.isBefore(twoMonths)) {
                            cell.setCellStyle(red);
                        } else if (active.isBefore(lastMonth)) {
                            cell.setCellStyle(orange);
                        }
                        row.createCell(rank).setCellValue(m.getRankName());
                        String rankNameIg = m.getRankName().replace("-", " ");
                        if (roles.contains(rankNameIg) || roles.stream().filter(s -> s.contains("[")).map(CommandHelper::trimPlayerName).anyMatch(s -> s.equals(rankNameIg))) {
                            row.getCell(rank).setCellStyle(green);
                            row.getCell(discord).setCellStyle(green);
                        }
                    });
            for (int i = 0; i < col2 + 1; i++) {
                sheet.autoSizeColumn(i);
            }

            if (args.equals("full")) {
                XSSFSheet dcSheet = workbook.createSheet("Discord");
                Row dcRow = dcSheet.createRow(0);
                dcRow.createCell(0).setCellValue("Discord Name:");
                dcRow.createCell(1).setCellValue("Discord Ranks:");
                dcRow.createCell(2).setCellValue("Server:");
                dcRow.createCell(3).setCellValue("Outfit:");
                AtomicInteger dcCounter = new AtomicInteger(2);
                members.stream()
                        .filter(m -> !processed.contains(m))
                        .forEach(m -> {
                            if (m.getUser().isBot()) {
                                return;
                            }
                            IPlayer player = null;
                            try {
                                player = CommandHelper.getPlayer(config, m, null);
                            } catch (Exception e) {
                                System.out.println("----------------------");
                                System.out.println(m.getEffectiveName());
                                e.printStackTrace();
                            }
                            if (player != null && player.getOutfit() != null && player.getOutfit().getId() == outfit.getId()) {
                                return;
                            }
                            Row row = dcSheet.createRow(dcCounter.getAndIncrement());
                            Cell c = row.createCell(0);
                            c.setCellValue(m.getEffectiveName());
                            XSSFCellStyle color = gray;
                            if (player != null) {
                                if (player.getServer().getId() != config.getOutfit().getServer().getId()) {
                                    color = orange;
                                } else {
                                    String tag = player.getFaction().getTag().toLowerCase(Locale.ROOT);
                                    if (tag.equals("vs")) {
                                        color = purple;
                                    }
                                    if (tag.equals("nc")) {
                                        color = blue;
                                    }
                                    if (tag.equals("tr")) {
                                        color = red;
                                    }
                                }
                                c.setCellStyle(color);
                            }
                            Cell cell = row.createCell(1);
                            List<String> roles = m.getRoles().stream().map(Role::getName).collect(Collectors.toList());
                            cell.setCellValue(roles.stream().map(s -> s.replace("-", " ")).collect(Collectors.joining(",")));
                            for (int i = 1; i < 9; i++) { //Ty DBG, im getting MatLab nightmares now...
                                String rankNameIg = outfit.getRankName(i).replace("-", " ");
                                if (roles.contains(rankNameIg) || roles.stream().filter(s -> s.contains("[")).map(CommandHelper::trimPlayerName).anyMatch(s -> s.equals(rankNameIg))) {
                                    color = orange;
                                    if (player != null && player.getOutfit() != null) {
                                        color = red;
                                    }
                                    cell.setCellStyle(color);
                                }
                            }
                            cell = row.createCell(2);
                            if (player != null) {
                                color = green;
                                if (player.getServer().getId() != config.getOutfit().getServer().getId()) {
                                    color = red;
                                }
                                cell.setCellValue(player.getServer().getName());
                                cell.setCellStyle(color);
                            }
                            cell = row.createCell(3);
                            if (player != null && player.getOutfit() != null) {
                                cell.setCellValue(player.getOutfit().getObject().getTag());
                            }
                        });
                for (int i = 0; i < 4; i++) {
                    dcSheet.autoSizeColumn(i);
                }
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.getProperties().getCoreProperties().setCreator("PS2OutfitStats Bot");
            workbook.getProperties().getExtendedProperties().getUnderlyingProperties().setApplication("PS2OutfitStats");
            try {
                workbook.write(bos);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            channel.sendFile(bos.toByteArray(), "outfitmembers.xlsx").submit().join().delete().queueAfter(30, TimeUnit.SECONDS);
        });
        return true;
    }

}
