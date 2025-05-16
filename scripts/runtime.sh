#!/bin/sh

cd ../jars

java ${JVMOPTS} -jar td_client.jar `cat $HOME/.vdbhostnames` RUN ../csv/subwaytestfullweek.csv 1000000 50000 1 SPEED 60

