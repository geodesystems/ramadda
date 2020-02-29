//
//Provides a wrapper around using the google earth plugin
//Supports multiple google earths in one web page
//

//list of all the RamaddaEarth objects 
var  ramaddaEarths = new Array();
var RAMADDA_EARTH_DEFAULT_RANGE = 4999999;

function RamaddaBounds() {
    this.maxLat = -90;
    this.minLat = 90;
    this.maxLon = -180;
    this.minLon = 180;


    this.getCenterLat = function() {
        return this.minLat + (this.maxLat-this.minLat)/2;
    }
    this.getCenterLon = function() {
        return this.minLon + (this.maxLon-this.minLon)/2;
    }

    this.merge = function(that) {
        this.maxLat =  Math.max (this.maxLat, that.maxLat);
        this.maxLon =  Math.max (this.maxLon, that.maxLon);
        this.minLat =  Math.min (this.minLat, that.minLat);
        this.minLon =  Math.min (this.minLon, that.minLon);
    }

    this.setLatLon = function(lat, lon) {
        this.maxLat =  Math.max (this.maxLat, lat);
        this.maxLon =  Math.max (this.maxLon, lon);
        this.minLat =  Math.min (this.minLat, lat);
        this.minLon =  Math.min (this.minLon, lon);
    }


    this.isLargeArea = function() {
        return (this.maxLon-this.minLon)>300;
    }
    this.getRange = function() {
        if(this.minLat == this.maxLat) {
            return  10000;
        }

        if(this.isLargeArea()) {
            return RAMADDA_EARTH_DEFAULT_RANGE;
        }

        //figure out the range based on the bounds
        var len, altV, altH, alt;
        len = ( this.maxLon - this.minLon) *
        60000 * Math.cos( Math.PI / 180 * (( this.maxLat +
                                             this.minLat) / 2 ) );
        altV = len * 1;

        len = ( this.maxLat - this.minLat ) * 60000;
        altH = len * 1 ;
        range =  Math.max (altH, altV) 
        //A fudge factor
        range = range*3;
        return range;
    }
}



//Class for holding placemark info
function RamaddaPlacemark(ramaddaEarth, id, name, desc, lat,lon, detailsUrl, icon, polygons, kmlUrl) {
    this.ramaddaEarth = ramaddaEarth;
    this.id = id;
    this.name = name;
    this.description = desc;
    this.detailsUrl = detailsUrl;
    this.lat = lat;
    this.lon = lon;
    this.icon = icon;
    this.polygons = polygons;
    this.details = null;
    this.bounds = new RamaddaBounds();
    this.visible = true;
    this.features = new Array();
    this.kmlUrl = kmlUrl;
    this.fromTime = null;
    this.toTime = null;
    this.addFeature = function(feature) {
        this.features.push(feature);
    }
    this.isVisible = function() {
        var cbx = GuiUtils.getDomObject("googleearth.visibility." + this.id);
        if(!cbx) {
            return true;
        }
        return  cbx.obj.checked;
    }

    this.checkVisibility = function() {
        var visible = this.isVisible();
        if(visible && !this.placemark) {
        }

        if(visible && !this.kmlFeature && this.kmlUrl) {
            this.ramaddaEarth.loadKml(this.kmlUrl, this);
        }

        if(this.kmlFeature) {
            this.kmlFeature.setVisibility(visible);
        }

        for (var i = 0; i < this.features.length; i++) {
            var feature = this.features[i];
            feature.setVisibility(visible);
        }
    }
}



