package SystemMonitoring.CommonModell;

/**
 * Szerver konfiguraciojat leiro osztaly, lenyegeben ugyanaz mint az AgentConfig logikaja
 * <p>
 * Ez az osztaly felel meg a server_config.json fajl strukturajanak.
 * A szerver inditasakor ebbol a fajlbol olvassa ki a beallitasokat.
 * </p>
 *
 * @author Arki Abel, FE8YA2
 * @version 1.0
 * @since 2026-05-06
 * @see ConfigLoader
 * @see AgentConfig
 */
public class ServerConfig { //2 mezot tartalmaz
    /** A szerver portszama, amelyen az agenteket várja (szokasosan 55555) */
    public int port;

    /** Az SQLite adatbazis fajl eleresi utvonala */
    public String dbPath;
}