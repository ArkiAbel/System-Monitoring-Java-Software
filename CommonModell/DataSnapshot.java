package SystemMonitoring.CommonModell;

import java.util.ArrayList;
import java.util.List;

/**
 * Egy adatpillanat (snapshot) osszes informaciojat tarolo osztaly.
 * <p>
 * A DataSnapshot az agent es a szerver kozotti kommunikacio alapegysége.
 * Egy objektum tartalmaza egy adott idopontban az osszes monitorozott
 * rendszerinformaciot (OS, CPU, RAM, disk, network, processek).
 * </p>
 *
 * <p>A GSON szerializacio miatt minden mezo public (igy a JSON konnyen
 * eloallithato es visszaolvashato). A lista tipusu mezo ures listakkal
 * inicializalodnak a NullPointerException elkerulese vegett.</p>
 *
 * @author Arki Abel, FE8YA2
 * @version 1.0
 * @since 2026-05-06
 * @see DiskInfo
 * @see NetworkInfo
 * @see ProcessInfo
 */
public class DataSnapshot { //az alap adatmodell amit kovetni fogunk

    /** Az agent egyedi azonositoja (pl. "agent-123456789") */
    public String agentID;

    /** Unix timestamp millisecundumban (a meres pontos idopontja), kell az SQLite adatbazis megfelelo (legutolso soranak) kiolvasasahoz, hogy megjelenitheto legyen */
    public long timestamp;

    // --- Single adat mezok ---

    //OS adatszekcio
    public String OSname;
    public String OSversion;
    public long uptimeSeconds;

    //CPU adatszekcio
    public String architecture;
    public int physicalCores;
    public int logicalCores;
    public double CPUusagePercent;

    //RAM adatszekcio
    public long totalMemoryBytes;
    public long usedMemoryBytes;
    public long swapTotalBytes;
    public long swapUsedBytes;

    // --- Listazott mezok --- tablazatos adatokat tesznek majd
    /** Lemez particiok es adataik listaja */
    public List<DiskInfo> disks = new ArrayList<>();

    /** Halozati interfeszek es adataik listaja */
    public List<NetworkInfo> interfaces = new ArrayList<>();

    /** Futo folyamatok es adataik listaja */
    public List<ProcessInfo> processes = new ArrayList<>();
}