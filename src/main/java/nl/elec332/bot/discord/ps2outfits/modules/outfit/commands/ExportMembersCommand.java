package nl.elec332.bot.discord.ps2outfits.modules.outfit.commands;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.utils.concurrent.Task;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.OutfitConfig;
import nl.elec332.discord.bot.core.api.util.SimpleCommand;
import nl.elec332.planetside2.ps2api.api.objects.player.IOutfit;
import nl.elec332.planetside2.ps2api.api.objects.player.IOutfitMember;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by Elec332 on 22/05/2021
 */
public class ExportMembersCommand extends SimpleCommand<OutfitConfig> {

    public ExportMembersCommand() {
        super("ExportMembers", "Exports the current members (with rank, activity and discord name) to a xlsx");
    }

    @Override
    public boolean executeCommand(MessageChannel channel, Member member, OutfitConfig config, String... args) {
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
        red.setFillForegroundColor(new XSSFColor(Color.RED, null));
        red.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        orange.setFillForegroundColor(new XSSFColor(Color.ORANGE, null));
        orange.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        green.setFillForegroundColor(new XSSFColor(Color.GREEN, null));
        green.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Instant lastMonth = Instant.now().minus(30, ChronoUnit.DAYS);
        Instant twoMonths = Instant.now().minus(60, ChronoUnit.DAYS);

        AtomicInteger counter = new AtomicInteger(2);
        Task<List<Member>> mt = ((TextChannel) channel).getGuild().loadMembers();
        int col2 = col;

        mt.onSuccess(members -> {
            outfit.getOutfitMemberInfo().stream()
                    .sorted(Comparator.comparing(IOutfitMember::getRankIndex).thenComparing(Collections.reverseOrder(Comparator.comparing(IOutfitMember::getLastPlayerActivity))))
                    .forEach(m -> {
                        Row row = sheet.createRow(counter.getAndIncrement());

                        row.createCell(name).setCellValue(m.getPlayerName());
                        String pnLower = m.getPlayerName().toLowerCase(Locale.ROOT);
                        String pnLowerSub = pnLower.length() > 9 ? pnLower.substring(2, pnLower.length() - 2) : null;
                        Member dcMember = members.stream()
                                .filter(m2 -> pnLower.equalsIgnoreCase(m2.getEffectiveName()) || m2.getEffectiveName().equalsIgnoreCase(pnLower))
                                .findFirst()
                                .orElse(members.stream()
                                        .filter(m2 -> {
                                            String nl = m2.getEffectiveName().toLowerCase(Locale.ROOT);
                                            return nl.length() >= 6 && (pnLower.contains(nl) || nl.contains(pnLower));
                                        })
                                        .findFirst()
                                        .orElse(pnLowerSub == null ? null : members.stream()
                                                .filter(m2 -> {
                                                    String nl = m2.getEffectiveName().toLowerCase(Locale.ROOT);
                                                    return pnLowerSub.contains(nl) || nl.contains(pnLowerSub);
                                                })
                                                .findFirst()
                                                .orElse(null)
                                        )
                                );
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
                        if (roles.contains(m.getRankName().replace("-", " "))) {
                            row.getCell(rank).setCellStyle(green);
                            row.getCell(discord).setCellStyle(green);
                        }
                    });
            for (int i = 0; i < col2 + 1; i++) {
                sheet.autoSizeColumn(i);
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
