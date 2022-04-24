package nl.elec332.bot.discord.ps2outfits;

import nl.elec332.discord.bot.core.api.IBotConfigurator;
import nl.elec332.planetside2.ps2api.api.IPS2APIAccessor;
import nl.elec332.planetside2.ps2api.api.objects.IPS2API;

import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by Elec332 on 22/05/2021
 */
public class PS2BotConfigurator implements IBotConfigurator {

    private static final String PS2_SID_PROP = "ps2ServiceID";

    public static IPS2API API = null;
    public static IPS2APIAccessor API_ACCESSOR = null;

    @Override
    public void addProperties(Consumer<String> registry) {
        registry.accept(PS2_SID_PROP);
    }

    @Override
    public void handleProperties(Function<String, String> propertyGetter, List<String> args) {
        IPS2APIAccessor accessor = ServiceLoader.load(IPS2APIAccessor.class).findFirst().get();
        accessor.setServiceId(propertyGetter.apply(PS2_SID_PROP));
        System.out.println(propertyGetter.apply(PS2_SID_PROP));
        API_ACCESSOR = accessor;
        if (args.contains("-noSSL")) {
            API_ACCESSOR.getCensusAPI().disableSSL();
        }
        API = accessor.getAPI();
    }

    @Override
    public void addHelpCommandNames(Consumer<String> names) {
        names.accept("help");
        names.accept("outfithelp");
        names.accept("outfitcommands");
        names.accept("ps2commands");
        names.accept("ps2commands");
    }

}
