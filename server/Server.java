package SystemMonitoring.server;

import SystemMonitoring.CommonModell.ConfigLoader;
import SystemMonitoring.CommonModell.ServerConfig;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;

/**
 * Kozponti szerver alkalmazas a monitoring rendszerhez
 * <p>
 * A szerver ket kulon porton figyel:
 * <ul>
 *   <li>port (alapbol 55555) - az agentek csatlakozasara (ClientHandler)</li>
 *   <li>55556 - a GUI kapcsolodasara (GUIhandler)</li>
 * </ul>
 * </p>
 *
 * <p>A szerver minden bejovo kapcsolathoz egy uj szalat indit,
 * igy tobb agent es a GUI egyidejuleg kezelheto. Az adatok perzisztens tarolasa SQLite adatbazisban tortenik (lasd: Store osztaly)</p>
 *
 * <p>Hasznalat:</p>
 * <pre>
 * java SystemMonitoring.server.Server                      -> ez lesz az alap config
 * java SystemMonitoring.server.Server -c server_config.json    -> ez pedig az egyedi config
 * </pre>
 *
 * @author Arki Abel, FE8YA2
 * @version 1.0
 * @since 2026-05-06
 * @see ClientHandler
 * @see GUIhandler
 * @see Store
 */
public class Server {

    /**
     * A szerver fo pontja -> main(). Betolti a konfiguraciot, letrehozza az adatbazis kapcsolatot,
     * majd elindit ket szalat: egyet az agentek fogadasara, egyet a GUI kapcsolodasara
     *
     * @param args parancssori argumentumok:
     *             <ul>
     *             <li><b>ha minden igaz ezek: -c &lt;path&gt; </b> : egyedi config fajl eleresi utvonala</li>
     *             </ul>
     * @throws IOException ha a halozati socket nem nyithato meg
     * @throws SQLException ha az adatbazis kapcsolat nem hozhato letre
     */
    public static void main(String[] args) throws IOException, SQLException { //dobhato kivetelek

        String confPath = "server_config.json"; //a megadott conf utvonal a projekten belul, ami default

        for (int i = 0; i < args.length; i++) { //szerverconfig argumentum vizsgalata, beallitasa ha van sajat
            if (args[i].equals("-c")) confPath = args[i+1];
        }

        ServerConfig conf = ConfigLoader.loadServerConfig(confPath); //itt is hasznaljuk a configLoadert, akarcsak az Agent-nel
        Store clientStore = new Store(conf.dbPath); //itt hozzuk letre az egyetlen Store-t az adatbazis eleresi utvonalaval, amit a Json definial
        System.out.println("- Server has been started - Waiting... -"); //log -> fontos visszajelzes, hogy figyel a kapcsolodasokra

        // ---------- AGENT SOCKET (55555) ---------- fontos resz:
        Thread agentThread = new Thread(() -> {
            try {
                ServerSocket agentSocket = new ServerSocket(conf.port); //socket letrehozasa az agenthez
                System.out.println("- Listening to agents on port " + conf.port + " -"); //log
                while (true) {
                    Socket clientSocket = agentSocket.accept(); //kapcsolat inditasa, figyeles az agent-re
                    System.out.println("- Agent connected from: " + clientSocket.getInetAddress()); //teszt log, hogy mukodik-e a get
                    new Thread(new ClientHandler(clientSocket, clientStore)).start(); //odaadja neki a sajat socket-jet, es a globalis Store-t, mindezt kulon a clientHandler modulbol, es el is iditja a sajat szalat
                }
            }
            catch (IOException e) {
                System.out.println("Agent listener error: " + e.getMessage());
            }
        });

        // ---------- GUI SOCKET (55556) ----------
        Thread guiThread = new Thread(() -> {
            try {
                ServerSocket guiSocket = new ServerSocket(55556); //kulon GUI listener port
                System.out.println("- Listening to GUI on port 55556 -");
                while (true) {
                    Socket clientSocket = guiSocket.accept(); //fogadas inditasa GUI-ra nyilo port felol
                    System.out.println("- GUI connected - ");
                    new Thread(new GUIhandler(clientSocket, clientStore)).start(); //uj szal a GUIhandler felol, es atadasa a socket-nek es a Store-nak, ahogy eddig volt
                }
            }
            catch (IOException e) {
                System.out.println("GUI listener error: " + e.getMessage());
            }
        });

        //vegul inditjuk oket
        agentThread.start();
        guiThread.start();
    }
}