;************************************************
; Program: plot.data.comp.ncl
; Purpose: plot data comparisons using NCL 
; Output:  map plot or kmz
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
load "$NCL_RESOURCES/kml.ncl"
load "$NCL_RESOURCES/remove_middle_contours.ncl"
load "$NCL_RESOURCES/change_units.ncl"
;************************************************

;*****************************************************************
; Replicate a variable
;*****************************************************************
undef("replicate_var")
function replicate_var(tmpvar)
local dummyvar
begin
  dummyvar = (/tmpvar/)
  copy_VarMeta(tmpvar, dummyvar)
  return(dummyvar)
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

;*****************************************************************
; Adjust the title
;*****************************************************************
undef("adjustTitle")
function adjustTitle(title:string)

begin
  retstring = ""
  len = strlen(title)
  if (len .gt. 60) then
     newstring = ""
     delims = str_split(title, ",")
     ;print(delims)
     numtoks = dimsizes(delims)
     didone = False
     do i = 0,numtoks-1
       if (strlen(newstring) .gt. 60) then
         retstring = retstring + newstring+"~C~"
         newstring = delims(i)
       else
         if (didone) then
           newstring = newstring + " " + delims(i) 
         else
           newstring = delims(i) 
           didone = True
         end if
       end if
     end do
     retstring = retstring+newstring
  else
     retstring = title
  end if
  ;print(retstring)
  return retstring
end

;*****************************************************************
; Function: set_kml_map_defaults
;
; Purpose:  Turn off map labelling, tickmarks, etc
;
; Params:   res - the map properites
; (override kml_map_defaults in kml.ncl)
;*****************************************************************
undef("set_kml_map_defaults")
procedure set_kml_map_defaults ( res:logical )
begin

  res@mpOutlineOn     = False
  res@mpPerimOn       = False
  res@mpGridAndLimbOn = False

;  Keep the border on so we can use it for clipping
;  res@tmXBBorderOn    = False
;  res@tmXTBorderOn    = False
;  res@tmYRBorderOn    = False
;  res@tmYLBorderOn    = False

  res@tmXBOn          = False
  res@tmXTOn          = False
  res@tmYROn          = False
  res@tmYLOn          = False
  res@gsnMaximize     = True

  ;; Annotations
  res@gsnLeftString = ""
  res@gsnRightString = ""
  res@tiMainString = ""

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
  minLat=opts@minLat
  maxLat=opts@maxLat
  minLon=opts@minLon
  maxLon=opts@maxLon
  noLatLon = ismissing(minLat) .or. \
             ismissing(maxLon) .or. \
             ismissing(minLat) .or. \
             ismissing(maxLat)
  
  varname=opts@variable
  varlevel=opts@level
  ;print(varlevel)

;--  Open the data file
  pd = opts@product_dir
  myfile = ncfile
  if (ismissing(str_index_of_substr(ncfile,pd,-1))) then
    myfile = pd + "/" + ncfile
  end if
  datafile = addfile(myfile ,"r")  ; open input data file
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

;-- get the lat/lon info for normalizing
  lonname=dNames(numdims-1)
  lonvar = datafile->$lonname$
  numlons=dimsizes(lonvar)
  minDLon=lonvar(0)
  maxDLon=lonvar(numlons(0)-1)
  if (minDLon .gt. maxDLon) then
    tmp = maxDLon
    maxDLat = minDLon
    minDLon = tmp
    delete(tmp)
  end if
  latname=dNames(numdims-2)
  latvar = datafile->$latname$
  numlats=dimsizes(latvar)
  minDLat=latvar(0)
  maxDLat=latvar(numlats(0)-1)
  if (minDLat .gt. maxDLat) then
    tmp = maxDLat
    maxDLat = minDLat
    minDLat = tmp
    delete(tmp)
  end if
  
; if not passed in, then set from data
  if (noLatLon) then
    minLon = doubletofloat(minDLon)
    maxLon = doubletofloat(maxDLon)
    minLat = doubletofloat(minDLat)
    maxLat = doubletofloat(maxDLat)
  end if
  
; check for 0-360
  if(minDLon.gt.180 .or. maxDLon.gt.180) then
    if (minLon.lt.0 .or. maxLon.lt.0) then
      minLon = minLon+360
      maxLon = maxLon+360
    end if
  else 
    if (minDLon.lt.0 .or. maxDLon.lt.0) then
      if (minLon.gt.180 .or. maxLon.gt.180) then
        minLon = minLon-180
        maxLon = maxLon-180
      end if
    end if
  end if

  ;print(minLon)
  ;print(maxLon)

  latdiff=abs(maxLat-minLat)
  londiff=abs(maxLon-minLon)

  ratio=(2*latdiff)/londiff
  ;print(ratio)
  opts@ratio=ratio

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
  if (numdims .eq. 3) then  ; (time,lat,lon)
    if (vartype .eq. "short") then
      vardata = short2flt(tmpdata(:,{minLat:maxLat},{minLon:maxLon}))
    else
      vardata = tmpdata(:,{minLat:maxLat},{minLon:maxLon})
    end if
  else ; (time,level,lat,lon)
    if (vartype .eq. "short") then
      if (varlevel .eq. "") then
        vardata = short2flt(tmpdata(:,0,{minLat:maxLat},{minLon:maxLon}))
      else
        vardata = short2flt(tmpdata(:,{stringtointeger(varlevel)},{minLat:maxLat},{minLon:maxLon}))
      end if
    else
      if (varlevel .eq. "") then
        vardata = tmpdata(:,0,{minLat:maxLat},{minLon:maxLon})
      else
        vardata = tmpdata(:,{stringtointeger(varlevel)},{minLat:maxLat},{minLon:maxLon})
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
  
  ;printVarSummary(vardata)
  if (noLatLon) then
    opts@minLat = minLat
    opts@maxLat = maxLat
    opts@minLon = minLon
    opts@maxLon = maxLon
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
; Procedure:  cropKmlImage
;
; Purpose:  crop the extra white space around the image for KML
;
; Return:  the name of the resulting image
;**************************************************
undef ("cropKmlImage")
function cropKmlImage(wksname:string, suffix:string)
begin
    oldimagename = wksname+".000001"+suffix
    imagename = wksname+suffix
    tmpimagename = wksname+"_tmp.png"
    outimagename = wksname+".png"
    system("mv -f "+oldimagename+" "+imagename)
    system("%convert% -trim -shave 1x1 +repage "+imagename+" "+tmpimagename)
    system("mv -f " + tmpimagename + " " + outimagename)
    return outimagename
end

;**************************************************
; Procedure:  cropImage
;
; Purpose:  crop the extra white space around the image
;
; Return:  the name of the resulting image
;**************************************************
undef ("cropImage")
function cropImage(wksname:string, wks_type:string, format:string)
begin
   
    suffix = "."+wks_type
    imagename = wksname+suffix
    tmpimagename = wksname+"_tmp."+format
    outimagename = wksname+"."+format
    if (suffix .eq. ".png") then
      oldimagename = wksname+".000001"+suffix
      system("mv -f "+oldimagename+" "+imagename)
    end if
    if (wks_type .eq. "png") then
      system("%convert% -trim +repage -bordercolor white -border 10 "+imagename+" "+tmpimagename)
    else
      system("%convert% -alpha off -density 300 -resize 38% -quality 92 -trim +repage "+imagename+" -bordercolor white -border 10 "+tmpimagename)
    end if
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

;**************************************************
; Procedure:  decorateImage
;
; Purpose:  crop and decorate the image.  Always produce a GIF
;
; Return:  the name of the resulting image
;**************************************************
undef ("decorateImage")
function decorateImage(wksname:string, wks_type:string, opts:logical)
begin
  ;; always crop the png image ??
  if ((opts@imageformat .eq. "png")) then
    image = cropImage(wksname, wks_type, opts@imageformat)
  end if
  image = cropImage(wksname, wks_type, "gif")
  addAnnotation(wksname, opts@annotation, "gif")
  if (isatt(opts,"logo") .and. .not. (opts@logo .eq. "")) then
     addLogo(wksname, opts@logo, "gif")
  end if
  return image
end

;************************************************************
;
; based on the NCL example script lb_19.ncl by Mary Haley
; 

undef("make_kml_labelbar")
function make_kml_labelbar (kml:string, name:string, wks:graphic,plot:graphic, res:logical)
local kml, name, plot, res, cmap, levels, colors, font_height, \
      labels, lbres, lbplot_name, lbplot_file, lbicon, lbwks, lbplot, \
      lbcropbox, files, out, views, lbres, just_strs, lbar_x, lbar_y, lbar_h, lbar_w, \
  lbar_orient, lbar_side, lbar_colors, lbar_labels, lbar_frac, lbar_ext, \
  lbar_bot, lbar_top, lbar_rgt, lbar_lft, lbar_fh, lbar_aln, lbar_ljust, \
  lbar_just, lbar_mnr, lbar_mjr, lbar_loff , lbid, nboxes, class_name, \
  labebar_object
begin
;
; This code attempts to see what kind of plot we have and
; to extract the labelbar from it.
;
  class_name = NhlClassName(plot)
  ;print(class_name)
  if(class_name.eq."mapPlotClass") then
    if(isatt(plot,"contour")) then
      getvalues plot@contour
        "pmAnnoViews" : views
      end getvalues
    else
      if(isatt(plot,"vector")) then
        getvalues plot@vector
          "pmAnnoViews" : views
        end getvalues
      else
        print("contour_labelbar: Error: can't determine type of plot")
        return
      end if
    end if
  else
    getvalues plot
      "pmAnnoViews" : views
   end getvalues
  end if
  ;print(views)

