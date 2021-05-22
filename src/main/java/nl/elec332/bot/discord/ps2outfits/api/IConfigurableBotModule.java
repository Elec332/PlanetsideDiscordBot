package nl.elec332.bot.discord.ps2outfits.api;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by Elec332 on 22/05/2021
 */
public interface IConfigurableBotModule<C> extends IBotModule<C> {

    void initialize(Runnable saveSettings);

    default int getFileVersion() {
        return 0;
    }

    void serialize(ObjectOutputStream oos) throws IOException;

    void deserialize(ObjectInputStream ois, int version) throws IOException;

}
