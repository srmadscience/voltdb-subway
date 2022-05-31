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

MOUNTPOINT=$HOME/voltdbroot
GRAFANA_VERSION=grafana-enterprise-8.5.3
GRAFANA_EXEC_NAME=grafana-server

if 
        [ -d "/voltdbdata" ]
then
        MOUNTPOINT=/voltdbdata/voltdbroot
fi


mkdir -p ${MOUNTPOINT}/log 2> /dev/null

LOGFILE=${MOUNTPOINT}/log/stop_grafana_if_needed_`date '+%y%m%d'`.log
touch $LOGFILE

OLDPROCESS=`ps -deaf | grep ${GRAFANA_EXEC_NAME} | grep -v grep | awk '{ print $2 }'`

if
	[ "${OLDPROCESS}" != "" ]
then
	kill -9 $OLDPROCESS	
fi

rm ${HOME}/.grafana.PID 2> /dev/null


exit 0
