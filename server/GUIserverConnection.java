package SystemMonitoring.server;

import java.io.*;

import SystemMonitoring.CommonModell.DataSnapshot;
import SystemMonitoring.CommonModell.DiskInfo;
import SystemMonitoring.CommonModell.NetworkInfo;
import SystemMonitoring.CommonModell.ProcessInfo;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.net.Socket;
import java.util.List;

/**
 * GUI kliens (GUI) oldali kapcsolatkezelo osztalya
 * <p>
 * Ez az osztaly biztosit kenyelmes, metodus alapú hozzaferest
 * a szerver GUIhandler-jéhez. Minden lekerdezes tipushoz
 * tartozik egy metodus (pl. getAgents(), getLastSnapshot()),
 * amely elkuld a kivant JSON kerest a szervernek, majd
 * visszaadja a deszerializalt valaszt
 * </p>
 *
 * @author Arki Abel, FE8YA2
 * @version 1.0
 * @since 2026-05-06
 * @see GUIhandler
 * @see UIview
 */

public class GUIserverConnection {
    //reader, writer, socket a kommunikaciohoz
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private final Gson gson = new Gson();

    /**
     * Kapcsolodas a szerver GUI portjahoz -> 55556
     *
     * @param host a szerver hosztneve (pl. "localhost")
     * @param port a szerver portszama (alapbol 55556)
     * @throws IOException ha a kapcsolat nem hozhato letre
     */
    public void connect(String host, int port) throws IOException { //lenyegeben ugyanaz a connect, csak a GUI oldalarol, es ebbol csak egy van kezelve
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true); //autoflush!!
    }

    /**
     * Az osszes regisztralt agent azonositojanak lekerese
     *
     * @return agentID-k listaja
     * @throws IOException ha a hálózati kommunikacio sikertelen
     */
    public List<String> getAgents() throws IOException {
        JsonObject req = new JsonObject(); //ez maga a Json keres elokeszitese
        req.addProperty("type", "getAgents"); //property - tulajdonsag hozzaadasa: megegy sor a Json-ban
        writer.println(gson.toJson(req)); //a JsonObject-bol Json lesz es elkuldjuk a szervernek -> { "type" : "getAgents" } lesz a kinezete
        String response = reader.readLine(); //getInputStream-bol szerzi a valaszt, amint jon valmami
        return gson.fromJson(response, new TypeToken<List<String>>(){}.getType()); //ez a csavaros resz -> List<String> jon majd letre a fromJson deszerializacio utan
    } // Itt kellett tobb segitseg, de a TypeToken ^^^^^^ itt felul a jol ismert Type erasure jelenseget elozi meg, mivel nem tudja a gson, hogy az String
    /** Azaz a Typetoken megmondja a Jsonnak, hogy String fog jonni .getType() -> es igy mukodik ez a szinkron keres-valasz dolog */

    /**
     * Adott agent legutolso snapshot-janak lekerese
     *
     * @param agentID az agent azonositoja
     * @return DataSnapshot objektum (legutolso adatok)
     * @throws IOException ha a hálózati kommunikacio sikertelen
     */
    public DataSnapshot getLastSnapshot(String agentID) throws IOException { //es ugyanaz a procedura megy vegig itt is
        JsonObject req = new JsonObject();
        req.addProperty("type", "getLastSnapshot");
        req.addProperty("agentID", agentID);
        writer.println(gson.toJson(req));
        String response = reader.readLine();
        return gson.fromJson(response, DataSnapshot.class);
    }

    /**
     * Adott agent lemezadatainak lekerese
     *
     * @param agentID az agent azonositoja
     * @return DiskInfo objektumok listaja
     * @throws IOException ha a hálózati kommunikacio sikertelen
     */
    public List<DiskInfo> getDisks(String agentID) throws IOException { // -||-
        JsonObject req = new JsonObject();
        req.addProperty("type", "getDisks");
        req.addProperty("agentID", agentID);
        writer.println(gson.toJson(req));
        String response = reader.readLine();
        return gson.fromJson(response, new TypeToken<List<DiskInfo>>(){}.getType());
    }

    /**
     * Adott agent halozati adatainak lekerese
     *
     * @param agentID az agent azonositoja
     * @return NetworkInfo objektumok listaja
     * @throws IOException ha a halozati kommunikacio sikertelen
     */
    public List<NetworkInfo> getNetworks(String agentID) throws IOException { // -||-
        JsonObject req = new JsonObject();
        req.addProperty("type", "getNetworks");
        req.addProperty("agentID", agentID);
        writer.println(gson.toJson(req));
        String response = reader.readLine();
        return gson.fromJson(response, new TypeToken<List<NetworkInfo>>(){}.getType());
    }

    /**
     * Adott agent folyamatinformacioinak lekerese
     *
     * @param agentID az agent azonositoja
     * @return ProcessInfo objektumok listaja
     * @throws IOException ha a hálózati kommunikacio sikertelen
     */
    public List<ProcessInfo> getProcesses(String agentID) throws IOException { // -||-
        JsonObject req = new JsonObject();
        req.addProperty("type", "getProcesses");
        req.addProperty("agentID", agentID);
        writer.println(gson.toJson(req));
        String response = reader.readLine();
        return gson.fromJson(response, new TypeToken<List<ProcessInfo>>(){}.getType());
    }
}