#!/bin/bash

echo "Removing old java .class files..."
rm ./*.class
rm ./*/*.class

echo "Compiling Peer..."
javac Peer.java

echo "Compiling TestApp..."
javac TestApp.java

echo "Compiled."