;---Error checking
  if(all(ismissing(views))) then
    print("contour_labelbar: Error: Couldn't extract labelbar associated with this plot")
    return
  end if

;---Attempt to extract labelbar
  labelbar_object = new(1,graphic)
  do i=0,dimsizes(views)-1
    if(NhlClassName(views(i)).eq."labelBarClass")  then
      labelbar_obj = views(i)
    end if
  end do

;---Error checking
  if(ismissing(labelbar_obj)) then
    print("contour_labelbar: Error: Couldn't extract labelbar associated with this plot")
    return
  end if
  
  getvalues wks
    "wkColorMap"             : cmap
  end getvalues

  getvalues plot@contour
    "cnLevels"               : levels
    "cnFillColors"           : colors
    "cnInfoLabelFontHeightF" : font_height
  end getvalues

  labels = "" + levels       ; Convert levels to a string array. This is
                             ; not necessary, but it gets rid of the
                             ; annoying error message about coercing types.

;---Retrieve labelbar resources associated with plot so we can regenerate it.
  getvalues labelbar_obj
    "vpXF"                 : lbar_x
    "vpYF"                 : lbar_y
    "vpHeightF"            : lbar_h
    "vpWidthF"             : lbar_w
    "lbOrientation"        : lbar_orient
    "lbLabelPosition"      : lbar_side
    "lbFillColors"         : lbar_colors
    "lbLabelStrings"       : lbar_labels
    "lbBoxFractions"       : lbar_frac
    "lbBoxMinorExtentF"    : lbar_ext
    "lbBottomMarginF"      : lbar_bot
    "lbTopMarginF"         : lbar_top
    "lbRightMarginF"       : lbar_rgt
    "lbLeftMarginF"        : lbar_lft
    "lbLabelFontHeightF"   : lbar_fh
    "lbLabelAlignment"     : lbar_aln
    "lbLabelJust"          : lbar_ljust
    "lbLabelFontColor"     : lbar_fc
    "lbJustification"      : lbar_just
    "lbBoxMinorExtentF"    : lbar_mnr
    "lbBoxMajorExtentF"    : lbar_mjr
    "lbLabelOffsetF"       : lbar_loff
    "lbPerimOn"            : perim_on
    "lbTitleOn"            : title_on
    "lbMonoFillPattern"    : mono_fill_pattern
    "lbBoxLinesOn"         : box_lines_on
    "lbLabelsOn"           : labels_on
  end getvalues

;
; Set some labelbar resources. Note: Not every single 
; labelbar resource is retrieved and set here. If the
; labelbar is not being recreated properly, then you
; may need to add some more resources to the "getvalues"
; block above, and then set them below.
;
  lbres                      = True

;---Set width and height of labelbar itself
  lbres@vpHeightF            = lbar_h
  lbres@vpWidthF             = lbar_w


; Allow more control over labelbars.
  lbres@lbAutoManage         = False

  lbres@lbOrientation        = lbar_orient
  lbres@lbLabelPosition      = lbar_side
  lbres@lbLabelFontHeightF   = lbar_fh
  lbres@lbLabelFontColor     = lbar_fc
;  print(lbar_fc)

; Turn various features on and off.
  lbres@lbLabelsOn           = labels_on
  lbres@lbPerimOn            = perim_on
  lbres@lbTitleOn            = title_on
  lbres@lbMonoFillPattern    = mono_fill_pattern
  lbres@lbBoxLinesOn         = box_lines_on
  lbres@lbFillColors         = lbar_colors

; Which point to position labelbar about.
  lbres@lbJustification      = lbar_just
  lbres@lbLabelAlignment     = lbar_aln
  lbres@lbLabelJust          = lbar_ljust

  lbres@lbLabelAutoStride    = True        ; NO LONGER NEEDED IN V6.1.0 

;---Regenerate the labelbar
  nboxes = dimsizes(lbar_colors)
    
  lbplot_name = wks@name  + "_lb"
  lbplot_file = lbplot_name + ".png"
  lbicon      = lbplot_name + ".png"

  lbwks = gsn_open_wks("png",lbplot_name)
  gsn_define_colormap(lbwks,cmap)
  
  
    lbid = gsn_create_labelbar(lbwks,dimsizes(levels)+1,labels,lbres)
    
    lbplot = create "lbplot" mapPlotClass lbwks
    "mpOutlineOn"     : False
    "mpPerimOn"       : False
    "mpGridAndLimbOn" : False
    "tmXBBorderOn"    : False
    "tmXTBorderOn"    : False
    "tmYRBorderOn"    : False
    "tmYLBorderOn"    : False
    "tmXBOn"          : False                  
    "tmXTOn"          : False
    "tmYROn"          : False
    "tmYLOn"          : False
    end create
      
   annoid = gsn_add_annotation(lbplot,lbid,False)
   draw(lbplot)
   frame(lbwks)
   
   wksname = lbwks@name
;   lbcropbox = kml_get_vp_cropbox(lbwks,lbid)
   delete(lbwks)

    lbicon = cropImage(wksname, "png", "png")
;   lbicon = kml_crop_plot(lbcropbox,res)

;---Turn off the current labelbar so we can draw the one we just regenerated.
  setvalues views(0)
    "lbLabelBarOn" : False
  end setvalues
  return(lbicon)
end

undef("make_kml_image")
function make_kml_image(kml:string, name:string, wks:graphic, plot:graphic, kres:logical, rescf:logical, makeLabelBar:logical)

begin

  if (makeLabelBar) then
    ; position resources for ScreenOverlay element
    kres@kmlOverlayXY = True
    kres@kmlOverlayXYx = 0
    kres@kmlOverlayXYy = 1
    kres@kmlOverlayXYxunits = "fraction" ;
    kres@kmlOverlayXYyunits = "fraction" ; pixels, or insetPixels
    kres@kmlScreenXY = True
    kres@kmlScreenXYx = 0
    kres@kmlScreenXYy = 1
    kres@kmlScreenXYxunits = "fraction"
    kres@kmlScreenXYyunits = "fraction"
  
    ; crops the labelbar from the plot and converts it to a png
    lbicon = make_kml_labelbar(kml, "label bar", wks, plot, kres)
  
    ; add labelbar to KML document as a ScreenOverlay element
    kml = kml_add_screenoverlay ( kml, "Legend", lbicon, kres )

    ; add cropped labelbar filename to list of files to 
    ; be compressed in KMZ archive
    kml_add_kmzfile(kml,lbicon)
  
    ;Turn off the label bar for future plots
    rescf@lbLabelBarOn = False         ; turn off label bar

    ; add a folder for the ground overlay(s)
    kml_folder = kml_open_folder(kml, "GroundOverlay Folder",False)

  end if

  draw(plot)
  frame(wks)

  ; "delete(wks)" can not be called within a function, 
  ;need to find a work around for this
;;  wksname = wks@name
;;  delete(wks)
;;    
;;  ;kres@kmlCropAlphaColor = "white"
;;  ; crops the groundoverlay map and converts it to png
;;  ; must "delete(wks)" before this command
;;  ;icon = kml_crop_plot(cropbox, kres)
;;  icon = cropKmlImage(wksname, ".png")
;;  
;;  ; add cropped map to KML document as a GroundOverlay element
;;  kml = kml_add_groundoverlay(kml, name, icon, LatLonBox, 0, kres)
;;    
;;  ; add cropped map filename to list of files to be 
;;  ; compressed in KMZ archive
;;  kml_add_kmzfile(kml,icon)
  return kml
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
  accumString = " Accumulation"
  if (isanom) then
      if (opts@anomtype .eq. "stdanom") then
        anomString = " Anomaly (Standardized)"
      end if
     if (opts@anomtype .eq. "pctanom") then
       anomString = " Anomaly (Percent)"
     end if
  end if
  if (isanom .and. ismissing(str_index_of_substr(str_lower(vartitle),"anom",-1))) then
    vartitle = vartitle + anomString
  end if 
  if (opts@anomtype .eq. "timstd") then
    vartitle = vartitle + " Standard Deviation"
  end if
  if (opts@stattype .eq. "sum") then
    vartitle = vartitle + " Accumulation"
  end if
  if (.not. (opts@level .eq. "")) then
    vartitle = vartitle + " (" + level +" "+levelunit+ ")"
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
function mask_data(data:float, masktype:string, lsdata[*][*]:byte)
;function mask_data(data[*][*]:float, masktype:string, lsdata[*][*]:byte)
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
; Procedure: plotmap
;
; Purpose: plot map(s)
;
; Params:  files  - the data files to plot
;          wks_name - workstation name
;          opts - allow passing optional args
;***********************************************
undef("plotmap")
procedure plotmap(files:list, wks_name:string, opts:logical)
local data, numfiles, plots, dodiff, doPanel, isanom

