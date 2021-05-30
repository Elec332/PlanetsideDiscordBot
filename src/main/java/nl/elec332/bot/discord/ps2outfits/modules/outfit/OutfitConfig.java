package nl.elec332.bot.discord.ps2outfits.modules.outfit;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import nl.elec332.bot.discord.ps2outfits.modules.PS2BotConfigurator;
import nl.elec332.planetside2.api.objects.player.IOutfit;
import nl.elec332.planetside2.api.objects.registry.IPS2ObjectReference;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Created by Elec332 on 22/05/2021
 */
public class OutfitConfig implements LongConsumer, Serializable {

    public OutfitConfig() {
        this.outfit = PS2BotConfigurator.API.getOutfitManager().getReference(0);
    }

    private transient long serverId;
    private IPS2ObjectReference<? extends IOutfit> outfit;

    public void setOutfit(IOutfit outfit) {
        this.outfit = PS2BotConfigurator.API.getOutfitManager().getReference(Objects.requireNonNull(outfit).getId());
    }

    public IOutfit getOutfit() {
        return this.outfit.getObject();
    }

    public Guild getGuild(JDA jda) {
        return jda.getGuildById(serverId);
    }

    @Override
    public void accept(long value) {
        this.serverId = value;
    }

}
