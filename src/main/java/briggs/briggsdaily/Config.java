package briggs.briggsdaily;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class Config {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("briggsdaily/rewards.properties");
    private static final Properties props = new Properties();

    public static void load() {
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            props.load(reader);
        } catch (IOException e) {
            setDefaults();
            save();
        }
    }

    private static void setDefaults() {
        props.setProperty("minimumPlaytime", "5");
    }

    public static void setMinimumPlaytime(int n) {
        props.setProperty("minimumPlaytime", String.valueOf(n));
        save();
    }

    public static int minimumPlaytime() {
        return Integer.parseInt(props.getProperty("minimumPlaytime", "5"));
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                props.store(writer, "BriggsDaily Mod Configuration");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}