package org.levimc.launcher.core.content.leveldb;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Inflater;

public class LevelDBReader {
    private static final String TAG = "LevelDBReader";
    private static final long TABLE_MAGIC_NUMBER = 0xdb4775248b80fb57L;
    private static final int BLOCK_TRAILER_SIZE = 5;
    private static final int FOOTER_SIZE = 48;
    private static final int LOG_BLOCK_SIZE = 32768;

    private final File dbPath;
    private final Map<ByteArrayWrapper, byte[]> allData = new HashMap<>();

    public LevelDBReader(File dbPath) {
        this.dbPath = dbPath;
    }

    public List<LevelDBEntry> readAllEntries() throws IOException {
        File[] sstFiles = dbPath.listFiles((dir, name) ->
            name.endsWith(".ldb") || name.endsWith(".sst"));

        if (sstFiles != null) {
            Arrays.sort(sstFiles, Comparator.comparing(File::getName));
            for (File sstFile : sstFiles) {
                try {
                    readSSTable(sstFile);
                } catch (Exception e) {
                    Log.w(TAG, "Failed to read SST file: " + sstFile.getName() + " - " + e.getMessage());
                    try {
                        readSSTableRaw(sstFile);
                    } catch (Exception e2) {
                        Log.w(TAG, "Raw scan also failed for: " + sstFile.getName());
                    }
                }
            }
        }

        File[] logFiles = dbPath.listFiles((dir, name) -> name.endsWith(".log"));
        if (logFiles != null) {
            Arrays.sort(logFiles, Comparator.comparing(File::getName));
            for (File logFile : logFiles) {
                try {
                    readLogFile(logFile);
                } catch (Exception e) {
                    Log.w(TAG, "Failed to read log file: " + logFile.getName() + " - " + e.getMessage());
                }
            }
        }

        List<LevelDBEntry> entries = new ArrayList<>();
        for (Map.Entry<ByteArrayWrapper, byte[]> entry : allData.entrySet()) {
            if (entry.getValue() != null) {
                entries.add(new LevelDBEntry(entry.getKey().data, entry.getValue()));
            }
        }

        Log.d(TAG, "Total entries read: " + entries.size());

        int structureCount = 0;
        for (LevelDBEntry entry : entries) {
            String keyStr = tryDecodeKey(entry.getKey().getRawKey());
            if (keyStr != null && keyStr.startsWith("structuretemplate_")) {
                structureCount++;
            }
        }
        Log.d(TAG, "Total structures found: " + structureCount);

        return entries;
    }

    private void readLogFile(File logFile) throws IOException {
        Log.d(TAG, "Reading log file: " + logFile.getName() + " size: " + logFile.length());

        try (FileInputStream fis = new FileInputStream(logFile)) {
            byte[] fileData = readAllBytes(fis, (int) logFile.length());

            int pos = 0;
            ByteArrayOutputStream fullRecord = new ByteArrayOutputStream();

            while (pos + 7 <= fileData.length) {
                int blockOffset = pos % LOG_BLOCK_SIZE;
                int blockRemaining = LOG_BLOCK_SIZE - blockOffset;

                if (blockRemaining < 7) {
                    pos += blockRemaining;
                    continue;
                }

                int length = readInt16LE(fileData, pos + 4);
                int type = fileData[pos + 6] & 0xFF;

                pos += 7;

                if (length < 0 || length > LOG_BLOCK_SIZE || pos + length > fileData.length) {
                    break;
                }

                byte[] recordData = Arrays.copyOfRange(fileData, pos, pos + length);
                pos += length;

                switch (type) {
                    case 1:
                        fullRecord.reset();
                        fullRecord.write(recordData);
                        parseWriteBatch(fullRecord.toByteArray());
                        break;
                    case 2:
                        fullRecord.reset();
                        fullRecord.write(recordData);
                        break;
                    case 3:
                        fullRecord.write(recordData);
                        break;
                    case 4:
                        fullRecord.write(recordData);
                        parseWriteBatch(fullRecord.toByteArray());
                        fullRecord.reset();
                        break;
                }
            }
        }
    }

