package nl.elec332.bot.discord.ps2outfits.api.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by Elec332 on 22/05/2021
 */
public interface ISimpleConfig {

    void serialize(ObjectOutputStream oos) throws IOException;

    void deserialize(ObjectInputStream ois, int version) throws IOException;

}
