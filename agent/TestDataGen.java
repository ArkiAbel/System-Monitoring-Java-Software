package SystemMonitoring.agent;

import SystemMonitoring.CommonModell.DataSnapshot;
import SystemMonitoring.CommonModell.DiskInfo;
import SystemMonitoring.CommonModell.NetworkInfo;
import SystemMonitoring.CommonModell.ProcessInfo;
//oshi-k itt nem kellenek :)
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Tesztadatokat generalo agent osztaly a Collector kiterjesztesekent, az elagazasban ha testmode == true, akkor ez hivodik meg
 * <p>
 * Ez az osztaly a valos adatgyujtes helyettesitesere szolgal, lehetove teve
 * tobb virtualis host szimulalasat anelkul, hogy valos VM-ekre vagy tobb fizikai
 * gepekre lenne szukseg. A generalt adatoknal igyekeztem, hogy minel realisztikusabbak legyenek
 * formatumuk es frissulesi gyakorisaguk alapjan, es megfeleljenek a DataSnapshot strukturanak
 * </p>
 *
 * <p>A statikus adatok (OS nev, architektura, magok szama, stb.) egy sesison belul
 * allandok, mig a dinamikus adatok (CPU kihasznaltsag, memoria hasznalat, stb.)
 * minden collect() hivasnal valtoznak. Ez nagyjabol a valos rendszer viselkedeset utanozza.</p>
 *
 * <p>Hasznalata a -t flaggel inditott agent eseten:</p>
 * <pre>
 * java SystemMonitoring.agent.Agent -t
 * </pre>
 *
 * @author Arki Abel, FE8YA2
 * @version 1.0
 * @since 2026-05-06
 * @see Collector
 * @see DataSnapshot
 */
public class TestDataGen extends Collector {
    private final Random random = new Random();
    private final String agentID;

    //meres ertelmeben statikus adatok
    private final String OSname;
    private final String OSversion;
    private final String architecture;
    private final int physicalCores;
    private final int logicalCores;
    private final long totalMemoryBytes;
    private final long swapTotalBytes;
    private final List<String> diskMountPoints;
    private final List<Long> diskTotalBytes;

    //network statikus mezoi
    private final List<String> interfaceNames;
    private final List<String> macAddresses;
    private final List<String> ipAddresses;
    private final String defaultGateway;
    private final String dnsServer;
    private final String domainName;
    private final String hostName;
    private final int interfaceCount;

    // random tombok a valtozatosabb tesztadatokhoz
    private static final String[] OS_NAMES = {"Windows", "Linux", "macOS"};
    private static final String[] OS_VERSIONS = {"11", "10", "Ubuntu 22.04", "Debian 12", "Ventura 13.0"};
    private static final String[] ARCHITECTURES = {"x86_64", "ARM64", "x86"};
    private static final String[] INTERFACE_NAMES = {"eth0", "wlan0", "enp3s0", "wlp2s0"};
    private static final String[] PROCESS_NAMES = {"chrome.exe", "java.exe", "explorer.exe", "discord.exe", "spotify.exe", "firefox.exe", "python.exe", "node.exe", "FNAF.exe", "Blender.exe"};
    private static final String[] MOUNT_POINTS = {"C:\\", "D:\\", "/", "/home", "/boot"};
    private static final String[] GATEWAYS = {"192.168.1.1", "192.168.0.1", "10.0.0.1"};
    private static final String[] DNS_SERVERS = {"8.8.8.8", "1.1.1.1", "9.9.9.9"};
    private static final String[] DOMAINS = {"local", "home.arpa", "---"};
    private static final String[] HOST_NAMES = {"desktop-pc", "laptop-01", "server-node"};

    /**
     * Konstruktor -> Letrehoz egy teszt agentet a megadott azonosítóval.
     * A konstruktor allando (statikus) adatokat general random modon,
     * amelyek a teljes futtatas soran valtozatlanok maradnak
     *
     * @param agentID az agent egyedi azonositoja (pl. "agent-123456789")
     */
    public TestDataGen(String agentID) {
        this.agentID = agentID; //ua a bemeneti konstruktor mint a valodinal

        //statikus adatok is itt lesznek definialva
        OSname = randomFrom(OS_NAMES);
        OSversion = randomFrom(OS_VERSIONS);
        architecture = randomFrom(ARCHITECTURES);
        physicalCores = random.nextInt(4,16);
        logicalCores = physicalCores * 2;
        totalMemoryBytes = (long) random.nextInt(8, 64) * 1024 * 1024 * 1024; //1024^3 nem volt jo, az itt mast jelent
        swapTotalBytes = 8L * 1024 * 1024 * 1024;

        // ----- statikus disk reszleg -----
        int diskCount = random.nextInt(1, 4); //2 bemenetu Rand hasznalata
        diskMountPoints = new ArrayList<>();
        diskTotalBytes = new ArrayList<>();

        for (int i = 0; i < diskCount; i++) {
            diskMountPoints.add(randomFrom(MOUNT_POINTS));
            diskTotalBytes.add( (long) random.nextInt(256, 2048) * 1024 * 1024 * 1024 );
        }

        //statikus network reszleg
        defaultGateway = randomFrom(GATEWAYS);
        dnsServer = randomFrom(DNS_SERVERS);
        domainName = randomFrom(DOMAINS);
        hostName = randomFrom(HOST_NAMES);

        interfaceCount = random.nextInt(1, 4); //letrehozzuk majd ugyanazokat a listakat itt is
        interfaceNames = new ArrayList<>();
        macAddresses = new ArrayList<>();
        ipAddresses = new ArrayList<>();

        for (int i = 0; i < interfaceCount; i++) {
            interfaceNames.add(randomFrom(INTERFACE_NAMES));
            macAddresses.add(String.format("%02X:%02X:%02X:%02X:%02X:%02X",
                    random.nextInt(255), random.nextInt(255), random.nextInt(255),
                    random.nextInt(255), random.nextInt(255), random.nextInt(255)));
            ipAddresses.add("192.168." + random.nextInt(255) + "." + random.nextInt(255));
        } //formazott kiiras alkalmazasa laboranyag alapjan
    }

