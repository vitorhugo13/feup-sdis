#!/bin/sh
#change to /bin/sh if it still doesn't work

# Multicast channels
MC_IP="225.0.0.1"
MC_PORT="25564"
MDB_IP="225.0.0.1"
MDB_PORT="25565"
MDR_IP="225.0.0.1"
MDR_PORT="25566"

if [ $# == 1 ]
then
  PROTOCOL_VERSION=$1
else
  PROTOCOL_VERSION="1.0"
fi

echo "Starting rmiregistry..."
gnome-terminal -e "rmiregistry" > /dev/null &
PROC_IDS[0]=$!

sleep 1

echo "Starting peers..."
gnome-terminal -e "java Peer $PROTOCOL_VERSION 1 access_peer_1 $MC_IP $MC_PORT $MDB_IP $MDB_PORT $MDR_IP $MDR_PORT" > /dev/null &
PROC_IDS[1]=$!
gnome-terminal -e "java Peer $PROTOCOL_VERSION 2 access_peer_2 $MC_IP $MC_PORT $MDB_IP $MDB_PORT $MDR_IP $MDR_PORT" > /dev/null &
PROC_IDS[2]=$!
gnome-terminal -e "java Peer $PROTOCOL_VERSION 3 access_peer_3 $MC_IP $MC_PORT $MDB_IP $MDB_PORT $MDR_IP $MDR_PORT" > /dev/null &
PROC_IDS[3]=$!
gnome-terminal -e "java Peer $PROTOCOL_VERSION 4 access_peer_4 $MC_IP $MC_PORT $MDB_IP $MDB_PORT $MDR_IP $MDR_PORT" > /dev/null &
PROC_IDS[4]=$!
gnome-terminal -e "java Peer $PROTOCOL_VERSION 5 access_peer_5 $MC_IP $MC_PORT $MDB_IP $MDB_PORT $MDR_IP $MDR_PORT" > /dev/null &
PROC_IDS[5]=$!

read -p "Peers running, press any key to finish execution and close all peers." -n 1 -s

echo "Closing peers..."
kill ${PROC_IDS[1]}
kill ${PROC_IDS[2]}
kill ${PROC_IDS[3]}
kill ${PROC_IDS[4]}
kill ${PROC_IDS[5]}

echo "Closing rmiregistry..."
kill ${PROC_IDS[0]}

echo "Execution finished."