    private void parseWriteBatch(byte[] data) {
        if (data.length < 12) return;

        try {
            int count = readInt32LE(data, 8);
            int pos = 12;

            for (int i = 0; i < count && pos < data.length; i++) {
                int recordType = data[pos++] & 0xFF;

                int[] keyLenResult = readVarInt(data, pos);
                int keyLen = keyLenResult[0];
                pos = keyLenResult[1];

                if (keyLen <= 0 || keyLen > 10000 || pos + keyLen > data.length) {
                    break;
                }

                byte[] key = Arrays.copyOfRange(data, pos, pos + keyLen);
                pos += keyLen;

                if (recordType == 1) {
                    int[] valLenResult = readVarInt(data, pos);
                    int valLen = valLenResult[0];
                    pos = valLenResult[1];

                    if (valLen >= 0 && pos + valLen <= data.length) {
                        byte[] value = Arrays.copyOfRange(data, pos, pos + valLen);
                        pos += valLen;

                        allData.put(new ByteArrayWrapper(key), value);
                        logStructureIfFound(key, value, "log");
                    }
                } else if (recordType == 0) {
                    allData.put(new ByteArrayWrapper(key), null);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error parsing write batch", e);
        }
    }

    private void logStructureIfFound(byte[] key, byte[] value, String source) {
        String keyStr = tryDecodeKey(key);
        if (keyStr != null && keyStr.startsWith("structuretemplate_")) {
            Log.d(TAG, "Found structure in " + source + ": " + keyStr + " value size: " + (value != null ? value.length : 0));
        }
    }

    private String tryDecodeKey(byte[] key) {
        if (key == null || key.length == 0) return null;
        try {
            boolean printable = true;
            for (byte b : key) {
                if (b < 32 && b != 0) {
                    printable = false;
                    break;
                }
            }
            if (printable) {
                return new String(key, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
        }
        return null;
    }

    private void readSSTable(File file) throws IOException {
        Log.d(TAG, "Reading SSTable: " + file.getName() + " size: " + file.length());

        try (RandomAccessFile raf = new RandomAccessFile(file, "r");
             FileChannel channel = raf.getChannel()) {

            long fileSize = channel.size();
            if (fileSize < FOOTER_SIZE) {
                return;
            }

            ByteBuffer footer = ByteBuffer.allocate(FOOTER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
            channel.position(fileSize - FOOTER_SIZE);
            channel.read(footer);
            footer.flip();

            readVarInt64(footer);
            readVarInt64(footer);
            long indexOffset = readVarInt64(footer);
            long indexSize = readVarInt64(footer);

            footer.position(40);
            long magic = footer.getLong();

            if (magic != TABLE_MAGIC_NUMBER) {
                Log.w(TAG, "Invalid magic number in " + file.getName() + ", trying raw scan");
                readSSTableRaw(file);
                return;
            }

            ByteBuffer indexBlock = readBlock(channel, indexOffset, (int) indexSize);
            if (indexBlock == null) {
                Log.w(TAG, "Failed to read index block from " + file.getName());
                return;
            }

            List<BlockHandle> dataBlocks = parseIndexBlock(indexBlock);
            Log.d(TAG, "Found " + dataBlocks.size() + " data blocks in " + file.getName());

            for (BlockHandle handle : dataBlocks) {
                try {
                    ByteBuffer dataBlock = readBlock(channel, handle.offset, (int) handle.size);
                    if (dataBlock != null) {
                        parseDataBlock(dataBlock);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to read data block at offset " + handle.offset, e);
                }
            }
        }
    }

    private void readSSTableRaw(File file) throws IOException {
        Log.d(TAG, "Raw scanning file: " + file.getName());
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = readAllBytes(fis, (int) file.length());

            byte[] prefix = "structuretemplate_".getBytes();
            int found = 0;

            for (int i = 0; i < data.length - prefix.length - 20; i++) {
                boolean match = true;
                for (int j = 0; j < prefix.length; j++) {
                    if (data[i + j] != prefix[j]) {
                        match = false;
                        break;
                    }
                }

                if (match) {
                    int keyEnd = i;
                    for (int j = i; j < Math.min(i + 256, data.length); j++) {
                        byte b = data[j];
                        if (b < 32 || b > 126) {
                            keyEnd = j;
                            break;
                        }
                    }

                    if (keyEnd > i) {
                        byte[] key = Arrays.copyOfRange(data, i, keyEnd);
                        String keyStr = new String(key, StandardCharsets.UTF_8);

                        int valueStart = keyEnd;
                        int maxSearch = Math.min(keyEnd + 100, data.length - 8);

                        while (valueStart < maxSearch) {
                            if (isValidNbtStart(data, valueStart)) {
                                break;
                            }
                            valueStart++;
                        }

                        int valueLen = estimateNbtLength(data, valueStart);
                        if (valueLen > 0 && valueStart + valueLen <= data.length) {
                            byte[] value = Arrays.copyOfRange(data, valueStart, valueStart + valueLen);
                            allData.put(new ByteArrayWrapper(key), value);
                            Log.d(TAG, "Found structure in raw scan: " + keyStr + " size: " + valueLen);
                            found++;
                            i = valueStart + valueLen - 1;
                        }
                    }
                }
            }
            Log.d(TAG, "Raw scan found " + found + " structures in " + file.getName());
        }
    }

    private boolean isValidNbtStart(byte[] data, int pos) {
        if (pos + 8 >= data.length) return false;
        int version = readInt32LE(data, pos);
        int length = readInt32LE(data, pos + 4);
        return version >= 1 && version <= 10 && length > 0 && length < 10 * 1024 * 1024;
    }

    private int estimateNbtLength(byte[] data, int start) {
        if (start + 8 >= data.length) return 0;
        int length = readInt32LE(data, start + 4);
        if (length > 0 && length < 10 * 1024 * 1024 && start + 8 + length <= data.length) {
            return 8 + length;
        }
        return 0;
    }

    private ByteBuffer readBlock(FileChannel channel, long offset, int size) throws IOException {
        if (size <= 0 || offset < 0 || offset + size + BLOCK_TRAILER_SIZE > channel.size()) {
            return null;
        }

        ByteBuffer buffer = ByteBuffer.allocate(size + BLOCK_TRAILER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        channel.position(offset);
        int read = channel.read(buffer);
        if (read < size + BLOCK_TRAILER_SIZE) {
            return null;
        }
        buffer.flip();

        byte[] blockData = new byte[size];
        buffer.get(blockData);
        byte compressionType = buffer.get();

        byte[] decompressed = blockData;
        if (compressionType == 1) {
            decompressed = decompressSnappy(blockData);
        } else if (compressionType == 2) {
            decompressed = decompressZlib(blockData);
        } else if (compressionType == 4 || compressionType == 5) {
            decompressed = decompressLZ4(blockData);
        } else if (compressionType == 7) {
            decompressed = decompressZstd(blockData);
        }

        if (decompressed == null || decompressed.length == 0) {
            decompressed = blockData;
        }

        return ByteBuffer.wrap(decompressed).order(ByteOrder.LITTLE_ENDIAN);
    }

    private byte[] decompressSnappy(byte[] data) {
        try {
            if (data.length == 0) return data;

            int[] result = readVarInt(data, 0);
            int uncompressedLen = result[0];
            int pos = result[1];

            if (uncompressedLen <= 0 || uncompressedLen > 100 * 1024 * 1024) {
                return data;
            }

            byte[] output = new byte[uncompressedLen];
            int outPos = 0;

            while (pos < data.length && outPos < uncompressedLen) {
                int tag = data[pos++] & 0xFF;
                int tagType = tag & 0x03;

                if (tagType == 0) {
                    int len = (tag >> 2);
                    if (len < 60) {
                        len += 1;
                    } else {
                        int extraBytes = len - 59;
                        len = 0;
                        for (int i = 0; i < extraBytes && pos < data.length; i++) {
                            len |= (data[pos++] & 0xFF) << (8 * i);
                        }
                        len += 1;
                    }

                    if (pos + len > data.length || outPos + len > uncompressedLen) {
                        break;
                    }
                    System.arraycopy(data, pos, output, outPos, len);
                    pos += len;
                    outPos += len;
                } else {
                    int len, offset;
                    if (tagType == 1) {
                        len = ((tag >> 2) & 0x07) + 4;
                        if (pos >= data.length) break;
                        offset = ((tag & 0xE0) << 3) | (data[pos++] & 0xFF);
                    } else if (tagType == 2) {
                        len = (tag >> 2) + 1;
                        if (pos + 2 > data.length) break;
                        offset = (data[pos++] & 0xFF) | ((data[pos++] & 0xFF) << 8);
                    } else {
                        len = (tag >> 2) + 1;
                        if (pos + 4 > data.length) break;
                        offset = (data[pos++] & 0xFF) | ((data[pos++] & 0xFF) << 8) |
                                ((data[pos++] & 0xFF) << 16) | ((data[pos++] & 0xFF) << 24);
                    }

                    if (offset <= 0 || offset > outPos) {
                        break;
                    }

                    int srcPos = outPos - offset;
                    for (int i = 0; i < len && outPos < uncompressedLen; i++) {
                        output[outPos++] = output[srcPos++];
                    }
                }
            }

            if (outPos == uncompressedLen) {
                return output;
            } else {
                Log.w(TAG, "Snappy decompression incomplete: " + outPos + "/" + uncompressedLen);
                return Arrays.copyOf(output, outPos);
            }
        } catch (Exception e) {
            Log.w(TAG, "Snappy decompression failed", e);
            return data;
        }
    }

    private byte[] decompressZlib(byte[] data) {
        try {
            Inflater inflater = new Inflater(false);
            inflater.setInput(data);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];

            while (!inflater.finished()) {
                try {
                    int count = inflater.inflate(buffer);
                    if (count == 0) {
                        if (inflater.needsInput()) break;
                    }
                    baos.write(buffer, 0, count);
                } catch (Exception e) {
                    break;
                }
            }
            inflater.end();

            if (baos.size() > 0) {
                return baos.toByteArray();
            }

            inflater = new Inflater(true);
            inflater.setInput(data);
            baos.reset();

            while (!inflater.finished()) {
                try {
                    int count = inflater.inflate(buffer);
                    if (count == 0) {
                        if (inflater.needsInput()) break;
                    }
                    baos.write(buffer, 0, count);
                } catch (Exception e) {
                    break;
                }
            }
            inflater.end();

            return baos.size() > 0 ? baos.toByteArray() : data;
        } catch (Exception e) {
            return data;
        }
    }

    private byte[] decompressLZ4(byte[] data) {
        try {
            if (data.length < 4) return data;

            int uncompressedSize = readInt32LE(data, 0);
            if (uncompressedSize <= 0 || uncompressedSize > 100 * 1024 * 1024) {
                return data;
            }

            byte[] output = new byte[uncompressedSize];
            int srcPos = 4;
            int dstPos = 0;

            while (srcPos < data.length && dstPos < uncompressedSize) {
                int token = data[srcPos++] & 0xFF;

                int literalLen = token >> 4;
                if (literalLen == 15) {
                    int b;
                    do {
                        if (srcPos >= data.length) break;
                        b = data[srcPos++] & 0xFF;
                        literalLen += b;
                    } while (b == 255);
                }

                if (srcPos + literalLen > data.length || dstPos + literalLen > uncompressedSize) {
                    break;
                }
                System.arraycopy(data, srcPos, output, dstPos, literalLen);
                srcPos += literalLen;
                dstPos += literalLen;

                if (srcPos >= data.length || dstPos >= uncompressedSize) {
                    break;
                }

                if (srcPos + 2 > data.length) break;
                int offset = (data[srcPos++] & 0xFF) | ((data[srcPos++] & 0xFF) << 8);
                if (offset == 0 || offset > dstPos) {
                    break;
                }

                int matchLen = (token & 0x0F) + 4;
                if (matchLen == 19) {
                    int b;
                    do {
                        if (srcPos >= data.length) break;
                        b = data[srcPos++] & 0xFF;
                        matchLen += b;
                    } while (b == 255);
                }

                int matchPos = dstPos - offset;
                for (int i = 0; i < matchLen && dstPos < uncompressedSize; i++) {
                    output[dstPos++] = output[matchPos++];
                }
            }

            return dstPos == uncompressedSize ? output : Arrays.copyOf(output, dstPos);
        } catch (Exception e) {
            Log.w(TAG, "LZ4 decompression failed", e);
            return data;
        }
    }

    private byte[] decompressZstd(byte[] data) {
        return data;
    }

    private List<BlockHandle> parseIndexBlock(ByteBuffer buffer) {
        List<BlockHandle> handles = new ArrayList<>();

        try {
            if (buffer.limit() < 4) return handles;

            buffer.position(buffer.limit() - 4);
            int numRestarts = buffer.getInt();

            if (numRestarts < 0 || numRestarts > 10000) return handles;

            int dataEnd = buffer.limit() - 4 - (numRestarts * 4);
            if (dataEnd <= 0) return handles;

            buffer.position(0);
            byte[] prevKey = new byte[0];

            while (buffer.position() < dataEnd && buffer.hasRemaining()) {
                int shared = readVarInt32(buffer);
                int nonShared = readVarInt32(buffer);
                int valueLen = readVarInt32(buffer);

                if (shared < 0 || nonShared < 0 || valueLen < 0) break;
                if (nonShared > 10000 || valueLen > 100) break;
                if (buffer.position() + nonShared + valueLen > buffer.limit()) break;

                byte[] key = new byte[shared + nonShared];
                if (shared > 0 && shared <= prevKey.length) {
                    System.arraycopy(prevKey, 0, key, 0, shared);
                }
                buffer.get(key, shared, nonShared);
                prevKey = key;

                if (valueLen > 0) {
                    int valStart = buffer.position();
                    long offset = readVarInt64(buffer);
                    long size = readVarInt64(buffer);

                    if (offset >= 0 && size > 0 && size < 100 * 1024 * 1024) {
                        handles.add(new BlockHandle(offset, size));
                    }

                    buffer.position(valStart + valueLen);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error parsing index block", e);
        }

        return handles;
    }

    private void parseDataBlock(ByteBuffer buffer) {
        try {
            if (buffer.limit() < 4) return;

            buffer.position(buffer.limit() - 4);
            int numRestarts = buffer.getInt();

            if (numRestarts < 0 || numRestarts > 100000) return;

            int dataEnd = buffer.limit() - 4 - (numRestarts * 4);
            if (dataEnd <= 0) return;

            buffer.position(0);
            byte[] prevKey = new byte[0];

            while (buffer.position() < dataEnd && buffer.hasRemaining()) {
                int shared = readVarInt32(buffer);
                int nonShared = readVarInt32(buffer);
                int valueLen = readVarInt32(buffer);

                if (shared < 0 || nonShared < 0 || valueLen < 0) break;
                if (shared > 10000 || nonShared > 10000 || valueLen > 50 * 1024 * 1024) break;
                if (buffer.position() + nonShared + valueLen > buffer.limit()) break;

                byte[] fullKey = new byte[shared + nonShared];
                if (shared > 0 && shared <= prevKey.length) {
                    System.arraycopy(prevKey, 0, fullKey, 0, shared);
                }
                buffer.get(fullKey, shared, nonShared);
                prevKey = fullKey;

                byte[] value = new byte[valueLen];
                buffer.get(value);

                byte[] userKey = fullKey;
                boolean isValue = true;

                if (fullKey.length > 8) {
                    userKey = Arrays.copyOf(fullKey, fullKey.length - 8);
                    int type = fullKey[fullKey.length - 8] & 0xFF;
                    isValue = (type == 1);
                }

                if (userKey.length > 0) {
                    if (isValue) {
                        allData.put(new ByteArrayWrapper(userKey), value);
                        logStructureIfFound(userKey, value, "SST");
                    } else {
                        allData.put(new ByteArrayWrapper(userKey), null);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error parsing data block", e);
        }
    }

    private int readVarInt32(ByteBuffer buffer) {
        int result = 0;
        int shift = 0;
        while (buffer.hasRemaining() && shift < 35) {
            byte b = buffer.get();
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) break;
            shift += 7;
        }
        return result;
    }

    private long readVarInt64(ByteBuffer buffer) {
        long result = 0;
        int shift = 0;
        while (buffer.hasRemaining() && shift < 70) {
            byte b = buffer.get();
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) break;
            shift += 7;
        }
        return result;
    }

    private int[] readVarInt(byte[] data, int pos) {
        int result = 0;
        int shift = 0;
        while (pos < data.length && shift < 35) {
            byte b = data[pos++];
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) break;
            shift += 7;
        }
        return new int[]{result, pos};
    }

    private int readInt32LE(byte[] data, int pos) {
        if (pos + 4 > data.length) return 0;
        return (data[pos] & 0xFF) |
               ((data[pos + 1] & 0xFF) << 8) |
               ((data[pos + 2] & 0xFF) << 16) |
               ((data[pos + 3] & 0xFF) << 24);
    }

    private int readInt16LE(byte[] data, int pos) {
        if (pos + 2 > data.length) return 0;
        return (data[pos] & 0xFF) | ((data[pos + 1] & 0xFF) << 8);
    }

    private byte[] readAllBytes(FileInputStream fis, int length) throws IOException {
        byte[] data = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = fis.read(data, offset, length - offset);
            if (read < 0) break;
            offset += read;
        }
        return data;
    }

    public void close() {
        allData.clear();
    }

    private static class BlockHandle {
        final long offset;
        final long size;

        BlockHandle(long offset, long size) {
            this.offset = offset;
            this.size = size;
        }
    }

    private static class ByteArrayWrapper {
        final byte[] data;
        final int hashCode;

        ByteArrayWrapper(byte[] data) {
            this.data = data;
            this.hashCode = Arrays.hashCode(data);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ByteArrayWrapper)) return false;
            return Arrays.equals(data, ((ByteArrayWrapper) o).data);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}