    /**
     * Segedmetodus random elem kivalasztasara egy tombol
     * Generikus implementacio, igy barmilyen tipusu tombbel mukodik
     *
     * @param array a tomb, amelybol valasztani szeretnenk
     * @param <T> a tomb tipusa
     * @return random elem a tombol
     */
    private <T> T randomFrom(T[] array) {
        return array[random.nextInt(array.length)];
    } //univerzalis fuggveny generikus template-szeru parametert fogad, igy mindenhol meghivhato fuggetlenul

    /**
     * Tesztadatok generalasa -> statikus adatokat (OS, architektura, stb.)
     * a konstruktorban beallitott ertekekbol veszi, a dinamikus adatokat
     * (CPU kihasznaltsag, memoria hasznalat, processzek) random generalja.
     *
     * @return DataSnapshot objektum szimulalt adatokkal
     */
    @Override
    public DataSnapshot collect() {

        DataSnapshot snapshot = new DataSnapshot();
        snapshot.agentID = agentID;
        snapshot.timestamp = System.currentTimeMillis();

        // statikus adatok atadasa a snapshot-ba
        snapshot.OSname = OSname;
        snapshot.OSversion = OSversion;
        snapshot.architecture = architecture;
        snapshot.physicalCores = physicalCores;
        snapshot.logicalCores = logicalCores;
        snapshot.totalMemoryBytes = totalMemoryBytes;
        snapshot.swapTotalBytes = swapTotalBytes;
        snapshot.uptimeSeconds = random.nextLong(100000);

        snapshot.disks = new ArrayList<>();
        snapshot.interfaces = new ArrayList<>();
        snapshot.processes = new ArrayList<>();

        //CPU usage reszleg -- szimulalt
        snapshot.CPUusagePercent = random.nextDouble(100); //double-kent taroljuk alapbol -> nextDouble

        //RAM reszleg -- szimulalt memoriahasznalat a teljes memoria max 90%-a (nekem altalaban tobb :) )
        snapshot.usedMemoryBytes = (long) (snapshot.totalMemoryBytes * random.nextDouble(0.9));
        snapshot.swapUsedBytes = (long) (snapshot.swapTotalBytes * random.nextDouble(0.5));

        //DISK reszleg -- szimulalt lemezadatok -> tovabbi feltoltesek innentol ugyanazok
        List<DiskInfo> disks = new ArrayList<>();

        for (int i = 0; i < diskMountPoints.size(); i++) {
            DiskInfo testDisk = new DiskInfo();
            testDisk.mountPoint = diskMountPoints.get(i);
            testDisk.totalBytes = diskTotalBytes.get(i);
            testDisk.usedBytes = (long) (testDisk.totalBytes * random.nextDouble(0.8));
            testDisk.readSpeed = random.nextDouble(500);
            testDisk.writeSpeed = random.nextDouble(500);
            disks.add(testDisk);
        }
        snapshot.disks = disks;

        //Network reszleg -- szimulalt halozati adatok
        List<NetworkInfo> interfaces = new ArrayList<>();
        for (int i = 0; i < interfaceCount; i++) {
            NetworkInfo testNetwork = new NetworkInfo();
            testNetwork.interfaceName = interfaceNames.get(i);
            testNetwork.macAddress = macAddresses.get(i);
            testNetwork.ipAddress = ipAddresses.get(i);
            testNetwork.defaultGateway = defaultGateway;
            testNetwork.dnsServer = dnsServer;
            testNetwork.domainName = domainName;
            testNetwork.hostName = hostName;
            interfaces.add(testNetwork);
        }
        snapshot.interfaces = interfaces;

        //Process reszleg -- szimulalt folyamatok adatai
        List<ProcessInfo> processes = new ArrayList<>();
        int processCount = random.nextInt(3,8);

        for (int i = 0; i < processCount; i++) {
            ProcessInfo testProcess = new ProcessInfo();
            testProcess.processName = randomFrom(PROCESS_NAMES);
            testProcess.CPUpercent = random.nextDouble(20);
            testProcess.memoryBytes = random.nextLong(500) * 1024 * 1024;
            processes.add(testProcess);
        }
        snapshot.processes = processes;

        return snapshot;
    }
}