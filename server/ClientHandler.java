package SystemMonitoring.server;

import com.google.gson.Gson;
import SystemMonitoring.CommonModell.DataSnapshot;

import java.io.BufferedReader;
import java.io.*;
import java.io.InputStreamReader;
import java.net.Socket;
import java.sql.SQLException;

/**
 * Agent kapcsolatokat kezelo szal osztaly a szerver oldalan
 * <p>
 * Minden agent kapcsolathoz egy ClientHandler peldany tartozik, amely a sajat szalan fut.
 * A handler folyamatosan olvassa a bejovo JSON
 * uzeneteket, deszerializalja oket DataSnapshot objektumokka -> Datasnapshot.class;
 * majd eltarolja oket az adatbazisban a Store osztaly segitsegevel
 * </p>
 *
 * <p>A megfelelo "fair scheduling" (minden kliens egyenlo kiszolgalasa)
 * azert biztositott, mert minden kliens sajat szalat kap (JVM biztositja, mivel tobb while ciklus kepes futni egymas mellett),
 * es a Java szal-utemezője korulbelul egyenlo idoszeleteket oszt mindenkinek</p>
 *
 * @author Arki Abel, FE8YA2
 * @version 1.0
 * @since 2026-05-06
 * @see Server
 * @see Store
 * @see DataSnapshot
 */
public class ClientHandler implements Runnable { //a handler csinalja a run()-t

    private final Socket socket;
    private final Store store;
    private final Gson gson = new Gson(); //megegy Gson, mert itt is hasznalni fogjuk

    /**
     * Konstruktor - letrehoz egy uj agent handlert
     *
     * @param socket a TCP kapcsolat (agent - szerver kozott)
     * @param store az adatbazis kapcsolat (a snapshotok mentesehez)
     */
    public ClientHandler(Socket socket, Store store) { //konstuktoron Socket atadas
        this.socket = socket;
        this.store = store;
    }

    /**
     * A szal futo metodusa -> folyamatosan olvassa a bejovo JSON adatokat
     * a socketbol, deszerializalja oket, menti az adatbazisba, majd
     * a konzolra is kiir egy egyszeru progress bart a CPU hasznalat alapjan, ez meg
     * a JavaFX implementalas elotti teszteket segitette ki
     */
    @Override
    public void run() { //itt a parhuzamos szal belepesi/futasi pontja
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream())); //folyamatosan fogadja az adatot
            String line; //segedvaltozo
            while ((line = reader.readLine()) != null) { //amig jon adat a klienstol
                DataSnapshot snapshot = gson.fromJson(line, DataSnapshot.class); //visszaalakitas snapshot objektumma
                store.save(snapshot); //lementi az SQLite-ba -> store-ban majd tobb info

                // ----- konzol kimenet a hibakereseshez, vagy debug-hoz -----
                StringBuilder bar = new StringBuilder();
                for (int i = 0; i < (int) snapshot.CPUusagePercent; i++) {
                    bar.append("|");
                }
                System.out.println("Data received: " + snapshot.OSname + " CPU: " + (int) snapshot.CPUusagePercent + "% : " + bar);
                // ----- konzol kimenet vege -----
            }
        }
        catch (IOException | SQLException e) {
            System.out.println("- Error: - " + e.getMessage());
            System.out.println("- StackTrace -");
            e.printStackTrace();
        }
    }
}