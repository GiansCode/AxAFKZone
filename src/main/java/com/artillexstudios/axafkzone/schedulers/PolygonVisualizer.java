package com.artillexstudios.axafkzone.schedulers;

import com.artillexstudios.axafkzone.AxAFKZone;
import com.artillexstudios.axafkzone.listeners.WandListeners;
import com.artillexstudios.axafkzone.selection.Selection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;

public class PolygonVisualizer implements Runnable {
    private static BukkitTask task;

    public static void start() {
        if (task != null) {
            task.cancel();
        }
        task = Bukkit.getScheduler().runTaskTimer(AxAFKZone.getInstance(), new PolygonVisualizer(), 0L, 10L);
    }

    public static void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @Override
    public void run() {
        for (Map.Entry<Player, Selection> entry : WandListeners.getSelections().entrySet()) {
            Player player = entry.getKey();
            Selection selection = entry.getValue();

            if (!player.isOnline()) continue;
            
            // Only show visualizer for polygon mode with at least one point
            if (selection.getMode() == Selection.SelectionMode.POLYGON && selection.getPolygonPointCount() > 0) {
                selection.show(player);
            }
            // Also show cuboid selections
            else if (selection.getMode() == Selection.SelectionMode.CUBOID) {
                selection.show(player);
            }
        }
    }
}
