All  of the specimen type definitions-
type_archive_botanical_specimen.xml
type_archive_geologic_specimen.xml
type_archive_herpetology_specimen.xml
type_archive_ichthyology_specimen.xml
type_archive_mammalogy_specimen.xml
type_archive_ornithology_specimen.xml
type_archive_paleontology_specimen.xml
type_archive_site.xml
type_archive_specimen.xml
type_archive_taxonomy.xml

were created with RAMADDA's Entry Type definition facility. The set of entries that
hold these type definitions are on github-
https://github.com/geodesystems/ramadda/blob/master/development/types/Archive_Types.zip

To make a change grab the .zip file and import it into your RAMADDA. Each of the Folders
has a Create Entry Type link in the entry popup menu. There is help available at
https://ramadda.org/repository/userguide/entrytypes.html#create_entry_type_form


The columns for each entry type are in a text file at-
https://github.com/geodesystems/ramadda/blob/master/development/types/archive_types.txt

To make a change it is best to edit the archive_types.txt
Then Clear  out the columns in the entry type form
Copy the column definitions from the text file
Do a Bulk Upload in the form and paste the columns
Then hit Download Type
Move the downloaded type_....xml file into this directory and build the plugin

