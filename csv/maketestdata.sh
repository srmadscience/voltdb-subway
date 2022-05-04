#!/bin/sh

rm Nov09JnyExport.csv 2> /dev/null

for i in Nov09JnyExport.csv.a?
do
 	cat $i >> Nov09JnyExport.csv
done

sh createTestDataMidnightFix.sh
sh assembleCsvFileByDay.sh	

split -l 400000 subwaytestfullweek.csv subwaytestfullweek.csv.
