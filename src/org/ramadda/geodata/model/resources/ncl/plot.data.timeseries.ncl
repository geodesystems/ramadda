;************************************************
; Program: plot.data.timeseries.ncl
; Purpose: plot data timeseries using NCL 
; Output:  XY plot
; Author:  Don Murray, CU-CIRES
;***********************************************

;--- load in the libraries
;************************************************
load "$NCARG_ROOT/lib/ncarg/nclscripts/csm/gsn_code.ncl"
load "$NCARG_ROOT/lib/ncarg/nclscripts/csm/gsn_csm.ncl"
load "$NCARG_ROOT/lib/ncarg/nclscripts/csm/contributed.ncl"
load "$NCARG_ROOT/lib/ncarg/nclscripts/csm/shea_util.ncl"
load "$NCARG_ROOT/lib/ncarg/nclscripts/contrib/ut_string.ncl"
load "$NCARG_ROOT/lib/ncarg/nclscripts/contrib/calendar_decode2.ncl"
load "$NCARG_ROOT/lib/ncarg/nclscripts/contrib/time_axis_labels.ncl"
load "$NCL_RESOURCES/change_units.ncl"
;************************************************

;**************************************************
; Procedure:  cropImage
;
; Purpose:  crop the extra white space around the image
;
; Return:  the name of the resulting image
;**************************************************
undef ("cropImage")
function cropImage(wksname:string, suffix:string, format:string)
begin
    oldimagename = wksname+".000001"+suffix
    imagename = wksname+suffix
    tmpimagename = wksname+"_tmp."+format
    outimagename = wksname+"."+format
    system("mv -f "+oldimagename+" "+imagename)
    system("%convert% -trim +repage -bordercolor white -border 10 "+imagename+" "+tmpimagename)
    system("mv -f " + tmpimagename + " " + outimagename)
    return outimagename
end

;************************************************
; Function: aavg
;
; Purpose: area weighted average
;
; Params:  data  - the data 
;***********************************************
undef("aavg")
function aavg(data[*][*][*]:float)

begin
   pi = 4.*atan(1.0)
   rad = (pi/180.)
   coslat = cos(data&lat*rad);
   aadata = wgt_areaave_Wrap(data,coslat,1.0,0)
   return aadata
end

;*****************************************************************
; Check for an array of all the same value
;*****************************************************************
undef("check_array_equal")
function check_array_equal(vals[*]:string)

local newval, first;

begin
  numvals = dimsizes(vals)
  first = vals[0];
  do idx=1,numvals-1
    newval = vals[idx]
    if (newval .ne. first) then
       return False
    end if
  end do
  return True
end


;**********************************************************
; Function: readdata
;
; Purpose:  Read data from a file
;
; Params:   ncfile - file to read
;           opts   - options
;
; Return:   the data that was read
;*********************************************************
undef("readdata")
function readdata(ncfile:string, opts:logical)

local minLat,maxLat,minLon,maxLon,varname,varlevel,vardata

begin

; Region parameters
  varname=opts@variable
  varlevel=opts@level
  ;print(varlevel)

