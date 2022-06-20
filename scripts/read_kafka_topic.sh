#!/bin/sh

PATH=${PATH}:/home/ubuntu/bin/kafka_2.13-2.6.0/bin
export PATH

kafka-console-consumer.sh --from-beginning  --topic  $1 --bootstrap-server vdb1:9092

