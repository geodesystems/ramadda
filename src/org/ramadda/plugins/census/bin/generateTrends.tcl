set ::statesFile  /Users/jeffmc/source/ramadda-code/src/org/ramadda/repository/resources/geo/states.txt
foreach line  [split [read [open $::statesFile r]] "\n"] {
    set line [string trim $line]
    if {$line == ""}  continue;
    if {[regexp {^#.*} $line]} continue;
    foreach {abbr name id lat lon} [split $line "\t"] break
    set  ::states($abbr) $name
    set  ::states([string tolower $abbr]) $name
}


proc msaToFips {msa f1 f2} {
     set ::msa($msa) "$f1$f2"
}

proc countyToGeo {c lat lon} {
    set ::fips($c) [list $lat $lon]
}

source msatofips.txt
source countytogeo.txt
set ::type "type_urbaninstitute_employment"

set ::xml "<entries>\n"
set ::cnt 0



proc cdata {tag s} {
    set c "<$tag><!\[CDATA\["
    append c $s
    append c  "]\]\></$tag>\n"
    set c
}

proc msa {id name st} {
    incr ::cnt
    regsub -all {(^[^-]+)-.*} $st \\1 st
    set st [string trim $st]
#    if {$::cnt>5} return;
     if {![info exists ::msa($id)]} {
          puts  stderr    "no msa: $id"
         return
#        puts      "msa: $id = $::msa($id)"
     }
    if {![info exists ::seen($st)]} {
        set ::seen($st) 1
        append ::xml "<entry type=\"group\" name=\"MetroTrends Reports - $::states($st)\" id=\"$st\" >\n"
##        append ::xml  [cdata description $desc]
        append ::xml "</entry>\n"

    }
    set fips $::msa($id)

     if {![info exists ::fips($fips)]} {
          puts stderr     "no fips: $fips"
         return
     }

    foreach {lat lon} $::fips($fips) break

    set name [string trim $name] 
    set st [string trim $st]
    set name "MetroTrends Report - $name - $st"
#unemployment - http://datatool.urban.org/charts/metrodata/metrodataq_test.cfm?y1=1990&y2=2015&m1=1&m2=12&metroidlist=10500&statidlist=1,2&format=csv
#house price - http://datatool.urban.org/charts/metrodata/metrodataq_test.cfm?y1=2000&y2=2013&m1=1&m2=4&metroidlist=10500&statidlist=3&format=csv
    set url {http://datatool.urban.org/charts/metrodata/metrodataq_test.cfm?y1=2000&y2=${now.year}&m1=1&m2=${now.month}&metroidlist=%msa%&statidlist=23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39&format=csv}
    regsub -all "%msa%" $url $id url
    set filename trends_$id.csv
    ##    puts "curl -o data/${filename} \"$url\""
    append ::xml  "<entry name=\"$name\" latitude=\"$lat\" longitude=\"$lon\" type=\"$::type\" parent=\"$st\">\n"
    set desc {Data courtesy of the Urban Institute <a href="http://www.metrotrends.org/data-download-new.cfm">Metro Trends report</a>.}


    append ::xml  [cdata description $desc]
    append ::xml  [cdata region $st]
    append ::xml  [cdata url $url]
#    append ::xml  [cdata file $filename]
    append ::xml  "</entry>"
}



msa 10180 {Abilene} { TX }
msa 10420 {Akron} { OH }
msa 10500 {Albany} { GA }
msa 10580 {Albany-Schenectady-Troy} { NY }
msa 10740 {Albuquerque} { NM }
msa 10780 {Alexandria} { LA }
msa 10900 {Allentown-Bethlehem-Easton} { PA-NJ }
msa 11020 {Altoona} { PA }
msa 11100 {Amarillo} { TX }
msa 11180 {Ames} { IA }
msa 11260 {Anchorage} { AK }
msa 11300 {Anderson} { IN }
msa 11340 {Anderson} { SC }
msa 11460 {Ann Arbor} { MI }
msa 11500 {Anniston-Oxford} { AL }
msa 11540 {Appleton} { WI }
msa 11700 {Asheville} { NC }
msa 12020 {Athens-Clarke County} { GA }
msa 12060 {Atlanta-Sandy Springs-Marietta} { GA }
msa 12100 {Atlantic City-Hammonton} { NJ }
msa 12220 {Auburn-Opelika} { AL }
msa 12260 {Augusta-Richmond County} { GA-SC }
msa 12420 {Austin-Round Rock} { TX }
msa 12540 {Bakersfield} { CA }
msa 12580 {Baltimore-Towson} { MD }
msa 12620 {Bangor} { ME }
msa 12700 {Barnstable Town} { MA }
msa 12940 {Baton Rouge} { LA }
msa 12980 {Battle Creek} { MI }
msa 13020 {Bay City} { MI }
msa 13140 {Beaumont-Port Arthur} { TX }
msa 13380 {Bellingham} { WA }
msa 13460 {Bend} { OR }
msa 13740 {Billings} { MT }
msa 13780 {Binghamton} { NY }
msa 13820 {Birmingham-Hoover} { AL }
msa 13900 {Bismarck} { ND }
msa 13980 {Blacksburg-Christiansburg-Radford} { VA }
msa 14020 {Bloomington} { IN }
msa 14060 {Bloomington-Normal} { IL }
msa 14260 {Boise City-Nampa} { ID }
msa 14460 {Boston-Cambridge-Quincy} { MA-NH }
msa 14500 {Boulder} { CO }
msa 14540 {Bowling Green} { KY }
msa 14600 {Bradenton-Sarasota-Venice} { FL }
msa 14740 {Bremerton-Silverdale} { WA }
msa 14860 {Bridgeport-Stamford-Norwalk} { CT }
msa 15180 {Brownsville-Harlingen} { TX }
msa 15260 {Brunswick} { GA }
msa 15380 {Buffalo-Niagara Falls} { NY }
msa 15500 {Burlington} { NC }
msa 15540 {Burlington-South Burlington} { VT }
msa 15940 {Canton-Massillon} { OH }
msa 15980 {Cape Coral-Fort Myers} { FL }
msa 16020 {Cape Girardeau-Jackson} { MO-IL }
msa 16180 {Carson City} { NV }
msa 16220 {Casper} { WY }
msa 16300 {Cedar Rapids} { IA }
msa 16580 {Champaign-Urbana} { IL }
msa 16620 {Charleston} { WV }
msa 16700 {Charleston-North Charleston-Summerville} { SC }
msa 16740 {Charlotte-Gastonia-Concord} { NC-SC }
msa 16820 {Charlottesville} { VA }
msa 16860 {Chattanooga} { TN-GA }
msa 16940 {Cheyenne} { WY }
msa 16980 {Chicago-Naperville-Joliet} { IL-IN-WI }
msa 17020 {Chico} { CA }
msa 17140 {Cincinnati-Middletown} { OH-KY-IN }
msa 17300 {Clarksville} { TN-KY }
msa 17420 {Cleveland} { TN }
msa 17460 {Cleveland-Elyria-Mentor} { OH }
msa 17660 {Coeur d'Alene} { ID }
msa 17780 {College Station-Bryan} { TX }
msa 17820 {Colorado Springs} { CO }
msa 17860 {Columbia} { MO }
msa 17900 {Columbia} { SC }
msa 17980 {Columbus} { GA-AL }
msa 18020 {Columbus} { IN }
msa 18140 {Columbus} { OH }
msa 18580 {Corpus Christi} { TX }
msa 18700 {Corvallis} { OR }
msa 19060 {Cumberland} { MD-WV }
msa 19100 {Dallas-Fort Worth-Arlington} { TX }
msa 19140 {Dalton} { GA }
msa 19180 {Danville} { IL }
msa 19260 {Danville} { VA }
msa 19340 {Davenport-Moline-Rock Island} { IA-IL }
msa 19380 {Dayton} { OH }
msa 19460 {Decatur} { AL }
msa 19500 {Decatur} { IL }
msa 19660 {Deltona-Daytona Beach-Ormond Beach} { FL }
msa 19740 {Denver-Aurora-Broomfield} { CO }
msa 19780 {Des Moines-West Des Moines} { IA }
msa 19820 {Detroit-Warren-Livonia} { MI }
msa 20020 {Dothan} { AL }
msa 20100 {Dover} { DE }
msa 20220 {Dubuque} { IA }
msa 20260 {Duluth} { MN-WI }
msa 20500 {Durham-Chapel Hill} { NC }
msa 20740 {Eau Claire} { WI }
msa 20940 {El Centro} { CA }
msa 21060 {Elizabethtown} { KY }
msa 21140 {Elkhart-Goshen} { IN }
msa 21300 {Elmira} { NY }
msa 21340 {El Paso} { TX }
msa 21500 {Erie} { PA }
msa 21660 {Eugene-Springfield} { OR }
msa 21780 {Evansville} { IN-KY }
msa 21820 {Fairbanks} { AK }
msa 22020 {Fargo} { ND-MN }
msa 22140 {Farmington} { NM }
msa 22180 {Fayetteville} { NC }
msa 22220 {Fayetteville-Springdale-Rogers} { AR-MO }
msa 22380 {Flagstaff} { AZ }
msa 22420 {Flint} { MI }
msa 22500 {Florence} { SC }
msa 22520 {Florence-Muscle Shoals} { AL }
msa 22540 {Fond du Lac} { WI }
msa 22660 {Fort Collins-Loveland} { CO }
msa 22900 {Fort Smith} { AR-OK }
msa 23020 {Fort Walton Beach-Crestview-Destin} { FL }
msa 23060 {Fort Wayne} { IN }
msa 23420 {Fresno} { CA }
msa 23460 {Gadsden} { AL }
msa 23540 {Gainesville} { FL }
msa 23580 {Gainesville} { GA }
msa 24020 {Glens Falls} { NY }
msa 24140 {Goldsboro} { NC }
msa 24220 {Grand Forks} { ND-MN }
msa 24300 {Grand Junction} { CO }
msa 24340 {Grand Rapids-Wyoming} { MI }
msa 24500 {Great Falls} { MT }
msa 24540 {Greeley} { CO }
msa 24580 {Green Bay} { WI }
msa 24660 {Greensboro-High Point} { NC }
msa 24780 {Greenville} { NC }
msa 24860 {Greenville-Mauldin-Easley} { SC }
msa 25060 {Gulfport-Biloxi} { MS }
msa 25180 {Hagerstown-Martinsburg} { MD-WV }
msa 25260 {Hanford-Corcoran} { CA }
msa 25420 {Harrisburg-Carlisle} { PA }
msa 25500 {Harrisonburg} { VA }
msa 25540 {Hartford-West Hartford-East Hartford} { CT }
msa 25620 {Hattiesburg} { MS }
msa 25860 {Hickory-Lenoir-Morganton} { NC }
msa 25980 {Hinesville-Fort Stewart} { GA }
msa 26100 {Holland-Grand Haven} { MI }
msa 26180 {Honolulu} { HI }
msa 26300 {Hot Springs} { AR }
msa 26380 {Houma-Bayou Cane-Thibodaux} { LA }
msa 26420 {Houston-Sugar Land-Baytown} { TX }
msa 26580 {Huntington-Ashland} { WV-KY-OH }
msa 26620 {Huntsville} { AL }
msa 26820 {Idaho Falls} { ID }
msa 26900 {Indianapolis-Carmel} { IN }
msa 26980 {Iowa City} { IA }
msa 27060 {Ithaca} { NY }
msa 27100 {Jackson} { MI }
msa 27140 {Jackson} { MS }
msa 27180 {Jackson} { TN }
msa 27260 {Jacksonville} { FL }
msa 27340 {Jacksonville} { NC }
msa 27500 {Janesville} { WI }
msa 27620 {Jefferson City} { MO }
msa 27740 {Johnson City} { TN }
msa 27780 {Johnstown} { PA }
msa 27860 {Jonesboro} { AR }
msa 27900 {Joplin} { MO }
msa 28020 {Kalamazoo-Portage} { MI }
msa 28100 {Kankakee-Bradley} { IL }
msa 28140 {Kansas City} { MO-KS }
msa 28420 {Kennewick-Pasco-Richland} { WA }
msa 28660 {Killeen-Temple-Fort Hood} { TX }
msa 28700 {Kingsport-Bristol-Bristol} { TN-VA }
msa 28740 {Kingston} { NY }
msa 28940 {Knoxville} { TN }
msa 29020 {Kokomo} { IN }
msa 29100 {La Crosse} { WI-MN }
msa 29140 {Lafayette} { IN }
msa 29180 {Lafayette} { LA }
msa 29340 {Lake Charles} { LA }
msa 29420 {Lake Havasu City-Kingman} { AZ }
msa 29460 {Lakeland-Winter Haven} { FL }
msa 29540 {Lancaster} { PA }
msa 29620 {Lansing-East Lansing} { MI }
msa 29700 {Laredo} { TX }
msa 29740 {Las Cruces} { NM }
msa 29820 {Las Vegas-Paradise} { NV }
msa 29940 {Lawrence} { KS }
msa 30020 {Lawton} { OK }
msa 30140 {Lebanon} { PA }
msa 30300 {Lewiston} { ID-WA }
msa 30340 {Lewiston-Auburn} { ME }
msa 30460 {Lexington-Fayette} { KY }
msa 30620 {Lima} { OH }
msa 30700 {Lincoln} { NE }
msa 30780 {Little Rock-North Little Rock-Conway} { AR }
msa 30860 {Logan} { UT-MSA }
msa 30980 {Longview} { TX }
msa 31020 {Longview} { WA }
msa 31100 {Los Angeles-Long Beach-Santa Ana} { CA }
msa 31140 {Louisville/Jefferson County} { KY-IN }
msa 31180 {Lubbock} { TX }
msa 31340 {Lynchburg} { VA }
msa 31420 {Macon} { GA }
msa 31460 {Madera-Chowchilla} { CA }
msa 31540 {Madison} { WI }
msa 31700 {Manchester-Nashua} { NH }
msa 31740 {Manhattan} { KS }
msa 31860 {Mankato-North Mankato} { MN }
msa 31900 {Mansfield} { OH }
msa 32580 {McAllen-Edinburg-Mission} { TX }
msa 32780 {Medford} { OR }
msa 32820 {Memphis} { TN-MS-AR }
msa 32900 {Merced} { CA }
msa 33100 {Miami-Fort Lauderdale-Pompano Beach} { FL }
msa 33140 {Michigan City-La Porte} { IN }
msa 33260 {Midland} { TX }
msa 33340 {Milwaukee-Waukesha-West Allis} { WI }
msa 33460 {Minneapolis-St. Paul-Bloomington} { MN-WI }
msa 33540 {Missoula} { MT }
msa 33660 {Mobile} { AL }
msa 33700 {Modesto} { CA }
msa 33740 {Monroe} { LA }
msa 33780 {Monroe} { MI }
msa 33860 {Montgomery} { AL }
msa 34060 {Morgantown} { WV }
msa 34100 {Morristown} { TN }
msa 34580 {Mount Vernon-Anacortes} { WA }
msa 34620 {Muncie} { IN }
msa 34740 {Muskegon-Norton Shores} { MI }
msa 34820 {Myrtle Beach-North Myrtle Beach-Conway} { SC }
msa 34900 {Napa} { CA }
msa 34940 {Naples-Marco Island} { FL }
msa 34980 {Nashville-Davidson--Murfreesboro--Franklin} { TN }
msa 35300 {New Haven-Milford} { CT }
msa 35380 {New Orleans-Metairie-Kenner} { LA }
msa 35620 {New York-Northern New Jersey-Long Island} { NY-NJ-PA }
msa 35660 {Niles-Benton Harbor} { MI }
msa 35980 {Norwich-New London} { CT }
msa 36100 {Ocala} { FL }
msa 36140 {Ocean City} { NJ }
msa 36220 {Odessa} { TX }
msa 36260 {Ogden-Clearfield} { UT }
msa 36420 {Oklahoma City} { OK }
msa 36500 {Olympia} { WA }
msa 36540 {Omaha-Council Bluffs} { NE-IA }
msa 36740 {Orlando-Kissimmee} { FL }
msa 36780 {Oshkosh-Neenah} { WI }
msa 36980 {Owensboro} { KY }
msa 37100 {Oxnard-Thousand Oaks-Ventura} { CA }
msa 37340 {Palm Bay-Melbourne-Titusville} { FL }
msa 37380 {Palm Coast} { FL }
msa 37460 {Panama City-Lynn Haven-Panama City Beach} { FL }
msa 37620 {Parkersburg-Marietta-Vienna} { WV-OH }
msa 37700 {Pascagoula} { MS }
msa 37860 {Pensacola-Ferry Pass-Brent} { FL }
msa 37900 {Peoria} { IL }
msa 37980 {Philadelphia-Camden-Wilmington} { PA-NJ-DE-MD }
msa 38060 {Phoenix-Mesa-Scottsdale} { AZ }
msa 38220 {Pine Bluff} { AR }
msa 38300 {Pittsburgh} { PA }
msa 38340 {Pittsfield} { MA }
msa 38540 {Pocatello} { ID }
msa 38860 {Portland-South Portland-Biddeford} { ME }
msa 38900 {Portland-Vancouver-Beaverton} { OR-WA }
msa 38940 {Port St. Lucie} { FL }
msa 39100 {Poughkeepsie-Newburgh-Middletown} { NY }
msa 39140 {Prescott} { AZ }
msa 39300 {Providence-New Bedford-Fall River} { RI-MA }
msa 39340 {Provo-Orem} { UT }
msa 39380 {Pueblo} { CO }
msa 39460 {Punta Gorda} { FL }
msa 39540 {Racine} { WI }
msa 39580 {Raleigh-Cary} { NC }
msa 39660 {Rapmsa City} { SD }
msa 39740 {Reading} { PA }
msa 39820 {Redding} { CA }
msa 39900 {Reno-Sparks} { NV }
msa 40060 {Richmond} { VA }
msa 40140 {Riverside-San Bernardino-Ontario} { CA }
msa 40220 {Roanoke} { VA }
msa 40340 {Rochester} { MN }
msa 40380 {Rochester} { NY }
msa 40420 {Rockford} { IL }
msa 40580 {Rocky Mount} { NC }
msa 40660 {Rome} { GA }
msa 40900 {Sacramento--Arden-Arcade--Roseville} { CA }
msa 40980 {Saginaw-Saginaw Township North} { MI }
msa 41060 {St. Cloud} { MN }
msa 41100 {St. George} { UT }
msa 41140 {St. Joseph} { MO-KS }
msa 41180 {St. Louis} { MO-IL }
msa 41420 {Salem} { OR }
msa 41500 {Salinas} { CA }
msa 41540 {Salisbury} { MD }
msa 41620 {Salt Lake City} { UT }
msa 41660 {San Angelo} { TX }
msa 41700 {San Antonio} { TX }
msa 41740 {San Diego-Carlsbad-San Marcos} { CA }
msa 41780 {Sandusky} { OH }
msa 41860 {San Francisco-Oakland-Fremont} { CA }
msa 41940 {San Jose-Sunnyvale-Santa Clara} { CA }
msa 42020 {San Luis Obispo-Paso Robles} { CA }
msa 42060 {Santa Barbara-Santa Maria-Goleta} { CA }
msa 42100 {Santa Cruz-Watsonville} { CA }
msa 42140 {Santa Fe} { NM }
msa 42220 {Santa Rosa-Petaluma} { CA }
msa 42340 {Savannah} { GA }
msa 42540 {Scranton--Wilkes-Barre} { PA }
msa 42660 {Seattle-Tacoma-Bellevue} { WA }
msa 42680 {Sebastian-Vero Beach} { FL }
msa 43100 {Sheboygan} { WI }
msa 43300 {Sherman-Denison} { TX }
msa 43340 {Shreveport-Bossier City} { LA }
msa 43580 {Sioux City} { IA-NE-SD }
msa 43620 {Sioux Falls} { SD }
msa 43780 {South Bend-Mishawaka} { IN-MI }
msa 43900 {Spartanburg} { SC }
msa 44060 {Spokane} { WA }
msa 44100 {Springfield} { IL }
msa 44140 {Springfield} { MA }
msa 44180 {Springfield} { MO }
msa 44220 {Springfield} { OH }
msa 44300 {State College} { PA }
msa 44700 {Stockton} { CA }
msa 44940 {Sumter} { SC }
msa 45060 {Syracuse} { NY }
msa 45220 {Tallahassee} { FL }
msa 45300 {Tampa-St. Petersburg-Clearwater} { FL }
msa 45460 {Terre Haute} { IN }
msa 45780 {Toledo} { OH }
msa 45820 {Topeka} { KS }
msa 45940 {Trenton-Ewing} { NJ }
msa 46060 {Tucson} { AZ }
msa 46140 {Tulsa} { OK }
msa 46220 {Tuscaloosa} { AL }
msa 46340 {Tyler} { TX }
msa 46540 {Utica-Rome} { NY }
msa 46660 {Valdosta} { GA }
msa 46700 {Vallejo-Fairfield} { CA }
msa 47020 {Victoria} { TX }
msa 47220 {Vineland-Millville-Bridgeton} { NJ }
msa 47260 {Virginia Beach-Norfolk-Newport News} { VA-NC }
msa 47300 {Visalia-Porterville} { CA }
msa 47380 {Waco} { TX }
msa 47580 {Warner Robins} { GA }
msa 47900 {Washington-Arlington-Alexandria} { DC-VA-MD-WV }
msa 47940 {Waterloo-Cedar Falls} { IA }
msa 48140 {Wausau} { WI }
msa 48260 {Weirton-Steubenville} { WV-OH }
msa 48300 {Wenatchee-East Wenatchee} { WA }
msa 48540 {Wheeling} { WV-OH }
msa 48620 {Wichita} { KS }
msa 48660 {Wichita Falls} { TX }
msa 48700 {Williamsport} { PA }
msa 48900 {Wilmington} { NC }
msa 49020 {Winchester} { VA-WV }
msa 49180 {Winston-Salem} { NC }
msa 49340 {Worcester} { MA }
msa 49420 {Yakima} { WA }
msa 49620 {York-Hanover} { PA }
msa 49660 {Youngstown-Warren-Boardman} { OH-PA }
msa 49700 {Yuba City} { CA }


append ::xml  "</entries>"

puts $::xml