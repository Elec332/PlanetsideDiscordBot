package nl.elec332.bot.discord.ps2outfits.modules;

import com.google.gson.Gson;
import nl.elec332.bot.discord.ps2outfits.api.util.AbstractConfigurableBotModule;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.function.LongConsumer;

/**
 * Created by Elec332 on 22/05/2021
 */
public abstract class AbstractGSONModule<C extends Serializable> extends AbstractConfigurableBotModule<C> {

    public AbstractGSONModule(String moduleName) {
        super(moduleName);
        this.gson = createGson();
    }

    private final Gson gson;

    protected abstract Gson createGson();

    protected abstract Class<C> getConfigType();

    @Override
    protected final C createConfig(long sid) {
        C ret;
        try {
            ret = getConfigType().getConstructor(long.class).newInstance(sid);
        } catch (Exception e) {
            try {
                ret = getConfigType().getConstructor().newInstance();
            } catch (Exception e1) {
                throw new RuntimeException(e1);
            }
        }
        if (ret instanceof LongConsumer) {
            ((LongConsumer) ret).accept(sid);
        }
        return ret;
    }

    @Override
    protected final void serializeConfig(C cfg, ObjectOutputStream oos) throws IOException {
        oos.writeUTF(gson.toJson(cfg));
    }

    @Override
    protected final C deserializeConfig(ObjectInputStream ois, int version, long sid) throws IOException {
        C ret = gson.fromJson(ois.readUTF(), getConfigType());
        if (ret instanceof LongConsumer) {
            ((LongConsumer) ret).accept(sid);
        }
        return ret;
    }

}
