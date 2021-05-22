package nl.elec332.bot.discord.ps2outfits.api.util;

import nl.elec332.bot.discord.ps2outfits.api.ICommand;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Created by Elec332 on 22/05/2021
 */
public abstract class SimpleCommand<C> implements ICommand<C> {

    public SimpleCommand(String name, String helpText, String... args) {
        this.name = name;
        this.helpText = helpText;
        this.aliases = new HashSet<>();
        this.args = List.of(args);
        addAliases(this.aliases::add);
    }

    private final String name;
    private final String helpText;
    private final Set<String> aliases;
    private final List<String> args;


    void addAliases(Consumer<String> reg) {
    }

    @Override
    public final String toString() {
        return this.name;
    }

    @Override
    public final String getHelpText() {
        return this.helpText;
    }

    @Override
    public final String getArgs() {
        return String.join(",", args);
    }

    @Override
    public final Collection<String> getAliases() {
        return this.aliases;
    }

}
