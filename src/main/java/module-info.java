import nl.elec332.bot.discord.ps2outfits.api.IBotConfigurator;
import nl.elec332.bot.discord.ps2outfits.api.IBotModule;
import nl.elec332.bot.discord.ps2outfits.modules.PS2BotConfigurator;
import nl.elec332.bot.discord.ps2outfits.modules.normal.NormalModule;
import nl.elec332.bot.discord.ps2outfits.modules.outfit.OutfitModule;
import nl.elec332.planetside2.api.IPS2APIAccessor;

/**
 * Created by Elec332 on 30/04/2021
 */
module nl.elec332.bot.discord.ps2outfits {

    requires net.dv8tion.jda;
    requires com.fasterxml.jackson.core;
    requires transitive java.desktop;

    requires nl.elec332.planetside2api;
    requires org.apache.poi.ooxml;
    requires org.apache.poi.poi;

    uses IPS2APIAccessor;
    uses IBotModule;
    uses IBotConfigurator;

    provides IBotModule with OutfitModule, NormalModule;
    provides IBotConfigurator with PS2BotConfigurator;

    opens nl.elec332.bot.discord.ps2outfits.modules.outfit to com.google.gson;

}