begin

  wks_type = opts@imageformat
  if (wks_type .eq. "gif") then
    wks_type = "png"
  end if
  numfiles = ListCount(files)
  ;print(numfiles)
  data = files[0]
  plots = new(numfiles,graphic)
  outtype = opts@outtype
  dodiff = outtype .eq. "diff" .and. numfiles .eq. 2
  isanom = opts@isanom
  addAnom = isanom .and. .not. (outtype .eq. "regression" .or. outtype .eq. "correlation")
  doGoogle = (opts@plottype .eq. "kmz")
  doPanel = (.not. dodiff) .and. (.not. doGoogle) .and. numfiles .gt. 1 
  plottitles = opts@titles
  numtitles = dimsizes(plottitles)
  usepolar = opts@usepolar
  if (isatt(opts,"mask") .and. .not.(opts@mask .eq. "none")) then
     a = addfile("$NCARG_ROOT/lib/ncarg/data/cdf/landsea.nc","r")
     maskdata = a->LSMASK
     masktype = opts@mask
  else
     maskdata = new((/1,1/),byte)
     masktype = "none"
  end if


  rescf = True;
  ;rescf@gsnAddCyclic          = (addCyclic .eq. "true")    ; turn off if subset
  rescf@gsnAddCyclic          = False
  rescf@mpMinLatF            = opts@minLat       ; range to zoom in on
  rescf@mpMaxLatF            = opts@maxLat
  if (.not. usepolar .or. doGoogle) then 
    rescf@mpMinLonF            = opts@minLon
    rescf@mpMaxLonF            = opts@maxLon
    centerLonF         = (opts@maxLon + opts@minLon)/2
    centerLatF         = (opts@maxLat + opts@minLat)/2
    rescf@mpCenterLonF         = centerLonF
    usepolar = False ;; in case this is doGoogle
    if (.not. doGoogle) then
       ;rescf@mpProjection = "Satellite"
       rescf@mpProjection = "CylindricalEquidistant"
    end if
    if (isatt(opts,"latlonlines") .and. opts@latlonlines) then
      rescf@mpGridAndLimbOn = True
      rescf@mpGridLineDashPattern = 2
      lonSpacing = 15.0
      latSpacing = 15.0
      deltaLon = opts@maxLon - opts@minLon
      deltaLat = opts@maxLat - opts@minLat
      if (deltaLon .lt. 50) then
        lonSpacing = 10.0
        if (deltaLon .lt. 20) then
          lonSpacing = 5.0
        end if
        if (deltaLon .lt. 10) then
          lonSpacing = 2.0
        end if
      end if
      if (deltaLat .lt. 50) then
        latSpacing = 10.0
        if (deltaLat .lt. 20) then
          latSpacing = 5.0
        end if
        if (deltaLat .lt. 10) then
          latSpacing = 2.0
        end if
      end if
      rescf@mpGridLonSpacingF = lonSpacing
      rescf@mpGridLatSpacingF = latSpacing
    end if
  else 
    if (opts@maxLat .gt. 0) then
      rescf@gsnPolar = "NH"
    else
      rescf@gsnPolar = "SH"
    end if
    rescf@mpCenterLonF = opts@meridian  
    rescf@gsnAddCyclic          = True
  end if


  rescf@gsnDraw           = False         ; Don't draw plots (for overlay)
  rescf@gsnFrame          = False         ; don't advance frame yet
  
  ; workstation stuff
  wks_type@wkBackgroundColor="white"
  wks_type@wkForegroundColor="black"
  if (wks_type .eq. "png") then
    if (doPanel) then
      wks_type@wkWidth                     = 1600
      wks_type@wkHeight                    = 1600
    else
      wks_type@wkWidth                     = 800
      wks_type@wkHeight                    = 800
    end if
  else
    if (doPanel) then
      wks_type@wkPaperSize                 = "letter"
    else
      wks_type@wkPaperSize                 = "letter"
    end if
  end if
  
  ; map stuff
  rescf@mpFillOn = False   ; don't fill in continents - maybe user selectable in future

  rescf@mpOutlineBoundarySets = "National"
  rescf@mpDataBaseVersion = "MediumRes"
  rescf@mpOutlineSpecifiers = (/"Canada : Provinces","United States : States"/) 
  rescf@mpDataSetName = "Earth..4"
  
  if (wks_type .eq. "png") then
    rescf@mpGeophysicalLineThicknessF = 1.5
    rescf@mpNationalLineThicknessF = 1.
    rescf@mpUSStateLineThicknessF = 1. 
  else
    rescf@mpGeophysicalLineThicknessF = .5
    rescf@mpNationalLineThicknessF = .5
    rescf@mpUSStateLineThicknessF = .5
  end if

  rescf@mpGeophysicalLineColor = "gray29"       ; color of cont. outlines
  rescf@mpNationalLineColor    = rescf@mpGeophysicalLineColor ; color of nat'l outlines
  rescf@mpUSStateLineColor     = rescf@mpGeophysicalLineColor ; color of state outlines
  

  ; contour stuff
  rescf@gsnSpreadColors = True
  rescf@cnFillOn= opts@cfill
  rescf@cnFillMode= "AreaFill"  
  rescf@lbLabelAutoStride = True

  if (isatt(data,"long_name")) then
    vartitle = data@long_name
  else
    vartitle = opts@variable
  end if

  ; labelbar and panel specific stuff
  if (doPanel) then
    rescf@lbLabelBarOn = False         ; turn off label bar
    rescf@mpGeophysicalLineThicknessF = .5  ; reduce the line thickness
    rescf@mpNationalLineThicknessF = .25     ; reduce the line thickness
    rescf@tmBorderThicknessF = 1.
    rescf@tmXBMajorThicknessF =  1.
    rescf@tmYLMajorThicknessF = 1.
  else
    rescf@lbLabelBarOn = True         ; turn on label bar
    if (opts@isanom .and. ismissing(str_index_of_substr(str_lower(vartitle),"anom",-1))) then
      climText = "Anomaly relative to "+opts@climId+opts@climyears+" average"
      rescf@lbTitleString = climText
      rescf@lbTitleFontHeightF = 0.01
      rescf@lbTitlePosition = "Bottom"
      rescf@lbTitleOffsetF = 0.2
    end if
  end if

  rescf@tiMainFontHeightF=0.018
  rescf@tmXBLabelFontHeightF = 0.011
  rescf@tmYLLabelFontHeightF = 0.011
  rescf@gsnLeftStringFontHeightF = 0.015
  rescf@gsnRightStringFontHeightF = 0.015
  if (doPanel .and. numfiles.gt.8) then  ; increase font size for panels
    rescf@tmXBLabelFontHeightF = 0.018
    rescf@tmYLLabelFontHeightF = 0.018
    rescf@gsnLeftStringFontHeightF = 0.024
    rescf@gsnRightStringFontHeightF = 0.024
  end if
  if (.not. usepolar) then  ; adjust the labels down a bit
    rescf@gsnLeftStringOrthogonalPosF = 0.01
    rescf@gsnRightStringOrthogonalPosF = 0.01
  end if
  rescf@tiMainFontThicknessF=.8
  rescf@tmXTOn          = False
  rescf@tmYROn          = False
  cint = opts@cint

  if (.not. ismissing(cint) .and. cint .ne. 0.) then
    rescf@cnLevelSelectionMode = "ManualLevels"
    rescf@cnLevelSpacingF = opts@cint
    rescf@cnMinLevelValF = opts@cmin
    rescf@cnMaxLevelValF = opts@cmax
  else
    if (outtype .eq. "correlation") then
        rescf@cnLevelSelectionMode = "ManualLevels"
        rescf@cnLevelSpacingF = 0.1
        rescf@cnMinLevelValF = -0.9
        rescf@cnMaxLevelValF = 0.9
        cint = 0.1
        ;rescf@lbLabelAlignment = "ExternalEdges"
        ;rescf@lbLabelAutoStride = False
        ;rescf@lbLabelStrings = (/"-0.9","","-0.7","","-0.5","","-0.3","","-0.1","0.1","","0.3","","0.5","","0.7","","0.9"/)
        ;rescf@lbLabelStrings = (/"-1.0","","-0.8","","-0.6","","-0.4","","-0.2","","0.2","","0.4","","0.6","","0.8","","1.0"/)
    end if
  end if
  
  if (doGoogle) then

    wks_type="png"
    set_kml_map_defaults(rescf) ; turn off many options
    rescf@cnLinesOn = False ; turn off contour lines
    rescf@cnLineLabelsOn=False
    ;rescf@lbLabelFontColor="SlateGray"
    rescf@gsnMaximize = True
    rescf@gsnPaperOrientation = "portrait"
    rescf@gsnLeftString = ""
    rescf@gsnCenterString = ""
    rescf@gsnRightString = ""
    rescf@gsnBoxMargin = 0.0

    ; label bar
    rescf@lbLabelBarOn = True;
    rescf@lbLabelFontHeightF = 0.008;
    rescf@lbOrientation         = "Horizontal"   
    rescf@pmLabelBarWidthF      = 0.3               ; default is shorter
    rescf@pmLabelBarHeightF     = 0.040             ; default is taller
      
  else

    if (.not. doPanel) then
      rescf@gsnMaximize = True
      rescf@gsnPaperOrientation = "portrait"
    else 
      rescf@gsnMaximize = False
    end if

    ;contour params
    rescf@cnLinesOn=opts@clines
    ;; If they don't want lines, still make some very light ones.
    ;if (.not. rescf@cnLinesOn) then
    ;  rescf@cnLinesOn=True
    ;  rescf@cnLinesThicknessF=0.8
    ;  rescf@cnLineColor="gray80"
    ;end if
    rescf@cnLineLabelsOn=opts@clabels
    rescf@cnLineLabelPlacementMode = "Computed"
    rescf@cnLineLabelAngleF=0
    rescf@cnConstFLabelFontHeightF=0.010

    ; label bar
    rescf@lbOrientation         = "Horizontal"   
    rescf@pmLabelBarWidthF      = 0.5               ; default is shorter
    rescf@pmLabelBarHeightF     = 0.060             ; default is taller
    rescf@lbTopMarginF          = 0.30

    ; lable bar title
    rescf@lbTitleFontHeightF = 0.013
    rescf@lbTitlePosition = "Bottom"
    ;rescf@lbTitleOffsetF = 0.005
    ;rescf@lbBottomMarginF = 0.025

  end if
    
  LatLonBox = new(4,float)
  LatLonBox(0) = stringtofloat(rescf@mpMaxLatF)
  LatLonBox(1) = stringtofloat(rescf@mpMinLatF)
  LatLonBox(2) = stringtofloat(rescf@mpMaxLonF)
  LatLonBox(3) = stringtofloat(rescf@mpMinLonF)

  wks = gsn_open_wks(wks_type,wks_name) 
  ;drawNDCGrid(wks)
  gsn_define_colormap(wks, opts@colormap)
  ;printVarSummary(data)

  if (opts@invert_cmap) then
      gsn_reverse_colormap(wks)
  end if
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

  ; TODO: Handle multiple times
  if (dodiff) then
    plottitle = ""
    if ((.not. ismissing(plottitles(0))) .and. (plottitles(0) .ne. "")) then
      plottitle = adjustTitle(plottitles(0))
    end if
    if (numfiles .gt. 1) then
      do i = 1,numfiles-1
        ;newdata = readdata(files(i),opts)
        ;;newdata = files(i,:,:)
        newdata = files[i]
        data = subData(data,newdata)
        if (numtitles .ge. i .and. \
            (.not. ismissing(plottitles(i))) .and. \
            (plottitles(i) .ne. "")) then
          plottitle = plottitle + " minus~C~" + adjustTitle(plottitles(i))
        end if
        delete(newdata)
      end do
    end if
    rescf@tiMainString        = plottitle
    ;mydata = data(0,:,:)
    mydata = mask_data(data,masktype,maskdata)
    rescf@gsnLeftString = setvartitle(mydata, opts, addAnom, level, levelunit)
    if (doGoogle) then
      set_kml_map_defaults(rescf) ; turn off many options
      ;;rescf@lbTitleString = rescf@gsnLeftString
    end if
    if (cint .eq. 0) then
      symMinMaxPlt(mydata,20,False,rescf)
    end if
    plots(0) = gsn_csm_contour_map(wks,mydata,rescf)
    ;if (cint .eq. 0) then
    if (rescf@cnMinLevelValF .eq. (-1*rescf@cnMaxLevelValF)) then 
      ;print("removing middle contours for plot 0")
      res2 = remove_middle_contours(plots(0), rescf, 1)
      plots(0) = gsn_csm_contour_map(wks,mydata,res2)
    end if
    ;if (.not. doGoogle) then
    ;    addAnnotation(wks,opts@annotation, opts@imageformat)
    ;end if
    delete(mydata)
  else 
    if (.not. doGoogle) then
      do i = 0,numfiles-1
        ;;mydata = files(i,:,:)
        mydata = files[i]
        mydata = mask_data(mydata,masktype,maskdata)
        if (isatt(opts,"longnames")) then
           if (.not. ismissing(opts@longnames(i))) then
             mydata@long_name = opts@longnames(i)
           else
             mydata@long_name = opts@variable
           end if
        end if
        ;rescf@gsnLeftString = setvartitle(mydata, opts, addAnom, level, levelunit)
        if (isatt(opts,"leftstring")) then
          rescf@gsnLeftString = opts@leftstring(i)
        else 
          rescf@gsnLeftString = setvartitle(mydata, opts, addAnom, level, levelunit)
          if ((.not. ismissing(plottitles(i))) .and. (plottitles(i) .ne. "")) then
            rescf@tiMainString        = adjustTitle(plottitles(i))
          end if
        end if
        ;rescf@lbTitleString = rescf@gsnLeftString
        if (i.eq. 0 .and. (isanom .and. cint .eq. 0)) then
          symMinMaxPlt(mydata,20,False,rescf)
        end if
        plots(i) = gsn_csm_contour_map(wks,mydata,rescf)
        if (isanom .or. \
         (isatt(rescf, "cnMinLevelValF") .and. isatt(rescf, "cnMaxLevelValF") .and. \
         (rescf@cnMinLevelValF .eq. (-1*rescf@cnMaxLevelValF)))) then 
          ;print("removing middle contours for plot "+i)
          res2 = remove_middle_contours(plots(i), rescf, 1)
          plots(i) = gsn_csm_contour_map(wks,mydata,res2)
        else
          if (i .eq. 0 .and. (ismissing(cint) .or. cint .eq. 0.)) then
             myplot = plots(0)
             getvalues myplot@contour
               "cnMinLevelValF" : lb0 ; low label value
               "cnMaxLevelValF" : lb1 ; high label value
               "cnLevelSpacingF" : spcF ;
             end getvalues
             rescf@cnLevelSelectionMode = "ManualLevels"
             rescf@cnLevelSpacingF = spcF
             rescf@cnMinLevelValF = lb0
             rescf@cnMaxLevelValF = lb1
             cint = spcF
          end if
        end if
        ;if (.not. doPanel) then
        ;  addAnnotation(wks,opts@annotation, opts@imageformat)
        ;end if
        delete(mydata)
      end do

    else ;; doGoogle

      ;; Make the first plot to get the labelbar
      mydata = mask_data(data,masktype,maskdata)
