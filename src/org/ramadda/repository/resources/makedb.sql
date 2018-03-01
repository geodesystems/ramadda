

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
                   description varchar(15000),
                   parent_group_id varchar(200),
   		   user_id varchar(200),
	           resource varchar(500),	           
                   resource_type varchar(200),
                   md5           varchar(32),
                   filesize ramadda.bigint,
		   datatype varchar(200),
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


---- ALTER table entries add column changedate ramadda.datetime;
--- for mysql
--- alter table entries modify column resource varchar(500);
--- for derby
---alter table entries alter column resource set data type varchar(500);



CREATE INDEX ENTRIES_INDEX_ID ON entries (ID);
CREATE INDEX ENTRIES_INDEX_RESOURCE ON entries (RESOURCE);
CREATE INDEX ENTRIES_INDEX_DATATYPE ON entries (DATATYPE);
CREATE INDEX ENTRIES_INDEX_PARENT_GROUP_ID ON entries (PARENT_GROUP_ID);
--- CREATE INDEX ENTRIES_INDEX_TOP_GROUP_ID ON entries (TOP_GROUP_ID);
CREATE INDEX ENTRIES_INDEX_TYPE ON entries (TYPE);
CREATE INDEX ENTRIES_INDEX_USER_ID ON entries (USER_ID);
CREATE INDEX ENTRIES_INDEX_FROMDATE ON entries (FROMDATE);
CREATE INDEX ENTRIES_INDEX_TODATE ON entries (TODATE);


CREATE TABLE ancestors (id varchar(200),
	                ancestor_id varchar(200));

CREATE INDEX ancestors_index_id ON ancestors (id);
CREATE INDEX ancestors_index_ancestor_id ON ancestors (ancestor_id);




-----------------------------------------------------------------------
---Holds metadata 
---Entries can have any number of metadata items
---The MetadataHandler classes handle the semantics. 
-----------------------------------------------------------------------
CREATE TABLE  metadata (id varchar(200),
			entry_id varchar(200),
                        type varchar(200),
                	inherited int,
                        attr1 varchar(5000),
                        attr2 varchar(5000),
                        attr3 varchar(5000),
                        attr4 varchar(5000),
		        extra ramadda.bigclob);


CREATE INDEX METADATA_INDEX_ID ON metadata (ID);
CREATE INDEX METADATA_INDEX_ENTRYID ON metadata (ENTRY_ID);
CREATE INDEX METADATA_INDEX_TYPE ON metadata (TYPE);
---- CREATE INDEX METADATA_INDEX_ATTR1 ON metadata (ATTR1);

CREATE TABLE  metadata_test1 (id varchar(200),
			entry_id varchar(200),
                        type varchar(200),
                	inherited int,
                        attr1 varchar(6000),
                        attr2 varchar(6000),
                        attr3 varchar(6000),
                        attr4 varchar(6000),
		        extra ramadda.bigclob);


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


-----------------------------------------------------------------------
--- users 
-----------------------------------------------------------------------
CREATE TABLE  users (id varchar(200),
                     name  varchar(200),
                     email varchar(200),
                     question  varchar(200),
                     answer  varchar(200),  
                     password  varchar(200),
                     description varchar(5000),
		     admin int,
		     language varchar(50),
		     template varchar(200),
                     isguest int,
                     properties varchar(10000));

alter table users add column description varchar(5000);


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
        role varchar(200));




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
CREATE TABLE  serverregistry (
        url varchar(1000),
        title varchar(1000),
        description varchar(10000),
        email varchar(200),
	isregistry int);



CREATE TABLE  remoteservers (
        url varchar(1000),
        title varchar(1000),
        description varchar(10000),
        email varchar(200),
	isregistry int,
        selected int);




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


---- CREATE INDEX POINTDATAMETADATA_INDEX_TABLENAME ON pointdatametadata (TABLENAME);



CREATE TABLE  jobinfos (id varchar(200),
                        entry_id varchar(200),
                        user_id varchar(200),
                        date ramadda.datetime,
                        type varchar(200),         
                        job_info_blob ramadda.bigclob);
