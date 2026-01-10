package com.artillexstudios.axafkzone.selection;

import com.artillexstudios.axapi.collections.ThreadSafeList;
import com.artillexstudios.axapi.scheduler.Scheduler;
import com.artillexstudios.axapi.selection.Cuboid;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.artillexstudios.axafkzone.AxAFKZone.MESSAGEUTILS;

public class Selection {
    public enum SelectionMode {
        CUBOID,
        POLYGON
    }

    private final Player player;
    private Location position1 = null;
    private Location position2 = null;
    private final ThreadSafeList<Location> borders = new ThreadSafeList<>();
    private SelectionMode mode = SelectionMode.CUBOID;
    
    // Polygon selection fields
    private final List<Location> polygonPoints = new ArrayList<>();
    private Location firstPolygonPoint = null;

    public Selection(Player player) {
        this.player = player;
    }

    public Location getPosition1() {
        return position1;
    }

    public void setPosition1(@NotNull Location position1) {
        this.position1 = position1;
        update();
    }

    public Location getPosition2() {
        return position2;
    }

    public void setPosition2(@NotNull Location position2) {
        this.position2 = position2;
        update();
    }

    private void update() {
        updateVisualizer();
        if (mode == SelectionMode.CUBOID) {
            if (position1 == null || position2 == null) return;
            if (position1.getBlockY() != position2.getBlockY()) return;
            Scheduler.get().run(() -> MESSAGEUTILS.sendLang(player, "selection.small-selection"));
        }
    }

    public void show(@NotNull Player player) {
        if (borders.isEmpty()) return;
        for (int i = 0; i < borders.size(); i++) {
            final Location l2 = borders.get(i).clone();
            l2.add(0.5, 0.5, 0.5);
            player.spawnParticle(Particle.WAX_ON, l2, 1, 0, 0, 0, 0);
        }
    }

    private void updateVisualizer() {
        borders.clear();
        
        if (mode == SelectionMode.CUBOID) {
            updateCuboidVisualizer();
        } else if (mode == SelectionMode.POLYGON) {
            updatePolygonVisualizer();
        }
    }

    private void updateCuboidVisualizer() {
        if (position1 == null || position2 == null || !Objects.equals(position1.getWorld(), position2.getWorld())) return;
        final Cuboid cube = new Cuboid(position1.getWorld(), position1.getBlockX(), position2.getBlockX(), position1.getBlockZ(), position2.getBlockZ(), position1.getBlockY(), position2.getBlockY());

        for (double x = cube.getMinX(); x <= cube.getMaxX(); x+=0.25) {
            for (double y = cube.getMinY(); y <= cube.getMaxY(); y+=0.25) {
                for (double z = cube.getMinZ(); z <= cube.getMaxZ(); z+=0.25) {
                    int components = 0;
                    if (x == cube.getMinX() || x == cube.getMaxX()) components++;
                    if (y == cube.getMinY() || y == cube.getMaxY()) components++;
                    if (z == cube.getMinZ() || z == cube.getMaxZ()) components++;
                    if (components >= 2) {
                        borders.add(new Location(cube.getWorld(), x, y, z));
                    }
                }
            }
        }
    }

    private void updatePolygonVisualizer() {
        if (polygonPoints.isEmpty()) return;
        
        // Make each clicked block glow
        for (Location point : polygonPoints) {
            drawGlowingBlock(point);
        }
        
        // Draw horizontal lines between consecutive points
        for (int i = 0; i < polygonPoints.size(); i++) {
            Location start = polygonPoints.get(i);
            Location end = polygonPoints.get((i + 1) % polygonPoints.size());
            
            // Only draw line if we have at least 2 points, or if polygon is complete
            if (i < polygonPoints.size() - 1 || isPolygonComplete()) {
                drawLine(start, end);
            }
        }
    }

    private void drawGlowingBlock(Location location) {
        // Create a glowing effect by placing particles at the block center
        double x = location.getBlockX() + 0.5;
        double y = location.getBlockY() + 0.5;
        double z = location.getBlockZ() + 0.5;
        
        // Add a concentrated cluster of particles at the center to make it glow
        for (int i = 0; i < 3; i++) {
            borders.add(new Location(location.getWorld(), x, y, z));
        }
        
        // Add particles around the block to create a subtle highlight
        double radius = 0.4;
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 4) {
            borders.add(new Location(location.getWorld(), 
                x + Math.cos(angle) * radius, 
                y, 
                z + Math.sin(angle) * radius));
        }
    }

    private void drawLine(Location start, Location end) {
        // Draw line from center of start block to center of end block
        double startX = start.getBlockX() + 0.5;
        double startY = start.getBlockY() + 0.5;
        double startZ = start.getBlockZ() + 0.5;
        
        double endX = end.getBlockX() + 0.5;
        double endY = end.getBlockY() + 0.5;
        double endZ = end.getBlockZ() + 0.5;
        
        double distance = Math.sqrt(
            Math.pow(endX - startX, 2) +
            Math.pow(endY - startY, 2) +
            Math.pow(endZ - startZ, 2)
        );
        
        for (double i = 0; i <= distance; i += 0.25) {
            double ratio = i / distance;
            double x = startX + (endX - startX) * ratio;
            double y = startY + (endY - startY) * ratio;
            double z = startZ + (endZ - startZ) * ratio;
            borders.add(new Location(start.getWorld(), x, y, z));
        }
    }

    // Polygon mode methods
    public SelectionMode getMode() {
        return mode;
    }

    public void setMode(SelectionMode mode) {
        this.mode = mode;
        if (mode == SelectionMode.POLYGON) {
            resetPolygonSelection();
        }
    }

    public void addPolygonPoint(@NotNull Location location) {
        if (firstPolygonPoint == null) {
            firstPolygonPoint = location.clone();
            polygonPoints.add(location.clone());
        } else {
            polygonPoints.add(location.clone());
        }
        update();
    }

    public boolean removeLastPolygonPoint() {
        if (polygonPoints.isEmpty()) return false;
        
        polygonPoints.remove(polygonPoints.size() - 1);
        
        // If we removed the first point, reset everything
        if (polygonPoints.isEmpty()) {
            firstPolygonPoint = null;
        }
        
        update();
        return true;
    }

    public boolean isNearFirstPoint(@NotNull Location location) {
        if (firstPolygonPoint == null) return false;
        if (polygonPoints.size() < 3) return false; // Need at least 3 points to complete
        
        // Check if clicking the same block as first point
        return firstPolygonPoint.getBlockX() == location.getBlockX() &&
               firstPolygonPoint.getBlockY() == location.getBlockY() &&
               firstPolygonPoint.getBlockZ() == location.getBlockZ();
    }

    public boolean isPolygonComplete() {
        return polygonPoints.size() >= 3 && firstPolygonPoint != null;
    }

    public void resetPolygonSelection() {
        polygonPoints.clear();
        firstPolygonPoint = null;
        borders.clear();
    }

    @NotNull
    public List<Location> getPolygonPoints() {
        return new ArrayList<>(polygonPoints);
    }

    @Nullable
    public Location getFirstPolygonPoint() {
        return firstPolygonPoint;
    }

    public int getPolygonPointCount() {
        return polygonPoints.size();
    }
}
