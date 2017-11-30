#!/bin/bash


echo Downloading api protocol
curl -o api.pdf http://localhost:8082/api/pdf

# call initialize
echo initializing
initResult=$(curl -s -H "Content-Type: application/json" --data @$1 http://localhost:8082/initialize)

echo Got response $initResult

sessionId=`echo $initResult | cut -d ":" -f 3- | cut -d "," -f 1`


echo sessionid = $sessionId
# find the session id

# start simulation
echo simulating

simResult=$(curl -s -H "Content-Type: application/json" --data '{"startTime":0.0, "endTime":10.1}' http://localhost:8082/simulate/$sessionId)

echo Got response $simResult


echo Fetching result

resResult=$(curl -s http://localhost:8082/result/$sessionId)

#echo Got result: $resResult


echo "$resResult" > "$sessionId.log"

python plot-session.py "$sessionId.log" $2 &

