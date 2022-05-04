#!/bin/sh

INFILE=subwaytest_unsortedday.csv
OUTFILE=subwaytestfullweek.csv
rm ${OUTFILE}
for i in Sun Mon Tue Wed Thu Fri Sat 
do
	echo $i
 	grep "^${i}" ${INFILE} | sort -t, -k2,3  >> ${OUTFILE}
done
