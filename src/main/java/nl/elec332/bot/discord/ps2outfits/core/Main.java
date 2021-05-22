package nl.elec332.bot.discord.ps2outfits.core;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import nl.elec332.bot.discord.ps2outfits.api.IBotConfigurator;
import nl.elec332.bot.discord.ps2outfits.api.IBotModule;
import nl.elec332.bot.discord.ps2outfits.api.ICommand;
import nl.elec332.bot.discord.ps2outfits.api.IConfigurableBotModule;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by Elec332 on 21-10-2020
 */
public class Main {

    public static final String INVITE_URL = "https://discord.com/api/oauth2/authorize?client_id=827934989764788259&permissions=313408&scope=bot";

    private static final String TOKEN_PROP = "discordToken";

    private static final File ROOT;
    private static final File EXEC;
    private static String TOKEN;

    //Start bot and load server mappings from file
    @SuppressWarnings("unchecked")
    public static void main(String... args) throws Exception {
        Set<String> props = new HashSet<>();
        Set<IBotConfigurator> pls = ServiceLoader.load(IBotConfigurator.class).stream()
                .map(ServiceLoader.Provider::get)
                .peek(pl -> pl.addProperties(props::add))
                .collect(Collectors.toSet());
        Function<String, String> propGetter = loadProperties(props);
        props.clear();
        pls.forEach(pl -> pl.handleProperties(propGetter));
        pls.forEach(pl -> pl.addHelpCommandNames(props::add));

        Map<IBotModule<?>, Set<ICommand<?>>> modules = ServiceLoader.load(IBotModule.class).stream()
                .map(ServiceLoader.Provider::get)
                .peek(m_ -> {
                    if (m_ instanceof IConfigurableBotModule) {
                        IConfigurableBotModule<?> m = (IConfigurableBotModule<?>) m_;
                        File f = getFile(m.getModuleName().toLowerCase(Locale.ROOT) + ".dmc");
                        if (f.exists()) {
                            try {
                                synchronized (m) {
                                    ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(new FileInputStream(f)));
                                    int ver = ois.readInt();
                                    m.deserialize(ois, ver);
                                    ois.close();
                                }
                            } catch (Exception e) {
                                throw new RuntimeException("Failed to load settings from module: " + m.getModuleName());
                            }
                        }
                        Runnable save = () -> {
                            try {
                                synchronized (m) {
                                    File back = new File(f.getAbsolutePath() + ".back");
                                    Files.move(f.toPath(), back.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                    ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(f)));
                                    oos.writeInt(m.getFileVersion());
                                    m.serialize(oos);
                                    oos.close();
                                    back.delete();
                                }
                            } catch (Exception e) {
                                throw new RuntimeException("Failed save settings from module: " + m.getModuleName(), e);
                            }
                        };
                        synchronized (m) {
                            m.initialize(save);
                        }
                    }
                }).collect(Collectors.toMap(k -> k, k -> {
                    Set<ICommand<?>> ret = new HashSet<>();
                    k.registerCommands(c -> ret.add((ICommand<?>) c));
                    return ret;
                }));
        try {
            JDA jda = JDABuilder.createDefault(TOKEN).enableIntents(GatewayIntent.GUILD_MEMBERS).build();
            jda.awaitReady();
            jda.addEventListener(new ChatHandler(Collections.unmodifiableMap(modules), props));
            System.out.println("Finished Building JDA!");
        } catch (LoginException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static File getFile(String name) {
        File ret = new File(EXEC, name);
        if (!ret.exists()) {
            ret = new File(ROOT, name);
        }
        return ret;
    }

    private static Function<String, String> loadProperties(Collection<String> extraProps) throws IOException {
        File f = getFile("bot.properties");
        System.out.println("Reading properties file: " + f);
        Properties appProps = new Properties();
        if (!f.exists()) {
            appProps.put(TOKEN_PROP, "");
            for (String s : extraProps) {
                appProps.put(s, "");
            }
            appProps.store(new FileOutputStream(f), "Discord Outfit bot settings");
            appProps = new Properties();
        }
        appProps.load(new FileInputStream(f));
        TOKEN = appProps.getProperty(TOKEN_PROP);
        appProps.remove(TOKEN_PROP);
        Properties finalAppProps = appProps;
        return finalAppProps::getProperty;
    }

    //Load bot properties & tokens
    static {
        try {
            ROOT = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
            EXEC = new File(new File("").getAbsolutePath());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to load property file locations: ", e);
        }
    }

}
