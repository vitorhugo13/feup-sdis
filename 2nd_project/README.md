# SDIS - Second Assignment
* Distributed Backup Service for the Internet

## Info
* The description of the first assignment is available [here](https://paginas.fe.up.pt/~pfs/aulas/sd2020/projs/proj2/proj2.html).

## Contributors
* **[Bruno Micaelo]()**
* **[Mariana Neto]()**
* **[Vítor Gonçalves](https://github.com/vitorhugo13)**
* **[Vítor Ventuzelos](https://github.com/BerserkingIdiot)**


**COMPILING**: <br>javac *.java

**STARTING THE REGISTRY**: <br>rmiregistry

**PEER (chord)**:<br>
java Peer <peer_version> <server_id> <access_point> <chord_address> <chord_port><br>
**ex**: java Peer 1.0 1 access_peer_1 8080 (first peer of a network)<br>
        java Peer 1.0 2 access_peer_2 8081 127.0.1.1 8080 (additional peers)

<br><br>
**TESTING PROTOCOLS**:<br>
java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2><br>
**ex**: java TestApp access_peer_1 BACKUP /folder1/fileX 4<br>
        java TestApp access_peer_1 RESTORE /folder1/fileX<br>
        java TestApp access_peer_1 DELETE /folder1/fileX<br>
        java TestApp access_peer_1 RECLAIM 6400000<br>
        java TestApp access_peer_1 STATE<br>
        java TestApp access_peer_1 CHORD<br><br>

Scripts to facilitate testing can be found in src/scripts/ and are to be run in the src folder. Additional information can be found in docs/report.pdf
