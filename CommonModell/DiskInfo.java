package SystemMonitoring.CommonModell;

/**
 * Lemez (disk) particioinformacioit tarolo osztaly.
 * <p>
 * Egy DiskInfo objektum egyetlen lemezparticio vagy meghajto
 * adatait tartalmazza: a mount pointot (vagy betujelet), a teljes
 * es hasznalt meretet, valamint az olvasasi/irasi sebessegeket
 * </p>
 *
 * <p>A transient mezok nem kerulnek bele a JSON szerializacioba,
 * ezek csak a GUI-ban hasznalt formatumok gyors eloallitasara
 * szolgalnak</p>
 *
 * @author Arki Abel, FE8YA2
 * @version 1.0
 * @since 2026-05-06
 * @see DataSnapshot
 * @see .Store
 */
public class DiskInfo {

    /** A partizio csatolasi pontja (Windows: "C:\", Linux: "/", "/home"), hasonlo amiket megadtam peldaknak a szimulalt reszen */
    public String mountPoint;

    /** Teljes meret byte-ban -> long */
    public long totalBytes;

    /** Hasznalt meret byte-ban -> long */
    public long usedBytes;

    /** Olvasasi sebesseg byte/sec-ben */
    public double readSpeed;

    /** Irasi sebesseg byte/sec-ben */
    public double writeSpeed;

    //transient mezok nem lesznek json-ban, ezek vegul nem lettek felhasznalva, ugy tekintettem rajuk, hogy lehet majd hasznuk
    public transient String usageFormatted;
    public transient String speedFormatted;

    public String getMountPoint() { return mountPoint; }
    //ezek nem relevansak a tovabbiakban, mivel szamolashoz kellenek, de azokat itt elvegeztuk helyben
    public long getTotalBytes() { return totalBytes; }
    public long getUsedBytes() { return usedBytes; }
    public double getReadSpeed() { return readSpeed; }
    public double getWriteSpeed() { return writeSpeed; }

    /**
     * Formazott szoveges reprezentacio a lemezhasznalatrol
     * pl: "42.5 / 128.0 GB (33.2%)" -> lentebb a formazas
     *
     * @return formazott hasznalati adat
     */
    public String getUsageFormatted() { //dinamikus kiszamitashoz
        double usedGB = this.usedBytes / 1024.0 / 1024.0 / 1024.0;
        double totalGB = this.totalBytes / 1024.0 / 1024.0 / 1024.0;
        double percent = (usedGB / totalGB) * 100;
        return String.format("%.1f / %.1f GB (%.1f%%)", usedGB, totalGB, percent); //%.1 -> 1 tizedest enged meg
    }

    /**
     * Formazott szoveges reprezentacio az I/O sebessegrol.
     * pl: "⬇ 125.3 - ⬆ 80.1 MB/s"
     *
     * @return formazott sebesseg adat
     */
    public String getSpeedFormatted() { //ASCII nyil karakterekkel ez talan jobban atjon :)
        return String.format("⬇ %.0f - ⬆ %.0f MB/s",
                this.readSpeed / 1024.0 / 1024.0,
                this.writeSpeed / 1024.0 / 1024.0);
    }
}