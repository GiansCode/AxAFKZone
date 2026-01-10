package com.artillexstudios.axafkzone.commands.subcommands;

import com.artillexstudios.axafkzone.listeners.WandListeners;
import com.artillexstudios.axafkzone.selection.PolygonRegion;
import com.artillexstudios.axafkzone.selection.Region;
import com.artillexstudios.axafkzone.selection.Selection;
import com.artillexstudios.axafkzone.zones.Zone;
import com.artillexstudios.axapi.serializers.Serializers;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Objects;

import static com.artillexstudios.axafkzone.AxAFKZone.MESSAGEUTILS;

public enum Redefine {
    INSTANCE;

    public void execute(Player sender, Zone zone) {
        if (!WandListeners.getSelections().containsKey(sender)) {
            MESSAGEUTILS.sendLang(sender, "selection.no-selection", Collections.singletonMap("%name%", zone.getName()));
            return;
        }

        final Selection sel = WandListeners.getSelections().remove(sender);

        Region newRegion;
        if (sel.getMode() == Selection.SelectionMode.POLYGON) {
            if (!sel.isPolygonComplete()) {
                MESSAGEUTILS.sendLang(sender, "selection.polygon-incomplete", Collections.singletonMap("%name%", zone.getName()));
                return;
            }
            
            // Calculate Y bounds from polygon points
            int minY = sel.getPolygonPoints().stream()
                .mapToInt(loc -> loc.getBlockY())
                .min()
                .orElse(0);
            int maxY = sel.getPolygonPoints().stream()
                .mapToInt(loc -> loc.getBlockY())
                .max()
                .orElse(256);
            
            newRegion = new PolygonRegion(sel.getPolygonPoints(), minY, maxY, zone);
            
            // Update config
            zone.getSettings().set("zone.type", "polygon");
            zone.getSettings().set("zone.points", sel.getPolygonPoints().stream()
                .map(Serializers.LOCATION::serialize)
                .toList());
            zone.getSettings().set("zone.minY", minY);
            zone.getSettings().set("zone.maxY", maxY);
            zone.getSettings().save();
        } else {
            if (sel.getPosition1() == null || sel.getPosition2() == null || !Objects.equals(sel.getPosition1().getWorld(), sel.getPosition2().getWorld())) {
                MESSAGEUTILS.sendLang(sender, "selection.no-selection", Collections.singletonMap("%name%", zone.getName()));
                return;
            }
            
            newRegion = new Region(sel.getPosition1(), sel.getPosition2(), zone);
        }

        zone.setRegion(newRegion);
        MESSAGEUTILS.sendLang(sender, "zone.redefined", Collections.singletonMap("%name%", zone.getName()));
    }
}
