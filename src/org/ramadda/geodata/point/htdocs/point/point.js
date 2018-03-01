
var timeSeriesMarker;
var lastBoxId;
var lastMap;
var lastEntryId;
var lastPointIndex=0;
var pointDataDomainBase = "points";

function getKeyChar(event) {
    event = util.getEvent(event);
    if(event.keyCode) {
        return String.fromCharCode(event.keyCode);
    }
    if(event.which)  {
        return String.fromCharCode(event.which);
    }
    return '';
}



function timeSeriesKeyPress(event) {
    var key = getKeyChar(event);
    if (lastBoxId && key=="n") {
        lastPointIndex++;
        setPointIndex(lastMap, lastEntryId, lastBoxId, lastPointIndex);
    }
}

//document.onkeypress = timeSeriesKeyPress;




function  onClickMap( point) { 
    alert('click');
}

function handleGetLatLon(request, map) {
    var xmlDoc=request.responseXML.documentElement;
    jsonText = getChildText(xmlDoc);
    if(JSON) {
        point = JSON.parse(jsonText);
    }  else {
        point = eval('(' + jsonText + ')');
    }
    //    alert(point.latitude+" " + point.longitude);
    if(timeSeriesMarker && map) {
        map.removeMarker(timeSeriesMarker);
    } 
    var location = new OpenLayers.LonLat(point.longitude,point.latitude);
    timeSeriesMarker = map.addMarker("nlasmarker", location, urlroot+"/point/laser.png");
    map.addMarker(timeSeriesMarker);
}




function timeSeriesClick(jqueryObject,event, map, entryId, imgId, boxId, minIndex,  maxIndex, minX, maxX, waveformName) {
    var theImage = jQuery("#" + imgId);
    var offset = theImage.offset();
    var imageHeight = theImage.height();
    var imageX = event.pageX-offset.left;
    if((imageX-minX)<0) {
        return;
    }

    var pointDelta = imageX-minX;
    var indexPerX = (maxIndex - minIndex) / (maxX - minX);
    var pointIndex = Math.round(indexPerX*pointDelta);
    if(pointIndex>maxIndex) pointIndex = maxIndex;
    lastBoxId = boxId;
    lastMap = map;
    lastEntryId = entryId;

    var box = util.getDomObject(boxId);
    var waveformImage = util.getDomObject("point_waveform_image");
    var waveformLink = util.getDomObject("point_waveform_csv");

    if(box) {
        show(boxId);
        var style = util.getStyle(box);
        style.visibility =  "visible";
        var topMargin  = 8;
        var bottomMargin  = 30;
        style.height=imageHeight-(topMargin+bottomMargin);
        jQuery("#"+boxId ).position({
                of: jQuery( "#" + imgId ),
                    my: 'left top',
                    at: 'left top',
                    offset: imageX +" " + topMargin,
                    collision: "none none"
                    });



    }

    if(waveformImage) {
        var waveformUrl = urlroot +"/entry/show?entryid=" + entryId +"&output=" + pointDataDomainBase + ".waveformimage&pointindex=" + pointIndex +"&waveform.name=" + waveformName;
        waveformImage.obj.src = waveformUrl;
    }

    if(waveformLink) {
        var waveformUrl = urlroot +"/entry/show?entryid=" + entryId +"&output=" + pointDataDomainBase + ".waveformcsv&pointindex=" + pointIndex +"&waveform.name=" + waveformName;
        waveformLink.obj.href= waveformUrl;
    }

    if(map) {
        var url = urlroot +"/entry/show?entryid=" + entryId +"&output="  + pointDataDomainBase + ".getlatlon&pointindex=" + pointIndex;
        util.loadXML( url, handleGetLatLon,map);
    }

}
