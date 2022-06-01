#!/bin/sh

cd /home/ubuntu

sudo cat 127.0.0.1 vdb1 >> /etc/hosts

echo vdb1 > .vdbhostnames  

. ./.profile

cd bin
sh prometheusserver_configure.sh

cd
cd voltdb-subway/ddl

sqlcmd --servers=vdb1 < transportDemoSchema.sql

cd ../scripts

java -jar $HOME/bin/addtodeploymentdotxml.jar vdb1 deployment topics.xml

cd ../csv

rm subwaytestfullweek.csv 2> /dev/null

for i in a b c d e f g h i
do
	gunzip subwaytestfullweek.csv.?${i}.gz
	cat subwaytestfullweek.csv.?${i} >> subwaytestfullweek.csv
	gzip subwaytestfullweek.csv.?${i}
done

sudo service grafana start 2> /dev/null
sudo service  voltdbprometheusbl start 2> /dev/null
sudo service  voltdbprometheus start 2> /dev/null



