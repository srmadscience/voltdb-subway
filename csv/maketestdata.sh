#!/bin/sh

rm Nov09JnyExport.csv 2> /dev/null

gunzip Nov09JnyExport.csv.a?.gz 

for i in Nov09JnyExport.csv.a?
do
 	cat $i >> Nov09JnyExport.csv
done

sh createTestDataMidnightFix.sh > /dev/null
sh assembleCsvFileByDay.sh	

split -l 400000 subwaytestfullweek.csv subwaytestfullweek.csv.

gzip Nov09JnyExport.csv.a?
gzip subwaytestfullweek.csv.??
rm subwaytestfullweek.csv
