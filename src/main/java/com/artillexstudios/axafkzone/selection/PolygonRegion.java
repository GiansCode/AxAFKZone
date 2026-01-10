package com.artillexstudios.axafkzone.selection;

import com.artillexstudios.axafkzone.zones.Zone;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PolygonRegion extends Region {
    private final List<Location> points;
    private final int minY;
    private final int maxY;
    private World world;
    private final Zone zone;
    private final Location center;

    public PolygonRegion(@NotNull List<Location> points, int minY, int maxY, @NotNull Zone zone) {
        super(points.get(0), points.get(points.size() - 1), zone);
        this.points = new ArrayList<>(points);
        this.minY = minY;
        this.maxY = maxY;
        this.zone = zone;
        this.world = points.get(0).getWorld();
        this.center = calculateCenter();
    }

    private Location calculateCenter() {
        double sumX = 0;
        double sumZ = 0;
        for (Location point : points) {
            sumX += point.getBlockX();
            sumZ += point.getBlockZ();
        }
        return new Location(world, sumX / points.size(), (minY + maxY) / 2.0, sumZ / points.size());
    }

    @Override
    public Set<Player> getPlayersInZone() {
        if (world == null) return Set.of();
        final HashSet<Player> players = new HashSet<>();

        String permission = zone.getSettings().getString("permission");
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isDead()) continue;
            if (!player.getWorld().equals(world)) continue;
            if (!permission.isBlank() && !player.hasPermission(permission)) continue;

            final Location loc = player.getLocation();

            // Check Y bounds
            if (loc.getBlockY() < minY || loc.getBlockY() > maxY) continue;

            // Check if player is inside the polygon using ray casting algorithm
            if (isPointInPolygon(loc.getBlockX(), loc.getBlockZ())) {
                players.add(player);
            }
        }

        return players;
    }

    /**
     * Ray casting algorithm to determine if a point is inside a polygon
     * Casts a ray from the point to the right and counts intersections
     * Odd number of intersections = inside, Even number = outside
     */
    private boolean isPointInPolygon(int x, int z) {
        boolean inside = false;
        int j = points.size() - 1;

        for (int i = 0; i < points.size(); i++) {
            int xi = points.get(i).getBlockX();
            int zi = points.get(i).getBlockZ();
            int xj = points.get(j).getBlockX();
            int zj = points.get(j).getBlockZ();

            if ((zi > z) != (zj > z) && (x < (xj - xi) * (z - zi) / (zj - zi) + xi)) {
                inside = !inside;
            }
            j = i;
        }

        return inside;
    }

    @NotNull
    public List<Location> getPoints() {
        return new ArrayList<>(points);
    }

    @Override
    @NotNull
    public Location getCenter() {
        return center;
    }

    @Override
    public long getCenterX() {
        return center.getBlockX();
    }

    @Override
    public long getCenterY() {
        return center.getBlockY();
    }

    @Override
    public long getCenterZ() {
        return center.getBlockZ();
    }

    @Override
    @Nullable
    public World getWorld() {
        return world;
    }

    @Override
    public void setWorld(World world) {
        this.world = world;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxY() {
        return maxY;
    }
}
