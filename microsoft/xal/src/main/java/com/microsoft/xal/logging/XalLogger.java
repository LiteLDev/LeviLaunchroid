package com.microsoft.xal.logging;

import android.annotation.SuppressLint;
import android.util.Log;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;

/**
 * 02.10.2020
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class XalLogger implements AutoCloseable {
    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat LogDateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    private static final String TAG = "XALJAVA";
    private final ArrayList<LogEntry> m_logs = new ArrayList<>();
    private final String m_subArea;
    private LogLevel m_leastVerboseLevel = LogLevel.Verbose;

    public XalLogger(String subArea) {
        m_subArea = subArea;
        Verbose("XalLogger created.");
    }

    private static native void nativeLogBatch(int i, LogEntry[] logEntryArr);

    public void close() {
        Flush();
    }

    public synchronized void Flush() {
        if (!m_logs.isEmpty()) {
            try {
                nativeLogBatch(m_leastVerboseLevel.ToInt(), m_logs.toArray(new LogEntry[m_logs.size()]));
                m_logs.clear();
                m_leastVerboseLevel = LogLevel.Verbose;
            } catch (Exception | UnsatisfiedLinkError e) {
                Log.e(TAG, "Failed to flush logs: " + e.toString());
            }
        }
    }

    public synchronized void Log(LogLevel level, String msg) {
        m_logs.add(new LogEntry(level, String.format("[%c][%s][%s] %s", level.ToChar(), Timestamp(), m_subArea, msg)));
        if (m_leastVerboseLevel.ToInt() > level.ToInt()) {
            m_leastVerboseLevel = level;
        }
    }

    public void Error(String msg) {
        Log.e(TAG, String.format("[%s] %s", this.m_subArea, msg));
        Log(LogLevel.Error, msg);
    }

    public void Warning(String msg) {
        Log.w(TAG, String.format("[%s] %s", this.m_subArea, msg));
        Log(LogLevel.Warning, msg);
    }

    public void Important(String msg) {
        Log.w(TAG, String.format("[%c][%s] %s", LogLevel.Important.ToChar(), this.m_subArea, msg));
        Log(LogLevel.Important, msg);
    }

    public void Information(String msg) {
        Log.i(TAG, String.format("[%s] %s", this.m_subArea, msg));
        Log(LogLevel.Information, msg);
    }

    public void Verbose(String msg) {
        Log.v(TAG, String.format("[%s] %s", this.m_subArea, msg));
        Log(LogLevel.Verbose, msg);
    }

    @NotNull
    private String Timestamp() {
        return LogDateFormat.format(GregorianCalendar.getInstance().getTime());
    }

    public enum LogLevel {
        Error(1, 'E'),
        Warning(2, 'W'),
        Important(3, 'P'),
        Information(4, 'I'),
        Verbose(5, 'V');

        private final char m_levelChar;
        private final int m_val;

        LogLevel(int val, char levelChar) {
            m_val = val;
            m_levelChar = levelChar;
        }

        public int ToInt() {
            return m_val;
        }

        public char ToChar() {
            return m_levelChar;
        }
    }
}