//Wrapper around an instantiation of a google earth plugin
function  RamaddaEarth(id, url, props) {
    this.googleEarth = null;
    this.placemarksToAdd = new Array();
    this.placemarks = new Array();
    this.url = url;
    this.id = id;
    this.showOverview = true;
    if(props!=null) {
        $.extend(this, props);
    }
    ramaddaEarths[id] = this;


    this.initCallback = function(instance) {
        this.googleEarth = instance;
        this.googleEarth.getWindow().setVisibility(true);

        // add a navigation control
        this.googleEarth.getNavigationControl().setVisibility(this.googleEarth.VISIBILITY_AUTO);

        // add some layers
        this.googleEarth.getLayerRoot().enableLayerById(this.googleEarth.LAYER_BORDERS, true);
        this.googleEarth.getLayerRoot().enableLayerById(this.googleEarth.LAYER_ROADS, true);
        this.googleEarth.getOptions().setFlyToSpeed(0.5);
        this.googleEarth.getOptions().setOverviewMapVisibility(this.showOverview);

        this.gex= new GEarthExtensions(this.googleEarth);



        if(this.url) {
            this.loadKml(this.url);
            this.url = null;
        }
    
        var tmpPlacemarks = this.placemarksToAdd;
        this.placemarksToAdd = new Array();
        var firstPlacemark = null;
        var bounds = new RamaddaBounds();
        for (var i = 0; i < tmpPlacemarks.length; i++) {
            var placemark =  tmpPlacemarks[i];
            if(!firstPlacemark) firstPlacemark = placemark;
            this.addRamaddaPlacemark(placemark, placemark.bounds);
            bounds.merge(placemark.bounds);
        }

        if(firstPlacemark) {
            this.setLocation(bounds.getCenterLat(),bounds.getCenterLon(),
                             bounds.getRange());
        }
    }

    this.loadKml = function(url, ramaddaPlacemark) {
        var theRamaddaEarth = this;
        var callback = function(object) {
            theRamaddaEarth.kmlUrlFinished(object,ramaddaPlacemark);
        }
        google.earth.fetchKml(this.googleEarth, url, callback);
    }

    this.kmlUrlFinished = function(object, ramaddaPlacemark) {
        if (!object) {
            // wrap alerts in API callbacks and event handlers
            // in a setTimeout to prevent deadlock in some browsers
            setTimeout(function() {
                    alert('Bad or null KML.');
                }, 0);
            return;
        }
        this.googleEarth.getFeatures().appendChild(object);
        if (object.getAbstractView()) {
            this.googleEarth.getView().setAbstractView(object.getAbstractView());
        }
        object.setVisibility(true);
        /*
        if(object.getFeatures) {
            var features = object.getFeatures();
            var children = features.getChildNodes();
            for(i=0;i<children.getLength();i++) {
                var child = children.item(i);

            }
            alert(children);
            }*/

        if(ramaddaPlacemark) {
            ramaddaPlacemark.kmlFeature = object;
            if(ramaddaPlacemark.timePrimitive) {
                ramaddaPlacemark.kmlFeature.setTimePrimitive(ramaddaPlacemark.timePrimitive);
            }
        }
    }


    function failureCallback(errorCode) {
        alert("Failure loading the Google Earth Plugin: " + errorCode);
    }

    this.addPlacemark = function(id, name, desc, lat,lon, detailsUrl, icon, points, kmlUrl, fromTime, toTime) {
        var polygons = new Array();
        if(points) {
            var tmpArray = new Array();
            polygons.push(tmpArray);
            var lastLon = points[1];
            var lastLat = points[0];
            for(i=0;i<points.length;i+=2) {
                var newlat = points[i];
                var newlon = points[i+1];
                //TODO: interpolate the latitude
                if(false  && lastLon!=newlon) {
                    var crosses = false;
                    if(lastLon<-90 && newlon>90) {
                        tmpArray.push(newlat);
                        tmpArray.push(-180);
                        tmpArray = new Array();
                        tmpArray.push(newlat);
                        tmpArray.push(180);
                        polygons.push(tmpArray);
                    } else  if(lastLon>90 && newlon<-90) {
                        tmpArray.push(newlat);
                        tmpArray.push(180);
                        tmpArray = new Array();
                        tmpArray.push(newlat);
                        tmpArray.push(-180);
                        polygons.push(tmpArray);
                    }
                    lastLon  = newlon;
                    lastLat  = newlat;
                }
                tmpArray.push(newlat);
                tmpArray.push(newlon);
            }
        }
        if(desc.indexOf("base64:")==0) {
            desc =             window.atob(desc.substring(7));
        }


        pm = new RamaddaPlacemark(this, id, name, desc, lat,lon,detailsUrl, icon, polygons,kmlUrl)
        pm.fromTime = fromTime;
        pm.toTime = toTime;
        this.placemarks[id] = pm;
        this.addRamaddaPlacemark(pm);
    }

    this.addRamaddaPlacemark = function(ramaddaPlacemark, bounds) {
        if (!this.googleEarth) {
            this.placemarksToAdd.push(ramaddaPlacemark);
            return;
        }
        if(bounds) {
            bounds.setLatLon(ramaddaPlacemark.lat, ramaddaPlacemark.lon);
        }

        var gePlacemark = this.googleEarth.createPlacemark('');
        var _this = this;
        google.earth.addEventListener(gePlacemark, 'click', function(event) {
                // Prevent the default balloon from appearing.
                event.preventDefault();
                _this.entryClicked(ramaddaPlacemark.id, true);
            });

        /*
          Don't set the time for now since the GE time animation widget is so bad
          if(ramaddaPlacemark.fromTime!=null) {
            if(ramaddaPlacemark.toTime) {
                var timeSpan = this.googleEarth.createTimeSpan(ramaddaPlacemark.id);
                timeSpan.getBegin().set(ramaddaPlacemark.fromTime.replace("GMT",""));
                timeSpan.getEnd().set(ramaddaPlacemark.toTime.replace("GMT",""));
                ramaddaPlacemark.timePrimitive  = timeSpan;
            } else {
                var timeStamp = this.googleEarth.createTimeStamp(ramaddaPlacemark.id);
                time = ramaddaPlacemark.fromTime.replace("GMT","");
                timeStamp.getWhen().set(time);
                ramaddaPlacemark.timePrimitive = timeStamp;
            }
        }
        */

        
        if(ramaddaPlacemark.timePrimitive)
            gePlacemark.setTimePrimitive(ramaddaPlacemark.timePrimitive);

        ramaddaPlacemark.placemark = gePlacemark;
        ramaddaPlacemark.addFeature(gePlacemark);
        gePlacemark.setName(ramaddaPlacemark.name);
        if(ramaddaPlacemark.description) {
            gePlacemark.setDescription(ramaddaPlacemark.description);
        }
        var point = this.googleEarth.createPoint('');
        point.setLatitude(ramaddaPlacemark.lat);
        point.setLongitude(ramaddaPlacemark.lon);
        gePlacemark.setGeometry(point);
        if(ramaddaPlacemark.icon) {
            var icon = this.googleEarth.createIcon('');
            icon.setHref(ramaddaPlacemark.icon);
            var style = this.googleEarth.createStyle(''); 
            style.getIconStyle().setIcon(icon); 
            gePlacemark.setStyleSelector(style); 
        }
        this.googleEarth.getFeatures().appendChild(gePlacemark);

        if(ramaddaPlacemark.polygons) {
            for(polygonIdx=0;polygonIdx<ramaddaPlacemark.polygons.length;polygonIdx++) {
                var points = ramaddaPlacemark.polygons[polygonIdx];
                var lineString = this.googleEarth.createLineString('');
                lineString.setTessellate(true);
                lineString.setAltitudeMode(this.googleEarth.ALTITUDE_CLAMP_TO_GROUND);
                for (i = 0; i < points.length; i+=2) {
                    if(bounds) {
                        bounds.setLatLon(points[i],points[i+1])
                    }
                    lineString.getCoordinates().pushLatLngAlt(points[i], points[i+1], 0);
                }

                var lineStringPlacemark = this.googleEarth.createPlacemark('');
                lineStringPlacemark.setGeometry(lineString);
                lineStringPlacemark.setStyleSelector(this.googleEarth.createStyle(''));
                var lineStyle = lineStringPlacemark.getStyleSelector().getLineStyle();
                lineStyle.setWidth(2);
                lineStyle.getColor().set('ff0000ff');  // aabbggrr format
                if(ramaddaPlacemark.timePrimitive)
                    lineStringPlacemark.setTimePrimitive(ramaddaPlacemark.timePrimitive);
                ramaddaPlacemark.addFeature(lineStringPlacemark);
                this.googleEarth.getFeatures().appendChild(lineStringPlacemark);
            }
        }

        ramaddaPlacemark.checkVisibility();
    }


    this.placemarkClick = function(id, popup) {
        placemark =this.placemarks[id];
        if(!placemark) {
            alert("no placemark");
            return;
        }
        this.goToPlacemark(placemark);

        if(!popup && !showDetails()) {
            return;
        }
        var content = placemark.description;
        var balloon = this.googleEarth.createHtmlStringBalloon('');
        balloon.setFeature(placemark.placemark);
        //        balloon.setMaxHeight(100);
        balloon.setContentString(content);
        this.googleEarth.setBalloon(balloon);
    }


    this.setLocation = function(lat,lon, range) {
        if (!this.googleEarth) return;
        var lookAt = this.googleEarth.getView().copyAsLookAt(this.googleEarth.ALTITUDE_RELATIVE_TO_GROUND);
        lookAt.setLatitude(lat);
        lookAt.setLongitude(lon);
        if(range) {
            lookAt.setRange(range);
        }
        this.googleEarth.getView().setAbstractView(lookAt);
    }

    this.showDetails = function() {
        var cbx = GuiUtils.getDomObject("googleearth.showdetails");
        if(cbx) {
            return cbx.obj.checked;
        }
        return true;
    }

    this.zoomOnClick = function() {
        var cbx = GuiUtils.getDomObject("googleearth.zoomonclick");
        if(cbx) {
            return cbx.obj.checked;
        }
        return true;
    }

    this.getThis = function() {
        return this;
    }

    this.googleEarthClickCnt=0;
    this.goToPlacemark = function(placemark) {
        if(this.zoomOnClick()) {
            if(placemark.kmlFeature) {
                this.gex.util.flyToObject(placemark.kmlFeature);
                return;
            }
            if(placemark.bounds.isLargeArea()) {
                this.setLocation(placemark.lat,placemark.lon,
                                 placemark.bounds.getRange());
            } else {
                this.setLocation(placemark.bounds.getCenterLat(),placemark.bounds.getCenterLon(),
                                 placemark.bounds.getRange());
            }
        } else {
            if(placemark.bounds.isLargeArea()) {
                this.setLocation(placemark.lat,placemark.lon); 
            } else {
                this.setLocation(placemark.bounds.getCenterLat(),placemark.bounds.getCenterLon());
            }
        }
    }


    this.togglePlacemarkVisible = function(id) {
        ramaddaPlacemark =this.placemarks[id];
        if(!ramaddaPlacemark) {
            return;
        }
        ramaddaPlacemark.checkVisibility();
    }


    this.entryClicked = function(id, force) {
        var _this = this;
        this.googleEarthClickCnt++;
        var myClick = this.googleEarthClickCnt;
        ramaddaPlacemark =this.placemarks[id];
        if(!ramaddaPlacemark) {
            return;
        }
        this.googleEarth.setBalloon(null);
        this.goToPlacemark(ramaddaPlacemark);
        if(!force && !this.showDetails()) {
            return;
        }
        //Have we gotten the details already?
        if(ramaddaPlacemark.details) {
            this.setBalloon(ramaddaPlacemark,ramaddaPlacemark.details); 
            return;
        }

        //For now just use the description in the placemark
        var content = ramaddaPlacemark.description;
        this.setBalloon(ramaddaPlacemark,ramaddaPlacemark.description); 
        return;


        var callback = function(request) {
            if(myClick != _this.googleEarthClickCnt) {
                return;
            }
            var xmlDoc=request.responseXML.documentElement;
            var text = getChildText(xmlDoc);
            Utils.checkTabs(text);
            ramaddaPlacemark.details = text;
            ramaddaPlacemark.placemark.setDescription(text);
            _this.setBalloon(ramaddaPlacemark,text); 
            if(ramaddaPlacemark.detailsUrl) {
                GuiUtils.loadUrl(ramaddaPlacemark.detailsUrl, callback,"");
            }
        }
    }


    this.setBalloon = function(ramaddaPlacemark, text) {
        var balloon = this.googleEarth.createHtmlStringBalloon('');
        balloon.setFeature(ramaddaPlacemark.placemark);
        balloon.setContentString(text);
        balloon.setMaxHeight(300);
        balloon.setMaxWidth(500);
        this.googleEarth.setBalloon(balloon);
    }

    var theRamaddaEarth = this;
    var callback = function(instance) {
        theRamaddaEarth.initCallback(instance);
    }
    google.earth.createInstance(this.id, callback, this.failureCallback);
}
