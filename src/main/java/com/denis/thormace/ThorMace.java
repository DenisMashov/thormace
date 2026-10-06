package com.denis.thormace;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ThorMace extends JavaPlugin {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private NamespacedKey key;
    private final Set<BlockBlast> activeBlasts = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        key = new NamespacedKey(this, "thor_mace");

        ThorCommand command = new ThorCommand(this);
        getCommand("thormace").setExecutor(command);
        getCommand("thormace").setTabCompleter(command);

        Bukkit.getPluginManager().registerEvents(new ThorListener(this), this);
    }

    @Override
    public void onDisable() {
        for (BlockBlast blast : new ArrayList<>(activeBlasts)) {
            blast.finish();
        }
    }

    public Set<BlockBlast> blasts() {
        return activeBlasts;
    }

    public Component color(String text) {
        return LEGACY.deserialize(text);
    }

    public Component msg(String path) {
        return LEGACY.deserialize(getConfig().getString("messages." + path, path));
    }

    public Component msg(String path, String placeholder, String value) {
        return LEGACY.deserialize(getConfig().getString("messages." + path, path).replace(placeholder, value));
    }

    public ItemStack createMace() {
        ItemStack item = new ItemStack(Material.MACE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(LEGACY.deserialize(getConfig().getString("item.name", "&b&lMjölnir"))
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        for (String line : getConfig().getStringList("item.lore")) {
            lore.add(LEGACY.deserialize(line).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);

        meta.setUnbreakable(true);
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    public boolean isThorMace(ItemStack item) {
        if (item == null || item.getType() != Material.MACE || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
}
