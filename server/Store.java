package SystemMonitoring.server;

import SystemMonitoring.CommonModell.DataSnapshot;
import SystemMonitoring.CommonModell.DiskInfo;
import SystemMonitoring.CommonModell.NetworkInfo;
import SystemMonitoring.CommonModell.ProcessInfo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import org.sqlite.SQLiteConfig;

/**
 * Adatbazis kezelo osztaly SQLite-al.
 * <p>
 * A Store felel az osszes perzisztens adattarolasi muveletert:
 * tablak letrehozasa, snapshot-ok mentese, valamint a GUI altal
 * igenyelt adatok lekerdezese. Az SQLite adatbazis hasznalata
 * biztosítja, hogy a szerver ujrainditasa utan is rendelkezesre
 * alljanak a korabbi meresi adatok.
 * </p>
 *
 * <p>Az osztaly a kovetkezo tablakat hozza letre:</p>
 * <ul>
 *   <li><b>snapshots</b> - a snapshot-ok fo tablaja (OS, CPU, RAM adatok)</li>
 *   <li><b>diskinfo</b> - lemez particiok adatai (foreign key: snapshotID)</li>
 *   <li><b>networkinfo</b> - halozati interfeszek adatai (foreign key: snapshotID)</li>
 *   <li><b>processinfo</b> - futo folyamatok adatai (foreign key: snapshotID)</li>
 * </ul>
 *
 * <p>A WAL (Write-Ahead Logging) uzemmod engedelyezese lehetove teszi az
 * egyidejű irast es olvasast a szinkronizacios problemak csökkentese erdekeben.</p>
 *
 * @author Arki Abel, FE8YA2
 * @version 1.0
 * @since 2026-05-06
 * @see ClientHandler
 * @see GUIhandler
 */
public class Store {
    private final Connection connection;

    /**
     * Konstruktor - adatbazis kapcsolat letrehozasa
     * Beallitja a WAL journal modot es a busy timeout-ot,
     * majd letrehozza a szukseges tablakat. Ez utobbi inkabb konkurrens adatbazis modositas
     * megelozesenek a potcselekmenye volt, de bent hagytam, mert artani nem fog
     *
     * @param DBpath az SQLite adatbazis fajl eleresi utvonala (pl. "system_monitoring.db")
     * @throws SQLException ha a kapcsolat nem hozhato letre vagy a tablak nem hozhatok letre
     */
    public Store(String DBpath) throws SQLException { //dobhat kivetelt, mivel it probalkozunk egy inicialis statement-et beletolteni az adatbazisba letrehozaskor
        SQLiteConfig config = new SQLiteConfig();
        config.setJournalMode(SQLiteConfig.JournalMode.WAL); //write ahead logging
        connection = DriverManager.getConnection("jdbc:sqlite:" + DBpath); //ezeknek kicsit utana jartam itt: https://www.sqlite.org/wal.html

        Statement stmt = connection.createStatement();
        stmt.execute("PRAGMA busy_timeout = 3000");
        createTables(); //letre is hozzuk ezzel a paranccsal
    } //vegul ez nem lett relevans megoldas -> nem vezetett sajnos eredmenyre, ezert lett bevezetve a szionkron kommunikacio es request keresek kuldese a GUI-tol a Server fele -> csak a szerver nyul az adatbazishoz

    /**
     * Az osszes regisztralt agent azonositojanak lekerese
     * A lekérdezes a snapshots tablaban szereplo osszes
     * kulonbozo agentID-t visszaadja
     *
     * @return agentID-k listaja
     * @throws SQLException ha a lekerdezes sikertelen
     */
    public List<String> getAgents() throws SQLException { //ahogy lattuk a lekerdezest a GUI-ban, ez listat ad vissza neki -> List<String>
        List<String> agents = new ArrayList<>();
        Statement stmt = connection.createStatement(); //Statement tipusbol innentol sok lesz, ezek hajtanak vegre lekerdezest, tabla letrehozast, torlest stb, akarcsak SQL-ben. Ez kotott parancs ezert csak statement.
        //Statement tobbszor fordul, es csak egyszer fut peldanyonkent
        ResultSet rs = stmt.executeQuery("SELECT DISTINCT agentID FROM snapshots"); //lekerdezzuk egy set (halmaz) -ba
        while (rs.next()) {
            agents.add(rs.getString("agentID")); //vegigiiteralunk rajta es visszaadjuk a listat a keronek
        }
        return agents;
    }

