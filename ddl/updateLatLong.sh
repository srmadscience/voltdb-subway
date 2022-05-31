#!/bin/sh
cat londonStations.csv  | tr '[a-z]' '[A-Z]' | awk -F, '{ print "exec UpdateStation ", $4,  $5, "\"0\"","\"",$1,"\";"  }' > loadStationCoords.sql

