package nl.elec332.bot.discord.ps2outfits.api.util;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import nl.elec332.bot.discord.ps2outfits.api.ICommand;
import nl.elec332.bot.discord.ps2outfits.api.IConfigurableBotModule;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Elec332 on 22/05/2021
 */
public abstract class AbstractConfigurableBotModule<C> implements IConfigurableBotModule<C> {

    public AbstractConfigurableBotModule(String moduleName) {
        this.moduleName = moduleName;
        this.config = new HashMap<>();
    }

    protected final String moduleName;
    protected final Map<Long, C> config;
    private Runnable saveSettings;

    @Override
    public final void initialize(Runnable saveSettings) {
        this.saveSettings = saveSettings;
        initialize();
    }

    protected void initialize() {
    }

    protected void saveSettingsFile() {
        this.saveSettings.run();
    }

    @Override
    public final String getModuleName() {
        return this.moduleName;
    }

    @Override
    public boolean canRunCommand(TextChannel channel, Member member, C config, ICommand<C> command) {
        return true;
    }

    protected abstract C createConfig(long sid);

    @Override
    public final C getConfigFor(long serverId) {
        return this.config.computeIfAbsent(serverId, this::createConfig);
    }

    @Override
    public final void serialize(ObjectOutputStream oos) throws IOException {
        int size = config.size();
        oos.writeInt(size);
        int i = 0;
        for (Map.Entry<Long, C> e : config.entrySet()) {
            oos.writeLong(e.getKey());
            serializeConfig(e.getValue(), oos);
            i++;
            if (i > size) {
                throw new RuntimeException();
            }
        }
        serializeModule(oos);
    }

    protected abstract void serializeConfig(C cfg, ObjectOutputStream oos) throws IOException;

    protected void serializeModule(ObjectOutputStream oos) throws IOException {
    }

    @Override
    public final void deserialize(ObjectInputStream ois, int version) throws IOException {
        int size = ois.readInt();
        config.clear();
        for (int i = 0; i < size; i++) {
            long l = ois.readLong();
            config.put(l, deserializeConfig(ois, version, l));
        }
        deserializeModule(ois, version);
    }

    protected abstract C deserializeConfig(ObjectInputStream ois, int version, long sid) throws IOException;

    protected void deserializeModule(ObjectInputStream ois, int version) throws IOException {
    }

}