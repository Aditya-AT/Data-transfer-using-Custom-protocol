#!/bin/bash

# file that gets run when the docker container starts up
# Sam Fryer.

#first, start the nodejs server to receive any initial commands
node run/run.js $1 &

#then wait 10 seconds to make sure the commands came and executed
sleep 10

#finally, start the initial program
if [ ! -z $2 ]
then
    java Main $1 $2
else
    java Main
fi