;;      ;; make title for labelbar
;;      vartitle = setvartitle(mydata, opts, addAnom, level, levelunit)

      name="Plot 1"
      if ((.not. ismissing(plottitles(0))) .and. (plottitles(0) .ne. "")) then
        name        = plottitles(0)
      else
        name = "Plot 1"
      end if 
      
      if (isanom .and. cint .eq. 0) then
        symMinMaxPlt(mydata,20,False,rescf)
      end if
      gplot = gsn_csm_contour_map_ce(wks,mydata,rescf)
      ;if (isanom) then
      ;if (isanom .or. (rescf@cnMinLevelValF .eq. rescf@cnMaxLevelValF)) then 
      if (isanom .or. \
        (isdefined(rescf@cnMinLevelValF) .and. isdefined(rescf@cnMaxLevelValF)\ 
       .and. (rescf@cnMinLevelValF .eq. (-1*rescf@cnMaxLevelValF)))) then 
        res2 = remove_middle_contours(gplot, rescf, 1)
        gplot = gsn_csm_contour_map_ce(wks,mydata,res2)
      end if
      delete(mydata)

      kres = True
      kres@product_dir=opts@product_dir
  
      ; open a new KML document
      kml = kml_open_document(wks_name, wks_name, kres) ; open new kml document
      kml = kml_add_lookat( kml, doubletofloat(centerLonF), doubletofloat(centerLatF), 0.0, 4616152, kres)
  
      kml = make_kml_image(kml, name, wks, gplot, kres, rescf, True)
        
      wksname = wks@name
      delete(wks)
      ;kres@kmlCropAlphaColor = "white"
      ; crops the groundoverlay map and converts it to png
      ; must "delete(wks)" before this command
      icon = cropKmlImage(wksname, ".png")
      
      ; add cropped map to KML document as a GroundOverlay element
      kml = kml_add_groundoverlay(kml, name, icon, LatLonBox, 0, kres)
        
      ; add cropped map filename to list of files to be 
      ; compressed in KMZ archive
      kml_add_kmzfile(kml,icon)

      if (numfiles .gt. 1) then
        do i = 1,numfiles-1

          if ((.not. ismissing(plottitles(i))) .and. (plottitles(i) .ne. "")) then
            name        = plottitles(i)
          else
            name = "Plot "+i
          end if 
      
          wks = gsn_open_wks(wks_type,wks_name+i) 
          ;drawNDCGrid(wks)
          gsn_define_colormap(wks, opts@colormap)
        
          ;; handle precip
          ;;if (data@units .eq. "mm/day") then
          if (opts@invert_cmap) then
            if (.not. isanom .and. .not. dodiff) then
              rescf@gsnSpreadColorEnd = -2
              newindex = NhlNewColor(wks,1.0,1.0,1.0)
              rescf@gsnCnMissingColor = newindex
            end if
            gsn_reverse_colormap(wks)
          end if
          ;;mydata = files(i,:,:)
          mydata = files[i]
          mydata = mask_data(mydata,masktype,maskdata)
          gplot = gsn_csm_contour_map_ce(wks,mydata,rescf)
          ;if (isanom .and. cint .eq. 0) then
          if (isanom .or. \
            (isdefined(rescf@cnMinLevelValF) .and. isdefined(rescf@cnMaxLevelValF) \ 
            .and. (rescf@cnMinLevelValF .eq. (-1*rescf@cnMaxLevelValF)))) then 
            res2 = remove_middle_contours(gplot, rescf, 1)
            gplot = gsn_csm_contour_map_ce(wks,mydata,res2)
          end if
          delete(mydata)

          kml = make_kml_image(kml, name, wks, gplot, kres, rescf, False)

          wksname = wks@name
          delete(wks)
        
          ; crops the groundoverlay map and converts it to png
          ; must "delete(wks)" before this command
          icon = cropKmlImage(wksname, ".png")
      
          ; add cropped map to KML document as a GroundOverlay element
          kml = kml_add_groundoverlay(kml, name, icon, LatLonBox, 0, kres)
        
          ; add cropped map filename to list of files to be 
          ; compressed in KMZ archive
          kml_add_kmzfile(kml,icon)
        end do
      end if
  
      kml_folder=kml_close_folder(kml) ; close the folder
        
      ; close kml document
      kml = kml_close_document(kml)
  
      ; write kml document to file 
      kml_write(kml)
  
      ; zip kml file and overlay images together in to "example05.kmz"
      kml_make_kmz(kml)
    end if
  end if
  
  if (doGoogle) then

    if (dodiff) then
      ;print("doGoogle, doDiff");
      kres = True
        
      kres@product_dir=opts@product_dir
  
      ; open a new KML document
      kml = kml_open_document(wks_name, wks_name, kres) ; open new kml document
      kml = kml_add_lookat( kml, doubletofloat(centerLonF), doubletofloat(centerLatF), 0.0, 4616152, kres)
  
      if ((.not. ismissing(plottitles(0))) .and. (plottitles(0) .ne. "")) then
        name        = plottitles(0)
      else
        name = "Plot "+i
      end if 

      kml = make_kml_image(kml, name, wks, plots(0), kres, rescf, True)

      ; "delete(wks)" can not be called within a function, 
      ; need to find a work around for this
      wksname = wks@name
      delete(wks)
        
      ; crops the groundoverlay map and converts it to png
      ; must "delete(wks)" before this command
      ;icon = kml_crop_plot(cropbox, kres)
      icon = cropKmlImage(wksname, ".png")
      
      ; add cropped map to KML document as a GroundOverlay element
      kml = kml_add_groundoverlay(kml, "GroundOverlay", icon, LatLonBox, 0, kres)
        
      ; add cropped map filename to list of files to be compressed in KMZ archive
      kml_add_kmzfile(kml,icon)
        
      kml_folder=kml_close_folder(kml) ; close the folder

      ; close kml document
      kml = kml_close_document(kml)
  
      ; write kml document to file 
      kml_write(kml)
  
      ; zip kml file and overlay images together in to "example05.kmz"
      kml_make_kmz(kml)
    end if
      
  else
    if (doPanel) then

      ;---Create resources for paneling
      resP                            = True
      resP@gsnMaximize                = True    
      resP@gsnPaperOrientation = "portrait"
      resP@gsnPanelLabelBar           = True    
      resP@txFontHeightF              = .015
      resP@txFontThicknessF=.8
      if (isatt(opts,"panel_title")) then
        resP@txString = opts@panel_title
      end if

      ; label bar
      resP@lbOrientation         = "Horizontal"   
      resP@lbLabelAutoStride     = "True"   
      ;resP@pmLabelBarWidthF      = 0.4               ; default is shorter
      ;resP@pmLabelBarHeightF     = 0.075             ; default is taller
      resP@pmLabelBarWidthF      = 0.5               ; default is shorter
      resP@pmLabelBarHeightF     = 0.060             ; default is taller
      resP@lbTopMarginF          = 0.30
      resP@lbLabelFontHeightF    = 0.013;

      ; label bar title
      ;resP@lbTitleString = rescf@lbTitleString
      ;resP@lbTitleFontHeightF = 0.013
      ;resP@lbTitlePosition = "Bottom"
      ;resP@lbTitleOffsetF = 0.005
      ;if (opts@isanom) then   ; add clim string as label bar title
      if (opts@isanom .and. ismissing(str_index_of_substr(str_lower(vartitle),"anom",-1))) then
        climText = "Anomaly relative to "+opts@climId+opts@climyears+" average"
        resP@lbTitleString = climText
        resP@lbTitleFontHeightF = 0.01
        resP@lbTitlePosition = "Bottom"
        resP@lbTitleOffsetF = 0.2
      end if

      resP@gsnFrame = False ; Don't advance frame for annotation overlay
      resP@gsnPanelDebug = False ; set to True for debugging

      ;---Scale plots based on one of the smaller plots, and not the first one.
      ;resP@gsnPanelScalePlotIndex     = 1

      ;---Add some extra space between plots since they are too close together
      resP@gsnPanelXWhiteSpacePercent = 2
      resP@gsnPanelYWhiteSpacePercent = 3

      ;---Leave some room for the leftmost and rightmost tickmark labels.
      resP@gsnPanelLeft               = 0.05
      resP@gsnPanelRight              = 0.95
      resP@gsnPanelCenter = False

      ;---figure out how to lay out the panels
      numPanels = dimsizes(plots)
      ncol = floattointeger(sqrt(numPanels))
      nrow = (numPanels/ncol)+mod(numPanels,ncol)
      panels = (/nrow,ncol/)
      ;if (opts@isanom) then
      ;  resP@gsnPanelBottom    = 0.05
      ;end if
      ;;print(panels)
      ;---Panel the plot.
      gsn_panel(wks,plots,panels,resP)

    else 

      draw(plots(0))

    end if
    frame(wks)
    wksname = wks@name
    delete(wks)
    delete(plots)
    image = decorateImage(wksname, wks_type, opts)
  end if

