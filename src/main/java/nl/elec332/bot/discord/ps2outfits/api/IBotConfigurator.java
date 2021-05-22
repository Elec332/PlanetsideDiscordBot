package nl.elec332.bot.discord.ps2outfits.api;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by Elec332 on 22/05/2021
 */
public interface IBotConfigurator {

    void addProperties(Consumer<String> registry);

    void handleProperties(Function<String, String> propertyGetter);

    default void addHelpCommandNames(Consumer<String> names) {
    }

}
