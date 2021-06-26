import nl.elec332.discord.bot.core.api.IBotConfigurator;
import nl.elec332.discord.bot.core.api.IBotModule;
import nl.elec332.bot.discord.ps2outfits.PS2BotConfigurator;
import nl.elec332.bot.discord.ps2outfits.modules.normal.NormalModule;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.OutfitModule;
import nl.elec332.planetside2.ps2api.api.IPS2APIAccessor;

/**
 * Created by Elec332 on 30/04/2021
 */
module nl.elec332.bot.discord.ps2outfits {

    requires nl.elec332.discord.bot.core;
    requires nl.elec332.planetside2.api;

    requires poi.fat.jpms;
    requires java.desktop;

    uses IPS2APIAccessor;
    uses IBotModule;
    uses IBotConfigurator;

    provides IBotModule with OutfitModule, NormalModule;
    provides IBotConfigurator with PS2BotConfigurator;

    opens nl.elec332.bot.discord.ps2outfits.modules.outfit to com.google.gson;

}