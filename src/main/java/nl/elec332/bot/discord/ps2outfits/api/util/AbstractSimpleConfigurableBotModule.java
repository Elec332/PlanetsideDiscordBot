package nl.elec332.bot.discord.ps2outfits.api.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by Elec332 on 22/05/2021
 */
public abstract class AbstractSimpleConfigurableBotModule<C extends ISimpleConfig> extends AbstractConfigurableBotModule<C> {

    public AbstractSimpleConfigurableBotModule(String moduleName) {
        super(moduleName);
    }

    @Override
    protected final void serializeConfig(C cfg, ObjectOutputStream oos) throws IOException {
        cfg.serialize(oos);
    }


    @Override
    protected final C deserializeConfig(ObjectInputStream ois, int version, long sid) throws IOException {
        C ret = createConfig(sid);
        ret.deserialize(ois, version);
        return ret;
    }

}
