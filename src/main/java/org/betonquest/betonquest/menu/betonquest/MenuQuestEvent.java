package org.betonquest.betonquest.menu.betonquest;

import org.betonquest.betonquest.Instruction;
import org.betonquest.betonquest.api.QuestEvent;
import org.betonquest.betonquest.exceptions.InstructionParseException;
import org.betonquest.betonquest.exceptions.ObjectNotFoundException;
import org.betonquest.betonquest.exceptions.QuestRuntimeException;
import org.betonquest.betonquest.menu.MenuID;
import org.betonquest.betonquest.menu.RPGMenu;
import org.betonquest.betonquest.utils.PlayerConverter;
import org.bukkit.entity.Player;

/**
 * Event to open or close menus
 * <p>
 * Created on 16.03.2018.
 *
 * @author Jonas Blocher
 */
public class MenuQuestEvent extends QuestEvent {

    private final Operation operation;
    private MenuID menu;

    public MenuQuestEvent(final Instruction instruction) throws InstructionParseException {
        super(instruction, true);
        this.operation = instruction.getEnum(Operation.class);
        if (this.operation == Operation.OPEN) {
            try {
                this.menu = new MenuID(instruction.getPackage(), instruction.next());
            } catch (final ObjectNotFoundException e) {
                throw new InstructionParseException("Error while parsing 2 argument: Error while loading menu: " + e.getMessage());
            }
        }
    }

    @Override
    public Void execute(final String playerID) throws QuestRuntimeException {
        final Player player = PlayerConverter.getPlayer(playerID);
        if (operation == Operation.OPEN) {
            RPGMenu.openMenu(player, menu);
        } else {
            RPGMenu.closeMenu(player);
        }
        return null;
    }

    public enum Operation {
        OPEN,
        CLOSE
    }
}