    /**
     * Adott agent legutolso snapshot-janak lekerese
     * A timestamp szerint csokkeno sorrendben rendezi, es az elsot adja vissza
     *
     * @param agentID az agent azonositoja
     * @return DataSnapshot objektum a legfrissebb adatokkal, vagy null ha nincs
     * @throws SQLException ha a lekerdezes sikertelen
     */
    public DataSnapshot getLastSnapshot(String agentID) throws SQLException { //ez is lenyegeben ugyanaz, csak a single mezoket kerdezi le
        PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM snapshots WHERE agentID = ? ORDER BY timestamp DESC LIMIT 1"); //ez egy nagyon fontos lekerdezes: a lenyeg, hogy az egesz rekordot lekerjuk (osszes mert adat), idorend szerint rendezzuk es kivalasztjuk a legelsot (idoben a legnagyobbat, azaz a legutolso mert adatot)
        ps.setString(1, agentID); //a preparedstatement ezert lett hasznalva, hogy az elore nem ismert agent-re kerdezhessen le -> ezt jeloli a '?' es utana a setstring
        ResultSet rs = ps.executeQuery(); //es utolag tudjuk futtatni, amikor mar fix lesz -> ez egyszer fordul, tobbszor futatthato

        if (rs.next()) { //ha tartalmaz: lekerdezzuk az osszes mezot
            DataSnapshot snapshot = new DataSnapshot();
            snapshot.agentID = rs.getString("agentID");
            snapshot.timestamp = rs.getLong("timestamp");
            snapshot.OSname = rs.getString("OSname");
            snapshot.OSversion = rs.getString("OSversion");
            snapshot.CPUusagePercent = rs.getDouble("CPUusagePercent");
            snapshot.totalMemoryBytes = rs.getLong("totalMemoryBytes");
            snapshot.usedMemoryBytes = rs.getLong("usedMemoryBytes");
            snapshot.physicalCores = rs.getInt("physicalCores");
            snapshot.logicalCores = rs.getInt("logicalCores");
            snapshot.architecture = rs.getString("architecture");
            snapshot.uptimeSeconds = rs.getLong("uptimeSeconds");
            snapshot.swapTotalBytes = rs.getLong("swapTotalBytes");
            snapshot.swapUsedBytes = rs.getLong("swapUsedBytes");
            return snapshot;
        }
        return null; //kulonben ures, ezt nem kapna meg a socket, es itt allna le magatol a socket, ezert nem is kifejezetten implementaltam a socket.close()-t
    }

    /**
     * Adott agent legutolso snapshot-jahoz tartozo lemezadatok lekerese a GUIserverconnection-bol szinten
     *
     * @param agentID az agent azonositoja
     * @return DiskInfo objektumok listaja
     * @throws SQLException ha a lekerdezes sikertelen
     */
    public List<DiskInfo> getDisks(String agentID) throws SQLException { //lenyegeben ugyanaz csak a lemezekre
        PreparedStatement ps = connection.prepareStatement(
                "SELECT id FROM snapshots WHERE agentID = ? ORDER BY timestamp DESC LIMIT 1"); //prepare eseteben rogton beadjuk a parancsot, megkeressuk amegfelelo agent-et akit kert a connection
        ps.setString(1, agentID);
        ResultSet rs = ps.executeQuery(); //vegrehajtjuk
        if (!rs.next()) return new ArrayList<>(); //ha nem talaljuk, akkor ures listat adunk vissza

        long snapshotID = rs.getLong("id"); //getteljuk az ID-t ahol amit meg nem tudunk elore -> PreparedStatement
        PreparedStatement diskPs = connection.prepareStatement(
                "SELECT * FROM diskinfo WHERE snapshotID = ?");
        diskPs.setLong(1, snapshotID);
        ResultSet diskRs = diskPs.executeQuery(); //diskRs resultset megszerzese

        List<DiskInfo> disks = new ArrayList<>();
        while (diskRs.next()) { //majd vegigjaras a resultset-en
            DiskInfo disk = new DiskInfo();
            disk.mountPoint = diskRs.getString("mountPoint");
            disk.totalBytes = diskRs.getLong("totalBytes");
            disk.usedBytes = diskRs.getLong("usedBytes");
            disk.readSpeed = diskRs.getDouble("readSpeed");
            disk.writeSpeed = diskRs.getDouble("writeSpeed");
            disks.add(disk);
        }
        return disks; //visszaadjuk az adott Agent legutolso listajat
    }

    /**
     * Adott agent legutolso snapshot-jahoz tartozo halozati adatok lekerese
     *
     * @param agentID az agent azonositoja
     * @return NetworkInfo objektumok listaja
     * @throws SQLException ha a lekerdezes sikertelen
     */
    public List<NetworkInfo> getNetworks(String agentID) throws SQLException { //a halozatok lekerese, ugyanigy
        PreparedStatement ps = connection.prepareStatement(
                "SELECT id FROM snapshots WHERE agentID = ? ORDER BY timestamp DESC LIMIT 1");
        ps.setString(1, agentID);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) return new ArrayList<>();

