#!/bin/bash

tmux send-keys -t 0 './test.sh' C-m
sleep 2
tmux send-keys -t 1 'java -jar ../../maestro/target/maestro-2.2.0-SNAPSHOT-jar-with-dependencies.jar interpret watertank.mabl' C-m
tmux send-keys -t 2 'tail -f outputs.csv' C-m



