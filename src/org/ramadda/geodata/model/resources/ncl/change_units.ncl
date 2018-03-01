;*************************************************
; Function: change_units
;
; Purpose:  change the units of the data
;
; Params:   dataIn - the data
;           unitsIn - the original units
;           unitsOut - the new units
;
; Return:   the data in the new units
;*************************************************
undef("change_units")
function change_units(dataIn:float, unitsIn:string, unitsOut:string)
local dataOut

begin

  ;print("unitsIn: "+unitsIn+", unitsOut: "+unitsOut)
  dataOut = dataIn
  if (ismissing(unitsIn) .or. ismissing(unitsOut)) then
     return dataOut
  end if
  if ((unitsIn .ne. unitsOut) .and. (unitsOut .ne. "")) then

    ; Temperature
    if (unitsIn .eq. "K" .or. \
        unitsIn .eq. "degK" .or. \
        unitsIn .eq. "Kelvin" .or. \
        unitsIn .eq. "Kelvins") then
      ; just a name change
      if (unitsOut .eq. "K" .or. \
          unitsOut .eq. "degK" .or. \
          unitsOut .eq. "Kelvin" .or. \
          unitsOut .eq. "Kelvins") then
        dataOut@units = unitsOut
      end if
      if (unitsOut .eq. "C" .or. \
          unitsOut .eq. "Cel" .or. \
          unitsOut .eq. "degC" .or. \
          unitsOut .eq. "Celsius") then
        dataOut = dataIn-273.15
        dataOut@units = "degC"
      end if
      if (unitsOut .eq. "F" .or. \
          unitsOut .eq. "degF" .or. \
          unitsOut .eq. "Fahrenheit") then
        dataOut = dataIn-273.15
        dataOut = (9*dataOut/5)+32
        dataOut@units = "F"
      end if
    end if

    if (unitsIn .eq. "C" .or. \
        unitsIn .eq. "Cel" .or. \
        unitsIn .eq. "degC" .or. \
        unitsIn .eq. "Celsius") then
      ; just a name change
      if (unitsOut .eq. "C" .or. \
          unitsOut .eq. "Cel" .or. \
          unitsOut .eq. "degC" .or. \
          unitsOut .eq. "Celsius") then
        dataOut@units = unitsOut
      end if
      if (unitsOut .eq. "K" .or. unitsOut .eq. "degK") then
        dataOut = dataIn+273.15
        dataOut@units = "K"
      end if
      if (unitsOut .eq. "F" .or. \
          unitsOut .eq. "degF" .or. \
          unitsOut .eq. "Fahrenheit") then
        dataOut = dataIn-32
        dataOut = (5*dataOut)/9
        dataOut@units = "F"
      end if
    end if

    if (unitsIn .eq. "F" .or. \
        unitsIn .eq. "degF" .or. \
        unitsIn .eq. "Fahrenheit") then
      ; just a name change
      if (unitsOut .eq. "F" .or. \
          unitsOut .eq. "degF" .or. \
          unitsOut .eq. "Fahrenheit") then
        dataOut@units = unitsOut
      end if
      if (unitsOut .eq. "C" .or. \
          unitsOut .eq. "Cel" .or. \
          unitsOut .eq. "degC" .or. \
          unitsOut .eq. "Celsius") then
          dataOut = (dataIn-32)*5/9
          dataOut@units = "degC"
      end if
      if (unitsOut .eq. "K" .or. unitsOut .eq. "degK") then
        dataOut = (dataIn-32)*5/9 ; convert to degC
        dataOut = dataOut+273.15
        dataOut@units = "K"
      end if
    end if

    ; precipitation  length/time
    if (unitsIn .eq. "kg m-2 s-1" .or. \
        unitsIn .eq. "kg m**-2 s**-1" .or. \
        unitsIn .eq. "kg/m^2/s" .or. \
        unitsIn .eq. "Kg/m^2/s" .or. \
        unitsIn .eq. "mm/s") then
      ; just a name change
      if (unitsOut .eq. "kg m-2 s-1" .or. \
          unitsOut .eq. "kg m**-2 s**-1" .or. \
          unitsOut .eq. "kg/m^2/s" .or. \
          unitsOut .eq. "Kg/m^2/s" .or. \
          unitsOut .eq. "mm/s") then
        dataOut@units = unitsOut
      end if
      if (unitsOut .eq. "mm/day") then
        dataOut = dataIn*86400
        dataOut@units = unitsOut
      end if
    end if
    if (unitsIn .eq. "m s-1" .or. \
        unitsIn .eq. "m s**-1" .or. \
        unitsIn .eq. "m/s") then
      ; just a name change
      if (unitsOut .eq. "m s-1" .or. \
          unitsOut .eq. "m s**-1" .or. \
          unitsOut .eq. "m/s") then
        dataOut@units = unitsOut
      end if
      if (unitsOut .eq. "mm/s") then
        dataOut = dataIn*1000
        dataOut@units = unitsOut
      end if
      if (unitsOut .eq. "mm/day") then
        dataOut = dataIn*86400000 ; 86400*1000
        dataOut@units = unitsOut
      end if
    end if
    if (unitsIn .eq. "m/day") then
      if (unitsOut .eq. "mm/s") then
        dataOut = dataIn/86.4
        dataOut@units = unitsOut
      end if
      if (unitsOut .eq. "mm/day") then
        dataOut = dataIn*1000 
        dataOut@units = unitsOut
      end if
    end if

    ; speed
    if (unitsIn .eq. "m s-1" .or. \
        unitsIn .eq. "m s**-1" .or. \
        unitsIn .eq. "m/s") then
      ; just a name change
      if (unitsOut .eq. "m s-1" .or. \
          unitsOut .eq. "m s**-1" .or. \
          unitsOut .eq. "m/s") then
        dataOut@units = unitsOut
      end if
      if (unitsOut .eq. "mi/h" .or. \
          unitsOut .eq. "mi h-1" .or. \
          unitsOut .eq. "mph") then
        dataOut = dataIn*2.23694
        dataOut@units = unitsOut
      end if
      if (unitsOut .eq. "kt" .or. \
          unitsOut .eq. "kts" .or. \
          unitsOut .eq. "knots") then
        dataOut = dataIn*1.94384
        dataOut@units = unitsOut
      end if
    end if
    if (unitsIn .eq. "mi/h" .or. \
        unitsIn .eq. "mi h-1") then
      ; just a name change
      if (unitsOut .eq. "mi/h" .or. \
          unitsOut .eq. "mi h-1") then
        dataOut@units = unitsOut
      end if
      if (unitsOut .eq. "m/s" .or. \
          unitsOut .eq. "m s-1") then
        dataOut = dataIn*0.44704
        dataOut@units = "m s-1"
      end if
      if (unitsOut .eq. "kt" .or. \
          unitsOut .eq. "kts" .or. \
          unitsOut .eq. "knots") then
        dataOut = dataIn*0.868976
        dataOut@units = unitsOut
      end if
    end if

    ; length
    ;; mm -> cm/in/ft  (short lengths)
    if (unitsIn .eq. "kg m-2" .or. \
        unitsIn .eq. "kg m**-2" .or. \
        unitsIn .eq. "kg/m^2" .or. \
        unitsIn .eq. "Kg/m^2" .or. \
        unitsIn .eq. "mm") then
      ; just a name change
      if (unitsOut .eq. "kg m-2" .or. \
          unitsOut .eq. "kg m**-2" .or. \
          unitsOut .eq. "kg/m^2" .or. \
          unitsOut .eq. "Kg/m^2" .or. \
          unitsOut .eq. "mm") then
        dataOut@units = unitsOut
      end if
      if (unitsOut .eq. "cm") then
        dataOut = dataIn/10
        dataOut@units = unitsOut
      end if
      if (unitsOut .eq. "m") then
        dataOut = dataIn/1000
        dataOut@units = unitsOut
      end if
      if (unitsOut .eq. "in") then
        dataOut = dataIn/25.4
        dataOut@units = unitsOut
      end if
      if (unitsOut .eq. "ft") then
        dataOut = dataIn/304.8
        dataOut@units = unitsOut
      end if
    end if

    ;; meters/geopotential meters -> ft/submeters (long_lengths)
    if (unitsIn .eq. "m" .or. \
        unitsIn .eq. "gpm") then
      ; just a name change
      if (unitsOut .eq. "m" .or. \
          unitsOut .eq. "gpm") then
        dataOut@units = unitsOut
      end if
      if (unitsOut .eq. "mm") then
        dataOut = dataIn*1000
        dataOut@units = unitsOut
      end if
      if (unitsOut .eq. "cm") then
        dataOut = dataIn*100
        dataOut@units = unitsOut
      end if
      if (unitsOut .eq. "ft") then
        dataOut = dataIn*.3048
        dataOut@units = unitsOut
      end if
    end if
    if (unitsIn .eq. "ft" .or. \
        unitsIn .eq. "feet") then
      ; just a name change
      if (unitsOut .eq. "ft" .or. \
          unitsOut .eq. "feet") then
        dataOut@units = unitsOut
      end if
      if (unitsOut .eq. "mm") then
        dataOut = dataIn*304.8
        dataOut@units = unitsOut
      end if
      if (unitsOut .eq. "cm") then
        dataOut = dataIn*30.48
        dataOut@units = unitsOut
      end if
      if (unitsOut .eq. "in") then
        dataOut = dataIn*12
        dataOut@units = unitsOut
      end if
    end if
    if (unitsIn .eq. "in" .or. \
        unitsIn .eq. "inch" .or. \
        unitsIn .eq. "inches") then
      ; just a name change
      if (unitsOut .eq. "in" .or. \
          unitsOut .eq. "inch" .or. \
          unitsOut .eq. "inches") then
        dataOut@units = unitsOut
      end if
      if (unitsOut .eq. "mm") then
        dataOut = dataIn*25.4
        dataOut@units = unitsOut
      end if
      if (unitsOut .eq. "cm") then
        dataOut = dataIn*2.54
        dataOut@units = unitsOut
      end if
      if (unitsOut .eq. "ft") then
        dataOut = dataIn/12
        dataOut@units = unitsOut
      end if
    end if

    ;; Pressure
    if (unitsIn .eq. "Pa" .or. \
        unitsIn .eq. "Pascal" .or. \
        unitsIn .eq. "Pascals" .or. \
        unitsIn .eq. "kg m-1 s-2") then
      ; just a name change
      if (unitsOut .eq. "Pa" .or. \
          unitsOut .eq. "Pascal" .or. \
          unitsOut .eq. "Pascals" .or. \
          unitsOut .eq. "kg m-1 s-2") then
        dataOut@units = unitsOut
      end if
      if (unitsOut .eq. "mb" .or.  \
          unitsOut .eq. "mbar" .or. \
          unitsOut .eq. "millibar" .or. \
          unitsOut .eq. "hectoPascal" .or. \
          unitsOut .eq. "hectoPascals" .or. \
          unitsOut .eq. "hPa") then
        dataOut = dataIn/100
        dataOut@units = "hPa"
      end if
    end if
    if (unitsIn .eq. "mb" .or.  \
        unitsIn .eq. "mbar" .or. \
        unitsIn .eq. "millibar" .or. \
        unitsIn .eq. "hectoPascal" .or. \
        unitsIn .eq. "hectoPascals" .or. \
        unitsIn .eq. "hPa") then
      ; just a name change
      if (unitsOut .eq. "mb" .or.  \
          unitsOut .eq. "mbar" .or. \
          unitsOut .eq. "millibar" .or. \
          unitsOut .eq. "hectoPascal" .or. \
          unitsOut .eq. "hectoPascals" .or. \
          unitsOut .eq. "hPa") then
        dataOut@units = unitsOut
      end if
      if (unitsOut .eq. "Pa") then
        dataOut = dataIn*100
        dataOut@units = "Pa"
      end if
    end if
  end if
  return dataOut

end

