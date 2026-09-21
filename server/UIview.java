package SystemMonitoring.server;

import SystemMonitoring.CommonModell.DataSnapshot;
import SystemMonitoring.CommonModell.DiskInfo;
import SystemMonitoring.CommonModell.NetworkInfo;
import SystemMonitoring.CommonModell.ProcessInfo;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

//process, disk ... tablazatokhoz
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.File;
import java.util.List;

import java.util.HashMap; //mapok miatt
import java.util.Map;

/**
 * A monitoring rendszer grafikus felulete (GUI) -> ez a javaFX belepesi pontja
 * <p>
 * Az UIview egy JavaFX alkalmazas, amely a szerver GUI portjahoz
 * (55556) kapcsolodva jeleniti meg a monitorozott agent-ek adatait
 * A felulet a kovetkezo komponenseket tartalmazza:
 * </p>
 * <ul>
 *   <li>Bal oldalon: az osszes agent listaja</li>
 *   <li>Kozepen: CPU es RAM grafikonok, valamint reszletes rendszerinformaciok</li>
 *   <li>Jobb oldalon: tablazatok a diszkekrol, halozati interfeszekrol es folyamatokrol</li>
 * </ul>
 *
 * @author Arki Abel, FE8YA2
 * @version 1.0
 * @since 2026-05-06
 * @see GUIserverConnection
 * @see Server
 */
public class UIview extends Application {

    //hasznalt objektumok deklaralasa
    private GUIserverConnection conn; //a GUI is referal majd ra termeszetesen

    //UI
    private ListView<String> agents; //itt is nyilvan lesznek tartva, mivel ki kell majd oket irni
    private Label OSlabel; //labelek elokeszitese
    private Label CPUlabel;
    private Label RAMlabel;

    private int time = 0; //az ido alapu diagrammok inicialis sajat-ideje

    //ezek pedig hashmapok a chart-ok parhuzamos rajzolasahoz
    private final Map<String, XYChart.Series<Number, Number>> CPUseriesMap = new HashMap<>(); //lenyege: az adott agentID-hez tarol Chart-okat, es azoknak az adatai is el lesznek tarolva
    private final Map<String, XYChart.Series<Number, Number>> RAMseriesMap = new HashMap<>(); //a String az ID, a Series mogotte meg az ido-meres egy pillanatban
    private final Map<String, Integer> timeMap = new HashMap<>();

    //tablazatok majd a GUI-hoz
    private TableView<DiskInfo> diskTable;
    private TableView<NetworkInfo> networkTable;
    private TableView<ProcessInfo> processTable;

    //tovabbi label-ek majd
    private Label archValue;
    private Label coresValue;
    private Label swapValue;
    private Label uptimeValue;
    private Label gatewayValue;
    private Label dnsValue;
    private Label domainValue;

