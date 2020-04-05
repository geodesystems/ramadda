;-----------------------------------------------------------------------------
;
; remove_middle_contours.ncl
; Remove one or more middle contour levels from a contour plot.
;
; This is a helper function to make symmetric color plots, such
; as white-in-the-middle plots, with a wide gap between the middle
; contour levels.  The purpose is to emphasize larger deviations
; from the center, such as when plotting anomalies and correlations.
;
; This function permits the use of existing automatic NCL methods
; to scale the plot to the input data, without special code to
; explicitly compute contour levels or numeric control parameters.
;
; 2011-oct-27	Original version.  By Dave Allured, NOAA/PSL/CIRES.
;
; Acknowledgements:  This function combines methods and ideas from
; Mary Haley, NCAR/CISL; Gibies George, IITM; and Cathy Smith,
; NOAA/PSL/CIRES.
;
; Input:
;
;	plot = graphic; preliminary plot object made by any
;		NCL contour plot function.
;	res = resource variable used to make the preliminary plot.
;	opt = specification for which middle levels to remove.
;		If integer: opt is the number of levels to remove.
;		If any other numeric type: opt is a data threshold;
;		  remove levels below opt, either side of zero.
;
; Function result:
;
;	Plot resource variable, copy of "res" input resources,
;	conservatively modified to create a plot with fewer contour
;	levels than the original.  The following resources are
;	changed, all others are preserved exactly:
;
;	res@cnLevels = Exactly the same contour levels as in the
;	  original plot, except middle level(s) removed.  It is not
;	  necessary that the original resource variable contained a
;	  cnLevels resource.
;
;	res@cnLevelSelectionMode = Set to "ExplicitLevels".  The
;	  original cnLevelSelectionMode, if present, is deleted.
;	  Note that setting this mode disables certain other
;	  resources that are now left over and unwanted, such as
;	  cnMinLevelValF, cnMaxLevelValF, and cnLevelSpacingF.
;
; Usage:
;
; First make a preliminary contour plot graphic object, with any
; valid NCL contour plot function.  Then call this function to
; remove selected contour level(s).  Lastly, call the same NCL plot
; function a second time, to make the final plot.  Example:
;
;	res@gsnDraw  = False	; disable output for preliminary plot
;	res@gsnFrame = False
;	plot = gsn_csm_contour_map (wks, data, res)  ; make prelim plot
;
;	opt = 1			; remove one level for white in middle
;	res2 = remove_middle_contours (plot, res, opt)
;
;	res2@gsnDraw  = True	; enable output for final plot
;	res2@gsnFrame = True
;	plot = gsn_csm_contour_map (wks, data, res2)
;				; make final plot with middle contour
;				; removed; overwrite previous plot oblect
;
; Notes:
;
; This function DOES NOT locate the zero contour level in the data.
; It ASSUMES that the middle levels in the original input plot are
; centered on the numeric level(s) to be removed.  If removal of
; level(s) around zero is desired, then it is the caller's
; responsibility to create input with levels symmetric around zero.
;
; A side effect of contour plots that are both symmetric and have a
; single contour band in the middle, is that there must be an even
; number of contour levels.  Therefore, this function normally
; outputs plot instructions to make an even number of contour levels.
;
; The only exception is the degenerate case with only one input level.
; In this case, it is impossible to delete contour levels, and still
; have any left over to make a meaningful contour plot.
;
; This version attempts to make a reasonable guess, rather than
; aborting, when confronted with inconsistent instructions such as
; removing more levels than available in the original plot.
;
;-----------------------------------------------------------------------------

undef  ("remove_middle_contours")
function remove_middle_contours (plot, res, opt)

local j, k, levels, nlevs, nkeep1, nkeep2, nkeep3, nrem1, nrem2, nrem3, res2

begin
  if (typeof (plot) .ne. "graphic") then
    print ("*** remove_middle_contours: First argument 'plot' must be graphic.")
    print ("*** Current type is " + typeof (plot) + ".  Abort.")
    status_exit (1)
  end if

; Retrieve original contour levels, computed by any NCL method.

  if (isatt (plot, "contour")) then	; method for map plots
     getvalues plot@contour
       "cnLevels" : levels
     end getvalues

   else					; method for simple contour plots
     getvalues plot
       "cnLevels" : levels
     end getvalues
  end if

;;  print (levels)

; If opt is type integer, then it is the number of levels to remove.
; If opt is any other type, then it is an absolute value threshold.
  
  if (typeof (opt) .eq. "integer") then
    nrem1 = opt
  else
    nrem1 = num (abs (levels(:)) .lt. opt)
  end if
  
;;  print ("nrem1 = " + nrem1)

; Compute number of levels to remove, and number to keep.  Apply constraints.

  nlevs = dimsizes (levels)		; number of original levels
  nrem2 = max ((/ nrem1, 1 /))		; try to remove at least one level

  nkeep1 = nlevs - nrem2		; compute number to keep
  nkeep2 = nkeep1 - (nkeep1 % 2)	; if odd number, remove one more
  nkeep3 = max ((/ nkeep2, 2 /))	; must have at least two final levels

  if (nkeep3 .ge. nlevs) then		; if number of levels is not decreased:
    return (res)			;   do not change plot,
  end if				;   do not modify original resources

  nrem3 = nlevs - nkeep3		; final number of levels to remove

;;  print ("Original nlevels, keep, remove = " + nlevs +" "+ nkeep3 +" "+ nrem3)

; Begin output.  Copy all original resources.

  res2 = res				; copy of input resources

  if (isatt (res2, "cnLevels")) then	; remove resources to be modified,
    delete (res2@cnLevels)		; avoid shape conflicts
  end if

  if (isatt (res2, "cnLevelSelectionMode")) then
    delete (res2@cnLevelSelectionMode)
  end if

; Remove computed number of middle levels.

  k = (nkeep3 / 2)			; start of upper half levels in output
  j = k + nrem3				; start of upper half levels in input

;;  print ("j, k = " + j +" "+ k)

  res2@cnLevels = levels(0:nkeep3-1)	; make new explicit levels array
  					; with reduced number of levels;
					; also copy lower half of levels to keep

  res2@cnLevels(k:) = levels(j:)	; copy upper half of levels to keep

  res2@cnLevelSelectionMode = "ExplicitLevels"	; set mode to explicit,
  						; replace any previous mode
;;  print (res2@cnLevels)

  return (res2)				; return modified resources;
end					; user makes the final plot
