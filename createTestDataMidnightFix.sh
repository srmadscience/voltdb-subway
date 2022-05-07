#!/bin/sh

#
# downo - a number between 1 and 7, 1 being Sunday, 2 being Monday etc
# daytype - Sun to Sat
# SubSystem - the mode(s) of the journey. LUL - London Underground, NR - National Rail, LTB - London Buses, DLR- Docklands Light Railway, LRC - London Overground, TRAM - Croydon Tram
# StartStn - Station the journey started at
# EndStation - Station the journey ended at
# EntTime - Entry time of the journey in minutes after midnight
# EntTimeHHMM - Entry time in HH:MM text format
# ExTime - Exit time of the journey in minutes after midnight
# EXTimeHHMM - Exit time in HH:MM text format
# ZVPPT - zones of Oyster Season ticket, if used
# JNYTYP - Product types involved in the journey. PPY - Pure PAYG, TKT - Pure Oyster Season, MIXED - Combined PAYG and Oyster Season
# DailyCapping - it shows as Y when PAYG journey was capped
# FFare - Full PAYG Fare before any discounts
# Dfare - PAYG Fare after usage based discounts
# RouteID - The Route Number of the Bus, if a Bus has been boarded
# FinalProduct - Combined Product Description used for journey
# 


INPUTFILE=Nov09JnyExport.csv 
INPUTFILE2=tmpDAY.csv

grep -v Unstarted $INPUTFILE | grep -v Unfinished | sed '1,$s/\"//'g > $INPUTFILE2

EVENTNO=1

rm subwaytest_unsortedday.csv 2> /dev/null

while IFS='' read -r line || [[ -n "$line" ]]; do

    DAYOFWEEK=`echo $line | awk -F, '{ print $1}'`
    DAYNAME=`echo $line | awk -F, '{ print $2}' `
    SUBSYSTEM=`echo $line | awk -F, '{ print $3}' `
    STARTSTATION=`echo $line | awk -F, '{ print $4}'`
    ENDSTATION=`echo $line | awk -F, '{ print $5}' `
    STIME=`echo $line | awk -F, '{ print $7}'`
    MINMIDNIGHT=`echo $line | awk -F, '{ print $8}' `
    ETIME=`echo $line | awk -F, '{ print $9}'`
    ZVPPT=`echo $line | awk -F, '{ print $10}'`
    JNYTYP=`echo $line | awk -F, '{ print $11}'`
    CAPPED=`echo $line | awk -F, '{ print $12}'`
    FFARE=`echo $line | awk -F, '{ print $13}'`
    DFARE=`echo $line | awk -F, '{ print $14}'`
    BUSROUTE=`echo $line | awk -F, '{ print $15}'`
    FINALPRODUCT=`echo $line | awk -F, '{ print $16}'`
    FINALPRODUCT=`echo $FINALPRODUCT | sed '1,$s///g'`
	

    while [ ${#STIME} -lt 5 ];
    do
		STIME="0"$STIME
    done

    while [ ${#ETIME} -lt 5 ];
    do
		ETIME="0"$ETIME
    done


    if 
		[ "$STIME" != "00:00"  ]
    then

    	if
    		[ "$SUBSYSTEM" = "LTB" ]
    	then
            	echo "$DAYNAME,$STIME,BUSTRIP,$EVENTNO,$SUBSYSTEM,$JNYTYP,$CAPPED,$FFARE,$DFARE,$BUSROUTE,$FINALPRODUCT"
    	else
    		if
    			[ "$ETIME" != "00:00"  ]
    		then
    
    		    ENDDAYNAME=${DAYNAME}
    
    		    if 
    			[ ${MINMIDNIGHT} -lt 240 ]
    		    then
    			# echo we finished before 0400 - bump day...
                            case $ENDDAYNAME in
    				Mon)
    				ENDDAYNAME=Tue
    				;;
    				Tue)
    				ENDDAYNAME=Wed
    				;;
    				Wed)
    				ENDDAYNAME=Thu
    				;;
    				Thu)
    				ENDDAYNAME=Fri
    				;;
    				Fri)
    				ENDDAYNAME=Sat
    				;;
    				Sat)
    				ENDDAYNAME=Sun
    				;;
    				Sun)
    				ENDDAYNAME=Mon
    				;;
    				*)
    				ENDDAYNAME=XXX
    				;;
      		 	esac
    		    fi
    
    	            if
    		        [ "${ENDDAYNAME}" != "${DAYNAME}" ]
    	            then
        	    	echo "$DAYNAME,$STIME,BEGINTRIP,$EVENTNO,$SUBSYSTEM,$STARTSTATION" >> specials.lst
        	    	echo "$ENDDAYNAME,$ETIME,ENDTRIP,$EVENTNO,$SUBSYSTEM,$ENDSTATION,$ZVPPT,$JNYTYP,$CAPPED,$FFARE,$DFARE,$FINALPRODUCT" >> specials.lst
    		    fi

        	    echo "$DAYNAME,$STIME,BEGINTRIP,$EVENTNO,$SUBSYSTEM,$STARTSTATION"
        	    echo "$ENDDAYNAME,$ETIME,ENDTRIP,$EVENTNO,$SUBSYSTEM,$ENDSTATION,$ZVPPT,$JNYTYP,$CAPPED,$FFARE,$DFARE,$FINALPRODUCT"
    		fi
    	
    	fi
    fi
    
    EVENTNO=`expr $EVENTNO + 1` 

done < "$INPUTFILE2"  | tee -a subwaytest_unsortedday.csv

#|  sort -t, -k3,2 > subwaytest.csv 

rm $INPUTFILE2

