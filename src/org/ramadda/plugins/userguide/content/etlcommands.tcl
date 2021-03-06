etlcat {Slice and Dice}
etl {-columns} {Select columns} {Only include the given columns}  {{columns} {Column indices, one per line.<br>Can include ranges, e.g. 0-5}} 
etl {-notcolumns} {Deselect columns} {Don't include given columns}  {{columns} {Column indices, one per line.<br>Can include ranges, e.g. 0-5}} 
etl {-delete} {Delete columns} {Remove the columns}  {{columns} {Column indices, one per line.<br>Can include ranges, e.g. 0-5}} 
etl {-cut} {Drop rows} {}  {{rows} {One or more rows. -1 to the end. e.g., 0-3,5,10,-1}} 
etl {-include} {Include rows} {Only include specified rows}  {{rows} {one or more rows, -1 to the end}} 
etl {-skip} {Skip} {Skip number of rows}  {{rows} {How many rows to skip}} 
etl {-copy} {Copy column} {}  {{column} {}}  {{name} {}} 
etl {-insert} {Insert column} {Insert new column values}  {{column} {Column to insert after}}  {{values} {Single value or comma separated for multiple rows}} 
etl {-concat} {Concat} {Create a new column from the given columns}  {{columns} {Column indices, one per line.<br>Can include ranges, e.g. 0-5}}  {{delimiter} {}} 
etl {-split} {Split} {Split the column}  {{column} {}}  {{delimiter} {What to split on}}  {{names} {Comma separated new column names}} 
etl {-splat} {Splat} {Create a new column from the values in the given column}  {{key col} {}}  {{column} {}}  {{delimiter} {}}  {{name} {new column name}} 
etl {-shift} {Shift columns} {Shift columns over by count for given rows}  {{rows} {Rows to apply to}}  {{column} {Column to start at}}  {{count} {}} 
etl {-addcell} {Add cell} {Add a new cell at row/column}  {{row} {}}  {{column} {}}  {{value} {}} 
etl {-deletecell} {Delete cell} {Delete cell at row/column}  {{row} {}}  {{column} {}} 
etl {-mergerows} {Merge rows} {}  {{rows} {2 or more rows}}  {{delimiter} {}}  {{close} {}} 
etl {-rowop} {Row Operator} {Apply an operator to columns and merge rows}  {{keys} {Key columns}}  {{values} {Value columns}}  {{operator} {Operator}} 
etl {-rotate} {Rotate} {Rotate the data} 
etl {-flip} {Flip} {Reverse the order of the rows except the header} 
etl {-unfurl} {Unfurl} {Make columns from data values}  {{column} {column to get new column header#}}  {{value columns} {Columns to get values from}}  {{unique column} {The unique value, e.g. date}}  {{other columns} {Other columns to include}} 
etl {-furl} {Furl} {Use values in header to make new row}  {{columns} {Column indices, one per line.<br>Can include ranges, e.g. 0-5}}  {{header label} {}}  {{value label} {}} 
etl {-explode} {Explode} {Make separate files based on value of column}  {{column} {}} 
etl {-join} {Join} {Join the 2 files together}  {{key columns} {}}  {{value_columns} {value columns}}  {{file} {File to join with}}  {{source_columns} {source key columns}} 
etlcat {Filter}
etl {-start} {Start} {Start at pattern in source file}  {{start pattern} {}} 
etl {-stop} {Stop} {End at pattern in source file}  {{stop pattern} {}} 
etl {-rawlines} {Rawlines} {}  {{lines} {How many lines to pass through unprocesed}} 
etl {-min} {Min} {Only pass thorough lines that have at least this number of columns}  {{min # columns} {}} 
etl {-max} {Max} {Only pass through lines that have no more than this number of columns}  {{max # columns} {}} 
etl {-pattern} {Pattern} {Pass through rows that match the pattern}  {{column} {}}  {{pattern} {}} 
etl {-notpattern} {Notpattern} {Pass through rows that don't match the pattern}  {{column} {}}  {{pattern} {}} 
etl {-unique} {Unique} {Pass through unique values}  {{columns} {Column indices, one per line.<br>Can include ranges, e.g. 0-5}} 
etl {-dups} {Duplicate values} {Pass through duplicate values}  {{columns} {Column indices, one per line.<br>Can include ranges, e.g. 0-5}} 
etl {-maxvalue} {Max value} {}  {{key column} {}}  {{value column} {}} 
etl {-eq} {Equals} {Extract rows that pass the expression}  {{column} {}}  {{value} {}} 
etl {-ne} {Not equals} {Extract rows that pass the expression}  {{column} {}}  {{value} {}} 
etl {-gt} {Greater than} {Extract rows that pass the expression}  {{column} {}}  {{value} {}} 
etl {-ge} {Greater than/equals} {Extract rows that pass the expression}  {{column} {}}  {{value} {}} 
etl {-lt} {Less than} {Extract rows that pass the expression}  {{column} {}}  {{value} {}} 
etl {-le} {Less than/equals} {Extract rows that pass the expression}  {{column} {}}  {{value} {}} 
etl {-groupfilter} {Group filter} {One row in each group has to match}  {{column} {}}  {{value_column} {Value column}}  {{operator} {}}  {{value} {}} 
etl {-before} {Before date} {}  {{column} {}}  {{format} {}}  {{date} {}}  {{format2} {}} 
etl {-after} {After date} {}  {{column} {}}  {{format} {}}  {{date} {}}  {{format2} {}} 
etl {-latest} {Latest date} {}  {{columns} {Key columns}}  {{column} {Date column}}  {{format} {}} 
etl {-countvalue} {Max unique values} {No more than count unique values}  {{column} {}}  {{count} {}} 
etl {-decimate} {Decimate} {only include every <skip factor> row}  {{rows} {# of start rows to include}}  {{skip} {skip factor}} 
etl {-skipline} {Skipline} {Skip any line that matches the pattern}  {{pattern} {}} 
etlcat {Change Values}
etl {-change} {Change} {Change columns}  {{columns} {Column indices, one per line.<br>Can include ranges, e.g. 0-5}}  {{pattern} {}}  {{substitution string} {use $1, $2, etc for pattern (...) matches}} 
etl {-changerow} {Changerow} {Change the values in the row/cols}  {{rows} {Row indices, one per line.<br>Can include ranges, e.g. 0-5}}  {{columns} {Column indices, one per line.<br>Can include ranges, e.g. 0-5}}  {{pattern} {}}  {{substitution string} {}} 
etl {-set} {Set} {Write the value into the cells}  {{columns} {Column indices, one per line.<br>Can include ranges, e.g. 0-5}}  {{rows} {Row indices, one per line.<br>Can include ranges, e.g. 0-5}}  {{value} {}} 
etl {-macro} {Macro} {Look for the pattern in the header and apply the template to make a new column, template: '{1} {2} ...', use 'none' for column name for no header}  {{pattern} {}}  {{template} {}}  {{column label} {}} 
etl {-setcol} {Setcol} {Write the value into the write col for rows that match the pattern}  {{column} {match col #}}  {{pattern} {}}  {{write column} {}}  {{value} {}} 
etl {-priorprefix} {Priorprefix} {Append prefix from the previous element to rows that match pattern}  {{column} {}}  {{pattern} {}}  {{delimiter} {}} 
etl {-case} {Case} {Change case of column}  {{type} {}}  {{column} {}} 
etl {-width} {Width} {Limit the string size of the columns}  {{columns} {Column indices, one per line.<br>Can include ranges, e.g. 0-5}}  {{size} {}} 
etl {-prepend} {Prepend} {Add the text to the beginning of the file. use _nl_ to insert newlines}  {{text} {}} 
etl {-pad} {Pad} {Add or remove columns to achieve the count}  {{count} {}}  {{pad string} {}} 
etl {-prefix} {Prefix} {Add prefix to column}  {{column} {}}  {{prefix} {}} 
etl {-suffix} {Suffix} {Add suffix to column}  {{column} {}}  {{suffix} {}} 
etl {-js} {Js} {Define Javascript (e.g., functions) to use later in the -func call}  {{javascript} {}} 
etl {-func} {Func} {Apply the javascript function. Use _colname or _col#}  {{names} {New column names}}  {{javascript} {javascript expression}} 
etl {-endswith} {Endswith} {Ensure that each column ends with the string}  {{column} {}}  {{string} {}} 
etl {-trim} {Trim} {Trim the string values}  {{columns} {Column indices, one per line.<br>Can include ranges, e.g. 0-5}} 
etl {-truncate} {Truncate} {}  {{column} {}}  {{max length} {}}  {{suffix} {}} 
etl {-extract} {Extract} {Extract text from column and make a new column}  {{column} {}}  {{pattern} {}}  {{replace with} {use 'none' for no replacement}}  {{new column name} {}} 
etl {-map} {Map} {Change values in column to new values}  {{column} {}}  {{new columns name} {}}  {{value newvalue ...} {}} 
etl {-combine} {Combine} {Combine columns with the delimiter. deleting columns}  {{column} {}}  {{delimiter} {}}  {{new column name} {}} 
etl {-combineinplace} {Combine in place} {Combine columns with the delimiter}  {{column} {}}  {{delimiter} {}}  {{new column name} {}} 
etl {-format} {Format} {}  {{columns} {Column indices, one per line.<br>Can include ranges, e.g. 0-5}}  {{format} {Decimal format  e.g. '##0.00'}} 
etl {-denormalize} {Denormalize} {Read the id,value from file and substitute the value in the dest file col idx}  {{file} {From csv file}}  {{from id idx} {}}  {{from value idx} {}}  {{to idx} {}}  {{new col name} {}}  {{mode replace add} {}} 
etl {-break} {Break} {Break apart column values and make new rows}  {{label1} {}}  {{label2} {}}  {{columns} {Column indices, one per line.<br>Can include ranges, e.g. 0-5}} 
etlcat {Add Values}
etl {-md} {Md} {Make a message digest of the column values}  {{columns} {Column indices, one per line.<br>Can include ranges, e.g. 0-5}}  {{type} {}} 
etl {-uuid} {Uuid} {Add a UUID field} 
etl {-number} {Number} {Add 1,2,3... as column} 
etl {-letter} {Letter} {Add 'A','B', ... as column} 
etlcat {Lookup}
etl {-wikidesc} {Wikidesc} {Add a description from wikipedia}  {{column} {}}  {{suffix} {}} 
etl {-image} {Image} {Search for an image}  {{column} {}}  {{suffix} {}} 
etl {-imagefill} {Imagefill} {Search for an image with the query column text if the given image column is blank. Add the given suffix to the search. }  {{querycolumn} {}}  {{suffix} {}}  {{imagecolumn} {}} 
etl {-gender} {Gender} {Figure out the gender of the name in the column}  {{column} {}} 
etlcat {Dates}
etl {-convertdate} {Convert date} {}  {{column} {}}  {{sourceformat} {Source format}}  {{destformat} {Target format}} 
etl {-extractdate} {Extract date} {}  {{date column} {}}  {{format} {Date format}}  {{timezone} {}}  {{what} {What to extract}} 
etl {-formatdate} {Format date} {}  {{columns} {Column indices, one per line.<br>Can include ranges, e.g. 0-5}}  {{intial date format} {}}  {{target date format} {}} 
etlcat {Numeric}
etl {-scale} {Scale} {Set value={value+delta1}*scale+delta2}  {{column} {}}  {{delta1} {}}  {{scale} {}}  {{delta2} {}} 
etl {-generate} {Generate} {Add row values}  {{label} {}}  {{start} {}}  {{step} {}} 
etl {-decimals} {Decimals} {}  {{column} {}}  {{how many decimals to round to} {}} 
etl {-delta} {Delta} {Add column that is the delta from the previous step}  {{key columns} {}}  {{columns} {Column indices, one per line.<br>Can include ranges, e.g. 0-5}} 
etl {-operator} {Operator} {Apply the operator to the given columns and create new one}  {{columns} {Column indices, one per line.<br>Can include ranges, e.g. 0-5}}  {{new col name} {}}  {{operator +,-,*,/} {}} 
etl {-round} {Round} {round the values}  {{columns} {Column indices, one per line.<br>Can include ranges, e.g. 0-5}} 
etl {-sum} {Sum} {Sum values keying on name column value. If no value columns specified then do a count}  {{key columns} {}}  {{value columns} {}}  {{carry over columns} {}} 
etl {-percent} {Percent} {}  {{columns to add} {}} 
etl {-increase} {Increase} {Calculate percent increase}  {{column} {}}  {{how far back} {}} 
etl {-average} {Average} {Calculate a moving average}  {{columns} {Column indices, one per line.<br>Can include ranges, e.g. 0-5}}  {{period} {}}  {{label} {}} 
etlcat {Geocode}
etl {-geocode} {Geocode} {}  {{columns} {Column indices, one per line.<br>Can include ranges, e.g. 0-5}}  {{prefix} {e.g., state: or county:}}  {{suffix} {}} 
etl {-geocodeaddressdb} {Geocode address for DB} {}  {{columns} {Column indices, one per line.<br>Can include ranges, e.g. 0-5}}  {{prefix} {}}  {{suffix} {}} 
etl {-geocodejoin} {Geocode with file} {Geocode with file}  {{column} {}}  {{csv file} {File to get lat/lon from}}  {{name idx} {}}  {{lat idx} {}}  {{lon idx} {}} 
etl {-statename} {Statename} {Add state name from state ID}  {{column} {}} 
etl {-mercator} {Mercator} {Convert x/y to lon/lat}  {{columns} {Column indices, one per line.<br>Can include ranges, e.g. 0-5}} 
etl {-region} {Region} {Add the state's region}  {{columns} {Column indices, one per line.<br>Can include ranges, e.g. 0-5}} 
etl {-population} {Population} {Add in population from address}  {{columns} {Column indices, one per line.<br>Can include ranges, e.g. 0-5}}  {{prefix} {e.g., state: or county:}}  {{suffix} {}} 
etlcat {Other Commands}
etl {-sort} {Sort} {}  {{column} {Column to sort on}} 
etl {-descsort} {Descsort} {}  {{column} {Column to descending sort on}} 
etl {-count} {Count} {Show count} 
etl {-maxrows} {Maxrows} {}  {{Max rows to print} {}} 
etl {-changeline} {Change line} {Change the line}  {{from} {}}  {{to} {}} 
etl {-changeraw} {Change input} {Change input text}  {{from} {}}  {{to} {}} 
etl {-crop} {Crop string} {Crop last part of string after any of the patterns}  {{columns} {Column indices, one per line.<br>Can include ranges, e.g. 0-5}}  {{pattern1,pattern2} {}} 
etl {-strict} {Strict} {Be strict on columns. any rows that are not the size of the other rows are dropped} 
etl {-flag} {Flag} {Be strict on columns. any rows that are not the size of the other rows are shown} 
etl {-verify} {Verify} {Throw error if a row has a different number of columns}  {{# columns} {}} 
etl {-prop} {Prop} {Set a property}  {{property} {}}  {{value} {start, end, etc}} 
etl {-comment} {Comment} {}  {{string} {}} 
etl {-verify} {Verify} {Verify that all of the rows have the same # of columns} 
etlcat {Input}
etl {-delimiter} {Delimiter} {Specify a delimiter}  {{delimiter} {Use 'space' for space, 'tab' for tab}} 
etl {-tab} {Tab} {Use tabs} 
etl {-widths} {Widths} {Columns are fixed widths}  {{widths} {w1,w2,...,wN}} 
etl {-header} {Header} {Raw header}  {{header} {Column names}} 
etl {-html} {Html} {Parse the table in the input html file}  {{skip} {Number of tables to skip}}  {{pattern} {Pattern to skip to}}  {{properties} {Other attributes - <br>&nbsp;&nbsp;removeEntity false removePattern pattern}} 
etl {-htmlpattern} {Extract from html} {Parse the input html file}  {{columns} {Column names}}  {{startPattern} {}}  {{endPattern} {}}  {{pattern} {Row pattern. Use (...) to match columns}} 
etl {-json} {Json} {Parse the input as json}  {{arrayPath} {Path to the array e.g., obj1.arr[2].obj2}}  {{objectPaths} {One or more paths to the objects e.g. geometry,features}} 
etl {-xml} {Xml} {Parse the input as xml}  {{path} {Path to the elements}} 
etl {-text} {Text} {Extract rows from the text}  {{comma separated header} {}}  {{chunk pattern} {}}  {{token pattern} {}} 
etl {-text2} {Text2} {Extract rows from the text}  {{comma separated header} {}}  {{chunk pattern} {}}  {{token pattern} {}} 
etl {-text3} {Text3} {Extract rows from the text}  {{comma separated header} {}}  {{token pattern} {}} 
etl {-tokenize} {Tokenize} {Tokenize the input from the pattern}  {{header} {header1,header2...}}  {{pattern} {}} 
etl {-sql} {Sql} {Connect to the given database}  {{db} {The database id (defined in the environment)}}  {{table} {The table to select from}}  {{properties} {'columns' c1,c2,...  'where' c1,<|>|<>|like|notlike;...}} 
etl {-prune} {Prune} {Prune out the first N bytes}  {{bytes} {Number of leading bytes to remove}} 
etlcat {Output}
etl {-print} {Print} {Output the rows} 
etl {-template} {Template} {Apply the template to make the output}  {{prefix} {}}  {{template} {Use ${0},${1}, etc for values}}  {{delimiter} {Output between rows}}  {{suffix} {}} 
etl {-raw} {Raw} {Print the file raw} 
etl {-record} {Record} {Print records} 
etl {-printheader} {Printheader} {Print the first line} 
etl {-pointheader} {Pointheader} {Generate the RAMADDA point properties} 
etl {-addheader} {Add header} {Add the RAMADDA point properties}  {{properties} {name1 value1 ... nameN valueN}} 
etl {-deheader} {Remove the  header} {Strip off the point header} 
etl {-db} {Db} {Generate the RAMADDA db xml from the header}  {{props} {Name value pairs:
		table.id <new id> table.name <new name> table.cansearch <true|false> table.canlist <true|false> table.icon <icon, e.g., /db/database.png>
		<column name>.id <new id for column> <column name>.label <new label>
		<column name>.type <string|enumeration|double|int|date>
		<column name>.format <yyyy MM dd HH mm ss format for dates>
		<column name>.canlist <true|false> <column name>.cansearch <true|false>
		install <true|false install the new db table>
		nukedb <true|false careful! this deletes any prior created dbs}} 
etl {-toxml} {Toxml} {Generate XML}  {{tag} {}} 
etl {-run} {Run} {}  {{Name of process directory} {}} 
etl {-cat} {Cat} {One or more csv files}  {{*.csv} {}} 
etl {-script} {Script} {Generate the script to call} 
etl {-args} {Args} {Generate the CSV file commands} 
etl {-args2} {Args2} {Print out the args} 
