puts "<entries>"
set ::cnt 0
proc well {well url lat lon} {
    incr ::cnt
#    if {$::cnt>2} return
    regexp {([^/]+)$} $url match file
    if {![file exists $file]} {
	catch {exec wget -O $file $url} err
    }
    puts "<entry isnew=\"true\" type=\"type_document_pdf\" name=\"$file\" latitude=\"$lat\" longitude=\"$lon\" file=\"$file\" />"
}



well {"R20-97-33, R20-97-32"} {http://sddenr.net/gwqmn/gwqmn%20data/gwqmn%20website%20connections/wellsites/PDFs/AK/AK-1.pdf} {43.111723} {-101.766436}
well {"R20-94-28, R20-94-29"} {http://sddenr.net/gwqmn/gwqmn%20data/gwqmn%20website%20connections/wellsites/PDFs/OS/OS-1.pdf} {43.024687} {-101.489359}
well {"R20-94-30, R20-94-31"} {http://sddenr.net/gwqmn/gwqmn%20data/gwqmn%20website%20connections/wellsites/PDFs/OS/OS-2.pdf} {43.171573} {-101.356029}
well {"R20-2005-31, R20-2005-32"} {http://sddenr.net/gwqmn/gwqmn%20data/gwqmn%20website%20connections/wellsites/PDFs/AK/AK-2a.pdf} {43.258777} {-101.51593}
well {"R20-96-13, R20-96-12"} {http://sddenr.net/gwqmn/gwqmn%20data/gwqmn%20website%20connections/wellsites/PDFs/VM/VM-1.pdf} {43.069488} {-96.96342}
well {"R20-95-06, R20-95-07"} {http://sddenr.net/gwqmn/gwqmn%20data/gwqmn%20website%20connections/wellsites/PDFs/MO/MO-3.pdf} {42.750208} {-96.927854}
well {"R20-91-55 (DC-1B), R20-91-53 (DC-1C)"} {http://sddenr.net/gwqmn/gwqmn%20data/gwqmn%20website%20connections/wellsites/PDFs/DM/DM-2.pdf} {43.263967} {-98.227318}
well {"R20-91-55, R20-2017-30, R20-91-54"} {http://sddenr.net/gwqmn/gwqmn%20data/gwqmn%20website%20connections/wellsites/PDFs/DM/DM-1.pdf} {43.268217} {-98.258178}
well {R20-94-50} {http://sddenr.net/gwqmn/gwqmn%20data/gwqmn%20website%20connections/wellsites/PDFs/OS/OS-8.pdf} {43.162647} {-99.296639}
well {"R20-89-59, R20-89-60"} {http://sddenr.net/gwqmn/gwqmn%20data/gwqmn%20website%20connections/wellsites/PDFs/BS/BS-17.pdf} {43.112549} {-96.460891}
well {"R20-94-32, R20-94-33"} {http://sddenr.net/gwqmn/gwqmn%20data/gwqmn%20website%20connections/wellsites/PDFs/OS/OS-5.pdf} {43.045737} {-100.491789}
well {"R20-94-34, R20-94-35"} {http://sddenr.net/gwqmn/gwqmn%20data/gwqmn%20website%20connections/wellsites/PDFs/OS/OS-4.pdf} {43.067757} {-100.748159}
well {"R20-94-36, R20-94-37"} {http://sddenr.net/gwqmn/gwqmn%20data/gwqmn%20website%20connections/wellsites/PDFs/OS/OS-3.pdf} {43.031467} {-100.886519}
well {"R20-94-40, R20-94-41"} {http://sddenr.net/gwqmn/gwqmn%20data/gwqmn%20website%20connections/wellsites/PDFs/OS/OS-7.pdf} {43.024297} {-99.869149}
well {"R20-94-38, R20-94-39"} {http://sddenr.net/gwqmn/gwqmn%20data/gwqmn%20website%20connections/wellsites/PDFs/OS/OS-6.pdf} {43.212207} {-99.929319}
well {"R20-97-38, R20-97-39"} {http://sddenr.net/gwqmn/gwqmn%20data/gwqmn%20website%20connections/wellsites/PDFs/AK/AK-4.pdf} {43.136787} {-100.166519}
well {"R20-90-09 (TU-13B), R20-90-10 (TU-13A)"} {http://sddenr.net/gwqmn/gwqmn%20data/gwqmn%20website%20connections/wellsites/PDFs/PC/PC-3.pdf} {43.214707} {-96.941574}
well {"R20-88-10 (TU-9B), CO-83-158"} {http://sddenr.net/gwqmn/gwqmn%20data/gwqmn%20website%20connections/wellsites/PDFs/PC/PC-1.pdf} {43.301754} {-97.034092}
well {"R20-2019-30, R20-2019-31"} {http://sddenr.net/gwqmn/gwqmn%20data/gwqmn%20website%20connections/wellsites/PDFs/PC/PC-1a.pdf} {43.301047} {-97.040788}
well {"CO-83-149 (TU-10A), R20-88-11"} {http://sddenr.net/gwqmn/gwqmn%20data/gwqmn%20website%20connections/wellsites/PDFs/PC/PC-2.pdf} {43.286783} {-97.004168}
well {"R20-89-65, R20-89-67"} {http://sddenr.net/gwqmn/gwqmn%20data/gwqmn%20website%20connections/wellsites/PDFs/BS/BS-19.pdf} {42.80906} {-96.596901}
well {"R20-94-15, R20-94-14"} {http://sddenr.net/gwqmn/gwqmn%20data/gwqmn%20website%20connections/wellsites/PDFs/BS/BS-18.pdf} {42.937897} {-96.529126}
well {"R20-95-08, R20-95-09"} {http://sddenr.net/gwqmn/gwqmn%20data/gwqmn%20website%20connections/wellsites/PDFs/MO/MO-4.pdf} {42.692595} {-96.747037}
well {"R20-95-11, R20-95-12"} {http://sddenr.net/gwqmn/gwqmn%20data/gwqmn%20website%20connections/wellsites/PDFs/MO/MO-5.pdf} {42.583878} {-96.645433}
well {"R20-2021-52, R20-95-04"} {http://sddenr.net/gwqmn/gwqmn%20data/gwqmn%20website%20connections/wellsites/PDFs/MO/MO-2.pdf} {42.830122} {-97.184281}
well {"R20-95-13, R20-95-14"} {http://sddenr.net/gwqmn/gwqmn%20data/gwqmn%20website%20connections/wellsites/PDFs/MO/MO-1.pdf} {42.967138} {-97.240511}
well {"R20-2019-29, R20-2021-53, R20-2019-28"} {http://sddenr.net/gwqmn/gwqmn%20data/gwqmn%20website%20connections/wellsites/PDFs/MO/MO-1a.pdf} {42.923807} {-97.236408}
well {"R20-97-34, R20-97-35"} {http://sddenr.net/gwqmn/gwqmn%20data/gwqmn%20website%20connections/wellsites/PDFs/AK/AK-2.pdf} {43.268625} {-101.514013}
well {"R20-2020-31, R20-2020-32"} {http://sddenr.net/gwqmn/gwqmn%20data/gwqmn%20website%20connections/wellsites/PDFs/PC/PC-2a.pdf} {43.284547} {-97.003978}
well {"B58-2023-16, B58-2023-15"} {http://sddenr.net/gwqmn/gwqmn%20data/gwqmn%20website%20connections/wellsites/PDFs/MO/MO-1b.pdf} {42.952738} {-97.239266}


puts "</entries>"
