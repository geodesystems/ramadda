

-----------------------------------------------------------------------
---Note: the ramadda.datetime and ramadda.double are replaced by ramadda
---with the appropriate datatype for the database being used.
---mysql has a datetime type, postgres and derby have timestamp
---we can't use timestamp for mysql because it only goes back to 1970
---derby and mysql have double. postgres has float8
-----------------------------------------------------------------------


-----------------------------------------------------------------------
--- the main entries table
-----------------------------------------------------------------------
CREATE TABLE entries (id varchar(200),
                   type varchar(200),
	           name varchar(200),
----               description varchar(32000),
                   description ramadda.bigclob,
                   parent_group_id varchar(200),
   		   user_id varchar(200),
	           resource varchar(500),	           
                   resource_type varchar(200),
                   md5           varchar(32),
                   filesize ramadda.bigint,
		   datatype varchar(200),
		   entryorder int,
	           createdate ramadda.datetime, 
                   changedate ramadda.datetime, 
	           fromdate ramadda.datetime, 
	           todate ramadda.datetime,
	           south ramadda.double,
	           north ramadda.double,
	           east ramadda.double,
	           west ramadda.double,
                   altitudetop ramadda.double,
                   altitudebottom ramadda.double); 


----update the table
ALTER table entries add column entryorder int;

---- Note: if you change the description length make sure to change in Entry.java:
----    public static final int MAX_DESCRIPTION_LENGTH = 32000;

--- for mysql
--- alter table entries modify column description varchar(32000);

--- for derby
--- alter table entries alter  description set data type varchar(32000)

--- alter table entries modify column description varchar(32000);
--- alter table entries alter column description set data type varchar(64000);

-----------------------------------------------------------------
---derby: sometime we need to change the varchar description to clob for existing dbs
--- alter table entries add column new_description clob;
--- update entries set new_description=description;
--- rename column entries.description to old_description;
--- rename column entries.new_description to description;
--- alter table entries drop column old_description;
-----------------------------------------------------------------



CREATE INDEX ENTRIES_INDEX_ID ON entries (ID);
CREATE INDEX ENTRIES_INDEX_RESOURCE ON entries (RESOURCE);
CREATE INDEX ENTRIES_INDEX_PARENT_GROUP_ID ON entries (PARENT_GROUP_ID);
CREATE INDEX ENTRIES_INDEX_TYPE ON entries (TYPE);
CREATE INDEX ENTRIES_INDEX_USER_ID ON entries (USER_ID);
CREATE INDEX ENTRIES_INDEX_FROMDATE ON entries (FROMDATE);
CREATE INDEX ENTRIES_INDEX_TODATE ON entries (TODATE);

--Never used
#if  derby postgres oracle
DROP INDEX ENTRIES_INDEX_DATATYPE;
#endif

#if  mysql
DROP INDEX ENTRIES_INDEX_DATATYPE ON ENTRIES;
#endif


--- We used to have an ancestors table but it was never used
DROP TABLE ancestors;


-----------------------------------------------------------------------
---Holds metadata 
---Entries can have any number of metadata items
---The MetadataHandler classes handle the semantics. 
-----------------------------------------------------------------------
CREATE TABLE  metadata (id varchar(200),
			entry_id varchar(200),
                        type varchar(200),
                     	inherited int,
			access varchar(500),
                        attr1 ramadda.bigvarchar_orclob,
                        attr2 ramadda.bigvarchar_orclob,
                        attr3 ramadda.bigvarchar_orclob,
                        attr4 ramadda.bigvarchar_orclob,
		        extra ramadda.bigclob);

alter table metadata add column access varchar(500);


CREATE INDEX METADATA_INDEX_ID ON metadata (ID);
CREATE INDEX METADATA_INDEX_ENTRYID ON metadata (ENTRY_ID);
CREATE INDEX METADATA_INDEX_TYPE ON metadata (TYPE);
---- CREATE INDEX METADATA_INDEX_ATTR1 ON metadata (ATTR1);


-----------------------------------------------------------------------
--- comments 
-----------------------------------------------------------------------
CREATE TABLE  comments (id varchar(200),
		        entry_id varchar(200),
			user_id  varchar(200),
                        date ramadda.datetime, 
			subject  varchar(200),
                        comment varchar(1000));

CREATE INDEX COMMENTS_INDEX_ID ON comments (ID);
CREATE INDEX COMMENTS_INDEX_ENTRY_ID ON comments (ENTRY_ID);


-----------------------------------------------------------------------
--- associations 
-----------------------------------------------------------------------
CREATE TABLE associations (id varchar(200),
                           name varchar(200),
		           type varchar(200),
			   from_entry_id varchar(200),
		           to_entry_id varchar(200));


--- drop table entry_activity;
---CREATE TABLE entry_activity (
---                     entryid varchar(200),
---       		     date ramadda.datetime,
---		     week ramadda.datetime, 	     
---		     activity varchar(200),
---		     count int,
---	     	     ipaddress varchar(200)); 
---CREATE INDEX ENTRY_ACTIVITY_ENTRYID ON entry_activity (ENTRYID);

-----------------------------------------------------------------------
--- users 
-----------------------------------------------------------------------
CREATE TABLE  users (id varchar(200),
                     name  varchar(200),
		     status  varchar(200),		     
                     email varchar(200),
		     institution  varchar(400),
		     country  varchar(400),		     
                     question  varchar(200),
                     answer  varchar(200),  
                     password  varchar(200),
                     description varchar(5000),
		     admin int,
		     language varchar(50),
		     template varchar(200),
                     isguest int,
		     account_creation_date ramadda.datetime,	
                     properties ramadda.clob);