;--  Open the data file
  datafile = addfile(ncfile ,"r")  ; open input data file
  vars = getfilevarnames(datafile)
  nvars = dimsizes(vars)
  do v = 0, nvars-1
    testvar = vars(v)
    if (testvar .eq. varname) then
      break
    end if
    if (dimsizes(getfilevardims(datafile, testvar)) .gt. 2) then
      varname = testvar
      break
    end if
  end do
  vartype = getfilevartypes(datafile,varname)
  dNames= getfilevardims(datafile,varname)
  numdims=dimsizes(dNames)

  if (numdims .gt. 3) then
     levname=dNames(numdims-3)
     levvar = datafile->$levname$
     datalevunits = levvar@units

     ;; find the level units
     if (.not. isatt(opts, "levelunit")) then
         opts@levelunit = datalevunits
     end if
     if (.not. (opts@levelunit .eq. datalevunits)) then
         if (.not. (varlevel .eq. "")) then
            newlev = change_units(stringtofloat(varlevel), opts@levelunit, datalevunits)
            varlevel = flt2string(newlev)
            ;print(varlevel)
         end if 
     end if
  end if

  tmpdata = datafile->$varname$
  ;printVarSummary(tmpdata)
  if (numdims .eq. 3) then  ; (time,lat,lon)
    if (vartype .eq. "short") then
      vardata = short2flt(tmpdata(:,:,:))
    else
      vardata = tmpdata(:,:,:)
    end if
  else ; (time,level,lat,lon)
    if (vartype .eq. "short") then
      if (varlevel .eq. "") then
        vardata = short2flt(tmpdata(:,0,:,:))
      else
        vardata = short2flt(tmpdata(:,{stringtointeger(varlevel)},:,:))
      end if
    else
      if (varlevel .eq. "") then
        vardata = tmpdata(:,0,:,:)
      else
        vardata = tmpdata(:,{stringtointeger(varlevel)},:,:)
      end if
    end if
  end if

  ;; get rid of any NaNs
  if (any(isnan_ieee(vardata))) then
    if (.not.isatt(vardata,"_FillValue")) then
      vardata@_FillValue = default_fillvalue(typeof(vardata))
    end if
    replace_ieeenan (vardata, vardata@_FillValue, 0)
  end if
  
  if (isatt(vardata,"units")) then
    units = vardata@units
  else
    units = ""
  end if
  outunits = opts@units
  ;; anomalies have to be handled separately
  if (opts@isanom .and.  (outunits .eq. "K" .or. outunits .eq. "degC")) then
    vardata@units = outunits
  else
    vardata = change_units(vardata, units, opts@units)
  end if
  return(vardata)

end

;************************************************
; Function:  remapData
;
; Purpose:   remap data from data2 into the grid of data1
;            if they are not on the same grid
;
; Params:    data1  - the first data (reference grid)
;            data2  - the second data
;
; return     data2 interpolated to data1 grid
;************************************************
undef("remapData")
function remapData(data1:float, data2:float)
local data, xi, yi, xo, yo

begin

; Get the dimensions

  xi = data2&lon
  yi = data2&lat
  xo = data1&lon
  yo = data1&lat
  data = linint2_Wrap(xi,yi,data2,False,xo,yo,0)

  return data

end

;*************************************************
; Function: subData
;
; Purpose:  subtract 2 datasets, normalizing grids
;           and making sure units are the same
;
; Params:   data1 - the first data (references grid and units)
;           data2 - the other data
;
; Return:   data1-data2
;*************************************************
undef("subData")
function subData(data1:float, data2:float)
local unit1, unit2, newData

begin

  remappedData = remapData(data1,data2)
  remappedData = change_units(remappedData, data2@units, data1@units)
  return data1-remappedData

end
;**************************************************
; Procedure:  cropImage
;
; Purpose:  crop the extra white space around the image
;
; Return:  the name of the resulting image
;**************************************************
undef ("cropImage")
function cropImage(wksname:string, suffix:string, format:string)
begin
    oldimagename = wksname+".000001"+suffix
    imagename = wksname+suffix
    tmpimagename = wksname+"_tmp."+format
    outimagename = wksname+"."+format
    system("mv -f "+oldimagename+" "+imagename)
    system("%convert% -trim +repage -bordercolor white -border 10 "+imagename+" "+tmpimagename)
    system("mv -f " + tmpimagename + " " + outimagename)
    return outimagename
end

;*************************************************
; Procedure:  addLogo
;
; Purpose:    add a logo to an image
;
; Params:     imageFilePath - the path to the image
;             logoFilePath - the logo path
;             annotation   - the annotation
;*************************************************
undef ("addLogo")
procedure addLogo (wksname:string, logoFilePath:string, format:string)
local imateFilePath, tmpFilePath
begin

  imageFilePath = wksname+"."+format
  tmpFilePath = imageFilePath + ".tmp"    
  system("%convert% -composite -gravity SouthWest " + imageFilePath + " " + logoFilePath + " " + tmpFilePath)
  system("mv -f " + tmpFilePath + " " + imageFilePath)

