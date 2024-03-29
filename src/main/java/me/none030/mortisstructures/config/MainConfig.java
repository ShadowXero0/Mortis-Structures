package me.none030.mortisstructures.config;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import me.none030.mortisstructures.data.DataManager;
import me.none030.mortisstructures.data.H2Database;
import me.none030.mortisstructures.structure.*;
import me.none030.mortisstructures.structure.mob.Mob;
import me.none030.mortisstructures.structure.mob.StructureMob;
import me.none030.mortisstructures.structure.mob.StructureMythicMob;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainConfig extends Config {

    public MainConfig(ConfigManager configManager) {
        super("config.yml", configManager);
    }

    @Override
    public void loadConfig() {
        File file = saveConfig();
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        DataManager dataManager = loadDatabase(config.getConfigurationSection("database"));
        if (dataManager == null) {
            return;
        }
        getConfigManager().getMainManager().setStructureManager(new StructureManager(dataManager));
        loadSchematics(config.getConfigurationSection("schematics"));
    }

    private DataManager loadDatabase(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String fileName = section.getString("file");
        if (fileName == null) {
            return null;
        }
        File file = new File(getPlugin().getDataFolder(), fileName);
        String username = section.getString("username");
        String password = section.getString("password");
        return new DataManager(new H2Database(file, username, password));
    }

    private void loadSchematics(ConfigurationSection schematics) {
        if (schematics == null) {
            return;
        }
        for (String key : schematics.getKeys(false)) {
            ConfigurationSection structure = schematics.getConfigurationSection(key);
            if (structure == null) {
                continue;
            }
            StructureWorld world = new StructureWorld(structure.getString("world"));
            int interval = structure.getInt("interval");
            int despawn = structure.getInt("despawn");
            int tries = structure.getInt("tries");
            int spawns = structure.getInt("spawn");
            List<String> commandsOnSpawn = structure.getStringList("command-on-spawn");
            List<String> commandsOnDeSpawn = structure.getStringList("command-on-despawn");
            boolean unbreakable = structure.getBoolean("unbreakable");
            Material replacement = Material.valueOf(structure.getString("replacement"));
            List<Mob> mobs = new ArrayList<>();
            if (structure.contains("mobs")) {
                for (String line : structure.getStringList("mobs")) {
                    String[] raw = line.split(":");
                    EntityType type = EntityType.valueOf(raw[0]);
                    StructureMob mob = new StructureMob(type, Integer.parseInt(raw[1]));
                    mobs.add(mob);
                }
            }
            if (structure.contains("mythic-mobs")) {
                for (String line : structure.getStringList("mythic-mobs")) {
                    String[] raw = line.split(":");
                    StructureMythicMob mob = new StructureMythicMob(raw[0], Integer.parseInt(raw[1]));
                    mobs.add(mob);
                }
            }
            if (mobs.size() == 0) {
                mobs = null;
            }
            StructureType type = StructureType.valueOf(structure.getString("type"));
            String[] location1 = Objects.requireNonNull(structure.getString("location1")).split(",");
            String[] location2 = Objects.requireNonNull(structure.getString("location2")).split(",");
            StructureLocation loc1 = new StructureLocation(world.getWorldName(), Double.parseDouble(location1[0]), Double.parseDouble(location1[1]), Double.parseDouble(location1[2]));
            StructureLocation loc2 = new StructureLocation(world.getWorldName(), Double.parseDouble(location2[0]), Double.parseDouble(location2[1]), Double.parseDouble(location2[2]));
            String schematicName = structure.getString("schematic");
            if (schematicName == null) {
                continue;
            }
            File schematic = new File(getPlugin().getDataFolder() + "/schematics/", schematicName);
            if (!schematic.exists()) {
                continue;
            }
            ClipboardFormat format = ClipboardFormats.findByFile(schematic);
            if (format == null) {
                continue;
            }
            Clipboard clipboard;
            try {
                ClipboardReader reader = format.getReader(Files.newInputStream(schematic.toPath()));
                clipboard = reader.read();
            } catch (IOException exp) {
                exp.printStackTrace();
                continue;
            }
            ConfigurationSection checks = structure.getConfigurationSection("checks");
            if (checks == null) {
                continue;
            }
            boolean town = checks.getBoolean("town");
            int townRange = checks.getInt("town-range");
            boolean water = checks.getBoolean("water");
            boolean lava = checks.getBoolean("lava");
            List<Material> mustHaveBlocks = null;
            if (checks.contains("must-have-blocks")) {
                mustHaveBlocks = new ArrayList<>();
                for (String line : checks.getStringList("must-have-blocks")) {
                    Material material = Material.valueOf(line);
                    mustHaveBlocks.add(material);
                }
            }
            List<Material> mustNotHaveBlocks = null;
            if (checks.contains("must-not-have-blocks")) {
                mustNotHaveBlocks = new ArrayList<>();
                for (String line : checks.getStringList("must-not-have-blocks")) {
                    Material material = Material.valueOf(line);
                    mustNotHaveBlocks.add(material);
                }
            }
            List<Biome> mustHaveBiomes = null;
            if (checks.contains("must-have-biomes")) {
                mustHaveBiomes = new ArrayList<>();
                for (String line : checks.getStringList("must-have-biomes")) {
                    Biome biome = Biome.valueOf(line);
                    mustHaveBiomes.add(biome);
                }
            }
            List<Biome> mustNotHaveBiomes = null;
            if (checks.contains("must-not-have-biomes")) {
                mustNotHaveBiomes = new ArrayList<>();
                for (String line : checks.getStringList("must-not-have-biomes")) {
                    Biome biome = Biome.valueOf(line);
                    mustNotHaveBiomes.add(biome);
                }
            }
            StructureChecks check = new StructureChecks(town, townRange, water, lava, mustHaveBlocks, mustNotHaveBlocks, mustHaveBiomes, mustNotHaveBiomes);
            Structure struc = new Structure(key, clipboard, type, world, loc1, loc2, unbreakable, spawns, interval, despawn, tries, replacement, check, mobs, commandsOnSpawn, commandsOnDeSpawn);
            getConfigManager().getMainManager().getStructureManager().getStructures().add(struc);
            getConfigManager().getMainManager().getStructureManager().getStructureById().put(key, struc);
        }
    }
}