end

;************************************************
; Procedure: plotcorr
;
; Purpose: plot correlation/regression map(s)
;
; Params:  files  - the data files to plot
;          wks_name - workstation name
;          opts - allow passing optional args
;***********************************************
undef("plotcorr")
procedure plotcorr(files[*]:string, wks_name:string, opts:logical)

local tsdata, ncdata, rawdata, vals, plotData, corr, cdata

begin
  numfiles = dimsizes(files);
  ;;print("numfiles: " + numfiles)
  csvidx = 0
  pd = opts@product_dir
  do i = 0,numfiles-1
    onefile = pd + "/" + files(i)
    suffix = get_file_suffix(onefile, 0)
    if (suffix .eq. ".csv") then
      ;print("reading csv file")
      csvidx = i
      lines = asciiread(onefile, -1, "string")
      nlines = dimsizes(lines)
      ncols = 2
      nl = 0;
      ;; pare off the comments
      do while (nl.lt.nlines)
        first = str_get_cols(lines(nl),0,0)
        if (first .eq. "#") then
          nl = nl +1
          continue
        else
          vals = lines(nl:nlines-1)
          ;printVarSummary(vals);
          break
        end if
      end do
      ;; now split it apart
      dates = str_get_field(vals, 1, ",")
      year   = stringtointeger(str_get_cols(dates,0,3))
      month  = stringtointeger(str_get_cols(dates,5,6))
      day    = stringtointeger(str_get_cols(dates,8,9))
      hour   = stringtointeger(str_get_cols(dates,11,12))
      minute = stringtointeger(str_get_cols(dates,14,15))
      sec    = stringtointeger(str_get_cols(dates,17,18))
      units  = "hours since 1901-01-01 00:00:00"

      time   = ut_inv_calendar(year,month,day,hour,minute,sec, units, 0)

      time!0          = "time"
      time@long_name  = "time"
      time@description= "time"
      time@units      =  units
      time&time       =  time        ; make coordinate variable
      ;printVarSummary(time)
      values = stringtofloat(str_get_field(vals, 2, ","))
      values!0        = "time"
      values&time     = time
      ;printVarSummary(values)
      break
    end if
  end do
  ncidx = 0;
  numplots = numfiles-1;
  if (numplots .gt. 1 .and. opts@ensavg) then
     numplots = numplots+1
  end if
  ;;print("numplots = " +numplots)
  ;;if (numplots .gt. 2) then
     newtitles = new(numfiles,string)
  ;;else
  ;;   newtitles = new(1,string)
  ;;end if
  firstnc = 1-csvidx
  if (opts@outtype .eq. "regression") then
     typeLabel = "Regression"
  else
     typeLabel = "Correlation"
  end if
  do nc = 0,numfiles-1
    if (nc .eq. csvidx) then
      ncidx = ncidx + 1
      continue
    end if
    ;print("reading nc file: " + files(ncidx) + " " + nc + "," + ncidx)
    ncdata = readdata(files(ncidx), opts)
    ;printVarSummary(ncdata)
    ncVarName = ncdata@long_name
    if (opts@outtype .eq. "regression") then
      corr = regCoef_n(values,ncdata,0,0)
      corr@long_name=ncVarName + " Regression"
      if (numfiles .gt. 2) then
        newtitles(nc-1) = opts@ens(ncidx-1)
      else
        newtitles(nc-1) = opts@titles(ncidx) + "~C~"+typeLabel+" with " + opts@titles(csvidx)
      end if
    else
      corr = escorc_n(values,ncdata,0,0)
      corr@long_name=ncVarName + " Correlation"
      if (numfiles .gt. 2) then
        newtitles(nc-1) = opts@ens(ncidx-1)
      else
        newtitles(nc-1) = opts@titles(ncidx) + "~C~"+typeLabel+" with " + opts@titles(csvidx)
      end if
    end if
    corr!0  = "lat"
    corr!1  = "lon"
    corr&lat = ncdata&lat
    corr&lon = ncdata&lon
    corr@units=""
    
    ;printVarSummary(corr)
    ;printMinMax(corr,True)
    if (.not. isdefined("cdata")) then
      shape = dimsizes(corr)
      ;;if (numfiles .eq. 2) then
      ;;   cdata = new((/1,shape(0),shape(1)/),float)
      ;;else
         cdata = new((/numplots,shape(0),shape(1)/),float)
      ;;end if
    end if
    ;; name the dimension to avoid warning message
    cdata!0 = "filenum"
    cdata(ncidx-1,:,:) = corr
    ;printVarSummary(cdata)
    ncidx = ncidx + 1
    delete(corr)
  end do
  ;;if (numfiles .gt. 2) then
  if (numplots .ge. 2 .and. opts@ensavg) then
    cdata(numplots-1,:,:) = dim_avg_n_Wrap(cdata(0:numplots-2,:,:),0)
    newtitles(numplots-1) = "Average "+typeLabel
  end if
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
  titleVarName = setvartitle(ncdata, opts, False, level, levelunit)
  if (numfiles .gt. 2) then
    opts@panel_title = titleVarName + " " + opts@models(firstnc) + " " + opts@exps(firstnc) + " " + opts@dates(firstnc) + "~C~"+typeLabel+" with " + opts@titles(csvidx)
  else
    opts@panel_title = newtitles(0)
  end if
  delete(opts@titles)
  opts@titles=newtitles
  if (numfiles .gt. 2) then
    opts@leftstring=newtitles
  end if

  ;print("calling plotmap")
  plotData = NewList("fifo")
  do i = 0,numplots-1
    ListAppend(plotData,replicate_var(cdata(i,:,:)))
  end do
  plotmap(plotData,wks_name,opts)
  delete(csvidx)
  delete(ncidx)
  delete(cdata)
  delete(shape)
  
