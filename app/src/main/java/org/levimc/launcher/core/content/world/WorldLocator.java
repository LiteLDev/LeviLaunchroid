package org.levimc.launcher.core.content.world;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WorldLocator {
    public enum Kind {
        BLOCK,
        BIOME,
        ENTITY,
        BLOCK_ENTITY,
        SAVED_RECORD
    }

    public interface Progress {
        boolean update(int done, int total);
    }

    public static final class Result {
        public final String label;
        public final int x, y, z, dimension;
        public final byte[] key;

        Result(String label, int x, int y, int z, int dimension, byte[] key) {
            this.label = label;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
            this.key = key;
        }

        public String toString() {
            return label
                    + "\n"
                    + x
                    + ", "
                    + y
                    + ", "
                    + z
                    + " · "
                    + WorldRecords.dimensionName(dimension);
        }
    }

    private final WorldSession session;
    private final WorldMapRenderer renderer;

    public WorldLocator(WorldSession session, WorldMapRenderer renderer) {
        this.session = session;
        this.renderer = renderer;
    }

    public List<Result> search(
            Kind kind,
            String query,
            int centerX,
            int centerZ,
            int radius,
            int dimension,
            int minY,
            int maxY,
            Progress progress)
            throws IOException {
        if (radius < 0 || radius > 32)
            throw new IOException("Search radius must be between 0 and 32 chunks");
        if (query.trim().isEmpty()) throw new IOException("Enter a search value");
        List<Result> result = new ArrayList<>();
        String lower = query.toLowerCase(Locale.ROOT).trim();
        if (kind == Kind.SAVED_RECORD) {
            byte[] after = null;
            int pages = 0;
            do {
                org.levimc.launcher.core.content.leveldb.WorldDatabase.Page page =
                        session.scan(new byte[0], after, 128, query);
                for (byte[] key : page.keys) {
                    result.add(
                            new Result(
                                    WorldRecords.label(key), centerX, 0, centerZ, dimension, key));
                    if (result.size() >= 512) return result;
                }
                after = page.next;
                if (!progress.update(++pages, 0)) throw new java.io.InterruptedIOException();
            } while (after != null);
            return result;
        }
        int biome = kind == Kind.BIOME ? Integer.parseInt(query) : -1,
                cx = Math.floorDiv(centerX, 16),
                cz = Math.floorDiv(centerZ, 16),
                done = 0,
                total = (radius * 2 + 1) * (radius * 2 + 1);
        for (int ring = 0; ring <= radius; ring++)
            for (int dx = -ring; dx <= ring; dx++)
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    if (Thread.currentThread().isInterrupted() || !progress.update(++done, total))
                        throw new java.io.InterruptedIOException();
                    int x = cx + dx, z = cz + dz;
                    if (kind == Kind.ENTITY || kind == Kind.BLOCK_ENTITY) {
                        MapSettings settings = new MapSettings();
                        settings.dimension = dimension;
                        settings.entities = kind == Kind.ENTITY;
                        settings.blockEntities = kind == Kind.BLOCK_ENTITY;
                        settings.entityFilter = query;
                        settings.blockEntityFilter = query;
                        for (WorldRecords.Marker marker :
                                WorldRecords.chunkMarkers(session, x, z, settings))
                            if (marker.y >= minY && marker.y <= maxY)
                                result.add(
                                        new Result(
                                                marker.label,
                                                (int) Math.floor(marker.x),
                                                (int) Math.floor(marker.y),
                                                (int) Math.floor(marker.z),
                                                dimension,
                                                marker.key));
                    } else {
                        WorldChunk chunk = renderer.chunk(x, z, dimension);
                        if (!chunk.exists) continue;
                        if (kind == Kind.BIOME) {
                            boolean found = false;
                            for (int yy = Math.max(minY, chunk.minY());
                                    yy <= Math.min(maxY, chunk.maxY()) && !found;
                                    yy += 4)
                                for (int xx = 0; xx < 16 && !found; xx += 4)
                                    for (int zz = 0; zz < 16 && !found; zz += 4)
                                        if (chunk.biome(xx, yy, zz) == biome) {
                                            result.add(
                                                    new Result(
                                                            MapPalette.biomeName(biome),
                                                            x * 16 + xx,
                                                            yy,
                                                            z * 16 + zz,
                                                            dimension,
                                                            null));
                                            found = true;
                                        }
                        } else
                            for (WorldChunk.Section section : chunk.sections.values()) {
                                if (section.y * 16 > maxY || section.y * 16 + 15 < minY) continue;
                                WorldChunk.Storage storage = section.layers.get(0);
                                boolean[] match = new boolean[storage.palette.size()];
                                boolean any = false;
                                for (int i = 0; i < match.length; i++) {
                                    match[i] =
                                            WorldRecords.filter(
                                                    WorldChunk.name(storage.palette.get(i)), lower);
                                    any |= match[i];
                                }
                                if (!any) continue;
                                for (int index = 0; index < 4096; index++)
                                    if (match[storage.indices[index]]) {
                                        int yy = section.y * 16 + (index & 15);
                                        if (yy < minY || yy > maxY) continue;
                                        result.add(
                                                new Result(
                                                        WorldChunk.name(
                                                                storage.palette.get(
                                                                        storage.indices[index])),
                                                        x * 16 + (index >>> 8),
                                                        yy,
                                                        z * 16 + ((index >>> 4) & 15),
                                                        dimension,
                                                        null));
                                        if (result.size() >= 512) return result;
                                    }
                            }
                    }
                    if (result.size() >= 512) return result;
                }
        return result;
    }
}
