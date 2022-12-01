<img title="Volt Active Data" alt="Volt Active Data Logo" src="http://52.210.27.140:8090/voltdb-awswrangler-servlet/VoltActiveData.png">


# README #

This README explains how to use this demo

## What is this repository for? ##

* The repo contains the code required to run a transport demo based on real user data.
* Version 1.0

## How do I get set up? ##

* Create a new VoltDB database
* Compile the classes in serverSrc and put them in a JAR file called td.jar next to the top level directory
* Create the schema and load the fare table:

```
cd transportdemo/ddl
sqlcmd < transportDemoSchema.sql
sqlcmd < subwayfares.sql
```

* Run the 'Demo' class in clientSrc. Demo takes the following parameters:

1. First hostname in cluster - e.g. 127.0.0.1
2. Path to test data csv file - e.g. /Users/drolfe/Desktop/EclipseWorkspace/transportdemo/csv/subwaytest.csv
3. Action - RUN or RESET
4. How many users - e.g.  980525, which is the number in the data. Each user makes one and only one trip because the original data is anonymized. See below..
5. Initial credit in pennies - 10000
6. Number of days: e.g. 30
7. Transasctions per MS e.g. 25 - note that we don't attempt to model real time.

E.g.:
````
java -jar td_client.jar localhost RUN transportdemo/csv/subwaytestfullweek.csv 1000000 200000 80 SPEED 720
````

## What is it based on? ##

This demo is inspired by a real live sample of Transport For London Oyster Card data gathered in 2009. The sample has 
had user identities tripped out but is of real people making real journies by Bus, Tram and Subway ("Tube").

The data came in a file called "Nov09JnyExport.csv", in which each line represented an entire journey. We've modified
it (subwaytest.csv) so each line represents one of the following events:

* BUSTRIP - Somebody boarded a bus, but we don't know where on the busroute.
* STARTTRIP - A user started a tube journey, but we don't know where they are going
* ENDTRIP - A user finished a tube journey. We need to find the matching STARTTRIP, calculate the cost and charge them

Note that the events are in chronological order. the Demo reads the events and calls CardSwipe for each one. If the response is that the user has 
insufficant funds it adds credit and then retriues.

## What does it show? ##

### VoltDB's suitability for complex real world applications ###

### Charging ###

### Fraud Detection ###

### Translytics / HTAP ###

### Feeds to streams ###

### Real Time Dashboards ###

## Messing with the parameters for fun and profit... ##

## Who do I talk to? ##

