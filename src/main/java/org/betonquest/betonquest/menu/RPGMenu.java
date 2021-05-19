package org.betonquest.betonquest.menu;

import org.betonquest.betonquest.BetonQuest;
import org.betonquest.betonquest.config.Config;
import org.betonquest.betonquest.config.ConfigPackage;
import org.betonquest.betonquest.exceptions.ObjectNotFoundException;
import org.betonquest.betonquest.menu.betonquest.MenuCondition;
import org.betonquest.betonquest.menu.betonquest.MenuObjective;
import org.betonquest.betonquest.menu.betonquest.MenuQuestEvent;
import org.betonquest.betonquest.menu.betonquest.MenuVariable;
import org.betonquest.betonquest.menu.commands.RPGMenuCommand;
import org.betonquest.betonquest.menu.config.RPGMenuConfig;
import org.betonquest.betonquest.menu.events.MenuOpenEvent;
import org.betonquest.betonquest.menu.utils.Log;
import org.betonquest.betonquest.menu.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created on 12.01.2018
 *
 * @author Jonas Blocher
 */
public class RPGMenu {

    private static RPGMenu instance;
    private RPGMenuConfig config = null;
    private HashMap<MenuID, Menu> menus;
    private RPGMenuCommand pluginCommand;

    public RPGMenu() {
        instance = this;
    }

    /**
     * @return the config of the plugin
     */
    public static RPGMenuConfig getConfiguration() {
        return instance.config;
    }

    /**
     * @return the instance of the plugin
     */
    public static RPGMenu getInstance() {
        return instance;
    }

    /**
     * Open a menu for a player
     *
     * @param player the player for which the menu should be opened
     * @param id     id of the menu
     */
    public static void openMenu(final Player player, final MenuID id) {
        final Menu menu = instance.menus.get(id);
        if (menu == null) {
            Log.error("Could not open menu §7" + id + "§4: §cUnknown menu");
            return;
        }
        final MenuOpenEvent openEvent = new MenuOpenEvent(player, id);
        Bukkit.getPluginManager().callEvent(openEvent);
        if (openEvent.isCancelled()) {
            Log.debug("A Bukkit listener canceled opening of menu " + id + " for " + player.getName());
            return;
        }
        new OpenedMenu(player, menu);
        Log.debug("opening menu " + id + " for " + player.getName());
    }

    /**
     * If the player has a open menu it closes it
     *
     * @param player the player himself
     */
    public static void closeMenu(final Player player) {
        OpenedMenu.closeMenu(player);
    }

    /**
     * Returns if the player has opened the specified menu
     *
     * @param player the player for which should be checked
     * @param id     the id of the menu the player should has opened, null will return true if the player has any menu opened
     * @return true if the player has opened the specified menu, false otherwise
     */
    public static boolean hasOpenedMenu(final Player player, final MenuID id) {
        final OpenedMenu menu = OpenedMenu.getMenu(player);
        if (menu == null) return false;
        if (id == null) return true;
        return menu.getId().equals(id);
    }

    /**
     * Returns if the player has opened any menu
     *
     * @param player guess what: the player!
     * @return true if player has opened a menu, false if not
     */
    public static boolean hasOpenedMenu(final Player player) {
        return RPGMenu.hasOpenedMenu(player, null);
    }

    public void onEnable() {
        //register events, objectives and conditions
        BetonQuest.getInstance().registerConditions("menu", MenuCondition.class);
        BetonQuest.getInstance().registerObjectives("menu", MenuObjective.class);
        BetonQuest.getInstance().registerEvents("menu", MenuQuestEvent.class);
        BetonQuest.getInstance().registerVariable("menu", MenuVariable.class);
        //load the plugin command
        this.pluginCommand = new RPGMenuCommand();
        //create config if it doesn't exist
        final File config = new File(BetonQuest.getInstance().getDataFolder(), "menuConfig.yml");
        if (!config.exists()) {
            try {
                Utils.saveResource(config, "menuConfig.yml");
            } catch (final IOException e) {
                Log.error("Could not create default config: §c" + e.getMessage());
            }
        }
        new ReloadListener();
    }

