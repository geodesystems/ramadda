puts "<entries>"
proc img {id name} {
    set p 30d
    set p Year
    set url "https://hprcc.unl.edu/products/maps/acis/hprcc/30d${id}HPRCC.png"
    set url "https://hprcc.unl.edu/products/maps/acis/hprcc/${p}PNormHPRCC.png"
#    puts "<img src=$url width=300> $id<br>"
    puts "<entry type=\"type_image\" name=\"$name\" url=\"$url\" />"
}




img PData {Precipitation}
img PDept {Departure from Normal Precipitation}
img PNorm {Percent of Normal Precipitation}
img SPIData {Standardized Precipitation Index (SPI)}
img SPEIData {Standardized Precipitation Evapotranspiration Index (SPEI)}
img TData {Temperature}
img TDept {Departure from Normal Temperature}
img TMAXData {Average Maximum Temperature}
img TMINData {Average Minimum Temperature}
img TMAXDept {Departure from Normal Average Maximum Temperature}
img TMINDept {Departure from Normal Average Minimum Temperature}
img TMAXMAX {Maximum Temperature (Highest 1-day)}
img TMINMIN {Minimum Temperature (Lowest 1-day)}
img TMAXDept {Departure from Normal Maximum Temperature}
img TMINDept {Departure from Normal Minimum Temperature}
img C65Data {Cooling Degree Days (Base 65)}
img C65Dept {Departure from Normal CDD (Base 65)}
img H65Data {Heating Degree Days (Base 65)}
img H65Dept {Departure from Normal HDD (Base 65)}

puts "</entries>"
