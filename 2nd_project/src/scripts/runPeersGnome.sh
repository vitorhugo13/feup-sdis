#!/bin/sh
#change to /bin/sh if it still doesn't work

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
gnome-terminal -e "java Peer $PROTOCOL_VERSION 1 access_peer_1 8000" > /dev/null &
PROC_IDS[1]=$!
sleep 1
gnome-terminal -e "java Peer $PROTOCOL_VERSION 2 access_peer_2 8001 127.0.1.1 8000" > /dev/null &
PROC_IDS[2]=$!
sleep 1
gnome-terminal -e "java Peer $PROTOCOL_VERSION 3 access_peer_3 8002 127.0.1.1 8001" > /dev/null &
PROC_IDS[3]=$!
sleep 1
gnome-terminal -e "java Peer $PROTOCOL_VERSION 4 access_peer_4 8003 127.0.1.1 8002" > /dev/null &
PROC_IDS[4]=$!

read -p "Peers running, press any key to finish execution and close all peers." -n 1 -s

echo "Closing peers..."
kill ${PROC_IDS[1]}
kill ${PROC_IDS[2]}
kill ${PROC_IDS[3]}
kill ${PROC_IDS[4]}

echo "Closing rmiregistry..."
kill ${PROC_IDS[0]}

echo "Execution finished."

