package nl.elec332.bot.discord.ps2outfits;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import nl.elec332.planetside2.api.IPS2APIAccessor;
import nl.elec332.planetside2.api.objects.IPS2API;
import nl.elec332.planetside2.util.NetworkUtil;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by Elec332 on 21-10-2020
 */
public class Main {

    public static final String INVITE_URL = "https://discord.com/api/oauth2/authorize?client_id=827934989764788259&permissions=313408&scope=bot";

    private static final String TOKEN_PROP = "discordToken";
    private static final String PS2_SID_PROP = "ps2ServiceID";

    private static final File ROOT;
    private static final File EXEC;
    private static final String TOKEN;
    public static final String PS2_SID;

    public static IPS2API API;

    //Start bot and load server mappings from file
    @SuppressWarnings("unchecked")
    public static void main(String... args) throws Exception {
        File sm = getFile("servermapping.pbm");
        Map<String, String> m;
        if (sm.exists()) {
            GZIPInputStream gis = new GZIPInputStream(new FileInputStream(sm));
            m = (Map<String, String>) new ObjectInputStream(gis).readObject();
            gis.close();
        } else {
            m = new HashMap<>();
        }
        Runnable save = () -> {
            try {
                GZIPOutputStream gos = new GZIPOutputStream(new FileOutputStream(sm));
                new ObjectOutputStream(gos).writeObject(m);
                gos.close();
            } catch (Exception e) {
                throw new RuntimeException("Failed to save server settings", e);
            }
        };
        try {
            JDA jda = JDABuilder.createDefault(TOKEN).enableIntents(GatewayIntent.GUILD_MEMBERS).build();
            jda.awaitReady();
            IPS2APIAccessor accessor = NetworkUtil.getAPIAccessor();//ServiceLoader.load(IPS2APIAccessor.class).findFirst().get();
            accessor.setServiceId(PS2_SID);
            API = accessor.getAPI();
            jda.addEventListener(new ChatHandler(m, save));
            System.out.println("Finished Building JDA!");
        } catch (LoginException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static File getFile(String name) {
        File ret = new File(EXEC, name);
        if (!ret.exists()) {
            ret = new File(ROOT, name);
        }
        return ret;
    }

    //Load bot properties & tokens
    static {
        try {
            ROOT = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
            EXEC = new File(new File("").getAbsolutePath());
            File f = getFile("bot.properties");
            System.out.println("Reading properties file: " + f);
            Properties appProps = new Properties();
            if (!f.exists()) {
                appProps.put(TOKEN_PROP, "");
                appProps.put(PS2_SID_PROP, "");
                appProps.store(new FileOutputStream(f), "Discord Outfit bot settings");
                appProps = new Properties();
            }
            appProps.load(new FileInputStream(f));
            TOKEN = appProps.getProperty(TOKEN_PROP);
            PS2_SID = appProps.getProperty(PS2_SID_PROP);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Failed to load properties: ", e);
        }
    }

}
