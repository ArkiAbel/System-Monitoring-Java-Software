package SystemMonitoring.agent;

import com.google.gson.Gson;
import SystemMonitoring.CommonModell.DataSnapshot;

import java.io.*;
import java.net.Socket;

/**
 * Hálózati kommunikációert felelos osztaly az agent oldalan
 * <p>
 * A Sender felelossege, hogy (jelen esetben TCP) kapcsolatot hozzon letre a szerverrel,
 * es a begyujtott DataSnapshot objektumokat JSON formatumba alakitva
 * elkuldje a szervernek. A kommunikacio push modellben tortenik (ezt tartottam erthetobbnek a valasztasnal):
 * az agent kezdemenyezi a kapcsolatot es kuld adatokat, minden agent maga keresi a szervert, azaz NEM a szerver oket,
 * hogy nekik dinamikusan uj session-oket -> fontos
 * </p>
 *
 * <p>A JSON szerializaciohoz a Google GSON konyvtarat hasznaltam,
 * mivel ez konnyen kezelheto es konnyen olvashato formatumot eredmenyez</p>
 *
 * @author Arki Abel, FE8YA2
 * @version 1.0
 * @since 2026-05-06
 * @see DataSnapshot
 * @see .Gson osztaly: gson.Gson
 */
public class Sender {

    //Socket, Printwriter, es gson inicializalas
    private Socket socket;
    private PrintWriter pw;
    private final Gson gson = new Gson(); //Google gson TCP kapcsolathoz, final, mert az IDE javasolta, ebbol fix nem orokol semmi, nem fogja felulirni

    /**
     * TCP kapcsolat letrehozasa a szerverrel
     *
     * @param host a szerver hosztneve (pl. "localhost" vagy IP cim)
     * @param port a szerver portszama (alapbol 55555), orai peldat tovabbvive
     * @throws IOException dobas -> ha a kapcsolat nem hozhato letre
     */
    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true); //autoflush fontos!!
    };

    /**
     * Adatkuldemeny elkuldese a szervernek.
     * A DataSnapshot objektumot JSON formatumba alakitja, majd a TCP kapcsolaton
     * keresztul elkuldi. Az autoflush bekapcsolasa miatt nem szukseges kulon hivni a flush()-t.
     *
     * @param snapshot a kuldeni kivant adatpillanat (DataSnapshot objektum)
     */
    public void send(DataSnapshot snapshot) { //Snapshot objektumot kuldunk, egy pillanatnyi merest
        String json = gson.toJson(snapshot);
        pw.println(json); //a felepitett json vegul el lesz kuldve a Socket-en -> (socket.getOutputStream()) korabban
    };

    /**
     * A TCP kapcsolat bontasa -> ez nincs hasznalatban, a feladat tobbnyire csak a csatlakozast kerte
     * Tovabba a JVM automatikusan kezeli, a readLine() null-t kap, es be lesz zarva a Socket is ha minden igaz
     * Alapjaraton fontos lenne a szerver leallitasakor meghivni, hogy a socket eroforrasok felszabaduljanak, de
     * ez itt most nem eletbevago, meg nem is biztos, hogy jol tudnam kezelni :)
     *
     * @throws IOException ha a socket zarasakor hiba tortenik
     */
    public void disconnect() throws IOException { //marad gondolati szinten
        if (socket != null) socket.close();
    };
}