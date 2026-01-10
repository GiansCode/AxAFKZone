package com.artillexstudios.axafkzone.listeners;

import com.artillexstudios.axafkzone.selection.Selection;
import com.artillexstudios.axapi.items.NBTWrapper;
import com.artillexstudios.axapi.serializers.Serializers;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Objects;
import java.util.WeakHashMap;

import static com.artillexstudios.axafkzone.AxAFKZone.MESSAGEUTILS;

public class WandListeners implements Listener {
    private static final WeakHashMap<Player, Selection> selections = new WeakHashMap<>();

    @NotNull
    public static WeakHashMap<Player, Selection> getSelections() {
        return selections;
    }

    @EventHandler
    public void onInteract(@NotNull PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (event.getItem() == null || event.getItem().getType().equals(Material.AIR)) return;

        ItemStack itemInHand = event.getPlayer().getInventory().getItemInMainHand();
        if (itemInHand.getType().equals(Material.AIR)) return;

        NBTWrapper wrapper = new NBTWrapper(itemInHand);
        if (!wrapper.getBooleanOr("axafkzone-wand", false)) return;
        event.setCancelled(true);

        if (!selections.containsKey(event.getPlayer())) {
            selections.put(event.getPlayer(), new Selection(event.getPlayer()));
        }
        
        Selection selection = selections.get(event.getPlayer());

        if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            if (selection.getMode() == Selection.SelectionMode.POLYGON) {
                onPolygonLeftClick(event.getPlayer(), event.getClickedBlock().getLocation());
            } else {
                onLeftClick(event.getPlayer(), event.getClickedBlock().getLocation());
            }
        } else if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            if (selection.getMode() == Selection.SelectionMode.POLYGON) {
                onPolygonRightClick(event.getPlayer(), event.getClickedBlock().getLocation());
            } else {
                onRightClick(event.getPlayer(), event.getClickedBlock().getLocation());
            }
        }
    }

    private void onLeftClick(@NotNull Player player, @NotNull Location location) {
        if (!selections.containsKey(player)) selections.put(player, new Selection(player));
        if (Objects.equals(selections.get(player).getPosition1(), location)) return;

        player.setCooldown(player.getInventory().getItemInMainHand().getType(), 5);
        selections.get(player).setPosition1(location);
        MESSAGEUTILS.sendLang(player, "selection.pos1", Collections.singletonMap("%location%", Serializers.LOCATION.serialize(location)));
    }

    private void onRightClick(@NotNull Player player, @NotNull Location location) {
        if (!selections.containsKey(player)) selections.put(player, new Selection(player));
        if (Objects.equals(selections.get(player).getPosition2(), location)) return;

        player.setCooldown(player.getInventory().getItemInMainHand().getType(), 5);
        selections.get(player).setPosition2(location);
        MESSAGEUTILS.sendLang(player, "selection.pos2", Collections.singletonMap("%location%", Serializers.LOCATION.serialize(location)));
    }

    private void onPolygonLeftClick(@NotNull Player player, @NotNull Location location) {
        Selection selection = selections.get(player);
        player.setCooldown(player.getInventory().getItemInMainHand().getType(), 5);

        // Check if clicking on the first point to complete the polygon
        if (selection.isNearFirstPoint(location)) {
            MESSAGEUTILS.sendLang(player, "selection.polygon-complete", 
                Collections.singletonMap("%points%", String.valueOf(selection.getPolygonPointCount())));
            return;
        }

        // Add new point
        selection.addPolygonPoint(location);
        MESSAGEUTILS.sendLang(player, "selection.polygon-point", 
            new java.util.HashMap<String, String>() {{
                put("%point%", String.valueOf(selection.getPolygonPointCount()));
                put("%location%", Serializers.LOCATION.serialize(location));
            }});

        // Show hint about completing if we have enough points
        if (selection.getPolygonPointCount() >= 3 && selection.getFirstPolygonPoint() != null) {
            MESSAGEUTILS.sendLang(player, "selection.polygon-hint");
        }
    }

    private void onPolygonRightClick(@NotNull Player player, @NotNull Location location) {
        Selection selection = selections.get(player);
        player.setCooldown(player.getInventory().getItemInMainHand().getType(), 5);

        // Right click removes the last point
        if (selection.getPolygonPointCount() > 0) {
            boolean removed = selection.removeLastPolygonPoint();
            if (removed) {
                if (selection.getPolygonPointCount() == 0) {
                    MESSAGEUTILS.sendLang(player, "selection.polygon-reset");
                } else {
                    MESSAGEUTILS.sendLang(player, "selection.polygon-point-removed", 
                        Collections.singletonMap("%points%", String.valueOf(selection.getPolygonPointCount())));
                }
            }
        }
    }
}
