
load classes ../jars/td.jar;

file -inlinebatch END_OF_DDL

create table simulation_speed 
(tps_or_speed varchar(5) not null
,number_value int not null);


create table simulation_time
(simulation_time TIMESTAMP not null primary key);

create table products
(product_name varchar(50) not null primary key);

create table subsystem
(subsystem_name varchar(15) NOT NULL PRIMARY KEY
,subsystem_desc varchar(56) NOT NULL);

create table event_code
(code INT NOT NULL PRIMARY KEY
,code_desc varchar(80) NOT NULL);

CREATE TABLE station
(station_name VARCHAR(30) PRIMARY KEY
,latitude     float
,longitude    float
,station_zone varchar(3)
,postcode     varchar(8));

CREATE TABLE busroutes
(route_id VARCHAR(10) PRIMARY KEY);

CREATE TABLE subway_fares
(from_station_name VARCHAR(30) NOT NULL
,to_station_name VARCHAR(30) NOT NULL
,fare_in_pennies INT NOT NULL
,discount_fare_in_pennies INT 
,CONSTRAINT sufr_pk PRIMARY KEY (from_station_name, to_station_name));

CREATE STREAM missing_subway_fares
PARTITION ON COLUMN id 
  EXPORT TO TOPIC missing_subway_fares_topic
(id BIGINT NOT NULL
,from_station_name VARCHAR(30) NOT NULL
,to_station_name VARCHAR(30) NOT NULL);

CREATE TABLE transport_user 
	(id BIGINT NOT NULL PRIMARY KEY
	,first_seen TIMESTAMP NOT NULL
	,last_seen  TIMESTAMP 
	,active_subway_event BIGINT
	,active_subway_event_start TIMESTAMP
	,last_dep_station VARCHAR(30)
	,last_arr_station VARCHAR(30)
	,last_add_finevent_date TIMESTAMP
	,last_add_finevent_amount INTEGER
	,last_spend_finevent_date TIMESTAMP
	,last_spend_finevent_amount INTEGER
	,swipecount integer default 0
	);

PARTITION TABLE transport_user ON COLUMN id;

echo Index used by view active_subway_users
CREATE INDEX tu_ix1 ON transport_user(active_subway_event_start);

echo Index used by subway_starts_by_min
CREATE INDEX tu_ix2 ON transport_user(truncate(minute, active_subway_event_start));


echo Index used to track purchase
CREATE INDEX tu_ix4 ON transport_user(truncate(minute, last_spend_finevent_date));

echo Index used to track spending
CREATE INDEX tu_ix5 ON transport_user(truncate(minute, last_add_finevent_date));



create table event_dup_check
(user_id BIGINT NOT NULL 
,event_id BIGINT NOT NULL
,event_type VARCHAR(10) NOT NULL
,event_timestamp TIMESTAMP NOT NULL
,CONSTRAINT edc_pk PRIMARY KEY (user_id,event_id, event_type));

PARTITION TABLE event_dup_check ON COLUMN user_id;

CREATE INDEX edc_purge ON event_dup_check
(event_timestamp,user_id,event_id, event_type);


CREATE STREAM transport_user_fraud_event
PARTITION ON COLUMN user_id 
  EXPORT TO TOPIC transport_user_fraud_event_topic
	(user_id BIGINT NOT NULL 
	,event_id BIGINT NOT NULL
	,event_type TINYINT NOT NULL
	,event_timestamp TIMESTAMP NOT NULL
	,event_comment VARCHAR(80)
	);
	
CREATE VIEW transport_user_fraud_event_summary AS
SELECT truncate(minute, event_timestamp) event_time, event_type,event_comment, count(*) fraud_events
FROM   transport_user_fraud_event
GROUP BY truncate(minute, event_timestamp), event_type,event_comment;

create index tufes_ix1 on transport_user_fraud_event_summary(event_time);
	
CREATE STREAM transport_user_financial_event
PARTITION ON COLUMN user_id 
  EXPORT TO TOPIC transport_user_financial_event_topic
	(user_id BIGINT NOT NULL 
	,event_id BIGINT NOT NULL
	,event_timestamp TIMESTAMP NOT NULL
	,credit_adjustment BIGINT NOT NULL
	,closing_credit    BIGINT NOT NULL
	,event_comment VARCHAR(80)
	);
	
CREATE TABLE transport_user_subway_event
	(user_id BIGINT NOT NULL 
	,event_id BIGINT NOT NULL
	,event_timestamp TIMESTAMP NOT NULL
	,start_station VARCHAR(30) 
    ,end_station VARCHAR(30)  
    ,duration_seconds INT NOT NULL
    ,subsystem_name varchar(15) not null
    ,CONSTRAINT tuse_pk PRIMARY KEY (user_id, event_id,event_timestamp)
	);
	
