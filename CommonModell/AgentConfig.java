package SystemMonitoring.CommonModell;

/**
 * Agent konfiguraciojat leiro osztaly -> json-ok ezt valositjak majd meg
 * <p>
 * Ez az osztaly felel meg az agent_config.json fajl strukturajanak.
 * A GSON konyvtar automatikusan hozzaadja a megfelelo ertekeket a
 * JSON fajlbol a public mezőkhöz
 * </p>
 *
 * @author Arki Abel, FE8YA2
 * @version 1.0
 * @since 2026-05-06
 * @see ConfigLoader
 * @see ServerConfig
 */
public class AgentConfig {
    /** A szerver hosztneve vagy IP cime */
    public String serverHost;

    /** A szerver portszama (alapbol 55555), ezt a laboros peldabol emeltem ki */
    public int serverPort;

    /** Adatgyujtes kozotti szunet masodpercben */
    public int pollSeconds;

    /** Agent egyedi azonositoja (opcionalis, ha nem adjuk meg, automatikusan generalodik) */
    public String agentID;
}