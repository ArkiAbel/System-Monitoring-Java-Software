package SystemMonitoring.CommonModell;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Konfiguracios fajlok betolto osztalya.
 * <p>
 * Ez az osztaly felel a JSON formatumu config fajlok beolvasasert
 * es a megfelelo objektumokka alakitasaert. A GSON konyvtarat hasznalja
 * a deszerializacios folyamathoz.
 * </p>
 *
 * <p>Hasznalati peldak:</p>
 * <pre>
 * AgentConfig agentConf = ConfigLoader.loadAgentConfig("agent_config.json");
 * ServerConfig serverConf = ConfigLoader.loadServerConfig("server_config.json");
 * </pre>
 *
 * @author Arki Abel, FE8YA2
 * @version 1.0
 * @since 2026-05-06
 * @see AgentConfig
 * @see ServerConfig
 * @see .Gson
 */
public class ConfigLoader {

    private static final Gson gson = new Gson(); //static, mivel egy Gson-t hasznal mindenki (osztalyvaltozo), nem akarunk mindig ujat letrehozni, final mert nem lehet felulirni

    /**
     * Agent konfiguracios fajl betoltese
     *
     * @param agentpath a konfiguracios fajl eleresi utvonala
     * @return AgentConfig objektum a beolvasott adatokkal
     * @throws IOException ha a fajl nem olvashato vagy nem talalhato a helyen
     */
    public static AgentConfig loadAgentConfig(String agentpath) throws IOException { //sima beolvaso fuggveny
        BufferedReader br = new BufferedReader(new FileReader(agentpath));
        return gson.fromJson(br, AgentConfig.class); //Json alakitasa AgentConfig.class objektumma
    }

    /**
     * Szerver konfiguracios fajl betoltese
     *
     * @param serverpath a konfiguracios fajl eleresi utvonala
     * @return ServerConfig objektum a beolvasott adatokkal
     * @throws IOException ha a fajl nem olvashato/talalhato
     */
    public static ServerConfig loadServerConfig(String serverpath) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(serverpath));
        return gson.fromJson(br, ServerConfig.class); // ua, csak szerver eseteben
    }
}