FEUP-SDIS
Assignment 1
t6g12

Java SE version: 11 & 13 (although the elements worked in different versions there were no conflicts)

 --------------
| INSTRUCTIONS |
 --------------

-> Within the src directory you can execute the following commands to start running the program

COMPILING
$ javac *.java

STARTING RMI
$ rmiregistry

RUNNING
- peer opening -
$ java Peer <version> <peerID> <access_point> <MC_IP> <MC_PORT> <MDB_IP> <MDB_PORT> <MDR_IP> <MDR_PORT>
e.g. java Peer 1.0 1 sdis 225.0.0.1 25564 225.0.0.1 25565 225.0.0.1 25566
NOTE: version 1.0 is the base project, with no improvements. Since enhancements have been implemented for the backup and delete protocol, 
to run the peer with the enhanced version, the version must be 2.0

- backup protocol -
$ java TestApp <access_point> BACKUP <file_path> <replication_degree>
e.g. java TestApp sdis BACKUP ./TestFiles/test.pdf 4

- restore protocol -
$ java TestApp <access_point> RESTORE <file_path> 
e.g. java TestApp sdis RESTORE ./TestFiles/test.pdf 

- delete protocol - 
$ java TestApp <access_point> DELETE <file_path> 
e.g. java TestApp sdis DELETE ./TestFiles/test.pdf 

- reclaim protocol - 
$ java TestApp <access_point> RECLAIM <file_path> <max_disk_space>
e.g. java TestApp sdis RECLAIM 100

- show peer's state -
$ java TestApp <access_point> STATE
e.g. java TestApp sdis STATE



--------------------------------------------------------------------------------------------------------------------------------
Given that it was suggested to create some scripts that facilitate and save us time when testing the programs, below is a version 
of instructions that should be followed if the use of the scripts is desired.

-> Within the src directory you can execute the following commands to start running the program

COMPILING
$ sh compile.sh

RUNNING
$ sh runPeers.sh <version> (for xterm Terminal)
version should be 1.0 or 2.0. If none of this values is used, script uses default value (1.0).

or

$ sh runPeersGnome.sh <version> (for gnome-terminal)
version should be 1.0 or 2.0. If none of this values is used, script uses default value (1.0).

* After running these scripts just enter the commands specified above for each subprotocol
  The access points for peers 1 through 5 are, respectively, access_peer_1 through access_peer_5

CLEAN
$ sh clean.sh

NOTE: The scripts presented were only tested on our own computers, and therefore we can't predict whether they'll work on other machines.
As an alternative to using $ sh script.sh, on some machines $ ./script.sh will suffice.
The runPeers.sh script is recommended over the Gnome alternative, as the second was developed purely to allow compatibility with systems lacking xterm.













Vítor Gonçalves
Vítor Ventuzelos
