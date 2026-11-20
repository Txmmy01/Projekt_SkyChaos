package de.skychaos.world;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

public final class VoidGenerator extends ChunkGenerator {
    @Override public boolean shouldGenerateNoise() { return false; }
    @Override public boolean shouldGenerateSurface() { return false; }
    @Override public boolean shouldGenerateDecorations() { return false; }
    @Override public boolean shouldGenerateMobs() { return false; }
    @Override public boolean shouldGenerateStructures() { return false; }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        return new Location(world,0.5,80.0,0.5);
    }
}