    /**
     * A JavaFX alkalmazas belesesi pontja.
     * Letrehozza a teljes UI elrendezest, beallitja az esemenykezeloket,
     * majd elinditja a frissito szalat.
     *
     * @param stage az alkalmazas foablaka
     * @throws Exception ha a GUI inicializalasa sikertelen
     */
    @Override
    public void start(Stage stage) throws Exception { //a GUI belepesi pontja -> kulon szal
        //kapcsolat inicializalas
        conn = new GUIserverConnection();
        conn.connect("localhost", 55556);

        // ---------- sima megjelenes ----------
        BorderPane rootPane = new BorderPane();

        //fejlec
        Label header = new Label("System Monitor Dashboard");
        header.getStyleClass().add("label-header");  //style css-bol hivatkozas
        header.setStyle("-fx-font-size: 18px; -fx-padding: 10px;"); //manualis beallitas itt
        rootPane.setTop(header); //bal felso sarokban jelenik majd meg

        //itt a dolgok egy resze marad ahogy volt, csupan kibovitjuk mar erdemi informacioval is
        agents = new ListView<>(); //agenteknek nyitunk egy listat ami kattinthato, bal oldalon lesz
        agents.setPrefWidth(180);
        agents.setMinWidth(180);
        agents.setMaxWidth(180);
        agents.setStyle("-fx-padding: 0;");
        rootPane.setLeft(agents);

        GridPane infoGrid = new GridPane(); //innentol inkabb kinezeti dolgok lesznek -> gridpane racsosan teszi lehetove az elrendezeset objektumoknak az ablakban
        infoGrid.setHgap(20);
        infoGrid.setVgap(10);
        infoGrid.setPadding(new Insets(10)); //padding korulotte

        //CPU blokk - felirattal az ertek megjelenitesevel es egy progress bar-ral
        VBox cpuBox = new VBox(5);
        Label cpuTitle = new Label("CPU Usage");
        cpuTitle.getStyleClass().add("info-label");
        CPUlabel = new Label("CPU - ");
        CPUlabel.getStyleClass().add("info-value");
        ProgressBar CPUbar = new ProgressBar(0); //progressbar
        CPUbar.setPrefWidth(200);
        CPUbar.setMaxHeight(8);
        cpuBox.getChildren().addAll(cpuTitle, CPUlabel, CPUbar); //getChildren() hasznalat
        cpuBox.getStyleClass().add("info-box"); //erre definialt stylesheet

        //RAM blokk resz - ugyanaz lesz kb
        VBox ramBox = new VBox(5);
        Label ramTitle = new Label("RAM Usage");
        ramTitle.getStyleClass().add("info-label");
        RAMlabel = new Label("RAM - ");
        RAMlabel.getStyleClass().add("info-value");
        ProgressBar RAMbar = new ProgressBar(0);
        RAMbar.setPrefWidth(200);
        RAMbar.setMaxHeight(8);
        ramBox.getChildren().addAll(ramTitle, RAMlabel, RAMbar);
        ramBox.getStyleClass().add("info-box");

        // OS blokk
        VBox osBox = new VBox(5);
        Label osTitle = new Label("Operating System");
        osTitle.getStyleClass().add("info-label");
        OSlabel = new Label("OS - ");
        OSlabel.getStyleClass().add("info-value");
        osBox.getChildren().addAll(osTitle, OSlabel);
        osBox.getStyleClass().add("info-box");

        infoGrid.add(cpuBox, 0, 0); //addoljuk a krealt koordinatak alabjan
        infoGrid.add(ramBox, 1, 0);
        infoGrid.add(osBox, 0, 1, 2, 1); //kiterjedesuket is befolyasolhatjuk

        // ----- CPU chart -----
        NumberAxis xAxisCPU = new NumberAxis(); //ezek ala mennek a chrt-ok, NumberAxis objektumok definialasa
        NumberAxis yAxisCPU = new NumberAxis(0, 100, 10); //chart meret definialas
        xAxisCPU.setLabel("Time"); //axis-ok cimkei
        yAxisCPU.setLabel("CPU (%)");

        LineChart<Number, Number> CPUchart = new LineChart<>(xAxisCPU, yAxisCPU); //az axis-ok pedig megadjak magat a LineChart-ot, ez fog vonalat huzni nekunk
        CPUchart.setPrefHeight(160);
        CPUchart.setTitle("CPU Usage Over Time"); //cim
        CPUchart.setCreateSymbols(false); //ne legyenek pontok a meresen, csak a vonal

        XYChart.Series<Number, Number> CPUseries = new XYChart.Series<>(); // -||-
        CPUseries.setName("CPU (%)");
        CPUchart.getData().add(CPUseries); //majd betolti a chart-ba az adatokat

        // ----- RAM chart -----
        NumberAxis xAxisRAM = new NumberAxis(); // ua lesz
        NumberAxis yAxisRAM = new NumberAxis(0, 16000, 1000); //a skala nyilvan a vart adatokhoz igazitott
        xAxisRAM.setLabel("Time");
        yAxisRAM.setLabel("RAM (MB)");

        LineChart<Number, Number> RAMchart = new LineChart<>(xAxisRAM, yAxisRAM);
        RAMchart.setPrefHeight(160);
        RAMchart.setTitle("RAM Usage Over Time");
        RAMchart.setCreateSymbols(false);

        XYChart.Series<Number, Number> RAMseries = new XYChart.Series<>();
        RAMseries.setName("RAM (MB)");
        RAMchart.getData().add(RAMseries);

        //grid lineset: off
        xAxisCPU.setMinorTickVisible(false); //globalis bellitasok a Chart-ok stilusara, hogy esztetikus es konnyen olvashato legyen
        yAxisCPU.setMinorTickVisible(false);
        xAxisRAM.setMinorTickVisible(false);
        yAxisRAM.setMinorTickVisible(false);
        yAxisRAM.setAutoRanging(true); //hogy ne ugraljon a mereseknel

        //chart-ok kontenere
        VBox chartBox = new VBox(15);
        chartBox.setPadding(new Insets(10));
        chartBox.getChildren().addAll(CPUchart, RAMchart); //elhelyezzuk oket is

        //kozepso elrendezes
        VBox center = new VBox(20);
        center.setPadding(new Insets(20));

        //tovabbi rendszerinfo - ezek mar a chart-ok alatt lesznek, alulra kerultek, mert mar addicionalis onfot irnak le, nem a legfontosabb dolgokat
        GridPane systemInfoGrid = new GridPane(); //szokasos gridpane
        systemInfoGrid.setHgap(20);
        systemInfoGrid.setVgap(10);
        systemInfoGrid.setPadding(new Insets(10));
        systemInfoGrid.getStyleClass().add("info-box");

        //tovabbi CPU reszletek - innentol ugyanaz mint fentebb
        Label archTitle = new Label("Architecture");
        archTitle.getStyleClass().add("info-label");
        archValue = new Label("-");
        archValue.getStyleClass().add("info-value");
        archValue.setId("archValue");

        Label coresTitle = new Label("CPU Cores");
        coresTitle.getStyleClass().add("info-label");
        coresValue = new Label("-");
        coresValue.getStyleClass().add("info-value");
        coresValue.setId("coresValue");

        //tovabbi RAM reszletek
        Label swapTitle = new Label("Swap Usage");
        swapTitle.getStyleClass().add("info-label");
        swapValue = new Label("-");
        swapValue.getStyleClass().add("info-value");
        swapValue.setId("swapValue");

        //kulon az uptime
        Label uptimeTitle = new Label("System Uptime");
        uptimeTitle.getStyleClass().add("info-label");
        uptimeValue = new Label("-");
        uptimeValue.getStyleClass().add("info-value");
        uptimeValue.setId("uptimeValue");

        systemInfoGrid.add(archTitle, 0, 0); //elhelyezesuk koordinataja a GridPane-ben -> title es az info maga
        systemInfoGrid.add(archValue, 1, 0);
        systemInfoGrid.add(coresTitle, 2, 0);
        systemInfoGrid.add(coresValue, 3, 0);
        systemInfoGrid.add(swapTitle, 0, 1);
        systemInfoGrid.add(swapValue, 1, 1);
        systemInfoGrid.add(uptimeTitle, 2, 1);
        systemInfoGrid.add(uptimeValue, 3, 1);

        //tovabbi halozati reszletek
        GridPane networkDetailGrid = new GridPane(); //ugyanott alul tovabbi reszletek
        networkDetailGrid.setHgap(20);
        networkDetailGrid.setVgap(10);
        networkDetailGrid.setPadding(new Insets(10)); //szoki padding
        networkDetailGrid.getStyleClass().add("info-box");

        Label gatewayTitle = new Label("Default Gateway");
        gatewayTitle.getStyleClass().add("info-label");
        gatewayValue = new Label("-");
        gatewayValue.getStyleClass().add("info-value");
        gatewayValue.setId("gatewayValue");

        Label dnsTitle = new Label("DNS Server");
        dnsTitle.getStyleClass().add("info-label");
        dnsValue = new Label("-");
        dnsValue.getStyleClass().add("info-value");
        dnsValue.setId("dnsValue");

        Label domainTitle = new Label("Domain Name");
        domainTitle.getStyleClass().add("info-label");
        domainValue = new Label("-");
        domainValue.getStyleClass().add("info-value");
        domainValue.setId("domainValue");

        networkDetailGrid.add(gatewayTitle, 0, 0); //elhelyezeseik
        networkDetailGrid.add(gatewayValue, 1, 0);
        networkDetailGrid.add(dnsTitle, 2, 0);
        networkDetailGrid.add(dnsValue, 3, 0);
        networkDetailGrid.add(domainTitle, 0, 1);
        networkDetailGrid.add(domainValue, 1, 1);

        center.getChildren().addAll(infoGrid, chartBox, systemInfoGrid, networkDetailGrid); //majd az egesz center-hez hozzaadjuk a konstrualt szekciokat -> felso resz, kozepso chart-ok, also resz
        rootPane.setCenter(center);

        // ---------- JOBB OLDAL TABLAZATAI ----------

        //jobb oldali panel
        VBox rightPanel = new VBox(15); //nagy box-ban majd minden
        rightPanel.setPrefWidth(550);
        rightPanel.setPadding(new Insets(20));

        // ---------- DISK TAB ----------
        Label diskTitle = new Label("Disk Usage"); //section nev
        diskTitle.getStyleClass().add("list-section-title");
        diskTable = new TableView<>(); //tablazat JavaFX objektum hasznalata
        diskTable.getStyleClass().add("custom-table"); //css stiluspanel hivatkozas

        TableColumn<DiskInfo, String> mountCol = new TableColumn<>("Mount Point"); //oszlopok definialasara szolgalo metodusok hivasa, innentol repetitiv lesz
        mountCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getMountPoint()));
        mountCol.setPrefWidth(120);

        TableColumn<DiskInfo, String> usageCol = new TableColumn<>("Usage"); //usage fejlecu oszlop letrehozasa -> diskinfora fogunk majd hivatkozni
        usageCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getUsageFormatted())); //majd megmondja, hogy a DiskInfo adatszerkezetbol melyik mezo menjen oda az adott cell-ba
        usageCol.setPrefWidth(220); //egyeni szelesseg, probaltam dinamikusan alakitani

        TableColumn<DiskInfo, String> speedCol = new TableColumn<>("Speed"); //innentol ugyanaz
        speedCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getSpeedFormatted()));
        speedCol.setPrefWidth(140);

        diskTable.getColumns().addAll(mountCol, usageCol, speedCol); //az oszlopok, azaz a tabla semajanak megalkotasa
        diskTable.setPrefHeight(150); //nem tul nagy de meg olvashato meret

        // ---------- NETWORKS TAB ----------
        Label networkTitle = new Label("Network Interfaces"); //ua a struktura innentol
        networkTitle.getStyleClass().add("list-section-title");
        networkTable = new TableView<>();
        networkTable.getStyleClass().add("custom-table");

        TableColumn<NetworkInfo, String> interfaceCol = new TableColumn<>("Interface");
        interfaceCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getInterfaceName()));
        interfaceCol.setPrefWidth(100);

        TableColumn<NetworkInfo, String> ipCol = new TableColumn<>("IP Address");
        ipCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getIpAddress()));
        ipCol.setPrefWidth(140);

        TableColumn<NetworkInfo, String> macCol = new TableColumn<>("MAC Address");
        macCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getMacAddress()));
        macCol.setPrefWidth(140);

        TableColumn<NetworkInfo, String> domainCol = new TableColumn<>("Domain");
        domainCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDomainName()));
        domainCol.setPrefWidth(100);

        TableColumn<NetworkInfo, String> gatewayCol = new TableColumn<>("Gateway");
        gatewayCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDefaultGateway()));
        gatewayCol.setPrefWidth(120);

        TableColumn<NetworkInfo, String> dnsCol = new TableColumn<>("DNS Server");
        dnsCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDnsServer()));
        dnsCol.setPrefWidth(120);

        TableColumn<NetworkInfo, String> hostCol = new TableColumn<>("Host Name");
        dnsCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getHostName()));
        dnsCol.setPrefWidth(120);

        networkTable.getColumns().addAll(interfaceCol, ipCol, macCol, domainCol, gatewayCol, dnsCol, hostCol); //addoljuk vegul az oszlopokat
        networkTable.setPrefHeight(160);

        // ---------- PROCESS TAB ----------
        Label processTitle = new Label("Running Processes"); // -||-
        processTitle.getStyleClass().add("list-section-title");
        processTable = new TableView<>();
        processTable.getStyleClass().add("custom-table");

        TableColumn<ProcessInfo, String> nameCol = new TableColumn<>("Process Name");
        nameCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getProcessName()));
        nameCol.setPrefWidth(200);

        TableColumn<ProcessInfo, String> cpuCol = new TableColumn<>("CPU (%)");
        cpuCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCpuPercentFormatted()));
        cpuCol.setPrefWidth(80);
        cpuCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<ProcessInfo, String> ramCol = new TableColumn<>("RAM (MB)");
        ramCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getMemoryMBFormatted()));
        ramCol.setPrefWidth(100);
        ramCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        processTable.getColumns().addAll(nameCol, cpuCol, ramCol);
        processTable.setPrefHeight(280);

        rightPanel.getChildren().addAll(diskTitle, diskTable, networkTitle, networkTable, processTitle, processTable); //es a jobb oldali panelhez adjuk az iment alkotott objektumokat
        rootPane.setRight(rightPanel); //jobbra rendezzuk


        // ---------- sima megjelenites vege ----------

        //agent kivalasztasa a bal oldalrol
        agents.getSelectionModel().selectedItemProperty().addListener((obs, oldAg, newAg) -> { //listener a kattintás eventre
            if (newAg != null) { //az ujonnan kattintott agent
                CPUchart.getData().setAll(CPUseriesMap.get(newAg)); //a map-ot get-eljuk mert az menti el a hatterben is az adatot, igy nem mindig kattintaskor lesz megkezdve a meres
                RAMchart.getData().setAll(RAMseriesMap.get(newAg));
                CPUseries.getData().clear(); //resetelve lesznek a regi vizualizaciok
                RAMseries.getData().clear();
                showAgentDetails(newAg, CPUbar, RAMbar); //a showdetails ez alatt lesz majd bovebben, ez tolti majd be a reszleteket
            }
        });

        //fo thread kezelese a dinamikus adatmegjeleniteshez
        Thread refresh = new Thread(() -> {
            while (true) {
                try {
                    List<String> agentList = conn.getAgents(); // GUIserverConnection-on keresztul lekerjuk a szervertol az agent-eket

                    Map<String, DataSnapshot> snapshots = new HashMap<>(); //hashmap-ba kerulo aktualis snapshot lesz majd az Agent ID alapjan
                    for (String agentID : agentList) { //feltoltjuk az osszas agent-et a hozzajuk tartozo utolso eredmenyekkel
                        DataSnapshot data = conn.getLastSnapshot(agentID);
                        if (data != null) snapshots.put(agentID, data); //es betesszuk oket a map-ba -> igy lesz a parhuzamos mentese az osszesnek
                    }

                    Platform.runLater(() -> { //runLater-ben lesz a dinamikusan valtozo dolgok szala
                        String selected = agents.getSelectionModel().getSelectedItem(); //kivalasztas modositasa grafikusan is az Agents ListView-ben
                        agents.getItems().setAll(agentList); //setting
                        if (selected != null) agents.getSelectionModel().select(selected); //ha a valasztott null lenne, visszaallitja a megfigyelest az elozore

                        int samples = 80; //a meres finomsaga a Chart-okon

                        // Minden agenthez frissitjuk a Series-t
                        for (String agentID : agentList) { //elovesszuk a map-okat es akorabban mentett adatokat most meg is jelenitjuk dinamikusan
                            CPUseriesMap.putIfAbsent(agentID, new XYChart.Series<>()); //dinamikusan addolunk uj mapot azoknak az agent-eknek, akik kozben csatlakoznak
                            RAMseriesMap.putIfAbsent(agentID, new XYChart.Series<>());
                            timeMap.putIfAbsent(agentID, 0); //ID time inicialis parossal

                            DataSnapshot data = snapshots.get(agentID); //szervertol kert snapshot feldolhozasa
                            if (data != null) {
                                int t = timeMap.get(agentID); //idopont szamlaloja dinamikusan

                                CPUseriesMap.get(agentID).getData().add(
                                        new XYChart.Data<>(t, data.CPUusagePercent) //a mert adat get-telese es hozzaadasa a Chart-hoz -> CPU
                                );
                                RAMseriesMap.get(agentID).getData().add(
                                        new XYChart.Data<>(t, data.usedMemoryBytes/1024/1024) //a mert adat get-telese es hozzaadasa a Chart-hoz -> RAM
                                );

                                // Sliding window
                                if (CPUseriesMap.get(agentID).getData().size() > samples) { // le lezs torolva a legregebbi mert pont ha tullepjuk a sample-t -> CPU-n
                                    CPUseriesMap.get(agentID).getData().remove(0);
                                }
                                if (RAMseriesMap.get(agentID).getData().size() > samples) { // le lezs torolva a legregebbi mert pont ha tullepjuk a sample-t -> RAM-on
                                    RAMseriesMap.get(agentID).getData().remove(0);
                                }

                                timeMap.put(agentID, t + 1); //a vonal huzasa a kovetkezo pontba

                                // X tengely csusztatasa csak a kivalasztott agentnel
                                if (agentID.equals(selected)) { //ez dinamikusan valtoztatja fix mereturol csuszo ablakra a chart-ban megjelenitett ertekeket
                                    if (t < samples) {
                                        xAxisCPU.setAutoRanging(false);
                                        xAxisCPU.setLowerBound(0);
                                        xAxisCPU.setUpperBound(t);
                                        xAxisRAM.setAutoRanging(false);
                                        xAxisRAM.setLowerBound(0);
                                        xAxisRAM.setUpperBound(t);
                                    } else {
                                        xAxisCPU.setAutoRanging(false);
                                        xAxisCPU.setLowerBound(t - samples);
                                        xAxisCPU.setUpperBound(t);
                                        xAxisRAM.setAutoRanging(false);
                                        xAxisRAM.setLowerBound(t - samples);
                                        xAxisRAM.setUpperBound(t);
                                    }

                                    //reszletek folyamatos frissitese
                                    showAgentDetails(selected, CPUbar, RAMbar);
                                }
                            }
                        }
                    });
                    Thread.sleep(1000); //1 masodperces idokozokkel
                } catch (Exception e) {
                    System.out.println("Refresh error in 'start': " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
        refresh.setDaemon(true); //runlater-es trukk inspiralodva a laboros peldakbol :)
        refresh.start(); //refresh thread inditasa

        File cssFile = new File("darktheme.css"); //css definialasa
        Scene dashScene = new Scene(rootPane, 1200, 700); //maga az ablak megadasa
        if (cssFile.exists()) dashScene.getStylesheets().add(cssFile.toURI().toString()); //URL formatumba alakitott css fajl
        else System.out.println(" - File not found: " + cssFile.getAbsolutePath() + " - ");

        stage.setTitle("System Monitor"); //ablak cima
        stage.setScene(dashScene); //scene bekeszitese
        stage.show(); //es mutatasa
    }

    /**
     * Egy adott agent reszletes adatainak megjelenitese.
     * Frissiti az informacios cimkeket, progress bar-okat,
     * valamint a disk, network es process tablazatokat.
     *
     * @param agentID a megjelenitendo agent azonositoja
     * @param CPUbar a CPU hasznalatot mutato progress bar
     * @param RAMbar a RAM hasznalatot mutato progress bar
     */
    private void showAgentDetails(String agentID, ProgressBar CPUbar, ProgressBar RAMbar) { //ID alapjan, tovabba frissiti a Bar-okat
        try {
            DataSnapshot snapshot = conn.getLastSnapshot(agentID); //megint getteljuk az utso merest
            if (snapshot != null) { //ha van ertek

                //segedszamolas
                long usedMB = snapshot.usedMemoryBytes/1024/1024;
                long totalMB = snapshot.totalMemoryBytes/1024/1024;

                //beallitjuk progresszive az osszes label-t es a Bar-okat formazott kiirasokkal
                OSlabel.setText("OS " + snapshot.OSname + " " + snapshot.OSversion);
                CPUlabel.setText("CPU " + String.format("%.1f", snapshot.CPUusagePercent) + "%");
                RAMlabel.setText("RAM " + usedMB + " MB / " + totalMB + " MB");

                CPUbar.setProgress(snapshot.CPUusagePercent/100.0);
                RAMbar.setProgress((double) snapshot.usedMemoryBytes/snapshot.totalMemoryBytes);

                archValue.setText(snapshot.architecture != null ? snapshot.architecture : "---"); //dinamikusan dol el, hogy ures mezot, vagy valid adatot tudunk-e beirni
                coresValue.setText(snapshot.physicalCores + " physical / " + snapshot.logicalCores + " logical");

                long swapUsedMB = snapshot.swapUsedBytes / 1024 / 1024;
                long swapTotalMB = snapshot.swapTotalBytes / 1024 / 1024;
                if (swapTotalMB > 0) { //hogy ne legyen nullaval valo osztas
                    double swapPercent = (double) snapshot.swapUsedBytes / snapshot.swapTotalBytes * 100;
                    swapValue.setText(String.format("%d / %d MB (%.1f%%)", swapUsedMB, swapTotalMB, swapPercent));
                } else {
                    swapValue.setText("No swap");
                }

                //tovabbi muveletek az uptime szamitashoz
                long seconds = snapshot.uptimeSeconds;
                long days = seconds / 86400;
                long hours = (seconds % 86400) / 3600;
                long minutes = (seconds % 3600) / 60;
                uptimeValue.setText(String.format("%d days, %d hours, %d minutes", days, hours, minutes)); //a formazott kiirassal

                //disk refresh
                List<DiskInfo> disks = conn.getDisks(agentID); //folyamatosan kerjuk le a szervertol a disk-eket
                if (disks != null) {
                    diskTable.getItems().clear(); //az elozo ertekeket pedig toroljuk
                    diskTable.getItems().addAll(disks); //majd ha van, beadjuk a tablanak
                }

                //network refresh
                List<NetworkInfo> networks = conn.getNetworks(agentID); //ua mint a disk-nel
                if (networks != null && !networks.isEmpty()) {
                    networkTable.getItems().clear();
                    networkTable.getItems().addAll(networks);

                    NetworkInfo firstNet = networks.get(0);
                    gatewayValue.setText(firstNet.defaultGateway != null ? firstNet.defaultGateway : "---");
                    dnsValue.setText(firstNet.dnsServer != null ? firstNet.dnsServer : "---");
                    domainValue.setText(firstNet.domainName != null ? firstNet.domainName : "---");
                } else {
                    gatewayValue.setText("---");
                    dnsValue.setText("---");
                    domainValue.setText("---");
                }

                //process refresh
                List<ProcessInfo> processes = conn.getProcesses(agentID); // -||-
                if (processes != null) {
                    processTable.getItems().clear();
                    processTable.getItems().addAll(processes);
                }
            }
        } catch (Exception e) {
            System.out.println("IO failure in function 'showAgentDetails': " + e.getMessage());
        }
    }

    /**
     * Alkalmazas inditasa.
     *
     * @param args parancssori argumentumok (nincs hasznalatban)
     */
    public static void main(String[] args) {
        launch(args);
    } //a UIview main()-ja pedig innan inditja -> launch() segitsegevel
}