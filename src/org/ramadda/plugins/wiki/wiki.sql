-----------------------------------------------------------------------
--- Holds changes to wiki pages
-----------------------------------------------------------------------
CREATE TABLE wikipagehistory (entry_id varchar(200),
 		          user_id varchar(200),
			  date ramadda.datetime, 
			  description varchar(2000),
			  wikitext ramadda.clob);


CREATE INDEX WIKIPAGEHISTORY_INDEX_ENTRY_ID ON wikipagehistory (ENTRY_ID);
