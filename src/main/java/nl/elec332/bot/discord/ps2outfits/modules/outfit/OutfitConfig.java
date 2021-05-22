package nl.elec332.bot.discord.ps2outfits.modules.outfit;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import nl.elec332.bot.discord.ps2outfits.api.util.ISimpleConfig;
import nl.elec332.bot.discord.ps2outfits.modules.PS2BotConfigurator;
import nl.elec332.planetside2.api.objects.player.IOutfit;
import nl.elec332.planetside2.api.objects.registry.IPS2ObjectReference;
import nl.elec332.planetside2.util.NetworkUtil;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by Elec332 on 22/05/2021
 */
public class OutfitConfig implements ISimpleConfig {

    public OutfitConfig() {
        this.outfit = PS2BotConfigurator.API.getOutfitManager().getReference(0);
    }

    private IPS2ObjectReference<? extends IOutfit> outfit;

    public void setOutfit(IOutfit outfit) {
        this.outfit = PS2BotConfigurator.API.getOutfitManager().getReference(outfit.getId());
    }

    public IOutfit getOutfit() {
        return this.outfit.getObject();
    }

    @Override
    public void serialize(ObjectOutputStream oos) throws IOException {
        JsonObject jo = new JsonObject();
        jo.add("oid", new JsonPrimitive(this.outfit.getId()));

        oos.writeUTF(NetworkUtil.GSON.toJson(jo));
    }

    @Override
    public void deserialize(ObjectInputStream ois, int version) throws IOException {
        JsonObject jo = NetworkUtil.GSON.fromJson(ois.readUTF(), JsonObject.class);
        this.outfit = PS2BotConfigurator.API.getOutfitManager().getReference(jo.get("oid").getAsLong());
    }

}
