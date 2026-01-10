package com.artillexstudios.axafkzone.commands.subcommands;

import com.artillexstudios.axafkzone.listeners.WandListeners;
import com.artillexstudios.axafkzone.selection.Selection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

import static com.artillexstudios.axafkzone.AxAFKZone.MESSAGEUTILS;

public enum Mode {
    INSTANCE;

    public void execute(@NotNull Player player, String mode) {
        Selection selection = WandListeners.getSelections().get(player);
        if (selection == null) {
            selection = new Selection(player);
            WandListeners.getSelections().put(player, selection);
        }

        Selection.SelectionMode newMode;
        try {
            newMode = Selection.SelectionMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            MESSAGEUTILS.sendLang(player, "selection.invalid-mode", 
                Collections.singletonMap("%mode%", mode));
            return;
        }

        selection.setMode(newMode);
        MESSAGEUTILS.sendLang(player, "selection.mode-changed", 
            Collections.singletonMap("%mode%", newMode.name().toLowerCase()));
    }
}
