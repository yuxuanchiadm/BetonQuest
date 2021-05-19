package org.betonquest.betonquest.menu.events;

import org.betonquest.betonquest.menu.MenuID;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * Called whenever a menu is opened
 * <p>
 * Created on 16.03.2018.
 *
 * @author Jonas Blocher
 */
public class MenuOpenEvent extends MenuEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private boolean cancelled = false;

    public MenuOpenEvent(final Player who, final MenuID menu) {
        super(who, menu);
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(final boolean b) {
        cancelled = b;
    }
}
