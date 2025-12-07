package org.little100.constructionWand.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.little100.constructionWand.ConstructionWand;
import org.little100.constructionWand.enchant.EnchantmentManager;
import org.little100.constructionWand.enchant.WandEnchantment;
import org.little100.constructionWand.gui.SettingsGUI;
import org.little100.constructionWand.gui.WandGUI;
import org.little100.constructionWand.i18n.I18nManager;
import org.little100.constructionWand.preview.PreviewManager;
import org.little100.constructionWand.utils.VersionHelper;
import org.little100.constructionWand.wand.WandItemManager;
import org.little100.constructionWand.wand.WandType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class WandCommand implements CommandExecutor, TabCompleter {

    private final ConstructionWand plugin;
    private final WandItemManager wandItemManager;
    private final I18nManager i18n;

    public WandCommand(ConstructionWand plugin, WandItemManager wandItemManager, I18nManager i18n) {
        this.plugin = plugin;
        this.wandItemManager = wandItemManager;
        this.i18n = i18n;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "give":
                return handleGive(sender, args);
            case "reload":
                return handleReload(sender);
            case "help":
                sendHelp(sender);
                return true;
            case "list":
                return handleList(sender);
            case "itemgui":
                return handleItemGUI(sender);
            case "gui":
            case "settings":
                return handleSettingsGUI(sender);
            case "enchant":
                return handleEnchant(sender, args);
            case "nbtdebug":
                return handleNbtDebug(sender);
            case "preview":
                return handlePreview(sender);
            case "lang":
            case "language":
                return handleLang(sender, args);
            default:
                sender.sendMessage(i18n.get("message.unknown-command", subCommand));
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("constructionwand.give")) {
            sender.sendMessage(i18n.get("message.no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(i18n.get("command.usage.give"));
            sender.sendMessage(i18n.get("message.available-types"));
            return true;
        }

        String typeId = args[1].toLowerCase();
        WandType wandType = WandType.fromId(typeId);

        if (wandType == null) {
            sender.sendMessage(i18n.get("message.unknown-wand-type", typeId));
            sender.sendMessage(i18n.get("message.available-types"));
            return true;
        }

        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(i18n.get("message.player-not-found", args[2]));
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(i18n.get("message.console-need-player"));
            return true;
        }

        ItemStack wand = wandItemManager.createWand(wandType);
        target.getInventory().addItem(wand);

        String wandName = i18n.get("wand." + wandType.getId() + ".name");
        sender.sendMessage(i18n.get("message.wand-given", target.getName(), wandName));
        if (target != sender) {
            target.sendMessage(i18n.get("message.wand-received", wandName));
        }

        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("constructionwand.reload")) {
            sender.sendMessage(i18n.get("message.no-permission"));
            return true;
        }

        plugin.reloadConfig();
        plugin.loadConfiguration();

        sender.sendMessage(i18n.get("message.config-reloaded"));
        return true;
    }

    private boolean handleItemGUI(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(i18n.get("message.console-need-player"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("constructionwand.itemgui")) {
            player.sendMessage(i18n.get("message.no-permission"));
            return true;
        }

        WandGUI gui = new WandGUI(wandItemManager, i18n);

        Bukkit.getPluginManager().registerEvents(gui, plugin);
        gui.openInventory(player);

        return true;
    }

    private boolean handleEnchant(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(i18n.get("message.console-need-player"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("constructionwand.enchant")) {
            player.sendMessage(i18n.get("message.no-permission"));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(i18n.get("command.usage.enchant"));
            player.sendMessage(i18n.get("message.available-enchants"));
            return true;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (!wandItemManager.isWand(itemInHand)) {
            player.sendMessage(i18n.get("message.not-holding-wand"));
            return true;
        }

        String enchantName = args[1].toLowerCase();
        WandEnchantment enchantment = WandEnchantment.fromId(enchantName);
        if (enchantment == null) {
            player.sendMessage(i18n.get("message.unknown-enchant", enchantName));
            player.sendMessage(i18n.get("message.available-enchants"));
            return true;
        }

        int level;
        try {
            level = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(i18n.get("message.invalid-level", args[2]));
            return true;
        }

        if (level <= 0) {

            EnchantmentManager enchantManager = plugin.getEnchantmentManager();
            if (enchantManager.removeEnchantment(itemInHand, enchantment)) {
                player.sendMessage(i18n.get("message.enchant-removed",
                        i18n.get("enchant." + enchantment.getId() + ".name")));
            } else {
                player.sendMessage(i18n.get("message.enchant-not-found"));
            }
            return true;
        }

        EnchantmentManager enchantManager = plugin.getEnchantmentManager();
        if (enchantManager.addEnchantment(itemInHand, enchantment, level)) {
            String enchantDisplayName = i18n.get("enchant." + enchantment.getId() + ".name");
            String levelStr = WandEnchantment.toRomanNumeral(level);
            player.sendMessage(i18n.get("message.enchant-added", enchantDisplayName, levelStr));
        } else {
            player.sendMessage(i18n.get("message.enchant-failed"));
        }

        return true;
    }

    private boolean handleNbtDebug(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(i18n.get("message.console-need-player"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("constructionwand.nbtdebug")) {
            player.sendMessage(i18n.get("message.no-permission"));
            return true;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand == null || itemInHand.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "手中没有物品！");
            return true;
        }

        player.sendMessage(ChatColor.GOLD + "=== NBT Debug 信息 ===");
        player.sendMessage(ChatColor.YELLOW + "物品类型: " + ChatColor.WHITE + itemInHand.getType().name());
        player.sendMessage(ChatColor.YELLOW + "数量: " + ChatColor.WHITE + itemInHand.getAmount());

        ItemMeta meta = itemInHand.getItemMeta();
        if (meta != null) {

            int oldCustomModelData = VersionHelper.getCustomModelData(meta);
            player.sendMessage(ChatColor.YELLOW + "CustomModelData (旧版整数): " + ChatColor.WHITE + oldCustomModelData);

            int newCustomModelData = VersionHelper.getCustomModelDataFromItem(itemInHand);
            player.sendMessage(ChatColor.YELLOW + "CustomModelData (新版组件): " + ChatColor.WHITE + newCustomModelData);

            java.util.List<String> cmdStrings = VersionHelper.getCustomModelDataStrings(itemInHand);
            if (!cmdStrings.isEmpty()) {
                player.sendMessage(ChatColor.YELLOW + "CustomModelData Strings: " + ChatColor.WHITE + cmdStrings);
            }

            if (meta.hasDisplayName()) {
                player.sendMessage(ChatColor.YELLOW + "显示名称: " + ChatColor.WHITE + meta.getDisplayName());
            }

            boolean isWand = wandItemManager.isWand(itemInHand);
            player.sendMessage(ChatColor.YELLOW + "是建筑之杖: " + ChatColor.WHITE + (isWand ? "是" : "否"));

            if (isWand) {
                WandType wandType = wandItemManager.getWandType(itemInHand);
                if (wandType != null) {
                    player.sendMessage(ChatColor.YELLOW + "手杖类型: " + ChatColor.WHITE + wandType.getId());
                    player.sendMessage(ChatColor.YELLOW + "最大方块数: " + ChatColor.WHITE + wandType.getMaxBlocks());
                    player.sendMessage(
                            ChatColor.YELLOW + "期望CustomModelData: " + ChatColor.WHITE + wandType.getCustomModelData());
                }

                EnchantmentManager enchantManager = plugin.getEnchantmentManager();
                for (WandEnchantment enchant : WandEnchantment.values()) {
                    int level = enchantManager.getEnchantmentLevel(itemInHand, enchant);
                    if (level > 0) {
                        player.sendMessage(
                                ChatColor.YELLOW + "附魔 " + enchant.getId() + ": " + ChatColor.WHITE + "等级 " + level);
                    }
                }
            }

            if (meta.hasLore()) {
                player.sendMessage(ChatColor.YELLOW + "Lore:");
                for (String line : meta.getLore()) {
                    player.sendMessage(ChatColor.GRAY + "  " + line);
                }
            }
        }

        player.sendMessage(ChatColor.GOLD + "======================");
        return true;
    }

    private boolean handleSettingsGUI(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(i18n.get("message.console-need-player"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("constructionwand.settings")) {
            player.sendMessage(i18n.get("message.no-permission"));
            return true;
        }

        PreviewManager previewManager = plugin.getPreviewManager();
        SettingsGUI gui = new SettingsGUI(previewManager, i18n, player);

        Bukkit.getPluginManager().registerEvents(gui, plugin);
        gui.openInventory(player);

        return true;
    }

    private boolean handlePreview(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(i18n.get("message.console-need-player"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("constructionwand.preview")) {
            player.sendMessage(i18n.get("message.no-permission"));
            return true;
        }

        PreviewManager previewManager = plugin.getPreviewManager();
        PreviewManager.PreviewMode newMode = previewManager.togglePlayerPreviewMode(player);

        String modeName;
        switch (newMode) {
            case FULL:
                modeName = i18n.get("preview.mode.full");
                break;
            case CORNERS:
                modeName = i18n.get("preview.mode.corners");
                break;
            case OUTLINE:
                modeName = i18n.get("preview.mode.outline");
                break;
            default:
                modeName = newMode.name();
        }

        player.sendMessage(i18n.get("message.preview-mode-changed", modeName));
        return true;
    }

    private boolean handleLang(CommandSender sender, String[] args) {
        if (!sender.hasPermission("constructionwand.lang")) {
            sender.sendMessage(i18n.get("message.no-permission"));
            return true;
        }

        List<String> availableLanguages = Arrays.asList("zh_CN", "zh_TW", "en_US", "lzh");

        if (args.length < 2) {

            sender.sendMessage(i18n.get("message.current-language", i18n.getCurrentLanguage()));
            sender.sendMessage(i18n.get("message.available-languages", String.join(", ", availableLanguages)));
            return true;
        }

        String newLang = args[1];

        if (!availableLanguages.contains(newLang)) {
            sender.sendMessage(i18n.get("message.unknown-language", newLang));
            sender.sendMessage(i18n.get("message.available-languages", String.join(", ", availableLanguages)));
            return true;
        }

        i18n.loadLanguage(newLang);

        plugin.getConfig().set("language", newLang);
        plugin.saveConfig();

        sender.sendMessage(i18n.get("message.language-changed", newLang));

        if (sender instanceof Player) {
            Player player = (Player) sender;
            int updated = wandItemManager.updateAllWandsInInventory(player);
            if (updated > 0) {
                sender.sendMessage(i18n.get("message.wands-updated", updated));
            }
        }

        return true;
    }

    private boolean handleList(CommandSender sender) {
        sender.sendMessage(i18n.get("command.list.title"));

        for (WandType type : WandType.values()) {
            StringBuilder sb = new StringBuilder();
            sb.append(ChatColor.YELLOW).append(type.getId());
            sb.append(ChatColor.WHITE).append(" - ");
            sb.append(i18n.get("wand." + type.getId() + ".name"));
            sb.append(ChatColor.GRAY).append(" (").append(i18n.get("command.list.max", type.getMaxBlocks()));
            if (type.isUnbreakable()) {
                sb.append(", ").append(i18n.get("command.list.infinite"));
            } else {
                sb.append(", ").append(i18n.get("command.list.durability", type.getMaxDurability()));
            }
            sb.append(ChatColor.GRAY).append(")");

            sender.sendMessage(sb.toString());
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(i18n.get("command.help.title"));
        sender.sendMessage(i18n.get("command.help.give"));
        sender.sendMessage(i18n.get("command.help.itemgui"));
        sender.sendMessage(i18n.get("command.help.gui"));
        sender.sendMessage(i18n.get("command.help.enchant"));
        sender.sendMessage(i18n.get("command.help.nbtdebug"));
        sender.sendMessage(i18n.get("command.help.preview"));
        sender.sendMessage(i18n.get("command.help.lang"));
        sender.sendMessage(i18n.get("command.help.list"));
        sender.sendMessage(i18n.get("command.help.reload"));
        sender.sendMessage(i18n.get("command.help.help"));
        sender.sendMessage("");
        sender.sendMessage(i18n.get("command.help.usage"));
        sender.sendMessage(i18n.get("command.help.usage-detail"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("give", "itemgui", "gui", "enchant", "nbtdebug", "preview", "lang",
                    "list", "reload", "help");
            String input = args[0].toLowerCase();
            completions = subCommands.stream()
                    .filter(s -> s.startsWith(input))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            String input = args[1].toLowerCase();
            completions = Arrays.stream(WandType.values())
                    .map(WandType::getId)
                    .filter(s -> s.startsWith(input))
                    .collect(Collectors.toList());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            String input = args[2].toLowerCase();
            completions = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("enchant")) {
            String input = args[1].toLowerCase();
            completions = Arrays.stream(WandEnchantment.values())
                    .map(WandEnchantment::getId)
                    .filter(s -> s.startsWith(input))
                    .collect(Collectors.toList());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("enchant")) {
            completions = Arrays.asList("1", "2", "3", "4", "5");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("lang") || args[0].equalsIgnoreCase("language"))) {
            String input = args[1].toLowerCase();
            completions = Arrays.asList("zh_CN", "zh_TW", "en_US", "lzh").stream()
                    .filter(s -> s.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }

        return completions;
    }
}