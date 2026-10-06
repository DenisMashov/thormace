package com.denis.thormace;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Vurulan yerin etrafindaki bloklari havaya ucurur.
 * Gercek bloklar degismez: oyunculara gecici "hava" gonderilir,
 * yerine BlockDisplay animasyonu oynatilir, sonunda bloklar geri gosterilir.
 */
public final class BlockBlast {

    private static final BlockFace[] FACES = {
            BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH,
            BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    private static final class Piece {
        Block block;
        BlockDisplay display;
        Location base;
        double height, dx, dz;
        float angle;
        Vector3f axis;
    }

    private final ThorMace plugin;
    private final Location center;
    private final List<Piece> pieces = new ArrayList<>();
    private final List<Player> viewers = new ArrayList<>();

    private BukkitRunnable task;
    private int tick = 0;
    private int total;
    private boolean finished = false;

    public BlockBlast(ThorMace plugin, Location center) {
        this.plugin = plugin;
        this.center = center.clone();
    }

    public void start() {
        FileConfiguration c = plugin.getConfig();
        final int radius = Math.max(1, c.getInt("blast.radius", 5));
        final int max = Math.max(1, c.getInt("blast.max-blocks", 200));
        final double minH = c.getDouble("blast.min-height", 3.0);
        final double maxH = Math.max(minH, c.getDouble("blast.max-height", 7.0));
        final double drift = c.getDouble("blast.drift", 1.5);
        total = Math.max(10, c.getInt("blast.duration-ticks", 40));

        World world = center.getWorld();
        int cx = center.getBlockX(), cy = center.getBlockY(), cz = center.getBlockZ();

        List<Block> candidates = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radius * radius) continue;
                    int x = cx + dx, z = cz + dz;
                    if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
                    Block b = world.getBlockAt(x, cy + dy, z);
                    if (launchable(b)) candidates.add(b);
                }
            }
        }

        // efektler (blok olmasa da calissin)
        world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
        world.spawnParticle(Particle.ELECTRIC_SPARK, center.clone().add(0, 1, 0), 60, 1.5, 1, 1.5, 0.2);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);

        if (candidates.isEmpty()) return;
        Collections.shuffle(candidates);
        if (candidates.size() > max) candidates = new ArrayList<>(candidates.subList(0, max));

        viewers.addAll(world.getNearbyPlayers(center, 64));
        BlockData air = Material.AIR.createBlockData();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        for (Block b : candidates) {
            Piece p = new Piece();
            p.block = b;
            p.base = b.getLocation();
            p.height = minH + rnd.nextDouble() * (maxH - minH);
            p.dx = (rnd.nextDouble() * 2 - 1) * drift;
            p.dz = (rnd.nextDouble() * 2 - 1) * drift;
            p.angle = (float) (rnd.nextDouble() * Math.PI);
            p.axis = new Vector3f(
                    (float) (rnd.nextDouble() * 2 - 1),
                    (float) (rnd.nextDouble() * 2 - 1),
                    (float) (rnd.nextDouble() * 2 - 1));
            if (p.axis.lengthSquared() < 0.0001f) p.axis.set(0, 1, 0);
            p.axis.normalize();

            BlockData data = b.getBlockData();
            p.display = world.spawn(p.base, BlockDisplay.class, d -> {
                d.setBlock(data);
                d.setPersistent(false);
                d.setTeleportDuration(2);
                d.setBrightness(new Display.Brightness(15, 15));
            });
            pieces.add(p);
        }

        for (Player viewer : viewers) {
            for (Piece p : pieces) {
                viewer.sendBlockChange(p.base, air);
            }
        }

        plugin.blasts().add(this);

        task = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        };
        task.runTaskTimer(plugin, 1L, 1L);
    }

    private void tick() {
        tick++;
        if (tick >= total) {
            finish();
            return;
        }

        double f = tick / (double) total;
        double s = 4 * f * (1 - f); // parabol: yukari cik, geri in

        int half = total / 2;
        for (Piece p : pieces) {
            if (!p.display.isValid()) continue;

            p.display.teleport(p.base.clone().add(p.dx * s, p.height * s, p.dz * s));

            if (tick == 1) {
                spin(p, p.angle, half);
            } else if (tick == half) {
                spin(p, 0f, half);
            }
        }
    }

    private void spin(Piece p, float angle, int duration) {
        Quaternionf q = new Quaternionf().fromAxisAngleRad(p.axis.x, p.axis.y, p.axis.z, angle);
        Vector3f c = new Vector3f(0.5f, 0.5f, 0.5f);
        Vector3f translation = new Vector3f(c).sub(q.transform(new Vector3f(c)));
        p.display.setInterpolationDelay(0);
        p.display.setInterpolationDuration(duration);
        p.display.setTransformation(new Transformation(translation, q, new Vector3f(1, 1, 1), new Quaternionf()));
    }

    public void finish() {
        if (finished) return;
        finished = true;
        if (task != null) task.cancel();

        for (Piece p : pieces) {
            if (p.display != null && p.display.isValid()) p.display.remove();
        }
        for (Player viewer : viewers) {
            if (!viewer.isOnline()) continue;
            for (Piece p : pieces) {
                viewer.sendBlockChange(p.base, p.block.getBlockData());
            }
        }
        plugin.blasts().remove(this);
    }

    private static boolean launchable(Block b) {
        Material t = b.getType();
        if (t.isAir() || t == Material.BEDROCK || t == Material.BARRIER) return false;
        if (b.isLiquid() || b.isPassable()) return false;
        if (b.getState(false) instanceof TileState) return false;
        for (BlockFace face : FACES) {
            if (b.getRelative(face).isPassable()) return true; // sadece yuzeydeki bloklar
        }
        return false;
    }
}
