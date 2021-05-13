package nl.elec332.bot.discord.ps2outfits;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.utils.concurrent.Task;
import nl.elec332.planetside2.api.objects.player.IOutfit;
import nl.elec332.planetside2.api.objects.player.IOutfitMember;
import nl.elec332.planetside2.api.objects.player.IPlayerResponseList;
import nl.elec332.planetside2.api.objects.player.request.ICharacterStat;
import nl.elec332.planetside2.api.objects.player.request.ICharacterStatHistory;
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
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by Elec332 on 27/04/2021
 */
public enum OutfitCommands {

    DAILYKD("Shows daily KD stats for everyone in your outfit.") {
        @Override
        public void executeCommand(TextChannel channel, IOutfit outfit, String... args) {
            CommandHelper.postKDInfo(channel, outfit, "today", Instant.now().minus(1, ChronoUnit.DAYS), h -> h.getDay(1));
        }

    },
    WEEKLYKD("Shows weekly KD stats for everyone in your outfit.") {
        @Override
        public void executeCommand(TextChannel channel, IOutfit outfit, String... args) {
            CommandHelper.postKDInfo(channel, outfit, "this week", fromCal(cal -> cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek())), h -> h.getWeek(1));
        }

    },
    MONTHLYKD("Shows monthly KD stats for everyone in your outfit.") {
        @Override
        public void executeCommand(TextChannel channel, IOutfit outfit, String... args) {
            CommandHelper.postKDInfo(channel, outfit, "this month", fromCal(cal -> cal.set(Calendar.DAY_OF_MONTH, 0)), h -> h.getMonth(1));
        }

    },
    DAILYSTATS("Shows daily stats for your outfit.") {
        @Override
        public void executeCommand(TextChannel channel, IOutfit outfit, String... args) {
            Collection<Long> onlineMembers = outfit.getPlayerIds(p -> p.getLastPlayerActivity().isAfter(Instant.now().minus(1, ChronoUnit.DAYS)));
            if (onlineMembers.isEmpty()) {
                channel.sendMessage("No members have been online!").submit();
                return;
            }
            String title = outfit.getName() + " daily stats";
            String description = "Out of " + outfit.getMembers() + " members, " + onlineMembers.size() + " have been online today";

            IPlayerResponseList<ICharacterStatHistory> historyStats = Main.API.getPlayerRequestHandler().getSlimCharacterStatHistory(onlineMembers, "facility_capture", "deaths", "kills");
            IPlayerResponseList<ICharacterStat> stats = Main.API.getPlayerRequestHandler().getSlimCharacterStats(onlineMembers, "assist_count");

            Collection<Map.Entry<String, String>> kdInfo = CommandHelper.getKDInfo(historyStats, i -> i.getDay(1));
            if (kdInfo.size() != onlineMembers.size()) {
                description += "\n (" + kdInfo.size() + " members were active in that period)";
            }

            EmbedBuilder builder = new EmbedBuilder()
                    .setTitle(title)
                    .setDescription(description);

            String kdStr = kdInfo.stream()
                    .limit(10)
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining("\n"));
            builder.addField("K/D Top-10", kdStr, false);

            String assistStr = stats
                    .streamResponse(p -> p.getFirstResponse().getDaily(), 1, Comparator.reverseOrder())
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .limit(10)
                    .collect(Collectors.joining("\n"));
            builder.addField("Assists Top-10", assistStr, false);

            String capDef = historyStats.streamResponse(p -> p.getResponseByName("facility_capture").getDay(1), 0, Comparator.reverseOrder())
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .limit(10)
                    .collect(Collectors.joining("\n"));
            builder.addField("Facilities captured Top-10:", capDef, false);

            builder.addField("Repair Ribbons Top-5", "//todo", false);

            builder.addField("Revive & Heal Ribbons Top-5", "//todo", false);

            channel.sendMessage(builder.build()).submit();
        }

    },
    BASTIONSTATS("Shows faction bastion statistics") {
        @Override
        public void executeCommand(TextChannel channel, IOutfit outfit, String... args) {
            Collection<Long> onlineMembers = outfit.getPlayerIds(p -> p.getLastPlayerActivity().isAfter(Instant.now().minus(30, ChronoUnit.DAYS)));
            if (onlineMembers.isEmpty()) {
                channel.sendMessage("No members have been online!").submit();
                return;
            }
            String title = outfit.getName() + " Bastion (Mauler Cannon) stats";
            EmbedBuilder builder = new EmbedBuilder()
                    .setTitle(title);


//            Outfit outfit = Outfit.getOutfit(outfitID);
//            List<String> onlineMembers = outfit.getOnlinePlayersBefore(Instant.now().minus(1, ChronoUnit.DAYS));
//
//
//            String title = outfit.getName() + " bastion stats";
//            EmbedBuilder builder = new EmbedBuilder()
//                    .setTitle(title);
//
//            String question = "character_id=" + String.join(",", onlineMembers) + "&";
//            question += "c:join=characters_weapon_stat_by_faction^terms:stat_name=weapon_kills'item_id=6009001'item_id=6009002'item_id=6009003'item_id=6009004^list:1&c:show=character_id,name";
//            JsonArray large = CensusAPI.API.invokeAPI("character", question);
//            Map<String, Integer> data = new HashMap<>();
//            for (int i = 0; i < large.size(); i++) {
//                JsonObject object = large.get(i).getAsJsonObject();
//                JsonArray array = object.getAsJsonArray("character_id_join_characters_weapon_stat_by_faction");
//                if (array == null) {
//                    continue;
//                }
//                int kills = 0;
//                for (int j = 0; j < array.size(); j++) {
//                    JsonObject o = array.get(j).getAsJsonObject();
//                    kills += Integer.parseInt(o.get("value_vs").getAsString());
//                    //kills += Integer.parseInt(o.get("value_nc").getAsString());
//                    kills += Integer.parseInt(o.get("value_tr").getAsString());
//                }
//                data.put(object.getAsJsonObject("name").get("first").getAsString(), kills);
//            }
//            data.entrySet().stream()
//                    .filter(e -> e.getValue() > 0)
//                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
//                    .limit(10)
//                    .forEach(e -> builder.addField(e.getKey(), e.getValue() + "kills", false));
//            channel.sendMessage(builder.build()).submit();
            channel.sendMessage("//TODO").submit();
        }
    },
    EXPORTMEMBERS("Exports the current members (with rank, activity and discord name) to a xlsx") {
        @Override
        public void executeCommand(TextChannel channel, IOutfit outfit, String... args) {
            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet("Members");
            int col = 0;
            int name = col++;
            int discord = col++;
            int lastOnline = col++;
            int rank = col++;
            Row main = sheet.createRow(0);
            main.createCell(name).setCellValue("Name:");
            main.createCell(discord).setCellValue("Discord Name:");
            main.createCell(lastOnline).setCellValue("Last Online:");
            main.createCell(rank).setCellValue("Current Rank:");
            XSSFCellStyle orange = workbook.createCellStyle();
            XSSFCellStyle red = workbook.createCellStyle();
            red.setFillForegroundColor(new XSSFColor(Color.RED, null));
            red.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            orange.setFillForegroundColor(new XSSFColor(Color.ORANGE, null));
            orange.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Instant lastMonth = Instant.now().minus(30, ChronoUnit.DAYS);
            Instant twoMonths = Instant.now().minus(60, ChronoUnit.DAYS);

            AtomicInteger counter = new AtomicInteger(2);
            Task<List<Member>> mt = channel.getGuild().loadMembers();
            int col2 = col;

            mt.onSuccess(members -> {
                outfit.getOutfitMemberInfo().stream()
                        .sorted(Comparator.comparing(IOutfitMember::getRankIndex).thenComparing(Collections.reverseOrder(Comparator.comparing(IOutfitMember::getLastPlayerActivity))))
                        .forEach(m -> {
                            Row row = sheet.createRow(counter.getAndIncrement());

                            row.createCell(name).setCellValue(m.getPlayerName());
                            String pnLower = m.getPlayerName().toLowerCase(Locale.ROOT);
                            String pnLowerSub = pnLower.length() > 9 ? pnLower.substring(2, pnLower.length() - 2) : null;
                            String dcName = members.stream()
                                    .map(Member::getEffectiveName)
                                    .filter(m2 -> pnLower.equalsIgnoreCase(m2) || m2.equalsIgnoreCase(pnLower))
                                    .findFirst()
                                    .orElse(members.stream()
                                            .map(Member::getEffectiveName)
                                            .filter(s -> s.length() >= 6)
                                            .filter(m2 -> {
                                                String nl = m2.toLowerCase(Locale.ROOT);
                                                return pnLower.contains(nl) || nl.contains(pnLower);
                                            })
                                            .findFirst()
                                            .orElse(pnLowerSub == null ? "-" : members.stream()
                                                    .map(Member::getEffectiveName)
                                                    .filter(m2 -> {
                                                        String nl = m2.toLowerCase(Locale.ROOT);
                                                        return pnLowerSub.contains(nl) || nl.contains(pnLowerSub);
                                                    })
                                                    .findFirst()
                                                    .orElse("-")
                                            )
                                    );
                            row.createCell(discord).setCellValue(dcName);
                            Cell cell = row.createCell(lastOnline);
                            Instant active = m.getLastPlayerActivity();
                            cell.setCellValue(active.toString());
                            if (active.isBefore(twoMonths)) {
                                cell.setCellStyle(red);
                            } else if (active.isBefore(lastMonth)) {
                                cell.setCellStyle(orange);
                            }
                            row.createCell(rank).setCellValue(m.getRankName());
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
        }
    };

    OutfitCommands(String help) {
        this(help, "");
    }

    OutfitCommands(String help, String args) {
        this.help = Objects.requireNonNull(help);
        this.args = Objects.requireNonNull(args);
        this.aliases = new ArrayList<>();
        addAliases(this.aliases::add);
    }

    private final String help;
    private final String args;
    private final List<String> aliases;

    void addAliases(Consumer<String> reg) {
    }

    public String getHelpText() {
        return help;
    }

    public String getArgs() {
        return args;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public abstract void executeCommand(TextChannel channel, IOutfit outfit, String... args);

    private static Instant fromCal(Consumer<Calendar> mod) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);

        mod.accept(cal);

        return Instant.ofEpochMilli(cal.getTimeInMillis());
    }

}
