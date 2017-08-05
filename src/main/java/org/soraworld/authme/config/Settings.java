package org.soraworld.authme.config;

import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.soraworld.authme.Authme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Settings {

    private final ConfigurationLoader<CommentedConfigurationNode> configManager;
    private final Path defaultConfigFile;

    private final Authme plugin = Authme.getInstance();

    private ObjectMapper<Config>.BoundInstance configMapper;
    private ObjectMapper<TextConfig>.BoundInstance textMapper;

    public Settings(ConfigurationLoader<CommentedConfigurationNode> configManager, Path defaultConfigFile) {
        this.configManager = configManager;
        this.defaultConfigFile = defaultConfigFile;

        try {
            configMapper = ObjectMapper.forClass(Config.class).bindToNew();
            textMapper = ObjectMapper.forClass(TextConfig.class).bindToNew();
        } catch (ObjectMappingException objMappingExc) {
            plugin.getLogger().error("Invalid plugin structure", objMappingExc);
        }
    }

    public void load() {
        if (Files.notExists(defaultConfigFile)) {
            try {
                if (Files.notExists(defaultConfigFile.getParent())) {
                    Files.createDirectory(defaultConfigFile.getParent());
                }

                Files.createFile(defaultConfigFile);
            } catch (IOException ioExc) {
                plugin.getLogger().error("Error creating a new config file", ioExc);
                return;
            }
        }

        loadMapper(configMapper, configManager);

        Path textFile = getConfigDir().resolve("messages.conf");
        HoconConfigurationLoader textLoader = HoconConfigurationLoader.builder().setPath(textFile).build();
        loadMapper(textMapper, textLoader);
    }

    public void loadMapper(ObjectMapper<?>.BoundInstance mapper
            , ConfigurationLoader<CommentedConfigurationNode> loader) {
        CommentedConfigurationNode rootNode = loader.createEmptyNode();
        if (mapper != null) {
            try {
                rootNode = loader.load();

                //load the config into the object
                mapper.populate(rootNode);

                //add missing default values
                mapper.serialize(rootNode);
                loader.save(rootNode);
            } catch (ObjectMappingException objMappingExc) {
                plugin.getLogger().error("Error loading the configuration", objMappingExc);
            } catch (IOException ioExc) {
                plugin.getLogger().error("Error saving the default configuration", ioExc);
            }
        }
    }

    public Config getConfig() {
        if (configMapper == null) {
            return null;
        }

        return configMapper.getInstance();
    }

    public TextConfig getTextConfig() {
        if (textMapper == null) {
            return null;
        }

        return textMapper.getInstance();
    }

    public Path getConfigDir() {
        return defaultConfigFile.getParent();
    }
}