end

;*************************************************
; Procedure:  addAnnotation
;
; Purpose:    add some text
;
; Params:     wksname - workstation name to add to
;             text - the text to incorporate
;*************************************************
undef ("addAnnotation")
procedure addAnnotation(wksname:string, text:string, format:string)
begin
  datestr = systemfunc("date -u +'%Y-%m-%d %H:%M UTC' ")
  txres               = True
  txres@txFontHeightF = 0.012
  if ((.not. ismissing(text)) .and. (text .ne. "")) then
    plottext = "Plotted from " + text + " on "+datestr
  else
    plottext = "Plotted on "+datestr
  end if
  ;gsn_text_ndc(wksname,plottext,0.5,0.025,txres)
  imageFilePath = wksname + "."+format
  tmpFilePath = imageFilePath + ".tmp"    
  system("%convert% " + imageFilePath + "  -background white label:'" + plottext + "' -gravity Center -append " + tmpFilePath)
  system("mv -f " + tmpFilePath + " " + imageFilePath)

end

;************************************************
; Procedure: set var title
;
; Purpose:  set the var title
;
; Params:  data  - the data files to plot
;          opts - global options
;          rescf - plot options
;          isanom - is this an anomaly
;***********************************************
undef("setvartitle")
function setvartitle(data:float, opts:logical, isanom:logical, level, levelunit:string)
local vartitle
begin
  if (isatt(data,"long_name")) then
    vartitle = data@long_name
  else
    vartitle = opts@variable
  end if
  anomString = " Anomaly"
  if (isanom .and. (opts@anomtype .eq. "stdanom")) then
      anomString = " Anomaly (Standardized)"
  else 
     if (isanom .and. (opts@anomtype .eq. "pctanom")) then
       anomString = " Anomaly (Percent)"
     end if
  end if
  if (isanom .and. ismissing(str_index_of_substr(str_lower(vartitle),"anom",-1))) then
    vartitle = vartitle + anomString
  end if 
  if (.not. (opts@level .eq. "")) then
    vartitle = vartitle + " (" + level +" "+levelunit+ ")"
  end if
  if (.not. (opts@units .eq. "")) then
    if (.not. (isanom .and. (opts@anomtype .eq. "pctanom" .or. opts@anomtype .eq. "stdanom"))) then
      vartitle = vartitle + " ("+opts@units+ ")"
    end if
  end if
  return vartitle
end

;************************************************
; Function: mask data
;
; Purpose:  mask the data
;
; Params: data  - the data to mask
;         masktype - the type of mask
;         maskdata - the mask
;
; Return:  the masked data
;************************************************
undef("mask_data")
function mask_data(data[*][*][*]:float, masktype:string, lsdata[*][*]:byte)
local lsm

begin

   if (masktype .eq. "none") then
     return data
   else
     lsm  = landsea_mask(lsdata,data&lat,data&lon)
     if (masktype .eq. "ocean") then
       data = mask(data,lsm.eq.0,False)
     else
       data = mask(data,lsm.ge.1,False)
     end if
     return data
   end if
   return data

end


;************************************************
; Procedure: plotts
;
; Purpose: plot timeseries(s)
;
; Params:  files  - the data files to plot
;          wks_name - workstation name
;          wks_type - workstation type (x11, png, etc)
;          opts - allow passing optional args
;***********************************************
undef("plotts")
procedure plotts(files:numeric, wks_name:string, wks_type:string,  opts:logical)
local data, numfiles, plots, dodiff, isanom

