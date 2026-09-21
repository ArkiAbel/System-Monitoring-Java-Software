package SystemMonitoring.agent;

import SystemMonitoring.CommonModell.DiskInfo;
import SystemMonitoring.CommonModell.NetworkInfo;
import SystemMonitoring.CommonModell.ProcessInfo;
import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.software.os.OSFileStore;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.software.os.NetworkParams;

import SystemMonitoring.CommonModell.DataSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Valos rendszerinformaciokat gyujto osztaly az OSHI konyvtar segitsegevel, ahogy kerte a feladat
 * <p>
 * Ez az osztaly felel a monitorozott host osszes hardver-es szoftverinformaciojanak
 * osszegyujteseert. Az OSHI konyvtar platformfuggetlen megoldast bizosit, igy
 * ugyanaz a kod fut Windows, Linux es macOS rendszereken is, ez kulon fontos egy ilyen rendszernel
 * </p>
 *
 * <p>Az osztaly a kovetkezo adatokat gyujti (atfogoan, a konkretumok majd lentebb lathatok):</p>
 * <ul>
 *   <li>OS adatok: nev, verzio, uptime</li>
 *   <li>CPU adatok: architektura, magok szama, aktualis kihasznaltsag</li>
 *   <li>RAM adatok: teljes es hasznalt memoria, swap</li>
 *   <li>Disk adatok: particiok, meret, kihasznaltsag, I/O sebessegek</li>
 *   <li>Network adatok: interfeszek, IP cimek, gateway, DNS</li>
 *   <li>Process adatok: futo processzek neve, CPU es memoriahasznalata</li>
 * </ul>
 *
 * @author Arki Abel, FE8YA2
 * @version 1.0
 * @since 2026-05-06
 * @see TestDataGen
 * @see DataSnapshot
 */
public class Collector {

    //segedosztalyok a megfelelo lekerdezo metodusok eleresehez
    protected final SystemInfo systemInfo;
    protected final HardwareAbstractionLayer harAbsLay;
    protected final OperatingSystem os;
    NetworkParams networkParams;

    /**
     * Konstruktor -> inicializalja az OSHI SystemInfo objektumot es eleri
     * a hardveres es operacios rendszerhez kapcsolodo absztrakcios retegeket
     */
    public Collector() {
        systemInfo = new SystemInfo();
        harAbsLay = systemInfo.getHardware();
        os = systemInfo.getOperatingSystem();
        networkParams = os.getNetworkParams();
    }