    public void onDisable() {
        //close all menus
        OpenedMenu.closeAll();
        //disable listeners
        HandlerList.unregisterAll(BetonQuest.getInstance());
        this.pluginCommand.unregister();
    }

    /**
     * Reload all plugin data
     *
     * @return information if the reload was successful
     */
    public ReloadInformation reloadData() {
        //unregister old menus
        if (menus != null) {
            final Iterator<Menu> iterator = this.menus.values().iterator();
            while (iterator.hasNext()) {
                iterator.next().unregister();
                iterator.remove();
            }
        }
        final ReloadInformation info = new ReloadInformation();
        menus = new HashMap<>();
        //load the plugin config
        try {
            this.config = new RPGMenuConfig();
        } catch (final InvalidConfigurationException e) {
            Log.error(e);
            info.addError(e);
            info.result = ReloadResult.FAILED;
            return info;
        }
        //enable debugging if enabled in config
        Log.setDebug(this.config.debug);
        //load files for all packages
        for (final ConfigPackage pack : Config.getPackages().values()) {
            final File menusFolder = new File(pack.getFolder(), "menus");
            if (!menusFolder.exists()) menusFolder.mkdir();
            final File[] files = menusFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files == null) continue;
            for (final File f : files) {
                try {
                    final MenuID id = new MenuID(pack, f.getName().substring(0, f.getName().length() - 4));
                    this.menus.put(id, new Menu(id));
                    info.loaded++;
                } catch (final InvalidConfigurationException e) {
                    Log.error(e);
                    info.addError(e);
                    info.result = ReloadResult.SUCCESS;
                } catch (final ObjectNotFoundException e) {
                    Log.error("Strange unhandled exception during loading: " + e);
                    info.result = ReloadResult.FAILED;
                    return info;
                }
            }
        }
        final ChatColor color = (info.result == ReloadResult.FULL_SUCCESS) ? ChatColor.GREEN : ChatColor.YELLOW;
        Log.info(color + "Reloaded §7" + info.loaded + color + " menus");
        return info;
    }

    /**
     * Reloads only one menu with the given id
     *
     * @param id id of the menu which should be reloaded
     * @return information if the reload was successful
     */
    public ReloadInformation reloadMenu(final MenuID id) {
        //unregister old menu if it exists
        if (this.menus.containsKey(id)) {
            this.menus.get(id).unregister();
            this.menus.remove(id);
        }
        final ReloadInformation info = new ReloadInformation();
        try {
            this.menus.put(id, new Menu(id));
            info.result = ReloadResult.FULL_SUCCESS;
            info.loaded = 1;
            Log.info("§aReloaded menu " + id);
        } catch (final InvalidConfigurationException e) {
            Log.error(e);
            info.result = ReloadResult.FAILED;
            info.addError(e);
        }
        return info;
    }

    /**
     * @return a collection containing all loaded menus
     */
    public Collection<MenuID> getMenus() {
        return menus.keySet();
    }

    /**
     * @param id id of the menu
     * @return menu with the given id
     */
    public Menu getMenu(final MenuID id) {
        return menus.get(id);
    }

    /**
     * Tells whether a reload was successful
     */
    public enum ReloadResult {
        /**
         * If all data could be successfully loaded
         */
        FULL_SUCCESS,
        /**
         * If reload was successful but some menus could not be loaded
         */
        SUCCESS,
        /**
         * If reload completely failed
         */
        FAILED
    }

    /**
     * Class containing all information about a reload
     */
    public static class ReloadInformation {

        private final List<String> errorMessages = new ArrayList<>();
        private int loaded = 0;
        private ReloadResult result = ReloadResult.FULL_SUCCESS;

        private void addError(final Throwable e) {
            errorMessages.add("§4" + e.getMessage());
        }

        /**
         * @return a list containing all errors that where thrown while reloading
         */
        public List<String> getErrorMessages() {
            return errorMessages;
        }

        /**
         * @return amount of menus that were loaded
         */
        public int getLoaded() {
            return loaded;
        }

        /**
         * @return the result of the reload (if it was fully successful, partially successful, or failed)
         */
        public ReloadResult getResult() {
            return result;
        }
    }
}
