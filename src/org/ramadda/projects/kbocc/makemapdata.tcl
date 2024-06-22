set url "https://landknowledge.org/repository/entry/show?&output=points.product&getdata=Get%20Data&product=points.csv&entryid="


set ids {e407cbf1-e68c-490a-95f3-8e22ac7e9ffb 7e0c6c71-10a3-4366-9392-8237a8bd1830 a69c4687-d637-4f5f-a3c1-8d3ac82dbb64 6f1adc28-36ac-438c-92ec-92f406f37bf4 31283cff-4ac2-4a64-94a3-d55dde4ae134 d7bd918a-0986-4e6e-a4fd-926ba2e0ab2d fed7063f-e62c-4624-af81-47aa5cdc66a4 88658d50-87f6-42b3-926e-072498f7e910 545fbfe9-8994-498c-baa5-4d6f6600a663 130a5794-4adb-4ea7-b92f-fae8bd3a6a61 96d9ec3d-1a8f-4a0a-8110-c3b89448bf1f 79675e0e-76ae-4153-ad60-2cf57bff03a0 b06b4483-3510-4fde-898e-1cf40fc85dc2 d3e4ae8f-5c8d-48dc-8405-c618f43db869 ef596479-4a55-4cf7-86e7-67a53b6b6e5b f54462ed-ccd2-45d4-afb3-cc84d8e7a55a bc2964a6-c03f-42d4-9c3d-6e2a92865310 1dc70ea4-6480-4c45-ba4d-10b040b390a1 1f3d35c0-693b-448d-a907-a9d82c136b15 04470e59-b3f4-4458-8cb3-e2b15ba5640f e1e91877-0ef4-4dda-bc27-0460ccc3b398 e0cdd8ba-1fd2-4aa2-a41b-20fa38b9ec51 ecf63cda-0229-4b6e-b5fe-67795db9b282 19657a85-bd2f-4665-aaad-a2bb1dbc1b85 c746a801-49c3-4c79-bba2-f9065dfe6a6b cf31e664-0ff8-4061-93e7-2020c970566c f4e50694-3e42-4856-a7e0-0b409aa5aada   5c580aa3-a77d-4c4b-b67c-7c6d7b48adbe 8223c072-3f98-49d8-a8af-2e7d9f08fa82}

#set ids {e407cbf1-e68c-490a-95f3-8e22ac7e9ffb 7e0c6c71-10a3-4366-9392-8237a8bd1830}

if {[file exists all.csv]} {
    file delete all.csv
}
foreach id $ids {
    set file kbocc_$id.csv
    set _url "$url$id"
    set clean clean_$file
    if {[file exists $file] && [file size $file]<100} {
	file delete $file
	file delete -force $clean
    }

    if {![file exists $file]} {
	puts stderr "fetching $file"
	catch {exec wget -O $file $_url}
    }
    if {![file exists $clean]} {
	puts stderr "converting $clean"
	exec /Users/jeffmc/bin/seesv.sh  -notmatch date_time 2024 -outdateformat "yyyy-MM-dd" GMT -formatdate date_time -extractdate date_time days_in_year -unique days_in_year exact -columns "date_time,temperature,latitude,longitude" -p $file > $clean
    }


    if {![file exists all.csv]} {
	exec cp $clean all.csv
    } else {
	exec tail -n +2 $clean >> all.csv
    }


}



exec /Users/jeffmc/bin/seesv.sh  -notmatch temperature NaN -p all.csv > kbocc_all_2023.csv