        long snapshotID = rs.getLong("id");
        PreparedStatement netPs = connection.prepareStatement(
                "SELECT * FROM networkinfo WHERE snapshotID = ?");
        netPs.setLong(1, snapshotID);
        ResultSet netRs = netPs.executeQuery();

        List<NetworkInfo> networks = new ArrayList<>();
        while (netRs.next()) {
            NetworkInfo net = new NetworkInfo();
            net.interfaceName = netRs.getString("interfaceName");
            net.ipAddress = netRs.getString("ipAddress");
            net.macAddress = netRs.getString("macAddress");
            net.domainName = netRs.getString("domainName");
            net.defaultGateway = netRs.getString("defaultGateway");
            net.dnsServer = netRs.getString("dnsServer");
            net.hostName = netRs.getString("hostName");
            networks.add(net);
        }
        return networks;
    }

    /**
     * Adott agent legutolso snapshot-jahoz tartozo folyamatinformaciok lekerese
     *
     * @param agentID az agent azonositoja
     * @return ProcessInfo objektumok listaja
     * @throws SQLException ha a lekerdezes sikertelen
     */
    public List<ProcessInfo> getProcesses(String agentID) throws SQLException { // -||-
        PreparedStatement ps = connection.prepareStatement(
                "SELECT id FROM snapshots WHERE agentID = ? ORDER BY timestamp DESC LIMIT 1");
        ps.setString(1, agentID);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) return new ArrayList<>();

        long snapshotID = rs.getLong("id");
        PreparedStatement procPs = connection.prepareStatement(
                "SELECT * FROM processinfo WHERE snapshotID = ?");
        procPs.setLong(1, snapshotID);
        ResultSet procRs = procPs.executeQuery();

        List<ProcessInfo> processes = new ArrayList<>();
        while (procRs.next()) {
            ProcessInfo proc = new ProcessInfo();
            proc.processName = procRs.getString("processName");
            proc.CPUpercent = procRs.getDouble("CPUpercent");
            proc.memoryBytes = procRs.getLong("memoryBytes");
            processes.add(proc);
        }
        return processes;
    }

    // ---------- TABLA LETREHOZASI SZEKCIO ----------

    /**
     * Az adatbazis tablak letrehozasa (ha meg nem leteznek)
     * A metodus letrehozza a snapshots, diskinfo, networkinfo es processinfo tablakat
     * a megfelelo foreign key kapcsolatokkal
     *
     * @throws SQLException ha a tablak letrehozasa sikertelen
     */
    private void createTables() throws SQLException { //szokasos egyszer futo parancsok inicializalaskor -> kulon single es a listak semaja
        Statement statement = connection.createStatement();

        // fo tabla a snapshot-ok szamara (nem-lista mezok)
        statement.execute("CREATE TABLE IF NOT EXISTS snapshots (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "agentID TEXT," +
                "timestamp INTEGER," +
                "OSname TEXT," +
                "OSversion TEXT," +
                "uptimeSeconds INTEGER," +
                "architecture TEXT," +
                "physicalCores INTEGER," +
                "logicalCores INTEGER," +
                "CPUusagePercent REAL," +
                "totalMemoryBytes INTEGER," +
                "usedMemoryBytes INTEGER," +
                "swapTotalBytes INTEGER," +
                "swapUsedBytes INTEGER)"); //konkatenalva irtam, mert en igy jobban atlatom a semat, de lehet egy sorban is nyilvan

        statement.execute("CREATE TABLE IF NOT EXISTS diskinfo (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "snapshotID INTEGER," +
                "mountPoint TEXT," +
                "totalBytes INTEGER," +
                "usedBytes INTEGER," +
                "readSpeed REAL," +
                "writeSpeed REAL," +
                "FOREIGN KEY (snapshotID) REFERENCES snapshots(id))");

        statement.execute("CREATE TABLE IF NOT EXISTS networkinfo (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "snapshotID INTEGER," +
                "interfaceName TEXT," +
                "ipAddress TEXT," +
                "macAddress TEXT," +
                "domainName TEXT," +
                "defaultGateway TEXT," +
                "dnsServer TEXT," +
                "hostName TEXT," +
                "FOREIGN KEY (snapshotID) REFERENCES snapshots(id))");

        statement.execute("CREATE TABLE IF NOT EXISTS processinfo (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "snapshotID INTEGER," +
                "processName TEXT," +
                "CPUpercent REAL," +
                "memoryBytes INTEGER," +
                "FOREIGN KEY (snapshotID) REFERENCES snapshots(id))");
    }

    /**
     * Egy teljes DataSnapshot elmentese az adatbazisba, ahogy kerte is a kiiras
     * <p>
     * A metodus synchronized, hogy tobb agent egyideju mentese
     * ne okozzon adatbazis konkurencia problemakat. Eloszor a fo
     * snapshot-ot menti el, majd a generalt ID-t felhasznalva
     * a kapcsolodo listakat (disks, interfaces, processes)
     * </p>
     *
     * @param snapshot a mentendo adatpillanat
     * @throws SQLException ha az adatbazis muvelet sikertelen
     */
    public synchronized void save(DataSnapshot snapshot) throws SQLException { //mentesi metodus -> kap egy snapshot-ot es ezeket beteszi a tablaba (INSERT)
        PreparedStatement ps = connection.prepareStatement("""
            INSERT INTO snapshots (
                                    agentID,
                                    timestamp,
                                    OSname,
                                    OSversion,
                                    uptimeSeconds,
                                    architecture,
                                    physicalCores,
                                    logicalCores,
                                    CPUusagePercent,
                                    totalMemoryBytes,
                                    usedMemoryBytes,
                                    swapTotalBytes,
                                    swapUsedBytes
                                    )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, Statement.RETURN_GENERATED_KEYS); // '?' mivel itt sem ismertek az ertekek, alatta adjuk meg oket

        ps.setString(1, snapshot.agentID); //innentol lefele
        ps.setLong(2, snapshot.timestamp);
        ps.setString(3, snapshot.OSname);
        ps.setString(4, snapshot.OSversion);
        ps.setLong(5, snapshot.uptimeSeconds);
        ps.setString(6, snapshot.architecture);
        ps.setInt(7, snapshot.physicalCores);
        ps.setInt(8, snapshot.logicalCores);
        ps.setDouble(9, snapshot.CPUusagePercent);
        ps.setLong(10, snapshot.totalMemoryBytes);
        ps.setLong(11, snapshot.usedMemoryBytes);
        ps.setLong(12, snapshot.swapTotalBytes);
        ps.setLong(13, snapshot.swapUsedBytes);
        ps.executeUpdate(); //majd vegrehajtjuk

        long snapshotID = ps.getGeneratedKeys().getLong(1); //hozzaadunk egy automata oszlopot ai general egy autoincremet alapu ID-t

        // diskek mentese
        if (snapshot.disks != null) {
            for (DiskInfo disk : snapshot.disks) { //az osszes disk mentese, a logika hasonlo
                PreparedStatement diskps = connection.prepareStatement("""
                INSERT INTO diskinfo (
                                      snapshotID,
                                      mountPoint,
                                      totalBytes,
                                      usedBytes,
                                      readSpeed,
                                      writeSpeed
                                      )
                VALUES (?, ?, ?, ?, ?, ?)
            """);
                diskps.setLong(1, snapshotID);
                diskps.setString(2, disk.mountPoint);
                diskps.setLong(3, disk.totalBytes);
                diskps.setLong(4, disk.usedBytes);
                diskps.setDouble(5, disk.readSpeed);
                diskps.setDouble(6, disk.writeSpeed);
                diskps.executeUpdate();
            }
        }

        // networkok mentese
        if (snapshot.interfaces != null) { // -||-
            for (NetworkInfo net : snapshot.interfaces) {
                PreparedStatement netps = connection.prepareStatement("""
                INSERT INTO networkinfo (
                                      snapshotID,
                                      interfaceName,
                                      ipAddress,
                                      macAddress,
                                      domainName,
                                      defaultGateway,
                                      dnsServer,
                                      hostName
                                      )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """);
                netps.setLong(1, snapshotID);
                netps.setString(2, net.interfaceName);
                netps.setString(3, net.ipAddress);
                netps.setString(4, net.macAddress);
                netps.setString(5, net.domainName);
                netps.setString(6, net.defaultGateway);
                netps.setString(7, net.dnsServer);
                netps.setString(8, net.hostName);
                netps.executeUpdate();
            }
        }

        // processzek mentese
        if (snapshot.processes != null) { // -||-
            for (ProcessInfo process : snapshot.processes) {
                PreparedStatement processps = connection.prepareStatement("""
                INSERT INTO processinfo(
                                        snapshotID,
                                        processName,
                                        CPUpercent,
                                        memoryBytes
                                        )
                VALUES (?, ?, ?, ?)
                """);
                processps.setLong(1, snapshotID);
                processps.setString(2, process.processName);
                processps.setDouble(3, process.CPUpercent);
                processps.setLong(4, process.memoryBytes);
                processps.executeUpdate();
            }
        }
    }
}