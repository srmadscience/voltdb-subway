#!/bin/sh

cd ../csv

rm subwaytestfullweek.csv 2> /dev/null

for i in a b c d e f g h i
do
	gunzip subwaytestfullweek.csv.?${i}.gz
	cat subwaytestfullweek.csv.?${i} >> subwaytestfullweek.csv
	gzip subwaytestfullweek.csv.?${i}
done




