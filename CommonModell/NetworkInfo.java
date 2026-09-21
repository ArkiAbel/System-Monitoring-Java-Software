package SystemMonitoring.CommonModell;

/**
 * Halozati interfesz informacioit tarolo osztaly
 * <p>
 * Egy NetworkInfo objektum egyetlen halozati interfesz adatait
 * tartalmazza: interfesz neve, IP cim, MAC cim, valamint a
 * rendszerszintu halozati beallitasok (gateway, DNS, domain, hostname)
 * </p>
 *
 * <p>A hostName, defaultGateway, dnsServer es domainName a
 * rendszerszintu beallitasok, ezert ezek minden interfeszhez
 * ugyanazok lesznek</p>
 *
 * @author Arki Abel, FE8YA2
 * @version 1.0
 * @since 2026-05-06
 * @see DataSnapshot
 */
public class NetworkInfo {
    public String interfaceName;
    public String ipAddress;
    public String macAddress;
    public String domainName;
    public String defaultGateway;
    public String dnsServer;
    public String hostName;

    //getterek a tablazatnak
    public String getInterfaceName() { return interfaceName; }
    public String getIpAddress() { return ipAddress; }
    public String getMacAddress() { return macAddress; }
    public String getDomainName() { return domainName; }
    public String getDefaultGateway() { return defaultGateway; }
    public String getDnsServer() { return dnsServer; }
    public String getHostName() { return hostName; }
}