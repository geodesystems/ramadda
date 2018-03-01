; Copyright (c) 2010, Ryan Pavlick (http://github.com/rpavlick)
;
; Permission is hereby granted, free of charge, to any person obtaining a copy
; of this software and associated documentation files (the "Software"), to deal
; in the Software without restriction, including without limitation the rights
; to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
; copies of the Software, and to permit persons to whom the Software is
; furnished to do so, subject to the following conditions:
;
; The above copyright notice and this permission notice shall be included in
; all copies or substantial portions of the Software.
;
; THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
; IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
; FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
; AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
; LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
; OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
; THE SOFTWARE.
; *************************************************

;************************************************************
; comments

undef("kml_add_kmzfile")
procedure kml_add_kmzfile ( kml:string, filename:string )
local kml, files
begin

  files = kml@files
  kml@files = files + " " + filename
  
end
; *****************************************************************


;************************************************************
; comments

undef("kml_setline")
procedure kml_setline (kml:string,kml_entry:string)
local kml_entry, nkml_entry
begin
; add lines to kml file

  nkml_entry = dimsizes(kml_entry)
  if(kml@nline+nkml_entry-1.ge.dimsizes(kml))
    print("kml_setline: bad index, not setting anything.")
    return
  end if
  kml(kml@nline:kml@nline+nkml_entry-1) = kml_entry
  kml@nline = kml@nline + nkml_entry
  kml@_FillValue = ""

  return
end
;**************************************************************

;************************************************************
; comments

undef("kml_setline_singletag")
procedure kml_setline_singletag (kml:string,kml_tags[*]:string,res[1]:logical)
local kml_entry, atts, i
begin
; add multiple "single tag" lines to kml file
; e.g. <kml_tags> res@kml$kml_tags$ </kml_tags>

  atts=str_right_strip("kml"+str_capital(kml_tags+" "))
  
  do i=0,dimsizes(kml_tags)-1
    if (isatt(res,atts(i)).and.dimsizes(res@$atts(i)$).eq.1) then
      kml_entry = "<"+kml_tags(i)+">"+res@$atts(i)$+"</"+kml_tags(i)+">"
      kml_setline(kml,kml_entry)
    end if
  end do

  return
end
;**************************************************************

;************************************************************
; comments

undef("kml_setline_xytag")
procedure kml_setline_xytag (kml:string,kml_tags[*]:string,res[1]:logical)
local kml_entry, atts, i, tagsXY, attsXY, j
begin
; add multiple "XY tag" lines to kml file
; e.g. <kml_tags x="res@kml$kml_tags+"x"$" xunits="res@kml$kml_tags+"xunits"$"
;                y="res@kml$kml_tags+"y"$" yunits="res@kml$kml_tags+"yunits"$" </>

  tagsXY=(/"x","y","xunits","yunits"/)

  atts=str_right_strip("kml"+str_capital(kml_tags+" "))

  do i=0,dimsizes(kml_tags)-1
    attsXY=atts(i)+tagsXY
    if (isatt(res,atts(i)).and.dimsizes(res@$atts(i)$).eq.1.and.\
        typeof(res@$atts(i)$).eq."logical".and.res@$atts(i)$.and.\
        all(isatt(res,attsXY))) then
      do j=0,dimsizes(attsXY)-1
        attsXY(j)=kml@quote+res@$attsXY(j)$+kml@quote
      end do
      kml_entry = "<"+kml_tags(i)+" "+str_join(tagsXY+"="+attsXY," ")+"/>"
      kml_setline(kml,kml_entry)
    end if
  end do

  return
end
;**************************************************************

;************************************************************
; comments

undef("kml_setline_idtag")
procedure kml_setline_idtag (kml:string,kml_tags[1]:string,res[1]:logical)
local kml_entry, atts, i
begin
; add one "id" line to kml file
;   e.g. <kml_tags id="res@kmlID"> or <kml_tags>
; add multiple "single tag" lines if kml_tags@single_tags
;   e.g. <kml_tags@single_tags> res@kml$kml_tags@single_tags$ </kml_tags@single_tags>
; add one "close" line if if kml_tags@close_tag
;   e.g. </kml_tags>

  if (isatt(res,"kmlID")) then
    kml_entry = "<"+kml_tags+" id="+kml@quote+res@kmlID+kml@quote+">"
  else
    kml_entry = "<"+kml_tags+">"
  end if
  kml_setline(kml,kml_entry)

  if (isatt(kml_tags,"single_tags")) then
    kml_setline_singletag(kml,kml_tags@single_tags,res)
  end if

  if (isatt(kml_tags,"close_tag").and.dimsizes(kml_tags@close_tag).eq.1.and.\
      typeof(kml_tags@close_tag).eq."logical".and.kml_tags@close_tag) then
    kml_entry = "</"+kml_tags+">"
    kml_setline(kml,kml_entry)
  end if

  return
end
;**************************************************************

;************************************************************
; comments

undef("kml_map_defaults")
procedure kml_map_defaults ( res:logical )
begin

  res@mpOutlineOn     = False
  res@mpPerimOn       = False
  res@mpGridAndLimbOn = False
  res@tmXBBorderOn    = False
  res@tmXTBorderOn    = False
  res@tmYRBorderOn    = False
  res@tmYLBorderOn    = False
  res@tmXBOn          = False
  res@tmXTOn          = False
  res@tmYROn          = False
  res@tmYLOn          = False
  res@gsnMaximize     = False

end ;kml_map_defaults
; *****************************************************************

;************************************************************
; comments

undef("kml_write")
procedure kml_write ( kml:string )
local kml, kml_file
begin

  kml_file = kml@filename+".kml"
  asciiwrite(kml_file,kml(0:kml@nline))

end 
; *****************************************************************

;************************************************************
; comments

undef("kml_make_kmz")
procedure kml_make_kmz ( kml:string )
local kml, kml_file, kmz_file
begin

  kml_file = kml@filename+".kml"
  kmz_file = kml@filename+".kmz"
  system("zip " + kmz_file + " " + kml_file + " " + kml@files)

end
; *****************************************************************


;************************************************************
; comments

undef("kml_altitudeMode_attr")
function kml_altitudeMode_attr ( kml:string, res:logical )
local kml, res, altmode
begin
  
  altmode = get_res_value_keep(res, "kmlAltitudeMode", "relativeToGround")
  kml_setline(kml,"<altitudeMode>"+altmode+"</altitudeMode>")

  return(kml)
end
; *****************************************************************

;************************************************************
; comments

undef("kml_icon_attr")
function kml_icon_attr ( kml:string, icon:string, ires:logical )
local kml, kml_entry, res
begin

  ; <Icon id="ID">
  ;   <!-- specific to Icon -->
  ;   <href>...</href>                         <!-- anyURI -->
  ;   <gx:x>0<gx:x/>                           <!-- int -->
  ;   <gx:y>0<gx:y/>                           <!-- int -->
  ;   <gx:w>...<gx:w/>                         <!-- int -->
  ;   <gx:h>...<gx:h/>                         <!-- int -->
  ;   <refreshMode>onChange</refreshMode>
  ;     <!-- kml:refreshModeEnum: onChange, onInterval, or onExpire -->
  ;   <refreshInterval>4</refreshInterval>     <!-- float -->
  ;   <viewRefreshMode>never</viewRefreshMode>
  ;     <!-- kml:viewRefreshModeEnum: never, onStop, onRequest, onRegion -->
  ;   <viewRefreshTime>4</viewRefreshTime>     <!-- float -->
  ;   <viewBoundScale>1</viewBoundScale>       <!-- float -->
  ;   <viewFormat>...</viewFormat>             <!-- string -->
  ;   <httpQuery>...</httpQuery>               <!-- string -->
  ; </Icon>

  kml_entry = "Icon"
  kml_entry@single_tags=(/"href","refreshMode","refreshInterval",\
              "viewRefreshMode","viewRefreshTime","viewBoundScale","viewFormat",\
              "httpQuery"/)
  kml_entry@close_tag=True
  res=ires
  res@kmlHref=icon
  kml_setline_idtag(kml,kml_entry,res)

  
  return(kml)
end
; *****************************************************************

;************************************************************
; comments

undef("kml_overlay_attr")
function kml_overlay_attr ( kml:string, icon:string, res:logical )
local kml_entry, kml_tags
begin

  ;
  ;     <!-- inherited from Overlay element -->
  ;     <color>ffffffff</color>                   <!-- kml:color -->
  ;     <drawOrder>0</drawOrder>                  <!-- int -->
  ;     <Icon>...</Icon>

  kml_tags=(/"color","drawOrder"/)
  kml_setline_singletag(kml,kml_tags,res)
  delete(kml_tags)
  
  kml = kml_icon_attr(kml,icon,res)

  return(kml)
end
; *****************************************************************

;************************************************************
; comments

undef("kml_link_attr")
function kml_link_attr ( kml:string, href:string, ires:logical )
local kml_entry, res
begin

  ; <Link id="ID">
  ;   <!-- specific to Link -->
  ;   <href>...</href>                      <!-- string -->
  ;   <refreshMode>onChange</refreshMode>
  ;     <!-- refreshModeEnum: onChange, onInterval, or onExpire -->
  ;   <refreshInterval>4</refreshInterval>  <!-- float -->
  ;   <viewRefreshMode>never</viewRefreshMode>
  ;     <!-- viewRefreshModeEnum: never, onStop, onRequest, onRegion -->
  ;   <viewRefreshTime>4</viewRefreshTime>  <!-- float -->
  ;   <viewBoundScale>1</viewBoundScale>    <!-- float -->
  ;   <viewFormat>BBOX=[bboxWest],[bboxSouth],[bboxEast],[bboxNorth]</viewFormat>
  ;                                         <!-- string -->
  ;   <httpQuery>...</httpQuery>            <!-- string -->
  ; </Link>


  kml_entry = "Link"
  kml_entry@single_tags=(/"href","refreshMode","refreshInterval",\
              "viewRefreshMode","viewRefreshTime","viewBoundScale","viewFormat",\
              "httpQuery"/)
  kml_entry@close_tag=True
  res=ires
  res@kmlHref=href
  kml_setline_idtag(kml,kml_entry,res)

  return(kml)
end
; *****************************************************************

;************************************************************
; comments

undef("kml_feature_attr")
function kml_feature_attr ( kml:string, name:string, ires:logical )
local kml_entry, res
begin

  ; <!-- abstract element; do not create -->
  ; <!-- Feature id="ID" -->                <!-- Document,Folder,
  ;                                              NetworkLink,Placemark,
  ;                                              GroundOverlay,PhotoOverlay,ScreenOverlay -->
  ;   <name>...</name>                      <!-- string -->
  ;   <visibility>1</visibility>            <!-- boolean -->
  ;   <open>0</open>                        <!-- boolean -->
  ;   <atom:author>...<atom:author>         <!-- xmlns:atom -->
  ;   <atom:link href=" "/>                 <!-- xmlns:atom -->
  ;   <address>...</address>                <!-- string -->
  ;   <xal:AddressDetails>...</xal:AddressDetails>  <!-- xmlns:xal -->
  ;   <phoneNumber>...</phoneNumber>        <!-- string -->
  ;   <Snippet maxLines="2">...</Snippet>   <!-- string -->
  ;   <description>...</description>        <!-- string -->
  ;   <AbstractView>...</AbstractView>      <!-- Camera or LookAt -->
  ;   <TimePrimitive>...</TimePrimitive>    <!-- TimeStamp or TimeSpan -->
  ;   <styleUrl>...</styleUrl>              <!-- anyURI -->
  ;   <StyleSelector>...</StyleSelector>
  ;   <Region>...</Region>
  ;   <Metadata>...</Metadata>              <!-- deprecated in KML 2.2 -->
  ;   <ExtendedData>...</ExtendedData>      <!-- new in KML 2.2 -->
  ; <-- /Feature -->


  ; inherits all Feature elements
  res=ires
  res@kmlName=name
  kml_entry=(/"name","visibility","open"/)
  kml_setline_singletag(kml,kml_entry,res)
  delete(kml_entry)

  if (isatt(res,"kmlAtomAuthor")) then
    kml_entry = "<atom:author>"+res@kmlAtomAuthor+"</atom:author>"
    kml_setline(kml,kml_entry)
    delete(kml_entry)
  end if

  if (isatt(res,"kmlAtomLink")) then
    kml_entry = "<atom:link href=" + kml@quote + res@kmlAtomLink + kml@quote + "/>"
    kml_setline(kml,kml_entry)
    delete(kml_entry)
  end if

  kml_entry=(/"address","Snippet"/)
  kml_setline_singletag(kml,kml_entry,res)
  delete(kml_entry)

  if (isatt(res,"kmlDescription")) then
    kml_entry = (/ \
    "<description>", \
      "<![CDATA["+res@kmlDescription+"]]>", \
    "</description>" \
    /)
    kml_setline(kml,kml_entry)
    delete(kml_entry)
  end if
  ;
  ; <TimeSpan id="ID">
  ;   <begin>...</begin>     <!-- yyyy-mm-ddThh:mm:sszzzzzz -->
  ;   <end>...</end>         <!-- yyyy-mm-ddThh:mm:sszzzzzz-->
  ; </TimeSpan>

  ; <TimeStamp id=ID>
  ;   <when>...</when>      <!-- kml:dateTime -->
  ; </TimeStamp>

  ; TODO error check here

  if (isatt(res,"kmlTimeSpanBegin")) then
    kml_entry = (/ \
    "<TimeSpan>", \
      "<begin>"+res@kmlTimeSpanBegin+"</begin>", \
      "<end>"+res@kmlTimeSpanEnd+"</end>",  \
    "</TimeSpan>" \
    /)
    kml_setline(kml,kml_entry)
    delete(kml_entry)
  else
    if (isatt(res,"kmlTimeStamp")) then
      kml_entry = (/ \
      "<TimeStamp>", \
        "<when>"+res@kmlTimeStamp+"</when>", \
      "</TimeStamp>" \
      /)
      kml_setline(kml,kml_entry)
      delete(kml_entry)
    end if
  end if

  kml_entry=(/"styleUrl"/)
  kml_setline_singletag(kml,kml_entry,res)
  delete(kml_entry)

  return(kml)
end
; *****************************************************************

;************************************************************
; comments

undef("kml_get_vp_cropbox")
function kml_get_vp_cropbox ( wks:graphic,plot:graphic )
local plot, slop, cropbox, dc, vp, lb
begin

  ; Based on example script from Rick Brownrigg in the thread "Re: No-frills contour plot" on ncl-talk

  ; find the location on the page of the normalized
  ; device coordinate space (NDC), using the workstation resources

  dc = new(4,float)
  getvalues wks
     "wkDeviceLowerX" : dc(0)
     "wkDeviceLowerY" : dc(1)
     "wkDeviceUpperX" : dc(2)
     "wkDeviceUpperY" : dc(3)
  end getvalues

  ; location of the "map" within NDC space
  ; is obtained from the plot resources

   vp = new(4,float)
   getvalues plot
         "vpXF" : vp(0)
         "vpYF" : vp(1)
         "vpWidthF" : vp(2)
         "vpHeightF" : vp(3)
   end getvalues
   print(vp)

  ; calculate the *location on the
  ; page* of the map portion of the plot:

  cropbox = new(6,string)
  cropbox(0) = floor(vp(2) * (dc(2) - dc(0)))
  cropbox(1) = floor(vp(3) * (dc(3) - dc(1)))
  cropbox(2) = floor(dc(0) + vp(0) * (dc(2) - dc(0)))
  cropbox(3) = floor(dc(1) + (vp(1) - vp(3)) * (dc(3) - dc(1)))
  cropbox(4) = wks@name + ".ps"
  cropbox(5) = wks@name + ".png"

  ; postscript page-coordinates
  ; "convert -crop cropbox(0)xcropbox(1)+cropbox(2)+cropbox(3) cropbox(4) cropbox(5)" 

  ; does not work with gsnMaximize=True

  return(cropbox)
end

;************************************************************
; comments

undef("kml_get_vp_latlonbox")
function kml_get_vp_latlonbox ( plot:graphic )
local plot, xf, yf, width, height, xNDC, yNDC, xWorld, yWorld, slop, LatLonBox
begin

  mp = new(4,float)
  getvalues plot
        "mpMaxLonF" : mp(3)
        "mpMinLonF" : mp(2)
        "mpMaxLatF" : mp(0)
        "mpMinLatF" : mp(1)
  end getvalues

  LatLonBox = new(4,float)
  LatLonBox = (/ mp(0), mp(1), mp(2), mp(3) /)

  return(LatLonBox)
end
; *****************************************************************

;************************************************************
; comments

undef("kml_add_groundoverlay")
function kml_add_groundoverlay ( kml:string, name:string, icon:string, LatLonBox[4]:float, z[1]:float, res:logical )
local kml, kml_entry, res, icon, name, rotation, LatLonBox
begin

  ; <GroundOverlay id="ID">

  kml_entry = "GroundOverlay"
  kml_setline_idtag(kml,kml_entry,res)
  delete(kml_entry)

  ;     <!-- inherits all Feature elements -->

  kml = kml_feature_attr(kml, name, res)
  ;
  ;     <!-- inherited from Overlay element -->
  ;     <color>ffffffff</color>                   <!-- kml:color -->
  ;     <drawOrder>0</drawOrder>                  <!-- int -->
  ;     <Icon>...</Icon>

  kml = kml_overlay_attr(kml, icon, res)
  ;
  ;     <!-- specific to GroundOverlay -->
  ;     <altitude>0</altitude>                    <!-- double -->
  ;     <altitudeMode>clampToGround</altitudeMode>
  ;        <!-- kml:altitudeModeEnum: clampToGround or absolute -->
  ;        <!-- or, substitute gx:altitudeMode: clampToSeaFloor or relativeToSeaFloor --


  kml_setline(kml,"<altitude>"+z+"</altitude>")
  kml = kml_altitudeMode_attr(kml,res)

  ;     <LatLonBox>
  ;       <north>...</north>                      <! kml:angle90 -->
  ;       <south>...</south>                      <! kml:angle90 -->
  ;       <east>...</east>                        <! kml:angle180 -->
  ;       <west>...</west>                        <! kml:angle180 -->
  ;       <rotation>0</rotation>                  <! kml:angle180 -->
  ;     </LatLonBox>
  ;     <gx:LatLonQuad>
  ;       <coordinates>...</coordinates>          <!-- four lon,lat tuples -->
  ;     </gx:LatLonQuad>

  rotation = get_res_value_keep(res,"kmlRotation",0)
  kml_entry = (/   \
              "<LatLonBox>", \
              "  <north>"+LatLonBox(0)+"</north>", \
              "  <south>"+LatLonBox(1)+"</south>", \
              "  <east>"+LatLonBox(2)+"</east>", \
              "  <west>"+LatLonBox(3)+"</west>", \
              "  <rotation>"+rotation+"</rotation>", \
              "</LatLonBox>" \
              /)
  kml_setline(kml,kml_entry)
  delete(kml_entry)

  kml_setline(kml,"</GroundOverlay>")   ;</GroundOverlay>

  return(kml)
end
; *****************************************************************

;************************************************************
; comments

undef("kml_add_screenoverlay")
function kml_add_screenoverlay ( kml:string, name:string, icon:string, res:logical )
local kml, kml_entry, res, plot_name, plot_file, icon, name
begin

  ; <ScreenOverlay id="ID">

  kml_entry = "ScreenOverlay"
  kml_setline_idtag(kml,kml_entry,res)
  delete(kml_entry)

  ; <!-- inherits all Feature elements -->

  kml = kml_feature_attr(kml, name, res)
  ;
  ;     <!-- inherited from Overlay element -->
  ;     <color>ffffffff</color>                   <!-- kml:color -->
  ;     <drawOrder>0</drawOrder>                  <!-- int -->
  ;     <Icon>...</Icon>

  kml = kml_overlay_attr(kml, icon, res)

  ;  <!-- specific to ScreenOverlay -->
  ;  <overlayXY x="double" y="double" xunits="fraction" yunits="fraction"/>
  ;    <!-- vec2 -->
  ;    <!-- xunits and yunits can be one of: fraction, pixels, or insetPixels -->
  ;  <screenXY x="double" y="double" xunits="fraction" yunits="fraction"/>
  ;    <!-- vec2 -->
  ;  <rotationXY x="double" y="double" xunits="fraction" yunits"fraction"/>
  ;    <!-- vec2 -->
  ;  <size x="double" y="double" xunits="fraction" yunits="fraction"/>
  ;    <!-- vec2 -->
  ;  <rotation>0</rotation>                   <!-- float -->
  ; </ScreenOverlay>

  ;TODO: error checking

  kml_entry = (/"overlayXY","screenXY","rotationXY","sizeXY"/)
  kml_setline_xytag(kml,kml_entry,res)
  delete(kml_entry)
  kml_entry="rotation"
  kml_setline_singletag(kml,kml_entry,res)
  delete(kml_entry)

  kml_setline(kml,"</ScreenOverlay>")

  return(kml)
end
; *****************************************************************

;************************************************************
; comments

undef("kml_open_document")
function kml_open_document(filename:string, name:string,res:logical)
local kml, kml_entry, name, maxlines,res, filename
begin
; create new string array to contain kml

    ; <Document id="ID">
    ;   <!-- inherits all Feature elements -->
    ;
    ;   <!-- specific to Document -->
    ;   <!-- 0 or more Schema elements -->
    ;   <!-- 0 or more Feature elements -->
    ; </Document>

  if (isatt(res,"maxlines")) then
    maxlines = res@maxlines
  else
    maxlines = 100000
  end if

  kml = new((/maxlines/),"string")
  kml@_FillValue = " "
  kml@nline = 0
  kml@filename = filename
  kml@files = " "

  kml@quote = str_get_dq()

  kml_entry = (/ \
  "<?xml version=" + kml@quote + "1.0" + kml@quote + " encoding=" + kml@quote + "UTF-8" + kml@quote + "?>", \
  "<kml xmlns=" + kml@quote + "http://www.opengis.net/kml/2.2" + kml@quote + ">" \
  /)
  kml_setline(kml,kml_entry)
  delete(kml_entry)

  kml_entry = "Document"
  kml_setline_idtag(kml,kml_entry,res)
  delete(kml_entry)

  kml = kml_feature_attr(kml, name, res)

return (kml)
end
; *****************************************************************



;************************************************************
; comments

undef("kml_crop_plot")
function kml_crop_plot ( cropbox:string, res:logical )
local cropbox, icon, convertcmd, res, AlphaColor
begin

  AlphaColor = ""
  if(isatt(res,"kmlCropAlphaColor")) then 
   AlphaColor = " -transparent "+res@kmlCropAlphaColor
  end if
  
  convertcmd = "%convert% "+cropbox(4)+" -crop "+cropbox(0)+"x"+cropbox(1)+"+"+cropbox(2)+"+"+cropbox(3)+" "+AlphaColor+" +repage -depth 8 "+cropbox(5)
  ;;print(convertcmd)
  system(convertcmd)
  
  icon = cropbox(5)
  return(icon)
end
; *****************************************************************


;************************************************************
; based on the NCL example script lb_19.ncl by Mary Haley
; 

undef("kml_crop_labelbar")
function kml_crop_labelbar (kml:string, name:string, wks:graphic,plot:graphic, res:logical)
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
  print(class_name)
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
  print(views)

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
  print(lbar_fc)

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
  lbplot_file = lbplot_name + ".ps"
  lbicon      = lbplot_name + ".png"


  lbwks = gsn_open_wks("ps",lbplot_name)
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
   
   lbcropbox = kml_get_vp_cropbox(lbwks,lbid)
   delete(lbwks)

   lbicon = kml_crop_plot(lbcropbox,res)

  return(lbicon)
end



;************************************************************
; comments

undef("kml_close_document")
function kml_close_document ( kml:string )
local kml, kml_entry
begin

  kml_setline(kml,"</Document>")
  kml_setline(kml,"</kml>")

  return(kml)
end
; *****************************************************************



;************************************************************
; comments

undef("kml_open_folder")
function kml_open_folder ( kml:string, name:string, res:logical )
local kml, kml_entry, res
begin

  ; <Folder id="ID">
  ;   <!-- inherits all Feature elements -->
  ;
  ;   <!-- specific to Folder -->
  ;   <!-- 0 or more Feature elements -->
  ; </Folder>

  kml_entry = "Folder"
  kml_setline_idtag(kml,kml_entry,res)
  delete(kml_entry)

  kml = kml_feature_attr(kml, name, res)

  return(kml)
end 
; *****************************************************************


;************************************************************
; comments

undef("kml_close_folder")
function kml_close_folder ( kml:string )
local kml
begin

  kml_setline(kml,"</Folder>")

  return(kml)
end 

; *****************************************************************

undef("kml_add_networklink")
function kml_add_networklink ( kml:string, name:string, link:string, res:logical )
local kml, kml_entry, res
begin

  ; <NetworkLink id="ID">
  ;   <!-- inherits all Feature elements --><name>...</name>                      <!-- string -->
  ;
  ;   <!-- specific to NetworkLink -->
  ;   <refreshVisibility>0</refreshVisibility> <!-- boolean -->
  ;   <flyToView>0</flyToView>                 <!-- boolean -->
  ;   <Link>...</Link>
  ; </NetworkLink>

  kml_entry = "NetworkLink"
  kml_setline_idtag(kml,kml_entry,res)
  delete(kml_entry)

  kml = kml_feature_attr(kml, name, res)

  kml_entry=(/"refreshVisibility","flyToView"/)
  kml_setline_singletag(kml,kml_entry,res)
  delete(kml_entry)

  kml = kml_link_attr(kml, link, res)

  kml_setline(kml,"</NetworkLink>")

  return(kml)
end 
; *****************************************************************

;************************************************************
; comments

undef("kml_open_style")
function kml_open_style ( kml:string, res:logical )
local kml, kml_entry, res
begin

  ; <Style id="ID">
  ; <!-- extends StyleSelector -->
  ;
  ; <!-- specific to Style -->
  ;   <IconStyle>...</IconStyle>
  ;   <LabelStyle>...</LabelStyle>
  ;   <LineStyle>...</LineStyle>
  ;   <PolyStyle>...</PolyStyle>
  ;   <BalloonStyle>...</BalloonStyle>
  ;   <ListStyle>...</ListStyle>
  ; </Style>

  kml_entry = "Style"
  kml_setline_idtag(kml,kml_entry,res)
  delete(kml_entry)

  return(kml)
end
; *****************************************************************

;************************************************************
; comments

undef("kml_open_stylemap")
function kml_open_stylemap ( kml:string, res:logical )
local kml, kml_entry, res
begin

  ; <StyleMap id="ID">
  ;   <!-- extends StyleSelector -->
  ;   <!-- elements specific to StyleMap -->
  ;   <Pair id="ID">
  ;     <key>normal</key>              <!-- kml:styleStateEnum:  normal or highlight -->
  ;     <styleUrl>...</styleUrl>
  ;   </Pair>
  ; </StyleMap>

  kml_entry = "StyleMap"
  kml_setline_idtag(kml,kml_entry,res)
  delete(kml_entry)

  return(kml)
end
; *****************************************************************


;************************************************************
; comments

undef("kml_add_polystyle")
function kml_add_polystyle ( kml:string, res:logical )
local kml_entry
begin

  ; <PolyStyle id="ID">
  ;   <!-- inherited from ColorStyle -->
  ;   <color>ffffffff</color>            <!-- kml:color -->
  ;   <colorMode>normal</colorMode>      <!-- kml:colorModeEnum: normal or random -->
  ;
  ;   <!-- specific to PolyStyle -->
  ;   <fill>1</fill>                     <!-- boolean -->
  ;   <outline>1</outline>               <!-- boolean -->
  ; </PolyStyle>

  kml_entry = "PolyStyle"
  kml_entry@single_tags=(/"color","colorMode","fill","outline"/)
  kml_entry@close_tag=True
  kml_setline_idtag(kml,kml_entry,res)

  return(kml)
end ;kml_add_polystyle
; *****************************************************************


;************************************************************
; comments

undef("kml_add_iconstyle")
function kml_add_iconstyle ( kml:string, icon:string, res:logical )
local kml_entry
begin

  ; <IconStyle id="ID">
  ;   <!-- inherited from ColorStyle -->
  ;   <color>ffffffff</color>            <!-- kml:color -->
  ;   <colorMode>normal</colorMode>      <!-- kml:colorModeEnum:normal or random -->
  ;
  ;   <!-- specific to IconStyle -->
  ;   <scale>1</scale>                   <!-- float -->
  ;   <heading>0</heading>               <!-- float -->
  ;   <Icon>
  ;     <href>...</href>
  ;   </Icon>
  ;   <hotSpot x="0.5"  y="0.5"
  ;     xunits="fraction" yunits="fraction"/>    <!-- kml:vec2 -->
  ; </IconStyle>

  kml_entry = "IconStyle"
  kml_entry@single_tags=(/"color","colorMode","scale","heading"/)
  kml_setline_idtag(kml,kml_entry,res)
  delete(kml_entry)

  kml = kml_icon_attr(kml, icon, res)

  if (isatt(res,"kmlHotSpot")) then
    if (res@kmlHotSpot)
    kml_entry = "<overlayXY x="+kml@quote+res@kmlHotSpotx+kml@quote+" y="+kml@quote+res@kmlHotSpoty+kml@quote+" xunits="+kml@quote+res@kmlHotSpotxunits+kml@quote+" yunits="+kml@quote+res@kmlHotSpotyunits+kml@quote+"/>"
    kml_setline(kml,kml_entry)
    delete(kml_entry)
    end if
  end if

  kml_setline(kml,"</IconStyle>")

  return(kml)
end ;kml_add_iconstyle
; *****************************************************************


;************************************************************
; comments

undef("kml_add_linestyle")
function kml_add_linestyle ( kml:string, res:logical )
local kml_entry
begin

  ; <LineStyle id="ID">
  ;   <!-- inherited from ColorStyle -->
  ;   <color>ffffffff</color>            <!-- kml:color -->
  ;   <colorMode>normal</colorMode>      <!-- colorModeEnum: normal or random -->
  ;
  ;   <!-- specific to LineStyle -->
  ;   <width>1</width>                   <!-- float -->
  ; </LineStyle>

  kml_entry = "LineStyle"
  kml_entry@single_tags=(/"color","colorMode","width"/)
  kml_entry@close_tag=True
  kml_setline_idtag(kml,kml_entry,res)

  return(kml)
end ;kml_add_linestyle
; *****************************************************************


;************************************************************
; comments

undef("kml_add_balloonstyle")
function kml_add_balloonstyle ( kml:string, res:logical )
local kml, kml_entry, res
begin

  ; <BalloonStyle id="ID">
  ;   <!-- specific to BalloonStyle -->
  ;   <bgColor>ffffffff</bgColor>            <!-- kml:color -->
  ;   <textColor>ff000000</textColor>        <!-- kml:color -->
  ;   <text>...</text>                       <!-- string -->
  ;   <displayMode>default</displayMode>     <!-- kml:displayModeEnum -->
  ; </BalloonStyle>

  kml_setline_idtag(kml,"BalloonStyle",res)

  kml_entry=(/"bgColor", "textColor","text","displayMode"/)
  kml_setline_singletag(kml,kml_entry,res)
  delete(kml_entry)

  kml_setline(kml,"</BalloonStyle>")

  return(kml)
end ;kml_add_balloonstyle
; *****************************************************************


;************************************************************
; comments

undef("kml_add_liststyle")
function kml_add_liststyle ( kml:string, href:string, res:logical )
local kml_entry, res
begin

  ; <ListStyle id="ID">
  ;   <!-- specific to ListStyle -->
  ;   <listItemType>check</listItemType> <!-- kml:listItemTypeEnum:check,
  ;                                           checkOffOnly,checkHideChildren,
  ;                                          radioFolder -->
  ;   <bgColor>ffffffff</bgColor>        <!-- kml:color -->
  ;   <ItemIcon>                         <!-- 0 or more ItemIcon elements -->
  ;     <state>open</state>
  ;       <!-- kml:itemIconModeEnum:open, closed, error, fetching0, fetching1, or fetching2 -->
  ;     <href>...</href>                 <!-- anyURI -->
  ;   </ItemIcon>
  ; </ListStyle>

  kml_setline_idtag(kml,"ListStyle",res)

  kml_entry=(/"listItemType","bgColor"/)
  kml_setline_singletag(kml,kml_entry,res)
  delete(kml_entry)

  ; NEEDS ERROR CHECKING
  if (isatt(res,"kmlItemIcon") .and. res@kmlItemIcon) then
    kml_entry = "<ItemIcon>"
    kml_entry@single_tags=(/"state","href"/)
    kml_entry@close_tag=True
    delete(res@ID)
    res@kmlHref=href
    kml_setline_idtag(kml,kml_entry,res)
  end if

  kml_setline(kml,"</ListStyle>")

  return(kml)
end ;kml_add_liststyle
; *****************************************************************

;************************************************************
; comments

undef("kml_close_stylemap")
function kml_close_stylemap ( kml:string )
local kml, kml_entry
begin

  kml_setline(kml,"</StyleMap>")

  return(kml)
end 
; *****************************************************************


;************************************************************
; comments

undef("kml_close_style")
function kml_close_style ( kml:string )
local kml, kml_entry
begin

  kml_setline(kml,"</Style>")

  return(kml)
end 
; *****************************************************************



; features

;************************************************************
; comments

undef("kml_add_lookat")
function kml_add_lookat ( kml:string, x[1]:float, y[1]:float, z[1]:float, range[1]:float, res:logical )
local kml, kml_entry, res, heading, tilt, roll, range, x, y, z
begin

  ; <LookAt id="ID">
  ;   <!-- inherited from AbstractView element -->
  ;   <TimePrimitive>...</TimePrimitive>  <!-- gx:TimeSpan or gx:TimeStamp -->
  ;
  ;   <!-- specific to LookAt -->
  ;   <longitude>0</longitude>            <!-- kml:angle180 -->
  ;   <latitude>0</latitude>              <!-- kml:angle90 -->
  ;   <altitude>0</altitude>              <!-- double -->
  ;   <heading>0</heading>                <!-- kml:angle360 -->
  ;   <tilt>0</tilt>                      <!-- kml:anglepos90 -->
  ;   <range></range>                     <!-- double -->
  ;   <altitudeMode>clampToGround</altitudeMode>
  ;           <!--kml:altitudeModeEnum:clampToGround, relativeToGround, absolute -->
  ;           <!-- or, gx:altitudeMode can be substituted: clampToSeaFloor, relativeToSeaFloor -->
  ;
  ; </LookAt>

  kml_setline_idtag(kml,"LookAt",res)
  ;
  ; <TimeSpan id="ID">
  ;   <begin>...</begin>     <!-- yyyy-mm-ddThh:mm:sszzzzzz -->
  ;   <end>...</end>         <!-- yyyy-mm-ddThh:mm:sszzzzzz-->
  ; </TimeSpan>

  ; <TimeStamp id=ID>
  ;   <when>...</when>      <!-- kml:dateTime -->
  ; </TimeStamp>

  ; TODO error check here

  if (isatt(res,"kmlTimeSpanBegin")) then
    kml_entry = (/ \
    "<TimeSpan>", \
      "<begin>"+res@kmlTimeSpanBegin+"</begin>", \
      "<end>"+res@kmlTimeSpanEnd+"</end>",  \
    "</TimeSpan>" \
    /)
    kml_setline(kml,kml_entry)
    delete(kml_entry)
  else
    if (isatt(res,"kmlTimeStamp")) then
      kml_entry = (/ \
      "<TimeStamp>", \
        "<when>"+res@kmlTimeStamp+"</when>", \
      "</TimeStamp>" \
      /)
      kml_setline(kml,kml_entry)
      delete(kml_entry)
    end if
  end if

  heading = get_res_value_keep(res,"kmlHeading",0)
  tilt    = get_res_value_keep(res,"kmlTilt",0)

  kml_entry = (/   \
              "  <longitude>"+x+"</longitude>", \
              "  <latitude>"+y+"</latitude>", \
              "  <altitude>"+z+"</altitude>", \
              "  <heading>"+heading+"</heading>", \
              "  <tilt>"+tilt+"</tilt>", \
              "  <range>"+range+"</range>" \
              /)
  kml_setline(kml,kml_entry)
  delete(kml_entry)

  kml = kml_altitudeMode_attr(kml,res)

  kml_setline(kml,"</LookAt>")
  
  return(kml)
end ;kml_add_lookat
; *****************************************************************


;************************************************************
; comments

undef("kml_add_camera")
function kml_add_camera ( kml:string, x[1]:float, y[1]:float, z[1]:float, res:logical )
local kml, kml_entry, res
begin

  ; <Camera id="ID">
  ;   <!-- inherited from AbstractView element -->
  ;   <TimePrimitive>...</TimePrimitive>  <!-- gx:TimeSpan or gx:TimeStamp -->
  ;
  ;   <!-- specific to Camera -->
  ;   <longitude>0</longitude>            <!-- kml:angle180 -->
  ;   <latitude>0</latitude>              <!-- kml:angle90 -->
  ;   <altitude>0</altitude>              <!-- double -->
  ;   <heading>0</heading>                <!-- kml:angle360 -->
  ;   <tilt>0</tilt>                      <!-- kml:anglepos180 -->
  ;   <roll>0</roll>                      <!-- kml:angle180 -->
  ;   <altitudeMode>clampToGround</altitudeMode>
  ;         <!-- kml:altitudeModeEnum: relativeToGround, clampToGround, or absolute -->
  ;         <!-- or, gx:altitudeMode can be substituted: clampToSeaFloor, relativeToSeaFloor -->
  ; </Camera>

  kml_setline_idtag(kml,"Camera",res)
  
  ;
  ; <TimeSpan id="ID">
  ;   <begin>...</begin>     <!-- yyyy-mm-ddThh:mm:sszzzzzz -->
  ;   <end>...</end>         <!-- yyyy-mm-ddThh:mm:sszzzzzz-->
  ; </TimeSpan>

  ; <TimeStamp id=ID>
  ;   <when>...</when>      <!-- kml:dateTime -->
  ; </TimeStamp>

  ; TODO error check here

  if (isatt(res,"kmlTimeSpanBegin")) then
    kml_entry = (/ \
    "<TimeSpan>", \
      "<begin>"+res@kmlTimeSpanBegin+"</begin>", \
      "<end>"+res@kmlTimeSpanEnd+"</end>",  \
    "</TimeSpan>" \
    /)
    kml_setline(kml,kml_entry)
    delete(kml_entry)
  else
    if (isatt(res,"kmlTimeStamp")) then
      kml_entry = (/ \
      "<TimeStamp>", \
        "<when>"+res@kmlTimeStamp+"</when>", \
      "</TimeStamp>" \
      /)
      kml_setline(kml,kml_entry)
      delete(kml_entry)
    end if
  end if

  heading = get_res_value_keep(res,"kmlHeading",0)
  tilt    = get_res_value_keep(res,"kmlTilt",0)
  roll    = get_res_value_keep(res,"kmlRoll",0)

  kml_entry = (/   \
              "  <longitude>"+x+"</longitude>", \
              "  <latitude>"+y+"</latitude>", \
              "  <altitude>"+z+"</altitude>", \
              "  <heading>"+heading+"</heading>", \
              "  <tilt>"+tilt+"</tilt>", \
              "  <roll>"+roll+"</roll>" \
              /)
  kml_setline(kml,kml_entry)
  delete(kml_entry)

  kml = kml_altitudeMode_attr(kml,res)

  kml_setline(kml,"</Camera>")

  return(kml)
end ;kml_add_camera
; *****************************************************************


;************************************************************
; comments

undef("kml_add_region")
function kml_add_region ( kml:string, res:logical )
local kml, kml_entry, res
begin

  ; <Region id="ID">
  ;   <LatLonAltBox>
  ;     <north></north>                            <!-- required; kml:angle90 -->
  ;     <south></south>                            <!-- required; kml:angle90 -->
  ;     <east></east>                              <!-- required; kml:angle180 -->
  ;     <west></west>                              <!-- required; kml:angle180 -->
  ;     <minAltitude>0</minAltitude>               <!-- float -->
  ;     <maxAltitude>0</maxAltitude>               <!-- float -->
  ;     <altitudeMode>clampToGround</altitudeMode>
  ;         <!-- kml:altitudeModeEnum: clampToGround, relativeToGround, or absolute -->
  ;         <!-- or, substitute gx:altitudeMode: clampToSeaFloor, relativeToSeaFloor -->
  ;   </LatLonAltBox>
  ;   <Lod>
  ;     <minLodPixels>0</minLodPixels>             <!-- float -->
  ;     <maxLodPixels>-1</maxLodPixels>            <!-- float -->
  ;     <minFadeExtent>0</minFadeExtent>           <!-- float -->
  ;     <maxFadeExtent>0</maxFadeExtent>           <!-- float -->
  ;   </Lod>
  ; </Region>

  return(kml)
end ;kml_add_region
; *****************************************************************



;************************************************************
; comments

undef("kml_open_placemark")
function kml_open_placemark ( kml:string, name:string, res:logical )
local kml_entry
begin

  ; <Placemark id="ID">
  ;   <!-- inherits all Feature elements -->

  ;
  ;   <!-- specific to Placemark element -->
  ;   <MultiGeometry>...</MultiGeometry>
  ; </Placemark>

  kml_setline_idtag(kml,"Placemark",res)

  kml = kml_feature_attr(kml, name, res)

  kml_setline(kml,"<MultiGeometry>")

  return(kml)
end ;kml_open_placemark
; *****************************************************************

;************************************************************
; comments

undef("kml_add_point")
function kml_add_point( kml:string, x[1]:float, y[1]:float, z[1]:float, res:logical )
local kml_entry
begin
  ; add point element to kml
  ; contained by placemark or multigeometry

  ; <Point id="ID">
  ;   <!-- specific to Point -->
  ;   <extrude>0</extrude>                        <!-- boolean -->
  ;   <altitudeMode>clampToGround</altitudeMode>
  ;         <!-- kml:altitudeModeEnum: clampToGround, relativeToGround, or absolute -->
  ;         <!-- or, substitute gx:altitudeMode: clampToSeaFloor, relativeToSeaFloor -->
  ;   <coordinates>...</coordinates>              <!-- lon,lat[,alt] -->
  ; </Point>

  kml_entry = "Point"
  kml_entry@single_tags=(/"extrude"/)
  kml_setline_idtag(kml,kml_entry,res)
  delete(kml_entry)

  kml = kml_altitudeMode_attr(kml,res)

  kml_entry = "<coordinates>"+x+","+y+","+z+"</coordinates>"
  kml_setline(kml,kml_entry)
  delete(kml_entry)

  kml_setline(kml,"</Point>")

  return (kml)
end ;kml_add_point
; *****************************************************************


;************************************************************
; comments

undef("kml_add_linestring")
function kml_add_linestring ( kml:string, x[*]:numeric, y[*]:numeric, z[*]:numeric, res:logical )
local kml, kml_entry, res
begin

  ; <LineString id="ID">
  ;   <!-- specific to LineString -->
  ;   <extrude>0</extrude>                   <!-- boolean -->
  ;   <tessellate>0</tessellate>             <!-- boolean -->
  ;   <altitudeMode>clampToGround</altitudeMode>
  ;       <!-- kml:altitudeModeEnum: clampToGround, relativeToGround, or absolute -->
  ;         <!-- or, substitute gx:altitudeMode: clampToSeaFloor, relativeToSeaFloor -->
  ;       <!-- or, substitute gx:altitudeMode: clampToSeaFloor, relativeToSeaFloor -->
  ;   <coordinates>...</coordinates>         <!-- lon,lat[,alt] -->
  ; </LineString>

  ; TODO: error check dimension sizes

  kml_entry = "LineString"
  kml_entry@single_tags=(/"extrude","tessellate"/)
  kml_setline_idtag(kml,kml_entry,res)
  delete(kml_entry)

  kml = kml_altitudeMode_attr(kml,res)

  kml_setline(kml,"<coordinates>")

  nx = dimsizes(x)
  ny = dimsizes(y)
  nz = dimsizes(z)

  if ((nx .ne.  ny) .or. (nx .ne. nz)) then
    print("kml_add_linestring: Fatal: dimensions of x, y, and z must be equal.")
    return
  end if

  do i = 0, nx-1
    kml_entry = x(i) + "," + y(i) + "," + z(i)
    kml_setline(kml,kml_entry)
    delete(kml_entry)
  end do

  kml_setline(kml,"</coordinates>")
  kml_setline(kml,"</LineString>")

  return(kml)
end ;kml_add_linestring
; *****************************************************************

;************************************************************
; comments

undef("kml_add_linearring")
function kml_add_linearring ( kml:string, x[*]:numeric, y[*]:numeric, z[*]:numeric, res:logical )
local kml, kml_entry, res
begin

  ; <LinearRing id="ID">
  ;   <!-- specific to LinearRing -->
  ;   <gx:altitudeOffset>0</gx:altitudeOffset>   <!-- double -->
  ;   <extrude>0</extrude>                       <!-- boolean -->
  ;   <tessellate>0</tessellate>                 <!-- boolean -->
  ;   <altitudeMode>clampToGround</altitudeMode> 
  ;     <!-- kml:altitudeModeEnum: clampToGround, relativeToGround, or absolute -->
  ;     <!-- or, substitute gx:altitudeMode: clampToSeaFloor, relativeToSeaFloor -->
  ;   <coordinates>...</coordinates>             <!-- lon,lat[,alt] tuples --> 
  ; </LinearRing>

  ; TODO: error check dimension sizes

  kml_entry = "LinearRing"
  kml_entry@single_tags=(/"extrude","tessellate"/)
  kml_setline_idtag(kml,kml_entry,res)
  delete(kml_entry)

  kml = kml_altitudeMode_attr(kml,res)

  kml_setline(kml,"<coordinates>")

  nx = dimsizes(x)
  ny = dimsizes(y)
  nz = dimsizes(z)

  if ((nx .ne.  ny) .or. (nx .ne. nz)) then
    print("kml_add_linestring: Fatal: dimensions of x, y, and z must be equal.")
    return
  end if

  do i = 0, nx-1
    kml_entry = x(i) + "," + y(i) + "," + z(i)
    kml_setline(kml,kml_entry)
    delete(kml_entry)
  end do

  kml_setline(kml,"</coordinates>")
  kml_setline(kml,"</LinearRing>")

  return(kml)
end ;kml_add_linearring
; *****************************************************************


;************************************************************
; comments

undef("kml_open_polygon")
function kml_open_polygon ( kml:string, res:logical )
local kml_entry
begin

  ; <Polygon id="ID">
  ;   <!-- specific to Polygon -->
  ;   <extrude>0</extrude>                       <!-- boolean -->
  ;   <tessellate>0</tessellate>                 <!-- boolean -->
  ;   <altitudeMode>clampToGround</altitudeMode>
  ;         <!-- kml:altitudeModeEnum: clampToGround, relativeToGround, or absolute -->
  ;         <!-- or, substitute gx:altitudeMode: clampToSeaFloor, relativeToSeaFloor -->
  ;   <outerBoundaryIs>
  ;     <LinearRing>
  ;       <coordinates>...</coordinates>         <!-- lon,lat[,alt] -->
  ;     </LinearRing>
  ;   </outerBoundaryIs>
  ;   <innerBoundaryIs>
  ;     <LinearRing>
  ;       <coordinates>...</coordinates>         <!-- lon,lat[,alt] -->
  ;     </LinearRing>
  ;   </innerBoundaryIs>
  ; </Polygon>

  ; TODO: error check dimension sizes

  kml_entry = "Polygon"
  kml_entry@single_tags=(/"extrude","tessellate"/)
  kml_setline_idtag(kml,kml_entry,res)

  kml = kml_altitudeMode_attr(kml,res)

  return(kml)
end ;kml_open_polygon
; *****************************************************************


;************************************************************
; comments

undef("kml_add_polygon_outer")
function kml_add_polygon_outer ( kml:string, x[*]:numeric, y[*]:numeric, z[*]:numeric, res:logical )
local kml, kml_entry, res, x, y, z
begin

  ; 1 contained in Polygon element

  kml_entry = (/   \
              "<outerBoundaryIs>", \
              "  <LinearRing>", \
              "    <coordinates>" \
              /)
  kml_setline(kml,kml_entry)
  delete(kml_entry)

  nx = dimsizes(x)
  ny = dimsizes(y)
  nz = dimsizes(z)

  if ((nx .ne.  ny) .or. (nx .ne. nz)) then
    print("kml_polygon_outer: Fatal: dimensions of x, y, and z must be equal.")
    return
  end if

  do i = 0, nx-1
    kml_entry = "      " + x(i) + "," + y(i) + "," + z(i)
    kml_setline(kml,kml_entry)
    delete(kml_entry)
  end do

  kml_entry = (/   \
              "    </coordinates>", \
              "  </LinearRing>", \
              "</outerBoundaryIs>" \
              /)
  kml_setline(kml,kml_entry)
  delete(kml_entry)

  return(kml)
end ;kml_add_polygon_outer
; *****************************************************************


;************************************************************
; comments

undef("kml_add_polygon_inner")
function kml_add_polygon_inner ( kml:string, x[*]:numeric, y[*]:numeric, z[*]:numeric, res:logical )
local kml, kml_entry, res, x, y, z
begin

  ; 0 or more contained in Polygon element

  kml_entry = (/   \
              "<innerBoundaryIs>", \
              "  <LinearRing>", \
              "    <coordinates>" \
              /)
  kml_setline(kml,kml_entry)
  delete(kml_entry)



  nx = dimsizes(x)
  ny = dimsizes(y)
  nz = dimsizes(z)

  if ((nx .ne.  ny) .or. (nx .ne. nz)) then
    print("kml_polygon_inner: Fatal: dimensions of x, y, and z must be equal.")
    return
  end if

  do i = 0, nx-1
    kml_entry = "      " + x(i) + "," + y(i) + "," + z(i)
    kml_setline(kml,kml_entry)
    delete(kml_entry)
  end do

  kml_entry = (/   \
              "    </coordinates>", \
              "  </LinearRing>", \
              "</innerBoundaryIs>" \
              /)
  kml_setline(kml,kml_entry)
  delete(kml_entry)

  return(kml)
end
; *****************************************************************


;************************************************************
; comments

undef("kml_close_polygon")
function kml_close_polygon ( kml:string )
local kml
begin

  kml_setline(kml,"</Polygon>")

  return(kml)
end 
; *****************************************************************


;************************************************************
; comments

undef("kml_close_placemark")
function kml_close_placemark ( kml:string )
local kml
begin

  kml_setline(kml,"</MultiGeometry>")
  kml_setline(kml,"</Placemark>" )

  return(kml)
end 
; *****************************************************************








;************************************************************
; comments

undef("kml_add_model")
function kml_add_model ( kml:string, name:string, link:string, x[1]:float, y[1]:float, z[1]:float, res:logical )
local kml, kml_entry, res, heading, tilt, roll, scalex, scaley, scalez
begin

  ; <Model id="ID">
  ;   <!-- specific to Model -->
  ;   <altitudeMode>clampToGround</altitudeMode>
  ;       <!-- kml:altitudeModeEnum: clampToGround,relativeToGround,or absolute -->
  ;       <!-- or, substitute gx:altitudeMode: clampToSeaFloor, relativeToSeaFloor -->
  ;   <Location>
  ;     <longitude></longitude> <!-- kml:angle180 -->
  ;     <latitude></latitude>   <!-- kml:angle90 -->
  ;     <altitude>0</altitude>  <!-- double -->
  ;   </Location>
  ;   <Orientation>
  ;     <heading>0</heading>    <!-- kml:angle360 -->
  ;     <tilt>0</tilt>          <!-- kml:angle360 -->
  ;     <roll>0</roll>          <!-- kml:angle360 -->
  ;   </Orientation>
  ;   <Scale>
  ;     <x>1</x>                <!-- double -->
  ;     <y>1</y>                <!-- double -->
  ;     <z>1</z>                <!-- double -->
  ;   </Scale>
  ;   <Link>...</Link>
  ;   <ResourceMap>
  ;     <Alias>
  ;       <targetHref>...</targetHref>   <!-- anyURI -->
  ;       <sourceHref>...</sourceHref>   <!-- anyURI -->
  ;     </Alias>
  ;   </ResourceMap>
  ; </Model>

  kml_setline_idtag(kml,"Model",res)
  
  kml = kml_altitudeMode_attr(kml,res)

  kml_entry = (/   \
              "<Location>", \
              "  <longitude>"+x+"</longitude>", \
              "  <latitude>"+y+"</latitude>", \
              "  <altitude>"+z+"</altitude>", \
              "</Location>" \
              /)
  kml_setline(kml,kml_entry)
  delete(kml_entry)

  heading = get_res_value_keep(res,"kmlHeading",0)
  tilt    = get_res_value_keep(res,"kmlTilt",0)
  roll    = get_res_value_keep(res,"kmlRoll",0)

  kml_entry = (/   \
              "<Orientation>", \
              "  <heading>"+heading+"</heading>", \
              "  <tilt>"+tilt+"</tilt>", \
              "  <roll>"+roll+"</roll>", \
              "</Orientation>" \
              /)
  kml_setline(kml,kml_entry)
  delete(kml_entry)

  scalex    = get_res_value_keep(res,"kmlScaleX",1)
  scaley    = get_res_value_keep(res,"kmlScaleY",1)
  scalez    = get_res_value_keep(res,"kmlScaleZ",1)

  kml_entry = (/   \
              "<Scale>", \
              "  <x>"+scalex+"</x>", \
              "  <y>"+scaley+"</y>", \
              "  <z>"+scalez+"</z>", \
              "</Scale>" \
              /)
  kml_setline(kml,kml_entry)
  delete(kml_entry)

  kml = kml_link_attr(kml, link, res)
  
  if (isatt(res,"kmlTargetHref") .and. isatt(res,"kmlSourceHref")) then
  
    ntarget = dimsizes(res@kmlTargetHref)
    nsource = dimsizes(res@kmlSourceHref)

    if ((ntarget .ne.  nsource) ) then
      print("kml_add_model: Fatal: dimensions of resources kmlTargetHref and kmlSourceHref must be equal.")
      return
    end if

    kml_setline(kml,"<ResourceMap>")
    do i = 0, ntarget-1 
      kml_entry = (/   \
                   "<Alias>", \
                   "  <targetHref>"+res@kmlTargetHref(i)+"</targetHref>", \
                   "  <sourceHref>"+res@kmlSourceHref(i)+"</sourceHref>", \
                   "</Alias>" \
                   /)
       kml_setline(kml,kml_entry)
       delete(kml_entry)
    end do
    kml_setline(kml,"</ResourceMap>")
    
  end if
  
  kml_setline(kml,"</Model>")

  return(kml)
end
; *****************************************************************

;************************************************************
; comments

undef("kml_add_stylepair")
function kml_add_stylepair ( kml:string, key:string, styleurl:string)
local kml, kml_entry, target, source
begin

  ; contained by <StyleMap>
  ; <Pair id="ID">
  ;   <key>normal</key>              <!-- kml:styleStateEnum:  normal or highlight -->
  ;   <styleUrl>...</styleUrl> or <Style>...</Style>
  ; </Pair>

  kml_entry = "Pair"
  kml_setline_idtag(kml,kml_entry,res)
  delete(kml_entry)

  kml_entry = (/   \
              "  <key>"+key+"</key>", \
              "  <styleUrl>"+styleurl+"</styleUrl>", \
              "</Pair>" \
              /)
  kml_setline(kml,kml_entry)
  delete(kml_entry)

  return(kml)
end
; *****************************************************************



;************************************************************
; comments

undef("kml_add_photooverlay")
function kml_add_photooverlay ( kml:string, name:string, icon:string, x[1]:float, y[1]:float, z[1]:float, res:logical )
local kml, kml_entry, res
begin

  ; <PhotoOverlay id="ID">

  kml_entry = "PhotoOverlay"
  kml_setline_idtag(kml,kml_entry,res)
  delete(kml_entry)

    ; <!-- inherits all Feature elements -->

    kml = kml_feature_attr(kml, name, res)
    ;
    ;     <!-- inherited from Overlay element -->
    ;     <color>ffffffff</color>                   <!-- kml:color -->
    ;     <drawOrder>0</drawOrder>                  <!-- int -->
    ;     <Icon>...</Icon>

    kml = kml_overlay_attr(kml, icon, res)

    ;   <!-- specific to PhotoOverlay -->
    ;   <rotation>0</rotation>                <!-- kml:angle180 -->
    ;   <ViewVolume>
    ;     <leftFov>0</leftFov>                <!-- kml:angle180 -->
    ;     <rightFov>0</rightFov>              <!-- kml:angle180 -->
    ;     <bottomFov>0</bottomFov>            <!-- kml:angle90 -->
    ;     <topFov>0</topFov>                  <!-- kml:angle90 -->
    ;     <near>0</near>                      <!-- double -->
    ;   </ViewVolume>

    rotation  = get_res_value_keep(res,"kmlRotation",0)
    leftfov   = get_res_value_keep(res,"kmlLeftFov" ,0)
    rightfov  = get_res_value_keep(res,"kmlRightFov" ,0)
    bottomfov = get_res_value_keep(res,"kmlBottomFov" ,0)
    topfov    = get_res_value_keep(res,"kmlTopFov" ,0)
    near      = get_res_value_keep(res,"kmlNear" ,0)

    kml_entry = (/   \
                "<rotation>"+rotation+"</rotation>", \
                "<ViewVolume>", \
                "  <leftFov>"+leftfov+"</leftFov>", \
                "  <rightFov>"+rightfov+"</rightFov>", \
                "  <bottomFov>"+bottomfov+"</bottomFov>", \
                "  <topFov>"+topfov+"</topFov>", \
                "  <near>"+near+"</near>", \
                "</ViewVolume>" \
                /)
    kml_setline(kml,kml_entry)
    delete(kml_entry)

      ;   <ImagePyramid>
      ;     <tileSize>256</tileSize>            <!-- int -->
      ;     <maxWidth>...</maxWidth>            <!-- int -->
      ;     <maxHeight>...</maxHeight>          <!-- int -->
      ;     <gridOrigin>lowerLeft</gridOrigin>  <!-- lowerLeft or upperLeft-->
      ;   </ImagePyramid>

    if (isatt(res,"kmlImagePyramid")) then
      if (res@kmlImagePyramid) then

        tilesize    = get_res_value_keep(res,"kmlTileSize",256)
        gridorigin  = get_res_value_keep(res,"kmlGridOrigin" ,"lowerLeft")

        kml_entry = (/   \
                    "<ImagePyramid>", \
                    "  <tileSize>"+tilesize+"</tileSize>", \
                    "  <maxWidth>"+res@kmlMaxWidth+"</maxWidth>", \
                    "  <maxHeight>"+res@kmlMaxHeight+"</maxHeight>", \
                    "  <gridOrigin>"+gridorigin+"</gridOrigin>", \
                    "</ImagePyramid>" \
                    /)
        kml_setline(kml,kml_entry)
        delete(kml_entry)
      end if
    end if

    ;   <Point>
    ;     <coordinates>...</coordinates>      <!-- lon,lat[,alt] -->
    ;   </Point>
    ;   <shape>rectangle</shape>              <!-- kml:shape -->
    ; </PhotoOverlay>

    kml = kml_add_point(kml, x, y, z, 0)
    
    shape = get_res_value_keep(res,"kmlShape","rectangle")
    kml_setline(kml,"<shape>"+shape+"</shape>")
    
    kml_setline(kml,"</PhotoOverlay>")

  return(kml)
end
; ***************************************************************** 