begin

  numdims = dimsizes(files)
  numfiles = numdims(0)
  numtimes = numdims(1)
  nave = opts@nave
  if (isatt(opts,"meanIndex")) then
    nmems = (numfiles-1)
    meanIndex = opts@meanIndex
  else
    nmems = numfiles
    meanIndex = 0
  end if
  
  if (numtimes .lt. nave) then
    avgdata = files
  else
    avgdata = runave_n_Wrap(files,nave,0,1)  ; 0=option, 1=n dimension
  end if

  sample = avgdata(0,:)
  time = sample&time
  ;printVarSummary(time)

  numlines = numfiles
  if (opts@plottype .eq. "shaded") then
     numlines = 1
  end if

  lineColors = new((/numlines/),string)
  lineThicknesses = new((/numlines/),float)
  dashPatterns = new((/numlines/),float)

  wks = gsn_open_wks(wks_type, wks_name)
  grayColor = "Gray40"

  res = True
  res@gsnDraw           = False         ; Don't draw plots (for overlay)
  res@gsnFrame          = False         ; don't advance frame yet

  res@vpHeightF  = 0.4               ; change aspect ratio of plot
  res@vpWidthF   = 0.7

  do i = 0,numlines-1
    if (i .eq. meanIndex .or. numlines .eq. 1) then
      ;print("mean index = "+opts@meanIndex)
      if (opts@plottype .eq. "lines" .and. numlines .eq. 1) then
        ;;lineColors(i) = "Light"+opts@linecolor
        lineColors(i) = opts@linecolor
      else
        lineColors(i) = "Black"
      end if
      lineThicknesses(i) = 3.
      dashPatterns(i) = 0
    else
      if (opts@plottype .eq. "lines") then
        lineColors(i) = "Light"+opts@linecolor
        ;lineColors(i) = opts@linecolor
      else
        lineColors(i) = grayColor
      end if
      lineThicknesses(i) = 1.
      dashPatterns(i) = 0
    end if
  end do

  res@xyLineColors = lineColors
  res@xyLineThicknesses = lineThicknesses
  res@xyDashPatterns = dashPatterns

  ;;  Labeling
  endtimeIdx = numtimes-(1+nave/2);
  res@trXMinF = time(nave/2)
  res@trXMaxF = time(endtimeIdx)
  res@tmXTOn = False          ; Turn off top tickmarks and labels
  res@tmYRLabelsOn = True     ; Turn on right side labels
  res@tmXBTickStartF = res@trXMinF
  timevals = cd_calendar(time,-5)
  firstyear = timevals(nave/2,0);
  lastyear = timevals(endtimeIdx,0);
  numyears = (lastyear-firstyear)+1
  yint = 1
  if (numyears .gt. 150) then
    yint = 4
  end if
  if (numyears .gt. 75) then
    yint = 3
  else
    if (numyears .gt. 50) then
      yint = 2
    end if
  end if
  ymod = numyears/(5*yint)
  labelyears = new((/ymod+3,6/),integer)
  labelidx = 0
  yintidx = 1
  firstLabelFound = False
  do idx=0,numtimes-1
    ;print(idx+","+timevals(idx,0))
    year = timevals(idx,0)
    if (year%5 .eq. 0) then
      if (yintidx .lt. yint .and. firstLabelFound) then
         yintidx = yintidx+1
      else
         firstLabelFound = True
         labelyears(labelidx,:) = timevals(idx,:)
         labelidx = labelidx + 1
         yintidx = 1
      end if
    end if
  end do
  ;print(labelyears)
  restick = True
  restick@ttmFormat = "%Y"
  restick@ttmValues = labelyears
  restick@ttmMinorStride = 1
  time_axis_labels(time,res,restick)

  xLabel = "Years"
  if (nave .gt. 1) then
    xLabel = xLabel + " (" +nave+" year running average)"
  end if
  res@tiXAxisString = xLabel
  res@tiXAxisFontHeightF = 0.015
  res@tmXBLabelFontHeightF   = 0.015

  res@tiYAxisString = opts@yTitle
  res@tiYAxisFontHeightF = 0.015
  res@tmYLLabelFontHeightF   = 0.015

  if (.not. (opts@ymax .eq. 0 .and. opts@ymin .eq. 0)) then
    res@trYMinF = opts@ymin
    res@trYMaxF = opts@ymax
  end if

  if (opts@isanom) then
    res@gsnYRefLine = 0.0 
    res@gsnYRefLineColor = "Black"
  end if

  if (opts@plottype .eq. "lines") then
    plot  = gsn_csm_xy (wks,avgdata&time,avgdata,res) ; create plot
  else
    if (numlines .eq. 1) then
      line_plot  = gsn_csm_xy (wks,avgdata&time,avgdata(meanIndex,:),res) ; plot Mean line
    else
      line_plot  = gsn_csm_xy (wks,avgdata&time,avgdata,res) ; plot Mean line
    end if
    ; calculate max/min at each time
    stats      = new ((/4,numtimes/), float)
    stats(0,:) = dim_min_n(avgdata,0)
    stats(1,:) = dim_max_n(avgdata,0)
    ; calculate sigma at each time 
    sigma = dim_stddev_n(avgdata,0)
    ;print(sigma)
    stats(2,:) = avgdata(meanIndex,:)+sigma
    stats(3,:) = avgdata(meanIndex,:)-sigma
    delete(res@xyLineColors) 
    ; plot the spread as a shaded area
    res@gsnXYFillColors = "Light"+opts@linecolor
    res@xyLineColor     = -1                           ; We don't want the line, so make it transparent.
    ; resize the Y-axis to get the max/min of all members
    ;if (opts@ymax .eq. 0 .and. opts@ymin .eq. 0 .and. numlines .eq. 1) then
    if (opts@ymax .eq. 0 .and. opts@ymin .eq. 0) then
      maxY = max(stats(1,:))
      minY = min(stats(0,:))
      res@trYMaxF = maxY
      res@trYMinF = minY
    end if

    if (opts@shadedata .eq. "maxmin") then
      plot  = gsn_csm_xy (wks,avgdata&time,stats(0:1,:),res)  ; Create filled XY max/min plot.
    else
      plot  = gsn_csm_xy (wks,avgdata&time,stats(2:3,:),res)  ; Create filled XY sigma plot.
    end if

    overlay(plot,line_plot)

   end if