end 

;************************************************
; Procedure: makePdfPlot
;
; Purpose: plot probability distribution
;
; Params:  files  - the data files to plot
;          wks_name - workstation name
;          opts - allow passing optional args
;***********************************************
undef("makePdfPlot")
procedure makePdfPlot(pdffiles[*]:string, wks_name:string, opts:logical)

begin

  wks_type = opts@imageformat
  if (wks_type .eq. "gif") then
    wks_type = "png"
  end if
  numpdfs = dimsizes(pdffiles)
  outfile = pdffiles(0)
  ;asciiwrite(outfile,pdffiles)
  rscript = new((/40/),string)
  if (isatt(opts,"rpath")) then
    if ((wks_type .eq. "png") .or. (wks_type .eq. "gif")) then
      rscript(0) = "png(filename = '"+wks_name+".png', width = 800, height = 800, units = 'px', pointsize = 12, bg = 'white',  res = NA, type = c('cairo', 'cairo-png', 'Xlib', 'quartz'))"
      c1 = "2."
      c2 = "1.25"
      lwd = "3"
    else 
      c1 = "1.25"
      c2 = ".75"
      lwd = "2"
      if ((wks_type .eq. "ps") .or. (wks_type .eq. "eps")) then
        rscript(0) = "postscript(paper='letter','"+wks_name+"."+wks_type+"',width=7.,height=7., horizontal=FALSE)"
      else
        rscript(0) = "pdf(paper='letter','"+wks_name+".pdf',width=7.,height=7.)"
      end if
    end if
    rscript(1) = "# R script for plotting density function"
    rscript(2) = "library(sm)"
    rscript(3) = "#  Read in the data"
    rscript(4) = "data<-matrix(scan(file='"+outfile+"'),ncol=1,byrow=TRUE)"
    rscript(5) = "par(cex="+c1+")"
    rscript(6) = "par(lwd="+c1+")"
    rscript(7) = "sm.options(rugplot=FALSE)"
    rscript(8) = "options(na.action=na.exclude)"
    linenum=9
    if (numpdfs .gt. 1) then
        outfile2 = pdffiles(1)
        rscript(linenum) = "data2<-matrix(scan(file='"+outfile2+"'),ncol=1,byrow=TRUE)"
        linenum=linenum+1
   end if

    rscript(linenum) = "# Make the density function"
    if (opts@xmax .eq. 0 .and. opts@xmin .eq. 0) then
      xlims = ""
    else
      xlims = ",xlim=c("+opts@xmin+","+opts@xmax+")"
    end if
    if (opts@ymax .eq. 0 .and. opts@ymin .eq. 0) then
      ylims = ""
    else
      ylims = ",ylim=c("+opts@ymin+","+opts@ymax+")"
    end if
    if (xlims .eq. "") then
      rscript(linenum+1) = "a<-sm.density(data[,1],display='none')"
      rscript(linenum+2) = "a<-cbind(a$eval.points,a$estimate)"
      if (numpdfs .gt. 1) then
          rscript(linenum+3) = "a2<-sm.density(data2[,1],display='none')"
          rscript(linenum+4) = "a2<-cbind(a2$eval.points,a2$estimate)"
          rscript(linenum+5) = "b<-min(a[,1],a2[,1])"
          rscript(linenum+6) = "c<-max(a[,1],a2[,1])"
          linenum=linenum+7
      else
          rscript(linenum+3) = "b<-min(a[,1])"
          rscript(linenum+4) = "c<-max(a[,1])"
          linenum=linenum+5
      end if
      if (opts@isanom) then
          rscript(linenum) = "d<-max(abs(b),c)"
          xlims = ",xlim=c(-d,d)"
          linenum=linenum+1
      else
          xlims = ",xlim=c(b,c)"
      end if
    end if
    vartitle = opts@varTitle
    rscript(linenum) = "a<-sm.density(data[,1],col='black',lwd="+lwd+",ylab='Density',xlab='"+vartitle+"'"+xlims+ylims+")"
    rscript(linenum+1) = "a<-cbind(a$eval.points,a$estimate)"
    rscript(linenum+2) = "rug(data[,1],lwd=1.,col='black',side=1)"
    if (opts@isanom) then  ; add a line at 0
        rscript(linenum+3) = "abline(v=0.0,lwd=2,col='black')"
        linenum = linenum+3
    else
        linenum = linenum+2
    end if
    rscript(linenum+1) = "sm.options(add=TRUE)"
    if (numpdfs .gt. 1) then
        rscript(linenum+2) = "a2<-sm.density(data2[,1],col='red',lwd="+lwd+xlims+ylims+")"
        rscript(linenum+3) = "a2<-cbind(a2$eval.points,a2$estimate)"
        rscript(linenum+4) = "rug(data2[,1],lwd=1.,col='red',side=1)"
        linenum = linenum+3
    end if
    rscript(linenum+2) = "title(main='"+opts@mainTitle+"',line=2., cex.main=1.2, font.main=1)"
    rscript(linenum+3) = "title(main='"+opts@regionTitle+"',line=1.2, cex.main=1.0, font.main=1)"
    ;if (opts@isanom .and. ) then   ; add multiple titles
    if (opts@isanom .and. ismissing(str_index_of_substr(str_lower(vartitle),"anom",-1))) then
      climText = "Anomaly relative to "+opts@climId+opts@climyears+" average"
      rscript(linenum+4) = "title(main='"+climText+"',line=.4,cex.main=0.85, font.main=1)"
      linenum = linenum+4
    else
      linenum = linenum+3
    end if
    if (opts@pdfstat .ne. "none") then
      rscript(linenum+1) = "stat1<-"+opts@pdfstat+"(data[,1])"
      rscript(linenum+2) = "abline(v=stat1,lwd=1.5,col='black',lty=3)"
      linenum = linenum+2
    end if
    if (numpdfs .gt. 1) then
      if (opts@pdfstat .ne. "none") then
        rscript(linenum+1) = "stat2<-"+opts@pdfstat+"(data2[,1])"
        rscript(linenum+2) = "abline(v=stat2,lwd=1.5,col='red',lty=3)"
        linenum = linenum+2
      end if
    end if
    ;;;  legend
    rscript(linenum+1) = "par(cex="+c2+")"
    label1 = ""
    label2 = ""
    if (numpdfs .gt. 1) then
      ;label1 = opts@models(0)+" "+opts@exps(0)
      ;label2 = opts@models(1)+" "+opts@exps(1)
      if (opts@dates(0) .ne. opts@dates(1)) then
        label1 = label1 + " " + opts@dates(0)
        label2 = label2 + " " + opts@dates(1)
      end if
      if (opts@exps(0) .ne. opts@exps(1)) then
         label1 = opts@exps(0) + " " + label1
         label2 = opts@exps(1) + " " + label2
      end if
      if (opts@models(0) .ne. opts@models(1)) then
         label1 = opts@models(0) + " " + label1
         label2 = opts@models(1) + " " + label2
      end if
      if (opts@pdfstat .ne. "none") then
        rscript(linenum+2) = "l1 = paste0('"+label1+" "+opts@pdfstat+": ',format(stat1,digits=3))"
        rscript(linenum+3) = "l2 = paste0('"+label2+" "+opts@pdfstat+": ',format(stat2,digits=3))"
      else 
        rscript(linenum+2) = "l1<-'"+label1+"'"
        rscript(linenum+3) = "l2<-'"+label2+"'"
      end if
      rscript(linenum+4) = "legend('topleft',lty=c(1,1),lwd="+lwd+",bty='n',col=c('black','red'),c(l1,l2))"
      linenum = linenum+4
    else
      if (opts@pdfstat .ne. "none") then
        rscript(linenum+2) = "l1 = paste0('"+label1+" "+opts@pdfstat+": ',format(stat1,digits=3))"
        rscript(linenum+3) = "legend('topleft',lty=1,lwd=3,bty='n',col='black',l1)"
        linenum = linenum+3
      else
        linenum = linenum+1
      end if
    end if
    rscript(linenum+1) = "dev.off()"
    do i = linenum+2,39
      rscript(i) = "#"
    end do
    scriptName = opts@product_dir+"/pdfscript.r"
    asciiwrite(scriptName,rscript)
    system(opts@rpath+" "+scriptName);
  else
    
    pdfres = True
    pdfres@bin_nice = "True"
    alldata = asciiread(pdffiles(0),-1,"float")
    zpdf = pdfx(alldata, 0, pdfres)
    nbins = zpdf@nbins
    if (numpdfs .eq. 1) then
      xx = zpdf@bin_center
      yy = zpdf
    else
      delete(alldata)
      xx = new ( (/2,nbins/), typeof(zpdf))
      yy = new ( (/2,nbins/), typeof(zpdf))
      xx(0,:) = zpdf@bin_center
      yy(0,:) = (/zpdf/)
      alldata = asciiread(pdffiles(1),-1,"float")
      zpdf2 = pdfx(alldata, nbins, pdfres)
      xx(1,:) = zpdf2@bin_center
      yy(1,:) = (/zpdf2/)
    end if
        
    ;printVarSummary(zpdf)
  
    wks = gsn_open_wks(wks_type,wks_name) 
    res  = True
    res@gsnDraw           = False         ; Don't draw plots (for overlay)
    res@gsnFrame          = False         ; don't advance frame yet
    res@tiXAxisString            = opts@varTitle
    res@tiXAxisFontHeightF = 0.015
    res@tmXBLabelFontHeightF   = 0.015
    if (opts@isanom) then
      pdfmax = max(alldata)
      pdfmin = min(alldata)
      range = max((/pdfmax,abs(pdfmin)/))
      res@trXMinF = -range
      res@trXMaxF = range
    end if
  
    res@tiYAxisString            = "PDF (%)"
    res@tiYAxisFontHeightF = 0.015
    res@tmYLLabelFontHeightF   = 0.015
    res@trYMinF = 0
    res@xyLineThicknessF = 2.0
    res@gsnXYBarChart            = True              ; Create bar plot
    res@gsnXYBarChartOutlineOnly = True
    if (numpdfs .gt. 1) then
      res@pmLegendDisplayMode    = "Always"            ; turn on legend
      res@pmLegendSide           = "Top"               ; Change location of
      res@pmLegendParallelPosF   = .20                 ; move units right
      res@pmLegendOrthogonalPosF = -0.12               ; move units down
      res@pmLegendWidthF         = 0.12                ; Change width and
      res@pmLegendHeightF        = 0.05                ; height of legend.
      res@lgPerimOn              = True                ; turn off/on box around
      res@lgLabelFontHeightF     = .010                ; label font height
      label1 = opts@models(0)+" "+opts@exps(0)
      label2 = opts@models(1)+" "+opts@exps(1)
      res@xyExplicitLegendLabels = (/label1,label2/)  ; create explicit labels
      res@xyLineColor = "Blue"
      res@xyLineThicknesses        = (/2.0,2.0/)
      res@xyLineColors             = (/"black","red"/)

    else
      res@xyLineColor = "Black"
    end if

    plot = gsn_csm_xy (wks, xx, yy, res)
    if (opts@isanom) then
      ;print("plotting polyline")
      getvalues plot
        "trYMaxF" : maxY 
      end getvalues
      resp = True
      resp@gsLineColor = "Black"
      lnid = gsn_add_polyline(wks,plot,(/0,0/),(/0,maxY/),resp)
    end if
  ;
  ; Attach some titles at the top.
  ;
    res_text               = True
    res_text@txFontHeightF = 0.02                      ; change font size
    txid_top = gsn_create_text(wks, opts@mainTitle, res_text)
  
    amres                  = True
    amres@amJust           = "BottomCenter"
    amres@amParallelPosF   =  0.0    ; This is the center of the plot.
    amres@amOrthogonalPosF = -0.61   ; This is above the top edge of the plot.
    annoid_top = gsn_add_annotation(plot, txid_top, amres)
  
    res_text@txFontHeightF = 0.018                     ; change font size
    txid_mid = gsn_create_text(wks, opts@regionTitle, res_text)
  
    amres@amOrthogonalPosF = -0.56  ; This is just below the previous title.
    annoid_mid = gsn_add_annotation(plot, txid_mid, amres)
  
    if (opts@isanom) then  
      res_text@txFontHeightF = 0.012                     ; change font size
      climText = "Anomaly relative to "+opts@climId+opts@climyears+" average"
      txid_bot = gsn_create_text(wks, climText, res_text)
  
      amres@amOrthogonalPosF = -0.525 ; This is just below the previous title.
      annoid_bot = gsn_add_annotation(plot, txid_bot, amres)
    end if
    draw(plot)
    frame(wks)
    wksname = wks@name
    delete(wks)
  end if
  wksname = wks_name
  image = decorateImage(wksname, wks_type, opts)
