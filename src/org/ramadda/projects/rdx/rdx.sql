-----------------------------------------------------------------------
--- The pending event notifications
-----------------------------------------------------------------------
CREATE TABLE rdx_notifications (entry_id varchar(200),
                  		   event_type varchar(200),
				   date ramadda.datetime); 

--- alter table rdx_notifications add column description varchar(5000);

CREATE TABLE rdx_test_instrument_status (
       instrument_id varchar(200),
       type varchar(200),
       last_network_connection ramadda.datetime,
       last_data_time ramadda.datetime,
       network_is_up int,
       data_down int
);

