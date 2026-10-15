package org.levimc.launcher.core.content.world;

import org.levimc.launcher.core.content.nbt.NativeNbt;
import org.levimc.launcher.core.content.nbt.NbtDocument;
import org.levimc.launcher.core.content.nbt.NbtTag;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WorldRecords {
    public enum Group {
        ALL,
        SINGLEPLAYER,
        MULTIPLAYER,
        STRUCTURES,
        BIOME_DATA,
        OVERWORLD,
        NETHER,
        END,
        VILLAGES,
        OLD_VILLAGES,
        PORTALS,
        AUTONOMOUS_ENTITIES,
        MAPS,
        WORLD_CLOCK,
        SCHEDULER,
        POSITION_TRACKING,
        ACTORS,
        CHUNKS
    }

    private WorldRecords() {}

    public static byte[] prefix(Group group) {
        String p =
                switch (group) {
                    case SINGLEPLAYER -> "~local_player";
                    case STRUCTURES -> "structuretemplate_";
                    case ACTORS -> "actorprefix";
                    case MAPS -> "map_";
                    default -> "";
                };
        return p.getBytes(StandardCharsets.UTF_8);
    }

    public static boolean matches(byte[] key, Group group) {
        String name = new String(key, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        ChunkKey chunk = ChunkKey.parse(key);
        return switch (group) {
            case ALL -> true;
            case SINGLEPLAYER -> name.equals("~local_player");
            case MULTIPLAYER -> name.startsWith("player_") || name.startsWith("player_server_");
            case STRUCTURES -> name.startsWith("structuretemplate_");
            case BIOME_DATA -> name.contains("biomedata");
            case OVERWORLD -> name.equals("overworld") || name.equals("dimension0");
            case NETHER -> name.equals("nether") || name.equals("dimension1");
            case END ->
                    name.equals("theend") || name.equals("the_end") || name.equals("dimension2");
            case VILLAGES -> name.startsWith("village_") || name.equals("villages");
            case OLD_VILLAGES -> name.contains("village") && !name.startsWith("village_");
            case PORTALS -> name.contains("portal");
            case AUTONOMOUS_ENTITIES -> name.contains("autonomous") || name.contains("mobevents");
            case MAPS -> name.startsWith("map_");
            case WORLD_CLOCK -> name.contains("worldclock");
            case SCHEDULER -> name.contains("scheduler") || name.contains("scheduled");
            case POSITION_TRACKING -> name.contains("postrack") || name.contains("positiontrack");
            case ACTORS -> name.startsWith("actorprefix");
            case CHUNKS -> chunk != null;
        };
    }

    public static String label(byte[] key) {
        ChunkKey c = ChunkKey.parse(key);
        if (c != null)
            return c.x
                    + ", "
                    + c.z
                    + "  ·  "
                    + dimensionName(c.dimension)
                    + "  ·  "
                    + String.format(Locale.ROOT, "0x%02x", c.tag)
                    + (c.subChunk ? " / Y " + c.section : "");
        boolean printable = true;
        for (byte b : key)
            if ((b & 255) < 32 || b == 127) {
                printable = false;
                break;
            }
        if (printable) return new String(key, StandardCharsets.UTF_8);
        String name = new String(key, StandardCharsets.UTF_8);
        if (name.startsWith("actorprefix"))
            return "Actor · " + WorldSession.hex(java.util.Arrays.copyOfRange(key, 11, key.length));
        if (name.startsWith("digp"))
            return "Actor index · "
                    + WorldSession.hex(java.util.Arrays.copyOfRange(key, 4, key.length));
        return "Hex · " + WorldSession.hex(key);
    }

    public static String dimensionName(int d) {
        return d == 0 ? "Overworld" : d == 1 ? "Nether" : d == 2 ? "The End" : "Dimension " + d;
    }

    public static final class Document {
        public NbtTag root;
        public final boolean sequence;
        public final NbtDocument file;

        public Document(NbtTag root, boolean sequence, NbtDocument file) {
            this.root = root;
            this.sequence = sequence;
            this.file = file;
        }

        public byte[] bytes() throws IOException {
            if (sequence) {
                if (root.getType() != NbtTag.TAG_LIST)
                    throw new IOException("An NBT sequence must remain a list of roots");
                return NativeNbt.sequenceBytes(root.getList());
            }
            return NativeNbt.encode(root, file.headerVersion, file.format, file.compressed);
        }
    }

    public static Document decode(byte[] bytes) throws IOException {
        NbtDocument doc = NativeNbt.readAuto(bytes);
        if (doc.bytesRead == bytes.length || doc.compressed || doc.format == NativeNbt.LEVEL_DAT)
            return new Document(doc.root, false, doc);
        List<NbtTag> roots = NativeNbt.sequence(bytes);
        return new Document(
                new NbtTag(NbtTag.TAG_LIST, "records", roots, roots.get(0).getType()), true, doc);
    }

    public static final class Marker {
        public static final int PLAYER = 0, ENTITY = 1, BLOCK_ENTITY = 2, BOOKMARK = 3;
        public final String label;
        public final double x, y, z;
        public final int dimension, kind;
        public final byte[] key;

        public Marker(
                String label, double x, double y, double z, int dimension, int kind, byte[] key) {
            this.label = label;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
            this.kind = kind;
            this.key = key;
        }
    }

    private static double number(NbtTag c, String key, double fallback) {
        NbtTag t = c.getTag(key);
        return t == null ? fallback : t.getDouble();
    }

    public static Marker marker(NbtTag root, int dimension, int kind, byte[] key) {
        String label = "";
        for (String field : new String[] {"CustomName", "identifier", "id", "NameTag", "name"}) {
            NbtTag t = root.getTag(field);
            if (t != null && t.getType() == NbtTag.TAG_STRING && !t.getString().isEmpty()) {
                label = t.getString();
                break;
            }
        }
        if (label.isEmpty()) label = kind == Marker.PLAYER ? "Player" : "Entity";
        NbtTag pos = root.getTag("Pos");
        if (pos != null && pos.getList().size() >= 3) {
            List<NbtTag> p = pos.getList();
            int dim = (int) number(root, "DimensionId", dimension);
            return new Marker(
                    label,
                    p.get(0).getDouble(),
                    p.get(1).getDouble(),
                    p.get(2).getDouble(),
                    dim,
                    kind,
                    key);
        }
        if (root.getTag("x") != null && root.getTag("z") != null)
            return new Marker(
                    label,
                    number(root, "x", 0),
                    number(root, "y", 0),
                    number(root, "z", 0),
                    dimension,
                    kind,
                    key);
        return null;
    }

    public static List<Marker> players(WorldSession session) throws IOException {
        List<Marker> result = new ArrayList<>();
        List<byte[]> keys = new ArrayList<>();
        byte[] local = "~local_player".getBytes(StandardCharsets.UTF_8);
        if (session.get(local) != null) keys.add(local);
        keys.addAll(session.keys("player_", 2048));
        for (byte[] key : keys) {
            byte[] data = session.get(key);
            if (data == null) continue;
            try {
                Marker m = marker(decode(data).root, 0, Marker.PLAYER, key);
                if (m != null)
                    result.add(new Marker(label(key), m.x, m.y, m.z, m.dimension, m.kind, key));
            } catch (IOException ignored) {
            }
        }
        return result;
    }

    public static List<Marker> chunkMarkers(
            WorldSession session, int cx, int cz, MapSettings options) throws IOException {
        List<Marker> result = new ArrayList<>();
        if (options.blockEntities)
            addSequenceMarkers(
                    result,
                    session,
                    ChunkKey.of(cx, cz, options.dimension, 0x31),
                    options.dimension,
                    Marker.BLOCK_ENTITY,
                    options.blockEntityFilter);
        if (options.entities) {
            addSequenceMarkers(
                    result,
                    session,
                    ChunkKey.of(cx, cz, options.dimension, 0x32),
                    options.dimension,
                    Marker.ENTITY,
                    options.entityFilter);
            byte[] digestKey = digestKey(cx, cz, options.dimension),
                    digest = session.get(digestKey);
            if (digest != null) {
                if (digest.length % 8 != 0) throw new IOException("Invalid modern entity index");
                for (int i = 0; i < digest.length && result.size() < 512; i += 8) {
                    byte[] key = actorKey(java.util.Arrays.copyOfRange(digest, i, i + 8)),
                            data = session.get(key);
                    if (data == null) continue;
                    try {
                        Marker m = marker(decode(data).root, options.dimension, Marker.ENTITY, key);
                        if (m != null && filter(m.label, options.entityFilter)) result.add(m);
                    } catch (IOException ignored) {
                    }
                }
            }
        }
        return result;
    }

    private static void addSequenceMarkers(
            List<Marker> out,
            WorldSession session,
            byte[] key,
            int dimension,
            int kind,
            String filter)
            throws IOException {
        byte[] data = session.get(key);
        if (data == null) return;
        try {
            for (NbtTag root : NativeNbt.sequence(data)) {
                Marker marker = marker(root, dimension, kind, key);
                if (marker != null && filter(marker.label, filter)) out.add(marker);
                if (out.size() >= 512) break;
            }
        } catch (IOException ignored) {
        }
    }

    public static boolean filter(String value, String filter) {
        if (filter == null || filter.trim().isEmpty()) return true;
        String lower = value.toLowerCase(Locale.ROOT);
        for (String part : filter.toLowerCase(Locale.ROOT).split(","))
            if (!part.trim().isEmpty() && lower.contains(part.trim())) return true;
        return false;
    }

    public static byte[] digestKey(int x, int z, int dimension) {
        ByteBuffer b = ByteBuffer.allocate(dimension == 0 ? 12 : 16).order(ByteOrder.LITTLE_ENDIAN);
        b.put(new byte[] {'d', 'i', 'g', 'p'}).putInt(x).putInt(z);
        if (dimension != 0) b.putInt(dimension);
        return b.array();
    }

    public static byte[] actorKey(byte[] id) {
        byte[] key = new byte[11 + id.length];
        System.arraycopy("actorprefix".getBytes(StandardCharsets.UTF_8), 0, key, 0, 11);
        System.arraycopy(id, 0, key, 11, id.length);
        return key;
    }
}