end

;************************************************
; Function: aavg
;
; Purpose: area weighted average
;
; Params:  data  - the data 
;***********************************************
undef("aavg")
function aavg(data:float)
;function aavg(data[*][*]:float)

begin
   pi = 4.*atan(1.0)
   rad = (pi/180.)
   coslat = cos(data&lat*rad);
   aadata = wgt_areaave_Wrap(data,coslat,1.0,0)
   return aadata
end

;************************************************
; Procedure: plotpdf
;
; Purpose: plot probability distribution
;
; Params:  files  - the data files to plot
;          wks_name - workstation name
;          opts - allow passing optional args
;***********************************************
undef("plotpdf")
procedure plotpdf(files[*]:string, wks_name:string, opts:logical)

begin
  if (isatt(opts,"mask") .and. .not.(opts@mask .eq. "none")) then
     a = addfile("$NCARG_ROOT/lib/ncarg/data/cdf/landsea.nc","r")
     maskdata = a->LSMASK
     masktype = opts@mask
  else
     maskdata = new((/1,1/),byte)
     masktype = "none"
  end if
  numpdfs = dimsizes(files)
  pdffiles = new((/numpdfs/),string)
  do pdf = 0,numpdfs-1
    ncfiles = str_split(files(pdf), ",")
    data = readdata(ncfiles(0), opts)
    if (dimsizes(getvardims(data)) .gt. 2 .and. opts@average_time) then
       tmpdata = dim_avg_n_Wrap(data,0)
       delete(data)
       data = tmpdata
       delete(tmpdata)
    end if
    mdata = mask_data(data,masktype,maskdata)
    aadata = aavg(mdata)
    ;printVarSummary(aadata)
    numfiles = dimsizes(ncfiles)
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
      tmpdata = readdata(ncfiles(i), opts)
      if (dimsizes(getvardims(tmpdata)) .gt. 2 .and. opts@average_time) then
         tmptmpdata = dim_avg_n_Wrap(tmpdata,0)
         delete(tmpdata)
         tmpdata = tmptmpdata
         delete(tmptmpdata)
      end if
      if (.not. ismissing(str_index_of_substr(ncfiles(i), "_mean", -1))) then
        opts@meanIndex = i
      end if
      mdata = mask_data(tmpdata,masktype,maskdata)
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
    if ( isatt(data,"units")) then
      if (.not. (opts@isanom .and. ((opts@anomtype .eq. "stdanom") .or. (opts@anomtype .eq. "pctanom"))) ) then
        varTitle = varTitle + " ("+data@units+")"
      end if
    end if
    opts@varTitle=varTitle
    delete(data)
    if (isatt(opts,"meanIndex") .and. (numfiles .gt. 1)) then
      dims = dimsizes(alldata);
      newAlldata = new((/dims(0)-1,dims(1)/),float)
      meanIndex = opts@meanIndex
      aIndex = 0;
      do i = 0,numfiles-1
        if (i .eq. meanIndex) then
          continue
        end if
        newAlldata(aIndex,:) = alldata(i,:)
        aIndex = aIndex+1
      end do
      delete(alldata)
      alldata = newAlldata
      delete(newAlldata)
    end if
    pdffile = opts@models(pdf) + "_" + opts@exps(pdf) + "_" + opts@dates(pdf)
    pdffile = str_sub_str(pdffile," ","_")
    outfile = opts@product_dir+"/"+pdffile+"_pdfvalues.txt"
    asciiwrite(outfile,alldata)
    pdffiles(pdf) = outfile
    delete(ncfiles)
    delete(alldata)
    delete(opts@meanIndex)
  end do
  if (numpdfs .gt. 1) then
    if (opts@dates(0) .ne. opts@dates(1)) then
      title = ""
    else
      title= opts@dates(0)
    end if
    if (opts@exps(0) .eq. opts@exps(1)) then
      title = opts@exps(0)+" "+title
    end if
    if (opts@models(0) .eq. opts@models(1)) then
      title = opts@models(0)+" "+title
    end if
  else
    title= opts@models(0) + " " + opts@exps(0) + " " + opts@dates(0)
  end if
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
  makePdfPlot(pdffiles, wks_name, opts)

end

;************************************************
; Procedure: plotcomp
;
; Purpose: plot map(s)
;
; Params:  files  - the data files to plot
;          wks_name - workstation name
;          opts - allow passing optional args
;***********************************************
undef("plotcomp")
procedure plotcomp(files[*]:string, wks_name:string, opts:logical)
local data, numfiles, onetime, alldata, doPanel, isanom