PARTITION TABLE transport_user_subway_event ON COLUMN user_id;

create index tuse_ix2 on transport_user_subway_event (truncate(minute, event_timestamp));
	
create index tuse_ix3 on transport_user_subway_event (event_timestamp,user_id, event_id);
	

create table transport_user_bus_event(user_id BIGINT NOT NULL 
	,event_id BIGINT NOT NULL
	,busroute varchar(10) NOT NULL 
	,event_timestamp TIMESTAMP NOT NULL
	,CONSTRAINT be_pk PRIMARY KEY (user_id, event_id));

PARTITION TABLE transport_user_bus_event ON COLUMN user_id;

create index tube_ix1 on transport_user_bus_event (event_timestamp,user_id, event_id);


create stream user_trip_outcomes
PARTITION ON COLUMN user_id 
  EXPORT TO TOPIC user_trip_outcomes_topic
	(user_id BIGINT NOT NULL 
	,trip_time TIMESTAMP NOT NULL
	,outcome_code INT NOT NULL
	,outcome_message VARCHAR(800));
	

CREATE view latest_subway_start_by_min AS
SELECT max(truncate(minute, active_subway_event_start)) event_time, count(*) starts_this_min
FROM   transport_user
WHERE active_subway_event IS NOT NULL;

CREATE view subway_starts_by_min AS
SELECT truncate(minute, active_subway_event_start) event_time,  count(*) boardings_this_min
FROM   transport_user
WHERE active_subway_event IS NOT NULL
GROUP BY truncate(minute, active_subway_event_start);

create index sbbm_ix1 on subway_starts_by_min(event_time);



create view subsystem_activity as
select subsystem_name, truncate(minute, event_timestamp) event_minute, count(*) how_many
from transport_user_subway_event group by subsystem_name, truncate(minute, event_timestamp) ;

create index ssa_ix1 on subsystem_activity(event_minute);



CREATE view subway_ends_by_min AS
SELECT truncate(minute, event_timestamp) event_time,  count(*) ends_this_min
FROM   transport_user_subway_event
GROUP BY truncate(minute, event_timestamp);

create index sebm_ix1 on subway_ends_by_min(event_time);


create view minimum_plausible_trip_lengths as
select start_station, end_station, count(*) how_many , max(duration_seconds) maxtime, min(duration_seconds) mintime
from transport_user_subway_event
group by start_station, end_station;

create index tuse_ix1 on transport_user_subway_event (start_station, end_station);

create view subway_activity_by_minute as
select start_station, end_station, truncate(minute,event_timestamp) event_minute, count(*) how_many 
from transport_user_subway_event
group by start_station, end_station, truncate(minute,event_timestamp) ;

create index sabm_ix1 on subway_activity_by_minute (event_minute,start_station, end_station);

create view subway_board_activity_by_minute as
select start_station, truncate(minute,event_timestamp) event_minute, count(*) how_many 
from transport_user_subway_event
group by start_station, truncate(minute,event_timestamp) ; 

create index sbabm_ix1 on subway_board_activity_by_minute (event_minute,start_station );

create view subway_finish_activity_by_minute as
select end_station, truncate(minute,event_timestamp) event_minute, count(*) how_many 
from transport_user_subway_event
group by end_station, truncate(minute,event_timestamp) ; 

create index sbaem_ix1 on subway_finish_activity_by_minute (event_minute,end_station );

create view bus_event_by_minute as
select busroute, truncate(minute, event_timestamp) event_minute, count(*) how_many
from transport_user_bus_event
group by busroute, truncate(minute, event_timestamp) ;

create index bebm_ix1 on  bus_event_by_minute (event_minute, busroute);

create view bus_event_total_by_minute as
select  truncate(minute, event_timestamp) event_minute, count(*) how_many
from transport_user_bus_event
group by  truncate(minute, event_timestamp) ;

create index betbm_ix1 on  bus_event_total_by_minute (event_minute);

create view total_swipes as select sum(swipecount) total_swipes, count(*) user_count from transport_user;

create view active_subway_users as 
select count(*) how_many, max(active_subway_event_start) latest_activity
from transport_user 
where active_subway_event IS NOT NULL;


	
create view trip_outcome_summary as
select user_id, trip_time, outcome_code, outcome_message, count(*)
from user_trip_outcomes
where outcome_code < 0
group by user_id, trip_time, outcome_code, outcome_message;

