#!/bin/bash

echo "Removing java .class files..."
rm ./*.class
rm ./data/*.class
rm ./udp_connection/*.class
echo "Removing peer databases..."
rm -r peer_disk
echo "Finished."