begin
  numfiles = dimsizes(files)
  data = readdata(files(0), opts)
  if (dimsizes(getvardims(data)) .gt. 2) then
     if (opts@stattype .eq. "sum") then
       atmpdata = dim_sum_n_Wrap(data, 0);
     else
       atmpdata = dim_avg_n_Wrap(data, 0);
     end if
     delete(data)
     onetime = atmpdata
     delete(atmpdata)
  else
     onetime = data(0,:,:)
  end if
  xys = dimsizes(onetime)
  outtype = opts@outtype
  numdatas = numfiles
  if (numfiles .gt. 1 .and. ((outtype .eq. "multicomp" .or. outtype .eq. "enscomp") .and. opts@ensavg)) then
    numdatas = numfiles +1
  end if
  ;;alldata = new((/numdatas,xys(0),xys(1)/),float)
  ;;alldata(0,:,:) = onetime
  alldata = NewList("fifo")
  ListAppend(alldata,onetime)
  if (numfiles .gt. 2 .or. ((outtype .eq. "multicomp" .or. outtype .eq. "enscomp") .and. numfiles .gt. 1)) then
     newtitles = new(numdatas,string)
      if (outtype .eq. "enscomp") then
        newtitles(0) = opts@ens(0)
      else
        newtitles(0) = opts@models(0)
      end if 
  else
     longnames = new(2,string)
     if (isatt(onetime,"long_name")) then
       longnames(0) = onetime@long_name
     else
       longnames(0) = opts@variable
     end if
     newtitles = new(2,string)
     newtitles(0) = opts@titles(0)
  end if
  ;print(numfiles)
  do i = 1,numfiles-1
    ;print(opts@titles(i))
    tmpdata := readdata(files(i), opts)
    ;printVarSummary(tmpdata)
    if (dimsizes(getvardims(tmpdata)) .gt. 2) then
       if (opts@stattype .eq. "sum") then
         ListAppend(alldata,dim_sum_n_Wrap(tmpdata, 0));
       else
         ListAppend(alldata,dim_avg_n_Wrap(tmpdata, 0));
       end if
       ;;atmpdata = dim_avg_n_Wrap(tmpdata, 0);
       ;;delete(tmpdata)
       ;;tmpdata = atmpdata
       ;;delete(atmpdata)
    else
     ListAppend(alldata,replicate_var(tmpdata))
    end if
    ;;alldata(i,:,:) = tmpdata
    ;;ListAppend(alldata,tmpdata)
    if (numfiles .gt. 2 .or. ((outtype .eq. "multicomp" .or. outtype .eq. "enscomp") .and. numfiles .gt. 1)) then
      if (outtype .eq. "enscomp") then
        newtitles(i) = opts@ens(i)
      else
        newtitles(i) = opts@models(i)
      end if 
    else
      if (isatt(tmpdata,"long_name")) then
        longnames(i) = tmpdata@long_name
      else
        longnames(i) = opts@variable
      end if
      newtitles(i) = opts@titles(i)
    end if
    ;;delete(tmpdata)
  end do
  if (numfiles .gt. 1 .and. ((outtype .eq. "multicomp" .or. outtype .eq. "enscomp") .and. opts@ensavg)) then
    ;;alldata(numdatas-1,:,:) = dim_avg_n_Wrap(alldata(0:numfiles-1,:,:),0)
    numgrids = ListCount(alldata)
    avgdata = new((/numgrids,xys(0),xys(1)/),float)
    do i = 1,numgrids-1
      avgdata(i,:,:) = remapData(onetime, alldata[i])
    end do
    ListAppend(alldata, dim_avg_n_Wrap(avgdata(0:numfiles-1,:,:),0))
    if (outtype .eq. "multicomp") then
      newtitles(numdatas-1) = "Multi-Model Mean"
    else
      newtitles(numdatas-1) = "Ensemble Average"
    end if
  end if
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
  titleVarName = setvartitle(onetime, opts, False, level, levelunit)
  if (numfiles .gt. 2 .or. ((outtype .eq. "multicomp" .or. outtype .eq. "enscomp"))) then
    if (outtype .eq. "enscomp") then  ; ensemble comp
      panel_title = titleVarName + " " + opts@models(0) + " " + opts@exps(0) + " " + opts@dates(0)
    else ; model comp 
      panel_title = titleVarName + " " + opts@exps(0) + " " + opts@ens(0) + " " + opts@dates(0)
    end if
    opts@panel_title = adjustTitle(panel_title)
  end if
  delete(opts@titles)
  opts@titles=newtitles
  if (isdefined("longnames")) then
     opts@longnames=longnames
  end if
  if (numfiles .gt. 2 .or. ((outtype .eq. "multicomp" .or. outtype .eq. "enscomp") .and. numfiles .gt. 1)) then
    opts@leftstring=newtitles
  end if
   ;printVarSummary(alldata)
   ;print("numdatas "+ListCount(alldata))
   plotmap(alldata,wks_name,opts)
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

  plotType=getenv("plot_type")
  if (ismissing(plotType)) then
    plotType = "png"
  end if
  
  imageFormat=getenv("image_format")
  if (ismissing(imageFormat)) then
    imageFormat = "gif"
  end if
  
  outType=getenv("output")
  if (ismissing(outType)) then
    outType = "comp"
  end if


  tmpfiles=getenv("ncfiles")
  if (ismissing(tmpfiles)) then 
    print("no files")
    status_exit(-2)
  end if

  if ((outType .eq. "enscomp") .and. (plotType .eq. "pdf")) then
     ncfiles = str_split(tmpfiles, "|")
  else
     ncfiles=str_split(tmpfiles,",")
  end if
  
  ;titles
  tmptitles=getenv("titles")
  if (ismissing(tmptitles)) then 
    numfiles = dimsizes(ncfiles)
    titles = new((/numfiles(0)/),string)
  else 
    titles=str_split(tmptitles,";")
  end if
  
  ;models
  tmpmodels=getenv("models")
  if (ismissing(tmpmodels)) then 
    numfiles = dimsizes(ncfiles)
    models = new((/numfiles(0)/),string)
  else 
    models=str_split(tmpmodels,";")
  end if
  
  ;experiments
  tmpexps=getenv("exps")
  if (ismissing(tmpexps)) then 
    numfiles = dimsizes(ncfiles)
    exps = new((/numfiles(0)/),string)
  else 
    exps=str_split(tmpexps,";")
  end if
  
  ;ensembles
  tmpens=getenv("ens")
  if (ismissing(tmpens)) then 
    numfiles = dimsizes(ncfiles)
    ens = new((/numfiles(0)/),string)
  else 
    ens=str_split(tmpens,";")
  end if
  
  ;ensembles
  tmpdates=getenv("dates")
  if (ismissing(tmpdates)) then 
    numfiles = dimsizes(ncfiles)
    dates = new((/numfiles(0)/),string)
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
  
  stattype=getenv("stat")
  if (ismissing(stattype)) then
    stattype = "mean"
  end if
  
  colormap=getenv("colormap")
  if (ismissing(colormap)) then
    if (isanom .or. outType .eq. "diff") then
      colormap = "testcmap"
    else
      colormap = "rainbow"
    end if
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
  
  ;
  ; contour parameters
  temp = getenv("cint")
  if (.not. ismissing(temp)) then
    cint = stringtofloat(temp)
  else
    cint = 0.
  end if
  cmin=stringtofloat(getenv("cmin"))
  cmax=stringtofloat(getenv("cmax"))

  polar=getenv("usepolar")
  if (.not. ismissing(polar)) then
    usepolar = polar .eq. "true"
  else
    usepolar = False
  end if
  
  temp = getenv("meridian")
  if (.not. ismissing(temp)) then
    meridian = stringtofloat(temp)
  else
    meridian = 0.
  end if
  if (meridian .gt. 180) then
     meridian = meridian - 360
  end if

  temp =getenv("cfill")
  if (.not. ismissing(temp)) then
    cfill = temp .eq. "true"
  else
    cfill = True
  end if
  
  climyears=getenv("climyears")
  if (ismissing(climyears)) then
    climyears = "1981-2010"
  end if
  
  temp=getenv("climdataId")
  if (ismissing(temp)) then
    climId = ""
  else
    climId=temp+" "
  end if
  
  linecolor=getenv("linecolor")
  if (ismissing(linecolor)) then
    linecolor = "black"
  end if
  
  ;
  ; X-Axis parameters
  temp=getenv("xmin")
  if (ismissing(temp)) then
    xmin = 0.
  else
    xmin=stringtofloat(temp)
  end if
  temp=getenv("xmax")
  if (ismissing(temp)) then
    xmax = 0.
  else
    xmax=stringtofloat(temp)
  end if

  ;
  ; X-Axis parameters
  temp=getenv("ymin")
  if (ismissing(temp)) then
    ymin = 0.
  else
    ymin=stringtofloat(temp)
  end if
  temp=getenv("ymax")
  if (ismissing(temp)) then
    ymax = 0.
  else
    ymax=stringtofloat(temp)
  end if

  temp =getenv("ensavg")
  if (.not. ismissing(temp)) then
    ensavg = temp .eq. "true"
  else
    ensavg = True
  end if
  
  temp =getenv("clines")
  if (.not. ismissing(temp)) then
    clines = temp .eq. "true"
  else
    clines = False
  end if
  
  temp = getenv("clabels")
  if (.not. ismissing(temp)) then
    clabels = temp .eq. "true"
  else
    clabels = False
  end if
  
  temp = getenv("reverse_cmap")
  if (.not. ismissing(temp)) then
    reverse_cmap = temp .eq. "true"
  else
    reverse_cmap = False
  end if
  
  temp = getenv("time_average")
  if (.not. ismissing(temp)) then
    time_average = temp .eq. "true"
  else
    time_average = True
  end if
  
  temp = getenv("latlonlines")
  if (.not. ismissing(temp)) then
    latlonlines = temp .eq. "true"
  else
    latlonlines = False
  end if
  
  temp = getenv("pdfstat")
  if (.not. ismissing(temp)) then
    pdfstat = temp
  else
    pdfstat = "none"
  end if
  
  options = True
  options@plottype=plotType
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
  options@colormap = colormap
  options@isanom = isanom
  options@anomtype = anomtype
  options@stattype = stattype
  options@annotation = getenv("annotation")
  options@titles = titles
  options@units=units
  options@cint=cint
  options@cmin=cmin
  options@cmax=cmax
  options@xmin=xmin
  options@xmax=xmax
  options@ymin=ymin
  options@ymax=ymax
  options@usepolar=usepolar
  options@meridian=meridian
  options@logo=logo
  options@cfill=cfill
  options@ensavg=ensavg
  options@clines=clines
  options@clabels=clabels
  options@models=models
  options@exps=exps
  options@ens=ens
  options@dates=dates
  options@invert_cmap=reverse_cmap
  options@climyears=climyears
  options@climId = climId
  options@average_time = time_average
  options@pdfstat = pdfstat
  options@latlonlines = latlonlines

  ; add in the path to R if it is defined
  temp = getenv("rpath")
  if (.not. ismissing(temp)) then
    options@rpath = temp
  end if

  
  if (outType .eq. "correlation" .or. (outType.eq."regression")) then
      ;print("calling plotcorr")
      plotcorr(ncfiles, wks_name, options)
  else 
    if (outType .eq. "enscomp" .and. plotType .eq. "pdf") then
        ;print("calling plotpdf")
        plotpdf(ncfiles, wks_name, options)
    else
        ;print("calling plotcomp")
        plotcomp(ncfiles, wks_name, options)
    end if
  end if
  
  status_exit(0)

end