create index tos_idx1 on trip_outcome_summary(user_id, trip_time);
	
create view transport_user_balance as
select user_id, count(*) how_many, sum(credit_adjustment) credit, max(event_timestamp) last_event
FROM transport_user_financial_event
group by user_id;

create index tub_idx1 on transport_user_balance(user_id, last_event);




	
create view transport_user_fraud_status as
select user_id, count(*) how_many, max(event_timestamp) latest_event
from transport_user_fraud_event
group by user_id;

CREATE TABLE sim_stats
(stat_name varchar(1024) not null 
,stat_label_1 varchar(80)
,stat_label_2 varchar(80)
,latitude float
,longitude float
,stat_value float not null
,primary key(stat_name,stat_label_1,stat_label_2));


END_OF_DDL
	


file -inlinebatch END_OF_PROC

 
CREATE PROCEDURE 
  FROM CLASS trandemo.server.ResetDatabase;

CREATE PROCEDURE 
  FROM CLASS trandemo.server.UpdateStation;
   
CREATE PROCEDURE 
   FROM CLASS trandemo.server.DashBoard2;

CREATE PROCEDURE 
   FROM CLASS trandemo.server.UpdateSimulationTime;
   
CREATE PROCEDURE 
   FROM CLASS trandemo.server.UpdateSimulationSpeed;
   
CREATE PROCEDURE 
   FROM CLASS trandemo.server.UpdateSubwayTripPrice;

CREATE PROCEDURE 
   PARTITION ON TABLE transport_user COLUMN id
   FROM CLASS trandemo.server.CardSwipe;
   
CREATE PROCEDURE 
   PARTITION ON TABLE transport_user COLUMN id
   FROM CLASS trandemo.server.UpdateCard;
   
CREATE PROCEDURE 
   PARTITION ON TABLE transport_user COLUMN id
   FROM CLASS trandemo.server.QueryUser;
   
create procedure  FROM CLASS trandemo.server.MeasureThroughput;

CREATE PROCEDURE UpdatePrices AS
UPDATE subway_fares 
SET fare_in_pennies = fare_in_pennies * CAST(? AS FLOAT);

CREATE PROCEDURE FreeSpace AS
BEGIN
--
DELETE FROM event_dup_check;
DELETE FROM transport_user_subway_event;
DELETE FROM transport_user_bus_event;
-- 
END;

CREATE PROCEDURE ReportStats__promBL AS
BEGIN
--
select 'speed' statname
     ,  'speed' stathelp  
     ,  decode(tps_or_speed,'SPEED',number_value,0) statvalue   
from simulation_speed order by  decode(tps_or_speed,'SPEED',number_value,0);
--
select 'tps' statname
     ,  'tps' stathelp  
     ,  decode(tps_or_speed,'TPS',number_value,0) statvalue   
from simulation_speed order by decode(tps_or_speed,'TPS',number_value,0) ;
--
select 'current_users' statname
     ,  'current_users' stathelp  
     ,  how_many statvalue
from active_subway_users 
order by how_many, latest_activity;
--
select stat_name statname
     ,  stat_name stathelp  
     ,  stat_value statvalue
from sim_stats 
where stat_label_1 is null
and   stat_label_2 is null
order by stat_name;
--
select stat_name statname
     , stat_label_1
     ,  stat_name stathelp  
     ,  stat_value statvalue
from sim_stats 
where stat_label_1 is not null 
and   stat_label_2 is null
and   latitude is  null 
and   longitude is  null
order by stat_name,stat_label_1;
--
select stat_name statname
     , stat_label_1
     , latitude
     , longitude
     ,  stat_name stathelp  
     ,  stat_value statvalue
from sim_stats 
where stat_label_1 is not null 
and   stat_label_2 is null
and   latitude is not null 
and   longitude is not null
order by stat_name,stat_label_1;
--
select stat_name statname
     , stat_label_1
     , stat_label_2
      , latitude
     , longitude
    ,  stat_name stathelp  
     ,  stat_value statvalue
from sim_stats 
where stat_label_1 is not  null
and   stat_label_2 is not null
and   latitude is not null 
and   longitude is not null
order by stat_name,stat_label_1, stat_label_2;
--
select stat_name statname
     , stat_label_1
     , stat_label_2
    ,  stat_name stathelp  
     ,  stat_value statvalue
from sim_stats 
where stat_label_1 is not  null
and   stat_label_2 is not null
and   latitude is  null 
and   longitude is  null
order by stat_name,stat_label_1, stat_label_2;
--
END;


END_OF_PROC
      

file subwayfares.sql	