;
; Set up resources for a customized legend.
;
  lgres                    = True
  lgres@lgMonoMarkerIndex  = True
  lgres@lgMarkerIndex      = 16
  lgres@lgMonoMarkerColor  = True
  lgres@lgMarkerColor      = "Light"+opts@linecolor

  if (opts@plottype .eq. "lines" .or. opts@plottype .eq. "shaded") then
    lgres@lgLineColors       = (/"Light"+opts@linecolor,"Black"/)
    lgres@lgLineThicknessF   = (/2.0,3.0/)
    if (opts@plottype .eq. "lines") then
      lgres@lgMonoItemType     = True
      lgres@lgItemType         = "Lines"        ; show lines only (default)
    else
      lgres@lgMonoItemType     = False
      lgres@lgItemTypes        = (/"Markers","Lines"/)
    end if
  else
    lgres@lgLineColors       = (/"Light"+opts@linecolor,grayColor,"Black"/)
    lgres@lgLineThicknessF   = (/2.0,2.0,3.0/)
    lgres@lgMonoItemType     = False
    lgres@lgItemTypes        = (/"Markers","Lines","Lines"/)
  end if
  ;lgres@lgItemType         = "Lines"        ; show lines only (default)
  lgres@lgLabelFontHeightF = .08            ; legend label font thickness
  lgres@vpWidthF           = 0.15           ; width of legend (NDC)
  if (opts@plottype .eq. "both") then
    lgres@vpHeightF          = 0.07           ; height of legend (NDC)
  else
    lgres@vpHeightF          = 0.05           ; height of legend (NDC)
  end if
  lgres@lgPerimThicknessF  = 2.0            ; thicken the box perimeter
  lgres@lgMonoDashIndex    = True
  lgres@lgDashIndex        = 0

  if (opts@shadedata .eq. "maxmin") then
    shadeLabel = " Ensemble Max/Min"
  else
    shadeLabel = " Ensemble Std. Deviation"
  end if
  ensLabel = " Individual Members ("+nmems+")"

  if (opts@plottype .eq. "lines") then
    labels = (/ensLabel, " Ensemble Mean"/)
  else
    if (opts@plottype .eq. "shaded") then
      labels = (/shadeLabel, " Ensemble Mean"/)
    else  ; both
      labels = (/shadeLabel, ensLabel, " Ensemble Mean"/)
    end if
  end if
  nitems = dimsizes(labels)                 ; number of legend items

