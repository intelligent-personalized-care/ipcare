package android.util;

/** JVM-only replacement for transport diagnostics, not for any Android behaviour under test. */
public final class Log {
    public static final int WARN = 5;
    private Log() {}
    public static int println(int priority, String tag, String message) { return 0; }
}