alter table users add column status  varchar(200);
alter table users add column institution varchar(400);
alter table users add column country varchar(400);
alter table users add column description varchar(5000);
alter table users add column account_creation_date ramadda.datetime;

#if SKIP
users:
     properties varchar(10000),

type_convertible:
convert_commands: varchar(10000:

media_3dmodel:
	annotations: varchar

type_document_ohms:
	change usage to ohms_usage


#endif




-----------------------------------------------------------------------
--- Updates for the different databases
-----------------------------------------------------------------------

#if  derby 
     alter table metadata alter  attr1 set data type ramadda.bigvarchar_orclob;
     alter table metadata alter  attr2 set data type ramadda.bigvarchar_orclob;
     alter table metadata alter  attr3 set data type ramadda.bigvarchar_orclob;
     alter table metadata alter  attr4 set data type ramadda.bigvarchar_orclob;     
---     alter table db_agendaitems add column new_description clob;
---     update db_agendaitems set new_description=description;
---     rename column db_agendaitems.description to old_description;
---     rename column db_agendaitems.new_description to description;
---     alter table db_agendaitems drop column old_description;



#if mysql
    alter table db_agendaitems modify column description text;	
    alter table metadata modify column attr1  text;
    alter table metadata modify column attr2  text;
    alter table metadata modify column attr3  text;
    alter table metadata modify column attr4  text;
#endif


#if TODO
    metadata:
                        attr1 varchar(32000),
                        attr2 varchar(32000),
                        attr3 varchar(32000),
                        attr4 varchar(32000),
#endif




#if postgres
--     alter table metadata modify column attr1 varchar(32000);
--     alter table metadata modify column attr2 varchar(32000);
--     alter table metadata modify column attr3 varchar(32000);
--     alter table metadata modify column attr4 varchar(32000);
#endif




-----------------------------------------------------------------------
--- roles users have
-----------------------------------------------------------------------
CREATE TABLE  userroles (
        user_id varchar(200),
        role varchar(200));


-----------------------------------------------------------------------
--- user's favorites
-----------------------------------------------------------------------
CREATE TABLE  favorites (
        id varchar(200),
        user_id varchar(200),
        entry_id varchar(200),
        name varchar(1000),
	category varchar(1000));

CREATE INDEX FAVORITES_INDEX_USER_ID ON favorites (USER_ID);

-----------------------------------------------------------------------
--- tracks logins
-----------------------------------------------------------------------
CREATE TABLE  user_activity (
	user_id  varchar(200),
	date ramadda.datetime, 
        what  varchar(100),  
        extra  varchar(1000),  
        ipaddress  varchar(400));

CREATE INDEX USER_ACTIVITY_INDEX_USER_ID ON user_activity (USER_ID);


-----------------------------------------------------------------------
--- tracks session
-----------------------------------------------------------------------
CREATE TABLE  sessions (
	session_id varchar(200),
	user_id  varchar(200),
	create_date ramadda.datetime, 
        last_active_date  ramadda.datetime,
        extra  varchar(10000));  


CREATE INDEX SESSIONS_INDEX_USER_ID ON sessions (USER_ID);


-----------------------------------------------------------------------
--- entry monitors
-----------------------------------------------------------------------
CREATE TABLE  monitors (
	monitor_id varchar(200),
        name       varchar(500),
	user_id  varchar(200),
	from_date ramadda.datetime, 
        to_date  ramadda.datetime,
        encoded_object  ramadda.clob);  


-----------------------------------------------------------------------
---  permissions on entries
-----------------------------------------------------------------------
CREATE TABLE  permissions (
       	entry_id varchar(200),
	action varchar(200),
        role varchar(200),
	data_policy varchar(400));

alter table permissions add column data_policy varchar(400);

-----------------------------------------------------------------------
--- the harvesters. content is the xml they encode/decode to store state
-----------------------------------------------------------------------
CREATE TABLE  harvesters (
       	      id varchar(200),
              class varchar(500),
              content varchar(10000));




-----------------------------------------------------------------------
--- global properties
-----------------------------------------------------------------------

CREATE TABLE  globals (name varchar(500),
                       value varchar(10000));








-----------------------------------------------------------------------
--- for storing the list of servers when acting as a registry
-----------------------------------------------------------------------
CREATE TABLE  remoteservers (
        url varchar(1000),
        title varchar(1000),
        description varchar(10000),
        email varchar(200),
	isregistry int,
	enabled int,
	live int,	
	searchroot varchar(1000),
	slug varchar(200));

ALTER table remoteservers add column  enabled int;
ALTER table remoteservers add column  live int;
ALTER table remoteservers add column  searchroot varchar(1000);
ALTER table remoteservers add column  slug varchar(200);


CREATE TABLE  localrepositories (
        id    varchar(200),
        email varchar(200),
        status varchar(200));



-----------------------------------------------------------------------
--- just here so ramadda knows if the db has been created
-----------------------------------------------------------------------
CREATE TABLE  dummy (name varchar(500));


-----------------------------------------------------------------------
--- holds information about the point databases
-----------------------------------------------------------------------
CREATE TABLE  pointdatametadata (
       tablename varchar(1000),
       columnname varchar(1000),
       columnnumber int,
       shortname varchar(1000),
       longname varchar(1000),
       unit varchar(100),
       vartype varchar(100));



CREATE TABLE  jobinfos (id varchar(200),
                        entry_id varchar(200),
                        user_id varchar(200),
                        date ramadda.datetime,
                        type varchar(200),         
                        job_info_blob ramadda.bigclob);
