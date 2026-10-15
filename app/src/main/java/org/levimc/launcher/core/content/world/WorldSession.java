package org.levimc.launcher.core.content.world;

import android.util.AtomicFile;

import org.json.JSONArray;
import org.json.JSONObject;
import org.levimc.launcher.core.content.leveldb.WorldDatabase;
import org.levimc.launcher.core.content.nbt.NativeNbt;
import org.levimc.launcher.core.content.nbt.NbtDocument;
import org.levimc.launcher.core.content.nbt.NbtTag;
import org.levimc.launcher.core.minecraft.MinecraftActivityState;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class WorldSession implements AutoCloseable {
    public final File world;
    private WorldDatabase database;
    private volatile byte[] initialMetadata;
    private volatile byte[] metadata;
    private final Map<String, Change> changes = new LinkedHashMap<>();
    private volatile long revision;
    private volatile int changedRecords;
    private volatile boolean metadataChanged;
    private long stagedBytes;
    private boolean initialized;
    private volatile boolean suspended;
    private volatile boolean closed;
    private static final long MAX_EDIT_BYTES = 128L * 1024 * 1024;

    private static final class Change {
        final byte[] key;
        final byte[] before;
        final byte[] after;

        Change(byte[] key, byte[] before, byte[] after) {
            this.key = key.clone();
            this.before = before;
            this.after = after;
        }
    }

    public WorldSession(File world) {
        this.world = world;
    }

    public synchronized void initialize() throws IOException {
        requireAvailable();
        if (!world.isDirectory()) throw new IOException("World folder was not found");
        db();
        recoverInterruptedSave();
        if (!initialized) {
            initialMetadata = readFile(new File(world, "level.dat"));
            metadata = initialMetadata;
            if (metadata == null) throw new IOException("level.dat was not found");
            NativeNbt.decode(metadata, 0, NativeNbt.LEVEL_DAT);
            initialized = true;
        }
    }

    private void requireAvailable() throws IOException {
        if (closed || suspended || Thread.currentThread().isInterrupted())
            throw new java.io.InterruptedIOException("World workspace is paused");
        if (MinecraftActivityState.isRunning())
            throw new IOException("Close Minecraft before opening or editing a world");
    }

    private WorldDatabase db() throws IOException {
        requireAvailable();
        if (database == null) database = new WorldDatabase(world);
        return database;
    }

    public long revision() {
        return revision;
    }

    public int pendingCount() {
        return changedRecords + (metadataChanged ? 1 : 0);
    }

    public synchronized NbtDocument metadata() throws IOException {
        requireAvailable();
        return NativeNbt.decode(metadata, 0, NativeNbt.LEVEL_DAT);
    }

    public synchronized void stageMetadata(NbtDocument doc) throws IOException {
        requireAvailable();
        if (doc.root.getType() != NbtTag.TAG_COMPOUND)
            throw new IOException("World metadata must be a compound");
        metadata = doc.encode();
        metadataChanged = !Arrays.equals(metadata, initialMetadata);
        revision++;
    }

    public synchronized byte[] get(byte[] key) throws IOException {
        requireAvailable();
        Change staged = changes.get(hex(key));
        return staged == null ? db().get(key) : staged.after;
    }

    public byte[] get(String key) throws IOException {
        return get(key.getBytes(StandardCharsets.UTF_8));
    }

    public synchronized WorldDatabase.Page scan(
            byte[] prefix, byte[] after, int limit, String contains) throws IOException {
        return db().scan(prefix, after, limit, contains);
    }

    public synchronized List<byte[]> stagedKeys(byte[] prefix) {
        List<byte[]> result = new ArrayList<>();
        for (Change c : changes.values()) {
            if (c.before != null || c.after == null || c.key.length < prefix.length) continue;
            boolean matches = true;
            for (int i = 0; i < prefix.length; i++)
                if (c.key[i] != prefix[i]) {
                    matches = false;
                    break;
                }
            if (matches) result.add(c.key);
        }
        return result;
    }

    public synchronized List<byte[]> keys(String prefix, int maximum) throws IOException {
        List<byte[]> result = new ArrayList<>();
        byte[] after = null;
        do {
            WorldDatabase.Page page = scan(prefix.getBytes(StandardCharsets.UTF_8), after, 256, "");
            for (byte[] key : page.keys) {
                if (result.size() >= maximum)
                    throw new IOException("Too many matching records; narrow the search");
                if (get(key) != null) result.add(key);
            }
            after = page.next;
        } while (after != null);
        for (Change c : changes.values()) {
            if (new String(c.key, StandardCharsets.UTF_8).startsWith(prefix)
                    && c.before == null
                    && c.after != null) result.add(c.key);
        }
        return result;
    }

    public synchronized void stage(byte[] key, byte[] value) throws IOException {
        Map<byte[], byte[]> edit = new LinkedHashMap<>();
        edit.put(key, value);
        stageBatch(edit);
    }

    public synchronized void stageBatch(Map<byte[], byte[]> edit) throws IOException {
        requireAvailable();
        Map<String, Change> next = new LinkedHashMap<>(changes);
        long size = stagedBytes;
        for (Map.Entry<byte[], byte[]> entry : edit.entrySet()) {
            if (entry.getKey().length == 0 || entry.getKey().length > 65536)
                throw new IOException("Invalid database key");
            if (entry.getValue() != null && entry.getValue().length > NativeNbt.MAX_BYTES)
                throw new IOException("Record exceeds 64 MiB");
            String id = hex(entry.getKey());
            Change old = next.get(id);
            byte[] before = old == null ? db().get(entry.getKey()) : old.before;
            if (old != null) size -= length(old.before) + length(old.after) + old.key.length;
            if (Arrays.equals(before, entry.getValue())) next.remove(id);
            else {
                Change replacement =
                        new Change(
                                entry.getKey(),
                                before,
                                entry.getValue() == null ? null : entry.getValue().clone());
                size += length(before) + length(replacement.after) + replacement.key.length;
                if (size > MAX_EDIT_BYTES)
                    throw new IOException("Edit exceeds 128 MiB; select a smaller area");
                next.put(id, replacement);
            }
        }
        changes.clear();
        changes.putAll(next);
        changedRecords = changes.size();
        stagedBytes = size;
        revision++;
    }

    public synchronized void discard() throws IOException {
        requireAvailable();
        changes.clear();
        changedRecords = 0;
        stagedBytes = 0;
        initialMetadata = readFile(new File(world, "level.dat"));
        metadata = initialMetadata;
        metadataChanged = false;
        revision++;
    }

    public synchronized File save() throws IOException {
        requireAvailable();
        if (pendingCount() == 0) return null;
        WorldDatabase d = db();
        for (Change c : changes.values()) {
            if (!Arrays.equals(c.before, d.get(c.key)))
                throw new IOException(
                        "The world changed since this edit was opened. Reload it before saving.");
        }
        if (!Arrays.equals(initialMetadata, readFile(new File(world, "level.dat"))))
            throw new IOException("level.dat changed outside this workspace");
        File history = new File(world, ".levi-editor/history");
        if (!history.isDirectory() && !history.mkdirs())
            throw new IOException("Cannot create edit history");
        File archive =
                new File(
                        history,
                        System.currentTimeMillis() + "-" + java.util.UUID.randomUUID() + ".zip");
        List<Change> batch = new ArrayList<>(changes.values());
        byte[] oldName = readFile(new File(world, "levelname.txt"));
        byte[] newName = oldName;
        if (!Arrays.equals(initialMetadata, metadata)) {
            NbtTag name = metadata().root.getTag("LevelName");
            if (name != null && name.getType() == NbtTag.TAG_STRING)
                newName = name.getString().getBytes(StandardCharsets.UTF_8);
        }
        writeArchive(archive, batch, initialMetadata, metadata, oldName, newName);
        File pending = new File(world, ".levi-editor/pending");
        atomic(pending, archive.getName().getBytes(StandardCharsets.UTF_8));
        try {
            writeBatch(d, batch, false);
            atomic(new File(world, "level.dat"), metadata);
            atomic(new File(world, "levelname.txt"), newName);
            Files.deleteIfExists(pending.toPath());
        } catch (IOException ex) {
            try {
                writeBatch(d, batch, true);
                atomic(new File(world, "level.dat"), initialMetadata);
                atomic(new File(world, "levelname.txt"), oldName);
                Files.deleteIfExists(pending.toPath());
                Files.deleteIfExists(archive.toPath());
            } catch (IOException rollback) {
                ex.addSuppressed(rollback);
            }
            throw ex;
        }
        initialMetadata = metadata;
        metadataChanged = false;
        changes.clear();
        changedRecords = 0;
        stagedBytes = 0;
        revision++;
        return archive;
    }

    public synchronized void restoreLatestSave() throws IOException {
        requireAvailable();
        if (pendingCount() != 0)
            throw new IOException("Save or discard current edits before restoring history");
        File[] files =
                new File(world, ".levi-editor/history")
                        .listFiles((d, name) -> name.endsWith(".zip"));
        if (files == null || files.length == 0)
            throw new IOException("No saved edit history is available");
        Arrays.sort(files, java.util.Comparator.comparingLong(File::lastModified).reversed());
        try (ZipFile archive = new ZipFile(files[0])) {
            List<Change> records = archiveChanges(archive);
            Map<byte[], byte[]> restore = new LinkedHashMap<>();
            for (Change c : records) {
                if (!Arrays.equals(db().get(c.key), c.after))
                    throw new IOException("A saved record changed outside the workspace");
                restore.put(c.key, c.before);
            }
            byte[] before = zipBytes(archive, "metadata.before");
            byte[] after = zipBytes(archive, "metadata.after");
            if (!Arrays.equals(metadata, after))
                throw new IOException("World metadata changed after the saved edit");
            stageBatch(restore);
            metadata = before;
            metadataChanged = !Arrays.equals(metadata, initialMetadata);
            revision++;
        } catch (org.json.JSONException ex) {
            throw new IOException("Invalid edit history", ex);
        }
    }

    private void recoverInterruptedSave() throws IOException {
        File pending = new File(world, ".levi-editor/pending");
        if (!pending.isFile()) return;
        String filename = new String(Files.readAllBytes(pending.toPath()), StandardCharsets.UTF_8);
        if (!filename.matches("[a-zA-Z0-9.-]+\\.zip"))
            throw new IOException("Invalid interrupted edit journal");
        try (ZipFile archive = new ZipFile(new File(world, ".levi-editor/history/" + filename))) {
            List<Change> records = archiveChanges(archive);
            for (Change c : records) {
                byte[] current = db().get(c.key);
                if (!Arrays.equals(current, c.before) && !Arrays.equals(current, c.after))
                    throw new IOException(
                            "Interrupted edit conflicts with later changes. Preserve the edit"
                                    + " history before continuing.");
            }
            byte[] current = readFile(new File(world, "level.dat"));
            byte[] before = zipBytes(archive, "metadata.before");
            byte[] after = zipBytes(archive, "metadata.after");
            if (!Arrays.equals(current, before) && !Arrays.equals(current, after))
                throw new IOException("Interrupted edit conflicts with world metadata");
            writeBatch(db(), records, true);
            atomic(new File(world, "level.dat"), before);
            atomic(new File(world, "levelname.txt"), zipBytes(archive, "name.before"));
            Files.deleteIfExists(pending.toPath());
            Files.deleteIfExists(new File(archive.getName()).toPath());
        } catch (org.json.JSONException ex) {
            throw new IOException("Invalid interrupted edit journal", ex);
        }
    }

    private static void writeArchive(
            File target,
            List<Change> changes,
            byte[] oldMetadata,
            byte[] newMetadata,
            byte[] oldName,
            byte[] newName)
            throws IOException {
        AtomicFile atomic = new AtomicFile(target);
        FileOutputStream raw = null;
        try {
            raw = atomic.startWrite();
            ZipOutputStream zip = new ZipOutputStream(raw);
            JSONArray entries = new JSONArray();
            for (int i = 0; i < changes.size(); i++) {
                Change c = changes.get(i);
                JSONObject entry =
                        new JSONObject()
                                .put("key", hex(c.key))
                                .put("before", c.before != null)
                                .put("after", c.after != null);
                entries.put(entry);
                putZip(zip, i + ".before", c.before);
                putZip(zip, i + ".after", c.after);
            }
            putZip(zip, "records.json", entries.toString().getBytes(StandardCharsets.UTF_8));
            putZip(zip, "metadata.before", oldMetadata);
            putZip(zip, "metadata.after", newMetadata);
            putZip(zip, "name.before", oldName);
            putZip(zip, "name.after", newName);
            zip.finish();
            zip.flush();
            atomic.finishWrite(raw);
        } catch (Exception ex) {
            if (raw != null) atomic.failWrite(raw);
            throw new IOException("Cannot save edit history", ex);
        }
    }

    private static List<Change> archiveChanges(ZipFile zip)
            throws IOException, org.json.JSONException {
        JSONArray data =
                new JSONArray(new String(zipBytes(zip, "records.json"), StandardCharsets.UTF_8));
        List<Change> result = new ArrayList<>();
        long size = 0;
        for (int i = 0; i < data.length(); i++) {
            JSONObject entry = data.getJSONObject(i);
            byte[] before = entry.getBoolean("before") ? zipBytes(zip, i + ".before") : null;
            byte[] after = entry.getBoolean("after") ? zipBytes(zip, i + ".after") : null;
            size += length(before) + length(after);
            if (size > MAX_EDIT_BYTES)
                throw new IOException("Edit history exceeds the memory budget");
            result.add(new Change(unhex(entry.getString("key")), before, after));
        }
        return result;
    }

    private static void writeBatch(WorldDatabase db, List<Change> changes, boolean undo)
            throws IOException {
        byte[][] keys = new byte[changes.size()][], values = new byte[changes.size()][];
        for (int i = 0; i < changes.size(); i++) {
            Change c = changes.get(i);
            keys[i] = c.key;
            values[i] = undo ? c.before : c.after;
        }
        db.write(keys, values);
    }

    private static void putZip(ZipOutputStream zip, String name, byte[] data) throws IOException {
        if (data == null) return;
        zip.putNextEntry(new ZipEntry(name));
        zip.write(data);
        zip.closeEntry();
    }

    private static byte[] zipBytes(ZipFile zip, String name) throws IOException {
        ZipEntry entry = zip.getEntry(name);
        if (entry == null) return null;
        try (java.io.InputStream in = zip.getInputStream(entry)) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[16384];
            int n;
            while ((n = in.read(buffer)) != -1) {
                if ((long) out.size() + n > NativeNbt.MAX_BYTES)
                    throw new IOException("Edit history entry exceeds 64 MiB");
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        }
    }

    public static void atomic(File file, byte[] bytes) throws IOException {
        if (bytes == null) {
            Files.deleteIfExists(file.toPath());
            return;
        }
        AtomicFile atomic = new AtomicFile(file);
        FileOutputStream stream = null;
        try {
            stream = atomic.startWrite();
            stream.write(bytes);
            atomic.finishWrite(stream);
        } catch (IOException ex) {
            if (stream != null) atomic.failWrite(stream);
            throw ex;
        }
    }

    private static byte[] readFile(File file) throws IOException {
        if (!file.isFile() && !new File(file.getPath() + ".bak").isFile()) return null;
        AtomicFile atomic = new AtomicFile(file);
        try (java.io.FileInputStream in = atomic.openRead()) {
            if (in.getChannel().size() > NativeNbt.MAX_BYTES)
                throw new IOException("World metadata exceeds 64 MiB");
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[16384];
            int n;
            while ((n = in.read(buffer)) != -1) {
                if ((long) out.size() + n > NativeNbt.MAX_BYTES)
                    throw new IOException("World metadata exceeds 64 MiB");
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        }
    }

    private static int length(byte[] a) {
        return a == null ? 0 : a.length;
    }

    public static String hex(byte[] bytes) {
        char[] alphabet = "0123456789abcdef".toCharArray(), result = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int n = bytes[i] & 255;
            result[i * 2] = alphabet[n >>> 4];
            result[i * 2 + 1] = alphabet[n & 15];
        }
        return new String(result);
    }

    public static byte[] unhex(String input) throws IOException {
        String s = input.replaceAll("\\s", "");
        if (s.length() == 0 || (s.length() & 1) != 0 || !s.matches("[0-9a-fA-F]+"))
            throw new IOException("Enter a valid hexadecimal key or value");
        byte[] result = new byte[s.length() / 2];
        for (int i = 0; i < result.length; i++)
            result[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        return result;
    }

    public void resume() {
        suspended = false;
    }

    public void suspend() {
        suspended = true;
    }

    public synchronized void releaseDatabase() {
        if (database != null) {
            database.close();
            database = null;
        }
    }

    @Override
    public synchronized void close() {
        closed = true;
        suspended = true;
        releaseDatabase();
        changes.clear();
        metadata = null;
        initialMetadata = null;
    }
}
