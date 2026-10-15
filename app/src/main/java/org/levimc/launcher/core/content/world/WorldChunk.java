package org.levimc.launcher.core.content.world;

import org.levimc.launcher.core.content.leveldb.WorldDatabase;
import org.levimc.launcher.core.content.nbt.NativeNbt;
import org.levimc.launcher.core.content.nbt.NbtDocument;
import org.levimc.launcher.core.content.nbt.NbtTag;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class WorldChunk {
    public final int x, z, dimension;
    public final TreeMap<Integer, Section> sections = new TreeMap<>();
    public byte[] data2d, data3d;
    public boolean exists;
    public boolean legacyTerrain;
    public boolean legacySubchunks;
    public String error;
    private final Map<Integer, int[]> biomes = new TreeMap<>();
    public static final NbtTag AIR = block("minecraft:air");

    public static final class Storage {
        public final int[] indices = new int[4096];
        public final List<NbtTag> palette = new ArrayList<>();
        private Map<String, Integer> paletteLookup;
        private NbtTag lastBlock;
        private int lastIndex;

        public Storage() {
            palette.add(AIR);
        }

        public NbtTag at(int x, int y, int z) {
            return palette.get(indices[(x << 8) | (z << 4) | y]);
        }

        public void set(int x, int y, int z, NbtTag block) throws IOException {
            if (block == lastBlock) {
                indices[(x << 8) | (z << 4) | y] = lastIndex;
                return;
            }
            if (paletteLookup == null) {
                paletteLookup = new LinkedHashMap<>();
                for (int i = 0; i < palette.size(); i++)
                    paletteLookup.put(NativeNbt.toSnbt(palette.get(i)), i);
            }
            String id = NativeNbt.toSnbt(block);
            Integer index = paletteLookup.get(id);
            if (index == null) {
                index = palette.size();
                palette.add(block);
                paletteLookup.put(id, index);
            }
            lastBlock = block;
            lastIndex = index;
            indices[(x << 8) | (z << 4) | y] = index;
        }
    }

    public static final class Section {
        public int y;
        public final List<Storage> layers = new ArrayList<>();
        public byte[] blockLight;
        public byte[] skyLight;

        public Section(int y) {
            this.y = y;
            layers.add(new Storage());
        }

        public NbtTag at(int x, int y, int z) {
            return layers.get(0).at(x, y, z);
        }
    }

    private WorldChunk(int x, int z, int dimension) {
        this.x = x;
        this.z = z;
        this.dimension = dimension;
    }

    public static WorldChunk load(WorldSession session, int x, int z, int dimension)
            throws IOException {
        WorldChunk chunk = new WorldChunk(x, z, dimension);
        byte[] after = null;
        do {
            WorldDatabase.Page page = session.scan(ChunkKey.prefix(x, z), after, 128, "");
            for (byte[] raw : page.keys) {
                ChunkKey key = ChunkKey.parse(raw);
                if (key == null || key.dimension != dimension || key.x != x || key.z != z) continue;
                byte[] data = session.get(raw);
                if (data == null) continue;
                chunk.exists = true;
                if (key.tag == 0x2f) {
                    chunk.sections.put(key.section, decodeSection(data, key.section));
                    chunk.legacySubchunks |= (data[0] & 255) < 8;
                } else if (key.tag == 0x2d || key.tag == 0x2e) chunk.data2d = data;
                else if (key.tag == 0x2b) chunk.data3d = data;
                else if (key.tag == 0x30) chunk.decodeLegacy(data);
            }
            after = page.next;
        } while (after != null);
        for (byte[] key : session.stagedKeys(ChunkKey.prefix(x, z))) {
            ChunkKey c = ChunkKey.parse(key);
            if (c == null || c.dimension != dimension) continue;
            byte[] data = session.get(key);
            if (c.tag == 0x2f) {
                chunk.sections.put(c.section, decodeSection(data, c.section));
                chunk.exists = true;
            } else if (c.tag == 0x2b) chunk.data3d = data;
            else if (c.tag == 0x2d) chunk.data2d = data;
        }
        chunk.decodeBiomes();
        return chunk;
    }

    public static Section decodeSection(byte[] data, int keyY) throws IOException {
        try {
            ByteBuffer b = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            int version = b.get() & 255;
            Section s = new Section(keyY);
            s.layers.clear();
            if (version <= 1) {
                if (b.remaining() < 6144) throw new IOException("Truncated legacy subchunk");
                byte[] ids = new byte[4096], meta = new byte[2048];
                b.get(ids);
                b.get(meta);
                Storage st = new Storage();
                st.palette.clear();
                Map<Integer, Integer> palette = new LinkedHashMap<>();
                for (int i = 0; i < 4096; i++) {
                    int id = (ids[i] & 255) << 4 | nibble(meta, i);
                    Integer index = palette.get(id);
                    if (index == null) {
                        index = st.palette.size();
                        st.palette.add(legacyBlock(id >>> 4, id & 15));
                        palette.put(id, index);
                    }
                    st.indices[i] = index;
                }
                s.layers.add(st);
                if (b.remaining() >= 4096) {
                    s.skyLight = new byte[2048];
                    b.get(s.skyLight);
                    s.blockLight = new byte[2048];
                    b.get(s.blockLight);
                }
                return s;
            }
            if (version != 8 && version != 9)
                throw new IOException("Unsupported subchunk version " + version);
            int count = b.get() & 255;
            if (version == 9 && b.get() != keyY)
                throw new IOException("Subchunk Y does not match its database key");
            if (count > 8) throw new IOException("Invalid block storage count");
            if (count == 0) {
                if (b.hasRemaining()) throw new IOException("Unexpected empty subchunk data");
                s.layers.add(new Storage());
                return s;
            }
            for (int layer = 0; layer < count; layer++) {
                Storage st = new Storage();
                st.palette.clear();
                int header = b.get() & 255, bits = header >>> 1;
                if ((header & 1) != 0)
                    throw new IOException(
                            "Runtime block palettes cannot be decoded without the matching game"
                                    + " palette");
                int[] indices = unpack(b, bits);
                int paletteSize = bits == 0 ? 1 : b.getInt();
                if (paletteSize < 1 || paletteSize > 65536 || bits == 0 && paletteSize != 1)
                    throw new IOException("Invalid block palette size");
                for (int i = 0; i < paletteSize; i++) {
                    NbtDocument doc = NativeNbt.decode(data, b.position(), NativeNbt.LITTLE_ENDIAN);
                    if (doc.root.getType() != NbtTag.TAG_COMPOUND
                            || doc.root.getTag("name") == null)
                        throw new IOException("Invalid persistent block palette entry");
                    st.palette.add(doc.root);
                    b.position(b.position() + doc.bytesRead);
                }
                for (int index : indices)
                    if (index < 0 || index >= paletteSize)
                        throw new IOException("Block index is outside its palette");
                System.arraycopy(indices, 0, st.indices, 0, 4096);
                s.layers.add(st);
            }
            if (b.hasRemaining()) throw new IOException("Unrecognized trailing subchunk data");
            return s;
        } catch (java.nio.BufferUnderflowException | IllegalArgumentException ex) {
            throw new IOException("Invalid subchunk data", ex);
        }
    }

    public static int[] unpack(ByteBuffer b, int bits) throws IOException {
        if (bits != 0
                && bits != 1
                && bits != 2
                && bits != 3
                && bits != 4
                && bits != 5
                && bits != 6
                && bits != 8
                && bits != 16) throw new IOException("Unsupported palette bit width " + bits);
        int[] result = new int[4096];
        if (bits == 0) return result;
        int perWord = 32 / bits, words = (4096 + perWord - 1) / perWord;
        if (b.remaining() < words * 4) throw new IOException("Truncated palette indices");
        int mask = (1 << bits) - 1;
        for (int w = 0, pos = 0; w < words; w++) {
            int value = b.getInt();
            for (int i = 0; i < perWord && pos < 4096; i++, pos++)
                result[pos] = (value >>> (i * bits)) & mask;
        }
        return result;
    }

    public static byte[] encodeSection(Section section) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(9);
        out.write(section.layers.size());
        out.write(section.y);
        for (Storage storage : section.layers) {
            Map<Integer, Integer> remap = new LinkedHashMap<>();
            int[] values = new int[4096];
            for (int i = 0; i < 4096; i++) {
                Integer index = remap.get(storage.indices[i]);
                if (index == null) {
                    index = remap.size();
                    remap.put(storage.indices[i], index);
                }
                values[i] = index;
            }
            int count = remap.size(), bits = bits(count);
            out.write(bits << 1);
            pack(out, values, bits);
            if (bits != 0) integer(out, count);
            for (int index : remap.keySet())
                out.write(
                        NativeNbt.encode(
                                storage.palette.get(index), 0, NativeNbt.LITTLE_ENDIAN, false));
        }
        return out.toByteArray();
    }

    public static int bits(int count) {
        for (int n : new int[] {0, 1, 2, 3, 4, 5, 6, 8, 16}) if (count <= (1 << n)) return n;
        throw new IllegalArgumentException("Palette is too large");
    }

    public static void pack(ByteArrayOutputStream out, int[] values, int bits) {
        if (bits == 0) return;
        int perWord = 32 / bits;
        for (int pos = 0; pos < 4096; ) {
            int word = 0;
            for (int i = 0; i < perWord && pos < 4096; i++, pos++)
                word |= values[pos] << (i * bits);
            integer(out, word);
        }
    }

    public static void integer(ByteArrayOutputStream out, int value) {
        out.write(value);
        out.write(value >>> 8);
        out.write(value >>> 16);
        out.write(value >>> 24);
    }

    private void decodeLegacy(byte[] data) throws IOException {
        if (data.length < 32768 + 16384) throw new IOException("Truncated legacy terrain");
        legacyTerrain = true;
        for (int sy = 0; sy < 8; sy++) {
            Section section = new Section(sy);
            Storage storage = section.layers.get(0);
            storage.palette.clear();
            Map<Integer, Integer> palette = new LinkedHashMap<>();
            for (int x = 0; x < 16; x++)
                for (int z = 0; z < 16; z++)
                    for (int y = 0; y < 16; y++) {
                        int pos = (x << 11) | (z << 7) | (sy * 16 + y),
                                block = ((data[pos] & 255) << 4) | nibble(data, 32768, pos);
                        Integer index = palette.get(block);
                        if (index == null) {
                            index = palette.size();
                            palette.put(block, index);
                            storage.palette.add(legacyBlock(block >>> 4, block & 15));
                        }
                        storage.indices[(x << 8) | (z << 4) | y] = index;
                    }
            if (data.length >= 81920) {
                section.skyLight = new byte[2048];
                section.blockLight = new byte[2048];
                for (int x = 0; x < 16; x++)
                    for (int z = 0; z < 16; z++)
                        for (int y = 0; y < 16; y++) {
                            int pos = (x << 11) | (z << 7) | (sy * 16 + y),
                                    local = (x << 8) | (z << 4) | y;
                            setNibble(section.skyLight, local, nibble(data, 49152, pos));
                            setNibble(section.blockLight, local, nibble(data, 65536, pos));
                        }
            }
            sections.putIfAbsent(sy, section);
        }
    }

    private void decodeBiomes() throws IOException {
        if (data3d == null || data3d.length <= 512) return;
        try {
            ByteBuffer b = ByteBuffer.wrap(data3d).order(ByteOrder.LITTLE_ENDIAN);
            b.position(512);
            int min = dimension == 0 ? -4 : 0;
            int[] previous = null;
            for (int sy = min; b.hasRemaining() && sy < 32; sy++) {
                int header = b.get() & 255;
                if (header == 255) {
                    if (previous == null)
                        throw new IOException(
                                "Biome storage references a missing previous section");
                    biomes.put(sy, previous);
                    continue;
                }
                int[] index = unpack(b, header >>> 1);
                int size = (header >>> 1) == 0 ? 1 : b.getInt();
                if (size < 1 || size > 65536) throw new IOException("Invalid biome palette");
                int[] palette = new int[size];
                for (int i = 0; i < size; i++) palette[i] = b.getInt();
                int[] values = new int[4096];
                for (int i = 0; i < values.length; i++) {
                    if (index[i] >= size) throw new IOException("Biome palette index out of range");
                    values[i] = palette[index[i]];
                }
                biomes.put(sy, values);
                previous = values;
            }
            if (b.hasRemaining()) throw new IOException("Unsupported biome height range");
        } catch (java.nio.BufferUnderflowException ex) {
            throw new IOException("Truncated 3D biome storage", ex);
        }
    }

    public int biome(int x, int y, int z) {
        int[] storage = biomes.get(Math.floorDiv(y, 16));
        if (storage != null) return storage[(x << 8) | (z << 4) | (y & 15)];
        if (data2d != null && data2d.length >= 768) return data2d[512 + z * 16 + x] & 255;
        return -1;
    }

    public byte[] withBiomes(int minX, int minZ, int maxX, int maxZ, int minY, int maxY, int biome)
            throws IOException {
        if (data3d == null || data3d.length < 512) {
            if (data2d == null || data2d.length < 768)
                throw new IOException("This chunk has no editable biome storage");
            byte[] copy = data2d.clone();
            for (int x = minX; x <= maxX; x++)
                for (int z = minZ; z <= maxZ; z++) copy[512 + z * 16 + x] = (byte) biome;
            return copy;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(data3d, 0, 512);
        if (biomes.isEmpty()) throw new IOException("No decoded biome storage");
        for (Map.Entry<Integer, int[]> section : biomes.entrySet()) {
            int[] values = section.getValue().clone();
            int sy = section.getKey();
            for (int x = minX; x <= maxX; x++)
                for (int z = minZ; z <= maxZ; z++)
                    for (int y = Math.max(minY, sy * 16); y <= Math.min(maxY, sy * 16 + 15); y++)
                        values[(x << 8) | (z << 4) | (y & 15)] = biome;
            boolean unknown = true;
            for (int value : values)
                if (value >= 0) {
                    unknown = false;
                    break;
                }
            if (unknown) {
                out.write(255);
                continue;
            }
            Map<Integer, Integer> palette = new LinkedHashMap<>();
            int[] indices = new int[4096];
            for (int i = 0; i < 4096; i++) {
                Integer n = palette.get(values[i]);
                if (n == null) {
                    n = palette.size();
                    palette.put(values[i], n);
                }
                indices[i] = n;
            }
            int bits = bits(palette.size());
            out.write(bits << 1);
            pack(out, indices, bits);
            if (bits != 0) integer(out, palette.size());
            for (int id : palette.keySet()) integer(out, id);
        }
        return out.toByteArray();
    }

    public NbtTag blockAt(int x, int y, int z) {
        Section s = sections.get(Math.floorDiv(y, 16));
        return s == null ? AIR : s.at(x, y & 15, z);
    }

    public NbtTag layerAt(int x, int y, int z, int layer) {
        Section s = sections.get(Math.floorDiv(y, 16));
        return s == null || s.layers.size() <= layer ? AIR : s.layers.get(layer).at(x, y & 15, z);
    }

    public int top(int x, int z, boolean leaves) {
        for (Section section : sections.descendingMap().values())
            for (int y = 15; y >= 0; y--) {
                String name = name(section.at(x, y, z));
                if (!isAir(name) && (leaves || !name.contains("leaves"))) return section.y * 16 + y;
            }
        return dimension == 0 ? -65 : -1;
    }

    public int minY() {
        return sections.isEmpty() ? dimension == 0 ? -64 : 0 : sections.firstKey() * 16;
    }

    public int maxY() {
        return sections.isEmpty() ? 319 : sections.lastKey() * 16 + 15;
    }

    public int storedLight(int x, int y, int z, boolean sky) {
        Section s = sections.get(Math.floorDiv(y, 16));
        if (s == null) return -1;
        byte[] light = sky ? s.skyLight : s.blockLight;
        return light == null ? -1 : nibble(light, (x << 8) | (z << 4) | (y & 15));
    }

    public static String name(NbtTag block) {
        NbtTag tag = block == null ? null : block.getTag("name");
        return tag == null ? "minecraft:unknown" : tag.getString();
    }

    public static boolean isAir(String name) {
        return name.equals("minecraft:air")
                || name.equals("minecraft:cave_air")
                || name.equals("minecraft:void_air");
    }

    public static NbtTag block(String name) {
        Map<String, NbtTag> c = new LinkedHashMap<>();
        c.put("name", new NbtTag(NbtTag.TAG_STRING, "name", name));
        c.put(
                "states",
                new NbtTag(NbtTag.TAG_COMPOUND, "states", new LinkedHashMap<String, NbtTag>()));
        c.put("version", new NbtTag(NbtTag.TAG_INT, "version", 18168865));
        return new NbtTag(NbtTag.TAG_COMPOUND, "", c);
    }

    private static NbtTag legacyBlock(int id, int data) {
        String name = LegacyBlocks.name(id, data);
        if (name != null) return block(name);
        Map<String, NbtTag> c = new LinkedHashMap<>();
        c.put("name", new NbtTag(NbtTag.TAG_STRING, "name", "legacy:" + id));
        c.put("val", new NbtTag(NbtTag.TAG_SHORT, "val", (short) data));
        return new NbtTag(NbtTag.TAG_COMPOUND, "", c);
    }

    public static int nibble(byte[] data, int i) {
        return nibble(data, 0, i);
    }

    public static int nibble(byte[] data, int offset, int i) {
        return (data[offset + (i >>> 1)] >>> ((i & 1) * 4)) & 15;
    }

    private static void setNibble(byte[] data, int i, int n) {
        int p = i >>> 1, shift = (i & 1) * 4;
        data[p] = (byte) ((data[p] & ~(15 << shift)) | (n << shift));
    }
}