; Create legend
  lbid = gsn_create_legend(wks,nitems,labels,lgres)

;
; Use gsn_add_annotation to attach this legend to our existing plot.
; This way, if we resize the plot, the legend will stay with the
; plot and be resized automatically.
;
  amres                  = True

;
; Point (0,0) is the dead center of the plot. Point (0,.5) is center,
; flush bottom. Point (0.5,0.5) is flush bottom, flush right.
;
  amres@amJust           = "BottomRight"    ; Use bottom right corner of box
                                            ; for determining its location.
  amres@amParallelPosF   = 0.5              ; Move legend to right
  amres@amOrthogonalPosF = 0.5              ; Move legend down.

  if ((opts@plottype .eq. "lines" .and. numlines .gt. 1) .or. (.not. (opts@plottype .eq. "lines"))) then
    annoid = gsn_add_annotation(plot,lbid,amres)  ; add legend to plot
  end if

;
; Manually create and attach titles
;

;
; Attach some titles at the top.
;
  res_text               = True
  res_text@txFontHeightF = 0.02                      ; change font size
  txid_top = gsn_create_text(wks, opts@mainTitle, res_text)

  amres                  = True
  amres@amJust           = "BottomCenter"
  amres@amParallelPosF   =  0.0    ; This is the center of the plot.
  amres@amOrthogonalPosF = -0.72   ; This is above the top edge of the plot.
  annoid_top = gsn_add_annotation(plot, txid_top, amres)

  res_text@txFontHeightF = 0.018                     ; change font size
  txid_mid = gsn_create_text(wks, opts@regionTitle, res_text)

  amres@amOrthogonalPosF = -0.62  ; This is just below the previous title.
  annoid_mid = gsn_add_annotation(plot, txid_mid, amres)

  ;;if (opts@isanom) then
  if (isatt(sample,"long_name")) then
    vartitle = sample@long_name
  else
    vartitle = opts@variable
  end if
  if (opts@isanom .and. ismissing(str_index_of_substr(str_lower(vartitle),"anom",-1))) then
    res_text@txFontHeightF = 0.012                     ; change font size
    climText = "Anomaly relative to "+opts@climyears+" average"
    txid_bot = gsn_create_text(wks, climText, res_text)

    amres@amOrthogonalPosF = -0.55  ; This is just below the previous title.
    annoid_bot = gsn_add_annotation(plot, txid_bot, amres)
  end if

;
; Draw the plot, add annotations and convert to gif
  draw(plot)
  frame(wks)
  wksname = wks@name
  delete(wks)
  image = cropImage(wksname,".png", opts@imageformat)
  addAnnotation(wksname, opts@annotation, opts@imageformat)
;  if (isatt(opts,"logo") .and. .not. (opts@logo .eq. "")) then
;     addLogo(wksname, opts@logo, opts@imageformat)
;  end if

end

;************************************************
; Procedure: plotcomp
;
; Purpose: plot map(s)
;
; Params:  files  - the data files to plot
;          wks_name - workstation name
;          wks_type - workstation type (x11, png, etc)
;          opts - allow passing optional args
;***********************************************
undef("plotcomp")
procedure plotcomp(files[*]:string, wks_name:string, wks_type:string,  opts:logical)
local data, numfiles, alldata, isanom

