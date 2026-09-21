package SystemMonitoring.CommonModell;

/**
 * Futo folyamat (process) informacioit tarolo osztaly
 * <p>
 * Egy ProcessInfo objektum egyetlen futo processz adatait tartalmazza:
 * a nevet, a CPU kihasznaltsagat (%) es a felhasznalt memoria meretet (byte)
 * </p>
 *
 * @author Arki Abel, FE8YA2
 * @version 1.0
 * @since 2026-05-06
 * @see DataSnapshot
 */
public class ProcessInfo {
    public String processName;
    public double CPUpercent;
    public long memoryBytes;

    //transient mezok - nem kerulnek JSON-ba, csak a GUI formatumahoz
    public transient String cpuPercentFormatted;
    public transient String memoryMBFormatted;

    public String getProcessName() { return processName; }
    //ezek nyersen nem kellettek, csak a formazott valtozataik
    public double getCPUpercent() { return CPUpercent; }
    public long getMemoryBytes() { return memoryBytes; }

    /**
     * Formazott CPU kihasznaltsag szazalekban
     *
     * @return string pl. "15.3"
     */
    public String getCpuPercentFormatted() {
        return String.format("%.1f", this.CPUpercent);
    }

    /**
     * Formazott memoriahasznalat megabajtban
     *
     * @return string pl. "256"
     */
    public String getMemoryMBFormatted() {
        return String.format("%.0f", this.memoryBytes / 1024.0 / 1024.0);
    }
}