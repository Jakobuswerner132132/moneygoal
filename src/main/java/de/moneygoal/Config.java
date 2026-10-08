package de.moneygoal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Einstellungen, gespeichert in .minecraft/config/moneygoal.json */
public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Config instance = new Config();

    public long goal = 0;
    public long current = 0;
    public boolean visible = true;
    public int x = 6;
    public int y = 6;
    /** Optional: Regex mit einer Gruppe, die den Kontostand aus Chatnachrichten liest. Leer = aus. */
    public String balanceRegex = "";

    public static Config get() {
        return instance;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("moneygoal.json");
    }

    public static void load() {
        Path file = file();
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                Config loaded = GSON.fromJson(reader, Config.class);
                if (loaded != null) {
                    instance = loaded;
                }
            } catch (Exception e) {
                MoneyGoalClient.LOGGER.warn("Konnte moneygoal.json nicht lesen, nutze Standardwerte", e);
            }
        }
        if (instance.balanceRegex == null) {
            instance.balanceRegex = "";
        }
        save();
    }

    public static void save() {
        try (Writer writer = Files.newBufferedWriter(file())) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            MoneyGoalClient.LOGGER.warn("Konnte moneygoal.json nicht speichern", e);
        }
    }
}
