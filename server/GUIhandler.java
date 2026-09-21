package SystemMonitoring.server;

import SystemMonitoring.CommonModell.DiskInfo;
import SystemMonitoring.CommonModell.NetworkInfo;
import SystemMonitoring.CommonModell.ProcessInfo;
import com.google.gson.Gson;
import SystemMonitoring.CommonModell.DataSnapshot;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.*;
import java.io.InputStreamReader;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;

/**
 * GUI kapcsolatokat kezelo szal osztaly a szerver oldalon
 * <p>
 * A GUIhandler a GUI alkalmazas (UIview.java) es a szerver kozotti
 * kommunikacioert felel. A GUI kuldhet kerdeseket a kovetkezo tipusokkal:
 * A fo motivacio az volt ezzel, hogy tehermentesitsuk a tablakat azzal, hogy kulon
 * a UIview kerje le az adatokat onnan -> tranzakcios gondokat okozhat, nem volt jo tapasztalatom vele,
 * igy keri majd a szervert, hogy mit akar lattatni majd, a szerver meg odaadja, igy tovabbra is egy valaki
 * fogja kezelni majd az adatbazist
 *
 * <ul>
 *   <li>"getAgents" - osszes agent listajanak lekerese</li>
 *   <li>"getLastSnapshot" - adott agent utolso snapshot-janak lekerese</li>
 *   <li>"getDisks" - adott agent lemezadatainak lekerese</li>
 *   <li>"getNetworks" - adott agent halozati adatainak lekerese</li>
 *   <li>"getProcesses" - adott agent folyamatinformacioinak lekerese</li>
 * </ul>
 * Ezek parancsai a megfelelo fuggvenyekben majd lathatoak lesznek a Store.java-ban
 * </p>
 *
 * @author Arki Abel, FE8YA2
 * @version 1.0
 * @since 2026-05-06
 * @see GUIserverConnection
 * @see Store
 * @see UIview
 */
public class GUIhandler implements Runnable { //parhuzamositas tervezese
    //szokasos struktura
    private final Socket socket;
    private final Store store;
    private final Gson gson = new Gson();

    public GUIhandler(Socket socket, Store store) {
        this.socket = socket;
        this.store = store;
    }

    @Override
    public void run() { //GUIhandler belepesi poontja
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            String line;

            while ((line = reader.readLine()) != null) { //olvasunk amig jon a socket-bol keres a szerverbe

                JsonObject req = gson.fromJson(line, JsonObject.class); //JsonObject-te alakitas a request-hez
                String type = req.get("type").getAsString(); //type-kent hivatkozas majd a metodusokra, amiket kerunk es atadunk, lasd: GUIserverConnection

                if (type.equals("getAgents")) { //switch-el lehet szebb lenne
                    List<String> agents = store.getAgents(); //visszaad majd egy listat
                    writer.println(gson.toJson(agents)); //szerializalas, kuldes socketen a writer-rel
                }
                else if (type.equals("getLastSnapshot")) {
                    String agentID = req.get("agentID").getAsString(); //egy mezo kerese kulon (az nem snapshot resze)
                    DataSnapshot snapshot = store.getLastSnapshot(agentID); //majd a snapshot kerese
                    writer.println(gson.toJson(snapshot));
                }
                else if (type.equals("getDisks")) {
                    String agentID = req.get("agentID").getAsString(); //ua szinte
                    List<DiskInfo> disks = store.getDisks(agentID); //diskek esete
                    writer.println(gson.toJson(disks));
                }
                else if (type.equals("getNetworks")) {
                    String agentID = req.get("agentID").getAsString();
                    List<NetworkInfo> networks = store.getNetworks(agentID); //halozatok esete
                    writer.println(gson.toJson(networks));
                }
                else if (type.equals("getProcesses")) {
                    String agentID = req.get("agentID").getAsString();
                    List<ProcessInfo> processes = store.getProcesses(agentID); //processzek esete
                    writer.println(gson.toJson(processes));
                }
            }
        }
        catch (IOException | SQLException e) { //writerek es lekerdezesek altal okozott kivetelek elkapasa
            System.out.println("- GUI connection error: - " + e.getMessage());
        }
    }
}