    /**
     * Osszegyujti az osszes rendszerinformaciot egy adatpillanatban
     * <p>
     * A metodus a CPU kihasznaltsag meresehez 1 masodpercet var, mivel
     * a terheles szamitasa ket idopont kozotti kulonbseget igenyel
     * </p>
     *
     * @return DataSnapshot objektum a begyujtott osszes adattal
     */
    public DataSnapshot collect() {
        DataSnapshot snapshot = new DataSnapshot();

        snapshot.timestamp = System.currentTimeMillis(); //fontos, hogy mindig a legutoso adatot kapja vissza a UI

        //OS reszleg ----------
        snapshot.OSname = os.getFamily();
        snapshot.OSversion = os.getVersionInfo().getVersion();
        snapshot.uptimeSeconds = os.getSystemUptime();

        //CPU reszleg ----------
        CentralProcessor cpu = harAbsLay.getProcessor();

        snapshot.architecture = cpu.getProcessorIdentifier().getMicroarchitecture();
        snapshot.physicalCores = cpu.getPhysicalProcessorCount();
        snapshot.logicalCores = cpu.getLogicalProcessorCount();

        //CPU usage reszleg ----------
        long[] prevTicks = cpu.getSystemCpuLoadTicks();
        try { //try-catch ag mert dobhet kivetelt a szal altatasa
            Thread.sleep(1000); //nem lehet egy idopontban merni (nem ad adatot), hanem csak idoeltolodasbol szamithato, utolag kis segitseggel sikerult megoldani
        } //1000ms delay
        catch (InterruptedException e) { //sleep miatt muszaj
            Thread.currentThread().interrupt(); //lezaras a szokasos modon
            System.out.println("CPU measurement interrupted: " + e.getMessage());
        }
        snapshot.CPUusagePercent = cpu.getSystemCpuLoadBetweenTicks(prevTicks) * 100; //percentage miatti szorzas

        //RAM reszleg ----------
        GlobalMemory memory = harAbsLay.getMemory(); //hardwareAbstractionLayer-t reprezentalja

        snapshot.totalMemoryBytes = memory.getTotal();
        snapshot.usedMemoryBytes = memory.getTotal() - memory.getAvailable(); //dinamikus kiszamitas
        snapshot.swapTotalBytes = memory.getVirtualMemory().getSwapTotal();
        snapshot.swapUsedBytes = memory.getVirtualMemory().getSwapUsed();

        //DISK reszleg
        //itt tortent hibajavitas es modositas az elso veglegesnek tekintheto verzioban
        List<OSFileStore> fileStores = os.getFileSystem().getFileStores(); //seged lista, getteli az elerheto fileStore-okat
        List<DiskInfo> disks = new ArrayList<>();

        for (OSFileStore fs : fileStores) { //eszlelt lemezeken valo vegigiteralas es adataik lekerese, listaba helyezese
            DiskInfo diskInfo = new DiskInfo();
            diskInfo.mountPoint = fs.getMount();
            diskInfo.totalBytes = fs.getTotalSpace();
            diskInfo.usedBytes = fs.getTotalSpace() - fs.getUsableSpace();
            diskInfo.readSpeed = 0; //placeholder lesz most, hogy tudjuk addolni a disks-be
            diskInfo.writeSpeed = 0;
            disks.add(diskInfo);
        }

        List<HWDiskStore> hwDisks = harAbsLay.getDiskStores(); //szinten seged lista, getteli az elerheto Disk-eket
        for (int i = 0; i < hwDisks.size() && i < disks.size(); i++) {
            disks.get(i).readSpeed = hwDisks.get(i).getReadBytes(); //itt pedig getteljuk mind a kettot es atadjuk az erteket
            disks.get(i).writeSpeed = hwDisks.get(i).getWriteBytes();
        }
        snapshot.disks = disks; //majd csak a vegen adjuk a snapshotnak, azaz "merge"-eljuk a ket kapott listat ("join on indexes" szeruen)

        //Network reszleg
        List<NetworkIF> networkIFs = harAbsLay.getNetworkIFs(); //ez hasonloan getteli a network interface-eket
        List<NetworkInfo> interfaces = new ArrayList<>();

        NetworkParams networkParams = os.getNetworkParams(); //ez egy getter interface, ezen keresztul hivatkozunk
        String hostName = networkParams.getHostName();
        String defaultGateway = networkParams.getIpv4DefaultGateway();
        String domainName = networkParams.getDomainName();
        String dnsServer = networkParams.getDnsServers().length > 0 ? networkParams.getDnsServers()[0] : "---";

        for (NetworkIF net : networkIFs) { //es atadjuk az infot
            NetworkInfo netInfo = new NetworkInfo();
            netInfo.interfaceName = net.getName();
            netInfo.ipAddress = net.getIPv4addr().length > 0 ? net.getIPv4addr()[0] : "---"; //az elso indexen levo elem amit keresunk, megnezzuk, hogy nem null-e majd beallitjuk "String"-kent
            netInfo.macAddress = net.getMacaddr();
            netInfo.hostName = hostName;
            netInfo.defaultGateway = defaultGateway;
            netInfo.domainName = domainName;
            netInfo.dnsServer = dnsServer;
            interfaces.add(netInfo);
        }
        snapshot.interfaces = interfaces;

        //Process reszleg adatai
        List<OSProcess> osProcesses = os.getProcesses(); //hasonlo getteles mint a fentiekben
        List<ProcessInfo> processes = new ArrayList<>();

        for (OSProcess process : osProcesses) {
            ProcessInfo processInfo = new ProcessInfo();
            processInfo.processName = process.getName();
            processInfo.CPUpercent = process.getProcessCpuLoadCumulative() * 100;
            processInfo.memoryBytes = process.getResidentSetSize();
            processes.add(processInfo);
        }
        snapshot.processes = processes;

        return snapshot; //visszaadjuk az egy meres eredmenyeit, ezt fogjuk ismetelni vegig
    };
}