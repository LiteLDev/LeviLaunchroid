package com.microsoft.xbox.toolkit;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;


public class XLEFileCache {
    private static final String TAG = XLEFileCache.class.getSimpleName();
    final int maxFileNumber;
    private final long expiredTimer;
    int size;
    private boolean enabled;
    private int readAccessCnt;
    private int readSuccessfulCnt;
    private int writeAccessCnt;

    XLEFileCache() {
        this.size = 0;
        this.enabled = true;
        this.readAccessCnt = 0;
        this.writeAccessCnt = 0;
        this.readSuccessfulCnt = 0;
        this.expiredTimer = Long.MAX_VALUE;
        this.maxFileNumber = 0;
        this.enabled = false;
    }

    XLEFileCache(String str, int i) {
        this(str, i, Long.MAX_VALUE);
    }

    XLEFileCache(String str, int i, long j) {
        this.size = 0;
        this.enabled = true;
        this.readAccessCnt = 0;
        this.writeAccessCnt = 0;
        this.readSuccessfulCnt = 0;
        this.maxFileNumber = i;
        this.expiredTimer = j;
    }

    public static int readInt(@NotNull InputStream inputStream) throws IOException {
        int read = inputStream.read();
        int read2 = inputStream.read();
        int read3 = inputStream.read();
        int read4 = inputStream.read();
        if ((read | read2 | read3 | read4) >= 0) {
            return (read << 24) + (read2 << 16) + (read3 << 8) + (read4 << 0);
        }
        throw new EOFException();
    }

    public int getItemsInCache() {
        return this.size;
    }

    public synchronized boolean contains(XLEFileCacheItemKey xLEFileCacheItemKey) {
        if (!this.enabled) {
            return false;
        }
        return new File(XLEFileCacheManager.getCacheRootDir(this), getCachedItemFileName(xLEFileCacheItemKey)).exists();
    }

    public synchronized OutputStream getOuputStreamForSave(XLEFileCacheItemKey xLEFileCacheItemKey) throws IOException {
        if (!this.enabled) {
            return new OutputStream() {
                public void write(int i) throws IOException {
                }
            };
        }
        XLEAssert.assertTrue(Thread.currentThread() != ThreadManager.UIThread);
        this.writeAccessCnt++;
        checkAndEnsureCapacity();
        File file = new File(XLEFileCacheManager.getCacheRootDir(this), getCachedItemFileName(xLEFileCacheItemKey));
        if (file.exists()) {
            file.delete();
            this.size--;
        }
        if (file.createNewFile()) {
            this.size++;
        }
        return new CachedFileOutputStreamItem(xLEFileCacheItemKey, file);
    }

