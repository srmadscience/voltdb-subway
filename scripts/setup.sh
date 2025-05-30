#!/bin/sh

. $HOME/.profile


cd 

AWSTYPE=` curl --connect-timeout 5 http://169.254.169.254/latest/meta-data/instance-type` 2> /dev/null


cd
cd voltdb-subway/ddl

sqlcmd --servers=vdb1 < transportDemoSchema.sql

cd ../scripts

java ${JVMOPTS}  -jar $HOME/bin/addtodeploymentdotxml.jar vdb1 deployment topics.xml

cd ../scripts
cp voltdb-subway.json ../../bin/dashboards
sh $HOME/bin/reload_dashboards.sh 

cd ../csv

rm subwaytestfullweek.csv 2> /dev/null

for i in a b c d e f g h i
do
	gunzip subwaytestfullweek.csv.?${i}.gz
	cat subwaytestfullweek.csv.?${i} >> subwaytestfullweek.csv
	gzip subwaytestfullweek.csv.?${i}
done

cd ../jars
java ${JVMOPTS}  -jar td_client.jar vdb1 USERS ../csv/subwaytestfullweek.csv 1000000 5000 1 SPEED 10

cd ../ddl
sqlcmd --servers=vdb1 < updateStations.sql 