begin
  if (isatt(opts,"mask") .and. .not.(opts@mask .eq. "none")) then
     a = addfile("$NCARG_ROOT/lib/ncarg/data/cdf/landsea.nc","r")
     maskdata = a->LSMASK
     masktype = opts@mask
  else
     maskdata = new((/1,1/),byte)
     masktype = "none"
  end if
  data = readdata(files(0), opts)
  mdata = mask_data(data,masktype,maskdata)
  ;aadata = wgt_areaave_Wrap(mdata,1.0,1.0,0)
  aadata = aavg(mdata)
  ;print(files(0))
  numfiles = dimsizes(files)
  xys = dimsizes(aadata)
  if (.not. ismissing(str_index_of_substr(files(0), "_mean", -1))) then
    opts@meanIndex = 0
  end if
  outtype = opts@outtype
  alldata = new((/numfiles,xys(0)/),float)
  ;printVarSummary(alldata);
  alldata(0,:) = aadata
  delete(aadata)
  delete(mdata)
  ;print(numfiles)
  do i = 1,numfiles-1
    ;print(opts@titles(i))
    ;print(files(i))
    tmpdata = readdata(files(i), opts)
    if (.not. ismissing(str_index_of_substr(files(i), "_mean", -1))) then
      opts@meanIndex = i
    end if
    mdata = mask_data(tmpdata,masktype,maskdata)
    ;aadata = wgt_areaave_Wrap(mdata,1.0,1.0,0)
    aadata = aavg(mdata)
    alldata(i,:) = aadata
    delete(tmpdata)
    delete(mdata)
    delete(aadata)
  end do
  level = opts@level
  if (isatt(opts,"levelunit")) then
    levelunit = opts@levelunit
  else
    levelunit = ""
  end if
  if (levelunit .eq. "Pa") then
    level = stringtoint(level)/100
    levelunit = "hPa"
  end if
  varTitle = setvartitle(data, opts, opts@isanom, level, levelunit)
  opts@yTitle=varTitle
  title= opts@models(0) + " " + opts@exps(0) + " " + opts@dates(0)
  opts@mainTitle = title
  if (isatt(opts,"region")) then
    region = "Region: "+opts@region
  else
    region = "Region: Lat: "+opts@minLat+"-"+opts@maxLat+" Lon: "+opts@minLon+"-"+opts@maxLon
  end if
  if (isatt(opts,"mask") .and. .not.(opts@mask .eq. "none")) then
    if (opts@mask .eq. "land") then
      region = region + " (ocean only)"
    else
      region = region + " (land only)"
    end if
  end if
  opts@regionTitle=region
  delete(data)
  ;printVarSummary(alldata)
  plotts(alldata,wks_name,wks_type,opts)

end

;**************************************************
; Main routine
;**************************************************

