array set ::iana {
    Eastern {America/New_York}
    Central {America/Chicago}
    Mountain {America/Denver}
    Pacific {America/Los_Angeles}
    {Hawaii-Aleutian} {Pacific/Honolulu}
    Alaska {America/Anchorage}
}

array set ::timezones {}

proc tz {state tz} {
    set iana $::iana($tz)
    set ::timezones($state) $iana
    set _state [string tolower $state]
    regsub -all { } $_state _ _state
    set ::timezones($_state) $iana    
}

proc getTimezone {state} {
    set _state [string tolower $state]
    return $::timezones($_state)
}


tz {Alabama} {Central}
tz {Alaska} {Alaska}
tz {Arizona} {Mountain}
tz {Arkansas} {Central}
tz {California} {Pacific}
tz {Colorado} {Mountain}
tz {Connecticut} {Eastern}
tz {Delaware} {Eastern}
tz {Florida} {Eastern}
tz {Georgia} {Eastern}
tz {Hawaii} {Hawaii-Aleutian}
tz {Idaho} {Mountain}
tz {Illinois} {Central}
tz {Indiana} {Eastern}
tz {Iowa} {Central}
tz {Kansas} {Central}
tz {Kentucky} {Eastern}
tz {Louisiana} {Central}
tz {Maine} {Eastern}
tz {Maryland} {Eastern}
tz {Massachusetts} {Eastern}
tz {Michigan} {Eastern}
tz {Minnesota} {Central}
tz {Mississippi} {Central}
tz {Missouri} {Central}
tz {Montana} {Mountain}
tz {Nebraska} {Central}
tz {Nevada} {Pacific}
tz {New Hampshire} {Eastern}
tz {New Jersey} {Eastern}
tz {New Mexico} {Mountain}
tz {New York} {Eastern}
tz {North Carolina} {Eastern}
tz {North Dakota} {Central}
tz {Ohio} {Eastern}
tz {Oklahoma} {Central}
tz {Oregon} {Pacific}
tz {Pennsylvania} {Eastern}
tz {Rhode Island} {Eastern}
tz {South Carolina} {Eastern}
tz {South Dakota} {Central}
tz {Tennessee} {Central}
tz {Texas} {Central}
tz {Utah} {Mountain}
tz {Vermont} {Eastern}
tz {Virginia} {Eastern}
tz {Washington} {Pacific}
tz {West Virginia} {Eastern}
tz {Wisconsin} {Central}
tz {Wyoming} {Mountain}
