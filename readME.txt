readME fajl - Arki Abel - FE8YA2





==================================
System Monitoring Project - README
==================================





EGYSZERU HASZNALAT:

1. Szerver inditasa (elso terminal):
   java -cp target/classes SystemMonitoring.server.Server

2. Agent inditasa (masodik terminal):
   java -cp target/classes SystemMonitoring.agent.Agent

3. GUI inditasa (harmadik terminal):
   java -cp target/classes SystemMonitoring.server.UIview

(Az agent meresnel csak egy indul sajat geprol, szimulalt klienseknel barmennyi indithato)
(Elofordulhat hogy kicsit akadozik a meres neha, az agent kivalasztasanal, ezt nem tudtam kikuszobolni)





KONFIGURACIOS FAJLOK (JSON):

agent_config.json (agent melle):
{
  "serverHost": "localhost",
  "serverPort": 55555,
  "pollSeconds": 5,
  "agentID": "my-pc"
}

server_config.json (szerver melle):
{
  "port": 55555,
  "dbPath": "system_monitoring.db"
}





TESZT MOD (-t):

Valos adatok helyett szimulalt adatokat kuld:
   java -cp [...] SystemMonitoring.agent.Agent -t

- Tobb agent indithato ugyanarrol a geprol lock nelkul
- Hasznos, ha nincs tobb fizikai gep es a mukodest teszteljuk





LOCK FAJL (agent.lock):

- Normal uzemmodban (-t nelkul) csak EGY agent futhat egy gepen
- Masodikat inditva: "Another real agent is already running"
- Teszt modban (-t) nincs ilyen korlatozas, ez a felesleges terhelest es a redundans merest elozi meg





ADATBIZIS TORLESE:

Tiszta mereshez torold a .db fajlt a szerver inditasa ELOTT mindig:
   rm system_monitoring.db     (Linux/macOS)
   del system_monitoring.db    (Windows)

Ha nem torlod, a reg adatok is latszodni fognak a GUI-ban, de nem okoz nagy gondot.





INDITASI SORREND (AMI MUKODIK):

1. Szerver
2. Agent (vagy tobb teszt agent -t-vel)
3. GUI





GYAKORI HIBAK:

- "Address already in use" -> szerver mar fut, allitsd le
- "Another real agent..." -> mar fut egy agent, hasznalj -t modot
- GUI ures -> elobb indits szervert ES agentet, vagy valassz egyet a panelbol, ha mar futnak





CSS STILUS:

Ha van darktheme.css a projekt gyokerben, a GUI betolti.
Ha nincs, alap stilus lesz -> szinten mukodik, csak nem lesz szep :(