begin

  ; --- Read in the environment variables
  
  ; data variables
  variable=(getenv("variable"))
  if (ismissing(variable)) then 
    print("no variable")
    status_exit(-1)
  end if

  ; level
  level = getenv("level")
  if (ismissing(level)) then
    level = ""
  end if

  ; level units
  levelunits = getenv("levelunits")
  if (ismissing(levelunits)) then
    levelunits = ""
  end if
  ;print(levelunits)

  tmpfiles=getenv("ncfiles")
  if (ismissing(tmpfiles)) then 
    print("no files")
    status_exit(-2)
  end if
  ncfiles=str_split(tmpfiles,",")
  
  ;titles
  tmptitles=getenv("titles")
  if (ismissing(tmptitles)) then 
    numfiles = dimsizes(ncfiles)
    titles = new((/numfiles/),string)
  else 
    titles=str_split(tmptitles,";")
  end if
  
  ;models
  tmpmodels=getenv("models")
  if (ismissing(tmpmodels)) then 
    numfiles = dimsizes(ncfiles)
    models = new((/numfiles/),string)
  else 
    models=str_split(tmpmodels,";")
  end if
  
  ;experiments
  tmpexps=getenv("exps")
  if (ismissing(tmpexps)) then 
    numfiles = dimsizes(ncfiles)
    exps = new((/numfiles/),string)
  else 
    exps=str_split(tmpexps,";")
  end if
  
  ;ensembles
  tmpens=getenv("ens")
  if (ismissing(tmpens)) then 
    numfiles = dimsizes(ncfiles)
    ens = new((/numfiles/),string)
  else 
    ens=str_split(tmpens,";")
  end if
  
  ;ensembles
  tmpdates=getenv("dates")
  if (ismissing(tmpdates)) then 
    numfiles = dimsizes(ncfiles)
    dates = new((/numfiles/),string)
  else 
    dates=str_split(tmpdates,";")
  end if
  
  ;logo
  tmplogo=getenv("logo")
  if (ismissing(tmplogo)) then 
    logo = ""
  else 
    logo=tmplogo
  end if
  
  ; output variables
  wks_name=(getenv("wks_name"))
  if (ismissing(wks_name)) then
    wks_name="foo"
  end if
  
  productDir=(getenv("productdir"))
  if (ismissing(productDir)) then
    productDir=""
  end if
  
  plotType=getenv("plot_type")
  if (ismissing(plotType)) then
    plotType = "lines"
  end if
  
  shadeData=getenv("shade_data")
  if (ismissing(shadeData)) then
    shadeData = "sigma"
  end if
  
  imageFormat=getenv("image_format")
  if (ismissing(imageFormat)) then
    imageFormat = "gif"
  end if
  
  outType=getenv("output")
  if (ismissing(outType)) then
    outType = "comp"
  end if

  maskType=getenv("mask")
  if (ismissing(maskType)) then
    maskType = "none"
  end if

  anom=getenv("anom")
  if (.not. ismissing(anom)) then
    isanom = anom .eq. "true"
  else
    isanom = False
  end if
  
  anomtype=getenv("anomtype")
  if (ismissing(anomtype) .and. isanom ) then
    anomtype = "anom"
  end if
  
  ;
  ; Region parameters
  minLat=stringtofloat(getenv("minLat"))
  maxLat=stringtofloat(getenv("maxLat"))
  minLon=stringtofloat(getenv("minLon"))
  maxLon=stringtofloat(getenv("maxLon"))

  units=getenv("units")
  if (ismissing(units)) then
    units = ""
  end if
  
  climyears=getenv("climyears")
  if (ismissing(climyears)) then
    climyears = "1981-2010"
  end if
  
  linecolor=getenv("linecolor")
  if (ismissing(linecolor)) then
    linecolor = "Gray"
  end if
  
  ;
  ; Y-Axis parameters
  ymin=stringtofloat(getenv("ymin"))
  ymax=stringtofloat(getenv("ymax"))

  ;
  ; running average
  tmp=getenv("nave")
  if (ismissing(tmp)) then
     nave = 5
  else
    nave=stringtointeger(tmp)
  end if

  options = True
  options@plottype=plotType
  options@shadedata=shadeData
  options@imageformat=imageFormat
  options@outtype=outType
  options@mask=maskType
  options@product_dir=productDir
  options@variable=variable
  options@level=level
  options@levelunit=levelunits
  options@minLat = minLat
  options@maxLat = maxLat
  options@minLon = minLon
  options@maxLon = maxLon
  temp=getenv("region")
  if (.not. ismissing(temp)) then
    options@region = temp
  end if
  options@isanom = isanom
  options@anomtype = anomtype
  options@annotation = getenv("annotation")
  options@titles = titles
  options@units=units
  options@ymin=ymin
  options@ymax=ymax
  options@nave=nave
  options@logo=logo
  options@models=models
  options@exps=exps
  options@ens=ens
  options@dates=dates
  options@climyears=climyears
  options@linecolor=linecolor
  
  ;print("calling plotts")
  plotcomp(ncfiles, wks_name, "png", options)
  
  status_exit(0)

end