    public synchronized void save(XLEFileCacheItemKey xLEFileCacheItemKey, InputStream inputStream) {
        try {
            OutputStream ouputStreamForSave = getOuputStreamForSave(xLEFileCacheItemKey);
            StreamUtil.CopyStream(ouputStreamForSave, inputStream);
            ouputStreamForSave.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized InputStream getInputStreamForRead(XLEFileCacheItemKey xLEFileCacheItemKey) {
        if (!this.enabled) {
            return null;
        }
        XLEAssert.assertTrue(Thread.currentThread() != ThreadManager.UIThread);
        this.readAccessCnt++;
        File file = new File(XLEFileCacheManager.getCacheRootDir(this), getCachedItemFileName(xLEFileCacheItemKey));
        if (file.exists()) {
            if (file.lastModified() < System.currentTimeMillis() - this.expiredTimer) {
                file.delete();
                this.size--;
                return null;
            }
            try {
                InputStream contentInputStream = new CachedFileInputStreamItem(xLEFileCacheItemKey, file).getContentInputStream();
                this.readSuccessfulCnt++;
                return contentInputStream;
            } catch (IOException unused) {
                return null;
            }
        }
        return null;
    }

    public @NotNull String toString() {
        return "Size=" + this.size + "\tRootDir=" + XLEFileCacheManager.getCacheRootDir(this) + "\tMaxFileNumber=" + this.maxFileNumber + "\tExpiredTimerInSeconds=" + this.expiredTimer + "\tWriteAccessCnt=" + this.writeAccessCnt + "\tReadAccessCnt=" + this.readAccessCnt + "\tReadSuccessfulCnt=" + this.readSuccessfulCnt;
    }

    private void checkAndEnsureCapacity() {
        if (this.size >= this.maxFileNumber && this.enabled) {
            File[] listFiles = XLEFileCacheManager.getCacheRootDir(this).listFiles();
            listFiles[new Random().nextInt(listFiles.length)].delete();
            this.size = listFiles.length - 1;
        }
    }

    private @NotNull String getCachedItemFileName(@NotNull XLEFileCacheItemKey xLEFileCacheItemKey) {
        return String.valueOf(xLEFileCacheItemKey.getKeyString().hashCode());
    }

    private class CachedFileInputStreamItem {
        private final byte[] computedMd5;
        private final InputStream contentInputStream;
        private final byte[] savedMd5;
        private MessageDigest mDigest = null;

        public CachedFileInputStreamItem(XLEFileCacheItemKey xLEFileCacheItemKey, File file) throws IOException {
            FileInputStream fileInputStream = new FileInputStream(file);
            try {
                MessageDigest instance = MessageDigest.getInstance("MD5");
                this.mDigest = instance;
                byte[] bArr = new byte[instance.getDigestLength()];
                this.savedMd5 = bArr;
                if (fileInputStream.read(bArr) == this.mDigest.getDigestLength()) {
                    int access$000 = XLEFileCache.readInt(fileInputStream);
                    byte[] bArr2 = new byte[access$000];
                    if (access$000 != fileInputStream.read(bArr2) || !xLEFileCacheItemKey.getKeyString().equals(new String(bArr2))) {
                        file.delete();
                        throw new IOException("File key check failed because keyLength != readKeyLength or !key.getKeyString().equals(new String(urlOrSomething))");
                    }
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    StreamUtil.CopyStream(byteArrayOutputStream, fileInputStream);
                    fileInputStream.close();
                    byte[] byteArray = byteArrayOutputStream.toByteArray();
                    this.mDigest.update(byteArray);
                    this.computedMd5 = this.mDigest.digest();
                    if (!isMd5Error()) {
                        this.contentInputStream = new ByteArrayInputStream(byteArray);
                        return;
                    }
                    file.delete();
                    throw new IOException(fileInputStream.getFD() + "the saved md5 is not equal computed md5.ComputedMd5:" + this.computedMd5 + "     SavedMd5:" + this.savedMd5);
                }
                fileInputStream.close();
                throw new IOException("Ddigest lengh check failed!");
            } catch (NoSuchAlgorithmException e) {
                fileInputStream.close();
                throw new IOException("File digest failed! " + e.getMessage());
            } catch (OutOfMemoryError e2) {
                fileInputStream.close();
                throw new IOException("File digest failed! Out of memory: " + e2.getMessage());
            }
        }

        public InputStream getContentInputStream() {
            return this.contentInputStream;
        }

        private boolean isMd5Error() {
            for (int i = 0; i < this.mDigest.getDigestLength(); i++) {
                if (this.savedMd5[i] != this.computedMd5[i]) {
                    return true;
                }
            }
            return false;
        }
    }

    private class CachedFileOutputStreamItem extends FileOutputStream {
        private final File destFile;
        private MessageDigest mDigest = null;
        private boolean startDigest = false;
        private boolean writeMd5Finished = false;

        public CachedFileOutputStreamItem(@NotNull XLEFileCacheItemKey xLEFileCacheItemKey, File file) throws IOException {
            super(file);
            this.destFile = file;
            try {
                MessageDigest instance = MessageDigest.getInstance("MD5");
                this.mDigest = instance;
                write(new byte[instance.getDigestLength()]);
                byte[] bytes = xLEFileCacheItemKey.getKeyString().getBytes();
                writeInt(bytes.length);
                write(bytes);
                this.startDigest = true;
            } catch (NoSuchAlgorithmException e) {
                throw new IOException("File digest failed!" + e.getMessage());
            }
        }

        public void close() throws IOException {
            super.close();
            if (!this.writeMd5Finished) {
                this.writeMd5Finished = true;
                RandomAccessFile randomAccessFile = new RandomAccessFile(this.destFile, "rw");
                byte[] digest = this.mDigest.digest();
                randomAccessFile.seek(0);
                randomAccessFile.write(digest);
                randomAccessFile.close();
            }
        }

        public void write(byte[] bArr, int i, int i2) throws IOException {
            super.write(bArr, i, i2);
            if (this.startDigest) {
                this.mDigest.update(bArr, i, i2);
            }
        }

        private final void writeInt(int i) throws IOException {
            write((i >>> 24) & 255);
            write((i >>> 16) & 255);
            write((i >>> 8) & 255);
            write((i >>> 0) & 255);
        }
    }
}
