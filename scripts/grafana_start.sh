#!/bin/sh

# This file is part of VoltDB.
#  Copyright (C) 2008-2020 VoltDB Inc.
# 
#  Permission is hereby granted, free of charge, to any person obtaining
# a copy of this software and associated documentation files (the
#  "Software"), to deal in the Software without restriction, including
#  without limitation the rights to use, copy, modify, merge, publish,
# distribute, sublicense, and/or sell copies of the Software, and to
#  permit persons to whom the Software is furnished to do so, subject to
#  the following conditions:
# 
#  The above copyright notice and this permission notice shall be
#  included in all copies or substantial portions of the Software.
# 
#  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
#  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
#  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
#  IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
#  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
#  ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
#  OTHER DEALINGS IN THE SOFTWARE.

cd $HOME

. ${HOME}/.profile

cd bin

MOUNTPOINT=$HOME/voltdbroot
GRAFANA_VERSION=grafana-8.5.3
GRAFANA_EXEC_NAME=grafana-server

if 
        [ -d "/voltdbdata" ]
then
        MOUNTPOINT=/voltdbdata/voltdbroot
fi


mkdir -p ${MOUNTPOINT}/log 2> /dev/null

LOGFILE=${MOUNTPOINT}/log/start_grafana_if_needed_`date '+%y%m%d'`.log
touch $LOGFILE

if 	
	[ ! -d "$GRAFANA_VERSION" ]
then
	echo `date` "Untarring files for ${GRAFANA_VERSION}" | tee -a $LOGFILE


	GRAFANA_VERSION_2=grafana-enterprise-8.5.3
	gunzip ${GRAFANA_VERSION_2}.linux-amd64.tar.gz | tee -a $LOGFILE
	tar xvf ${GRAFANA_VERSION_2}.linux-amd64.tar | tee -a $LOGFILE
	gzip ${GRAFANA_VERSION_2}.linux-amd64.tar
	gunzip  grafana.db.site0.gz
	mkdir ${GRAFANA_VERSION}/data 2> /dev/null
	cp grafana.db.site0 ${GRAFANA_VERSION}/data/grafana.db
	gzip  grafana.db.site0

fi



#
# See if we need to start 
#

curl -m 1 localhost:3000 > /tmp/$$curl.log

if
	[ -s /tmp/$$curl.log ]
then
        echo `date` "grafana already running" | tee -a  $LOGFILE
else
	# kill it if its hung...
	OLDPROCESS=`ps -deaf | grep  ${GRAFANA_EXEC_NAME} | grep -v grep | awk '{ print $2 }'`
	if
	        [ "$OLDPROCESS" != "" ]
	then
		echo `date` killed process $OLDPROCESS
		kill -9 $OLDPROCESS
	fi

        cd ${HOME}/bin/${GRAFANA_VERSION}/bin
        echo `date` "starting grafana" | tee -a  $LOGFILE
        nohup ./${GRAFANA_EXEC_NAME} 2>&1 >  $LOGFILE &

       VRUN=`ps -deaf | grep ${GRAFANA_EXEC_NAME} | grep -v grep | awk '{ print $2}'`
       echo $VRUN > ${HOME}/.grafana.PID
fi

rm /tmp/$$curl.log

exit 0
