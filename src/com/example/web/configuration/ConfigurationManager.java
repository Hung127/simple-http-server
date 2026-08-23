package com.example.web.configuration;

import java.io.IOException;
import com.example.json.Json;
import java.nio.file.Files;
import java.nio.file.Path;

// singleton
public class ConfigurationManager {
    private static ConfigurationManager instance;

    private static Configuration currentConfiguration;

    private ConfigurationManager() {
    }

    public static ConfigurationManager getInstance() {
        if (ConfigurationManager.instance == null) {
            ConfigurationManager.instance = new ConfigurationManager();
        }
        return ConfigurationManager.instance;
    }

    public void loadConfigFile(String filePath) {
        String configString = null;

        try {
            configString = Files.readString(Path.of(filePath));
        } catch (IOException e) {
            throw new HTTPConfigurationException("Error when reading config file", e);
        }

        try {
            ConfigurationManager.currentConfiguration = Json.fromString(configString, Configuration.class);
        } catch (IOException e) {
            throw new HTTPConfigurationException(
                    "Error when converting JSON string to object", e);
        }
    }

    public Configuration getCurrentConfiguration() {
        if (ConfigurationManager.currentConfiguration == null) {
            throw new HTTPConfigurationException("No current configuration set");
        }
        return ConfigurationManager.currentConfiguration;
    }
}
