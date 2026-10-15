package org.levimc.launcher.core.content.world;

import org.levimc.launcher.core.content.leveldb.WorldDatabase;
import org.levimc.launcher.core.content.nbt.NativeNbt;
import org.levimc.launcher.core.content.nbt.NbtTag;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WorldOperations {
    public static final class Region {
        public final int x1, y1, z1, x2, y2, z2, dimension;

        public Region(int x1, int y1, int z1, int x2, int y2, int z2, int dimension) {
            this.x1 = Math.min(x1, x2);
            this.y1 = Math.min(y1, y2);
            this.z1 = Math.min(z1, z2);
            this.x2 = Math.max(x1, x2);
            this.y2 = Math.max(y1, y2);
            this.z2 = Math.max(z1, z2);
            this.dimension = dimension;
        }

        public int width() {
            return x2 - x1 + 1;
        }

        public int height() {
            return y2 - y1 + 1;
        }

        public int depth() {
            return z2 - z1 + 1;
        }

        public long volume() {
            return (long) width() * height() * depth();
        }

        public void validateBounds() throws IOException {
            if (Math.abs((long) x1) > 30000000
                    || Math.abs((long) x2) > 30000000
                    || Math.abs((long) z1) > 30000000
                    || Math.abs((long) z2) > 30000000)
                throw new IOException("Coordinates exceed the world bounds");
            if (y1 < (dimension == 0 ? -64 : 0) || y2 > (dimension == 0 ? 319 : 255))
                throw new IOException("Y range is outside the dimension's build height");
        }

        public long chunks() {
            return (long) (Math.floorDiv(x2, 16) - Math.floorDiv(x1, 16) + 1)
                    * (Math.floorDiv(z2, 16) - Math.floorDiv(z1, 16) + 1);
        }

        public void validate() throws IOException {
            validateBounds();
            if (chunks() > 256) throw new IOException("Select at most 256 chunks per operation");
            if (volume() < 1 || volume() > 524288)
                throw new IOException("Select at most 524288 blocks per edit");
        }

        public boolean contains(double x, double y, double z) {
            return x >= x1 && x < x2 + 1.0 && y >= y1 && y < y2 + 1.0 && z >= z1 && z < z2 + 1.0;
        }
    }

    public interface Progress {
        boolean update(int done, int total);
    }

    private final WorldSession session;

    public WorldOperations(WorldSession session) {
        this.session = session;
    }

    public static NbtTag copy(NbtTag tag) throws IOException {
        return NativeNbt.decode(
                        NativeNbt.encode(tag, 0, NativeNbt.LITTLE_ENDIAN, false),
                        0,
                        NativeNbt.LITTLE_ENDIAN)
                .root;
    }

    public static NbtTag parseBlock(String text) throws IOException {
        NbtTag result =
                text.trim().startsWith("{")
                        ? NativeNbt.fromSnbt(text)
                        : WorldChunk.block(
                                text.contains(":") ? text.trim() : "minecraft:" + text.trim());
        if (result.getType() != NbtTag.TAG_COMPOUND
                || result.getTag("name") == null
                || result.getTag("states") == null)
            throw new IOException("Use a block name or a compound containing name and states");
        if (!WorldChunk.name(result).matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))
            throw new IOException("Invalid block identifier");
        return result;
    }

    public int blocks(Region region, String find, NbtTag replacement, int layer, Progress progress)
            throws IOException {
        region.validate();
        String replacementSnbt = NativeNbt.toSnbt(replacement);
        if (layer < 0 || layer > 1)
            throw new IOException("Select foreground or background storage");
        Map<byte[], byte[]> edits = new LinkedHashMap<>();
        int changed = 0, done = 0;
        int total =
                (Math.floorDiv(region.x2, 16) - Math.floorDiv(region.x1, 16) + 1)
                        * (Math.floorDiv(region.z2, 16) - Math.floorDiv(region.z1, 16) + 1);
        for (int cx = Math.floorDiv(region.x1, 16); cx <= Math.floorDiv(region.x2, 16); cx++)
            for (int cz = Math.floorDiv(region.z1, 16); cz <= Math.floorDiv(region.z2, 16); cz++) {
                if (!progress.update(++done, total) || Thread.currentThread().isInterrupted())
                    throw new java.io.InterruptedIOException();
                WorldChunk chunk = WorldChunk.load(session, cx, cz, region.dimension);
                if (!chunk.exists)
                    throw new IOException(
                            "Selection includes an ungenerated chunk at " + cx + ", " + cz);
                requireWritable(chunk);
                BitSet touched = new BitSet(16 * 16 * (region.dimension == 0 ? 384 : 256));
                boolean any = false;
                for (int sy = Math.floorDiv(region.y1, 16);
                        sy <= Math.floorDiv(region.y2, 16);
                        sy++) {
                    WorldChunk.Section section = chunk.sections.get(sy);
                    if (section == null) section = new WorldChunk.Section(sy);
                    boolean sectionChanged = false;
                    while (section.layers.size() <= layer)
                        section.layers.add(new WorldChunk.Storage());
                    WorldChunk.Storage storage = section.layers.get(layer);
                    boolean[] replaceable = new boolean[storage.palette.size()];
                    for (int p = 0; p < replaceable.length; p++) {
                        NbtTag old = storage.palette.get(p);
                        replaceable[p] =
                                (find == null
                                                || find.isBlank()
                                                || WorldRecords.filter(WorldChunk.name(old), find))
                                        && !NativeNbt.toSnbt(old).equals(replacementSnbt);
                    }
                    for (int x = Math.max(region.x1, cx * 16);
                            x <= Math.min(region.x2, cx * 16 + 15);
                            x++)
                        for (int z = Math.max(region.z1, cz * 16);
                                z <= Math.min(region.z2, cz * 16 + 15);
                                z++)
                            for (int y = Math.max(region.y1, sy * 16);
                                    y <= Math.min(region.y2, sy * 16 + 15);
                                    y++) {
                                NbtTag old = storage.at(x & 15, y & 15, z & 15);
                                int oldIndex =
                                        storage.indices[
                                                ((x & 15) << 8) | ((z & 15) << 4) | (y & 15)];
                                if (oldIndex >= replaceable.length || !replaceable[oldIndex])
                                    continue;
                                storage.set(x & 15, y & 15, z & 15, replacement);
                                changed++;
                                sectionChanged = true;
                                any = true;
                                if (layer == 0
                                        && !WorldChunk.name(old)
                                                .equals(WorldChunk.name(replacement)))
                                    touched.set(
                                            ((y - (region.dimension == 0 ? -64 : 0)) * 16
                                                                    + (z & 15))
                                                            * 16
                                                    + (x & 15));
                            }
                    if (sectionChanged) {
                        chunk.sections.put(sy, section);
                        edits.put(
                                ChunkKey.of(cx, cz, region.dimension, 0x2f, sy),
                                WorldChunk.encodeSection(section));
                    }
                }
                if (any) {
                    updateHeightmap(edits, chunk);
                    filterBlockEntities(edits, chunk, touched);
                    invalidateChecksums(edits, chunk);
                }
            }
        session.stageBatch(edits);
        return changed;
    }

    public int biome(Region region, int biome, Progress progress) throws IOException {
        region.validateBounds();
        if (region.chunks() > 256)
            throw new IOException("Select at most 256 chunks for a biome edit");
        if (biome < 0 || biome > 65535)
            throw new IOException("Biome ID must be between 0 and 65535");
        Map<byte[], byte[]> edits = new LinkedHashMap<>();
        int done = 0,
                total =
                        (Math.floorDiv(region.x2, 16) - Math.floorDiv(region.x1, 16) + 1)
                                * (Math.floorDiv(region.z2, 16) - Math.floorDiv(region.z1, 16) + 1);
        for (int cx = Math.floorDiv(region.x1, 16); cx <= Math.floorDiv(region.x2, 16); cx++)
            for (int cz = Math.floorDiv(region.z1, 16); cz <= Math.floorDiv(region.z2, 16); cz++) {
                if (!progress.update(++done, total)) throw new java.io.InterruptedIOException();
                WorldChunk chunk = WorldChunk.load(session, cx, cz, region.dimension);
                if (!chunk.exists) throw new IOException("Selection contains an ungenerated chunk");
                if (chunk.data3d == null && biome > 255)
                    throw new IOException("Legacy biome storage only supports IDs up to 255");
                byte[] data =
                        chunk.withBiomes(
                                Math.max(region.x1 - cx * 16, 0),
                                Math.max(region.z1 - cz * 16, 0),
                                Math.min(region.x2 - cx * 16, 15),
                                Math.min(region.z2 - cz * 16, 15),
                                region.y1,
                                region.y2,
                                biome);
                edits.put(
                        ChunkKey.of(cx, cz, region.dimension, chunk.data3d == null ? 0x2d : 0x2b),
                        data);
            }
        session.stageBatch(edits);
        return done;
    }

    public int deleteChunks(Region region, Progress progress) throws IOException {
        region.validateBounds();
        if (region.chunks() > 64)
            throw new IOException("Select at most 64 chunks for one deletion");
        int total = (int) region.chunks();
        Map<byte[], byte[]> edits = new LinkedHashMap<>();
        int done = 0;
        for (int x = Math.floorDiv(region.x1, 16); x <= Math.floorDiv(region.x2, 16); x++)
            for (int z = Math.floorDiv(region.z1, 16); z <= Math.floorDiv(region.z2, 16); z++) {
                if (!progress.update(++done, total)) throw new java.io.InterruptedIOException();
                byte[] after = null;
                do {
                    WorldDatabase.Page page = session.scan(ChunkKey.prefix(x, z), after, 256, "");
                    for (byte[] raw : page.keys) {
                        ChunkKey c = ChunkKey.parse(raw);
                        if (c != null && c.x == x && c.z == z && c.dimension == region.dimension)
                            edits.put(raw, null);
                    }
                    after = page.next;
                } while (after != null);
                byte[] key = WorldRecords.digestKey(x, z, region.dimension),
                        digest = session.get(key);
                if (digest != null) {
                    if (digest.length % 8 != 0) throw new IOException("Invalid entity index");
                    for (int i = 0; i < digest.length; i += 8)
                        edits.put(
                                WorldRecords.actorKey(Arrays.copyOfRange(digest, i, i + 8)), null);
                    edits.put(key, null);
                }
            }
        session.stageBatch(edits);
        return done;
    }

    public byte[] exportStructure(Region region, Progress progress) throws IOException {
        region.validate();
        if ((long) (Math.floorDiv(region.x2, 16) - Math.floorDiv(region.x1, 16) + 1)
                        * (Math.floorDiv(region.z2, 16) - Math.floorDiv(region.z1, 16) + 1)
                > 64) throw new IOException("Select at most 64 chunks for structure extraction");
        List<NbtTag> foreground = new ArrayList<>((int) region.volume()),
                background = new ArrayList<>((int) region.volume()),
                palette = new ArrayList<>(),
                entities = new ArrayList<>();
        Map<String, NbtTag> positions = new LinkedHashMap<>();
        Map<String, Integer> paletteIds = new LinkedHashMap<>();
        java.util.IdentityHashMap<NbtTag, Integer> objectIds = new java.util.IdentityHashMap<>();
        Map<String, WorldChunk> chunks = new LinkedHashMap<>();
        int done = 0;
        for (int x = region.x1; x <= region.x2; x++) {
            if (!progress.update(++done, region.width()))
                throw new java.io.InterruptedIOException();
            for (int y = region.y1; y <= region.y2; y++)
                for (int z = region.z1; z <= region.z2; z++) {
                    int cx = Math.floorDiv(x, 16), cz = Math.floorDiv(z, 16);
                    String key = cx + ":" + cz;
                    WorldChunk chunk = chunks.get(key);
                    if (chunk == null) {
                        chunk = WorldChunk.load(session, cx, cz, region.dimension);
                        if (!chunk.exists)
                            throw new IOException("Selection includes an ungenerated chunk");
                        chunks.put(key, chunk);
                    }
                    if (chunk.legacyTerrain || chunk.legacySubchunks)
                        throw new IOException(
                                "Export legacy worlds through Minecraft before extracting palette"
                                        + " structures");
                    foreground.add(
                            intTag(
                                    "",
                                    paletteIndex(
                                            chunk.blockAt(x & 15, y, z & 15),
                                            palette,
                                            paletteIds,
                                            objectIds)));
                    NbtTag overlay = chunk.layerAt(x & 15, y, z & 15, 1);
                    background.add(
                            intTag(
                                    "",
                                    WorldChunk.isAir(WorldChunk.name(overlay))
                                            ? -1
                                            : paletteIndex(
                                                    overlay, palette, paletteIds, objectIds)));
                }
        }
        for (WorldChunk chunk : chunks.values()) {
            byte[] blockData = session.get(ChunkKey.of(chunk.x, chunk.z, region.dimension, 0x31));
            if (blockData != null)
                for (NbtTag entity : NativeNbt.sequence(blockData)) {
                    WorldRecords.Marker m =
                            WorldRecords.marker(
                                    entity,
                                    region.dimension,
                                    WorldRecords.Marker.BLOCK_ENTITY,
                                    null);
                    if (m == null || !region.contains(m.x, m.y, m.z)) continue;
                    int index =
                            (((int) m.x - region.x1) * region.height() + ((int) m.y - region.y1))
                                            * region.depth()
                                    + ((int) m.z - region.z1);
                    positions.put(
                            Integer.toString(index),
                            compound(
                                    Integer.toString(index),
                                    Map.of(
                                            "block_entity_data",
                                            named(copy(entity), "block_entity_data"))));
                }
            MapSettings settings = new MapSettings();
            settings.dimension = region.dimension;
            settings.entities = true;
            java.util.Set<String> entityRecords = new java.util.HashSet<>();
            for (WorldRecords.Marker marker :
                    WorldRecords.chunkMarkers(session, chunk.x, chunk.z, settings)) {
                if (!entityRecords.add(WorldSession.hex(marker.key))) continue;
                if (!region.contains(marker.x, marker.y, marker.z)) continue;
                byte[] data = session.get(marker.key);
                if (data == null) continue;
                for (NbtTag entity : NativeNbt.sequence(data)) {
                    WorldRecords.Marker m =
                            WorldRecords.marker(entity, region.dimension, 1, marker.key);
                    if (m != null && region.contains(m.x, m.y, m.z)) entities.add(copy(entity));
                }
            }
        }
        NbtTag defaults =
                compound(
                        "default",
                        Map.of(
                                "block_palette",
                                list("block_palette", palette, NbtTag.TAG_COMPOUND),
                                "block_position_data",
                                compound("block_position_data", positions)));
        NbtTag structure =
                compound(
                        "structure",
                        Map.of(
                                "block_indices",
                                list(
                                        "block_indices",
                                        new ArrayList<>(
                                                List.of(
                                                        list("", foreground, NbtTag.TAG_INT),
                                                        list("", background, NbtTag.TAG_INT))),
                                        NbtTag.TAG_LIST),
                                "entities",
                                list("entities", entities, NbtTag.TAG_COMPOUND),
                                "palette",
                                compound("palette", Map.of("default", defaults))));
        NbtTag root =
                compound(
                        "",
                        Map.of(
                                "format_version",
                                intTag("format_version", 1),
                                "size",
                                ints("size", region.width(), region.height(), region.depth()),
                                "structure_world_origin",
                                ints("structure_world_origin", region.x1, region.y1, region.z1),
                                "structure",
                                structure));
        return NativeNbt.encode(root, 0, NativeNbt.LITTLE_ENDIAN, false);
    }

    public void saveStructure(String id, byte[] data) throws IOException {
        if (!id.matches("[a-z0-9_.-]+:[a-zA-Z0-9_./-]+") || id.contains(".."))
            throw new IOException("Use a structure ID such as mystructure:house");
        validateStructure(data);
        session.stage(
                ("structuretemplate_" + id).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                data);
    }

    public static NbtTag validateStructure(byte[] bytes) throws IOException {
        org.levimc.launcher.core.content.nbt.NbtDocument doc =
                NativeNbt.decode(bytes, 0, NativeNbt.LITTLE_ENDIAN);
        NbtTag root = doc.root;
        if (doc.bytesRead != bytes.length
                || root.getType() != NbtTag.TAG_COMPOUND
                || root.getTag("structure") == null
                || root.getTag("size") == null
                || root.getTag("size").getList().size() != 3)
            throw new IOException("File is not a valid .mcstructure");
        List<NbtTag> size = root.getTag("size").getList();
        long volume = 1;
        for (NbtTag n : size) {
            if (n.getInt() < 1 || n.getInt() > 512)
                throw new IOException("Invalid structure dimensions");
            volume *= n.getInt();
        }
        NbtTag structure = root.getTag("structure"),
                indices = structure.getTag("block_indices"),
                p = structure.getTag("palette");
        if (volume > 524288
                || indices == null
                || indices.getList().size() != 2
                || p == null
                || p.getTag("default") == null)
            throw new IOException("Invalid structure palette or size");
        NbtTag palette = p.getTag("default").getTag("block_palette");
        if (palette == null) throw new IOException("Structure block palette is missing");
        for (NbtTag layer : indices.getList()) {
            if (layer.getList().size() != volume)
                throw new IOException("Structure indices do not match its size");
            for (NbtTag n : layer.getList())
                if (n.getInt() < -1 || n.getInt() >= palette.getList().size())
                    throw new IOException("Invalid structure palette index");
        }
        return root;
    }

    public int pasteStructure(
            byte[] data, int startX, int startY, int startZ, int dimension, Progress progress)
            throws IOException {
        NbtTag root = validateStructure(data);
        List<NbtTag> size = root.getTag("size").getList();
        Region region =
                new Region(
                        startX,
                        startY,
                        startZ,
                        startX + size.get(0).getInt() - 1,
                        startY + size.get(1).getInt() - 1,
                        startZ + size.get(2).getInt() - 1,
                        dimension);
        region.validate();
        NbtTag structure = root.getTag("structure"),
                defaults = structure.getTag("palette").getTag("default");
        List<NbtTag> palette = defaults.getTag("block_palette").getList();
        List<NbtTag> indices = structure.getTag("block_indices").getList();
        Map<byte[], byte[]> edits = new LinkedHashMap<>();
        Map<String, WorldChunk> chunks = new LinkedHashMap<>();
        Map<String, BitSet> touched = new LinkedHashMap<>();
        int changed = 0;
        for (int x = 0; x < region.width(); x++) {
            if (!progress.update(x + 1, region.width())) throw new java.io.InterruptedIOException();
            for (int y = 0; y < region.height(); y++)
                for (int z = 0; z < region.depth(); z++) {
                    int wx = startX + x,
                            wy = startY + y,
                            wz = startZ + z,
                            cx = Math.floorDiv(wx, 16),
                            cz = Math.floorDiv(wz, 16),
                            index = (x * region.height() + y) * region.depth() + z;
                    String key = cx + ":" + cz;
                    WorldChunk chunk = chunks.get(key);
                    if (chunk == null) {
                        chunk = WorldChunk.load(session, cx, cz, dimension);
                        if (!chunk.exists)
                            throw new IOException("Destination contains an ungenerated chunk");
                        requireWritable(chunk);
                        chunks.put(key, chunk);
                        touched.put(key, new BitSet());
                    }
                    WorldChunk.Section section = chunk.sections.get(Math.floorDiv(wy, 16));
                    if (section == null) {
                        section = new WorldChunk.Section(Math.floorDiv(wy, 16));
                        chunk.sections.put(section.y, section);
                    }
                    for (int layer = 0; layer < 2; layer++) {
                        int pi = indices.get(layer).getList().get(index).getInt();
                        if (pi < 0) continue;
                        while (section.layers.size() <= layer)
                            section.layers.add(new WorldChunk.Storage());
                        section.layers.get(layer).set(wx & 15, wy & 15, wz & 15, palette.get(pi));
                        changed++;
                        if (layer == 0)
                            touched.get(key)
                                    .set(
                                            ((wy - (dimension == 0 ? -64 : 0)) * 16 + (wz & 15))
                                                            * 16
                                                    + (wx & 15));
                    }
                }
        }
        Map<String, List<NbtTag>> newEntities = new LinkedHashMap<>();
        NbtTag positions = defaults.getTag("block_position_data");
        if (positions != null)
            for (Map.Entry<String, NbtTag> entry : positions.getCompound().entrySet()) {
                int index = Integer.parseInt(entry.getKey());
                if (index < 0 || index >= region.volume())
                    throw new IOException("Invalid structure block entity index");
                NbtTag entity = entry.getValue().getTag("block_entity_data");
                if (entity == null) continue;
                int x = index / (region.height() * region.depth()),
                        y = (index / region.depth()) % region.height(),
                        z = index % region.depth();
                NbtTag copy = copy(entity);
                int wx = startX + x, wy = startY + y, wz = startZ + z;
                copy.putTag("x", intTag("x", wx));
                copy.putTag("y", intTag("y", wy));
                copy.putTag("z", intTag("z", wz));
                newEntities
                        .computeIfAbsent(
                                Math.floorDiv(wx, 16) + ":" + Math.floorDiv(wz, 16),
                                k -> new ArrayList<>())
                        .add(copy);
            }
        for (Map.Entry<String, WorldChunk> entry : chunks.entrySet()) {
            WorldChunk chunk = entry.getValue();
            for (WorldChunk.Section section : chunk.sections.values())
                edits.put(
                        ChunkKey.of(chunk.x, chunk.z, dimension, 0x2f, section.y),
                        WorldChunk.encodeSection(section));
            updateHeightmap(edits, chunk);
            invalidateChecksums(edits, chunk);
            filterBlockEntities(edits, chunk, touched.get(entry.getKey()));
            List<NbtTag> additional = newEntities.get(entry.getKey());
            if (additional != null) {
                byte[] key = ChunkKey.of(chunk.x, chunk.z, dimension, 0x31),
                        value = findEdit(edits, key);
                List<NbtTag> all =
                        value == null
                                ? new ArrayList<>()
                                : new ArrayList<>(NativeNbt.sequence(value));
                all.addAll(additional);
                putEdit(edits, key, NativeNbt.sequenceBytes(all));
            }
        }
        session.stageBatch(edits);
        return changed;
    }

    public int shadeTorches(Region region, Progress progress) throws IOException {
        region.validate();
        Map<byte[], byte[]> edits = new LinkedHashMap<>();
        int count = 0, done = 0;
        int total =
                (Math.floorDiv(region.x2, 16) - Math.floorDiv(region.x1, 16) + 1)
                        * (Math.floorDiv(region.z2, 16) - Math.floorDiv(region.z1, 16) + 1);
        for (int cx = Math.floorDiv(region.x1, 16); cx <= Math.floorDiv(region.x2, 16); cx++)
            for (int cz = Math.floorDiv(region.z1, 16); cz <= Math.floorDiv(region.z2, 16); cz++) {
                if (!progress.update(++done, total)) throw new java.io.InterruptedIOException();
                WorldChunk chunk = WorldChunk.load(session, cx, cz, region.dimension);
                requireWritable(chunk);
                boolean changed = false;
                for (int x = Math.max(region.x1, cx * 16);
                        x <= Math.min(region.x2, cx * 16 + 15);
                        x++)
                    for (int z = Math.max(region.z1, cz * 16);
                            z <= Math.min(region.z2, cz * 16 + 15);
                            z++)
                        for (int y = region.y1; y < region.y2; y++) {
                            if (!WorldChunk.name(chunk.blockAt(x & 15, y, z & 15)).contains("torch")
                                    || !WorldChunk.isAir(
                                            WorldChunk.name(chunk.blockAt(x & 15, y + 1, z & 15))))
                                continue;
                            int sy = Math.floorDiv(y + 1, 16);
                            WorldChunk.Section s = chunk.sections.get(sy);
                            if (s == null) {
                                s = new WorldChunk.Section(sy);
                                chunk.sections.put(sy, s);
                            }
                            s.layers
                                    .get(0)
                                    .set(
                                            x & 15,
                                            (y + 1) & 15,
                                            z & 15,
                                            WorldChunk.block("minecraft:glass"));
                            changed = true;
                            count++;
                        }
                if (changed) {
                    for (WorldChunk.Section s : chunk.sections.values())
                        edits.put(
                                ChunkKey.of(cx, cz, region.dimension, 0x2f, s.y),
                                WorldChunk.encodeSection(s));
                    updateHeightmap(edits, chunk);
                    invalidateChecksums(edits, chunk);
                }
            }
        session.stageBatch(edits);
        return count;
    }

    private static void requireWritable(WorldChunk chunk) throws IOException {
        if (chunk.legacyTerrain || chunk.legacySubchunks)
            throw new IOException(
                    "Legacy terrain can be viewed; open and save it in Minecraft before palette"
                            + " editing");
        for (WorldChunk.Section section : chunk.sections.values())
            for (WorldChunk.Storage storage : section.layers)
                for (NbtTag block : storage.palette)
                    if (WorldChunk.name(block).startsWith("legacy:"))
                        throw new IOException(
                                "Unmapped legacy block state; upgrade this world in Minecraft"
                                        + " before editing blocks");
    }

    private void filterBlockEntities(Map<byte[], byte[]> edits, WorldChunk chunk, BitSet touched)
            throws IOException {
        byte[] key = ChunkKey.of(chunk.x, chunk.z, chunk.dimension, 0x31), data = session.get(key);
        if (data == null) return;
        List<NbtTag> keep = new ArrayList<>();
        for (NbtTag entity : NativeNbt.sequence(data)) {
            WorldRecords.Marker marker = WorldRecords.marker(entity, chunk.dimension, 2, key);
            boolean remove = false;
            if (marker != null) {
                int localY = (int) marker.y - (chunk.dimension == 0 ? -64 : 0);
                if (localY >= 0 && localY < 512)
                    remove =
                            touched.get(
                                    (localY * 16 + ((int) marker.z & 15)) * 16
                                            + ((int) marker.x & 15));
            }
            if (!remove) keep.add(entity);
        }
        putEdit(edits, key, keep.isEmpty() ? null : NativeNbt.sequenceBytes(keep));
    }

    private static void updateHeightmap(Map<byte[], byte[]> edits, WorldChunk chunk) {
        byte[] data = chunk.data3d != null ? chunk.data3d : chunk.data2d;
        if (data == null || data.length < 512) return;
        byte[] updated = data.clone();
        ByteBuffer b = ByteBuffer.wrap(updated).order(ByteOrder.LITTLE_ENDIAN);
        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++)
                b.putShort(
                        (z * 16 + x) * 2,
                        (short)
                                Math.max(
                                        0,
                                        chunk.top(x, z, true)
                                                + 1
                                                - (chunk.dimension == 0 && chunk.data3d != null
                                                        ? -64
                                                        : 0)));
        putEdit(
                edits,
                ChunkKey.of(chunk.x, chunk.z, chunk.dimension, chunk.data3d != null ? 0x2b : 0x2d),
                updated);
    }

    private static void invalidateChecksums(Map<byte[], byte[]> edits, WorldChunk chunk) {
        putEdit(edits, ChunkKey.of(chunk.x, chunk.z, chunk.dimension, 0x3b), null);
    }

    private static int paletteIndex(
            NbtTag block,
            List<NbtTag> palette,
            Map<String, Integer> ids,
            java.util.IdentityHashMap<NbtTag, Integer> objects)
            throws IOException {
        Integer cached = objects.get(block);
        if (cached != null) return cached;
        String name = NativeNbt.toSnbt(block);
        Integer id = ids.get(name);
        if (id == null) {
            id = palette.size();
            palette.add(copy(block));
            ids.put(name, id);
        }
        objects.put(block, id);
        return id;
    }

    private static byte[] findEdit(Map<byte[], byte[]> edits, byte[] key) {
        for (Map.Entry<byte[], byte[]> e : edits.entrySet())
            if (Arrays.equals(e.getKey(), key)) return e.getValue();
        return null;
    }

    private static void putEdit(Map<byte[], byte[]> edits, byte[] key, byte[] value) {
        byte[] old = null;
        for (byte[] k : edits.keySet())
            if (Arrays.equals(k, key)) {
                old = k;
                break;
            }
        if (old != null) edits.remove(old);
        edits.put(key, value);
    }

    public static NbtTag intTag(String name, int value) {
        return new NbtTag(NbtTag.TAG_INT, name, value);
    }

    public static NbtTag compound(String name, Map<String, NbtTag> values) {
        return new NbtTag(NbtTag.TAG_COMPOUND, name, new LinkedHashMap<>(values));
    }

    public static NbtTag list(String name, List<NbtTag> values, byte type) {
        return new NbtTag(NbtTag.TAG_LIST, name, values, type);
    }

    public static NbtTag ints(String name, int... values) {
        List<NbtTag> result = new ArrayList<>();
        for (int i : values) result.add(intTag("", i));
        return list(name, result, NbtTag.TAG_INT);
    }

    public static NbtTag named(NbtTag tag, String name) {
        tag.setName(name);
        return tag;
    }
}
