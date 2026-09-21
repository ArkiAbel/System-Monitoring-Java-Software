package SystemMonitoring.agent;

import SystemMonitoring.CommonModell.AgentConfig;
import SystemMonitoring.CommonModell.ConfigLoader;
import SystemMonitoring.CommonModell.DataSnapshot;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Agent (kliens) program a rendszer monitoringhoz
 * <p>
 * Az agent minden monitorozott hoston fut, gyujti a rendszerinformaciokat (OS, CPU, RAM, disk, network, processek)
 * es elkuldi oket a kozponti szervernek. Tamogatja a tesztuzemmodot is (-t flag), ahol valos adatok helyett
 * szimulalt adatokat general a teszteles konnyitesehez
 * </p>
 *
 * <p>Hasznalat:</p>
 * <pre>
 * java SystemMonitoring.agent.Agent        -> normal uzemeles
 * java SystemMonitoring.agent.Agent -t           -> teszt uzemmod
 * java SystemMonitoring.agent.Agent -c config.json     -> egyedi config fajl agent-hez
 * </pre>
 *
 * <p>A program lock file-t hasznal (agent.lock), hogy egy gepen egyszerre csak egy valos agent fusson
 * Teszt modban ez a korlatozas nincs ervenyben, mivel abbol celszeruan barmannyi lehet (az a celja)</p>
 *
 * @author Arki Abel, FE8YA2
 * @version 1.0
 * @since 2026-05-06
 * @see Collector
 * @see TestDataGen
 * @see Sender
 */
public class Agent {

    /**
     * Az agent fopontja. Betolti a konfiguraciot, inicializalja az adatgyujtot (valos vagy teszt),
     * majd folyamatosan (pollSeconds idokozonkent) kuldi az adatokat a szervernek.
     *
     * @param args parancssori argumentumok:
     *             <ul>
     *             <li><b>-t</b> : teszt uzemmod bekapcsolasa (szimulalt adatok)</li>
     *             <li><b>-c &lt;path&gt;</b> : egyedi config fajl eleresi utvonala</li>
     *             </ul>
     * @throws IOException ha a config fajl nem olvashato vagy a hálózati kapcsolat sikertelen
     */
    public static void main(String[] args) throws IOException {

        String confPath = "agent_config.json";
        String agentID = "agent-" + System.currentTimeMillis(); //egyszeru megkulonbozteto az egyediseghez, a tesztmod konstruktora vesz majd at

        boolean testMode = false; //ez allithato teszteleshez

        //a main-ben kapott inditasi parameterek argumentumait vizsgaljuk
        for (int i = 0; i < args.length; i++) { //loop-pal
            if (args[i].equals("-t")) testMode = true;
            if (args[i].equals("-c")) confPath = args[i+1]; //azaz a -c flag utani path beolvasasa
        }

        //a legutolso patch kod update, hogy ne lehessen 2 real agentet egy geprol inditani, kulonben crashel a program
        File lockFile = new File("agent.lock"); //maga a lock fajl

        if (!testMode) {
            if (lockFile.exists()) { //figyelmezteto log
                System.out.println(" - Error: Another real agent is already running on this device! -");
                System.out.println(" - Use -t flag for test mode to run multiple instances -");
                System.exit(1); //hibakoddal valo terminalas
            }
            lockFile.createNewFile(); //itt letrehozzuk
            lockFile.deleteOnExit(); //JVM utan torli
        }

        AgentConfig conf = ConfigLoader.loadAgentConfig(confPath); //confPath adatait toltjuk me ezuttal

        Collector collector; //referencia, hogy eldontsuk valos merest vagy szimulalt adatokat akarunk szolgaltatni ide
        if (testMode) {
            System.out.println(" - Testmode flag detected - starting simulated data generation -");
            collector = new TestDataGen(agentID); //tesztmod feltoltes
        }
        else {
            System.out.println(" - Collecting real data from devices -");
            collector = new Collector(); //valodi meres
        }

        Sender sender = new Sender(); //uj sender opbejktum logike kulonveve
        sender.connect(conf.serverHost, conf.serverPort); // "localhost", "55555" alapbol, a sender intezi

        while(true) {
            DataSnapshot snapshot = collector.collect(); //folyamatosan letrehozott snapshot-ok
            snapshot.agentID = agentID; //agentID kulon beallitasa
            sender.send(snapshot); //ezt elkuldjuk, nem kell belole peldany
            System.out.println("- Data sent -"); //kuldes es sor lezaras

            try { //varakozas (polling) hogy ne busy waiting legyen -> ez a szal varakozni fog addig
                Thread.sleep(conf.pollSeconds * 1000L); //mivel long-kent taroljuk, conf-bol szedjuk ki az erteket
            }
            catch (InterruptedException e) { //kivetel elkapasa, kezelese, ahogy a moodle anyagokban is kezeltuk
                Thread.currentThread().interrupt(); // helyes interrupt restore, ez le fog allni
                System.out.println("Sleep interrupted: " + e.getMessage());
            }
        }
    }
}