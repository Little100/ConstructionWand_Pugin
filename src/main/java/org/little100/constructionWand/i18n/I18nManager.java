package org.little100.constructionWand.i18n;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class I18nManager {

    private final Plugin plugin;
    private final Map<String, String> messages = new HashMap<>();
    private String currentLanguage = "zh_CN";

    public static final String WAND_STONE_NAME = "wand.stone.name";
    public static final String WAND_IRON_NAME = "wand.iron.name";
    public static final String WAND_DIAMOND_NAME = "wand.diamond.name";
    public static final String WAND_NETHERITE_NAME = "wand.netherite.name";
    public static final String WAND_INFINITY_NAME = "wand.infinity.name";

    public static final String WAND_LORE_MAX_BLOCKS = "wand.lore.max-blocks";
    public static final String WAND_LORE_DURABILITY = "wand.lore.durability";
    public static final String WAND_LORE_DURABILITY_INFINITE = "wand.lore.durability-infinite";
    public static final String WAND_LORE_USAGE_1 = "wand.lore.usage-1";
    public static final String WAND_LORE_USAGE_2 = "wand.lore.usage-2";

    public static final String MSG_PLACE_SUCCESS = "message.place-success";
    public static final String MSG_NO_PLACE = "message.no-place";
    public static final String MSG_NO_PERMISSION = "message.no-permission";
    public static final String MSG_WAND_GIVEN = "message.wand-given";
    public static final String MSG_WAND_RECEIVED = "message.wand-received";
    public static final String MSG_CONFIG_RELOADED = "message.config-reloaded";
    public static final String MSG_PLAYER_NOT_FOUND = "message.player-not-found";
    public static final String MSG_UNKNOWN_WAND_TYPE = "message.unknown-wand-type";
    public static final String MSG_CONSOLE_NEED_PLAYER = "message.console-need-player";
    public static final String MSG_UNKNOWN_COMMAND = "message.unknown-command";

    public static final String CMD_HELP_TITLE = "command.help.title";
    public static final String CMD_HELP_GIVE = "command.help.give";
    public static final String CMD_HELP_LIST = "command.help.list";
    public static final String CMD_HELP_RELOAD = "command.help.reload";
    public static final String CMD_HELP_HELP = "command.help.help";
    public static final String CMD_HELP_USAGE = "command.help.usage";

    public static final String CMD_LIST_TITLE = "command.list.title";
    public static final String CMD_LIST_MAX = "command.list.max";
    public static final String CMD_LIST_DURABILITY = "command.list.durability";
    public static final String CMD_LIST_INFINITE = "command.list.infinite";

    public I18nManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void loadLanguage(String language) {
        this.currentLanguage = language;
        messages.clear();

        loadLanguageFile("zh_CN", true);

        if (!language.equals("zh_CN")) {
            loadLanguageFile(language, false);
        }

        plugin.getLogger().info("已加载语言: " + language);
    }

    private void loadLanguageFile(String language, boolean isDefault) {
        String fileName = "lang/" + language + ".yml";

        File langFile = new File(plugin.getDataFolder(), fileName);
        if (!langFile.exists()) {
            plugin.saveResource(fileName, false);
        }

        FileConfiguration langConfig = YamlConfiguration.loadConfiguration(langFile);

        InputStream defaultStream = plugin.getResource(fileName);
        if (defaultStream != null) {
            FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            langConfig.setDefaults(defaultConfig);
        }

        for (String key : langConfig.getKeys(true)) {
            if (langConfig.isString(key)) {
                String value = langConfig.getString(key);
                if (value != null) {

                    if (isDefault || !messages.containsKey(key)) {
                        messages.put(key, value);
                    }

                    if (!isDefault) {
                        messages.put(key, value);
                    }
                }
            }
        }
    }

    public String get(String key) {
        String message = messages.getOrDefault(key, key);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String get(String key, Object... args) {
        String message = get(key);

        for (int i = 0; i < args.length; i++) {
            message = message.replace("{" + i + "}", String.valueOf(args[i]));
        }

        if (args.length >= 2) {
            for (int i = 0; i < args.length - 1; i += 2) {
                if (args[i] instanceof String) {
                    message = message.replace("{" + args[i] + "}", String.valueOf(args[i + 1]));
                }
            }
        }

        return message;
    }

    public String getRaw(String key) {
        return messages.getOrDefault(key, key);
    }

    public boolean has(String key) {
        return messages.containsKey(key);
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    public void reload() {
        loadLanguage(currentLanguage);
    }
}