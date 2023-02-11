

var LINETYPE_STRAIGHT='straight';
var LINETYPE_GREATCIRCLE='greatcircle';
var LINETYPE_CURVE='curve';
var LINETYPE_STEPPED='stepped';        
var ID_ADDDOTS = 'adddots';
var ID_LINETYPE = 'linetype';

function MapGlyph(display,type,attrs,feature,style,fromJson,json) {

    if(!type) {
	console.log("no type given for MapGlyph");
	console.trace();
	return
    }
    style = style??{};
    if(style.mapOptions) {
	delete style.mapOptions;
    }
    this.transientProperties = {};

    let glyphType = display.getGlyphType(type);
    if(attrs.routeProvider)
	this.name = "Route: " + attrs.routeProvider +" - " + attrs.routeType;
    else 
	this.name = attrs.name || glyphType.getName() || type;
    let mapGlyphs = attrs.mapglyphs;
    if(attrs.mapglyphs) delete attrs.mapglyphs;
    if(mapGlyphs){
	mapGlyphs = mapGlyphs.replace(/\\n/g,"\n");
	this.putTransientProperty("mapglyphs", mapGlyphs);
    }
    this.display = display;
    this.type = type;
    this.features = [];
    this.attrs = attrs;
    this.style = style;
    this.id = attrs.id ?? HU.getUniqueId("glyph_");
    if(this.isEntry()) {
	if(!Utils.isDefined(this.attrs.useentryname))
	    this.attrs.useentryname = true;
	if(!Utils.isDefined(this.attrs.useentrylabel))
	    this.attrs.useentrylabel = true;	
    }


    //Get the style with the macros applied
    style = this.getStyle(true);
    if(fromJson) {
	if(this.isStraightLine()) {
	    this.checkLineType(json.points);
	} else {
	    feature = this.display.makeFeature(this.display.getMap(),json.geometryType, style,
					       json.points);
	}
    }
    if(feature) {
	this.addFeature(feature);
	feature.style = style;
	this.display.addFeatures([feature]);
    }
    if(fromJson) {
	if(this.isImage()) {
	    this.checkImage(feature);
	}

	//If its an entry then fetch the entry info from the repository and use the updated lat/lon and name
	if(this.isEntry()) {
	    let callback = (entry)=>{
		//the mapglyphs are defined by the type
		this.putTransientProperty("mapglyphs", entry.mapglyphs);
		if(this.getUseEntryName()) 
		    this.setName(entry.getName());
		if(this.getUseEntryLabel())
		    this.style.label= entry.getName();
		if(this.getUseEntryLocation() && entry.hasLocation()) {
		    let feature = this.display.makeFeature(this.getMap(),"OpenLayers.Geometry.Point", this.style,
						   [entry.getLatitude(), entry.getLongitude()]);
		    feature.style = this.style;
		    this.addFeature(feature,true,true);
		}

		this.applyEntryGlyphs();
		this.applyMapStyle();
		this.display.redraw(this);
		this.display.makeLegend();
	    };
	    getRamadda().getEntry(this.attrs.entryId, callback);
	}
    }


    if(this.isRings()) {
	this.checkRings();
    }


}


var ID_MAPFILTERS = 'mapfilters';
var ID_MAPLEGEND = 'maplegend';


MapGlyph.prototype = {
    CLASS_NAME:'MapGlyph',
    animationInfo:{},
    domId:function(id) {
	return this.getId() +'_'+this.display.domId(id);
    },
    jq:function(id) {
	return jqid(this.domId(id));
    },
    getFixedId: function() {
	return this.domId('_fixed');
    },
    initSideHelp:function(dialog) {
	let _this = this;
	dialog.find('.imdv-property-popup').click(function() {
	    let id  = $(this).attr('info-id');
	    let target  = $(this).attr('target');	    
	    let info = _this.getFeatureInfo(id);
	    if(!info) return;
	    let html = HU.b(info.getLabel());
	    let items =   ['show=true','label=','filter.first=true','type=enum']
	    if(info.isNumeric()) {
		items.push('format.decimals=0',
			   'filter.min=0',
			   'filter.max=100',
			   'filter.animate=true',
			   'filter.animate.step=1',
			   'filter.animate.sleep=100',
			   'filter.live=true');

	    }
	    items.push('colortable.select=true');

	    items.forEach(item=>{
		let label = item.replace('=.*','');
		html+=HU.div(['style','margin-left:10px;', 'class','ramadda-menu-item ramadda-clickable','item',item],item);
	    });

	    html = HU.div(['style','margin-left:10px;margin-right:10px;'],html);
	    let dialog =  HU.makeDialog({content:html, anchor:$(this)});
	    dialog.find('.ramadda-clickable').click(function() {
		dialog.remove();
		let item = $(this).attr('item');
		let line = info.id+'.' + item+'\n';
		let textComp = GuiUtils.getDomObject(target);
		if(textComp) {
		    insertAtCursor('', textComp.obj, line);
		}
	    });
	});
    },

    getElevations:async function(points,callback,update) {
	let elevations = points.map(()=>{return 0;});
	let ok = true;
	let count=0;
	for(let i=0;i<points.length;i++) {
	    if(!ok) break;
	    let point = points[i];
	    let url = "https://nationalmap.gov/epqs/pqs.php?x="
		+ point.x + "&y=" + point.y
                + "&units=feet&output=json";
	    //	    console.log('url:'+ url);
            await  $.getJSON(url, (data)=> {
		let elevation = data?.USGS_Elevation_Point_Query_Service?.Elevation_Query?.Elevation;
		elevations[i]= elevation;
		count++;
		if(update)
		    ok = update(count,points.length);
		//		console.log("elevation #" + elevations.length+"/" + points.length+": " + elevation);
	    }).fail((data)=>{
		console.log('Failed to find elevation');
		console.dir(data);
		elevations.push(NaN);
	    });
	}
	callback(elevations,ok);
    },
    getIcon: function() {
	return this.attrs.icon??this.display.getGlyphType(this.getType()).getIcon();
    },
    putTransientProperty(name,value) {
	this.transientProperties[name] = value;
    },
    getTransientProperty(name) {
	return this.transientProperties[name];
    },    
    clone: function() {
	let style = Utils.clone(this.style);
	let attrs = Utils.clone(this.attrs);
	let cloned =  new MapGlyph(this.display, this.type,attrs,null,style);
	//give it a new ID
	cloned.id = HU.getUniqueId("glyph_");
	//clone the features and their styles
	let features = this.features.map(f=>{
	    f = f.clone();
	    if(f.style) {
		if(f.style.fixed) {
		    f.style = Utils.clone(f.style);
		} else {
		    f.style = style;
		}
	    }
	    f.layer=this.display.myLayer;
	    f.mapGlyph = cloned;
	    return f;
	});
	cloned.features=features;
	this.display.addFeatures(features);
	return cloned;
    },
    makeJson:function() {
	let attrs=this.getAttributes();
	let obj = {
	    mapOptions:attrs,
	    id:this.getId()
	};
	let style = this.getStyle();
	if(this.getMapLayer()) {
	    style = this.getMapLayer().style||style;
	}
	if(style) {
	    style = $.extend({},style);
	    if(this.getImage() && Utils.isDefined(this.getImage().opacity)) {
		style.imageOpacity=this.getImage().opacity;
	    }
	    obj.style = style;
	}
	obj.points = this.getPoints(obj);
	if(this.children) {
	    let childrenJson=[];
	    this.children.forEach(child=>{
		if(child.isEphemeral) return;
		childrenJson.push(child.makeJson());
	    });
	    obj.children = childrenJson;
	}
	return obj;
    },	

    applyStyleToChildren:function(prop,value) {
	if(!this.children) return;
	this.children.forEach(child=>{
	    if(child.style) {
		child.style[prop] = value;
		child.applyStyle(child.style);
		child.applyStyleToChildren(prop,value);
		this.display.redraw(child);
	    }
	});
    },
    getPoints:function(obj) {
	if(this.attrs.originalPoints) return this.attrs.originalPoints;
	let geom = this.getGeometry();
	if(!geom) return null;
	obj.geometryType=geom.CLASS_NAME;
	let points = obj.points=[];
	let vertices  = geom.getVertices();
	let  p =d=>{
	    return Utils.trimDecimals(d,6);
	};
	if(false && this.getImage()) {
	    let b = this.getMap().transformProjBounds(geom.getBounds());
	    points.push(p(b.top),p(b.left),
			p(b.top),p(b.right),
			p(b.bottom),p(b.right),
			p(b.bottom),p(b.left));				    
	} else {
	    vertices.forEach(vertex=>{
		let pt = vertex.clone().transform(this.getMap().sourceProjection, this.getMap().displayProjection);
		points.push(p(pt.y),p(pt.x));
	    });
	}
	return points;
    },

    isMultiEntry:  function() {
	return this.type == GLYPH_MULTIENTRY;
    },
    getEntryGlyphs:function(checkTransient) {
	if(Utils.stringDefined(this.attrs.entryglyphs))
	    return this.attrs.entryglyphs;
	if(checkTransient)
	    return this.transientProperties.mapglyphs;
	return null;
    },
    getRadii:function() {
	if(!this.attrs.radii) {
	    let level = this.display.getCurrentLevel();
	    //	    console.log("level:" + level);
	    let r = (size,unit) =>{
		let s =[];
		for(let i=1;i<=5;i++) {
		    s.push((size*i)+unit);
		}
		this.attrs.radii = s;
		return s;
	    }
	    if(level>=19) return r(25,UNIT_FT);
	    if(level>=18) return r(50,UNIT_FT);
	    if(level>=17) return r(75,UNIT_FT);	    
	    if(level>=16) return r(150,UNIT_FT);
	    if(level>=15) return r(250,UNIT_FT);	    
	    if(level>=14) return r(500,UNIT_FT);
	    if(level>=13) return r(1500,UNIT_FT);
	    if(level>=12) return r(0.5,UNIT_MILES);
	    if(level>=11) return r(1,UNIT_MILES);
	    if(level>=10) return r(2,UNIT_MILES);
	    if(level>=9) return r(2.5,UNIT_MILES);	    	    	    
	    if(level>=8) return r(5,UNIT_MILES);
	    if(level>=7) return r(10,UNIT_MILES);
	    if(level>=6) return r(25,UNIT_MILES);
	    if(level>=5) return r(50,UNIT_MILES);
	    if(level>=4) return r(100,UNIT_MILES);	    	    	    	    
	    if(level>=3) return r(250,UNIT_MILES);	    	    	    	    
	    return	 r(500,UNIT_MILES);
	}
	return this.attrs.radii;
    },
    addToStyleDialog:function(style) {
	if(this.isRings()) {
	    return HU.formEntry('Rings Radii:',HU.input('',Utils.join(this.getRadii(),','),
							['id',this.domId('radii'),'size','40'])+' e.g., 1km, 2mi (miles), 100ft') +
		HU.formEntryTop('Rings Labels:',
				HU.hbox([HU.input('',this.attrs.rangeRingLabels??'',
						  ['id',this.domId('rangeringlabels'),'size','40']),
					 'Use ${d} macro for the distance e.g.:<br> Label 1 ${d}, ..., Label N ${d}  '])) +

		HU.formEntry('Ring label angle:',
			     HU.input('',Utils.isDefined(this.attrs.rangeRingAngle)?this.attrs.rangeRingAngle:90+45,[
				 'id',this.domId('rangeringangle'),'size',4]) +' Leave blank to not show labels') +
		HU.formEntryTop('Ring Styles',
				HU.hbox([HU.textarea('',this.attrs.rangeRingStyle??'',['id',this.domId('rangeringstyle'),'rows',5,'cols', 40]),
					 'Format:<br>ring #,style:value,style:value  e.g.:<br>1,fillColor:red,strokeColor:blue<br>2,strokeDashstyle:dot|dash|dashdot|longdash<br>N,strokeColor:black<br>*,strokeWidth:5<br>even,...<br>odd,...']));
	}
	if(this.isMapServer() && this.getDatacubeVariable()) {
	    return HU.formEntry('Color Table:',HU.div(['id',this.domId('colortableproperties')]));
	}	    

	if(this.isStraightLine()) {
	    return HU.formEntry('Line type:',
				HU.select('',['id',this.domId(ID_LINETYPE)],[
				    {value:LINETYPE_STRAIGHT,label:'Straight'},
				    {value:LINETYPE_STEPPED,label:'Stepped'},
				    {value:LINETYPE_CURVE,label:'Curve'},
				    {value:LINETYPE_GREATCIRCLE,label:'Great Circle'}],
					  this.attrs.lineType)) +
		HU.formEntry('',HU.checkbox(this.domId(ID_ADDDOTS),['id',this.domId(ID_ADDDOTS)],this.attrs.addDots,'Add dots'));
	}

	return '';
    },

    addToPropertiesDialog:function(content,style) {
	let html='';
	let layout = (lbl,widget)=>{
	    html+=HU.b(lbl)+'<br>'+widget+'<br>';
	}
	let nameWidget = HU.input('',this.getName(),['id',this.domId('mapglyphname'),'size','40']);
	if(this.isEntry()) {
	    nameWidget+='<br>' +HU.checkbox(this.domId('useentryname'),[],this.getUseEntryName(),'Use name from entry');
	    nameWidget+=HU.space(3) +HU.checkbox(this.domId('useentrylocation'),[],this.getUseEntryLocation(),'Use location from entry');
	}
	html+=HU.b('Name: ') +nameWidget+'<br>';
	if(this.isEntry()) {
	    layout('Glyphs:</b> <a target=_help href=https://ramadda.org/repository/userguide/imdv.html#glyphs>Help</a><b>',
		   HU.textarea('',this.getEntryGlyphs()??'',[ID,this.domId('entryglyphs'),'rows',5,'cols', 90]));
	    /*
	      glyph1='type:gauge,color:red,pos:sw,width:50,height:50,dx:20,dy:-30,sizeBy:atmos_temp,sizeByMin:0,sizeByMax:100'
	      glyph2='type:label,pos:sw,dx:25,dy:0,label:${atmos_temp}'
	    */
	}

	let level = this.getVisibleLevelRange()??{};
	html+= HU.checkbox(this.domId('visible'),[],this.getVisible(),'Visible')+'<br>';


	html+=this.display.getLevelRangeWidget(level,this.getShowMarkerWhenNotVisible());
	
	/*
	  let elevButtons = [];
	  if(this.isOpenLine()|| this.mapLayer) {
	  elevButtons.push(HU.span(['style','margin-top:4px;','id',this.domId('makeelevations'),'class','ramadda-clickable'],'Add elevations'));
	  elevButtons.push(HU.span(['style','margin-top:4px;','id',this.domId('clearelevations'),'class','ramadda-clickable'],'Clear elevations'));
	  elevButtons.push(HU.span(['id',this.domId('elevationslabel')],''));	    
	  }

	  if(elevButtons.length) {
	  html+= Utils.wrap(elevButtons,HU.open('span',['style',HU.css('margin-right','8px')]),'</span>');
	  }
	*/

	let domId = this.display.domId('glyphedit_popupText');
	let featureInfo = this.getFeatureInfoList();
	let lines = ['${default}'];
	lines = Utils.mergeLists(['default'],featureInfo.map(info=>{return info.id;}));

	let propsHelp =this.display.makeSideHelp(lines,domId,{prefix:'${',suffix:'}'});
	html+=HU.leftRightTable(HU.b('Popup Text:'),
				this.getHelp('#popuptext'));
	let help = 'Add macro:'+ HU.div(['class','imdv-side-help'],propsHelp);
	html+= HU.hbox([HU.textarea('',style.popupText??'',[ID,domId,'rows',4,'cols', 40]),HU.space(2),help]);

	html+=HU.b('Legend Text:') +'<br>' +
	    HU.textarea('',this.attrs.legendText??'',
			[ID,this.domId('legendtext'),'rows',4,'cols', 40]);
	
	if(this.isMultiEntry()) {
	    html+='<br>';
	    html+= HU.checkbox(this.domId('showmultidata'),[],this.getShowMultiData(),'Show entry data');
	}

	content.push({header:'Properties',contents:html});

	html=  this.getHelp('#miscproperties')+'<br>';
	let miscLines =[...IMDV_PROPERTY_HINTS];
	if(this.isMap()) {
	    miscLines.push('map.label.maxlength=100','map.label.maxlinelength=15',
			   'map.label.pixelsperline=10',
			   'map.label.pixelspercharacter=4',
			   'map.label.padding=2');
	}

	miscLines.push('<hr>');
	this.getFeatureInfoList().forEach(info=>{
	    //	    miscLines.push({line:info.id+'.show=true',title:info.property});
	    miscLines.push({info:info.id,title:info.getLabel()});	    
	});

	let miscHelp =this.display.makeSideHelp(miscLines,this.domId('miscproperties'),{suffix:'\n'});
	let ex = 'Add property:' + miscHelp;

	html += HU.hbox([HU.textarea('',this.attrs.properties??'',[ID,this.domId('miscproperties'),'rows',6,'cols', 40]),
			 HU.space(2),ex]);
	content.push({header:'Flags',contents:html});
    },
    addElevations:async function(update,done) {
	let pts;
	if(this.mapLayer) {
	    pts = [];
	    let features= this.mapLayer.features;
	    features.forEach((feature,idx)=>{
		let pt = feature.geometry.getCentroid();
		pts.push(this.display.getMap().transformProjPoint(pt))
	    });
	    console.dir(pts);
	} else {
	    pts = this.display.getLatLonPoints(this.getGeometry());
	}
	let callback = (points)=>{
	    this.attrs.elevations = points;
	    this.features[0].elevations = points;
	    done();
	};
	await this.getElevations(pts,callback,update);
    },


    applyPropertiesDialog: function() {

	if(this.isMultiEntry()) {
	    this.setShowMultiData(this.jq("showmultidata").is(':checked'));
	}
	//Make sure we do this after we set the above style properties
	this.setName(this.jq("mapglyphname").val());
	this.attrs.legendText = this.jq('legendtext').val();
	if(this.isEntry()) {
	    this.setUseEntryName(this.jq("useentryname").is(":checked"));
	    this.setUseEntryLabel(this.jq("useentrylabel").is(":checked"));
	    this.setUseEntryLocation(this.jq("useentrylocation").is(":checked"));
	    let glyphs = this.jq("entryglyphs").val();
	    this.setEntryGlyphs(glyphs);
	    this.applyEntryGlyphs();
	}
	

	this.setVisible(this.jq("visible").is(":checked"),true);
	this.parsedProperties = null;
	this.attrs.properties = this.jq('miscproperties').val();
	this.setVisibleLevelRange(this.display.jq("minlevel").val().trim(),
				  this.display.jq("maxlevel").val().trim());
	this.setShowMarkerWhenNotVisible(this.display.jq('showmarkerwhennotvisible').is(':checked'));

	if(this.isMapServer()  && this.getDatacubeVariable()) {
	    if(this.currentColorbar!=this.getDatacubeVariable().colorBarName) {
		this.getDatacubeVariable().colorBarName = this.currentColorbar;
		this.mapServerLayer.url = this.getMapServerUrl();
		this.mapServerLayer.redraw();
	    }
	}
	if(this.isRings()) {
	    this.attrs.radii=Utils.split(this.jq('radii').val()??'',',',true,true);
	    this.attrs.rangeRingLabels =this.jq('rangeringlabels').val();
	    this.attrs.rangeRingAngle=this.jq('rangeringangle').val();
	    this.attrs.rangeRingStyle = this.jq('rangeringstyle').val();
	    if(this.features.length>0) this.features[0].style.strokeColor='transparent';
	}
    },
    featureSelected:function(feature,layer,event) {
	//	console.log('imdv.featureSelected');
	if(this.selectedStyleGroup) {
	    let indices = this.selectedStyleGroup.indices;
	    //	    console.log('\thave a selectedStyleGroup');
	    if(indices.includes(feature.featureIndex)) {
		this.selectedStyleGroup.indices = Utils.removeItem(indices,feature.featureIndex);
		feature.style =  feature.originalStyle = null;
		//		console.log("removing selected:" + feature.featureIndex,indices);
	    } else {
		this.getStyleGroups().forEach((group,idx)=>{
		    group.indices = Utils.removeItem(group.indices,feature.featureIndex);
		});
		feature.style = feature.originalStyle = $.extend(feature.style??{},this.selectedStyleGroup.style);
		indices.push(feature.featureIndex);
		//		console.log("adding selected:" + feature.featureIndex,indices);
	    }
	    ImdvUtils.scheduleRedraw(layer,feature);
	    this.display.featureChanged(true);	    
	    return
	}
	//	console.log('\tcalling onFeatureSelect');
	this.display.getMap().onFeatureSelect(feature.layer,event)
    },
    featureUnselected:function(feature,layer,event) {
	//	this.display.getMap().onFeatureSelect(feature.layer,event)
    },
    glyphCreated:function() {
	this.applyEntryGlyphs();
    },
    applyEntryGlyphs:function(args) {
	if(!Utils.stringDefined(this.getEntryGlyphs(true))) {
	    return;
	}

	let opts = {
	    entryId:this.attrs.entryId
	};

	if(args) {
	    $.extend(opts,args);
	}

	let glyphs = [];
	this.getEntryGlyphs(true).trim().split("\n").forEach(line=>{
	    line = line.trim();
	    if(line.startsWith("#") || line == "") return;
	    glyphs.push(line);
	});
	if(glyphs.length==0) {
	    console.log("\tno glyphs-2");
	    return;
	}
	let url = Ramadda.getUrl("/entry/data?record.last=1&max=1&entryid=" + opts.entryId);
	let pointData = new PointData("",  null,null,url,
				      {entryId:opts.entryId});
	let callback = (data)=>{
	    this.makeGlyphs(pointData,data,glyphs);
	    this.display.clearFeatureChanged();
	}
	let fauxDisplay  = {
	    display:this.display,
	    type: "map glyph proxy",
	    getId() {
		return "ID";
	    },
	    pointDataLoaded:function(data,url) {
		callback(data);

	    },
            pointDataLoadFailed:function(err){
		this.display.pointDataLoadFailed(err);
	    },
	    applyRequestProperties:function(props) {
	    },
	    handleLog:function(err) {
		this.display.handleLog(err);
	    },
	    displayError:function(err) {
		console.log("Error:" + err);
	    }
	    
	}
	pointData.loadData(fauxDisplay,null);
    },
    makeGlyphs: function(pointData,data,glyphLines) {
	let glyphs = [];
	let lines=[];
	let canvasWidth=100;
	let canvasHeight=100;
	let widthRegexp = /width *= *([0-9]+)/;
	let heightRegexp = /height *= *([0-9]+)/;
	let fillRegexp = /fill *= *(.+)/;
	let borderRegexp = /border *= *(.+)/;
	let fontRegexp = /font *= *(.+)/;				
	let sizeRegexp = /size *= *(.+)/;
	let fontSizeRegexp = /fontSize *= *(.+)/;					
	let fill;
	let border;	
	let font;
	let size;
	let fontSize;		

	glyphLines.forEach(line=>{
	    line = line.trim();
	    let skip = true;
	    line.split(";").forEach(line2=>{
		line2 = line2.trim();
		let match;
		if(match  = line2.match(widthRegexp)) {
		    canvasWidth=parseFloat(match[1]);
		    return;
		}
		
		if(match  = line2.match(heightRegexp)) {
		    canvasHeight=parseFloat(match[1]);
		    return;
		}
		if(match  = line2.match(fillRegexp)) {
		    fill=match[1];
		    return;
		}
		if(match  = line2.match(sizeRegexp)) {
		    size=match[1];
		    return;
		}		
		if(match  = line2.match(fontSizeRegexp)) {
		    fontSize=match[1];
		    return;
		}		
		if(match  = line2.match(borderRegexp)) {
		    border=match[1];
		    return;
		}	    	    	    
		if(match  = line2.match(fontRegexp)) {
		    font = match[1];
		    return;
		}
		skip=false;
	    })
	    if(!skip)
		lines.push(line);
	});


	lines.forEach(line=>{
	    glyphs.push(new Glyph(this.display,1.0, data.getRecordFields(),data.getRecords(),{
		canvasWidth:canvasWidth,
		canvasHeight: canvasHeight,
		entryname: this.getName(),
		font:font
	    },line));
	});
	let cid = HU.getUniqueId("canvas_");
	let c = HU.tag("canvas",[CLASS,"", STYLE,"xdisplay:none;", 	
				 WIDTH,canvasWidth,HEIGHT,canvasHeight,ID,cid]);

	$(document.body).append(c);
	let canvas = document.getElementById(cid);
	let ctx = canvas.getContext("2d");
	if(fill) {
	    ctx.fillStyle=fill;
	    ctx.fillRect(0,0,canvasWidth,canvasHeight);
	}
	if(border) {
	    ctx.strokeStyle = border;
	    ctx.strokeRect(0,0,canvasWidth,canvasHeight);
	}
	ctx.strokeStyle ="#000";
	ctx.fillStyle="#000";
	let pending = [];
	let records = data.getRecords();
	glyphs.forEach(glyph=>{
	    //if its an image glyph then the image might not be loaded so the call returns a
	    //isReady function that we keep checking until it is ready
	    let isReady =  glyph.draw({}, canvas, ctx, 0,canvasHeight,{record:records[records.length-1]});
	    if(isReady) pending.push(isReady);
	});

	let finish = ()=>{
	    let img = canvas.toDataURL();
	    if($('#testimg').length) 
		$("#testimg").html(HU.tag("img",["src",img]));
	    canvas.remove();
	    if(fontSize) {
		this.style.fontSize=fontSize;
	    }
	    
	    if(size) {
		this.style.pointRadius=size;
	    }
	    this.style.externalGraphic=img;
	    this.applyStyle(this.style);		
	    this.display.redraw();
	};


	let check = () =>{
	    let allGood = true;
	    pending.every(p=>{
		if(!p()) {
		    allGood=false;
		    return false;
		}
		return true;
	    });
	    if(allGood) {
		finish();
	    }  else {
		setTimeout(check,100);
	    }
	};
	check();
    },

    setDownloadUrl:function(url) {
	this.downloadUrl =url;
    },
    setEntryGlyphs:function(v) {
	this.attrs.entryglyphs = v;
	return this;
    },
    getUseEntryLocation: function() {
	return this.attrs.useentrylocation;
    },
    setUseEntryLocation: function(v) {
	this.attrs.useentrylocation = v;
	return this;
    },
    getUseEntryName: function() {
	return this.attrs.useentryname;
    },
    setUseEntryName: function(v) {
	this.attrs.useentryname=v;
	return this;
    },
    getUseEntryLabel: function() {
	return this.attrs.useentrylabel;
    },    
    setUseEntryLabel: function(v) {
	this.attrs.useentrylabel =v;
	return this;
    },
    showMultiEntries:function() {
	let _this = this;
	if(!this.entries) return;
	let html = '';
	let map = {};
	this.entries.forEach(entry=>{
	    map[entry.getId()] = entry;
	    let link = entry.getLink(null,true,['target','_entry']);
	    link = HU.div(['style','white-space:nowrap;max-width:180px;overflow-x:hidden;','title',entry.getName()], link);
	    let add = '';
	    if(MAP_TYPES.includes(entry.getType().getId())) {
		add = HU.span(['class','ramadda-clickable','title','add map','entryid',entry.getId(),'command',GLYPH_MAP],HU.getIconImage('fas fa-plus'));
	    } else if(entry.isPoint) {
		add = HU.span(['class','ramadda-clickable','title','add data','entryid',entry.getId(),'command',GLYPH_DATA],HU.getIconImage('fas fa-plus'));
	    } else if(entry.isGroup) {
		add = HU.span(['class','ramadda-clickable','title','add multi entry','entryid',entry.getId(),'command',GLYPH_MULTIENTRY],HU.getIconImage('fas fa-plus'));
	    } else {
	    }		
	    if(add!='') {
		link = HU.leftRightTable(link,add);
	    }

	    html+=HU.div(['style',HU.css('white-space','nowrap')],link);
	});
	if(html!='') {
	    html = HU.div(['class','ramadda-cleanscroll', 'style','max-height:200px;overflow-y:auto;'], HU.div(['style','margin-right:10px;'],html));
	    this.jq('multientry').html(HU.b('Entries')+html);
	}
	this.jq('multientry').find('[command]').click(function(){
	    let command = $(this).attr('command');
	    let entry = map[$(this).attr('entryid')];
	    let glyphType = _this.display.getGlyphType(command);
	    let style = $.extend({},glyphType.getStyle());
	    let mapOptions = {
		type:command,
		entryType: entry.getType().getId(),
		entryId:entry.getId(),
		name:entry.getName(),
		icon:entry.getIconUrl()
	    }
	    if(command===GLYPH_MAP) {
		let mapGlyph = _this.display.handleNewFeature(null,style,mapOptions);
		mapGlyph.checkMapLayer();
	    } else if(command==GLYPH_DATA) {
		_this.display.createData(mapOptions);
	    } else if(command==GLYPH_MULTIENTRY) {
		let mapGlyph = _this.display.handleNewFeature(null,style,mapOptions);
		mapGlyph.addEntries(true);
	    }
	});
    },
    getMap: function() {
	return this.display.getMap();
    },
    changeOrder:function(toFront) {
	if(this.mapServerLayer) {
	    if(toFront)
		this.display.getMap().toFrontLayer(this.mapServerLayer);
	    else
		this.display.getMap().toBackLayer(this.mapServerLayer);		    
	    return;		
	}
	if(this.getImage()) {
	    if(toFront)
		this.display.getMap().toFrontLayer(this.getImage());
	    else
		this.display.getMap().toBackLayer(this.getImage());		    
	    return;		
	}
	if(this.features.length) {
	    if(toFront)
		Utils.toFront(this.display.getLayer().features, this.features,true);
	    else
		Utils.toBack(this.display.getLayer().features, this.features,true);
	}
    },	
    getId:function() {
	return this.id;
    },
    getFilterable: function() {
	return false;
	return this.attrs.filterable??true;
    },
    getAllFeatures: function() {
	let features=[];
	if(this.features) features.push(...this.features);
	if(this.mapLayer) features.push(...this.mapLayer.features);
	return features;
    },
    getFeatures: function() {
	return this.features;
    },
    checkLineType:function(points) {
	if(this.attrs.lineType==LINETYPE_GREATCIRCLE || this.attrs.lineType==LINETYPE_CURVE) {
	    if(!MapUtils.loadTurf(()=>{
		this.checkLineType(points);
	    })) {
		return;
	    }
	}

	if(!this.attrs.originalPoints) {
	    this.attrs.originalPoints = points??this.getPoints({});
	}
	let pts = points??this.attrs.originalPoints;
	if(!pts || pts.length==0) return;
	let newPts = [];
	let getPoints = f=>{
	    let p=[];
	    let coords = f.geometry.coordinates;
	    coords.forEach(pair=>{
		p.push(pair[1],pair[0]);
	    });
	    return p;
	};
	let latlon=(pts,i) =>{
	    return [pts[i+0], pts[i+1]];
	}
	isDraggable = true;
	if(this.attrs.lineType==LINETYPE_STEPPED) {
	    isDraggable = false;
	    let mid = (v1,v2)=>{
		return v1+(v2-v1)/2;
	    };
	    for(let i=0;i<pts.length-2;i+=2) {
		let [lat1,lon1] = latlon(pts,i);
		let [lat2,lon2] = latlon(pts,i+2);				
		newPts.push(lat1,lon1);
		newPts.push(lat1,mid(lon1,lon2));
		newPts.push(lat2,mid(lon1,lon2));		
		newPts.push(lat2,lon2);
	    }
	} else if(this.attrs.lineType==LINETYPE_GREATCIRCLE) {
	    isDraggable = false;
	    let options = {units: 'miles'};
	    for(let i=0;i<pts.length-2;i+=2) {
		let [lat1,lon1] = latlon(pts,i);
		let [lat2,lon2] = latlon(pts,i+2);				
		let start = turf.point([lon1,lat1]);
		let end = turf.point([lon2,lat2]);
		let circlePts =this.display.getTurfPoints(turf.greatCircle(start, end)); 
		circlePts.forEach(pair=>{
		    if(Array.isArray(pair)) {
			newPts.push(pair[1],pair[0]);
		    } else {
			newPts.push(pair);
		    }
		});
	    }

	} else if(this.attrs.lineType==LINETYPE_CURVE) {
	    isDraggable = false;
	    let tmp = [];
	    for(let i=0;i<pts.length;i+=2) {
		tmp.push([pts[i+1],pts[i]]);
	    }
	    var line = turf.lineString(tmp);
	    newPts = getPoints(turf.bezierSpline(line));
	} else  {
	    newPts = pts;
	    this.attrs.originalPoints=null;
	}
	this.addLine(newPts, isDraggable);
    },

    addLine: function(pts,isDraggable) {
	let features=[];
	let stride = 2;
	if(pts.length>20)
	    stride=parseInt(pts.length/10);
	let type = 'OpenLayers.Geometry.LineString';
	let feature = this.display.makeFeature(this.getMap(),type,this.style,pts);
	feature.isDraggable =isDraggable;
	features.push(feature);
	if(this.attrs.addDots) {
	    let dotStyle  = {
		strokeWidth:Utils.isDefined(this.style.dotStrokeWidth)?
		    this.style.dotStrokeWidth:1,
		pointRadius:this.style.dotSize??3,
		strokeColor: this.style.dotStrokeColor||this.style.strokeColor,
		fillColor: this.style.dotFillColor||this.style.strokeColor,		
	    }
	    if(Utils.stringDefined(this.style.dotExternalGraphic)) {
		dotStyle.externalGraphic = this.style.dotExternalGraphic;
	    }
	    let addPoint= (lat,lon)=>{
		let feature = this.display.makeFeature(this.getMap(),'OpenLayers.Geometry.Point',dotStyle,
						       [lat,lon]);
		features.push(feature);
		feature.fixedStyle = true;
		feature.style=dotStyle;
		feature.isDraggable = false;
	    }
	    addPoint(pts[0],pts[1]);
	    for(let i=2;i<pts.length-2;i+=stride) {
		addPoint(pts[i],pts[i+1]);
	    }
	    addPoint(pts[pts.length-2],pts[pts.length-1]);
	}
	this.addFeatures(features,true,true);
    },	

    clearFeatures: function() {
	this.display.removeFeatures(this.features);
	this.features = [];
    },
    addFeatures: function(features,andClear,addToDisplay) {
	if(andClear) this.clearFeatures();
	this.features.push(...features);
	features.forEach(feature=>{
	    feature.mapGlyph = this;   
	});

	if(addToDisplay) {
	    this.display.addFeatures(features);
	}
    },

    addFeature: function(feature,andClear,addToDisplay) {
	this.addFeatures([feature],andClear,addToDisplay);
    },
    handleKeyDown:function(event) {
	if(event.key=='o' || event.key=='O') {
	    if(this.isImage() && this.image) {
		let delta = event.key=='o'?-0.05:0.05;
		let op =parseFloat(Utils.isDefined(this.style.imageOpacity)?this.style.imageOpacity:1);
		op = Math.max(Math.min(op+delta,1),0);
		this.style.imageOpacity = op;
		this.image.setOpacity(op);
	    }
	    return;
	}
	if(event.key=='v') {
	    this.setVisible(!this.getVisible(),true);
	    return
	}
	if(event.key=='r' || event.key=='l' || event.key=='R' || event.key=='L') {
	    if(this.isImage() && this.image) {
		let delta = 0.5;
		if(event.key=='l') delta=-0.5;
		else if(event.key=='R') delta=5;
		else if(event.key=='L') delta=-5;
		this.setRotation(this.style.rotation +delta);
		this.unselect();
		this.select();
	    }
	}
    },
    getStyle: function(applyMacros) {
	if(applyMacros) {
	    let tmpStyle = this.style??{};
	    if(Utils.stringDefined(tmpStyle.labelYOffset) || Utils.stringDefined(tmpStyle.labelXOffset)) {
		tmpStyle=Utils.clone(tmpStyle);
		let a = v=>{
		    v= String(v??'');
		    let r = tmpStyle.pointRadius;
		    if(isNaN(r)) r=0;
		    v = v.replace(/\${size}/g,r).replace(/\${size2}/g,r/2);
		    try {
			v = eval(v);
		    } catch(err) {
			console.log('error applying macro:' +err);
		    }
		    return v;
		}
		tmpStyle.labelXOffset = a(tmpStyle.labelXOffset);
		tmpStyle.labelYOffset = a(tmpStyle.labelYOffset);
	    }	
	    return tmpStyle;
	}
	return this.style;
    },
    panMapTo: function(andZoomIn) {
	let bounds = this.getBounds();
	if(bounds) {
	    this.display.getMap().zoomToExtent(bounds);
	}
	if(andZoomIn) {
	    this.display.getMap().setZoom(16);
	}
    },
    getBounds: function() {
	let bounds = null;
	if(this.isMultiEntry() && this.entries) {
	    this.entries.forEach(entry=>{
		let b = null;
		if(entry.hasBounds()) {
		    b =   MapUtils.createBounds(entry.getWest(),entry.getSouth(),entry.getEast(), entry.getNorth());
		} else if(entry.hasLocation()) {
		    b =   MapUtils.createBounds(entry.getLongitude(),entry.getLatitude(),
						entry.getLongitude(),entry.getLatitude());
		} 
		if(b) {
		    bounds = MapUtils.extendBounds(bounds,b);
		}
	    });
	    if(bounds) {
		return this.display.getMap().transformLLBounds(bounds);
	    }
	    return null;
	}

	if(this.children) {
	    this.children.forEach(child=>{
		bounds =  MapUtils.extendBounds(bounds,child.getBounds());
	    });
	} else 	if(this.features && this.features.length) {
	    bounds = this.display.getMap().getFeaturesBounds(this.features);
	    if(this.rings) {
		bounds = MapUtils.extendBounds(bounds,
					       this.display.getMap().getFeaturesBounds(this.rings));
	    }
	} if(this.isMapServer()) {
	    if(this.getDatacubeVariable() && Utils.isDefined(this.getDatacubeAttr('geospatial_lat_min'))) {
		let attrs = this.getDatacubeAttrs();
		bounds= MapUtils.createBounds(attrs.geospatial_lon_min, attrs.geospatial_lat_min, attrs.geospatial_lon_max, attrs.geospatial_lat_max);
		bounds= this.display.getMap().transformLLBounds(bounds);
	    }
	} else if(this.getMapLayer()) {
	    if(this.getMapLayer().getVisibility()) {
		bounds =  this.display.getMap().getFeaturesBounds(this.getMapLayer().features);
		if(!bounds) {
		    if(this.getMapLayer().maxExtent) {
			let e = this.getMapLayer().maxExtent;
			bounds = MapUtils.createBounds(e.left,e.bottom,e.right,e.top);
		    }
		}
	    }
	    if(this.imageLayers) {
		this.imageLayers.forEach(obj=>{
		    if(!obj.layer || !obj.layer.getVisibility()) return;
		    bounds = MapUtils.extendBounds(bounds,
						   this.getMap().getLayerVisbileExtent(obj.layer)||obj.layer.extent);
		});
	    }
	} else	if(this.displayInfo?.display) {
	    if(this.displayInfo.display.myFeatureLayer && (
		!Utils.isDefined(this.displayInfo.display.layerVisible) ||
		    this.displayInfo.display.layerVisible)) {
		bounds =  this.display.getMap().getFeaturesBounds(this.displayInfo.display.myFeatureLayer.features);
	    } else  if(this.displayInfo.display.pointBounds) {
		bounds= this.display.getMap().transformLLBounds(this.displayInfo.display.pointBounds);
	    }
	}
	return bounds;
    },


    collectFeatures: function(features) {
	if(this.children) {
	    this.children.forEach(child=>{
		child.collectFeatures(features);
	    });
	} else 	if(this.features.length) {
	    features.push(...this.features);
	} else if(this.getMapLayer()) {
	    let f = this.getMapLayer().features;
	    if(f)
		features.push(...f);
	}
    },
    getGeometry: function() {
	if(this.features.length>0) return this.features[0].geometry;
	else return null;
    },
    setName: function(name) {
	this.attrs.name = name;
    },
    getName: function() {
	return this.attrs.name||this.name;
    },
    setName: function(name) {
	this.attrs.name = name;
    },    
    getFeature: function() {
	if(this.features.length>0) return this.features[0];
	return null;
    },
    getFeatures: function() {
	return this.features;
    },    
    getType: function() {
	return this.type;
    },
    getWikiText:function() {
	return this.style.wikiText || this.getPopupText();
    },

    getPopupText: function() {
	return this.style.popupText;
    },
    getEntryId: function() {
	return this.attrs.entryId;
    },
    hasBounds:function() {
	if(this.isMapServer()) {
	    if(this.getDatacubeVariable() && Utils.isDefined(this.getDatacubeAttr('geospatial_lat_min'))) {
		return true;
	    }
	    return false;
	}
	return  !this.isFixed();
    },
    getLabel:function(forLegend,addDecorator) {
	let name = this.getName();
	let label;
	let theLabel;
	if(Utils.stringDefined(name)) {
	    if(!forLegend)
		theLabel= this.getType()+': '+name;
	    else
		theLabel = name;
	} else if(this.isFixed()) {
	    theLabel = this.style.text;
	    if(theLabel && theLabel.length>15) theLabel = theLabel.substring(0,14)+'...'
	} else {
	    theLabel =  this.getType();
	}
	label = theLabel;
	let url = null;
	let glyphType = this.display.getGlyphType(this.getType());
	let right = '';
	if(addDecorator) {
	    //For now don't add the decoration (the graphic indicator)
	    //right+=this.getDecoration(true);
	}

	if(glyphType) {
	    let icon = Utils.getStringDefined([this.style.externalGraphic,this.attrs.icon,glyphType.getIcon()]);
	    if(icon.startsWith('data:')) icon = this.attrs.icon;
	    if(icon && icon.endsWith('blank.gif')) icon = glyphType.getIcon();
	    icon = HU.image(icon,['width','18px']);
	    if(url && forLegend)
		icon = HU.href(url,icon,['target','_entry']);
	    let showZoomTo = forLegend && this.hasBounds();
	    if(showZoomTo) {
		right+=SPACE+
		    HU.span([CLASS,'ramadda-clickable imdv-legend-item-view',
			     'glyphid',this.getId(),
			     TITLE,'Click:Move to; Shift-click:Zoom in',],
			    HU.getIconImage('fas fa-magnifying-glass',[],LEGEND_IMAGE_ATTRS));
	    }
	    label = HU.span(['style','margin-right:5px;'], icon)  + label;
	}

	if(forLegend) {
	    label = HU.div(['title',theLabel+'<br>Click to toggle visibility<br>Shift-click to select','style',HU.css('xmax-width','150px','overflow-x','hidden','white-space','nowrap')], label);	    
	}
	if(right!='') {
	    right= HU.span(['style',HU.css('white-space','nowrap')], right);
	    //	    label=HU.leftRightTable(label,right);
	}
	if(forLegend) {
	    let clazz = 'imdv-legend-label';
	    label = HU.div(['class','ramadda-clickable ' + clazz,'glyphid',this.getId()],label);
	    return [label,right];
	}
	return label;
    },

    removeChildGlyph: function(child) {
	if(this.children) this.children = Utils.removeItem(this.children, child);
    },

    loadJson:function(jsonObject) {
	if(jsonObject.children) {
	    this.children = [];
	    jsonObject.children.forEach(childJson=>{
		let child = this.display.makeGlyphFromJson(childJson);
		if(child) {
		    this.addChildGlyph(child);
		}
	    });
	}
    },
    getChildren: function(child) {
	if(!this.children) this.children = [];
	return this.children;
    },
    findGlyph:function(id) {
	if(id == this.getId()) return this;
	return ImdvUtils.findGlyph(this.children,id);
    },
    addChildGlyph: function(child) {
	this.getChildren().push(child);
	child.setParentGlyph(this);
    },
    getParentGlyph: function() {
	return this.parentGlyph;
    },
    setParentGlyph: function(parent) {
	if(this.parentGlyph) this.parentGlyph.removeChildGlyph(this);
	this.parentGlyph = parent;
	if(parent) this.display.removeMapGlyph(this);
    },    
    getLegendVisible:function() {
	return this.attrs.legendVisible;
    },
    setLegendVisible:function(visible) {
	this.attrs.legendVisible = visible;
    },
    getFiltersVisible:function() {
	return this.attrs.filtersVisible;
    },
    setFiltersVisible:function(visible) {
	this.attrs.filtersVisible = visible;
    },    
    convertText:function(text) {
	text =text.replace(/\n *\*/g,'\n &bull;');
	text =text.replace(/^ *\*/g,'&bull;');
	text = text.replace(/\"/g,"\\").replace(/\n/g,'<br>');
	return text;
    },
    

    getLegendDiv:function() {
	return this.jq('legend_');
    },
    setLayerLevel:function(level) {
	if(this.getMapLayer()) {
	    this.getMapLayer().ramaddaLayerIndex=level++;
	}
	if(this.imageLayers) {
	    this.imageLayers.forEach(obj=>{
		if(obj.layer) {
		    obj.layer.ramaddaLayerIndex=level++;
		}
	    });
	}
	if(this.mapServerLayer) {
	    this.mapServerLayer.ramaddaLayerIndex=level++;
	}

	if(this.image) {
	    this.image.ramaddaLayerIndex=level++;
	}

	if(this.isGroup()) {
	    this.getChildren().forEach(mapGlyph=>{
		level = mapGlyph.setLayerLevel(level);
	    });
	}
	return level;
    },
    makeLegend:function(opts) {
	opts = opts??{};
	let html = '';
	if(!this.display.getShowLegendShapes() && this.isShape()) {
	    return "";
	}
	let label =  this.getLabel(true,true);
	let body = HU.div(['class','imdv-legend-inner'],this.getLegendBody());

	if(this.imageLayers) {
	    let cbx='';
	    if(this.getMapLayer() && this.getMapLayer().features.length) {
		cbx+=HU.div([],HU.checkbox(Utils.getUniqueId(''),['imageid','main','class','imdv-imagelayer-checkbox'],
					   this.isImageLayerVisible({id:'main'}),
					   'Main Layer'));
	    }
	    this.imageLayerMap = {};
	    this.imageLayers.forEach(obj=>{
		this.imageLayerMap[obj.id] = obj;
		cbx+=HU.div([],HU.checkbox(Utils.getUniqueId(''),['imageid',obj.id,'class','imdv-imagelayer-checkbox'],
					   this.isImageLayerVisible(obj),
					   obj.name));
	    });
	    body+=HU.div(['class','imdv-legend-offset'],cbx);
	}
	if(this.isGroup()) {
	    let child="";
	    this.getChildren().forEach(mapGlyph=>{
		let childHtml = mapGlyph.makeLegend(opts);
		if(childHtml) child+=childHtml;
	    });
	    body+=HU.div(['class','imdv-legend-offset'],child);
	}

	let block = HU.toggleBlockNew("",body,this.getLegendVisible(),
				      {separate:true,headerStyle:'display:inline-block;',
				       extraAttributes:['map-glyph-id',this.getId()]});		
	if(opts.idToGlyph)
	    opts.idToGlyph[this.getId()] = this;
	let clazz = "";
	if(!this.getVisible()) clazz+=' imdv-legend-label-invisible ';
	html+=HU.open('div',['id',this.domId('legend_'),'glyphid',this.getId(),'class','imdv-legend-item '+clazz]);
	html+=HU.div(['style','display: flex;'],HU.div(['style','margin-right:4px;'],block.header)+
		     HU.div(['style','width:80%;'], label[0])+
		     HU.div([],label[1]));

	html+=HU.div(['class','imdv-legend-body'],block.body);
	html+=HU.close('div');
	return html;
    },

    getLegendStyle:function(style) {
	style = $.extend($.extend({},this.style),style??{});
	let s = '';
	let lineColor;
	let lineStyle;
	let lineWidth;
	if(Utils.stringDefined(style.fillPattern)) {
	    let svg = window.olGetSvgPattern(style.fillPattern,
					     style.strokeColor,style.fillColor);
	    s+='background-image: url(\''+ svg.url+'\');background-repeat: repeat;';
	}  else if(style.fillColor) {
	    s+=HU.css('background',style.fillColor);
	} 
	if(Utils.stringDefined(style.strokeColor)) 
	    lineColor = style.strokeColor;
	if(Utils.stringDefined(style.strokeWidth)) 
	    lineWidth = style.strokeWidth;
	if(Utils.stringDefined(style.strokeDashstyle)) {
	    if(['dot','dashdot'].includes(style.strokeDashstyle)) {
		lineStyle = "dotted";
	    } else  if(style.strokeDashstyle.indexOf("dash")>=0) {
		lineStyle = "dashed";
	    }
	}
	if(lineColor || lineColor||lineWidth) {
	    s+=HU.css('border',HU.getDimension(lineWidth??'1px')+ ' ' + (lineStyle??'solid') + ' ' +
		      (lineColor??'black'));
	}
	return s;
    },
    getLegendBody:function() {
	let showInMapLegend=this.getProperty('showLegendInMap',false) && !this.display.getShowLegendInMap();
	let inMapLegend='';
	let body = '';
	let buttons = this.display.makeGlyphButtons(this,true);
	if(this.isMap() && this.getProperty('showFeaturesTable',true))  {
	    this.showFeatureTableId = HU.getUniqueId('btn');
	    if(buttons!=null) buttons = HU.space(1)+buttons;
	    buttons =  HU.span(['id',this.showFeatureTableId,'title','Show features table','class','ramadda-clickable'],
			       HU.getIconImage('fas fa-table',[],BUTTON_IMAGE_ATTRS)) +buttons;
	}

	/** For now don't add this as we can also get it through the entry link below
	    if(this.downloadUrl) {
	    if(buttons!=null) buttons = HU.space(1)+buttons;
	    buttons = HU.href(this.downloadUrl,HU.getIconImage('fas fa-download',[],BUTTON_IMAGE_ATTRS),['target','_download','title','Download','class','ramadda-clickable']) +buttons;
	    }
	*/

	if(this.attrs.entryId) {
	    if(buttons!=null) buttons = HU.space(1)+buttons;
	    url = RamaddaUtils.getEntryUrl(this.attrs.entryId);
	    buttons = HU.href(url,HU.getIconImage('fas fa-home',[],BUTTON_IMAGE_ATTRS),['target','_entry','title','View entry','class','ramadda-clickable']) +buttons;
	}	

	
	if(!this.display.canEdit() && !this.getProperty('showButtons',true))
	    buttons = '';

	if(buttons!='')
	    body+=HU.div(['class','imdv-legend-offset'],buttons);
	//	    body+=HU.center(buttons);
	if(Utils.stringDefined(this.attrs.legendText)) {
	    let text = this.attrs.legendText.replace(/\n/g,'<br>');
	    body += HU.div(['class','imdv-legend-offset imdv-legend-text'],text);
	}


	if(this.isMap()) {
	    if(!this.mapLoaded) {
		if(this.isVisible()) 
		    body += HU.div(['class','imdv-legend-inner'],'Loading...');
		return body;
	    }

	    let boxStyle = 'display:inline-block;width:14px;height:14px;margin-right:4px;';
	    let legend = '';
	    let styleLegend='';
	    this.getStyleGroups().forEach((group,idx)=>{
		styleLegend+=HU.openTag('table',['width','100%']);
		styleLegend+= HU.openTag('tr',['title',this.display.canEdit()?'Select style':'','class',CLASS_IMDV_STYLEGROUP +(this.display.canEdit()?' ramadda-clickable':''),'index',idx]);
		let style = boxStyle + this.getLegendStyle(group.style);
		styleLegend+=HU.tag('td',['width','18px'],
				    HU.div(['style', style]));
		styleLegend +=HU.tag('td',[], group.label);
		styleLegend+='</tr>';
		styleLegend+='</table>'
	    });


	    if(styleLegend!='') {
		styleLegend=HU.div(['id','glyphstyle_' + this.getId()], styleLegend);
		if(showInMapLegend)
		    inMapLegend+=styleLegend;
		else
		    legend+=styleLegend;
	    }

	    if(this.attrs.mapStyleRules) {
		let rulesLegend = '';
		let lastProperty='';
		this.attrs.mapStyleRules.forEach(rule=>{
		    if(rule.type=='use') return;
		    if(!Utils.stringDefined(rule.property)) return;
		    let propOp = rule.property+rule.type;
		    if(lastProperty!=propOp) {
			if(rulesLegend!='') rulesLegend+='</table>';
			let type = rule.type;
			if(type=='==') type='=';
			type = type.replace(/</g,'&lt;').replace(/>/g,'&gt;');
			rulesLegend+= HU.b(this.makeLabel(rule.property,true))+' ' +type+'<br><table width=100%>';
		    }
		    lastProperty  = propOp;
		    let label = rule.value;
		    label   = HU.span(['style','font-size:9pt;'],label);
		    let item = '<tr><td width=16px>';
		    let lineWidth;
		    let lineStyle;
		    let lineColor;
		    let svg;
		    let havePattern = rule.style.indexOf('fillPattern')>=0;
		    let fillColor,strokeColor;
		    let styleObj = {};
		    rule.style.split('\n').forEach(line=>{
			line  = line.trim();
			if(line=='') return;
			let toks = line.split(':');
			styleObj[toks[0]] = toks[1];
		    });

		    let style = boxStyle +this.getLegendStyle(styleObj);
		    let div=HU.div(['class','circles-1','style',style],'');
		    item+=div+'</td>';
		    item += '</td><td>'+ label+'</td></tr>';
		    rulesLegend+=HU.div([],item);
		});
		if(rulesLegend!='') {
		    rulesLegend+= '</table>';
		    legend+=rulesLegend;
		}
	    }
	    if(legend!='') {
		if(showInMapLegend)
		    inMapLegend+=legend;
		else
		    body+=HU.toggleBlock('Legend',legend,true);
	    }
	}


	let showAnimation = false;
	if(this.isMapServer() && this.getDatacubeVariable() && this.getDatacubeVariable().dims && this.getDatacubeVariable().shape && this.getDatacubeAttr('time_coverage_start')) {
	    let v = this.getDatacubeVariable();
	    body+='Time: ' + HU.span(['id',this.domId('time_current')],this.getCurrentTimeStep()??'')+'<br>';
	    let idx=v.dims.indexOf('time');
	    let numTimes = v.shape[idx];
	    let start =new Date(v.attrs.time_coverage_start);
	    let end =new Date(v.attrs.time_coverage_end);
	    let value = end.getTime();
	    if(this.attrs.currentTimeStep) {
		value = new Date(this.attrs.currentTimeStep).getTime();
	    }

	    let slider = 
		HU.div(['title','Set time','slider-min',start.getTime(),'slider-max',end.getTime(),'slider-value',value,
			ID,this.domId('time_slider'),'class','ramadda-slider',STYLE,HU.css('display','inline-block','width','90%')],'');

	    let anim = HU.join([
		['Settings','fa-cog','settings'],
		['Go to start','fa-backward','start'],
		['Step backward','fa-step-backward','stepbackward'],
		['Play','fa-play','play'],
		['Step forward','fa-step-forward','stepforward'],
		['Go to end','fa-forward','end']
	    ].map(t=>{
		return HU.span(['class','imdv-time-anim ramadda-clickable','title',t[0],'action',t[2]],HU.getIconImage(t[1]));
	    }),HU.space(2));

	    if(this.getProperty('showAnimation',true)) {
		showAnimation  = true;
		body+=HU.center(anim);
		body+=slider;
		let fstart = this.formatDate(start);
		let fend = this.formatDate(end);
		body+=HU.leftRightTable(HU.span(['id',this.domId('time_min')],fstart),
					HU.span(['id',this.domId('time_max')],fend));
	    }
	}



	if(this.isMapServer() || Utils.stringDefined(this.style.imageUrl) || this.imageLayers || this.image) {
	    let v = (this.imageLayers||this.isImage())?this.style.imageOpacity:this.style.opacity;
	    if(!Utils.isDefined(v)) v = 1;
	    if(showAnimation)
		body+='Opacity:';
	    body += 
		HU.center(
		    HU.div(['title','Set image opacity','slider-min',0,'slider-max',1,'slider-value',v,
			    ID,this.domId('image_opacity_slider'),'class','ramadda-slider',STYLE,HU.css('display','inline-block','width','90%')],''));
	}

	if(this.display.canEdit() && (this.image || Utils.stringDefined(this.style.imageUrl))) {
	    body+='Rotation:';
	    body += HU.center(
		HU.div(['title','Set image rotation','slider-min',-360,'slider-max',360,'slider-value',this.style.rotation??0,
			ID,this.domId('image_rotation_slider'),'class','ramadda-slider',STYLE,HU.css('display','inline-block','width','90%')],''));
	}

	let item  = (content,checkInMap,addDecoration) => {
	    if(checkInMap && showInMapLegend) {
		if(addDecoration && Utils.stringDefined(content)) {
		    content = HU.hbox([this.getDecoration(true),content]);
		}
		inMapLegend+=HU.div(['style','max-width:200px'],content);
	    } else {
		if(Utils.stringDefined(content)) {	
		    body+=HU.div(['class','imdv-legend-body-item'], content);
		}
	    }
	};


	let colorTableLegend ='';
	body+=HU.div(['id',this.domId('legendcolortableprops')]);
	colorTableLegend+=HU.div(['id',this.domId('legendcolortable_fill')]);
	colorTableLegend+=HU.div(['id',this.domId('legendcolortable_stroke')]);	
	colorTableLegend+=HU.div(['id',this.domId(ID_MAPLEGEND)]);
	if(showInMapLegend)
	    inMapLegend+=colorTableLegend;
	else
	    body+=colorTableLegend;

	//Put the placeholder here for map filters
	body+=HU.div(['id',this.domId(ID_MAPFILTERS)]);

	if(this.type==GLYPH_LABEL && this.style.label) {
	    item(this.style.label.replace(/\"/g,"\\"));
	}
	let distances = this.display.getDistances(this.getGeometry(),this.getType());
	item(distances,true,true);
	if(this.isMultiEntry()) {
	    item(HU.div(['id',this.domId('multientry')]));
	}

	if(this.isShape()) {
	    item('',true,true);
	}
	if(Utils.stringDefined(this.style.imageUrl)) {
	    item(HU.center(HU.href(this.style.imageUrl,HU.image(this.style.imageUrl,['style',HU.css('margin-bottom','4px','border','1px solid #ccc','width','150px')]),['target','_image'])));
	}
	if(Utils.stringDefined(this.style.legendUrl)) {
	    item(HU.center(HU.href(this.style.legendUrl,HU.image(this.style.legendUrl,['style',HU.css('margin-bottom','4px','border','1px solid #ccc','width','150px')]),['target','_image'])));
	}

	this.jq('maplegend').remove();
	if(inMapLegend!='') {
	    inMapLegend=
		HU.div(['title',this.getName(),'style','white-space:nowrap;max-width:150px;overflow-x:hidden'],HU.b(this.getName())) +
		inMapLegend;

	    inMapLegend = HU.div(['style','border-bottom:var(--basic-border);padding:4px;','id',this.domId('maplegend')], inMapLegend);
	    this.display.addToMapLegend(this,inMapLegend);
	}
	



	return body;
    },
    initLegend:function() {
	let _this = this;

	if(this.imageLayers) {
	    this.getLegendDiv().find('.imdv-imagelayer-checkbox').change(function() {
		let visible = $(this).is(':checked');
		let id = $(this).attr('imageid');
		let obj = _this.imageLayerMap[id]??{id:id};
		_this.setImageLayerVisible(obj,visible);
	    });
	}


	if(this.display.canEdit()) {
	    let label = this.getLegendDiv().find('.imdv-legend-label');
	    //Set the last dropped time so we don't also handle this as a setVisibility click
	    let notify = ()=>{_this.display.setLastDroppedTime(new Date());};
	    this.getLegendDiv().draggable({
		start: notify,
		drag: notify,
		stop: notify,
		containment:this.display.domId(ID_LEGEND),
		revert: true
	    });
	    this.display.makeLegendDroppable(this,label,notify);
	    //Only drop on the legend label

	    let items = this.jq(ID_LEGEND).find('.imdv-legend-label');
	    let rows = jqid('glyphstyle_'+this.getId()).find('.' + CLASS_IMDV_STYLEGROUP);

	    rows.click(function() {
		if($(this).hasClass(CLASS_IMDV_STYLEGROUP_SELECTED)) {
		    $(this).removeClass(CLASS_IMDV_STYLEGROUP_SELECTED);
		    _this.selectedStyleGroup = null;
		    return;
		}
		rows.removeClass(CLASS_IMDV_STYLEGROUP_SELECTED);
		$(this).addClass(CLASS_IMDV_STYLEGROUP_SELECTED);
		_this.selectedStyleGroup = _this.getStyleGroups()[$(this).attr('index')];

	    });
	}


	if(this.isMultiEntry() && this.entries) {
	    this.showMultiEntries();
	}
	if(this.showFeatureTableId) {
	    $('#'+ this.showFeatureTableId).click(function() {
		_this.showFeaturesTable($(this));
	    });
	}

	let setRotation = (event,ui) =>{
	    this.setRotation(ui.value);
	}
	let setOpacity = (event,ui) =>{
	    if(this.isMapServer())
		this.style.opacity = ui.value;
	    else if(this.image || this.imageLayers) {
		this.style.imageOpacity = ui.value;
		if(this.image) {
		    this.image.setOpacity(this.style.imageOpacity);
		}
		if(this.imageLayers)
		    this.imageLayers.forEach(obj=>{
			if(obj.layer)
			    obj.layer.setOpacity(this.style.imageOpacity);
		    });
	    }
	    this.applyStyle(this.style);
	}

	let getSliderTime=(value)=>{
	    return new Date(parseFloat(value));
	}
	let getStep = ()=>{
	    let min =  +this.jq('time_slider').attr('slider-min');
	    let max= +this.jq('time_slider').attr('slider-max');
	    let temp = this.getDatacubeAttr('temporal_resolution');
	    let msPerDay = 1000*60*60*24;
	    if(temp) {
		let match = temp.match(/(\d+)D/);
		if(match) {
		    return +match[1]*msPerDay;
		}
	    }
	    return msPerDay;
	}	    
	let timeSliderStop=v=>{
	    let current = getSliderTime(v);
	    let s = current.format('isoDate');
	    this.jq('time_current').html(s);
	    this.attrs.currentTimeStep = current.getTime();
	    if(this.mapServerLayer) {
		this.mapServerLayer.url = this.getMapServerUrl();
		this.getMapServerLayer().setVisibility(false);
		this.getMapServerLayer().setVisibility(true);
		ImdvUtils.scheduleRedraw(this.getMapServerLayer());
	    }
	}


	this.getLegendDiv().find('.imdv-time-anim').click(function() {
	    let action = $(this).attr('action');
	    let slider=	_this.jq('time_slider');
	    let current = +slider.slider('value');
	    let min = +slider.attr('slider-min');
	    let max = +slider.attr('slider-max');	    
	    let step = +slider.slider('option','step');
	    let change = (v)=>{
		v = Math.min(max,Math.max(min,v));
		slider.slider('value',v);
		timeSliderStop(v);
	    }

	    switch(action) {
	    case 'play':
		if(_this.timeAnimationTimeout)
		    clearTimeout(_this.timeAnimationTimeout);
		_this.timeAnimationTimeout=null;
		if(_this.timeAnimationRunning) {
		    $(this).html(HU.getIconImage('fas fa-play'));
		} else {
		    $(this).html(HU.getIconImage('fas fa-stop'));
		    let stepTime = () =>{
			let current = +slider.slider('value');
			current = current+(_this.attrs.timeAnimationStep??1)*step;
			change(current);
			//			console.log("current time: " +new Date(current) +' step:' + _this.attrs.timeAnimationStep);
			if(current>=max) {
			    $(this).html(HU.getIconImage('fas fa-play'));
			    _this.timeAnimationRunning = false;
			    return
			}
			_this.timeAnimationTimeout=setTimeout(stepTime,_this.attrs.timeAnimationPause??4000);
		    }
		    stepTime();
		}
		_this.timeAnimationRunning = !_this.timeAnimationRunning;
		break;
	    case 'start': 
		change(min);
		break;
	    case 'end': 
		change(max);
		break;		
	    case 'stepforward': 
		change(current+step);
		break;
	    case 'stepbackward': 
		change(current-step);
		break;		
	    case 'settings':
		let html = HU.formTable();

		html+=HU.formEntry('Time Pause:', HU.input("",_this.attrs.timeAnimationPause??4000,
							   ['id',_this.domId('timeanimation_pause'),'size','4']) +' (ms)');
		html+=HU.formEntry('Time Step:', HU.input("",_this.attrs.timeAnimationStep??1,
							  ['id',_this.domId('timeanimation_step'),'size','4']) +' Time steps to skip');		
		html+='</table>';

		let buttons = HU.buttons([
		    HU.div([CLASS,'ramadda-button-ok ramadda-button-apply display-button'], 'Apply'),
		    HU.div([CLASS,'ramadda-button-ok display-button'], 'OK'),
		    HU.div([CLASS,'ramadda-button-cancel display-button'], 'Cancel')]);
		html+=buttons;
		html = HU.div(['style','margin:6px;'], html);
		let dialog = HU.makeDialog({content:html,title:'Time Animation Settings',draggable:true,header:true,my:"left top",at:"left bottom",anchor:$(this)});
		
		dialog.find('.display-button').button().click(function() {
		    if($(this).hasClass('ramadda-button-ok')) {
			_this.attrs.timeAnimationPause = parseFloat(_this.jq('timeanimation_pause').val());
			_this.attrs.timeAnimationStep = parseFloat(_this.jq('timeanimation_step').val());
			if($(this).hasClass('ramadda-button-apply')) return;
		    }
		    dialog.remove();
		});

		break
	    }
	});

	this.jq('time_slider').slider({
	    min: +this.jq('time_slider').attr('slider-min'),
	    max: +this.jq('time_slider').attr('slider-max'),
	    step:getStep(),
	    value:+this.jq('time_slider').attr('slider-value'),
	    slide: ( event, ui )=> {
		let current = getSliderTime(ui.value);
		this.jq('time_current').html(current.format('isoDate'));
	    },
	    stop: ( event, ui )=> {
		timeSliderStop(ui.value);
	    }});

	this.jq('image_rotation_slider').slider({
	    min: -360,
	    max: 360,
	    step:1,
	    value:this.jq('image_rotation_slider').attr('slider-value'),
	    slide:function(event,ui) {
		setRotation(event,ui);
	    },
	    stop: function( event, ui ) {
		setRotation(event,ui);
	    }});


	this.jq('image_opacity_slider').slider({		
	    min: 0,
	    max: 1,
	    step:0.01,
	    value:this.jq('image_opacity_slider').attr('slider-value'),
	    slide:function(event,ui) {
		setOpacity(event,ui);
	    },
	    stop: function( event, ui ) {
		setOpacity(event,ui);
	    }});
	
	this.makeFeatureFilters();
	if(this.isMap() && this.mapLoaded) {
	    let addColor= (obj,prefix, strings) => {
		if(obj && Utils.stringDefined(obj.property)) {
		    let div = this.getColorTableDisplay(obj.colorTable,obj.min,obj.max,true,obj.isEnumeration, strings);
		    let html = HU.b(HU.center(this.makeLabel(obj.property,true)));
		    if(obj.isEnumeration) {
			html+=HU.div(['style','max-height:150px;overflow-y:auto;'],div);
		    } else {
			html+=HU.center(div);
		    }

		    this.jq('legendcolortable_'+prefix.toLowerCase()).html(html);
		    this.initColorTableDots(obj,this.jq('legendcolortable_'+prefix.toLowerCase()));

		    if(obj.isEnumeration) {
			if(!this.extraFilter) this.extraFilter = {};
			let slices =jqid(this.domId('legendcolortable_'+ prefix.toLowerCase())).find('.display-colortable-slice'); 
			slices.css('cursor','pointer');
			slices.click(function() {
			    let ct = Utils.ColorTables[obj.colorTable];
			    let html = Utils.getColorTableDisplay(ct, 0,0, {
				tooltips:strings,
				showColorTableDots:true,
				horizontal:false,
				showRange: false,
			    });
			    html = HU.div(['style','max-height:200px;overflow-y:auto;margin:2px;'], html);
			    let dialog = HU.makeDialog({content:html,title:HU.div(['style','margin-left:20px;margin-right:20px;'], _this.makeLabel(obj.property,true)+' Legend'),header:true,my:"left top",at:"left bottom",draggable:true,anchor:$(this)});
			    _this.initColorTableDots(obj, dialog);
			});
		    }
		}	
	    };

	    let ctProps = [];
	    this.getFeatureInfoList().forEach(info=>{
		if(!info.isColorTableSelect()) return;
		ctProps.push(info);
	    });

	    if(ctProps.length>1) {
		let menu = HU.select('',['id',this.domId('colortableproperty')],
				     ctProps.map(info=>{return {
					 value:info.id,label:info.getLabel()}}),this.attrs.fillColorBy.property);
		this.jq('legendcolortableprops').html(HU.b('Color by: ') + menu);
		this.jq('colortableproperty').change(()=>{
		    let val = this.jq('colortableproperty').val();
		    let info = this.getFeatureInfo(val);
		    if(!info) return;
		    $.extend(this.attrs.fillColorBy,{
			property:info.id,
			min:info.min,
			max:info.max});
		    this.applyMapStyle();
		    this.display.makeLegend();
		});
	    }
	    if(this.getProperty('showColorTableLegend',true)) {
		addColor(this.attrs.fillColorBy,'Fill',this.fillStrings);
		addColor(this.attrs.strokeColorBy,'Stroke',this.strokeStrings);
	    }
	}	



	if(this.isGroup()) {
	    this.getChildren().forEach(mapGlyph=>{mapGlyph.initLegend();});
	}
    },

    convertPopupText:function(text) {
	if(this.getImage()) {
	    text = text.replace(/\${image}/g,HU.image(this.style.imageUrl,['width','200px']));
	}
	return text;
    },
    
    isGroup:function() {
	return this.getType() ==GLYPH_GROUP;
    },
    isEntry:function() {
	return this.getType() ==GLYPH_ENTRY;
    },
    isImage:function() {
	return this.getType() ==GLYPH_IMAGE;
    },    
    isMap:function() {
	return this.getType()==GLYPH_MAP;
    },
    isRings:function() {
	return this.getType()==GLYPH_RINGS;
    },    
    isMapServer:function() {
	return this.getType()==GLYPH_MAPSERVER;
    },    
    isMultiEntry:function() {
	return this.getType()==GLYPH_MULTIENTRY;
    },
    getShowMultiData:function() {
	return this.attrs.showmultidata;
    },
    setShowMultiData:function(v) {
	this.attrs.showmultidata = v;
    },
    setMapServerUrl:function(url,wmsLayer,legendUrl,predefined,mapOptions) {
	this.style.legendUrl = legendUrl;
	this.attrs.mapServerUrl = url;
	this.attrs.wmsLayer = wmsLayer;
	this.attrs.predefinedLayer = predefined;
	this.mapServerOptions = mapOptions;

	if(Utils.stringDefined(predefined)) {
	    if(!Utils.stringDefined(this.attrs.name)) {
		let mapLayer = RAMADDA_MAP_LAYERS_MAP[predefined];
		if(mapLayer) this.attrs.name = mapLayer.name;
	    }
	} else {
	    if(!Utils.stringDefined(wmsLayer)) {
		if(url.match('&request=GetMap')) {
		    try {
			let _url = new URL(url);
			url = _url.protocol+'//' + _url.host +_url.pathname;
			wmsLayer = _url.searchParams.get('layers');
		    } catch(err) {
			console.log(err);
		    }
		}
	    }
	    if(!Utils.stringDefined(this.attrs.name)) {
		let _url = new URL(url);
		this.attrs.name = _url.hostname;
	    }
	}



    },
    getAttributes: function() {
	return this.attrs;
    },
    setMapLayer:function(mapLayer) {
	this.mapLayer = mapLayer;
	if(mapLayer) {
	    mapLayer.mapGlyph = this;
	    mapLayer.textGetter = (feature)=>{
		let text  = this.getPopupText();
		if(text=='none') return null;
		if(!Utils.stringDefined(text)) text = '${default}';
		if(!feature.attributes) return null;
		return this.applyMacros(text, feature.attributes);
	    };
	}
    },
    getMapLayer: function() {
	return this.mapLayer;
    },
    setMapServerLayer:function(mapLayer) {
	this.mapServerLayer = mapLayer;
	if(mapLayer)
	    mapLayer.mapGlyph = this;
    },
    getMapServerLayer: function() {
	return this.mapServerLayer;
    },    
    checkMapLayer:function(andZoom) {
	//Only create the map if we're visible
	if(!this.isMap() || !this.isVisible()) return;
	if(this.mapLayer==null) {
	    if(!Utils.isDefined(andZoom)) andZoom = true;
	    this.setMapLayer(this.display.createMapLayer(this,this.attrs,this.style,andZoom));
	    this.applyMapStyle();
	}
    },
    getMapServerUrl:function() {
	let url=this.attrs.mapServerUrl;
	//Convert malformed TMS url
	url = url.replace(/\/{/g,"/${");
	if(this.getDatacubeVariable()) {
	    let variable = this.getDatacubeVariable();
	    url = url.replace(/\{colorbar\}/,variable.colorBarName);
	    url = url.replace(/\{vmin\}/,variable.colorBarMin);
	    url = url.replace(/\{vmax\}/,variable.colorBarMax);	    	    
	    if(variable.attrs.time_coverage_start) {
		let time = this.getCurrentTimeStep();
		url = url.replace(/\{time\}/,encodeURIComponent(time));
	    }
	}
	return url;
    },

    getDatacubeVariable:function() {
	return  this.attrs.variable;
    },
    getDatacubeAttrs:function() {
	return this.getDatacubeVariable()?.attrs;
    },
    getDatacubeAttr:function(attr) {
	let attrs =  this.getDatacubeAttrs();
	return attrs?attrs[attr]:null;
    },    
    getCurrentTimeStep:function() {
	if(Utils.isDefined(this.attrs.currentTimeStep)) {
	    return  this.formatDate(new Date(this.attrs.currentTimeStep));
	}
	if(!this.getDatacubeAttr('time_coverage_end')) return null;		
	return this.formatDate(new Date(this.getDatacubeAttr('time_coverage_end')));
    },

    formatDate: function(date) {
	return date.format('isoDate');
    },
    createMapServer:function() {
	this.mapServerLayer =  this.display.getMap().createXYZLayer(this.getName(), this.getMapServerUrl());
    },
    
    checkMapServer:function(andZoom) {
	if(!this.isMapServer()) return;
	if(this.mapServerLayer==null) {
	    let url=this.attrs.mapServerUrl;
	    let wmsLayer=this.attrs.wmsLayer;
	    if(Utils.stringDefined(wmsLayer)) {
		this.mapServerLayer = MapUtils.createLayerWMS(this.getName(), url, {
		    layers: wmsLayer,
		    format: "image/png",
		    isBaseLayer: false,
		    srs: "epse:4326",
		    transparent: true
		}, {
		    opacity:1.0
		});
	    } else if(Utils.stringDefined(url)) {
		this.createMapServer();
	    } else if(Utils.stringDefined(this.attrs.predefinedLayer)) {
		let mapLayer = RAMADDA_MAP_LAYERS_MAP[this.attrs.predefinedLayer];
		if(mapLayer) {
		    this.mapServerLayer = this.display.getMap().makeMapLayer(this.attrs.predefinedLayer);
		} else {
		    console.error("Unknown map layer:" +this.attrs.predefinedLayer);
		}
	    } else {
		console.error("No map server url defined");
		return;
	    }

	    if(!Utils.isDefined(andZoom)) andZoom = true;
	    if(Utils.isDefined(this.style.opacity)) {
		this.mapServerLayer.opacity = +this.style.opacity;
	    }

	    this.mapServerLayer.setVisibility(this.isVisible());
	    this.mapServerLayer.isBaseLayer = false;
	    this.mapServerLayer.visibility = this.isVisible();
	    this.mapServerLayer.canTakeOpacity = true;
	    this.display.getMap().addLayer(this.mapServerLayer,true);
	}
    },

    getImage:function() {
	return this.image;
    },
    setImage:function(image) {
	this.image = image;
	this.image.mapGlyph = this;
	this.initImageLayer(this.image);
    },     
    hasMapFeatures: function() {
	if(!this.isMap() || !this?.mapLayer?.features || this.mapLayer.features.length==0) return false;
	return true;
    },
    canDoMapStyle: function() {
	if(!this.hasMapFeatures() || !this.mapLayer.features[0].attributes ||
	   Object.keys(this.mapLayer.features[0].attributes).length==0) {
	    return false;
	}
	return true;
    },

    applyPropertiesComponent: function(newStyle) {
	let oldStyle = this.style;
	this.style=newStyle;
	if(this.isStraightLine()) {
	    let changed = false;
	    let dots=  this.jq(ID_ADDDOTS).is(':checked');
	    if(dots!=this.attrs.addDots) {
		this.attrs.addDots= dots;
		changed=true;
	    }
	    Object.keys(newStyle).forEach(key=>{
		if(key.indexOf('dot')>=0 && oldStyle[key]!=newStyle[key]) changed=true;
	    });
	    let v = this.jq(ID_LINETYPE).val();
	    if(v!=this.attrs.lineType) {
		changed=true;
		this.attrs.lineType = v;
	    }
	    if(changed) {
		this.checkLineType();
	    }
	}


	if(this.isMap()) {
	    this.setMapPointsRange(jqid('mappoints_range').val());
	    this.setMapLabelsTemplate(jqid('mappoints_template').val());	    
	    let styleGroups = this.getStyleGroups();
	    let groups = [];
	    for(let i=0;i<20;i++) {
		let prefix = 'mapstylegroups_' + i;
		let group = styleGroups[i];
		let label = jqid(prefix+'_label').val();
		if(!Utils.stringDefined(label)) continue;
		if(!group) group = {
		    style:{},
		    indices:[],
		}
		group.label = label
		group.style = {};

		let value;
		if(Utils.stringDefined(value=jqid(prefix+'_fillcolor').val())) group.style.fillColor = value;
		if(Utils.stringDefined(value=jqid(prefix+'_fillopacity').val())) group.style.fillOpacity = value;
		if(Utils.stringDefined(value=jqid(prefix+'_strokecolor').val())) group.style.strokeColor = value;
		if(Utils.stringDefined(value=jqid(prefix+'_strokewidth').val())) group.style.strokeWidth = value;
		if(Utils.stringDefined(value=jqid(prefix+'_fillpattern').val())) group.style.fillPattern = value;
		groups.push(group);
	    }
	    this.attrs.styleGroups = groups;
	}	    

	if(!this.canDoMapStyle()) return;
	this.attrs.fillColors = this.jq('fillcolors').is(':checked');
	let getColorBy=(prefix)=>{
	    return {
		property:this.jq(prefix +'colorby_property').val(),
		min:this.jq(prefix +'colorby_min').val(),
		max:this.jq(prefix +'colorby_max').val(),
		colorTable:this.jq(prefix +'colorby_colortable').val()};		
	};

	this.attrs.fillColorBy =  getColorBy('fill');
	this.attrs.strokeColorBy =  getColorBy('stroke');	

	let rules = [];
	for(let i=0;i<20;i++) {
	    if(!Utils.stringDefined(jqid('mapproperty_' + i).val()) &&
	       !Utils.stringDefined(jqid('mapstyle_' + i).val())) {
		continue;
	    }
	    let rule = {
		property:jqid('mapproperty_' + i).val(),
		type:jqid('maptype_' + i).val(),
		value:jqid('mapvalue_' + i).val(),
		style:jqid('mapstyle_' + i).val(),
	    }
	    rules.push(rule);
	}
	this.attrs.mapStyleRules =rules;
    },
    getColorTableDisplay:function(id,min,max,showRange,isEnum,strings) {
	if(isEnum) showRange=false;
	let ct = Utils.ColorTables[id];
	if(!ct) {
	    return "----";
	}
        let display = Utils.getColorTableDisplay(ct,  min??0, max??1, {
	    tooltips:strings,
	    showColorTableDots:isEnum&& strings.length<=15,
	    horizontal:!isEnum || strings.length>15,
	    showRange: false,
            height: "20px",
	    showRange:showRange
        });
	let attrs = [TITLE,id,'style','margin-right:4px;',"colortable",id]
	//	if(ct.colors.length>20)   attrs.push(STYLE,HU.css('width','400px'));
        return  HtmlUtils.div(attrs,display);
    },
    initColorTables: function(currentColorbar) {
	if(!currentColorbar)
	    this.currentColorbar=this.getDatacubeVariable()?.colorBarName;
	currentColorbar = currentColorbar??this.getDatacubeVariable()?.colorBarName;
	let items = [];
	let image;
	let html = '';
	this.display.colorbars.forEach(coll=>{
	    let cat=coll[0];
	    html+=HU.b(cat)+'<br>';
	    coll[2].forEach(c=>{
		let name = c[0];
		let url = 'data:image/png;base64,' + c[1];
		html+=HU.image(url,['class','ramadda-clickable','colorbar',name, 'height','20px','width','256px','title',name]);
		html+='<br>';
		if(name==currentColorbar) {
		    image = c[1];
		}
	    });
	});
	let url = image?('data:image/png;base64,' + image):null;
	this.jq('colortableproperties').html(HU.div(['id',this.domId('colortable'),'class','ramadda-clickable','title','Click to select color bar'],
						    url? HU.image(url,['height','20px','width','256px']):'No image'));

	let _this = this;
	html = HU.div(['style','margin:8px;max-height:200px;overflow-y:auto;'], html);
	this.jq('colortable').click(function() {
	    let colorSelect = HU.makeDialog({content:html,
					     my:'left top',
					     at:'left bottom',
					     anchor:$(this)});
	    colorSelect.find('img').click(function() {
		_this.currentColorbar = $(this).attr('colorbar');
		colorSelect.remove();
		_this.initColorTables(_this.currentColorbar);
	    });
	});
    },
    initPropertiesComponent: function(dialog) {
	this.jq('createroute').button().click(()=>{
	    this.makeGroupRoute();
	});


	let _this = this;
	if(this.isMapServer() && this.getDatacubeVariable()) {
	    if(!this.display.colorbars) {
		let dataCubeServers=  MapUtils.getMapProperty('datacubeservers','').split(',');
		let url = dataCubeServers[0]+'/colorbars';
		$.getJSON(url, (data)=> {
		    this.display.colorbars = data;
		    this.initColorTables();
		});
	    } else {
		this.initColorTables();
	    }
	}	    	


	let clearElevations = this.jq('clearelevations');
	clearElevations.button().click(function(){
	    _this.attrs.elevations = null;
	    $(this).attr('disabled','disabled');
	    $(this).addClass('ramadda-button-disabled');
	});
	if(!this.attrs.elevations) {
	    this.jq('clearelevations').prop('disabled',true);
	}

	this.jq('makeelevations').button().click(function(){
	    $(this).html('Fetching elevations');
	    let callback =(count,total)=>{
		$(this).html('Processed ' + count + ' of ' + total);
		return true;
	    }
	    let done = ()=>{
		clearElevations.removeClass('ramadda-button-disabled');
		clearElevations.attr('disabled',null);
		$(this).html('Done');
		setTimeout(()=>{
		    $(this).html('Add elevations');
		},3000);

	    }
	    _this.addElevations(callback,done)
	});


	let decorate = (prefix) =>{
	    let div = this.getColorTableDisplay(this.jq(prefix+'colorby_colortable').val(),NaN,NaN,false);
	    this.jq(prefix+'colorby_colortable_label').html(div);
	};

	decorate('fill');
	decorate('stroke');	
	dialog.find(".ramadda-colortable-select").click(function() {
	    let prefix = $(this).attr('prefix');
	    let ct = $(this).attr("colortable");
	    _this.jq(prefix+'colorby_colortable').val(ct);
	    decorate(prefix);
	});
	Utils.displayAllColorTables(this.display.domId('fillcolorby'));
	Utils.displayAllColorTables(this.display.domId('strokecolorby'));

	let initColor  = prefix=>{
	    dialog.find('#'+this.domId(prefix+'colorby_property')).change(function() {
		let prop =  $(this).val();
		_this.featureInfo.every(info=>{
		    if(info.property!=prop) return true;
		    if(info.isString() || info.isEnumeration()) {
			_this.jq(prefix+'colorby_min').val('');
			_this.jq(prefix+'colorby_max').val('');		    
		    }  else {
			_this.jq(prefix+'colorby_min').val(info.min);
			_this.jq(prefix+'colorby_max').val(info.max);
		    }
		    return true;
		});
	    });
	};
	initColor('fill');
	initColor('stroke');

	dialog.find('[mapproperty_index]').change(function() {
	    let info = _this.featureInfoMap[$(this).val()];
	    if(!info) return;
	    let index  = $(this).attr('mapproperty_index');	    
	    let tt = "";
	    let value = jqid('mapvalue_' + index).val();
	    let wrapper = jqid('mapvaluewrapper_' + index);
	    if(info.isNumeric()) {
		wrapper.html(HU.input("",value,['id','mapvalue_' + index,'size','15']));
		tt=info.min +" - " + info.max;
	    }  else  if(info.samples.length) {
		tt = Utils.join(info.getSamplesLabels(), ", ");
		if(info.isEnumeration()) {
		    wrapper.html(HU.select("",['id','mapvalue_' + index],info.samples,value));
		} else {
		    wrapper.html(HU.input("",value,['id','mapvalue_' + index,'size','15']));
		}
	    }
	    jqid('mapvalue_' + index).attr('title',tt);
	});


    },

    getFeaturesTable:function(id) {
	let columns  =this.getFeatureInfoList().filter(info=>{
	    return info.showTable();
	});
	let table;
	this.featureTableMap = {};

	let featureInfo = this.getFeatureInfoList();
	let rowCnt=0;
	let stats;
	this.mapLayer.features.forEach((feature,rowIdx)=>{
	    if(Utils.isDefined(feature.isVisible) && !feature.isVisible) {
		return
	    }
	    let attrs = feature.attributes;
	    let first = rowCnt++==0;
	    if(first) {
		table = HU.openTag('table',['id',id,'table-ordering','true','table-searching','true','table-height','400px','class','stripe rowborder ramadda-table'])
		table+='<thead><tr>';
		stats = [];
		columns.forEach((column,idx)=>{
		    table+=HU.tag('th',[],column.getLabel(true));
		    stats.push({total:0,count:0,min:0,max:0});
		});
		table+=HU.close('tr','thead','tbody');
	    }
	    this.featureTableMap[rowIdx] =feature;
	    table+=HU.openTag('tr',['title','Click to zoom to','featureidx', rowIdx,'class','imdv-feature-table-row ramadda-clickable']);
	    columns.forEach((column,idx)=>{
		let stat =  stats[idx];
		let v= this.getFeatureValue(feature,column.property)??'';
		if(v && Utils.isDefined(v.value)) v = v.value;
		let nv = +v;
		let sv = String(v);
		let isNumber = column.isNumeric();
		if(isNumber && sv!='' && !isNaN(nv)) {
		    stat.count++;
		    stat.min = first?nv:Math.min(nv, stat.min);
		    stat.max = first?nv:Math.max(nv, stat.max);		    
		    stat.total+=nv;
		}
		//Check for html. Maybe just convert to entities?
		if(sv.indexOf('<')>=0) {
		    sv = Utils.stripTags(sv);
		}
		sv=column.format(sv);
		table+=HU.tag('td',['style',isNumber?'text-align:right':'','align',isNumber?'right':'left'],sv);
	    });
	});
	if(stats) {
	    table+='<tfoot><tr>';
	    let fmt = (label,amt) =>{
		return HU.tr(HU.td(['style','text-align:right','align','right'],HU.b(label)) +
			     HU.td(['style','text-align:right','align','right'],Utils.formatNumberComma(amt)));
	    };
	    columns.forEach((column,idx)=>{
		let stat =  stats[idx];
		table+='<td align=right>';
		if(stat.count!=0) {
		    let inner = '<table>';
		    inner +=
			fmt('Total:', stat.total) +
			fmt('Min:', stat.min) +
			fmt('Max:', stat.max) +
			fmt('Avg:', stat.total/stat.count);
		    inner+='</table>';
		    table+=inner;
		}
		table+='</td>';
	    });
	    table+='</tr></tfoot>';
	    table+=HU.close('tbody');
	}

	return table;
    },
    downloadFeaturesTable:function(id) {
	let columns;
	let csv='';
	this.mapLayer.features.forEach((feature,rowIdx)=>{
	    if(Utils.isDefined(feature.isVisible) && !feature.isVisible) {
		return;
	    }
	    let attrs = feature.attributes;
	    if(columns==null) {
		columns  =this.getFeatureInfoList().filter(info=>{
		    return info.showTable();
		});
		let rows = columns.map((column,idx)=>{ return column.getLabel();});
		csv+=Utils.join(rows,',');
		csv+='\n';
	    }
	    let rows = columns.map((column,idx)=>{
		let v = attrs[column.property]??'';
		if(Utils.isDefined(v.value)) v = v.value;
		return  String(v).replace(/\n/g,'_nl_');
	    });
	    csv+=Utils.join(rows,',');
	    csv+='\n';
	});
	Utils.makeDownloadFile('features.csv',csv);
    },

    updateFeaturesTable:function() {
	if(!this.featuresTableDialog) return;
	let tableId = HU.getUniqueId("table");
	let table =this.getFeaturesTable(tableId);
	let html='';
	if(!table) {
	    html += HU.div(['style','width:200px;margin:10px;'],'No data');
	} else {
	    html +=  
		HU.div(['style',HU.css('margin','5px','max-width','1000px','overflow-x','scroll')],table);
	    html = HU.div(['style','margin:5px;'], html);
	}
	this.jq('featurestable').html(html);
	HU.formatTable('#'+tableId,{scrollX:true});
	let _this = this;
	this.featuresTableDialog.find('.imdv-feature-table-row').click(function() {
	    let feature = _this.featureTableMap[$(this).attr('featureidx')];
	    _this.display.getMap().centerOnFeatures([feature]);
	});
    },
    showFeaturesTable:function(anchor) {
	if(this.featuresTableDialog)
	    this.featuresTableDialog.remove();
	let html =  HU.div(['id',this.domId('downloadfeatures'),'class','ramadda-clickable'],'Download Table');
	html+=HU.div(['id',this.domId('featurestable')]);
	html = HU.div(['style','margin:10px;'], html);
	this.featuresTableDialog =
	    HU.makeDialog({content:html,title:this.name,header:true,draggable:true,
			   my:'left top',
			   at:'left bottom',
			   anchor:anchor});
	
	this.updateFeaturesTable();
	this.jq('downloadfeatures').button().click(()=>{
	    this.downloadFeaturesTable();
	});
    },
    getHelp:function(url,label) {
	if(url.startsWith('#')) url = '/userguide/imdv.html' + url;
	return HU.href(Ramadda.getUrl(url),HU.getIconImage(icon_help) +' ' +(label??'Help'),['target','_help']);
    },
    getCentroid: function() {
	if(this.features && this.features.length) {
	    return  this.features[0].geometry.getCentroid(true);
	}
    },
    makeGroupRoute: function() {
	let mode = this.display.jq('routetype').val()??'car';
	let provider = this.display.jq('routeprovider').val();
	let pts = [];
	if(this.children) {
	    this.children.forEach(child=>{
		let centroid  = child.getCentroid();
		if(!centroid) return;
		var lonlat = this.getMap().transformProjPoint(centroid)
		pts.push(lonlat);
	    });
	}
	if(pts.length==0) {
	    alert('No points to make route from');
	    return;
	}
	this.display.createRoute(provider,mode,pts);
    },
    getPropertiesComponent: function(content) {
	if(this.isGroup() && this.display.isRouteEnabled()) {
	    let html = this.display.createRouteForm();
	    let buttons  =HU.div(['id',this.domId('createroute'),CLASS,'display-button'], 'Create Route');
	    html+=HU.div(['style',HU.css('margin-top','5px')], buttons);
	    html=HU.div(['style',HU.css('margin','5px')],html);
	    content.push({header:'Make Route', contents:html});
	}


	if(!this.canDoMapStyle()) return;
	let attrs = this.mapLayer.features[0].attributes;
	let featureInfo = this.featureInfo = this.getFeatureInfoList();
	let keys  = Object.keys(featureInfo);
	let numeric = featureInfo.filter(info=>{return info.isNumeric();});
	let enums = featureInfo.filter(info=>{return info.isEnumeration();});
	let colorBy = '';
	colorBy+=HU.leftRightTable(HU.checkbox(this.domId('fillcolors'),['id',this.domId('fillcolors')],
					       this.attrs.fillColors,'Fill Colors'),
				   this.getHelp('#mapstylerules'));

	numeric = featureInfo;
	if(numeric.length) {
	    let numericProperties=Utils.mergeLists([['','Select']],numeric.map(info=>{return {value:info.property,label:info.getLabel()};}));
	    let mapComp = (obj,prefix) =>{
		let comp = '';
		comp+=HU.div(['class','formgroupheader'], 'Map value to ' + prefix +' color')+ HU.formTable();
		comp += HU.formEntry('Property:', HU.select('',['id',this.domId(prefix+'colorby_property')],numericProperties,obj.property) +HU.space(2)+ HU.b('Range: ') + HU.input('',obj.min??'', ['id',this.domId(prefix+'colorby_min'),'size','6','title','min value']) +' -- '+    HU.input('',obj.max??'', ['id',this.domId(prefix+'colorby_max'),'size','6','title','max value']));
		comp += HU.hidden('',obj.colorTable||'blues',['id',this.domId(prefix+'colorby_colortable')]);
		comp+=HU.formEntry('Color table:', HU.div(['id',this.domId(prefix+'colorby_colortable_label')])+
				   Utils.getColorTablePopup(null,null,'Select',true,'prefix',prefix));
		comp+=HU.close('table');
		return comp;
	    };
	    colorBy+=mapComp(this.attrs.fillColorBy ??{},'fill');
	    colorBy+=mapComp(this.attrs.strokeColorBy ??{},'stroke');	    
	}

	let properties=Utils.mergeLists([['','Select']],featureInfo.map(info=>{
	    return {value:info.property,label:info.getLabel()};}));
	let ex = '';
	let helpLines = [];
	featureInfo.forEach(info=>{
	    helpLines.push(info.id);
	    let seen ={};
	    let list =[];
	    let label = HU.b(this.makeLabel(info.property,true));
	    let line = ''
	    if(info.isNumeric()) {
		line =  info.min +' - ' + info.max;
	    } else if(info.isEnumeration()) {
		line =  Utils.join(info.getSamplesValues(),', ');
	    } else {
		line =  Utils.join(info.getSamplesValues(),', ');
	    }
	    ex+=label+': ' + line +'<br>';
	});
	let c = OpenLayers.Filter.Comparison;
	let operators = [c.EQUAL_TO,c.NOT_EQUAL_TO,c.LESS_THAN,c.GREATER_THAN,c.LESS_THAN_OR_EQUAL_TO,c.GREATER_THAN_OR_EQUAL_TO,[c.BETWEEN,'between'],[c.LIKE,'like'],[c.IS_NULL,'is null'],['use','Use']];
	let rulesTable = HU.formTable();
	let sample = 'Samples&#013;';
	for(a in attrs) {
	    let v = attrs[a]?String(attrs[a]):'';
	    v = v.replace(/"/g,'').replace(/\n/g,' ');
	    sample+=a+'=' + v+'&#013;';
	}
	rulesTable+=HU.tr([],HU.tds(['style','font-weight:bold;'],['Property','Operator','Value','Style']));
	let rules = this.getMapStyleRules();
	let styleTitle = 'e.g.:&#013;fillColor:red&#013;fillOpacity:0.5&#013;strokeColor:blue&#013;strokeWidth:1&#013;strokeDashstyle:solid|dot|dash|dashdot|longdash|longdashdot';
	for(let index=0;index<20;index++) {
	    let rule = index<rules.length?rules[index]:{};
	    let value = rule.value??'';
	    let info = this.featureInfoMap[rule.property];
	    let title = sample;
	    let valueInput;
	    if(info) {
		if(info.isNumeric())
		    title=info.min +' - ' + info.max;
		else if(info.samples.length)
		    title = Utils.join(info.getSamplesLabels(), ', ');
	    }
	    if(info?.isEnumeration()) {
		valueInput = HU.select('',['id','mapvalue_' + index],info.samples,value); 
	    } else {
		valueInput = HU.input('',value,['id','mapvalue_' + index,'size','15']);
	    }
	    let propSelect =HU.select('',['id','mapproperty_' + index,'mapproperty_index',index],properties,rule.property);
	    let opSelect =HU.select('',['id','maptype_' + index],operators,rule.type);	    
	    valueInput =HU.span(['id','mapvaluewrapper_' + index],valueInput);
	    let s = Utils.stringDefined(rule.style)?rule.style:'';
	    let styleInput = HU.textarea('',s,['id','mapstyle_' + index,'rows','3','cols','30','title',styleTitle]);
	    rulesTable+=HU.tr(['valign','top'],HU.tds([],[propSelect,opSelect,valueInput,styleInput]));
	}
	rulesTable += '</table>';
	let table = HU.b('Style Rules')+HU.div(['class','imdv-properties-section'], rulesTable);
	content.push({header:'Style Rules', contents:colorBy+table});


	let mapPointsRange = HU.leftRightTable(HU.b('Visiblity limit: ') + HU.select('',[ID,'mappoints_range'],this.display.levels,this.getMapPointsRange()??'',null,true) + ' '+
					       HU.span(['class','imdv-currentlevellabel'], '(current level: ' + this.display.getCurrentLevel()+')'),
					       this.getHelp('#map_labels'));
	let mapPoints = HU.textarea('',this.getMapLabelsTemplate()??'',['id','mappoints_template','rows','6','cols','40','title','Map points template, e.g., ${code}']);

	let propsHelp =this.display.makeSideHelp(helpLines,'mappoints_template',{prefix:'${',suffix:'}'});
	mapPoints = HU.hbox([mapPoints,HU.space(2),'Add property:' + propsHelp]);


	let styleGroups =this.getStyleGroups();
	let styleGroupsUI = HU.leftRightTable('',
					      this.getHelp('#adding_a_map'));
	styleGroupsUI+=HU.openTag('table',['width','100%']);
	styleGroupsUI+=HU.tr([],HU.tds([],
				       ['Group','Fill','Opacity','Stroke','Width','Pattern','Features']));
	for(let i=0;i<20;i++) {
	    let group = styleGroups[i];
	    let prefix = 'mapstylegroups_' + i;
	    styleGroupsUI+=HU.tr([],HU.tds([],[
		HU.input('',group?.label??'',['id',prefix+'_label','size','10']),
		HU.input('',group?.style.fillColor??'',['class','ramadda-imdv-color','id',prefix+'_fillcolor','size','6']),
		HU.input('',group?.style.fillOpacity??'',['title','0-1','id',prefix+'_fillopacity','size','2']),		
		HU.input('',group?.style.strokeColor??'',['class','ramadda-imdv-color','id',prefix+'_strokecolor','size','6']),
		HU.input('',group?.style.strokeWidth??'',['id',prefix+'_strokewidth','size','6']),
		this.display.getFillPatternSelect(prefix+'_fillpattern',group?.style.fillPattern??''),
		Utils.join(group?.indices??[],',')]));
	}
	styleGroupsUI += HU.closeTag('table');
	styleGroupsUI = HU.div(['style',HU.css('max-height','150px','overflow-y','auto')], styleGroupsUI);

	content.push({header:'Style Groups',contents:styleGroupsUI});
	content.push({header:'Labels',
		      contents:mapPointsRange+  HU.b('Label Template:')+'<br>' +mapPoints});

	content.push({header:'Sample Values',contents:ex});
    },
    getStyleGroups: function() {
	if(!this.attrs.styleGroups) {
	    this.attrs.styleGroups = [];
	}
	return this.attrs.styleGroups??[];
    },
    getMapStyleRules: function() {
	return this.attrs.mapStyleRules??[];
    },

    
    numre : /^[\d,\.]+$/,
    cleanupFeatureValue:function(v) {
	if(v===null) return null;
	if(Utils.isDefined(v.value)) {
	    v = v.value;
	}
	let sv = String(v);
	//	if(sv.indexOf('[object')>=0) {console.log('X',v,(typeof v));} else console.log(sv);
	if(sv.trim()=="") return "";
	if(sv.match(this.numre)) {
	    sv = sv.replace(/,/g,'').replace(/^0+/,"");
	}
	return sv;
    },
    getFeatureValue:function(feature,property) {
	if(!feature.attributes) return null;
	let value = feature.attributes[property];
	if(!Utils.isDefined(value)) return null;
	return  this.cleanupFeatureValue(value);
    },
    getFeatureInfoList:function() {
	if(this.featureInfo) return this.featureInfo;
	if(!this.mapLayer?.features) return [];
	let features= this.mapLayer.features;
	if(!this.mapLayer || this.mapLayer.features.length==0) return [];
	let attrs = this.mapLayer.features[0].attributes;
	let keys =   Object.keys(attrs);
	let _this = this;
	let first = [];		
	let middle=[];
	let last = [];
	keys.forEach(key=>{
	    let info = {
		property:key,
		id:Utils.makeId(key),
		min:NaN,
		max:NaN,
		type:'',
		getId:function() {
		    return this.id;
		},
		getProperty:function(prop,dflt,checkGlyph) {
		    if(checkGlyph) dflt=_this.getProperty(prop,dflt);
		    let v =   _this.getProperty(this.id+'.' + prop,dflt);
		    return v;
		},
		show: function() {
		    return  this.getProperty('show',_this.getProperty('feature.show',true));
		},
		showFilter: function() {
		    return   this.getProperty('filter.show',_this.getProperty('filter.show',this.show()));
		},
		showTable: function() {
		    return this.getProperty('table.show',_this.getProperty('table.show',this.show()));
		},
		showPopup: function() {
		    return this.getProperty('popup.show',_this.getProperty('popup.show',this.show()));
		},				
		isColorTableSelect: function() {
		    return this.getProperty('colortable.select',_this.getProperty('colortable.select',false));
		},

		getType:function() {
		    return this.getProperty('type',this.type);
		},
		getLabel:function(addSpan) {
		    let label  =this.getProperty('label');
		    if(!Utils.stringDefined(label)) label  =_this.display.makeLabel(this.property);
		    if(addSpan) label = HU.span(['title',this.property],label);
		    return label;
		},

		format:function(value) {
		    if(this.isNumeric()) {
			let decimals = this.getProperty('format.decimals',null,true);
			if(Utils.isDefined(decimals)&&!isNaN(value)) {
			    value = Utils.trimDecimals(value,decimals);
			}
		    }
		    return value;
		},
		isNumeric:function(){return this.isInt() || this.getType()=='numeric';},
		isInt:function() {return this.getType()=='int';},
		isString:function() {return this.getType()=='string';},
		isEnumeration:function() {return this.getType()=='enumeration';},
		seen:{},
		samples:[],
		getSamplesLabels:function() {
		    return this.samples.map(sample=>{return sample.label;});
		},
		getSamplesValues:function() {
		    return this.samples.map(sample=>{
			let cnt = this.seen[sample.value];
			return sample.value +' ('+ cnt+')';
		    });
		}		

	    };
	    let _c = info.property.toLowerCase();
	    if(_c.indexOf('objectid')>=0) {
		last.push(info);
	    } else if(_c.indexOf('name')>=0) {
		first.push(info);
	    } else {
		middle.push(info);
	    }
	});


	this.featureInfo =  Utils.mergeLists(first,middle,last);
	features.forEach((f,fidx)=>{
	    this.featureInfo.forEach(info=>{
		let value= this.getFeatureValue(f,info.property);
		if(!Utils.isDefined(value)) return;
		if(isNaN(value) || info.samples.length>0) {
		    if(info.samples.length<30) {
			info.type='enumeration';
			if(!info.seen[value]) {
			    info.seen[value] = 0;
			    info.samples.push(value);
			}
			info.seen[value]++;
		    } else {
			info.type='string';
		    }
		} else if(!isNaN(value)) {
		    if(info.type != 'numeric')
			info.type='int';
		    if(Math.round(value)!=value) {
			info.type = 'numeric';
		    }
		    info.min = isNaN(info.min)?value:Math.min(info.min,value);
		    info.max = isNaN(info.max)?value:Math.max(info.max,value);			
		}
	    });
	});


	this.featureInfoMap = {};
	this.featureInfo.forEach(info=>{
	    if(info.samples.length) {
		let items = info.samples.map(item=>{
		    return {value:item,label:Utils.makeLabel(item)};
		});
		info.samples =  items.sort((a,b)=>{
		    return a.label.localeCompare(b.label);
		});
	    }
	    this.featureInfoMap[info.property] = info;
	    this.featureInfoMap[info.id] = info;	    
	});
	return this.featureInfo;
    },
    getFeatureInfo:function(property) {
	this.getFeatureInfoList();
	if(this.featureInfoMap) return this.featureInfoMap[property];
	return null;
    },
    makeLabel:function(l,makeSpan) {
	let info = this.getFeatureInfo(l);
	if(info) return info.getLabel(makeSpan);
	let id = Utils.makeId(l);
	let label = l;
	if(id=='shapestlength') {
	    label =  'Shape Length';
	} else 	if(this.getProperty(id+'.feature.label')) {
	    label =  this.getProperty(id+'.feature.label');
	} else {
	    label =  this.display.makeLabel(l);
	}
	if(makeSpan) label = HU.span(['title','aka:' +id], label);
	return label;
    },

    getProperty:function(key,dflt) {
	let debug = false;
	if(debug)
	    console.log("KEY:" + key);
	if(this.attrs.properties) {
	    if(!this.parsedProperties) {
		this.parsedProperties = Utils.parseMap(this.attrs.properties,"\n","=")??{};
	    }

	    let v = this.parsedProperties[key];
	    if(debug) console.log("V:" + v);
	    if(debug) console.log("PROPS:",this.parsedProperties);	    
	    if(v) {
		return Utils.getProperty(v);
	    }
	}
	return this.display.getMapProperty(key,dflt);
    },
    makeFeatureFilters:function() {
	let _this = this;
	let first = "";
	let sliders = "";
	let strings = "";
	let enums = "";
	let filters = this.attrs.featureFilters = this.attrs.featureFilters ??{};
	this.filterInfo = {};
	this.getFeatureInfoList().forEach(info=>{
	    if(!info.showFilter()) {
		return;
	    }
	    this.filterInfo[info.property] = info;
	    this.filterInfo[info.getId()] = info;	    
	    if(!filters[info.property]) filters[info.property]= {};
	    let filter = filters[info.property];
	    if(!Utils.isDefined(filter.min) || isNaN(filter.min)) filter.min = info.min;
	    if(!Utils.isDefined(filter.max) || isNaN(filter.max)) filter.max = info.max;	    
	    filter.property =  info.property;
	    let id = info.getId();
	    let label = HU.span(['title',info.property],HU.b(info.getLabel()));
	    if(info.isString())  {
		filter.type="string";
		let attrs =['filter-property',info.property,'class','imdv-filter-string','id',this.domId('string_'+ id),'size',20];
		attrs.push('placeholder',this.getProperty(info.property.toLowerCase()+'.filterPlaceholder',''));
		let string=label+":<br>" +
		    HU.input("",filter.stringValue??"",attrs) +"<br>";
		if(info.getProperty('filter.first')) first+=string; else strings+=string;
		return
	    } 
	    if(info.samples.length)  {
		filter.type="enum";
		if(info.samples.length>1) {
		    let sorted = info.samples.sort((a,b)=>{
			return a.value.localeCompare(b.value);
		    });
		    let options = sorted.map(sample=>{
			let label = sample.label +' (' + info.seen[sample.value]+')';
			if(sample.value=='')
			    label = '&lt;blank&gt;' +' (' + info.seen[sample.value]+')';
			return {value:sample.value,label:label}
		    });
		    let line=label+":<br>" +
			HU.select("",['style','width:90%;','filter-property',info.property,'class','imdv-filter-enum','id',this.domId('enum_'+ id),'multiple',null,'size',Math.min(info.samples.length,5)],options,filter.enumValues,50)+"<br>";
		    if(info.getProperty('filter.first')) first+=line; else enums+=line;
		}
		return;
	    }

	    if((info.isNumeric() || info.isInt()) && (info.min<info.max)) {
		let min = info.getProperty('filter.min',info.min);
		let max = info.getProperty('filter.max',info.max);		
		filter.minValue = min;
		filter.maxValue = max;
		if(isNaN(filter.min)||filter.min<min) filter.min = min;
		if(isNaN(filter.max) || filter.max>max) filter.max = max;
		filter.type="range";
		let line =
		    HU.leftRightTable(HU.div(['id',this.domId('slider_min_'+ id),'style','max-width:50px;overflow-x:auto;'],Utils.formatNumber(filter.min??min)),
				      HU.div(['id',this.domId('slider_max_'+ id),'style','max-width:50px;overflow-x:auto;'],Utils.formatNumber(filter.max??max)));
		let slider =  HU.div(['slider-min',min,'slider-max',max,'slider-isint',info.isInt(),
				      'slider-value-min',filter.min??info.min,'slider-value-max',filter.max??info.max,
				      'filter-property',info.property,'feature-id',info.id,'class','imdv-filter-slider',
				      'style',HU.css("display","inline-block","width","100%")],"");
		if(info.getProperty('filter.animate',false)) {
		    line+=HU.table(['width','100%'],
				   HU.tr([],
					 HU.td(['width','18px'],HU.span(['feature-id',info.id,'class','imdv-filter-play ramadda-clickable','title','Play'],HU.getIconImage('fas fa-play'))) +
					 HU.td([],slider)));
		} else {
		    line+=slider;
		}


		line =  HU.b(label)+":<br>" +line;
		if(info.getProperty('filter.first')) first+=line; else sliders+=line;
	    }
	});


	if(sliders!='')
	    sliders = HU.div(['style',HU.css('margin-left','10px','margin-right','20px')],sliders);
	if(first!='')
	    first = HU.div(['style',HU.css('margin-left','10px','margin-right','20px')],first);	    

	let widgets = first+enums+sliders+strings;
	if(widgets!="") {
	    let update = () =>{
		this.display.featureHasBeenChanged = true;
		this.applyMapStyle(true);
		if($("#"+this.zoomonchangeid).is(':checked')) {
		    this.panMapTo();
		}
		this.updateFeaturesTable();
	    };

	    let clearAll = HU.span(['style','margin-right:5px;','class','ramadda-clickable','title','Clear Filters','id',this.domId('filters_clearall')],HU.getIconImage('fas fa-eraser',null,LEGEND_IMAGE_ATTRS));

	    this.zoomonchangeid = HU.getUniqueId("andzoom");

	    widgets = HU.div(['style','padding-bottom:5px;max-height:200px;overflow-y:auto;'], widgets);
	    let filtersHeader ='';
	    if(this.getProperty('filter.zoomonchange.show',true)) {
		filtersHeader = HU.checkbox(this.zoomonchangeid,
					    ['title','Zoom on change','id',this.zoomonchangeid],
					    this.getZoomOnChange(),
					    HU.span(['title','Zoom on change','style','margin-left:12px;'], HU.getIconImage('fas fa-binoculars',[],LEGEND_IMAGE_ATTRS)));
	    }
	    filtersHeader+=HU.span(['id',this.domId('filters_count')],Utils.isDefined(this.visibleFeatures)?'#'+this.visibleFeatures:'');
	    filtersHeader = HU.leftRightTable(filtersHeader, clearAll);

	    if(this.getProperty('filter.toggle.show',true)) {
		let toggle = HU.toggleBlockNew('Filters',filtersHeader + widgets,this.getFiltersVisible(),{separate:true,headerStyle:'display:inline-block;',callback:null});
		this.jq(ID_MAPFILTERS).html(HU.div(['style','margin-right:5px;'],toggle.header+toggle.body));
		HU.initToggleBlock(this.jq(ID_MAPFILTERS),(id,visible)=>{this.setFiltersVisible(visible);});
	    } else  {
		this.jq(ID_MAPFILTERS).html(HU.div(['style','margin-right:5px;'],filtersHeader  + 
						   widgets));
		this.setFiltersVisible(true);		    
	    }


	    jqid(this.zoomonchangeid).change(function() {
		_this.setZoomOnChange($(this).is(':checked'));
	    });

	    this.jq('filters_clearall').click(()=>{
		this.display.featureChanged();
		this.attrs.featureFilters = {};
		this.applyMapStyle();
		this.updateFeaturesTable();
		if($("#"+this.zoomonchangeid).is(':checked')) {
		    this.panMapTo();
		}
	    });
	    this.jq(ID_MAPFILTERS).find('.imdv-filter-string').keypress(function(event) {
		let keycode = (event.keyCode ? event.keyCode : event.which);
                if (keycode == 13) {
		    let key = $(this).attr('filter-property');
		    let filter = filters[key]??{};
		    filter.type='string';
		    filter.stringValue = ($(this).val()??"").trim();
		    filter.property = key;
		    update();
		}
	    });
	    this.jq(ID_MAPFILTERS).find('.imdv-filter-enum').change(function(event) {
		let key = $(this).attr('filter-property');
		let filter = filters[key]??{};
		filter.property = key;
		filter.type='enum';
		filter.enumValues=$(this).val();
		update();
	    });

	    let sliderMap = {};
	    let sliders = this.jq(ID_MAPFILTERS).find('.imdv-filter-slider');
	    sliders.each(function() {
		let theFeatureId = $(this).attr('feature-id');
		let onSlide = function( event, ui, force) {
		    let featureInfo = _this.getFeatureInfo(theFeatureId);
		    let id = featureInfo.property;
		    let filter = filters[id]??{};
		    if(ui.animValues) {
			filter.min = ui.animValues[0];
			filter.max = ui.animValues[1];			
		    } else {
			if(ui.handleIndex==0)
			    filter.min = ui.value;
			else
			    filter.max = ui.value;
		    }

		    filter.property=id;
		    _this.jq('slider_min_'+ featureInfo.getId()).html(Utils.formatNumber(filter.min));
		    _this.jq('slider_max_'+ featureInfo.getId()).html(Utils.formatNumber(filter.max));			    
		    if(force ||_this.getProperty('filter.live') ||  _this.getProperty(featureInfo.getId()+'.filter.live')) {
			update();
			return
		    }
		    if(!_this.sliderThrottle) 
			_this.sliderThrottle=Utils.throttle(()=>{
			    update();
			},500);
		    _this.sliderThrottle();
		};
		let min = +$(this).attr('slider-min');
		let max = +$(this).attr('slider-max');
		let isInt = $(this).attr('slider-isint')=="true";
		let step = 1;
		let range = max-min;
		if(!isInt) {
		    step = range/100;
		} else {
		    if(range>10)
			step = Math.max(1,Math.floor(range/100));		    
		}
		sliderMap[$(this).attr('feature-id')] = {
		    slider:   $(this),
		    slide:onSlide,
		    min:min,
		    max:max,
		    step:step
		}
		//Round the step
		//		if(step>10) step=parseInt(step);
		let args = {
		    range:true,
		    min: parseFloat(min),
		    max: parseFloat(max),
		    step:step,
		    values:[parseFloat($(this).attr('slider-value-min')),
			    parseFloat($(this).attr('slider-value-max'))],
		    slide: onSlide};
		$(this).slider(args);		
	    });

	    this.jq(ID_MAPFILTERS).find('.imdv-filter-play').click(function() {
		let playing = $(this).attr('playing');
		let info = _this.filterInfo[$(this).attr('feature-id')];
		if(!info) return;
		let animation = _this.animationInfo[info.id];
		if(!animation) {
		    _this.animationInfo[info.id] = animation  = {};
		}
		let sliderInfo = sliderMap[info.id];
		if(!sliderInfo) return;
		let slider = sliderInfo.slider;
		if(Utils.isDefined(playing) && playing==='true') {
		    $(this).html(HU.getIconImage('fas fa-play'));
		    $(this).attr('playing',false);
		    if(animation.timeout) {
			clearTimeout(animation.timeout);
			animation.timeout = null;
		    }
		} else {
		    $(this).html(HU.getIconImage('fas fa-stop'));
		    $(this).attr('playing',true);
		    let step = (_values) =>{
			let values = _values?? slider.slider('values');
			if(values[1]>=sliderInfo.max) {
			    $(this).html(HU.getIconImage('fas fa-play'));
			    $(this).attr('playing',false);			    
			    return;
			}
			values = [values[0],values[1]];
			let stepSize = parseFloat(info.getProperty('filter.animate.step',sliderInfo.step*2));
			values[1]=Math.min(sliderInfo.max,values[1]+stepSize);
			slider.slider('values',values);
			sliderInfo.slide({},{animValues:values},true);
			animation.timeout = setTimeout(step,info.getProperty('filter.animate.sleep',1000));
		    };
		    let values = slider.slider('values');
		    //If at the end then set to the start
		    if(values[1]>=sliderInfo.max) {
			values[1] = values[0];
			step(values);
		    } else {
			step();
		    }
		}

	    });

	}
    },
    handleMapLoaded: function(map,layer) {
	//Check if there are any KML ground overlays
	//TODO: limit the number we add?
	if(layer?.protocol?.format?.groundOverlays) {
	    let format = layer.protocol.format;
	    let text=(node,tag)=>{
		let child = format.getElementsByTagNameNS(node,'*',tag);
		if(!child.length) return null;
		return child[0].innerHTML;
	    };
	    this.imageLayers=[];
	    let cnt=0;
	    format.groundOverlays.forEach(go=>{
		let icons = format.getElementsByTagNameNS(go,'*','Icon');
		let ll = format.getElementsByTagNameNS(go,'*','LatLonBox');
		if(!icons.length || !ll.length) return;
		ll =ll[0];
		let name = text(go,'name');
		let url = text(icons[0],'href');
		if(!url) return;
		url = url.replace(/&amp;/g,'&');
		let north = text(ll,'north');
		let south = text(ll,'south');
		let east = text(ll,'east');
		let west = text(ll,'west');			    
		let obj = {
		    id:'groundoverlay_'+(cnt++),
		    url:url,
		    name:name,
		    north:north,west:west,south:south,east:east
		}
		this.imageLayers.push(obj);
		//We might have a bunch of overlays so don't add them if there are lots
		this.isImageLayerVisible(obj,cnt<=3);
	    });
	}

	this.mapLoaded = true;
	this.makeFeatureFilters();
	this.applyMapStyle();
    },
    applyMacros:function(template, attributes, macros) {
	if(!macros) macros =  Utils.tokenizeMacros(template);
	let infos = this.getFeatureInfoList();
	if(attributes) {
	    let attrs={};
	    infos.forEach(info=>{
		let attr=info.property;
		let value;
		if (typeof attributes[attr] == 'object' || typeof attributes[attr] == 'Object') {
                    let o = attributes[attr];
		    if(o)
			value = "" + o["value"];
		    else
			value = "";
		} else {
                    value = "" + attributes[attr];
		}
		value = info.format(value);
		let _attr = attr.toLowerCase();
		attrs[attr] = attrs[_attr] = value;			
	    });
	    template = macros.apply(attrs);
	}
	if(template.indexOf('${default}')>=0) {
	    let columns = [];
	    let labelMap = {};
	    let infoMap = {};
	    infos.forEach(info=>{
		infoMap[info.property] = info;
		if(info.showPopup()) {
		    columns.push(info.property);
		    labelMap[info.property] = info.getLabel();
		}
	    });
	    let formatter = (attr,value)=>{
		let info = infoMap[attr];
		if(info) {
		    value = info.format(value);
		}
		return value;
	    };
	    let labelGetter = attr=>{
		return labelMap[attr];
	    }
	    template = template.replace('${default}',MapUtils.makeDefaultFeatureText(attributes,columns,
										     formatter,labelGetter));
	}
	return template;
    },

    initColorTableDots:function(obj, dialog) {
	let _this  = this;
	let dots = dialog.find('.display-colortable-dot-item');
	dots.css({cursor:'pointer',title:'Click to show legend'});
	dots.addClass('ramadda-clickable');
	let select = jqid(_this.domId('enum_'+ Utils.makeId(obj.property)));
	select.find('option').each(function() {
	    if($(this).prop('selected')) {
		let value = $(this).prop('title');
		let dot = dialog.find('.display-colortable-dot-item[label="' +value+'"]');
		dot.addClass('display-colortable-dot-item-selected');
	    }
	});

	dots.click(function(event) {
	    let meta = event.metaKey || event.ctrlKey;
	    let label = $(this).attr('label');
	    let selected = $(this).hasClass('display-colortable-dot-item-selected');
	    let option = select.find('option[value="' +label+'"]');
	    if(!meta) {
		select.find('option').prop('selected',null);
		dots.removeClass('display-colortable-dot-item-selected');
	    }				
	    if(!selected) {
		$(this).addClass('display-colortable-dot-item-selected');
		option.prop('selected','selected');
	    } else {
		$(this).removeClass('display-colortable-dot-item-selected');
		option.prop('selected',null);
	    }
	    select.trigger('change');
	});
    },
    

    addFillImage:function(features) {
	features.forEach((feature,idx)=>{
	    if(!Utils.stringDefined(feature?.attributes.fillimage)) return;
	    let points =this.getFeaturePoints(feature);
	    if(!points || points.length!=5) return;
	    let tmp = [];
	    points.forEach(p=>{
		let pt = MapUtils.createPoint(p.x,p.y);
		pt = this.getMap().transformProjPoint(pt);
		tmp.push(pt.y,pt.x);
	    });
	    points = tmp;
	    let style = {strokeColor:'transparent',imageUrl:feature.attributes.fillimage};
	    let mapGlyph = new MapGlyph(this.display,GLYPH_IMAGE,{type:GLYPH_IMAGE},null,style,
					true,{geometryType:'OpenLayers.Geometry.LineString',points:points});
	    this.addChildGlyph(mapGlyph);
	    mapGlyph.isEphemeral = true;
	    mapGlyph.checkImage(feature,true);
	});
    },

    applyMapStyle:function(skipLegendUI) {
	let _this = this;
	//If its a map then set the style on the map features
	if(!this.mapLayer || !this.mapLoaded) return;

	let features= this.mapLayer.features;
	if(!features) return
	if(!skipLegendUI && this.canDoMapStyle()) {
	    this.makeFeatureFilters();
	}

	//Check for fillimage
	if(features.length>0 && features[0].attributes?.fillimage) {
	    this.addFillImage(features);
	}
	let style = this.style;
	let rules = this.getMapStyleRules();
	let useRules = [];
	if(rules) {
	    rules = rules.filter(rule=>{
		if(rule.type=='use') {
		    useRules.push(rule);
		    return false;
		}
		return Utils.stringDefined(rule.property);
	    });
	}

	let featureFilters = this.attrs.featureFilters ??{};
	let rangeFilters = [];
	let stringFilters =[];
	let enumFilters =[];	
	for(a in featureFilters) {
	    let filter= featureFilters[a];
	    if(!filter.property) {
		continue;
	    }
	    let info =this.getFeatureInfo(filter.property);
	    if(!info) {
		continue;
	    }
	    if(info && !info.showFilter()) continue;
	    if(filter.type=="string") {
		if(Utils.stringDefined(filter.stringValue)) stringFilters.push(filter);
	    } else if(filter.type=="enum") {
		if(filter.enumValues && filter.enumValues.length>0) enumFilters.push(filter);
	    } else {
		if(Utils.isDefined(filter.min) || Utils.isDefined(filter.max)) {
		    if(filter.min!=filter.minValue || filter.max!=filter.maxValue) {
			rangeFilters.push(filter);
		    }
		}
	    }
	}

	if(features) {
	    features.forEach((feature,idx)=>{
		feature.featureIndex = idx;
	    });
	}

	if(this.mapLabels) {
	    this.display.removeFeatures(this.mapLabels);
	    this.mapLabels = null;
	}


	//Apply the base style here
	
	let fillProperty = this.getProperty('map.property.fillColor');
	let fillOpacityProperty = this.getProperty('map.property.fillOpacity');
	let strokeProperty = this.getProperty('map.property.strokeColor');
	let labelProperty = this.getProperty('map.property.label');	

	this.mapLayer.style = style;
	if(features) {
	    features.forEach((f,idx)=>{
		let featureStyle = Utils.clone(style);
		if(f.attributes) {
		    if(fillProperty && Utils.stringDefined(f.attributes[fillProperty])) 
			featureStyle.fillColor=f.attributes[fillProperty];
		    if(fillOpacityProperty && Utils.stringDefined(f.attributes[fillOpacityProperty]))  
			featureStyle.fillOpacity=f.attributes[fillOpacityProperty];
		    if(labelProperty && Utils.stringDefined(f.attributes[labelProperty]))  {
			let label = f.attributes[labelProperty];
			if(true || label.indexOf('disproportionate')>=0) {
			if(label.length>20) {
			    let tmp = Utils.splitList(label.split(' '),4);
			    label = '';
			    tmp.forEach(l=>{
				label+=Utils.join(l,' ')+'\n'
			    });
			}
//			    label = "hello there\nhow are you\nI am fine"
			    featureStyle = Utils.clone(featureStyle,{
				strokeColor:'transparent',
				textBackgroundFillColor:featureStyle.fillColor,
				textBackgroundPadding:2,
				textBackgroundShape:'rectangle',				
				labelXOffset:4,
				labelYOffset:-8,
				labelAlign:'lt',
				fontSize:'6pt',
				label:label});
//			    featureStyle.fillColor = 'transparent'
			}
		    }

		}
		ImdvUtils.applyFeatureStyle(f, featureStyle);
		f.originalStyle = Utils.clone(style);			    
	    });
	}


	//Check for any rule based styles
	let attrs = features.length>0?features[0].attributes:{};
	let keys  = Object.keys(attrs);
	if(rules && rules.length>0) {
	    this.mapLayer.style = null;
	    this.mapLayer.styleMap = this.display.getMap().getVectorLayerStyleMap(this.mapLayer, style,rules);
	    features.forEach((f,idx)=>{
		f.style = null;
	    });
	} 

	let applyColors = (obj,attr,strings)=>{
	    if(!obj || !Utils.stringDefined(obj?.property))  return;
	    let prop =obj.property;
	    let min =Number.MAX_VALUE;
	    let max =Number.MIN_VALUE;
	    let ct =Utils.getColorTable(obj.colorTable,true);
	    let anyNumber =  false;
	    features.forEach((f,idx)=>{
		let value = this.getFeatureValue(f,prop);
		if(isNaN(+value)) {
		    if(!strings.includes(value)) {
			strings.push(value);
		    }
		} else {
		    anyNumber =  true;
		    min = Math.min(min,value);
		    max = Math.max(max,value);		    
		}
	    });
	    

	    if(!anyNumber) {
		obj.min =min = 0;
		obj.max = max= strings.length-1;
		obj.isEnumeration = true;
	    } else {
		obj.isEnumeration = false;
		obj.min = min;
		obj.max = max;		
	    }
	    strings = strings.sort((a,b)=>{
		return a.localeCompare(b);
	    });

	    let range = max-min;
	    features.forEach((f,idx)=>{
		let value = this.getFeatureValue(f,prop);
		if(!Utils.isDefined(value)) {
		    return;
		}
		let index;
		if(obj.isEnumeration) {
		    index = (strings.indexOf(value)%ct.length);
		} else {
		    value = +value;
		    let percent = (value-min)/range;
		    index = Math.max(0,Math.min(ct.length-1,Math.round(percent*ct.length)));
		}
		if(!f.style)
		    f.style = $.extend({},style);
		f.style[attr]=ct[index];
	    });
	};


	this.fillStrings = [];
	this.strokeStrings = [];		
	
	applyColors(this.attrs.fillColorBy,'fillColor',this.fillStrings);
	applyColors(this.attrs.strokeColorBy,'strokeColor',this.strokeStrings);	

	if(useRules.length>0) {
	    useRules.forEach(rule=>{
		let styles = [];
		let styleMap = {};
		Utils.split(rule.style,'\n',true,true).forEach(s=>{
		    let toks = [];
		    let index = s.indexOf(':');
		    if(index>=0) {
			toks.push(s.substring(0,index).trim());
			toks.push(s.substring(index+1).trim());			
		    }
		    if(toks.length!=2) return;
		    styleMap[toks[0].trim()] = toks[1].trim();
		    styles.push(toks[0].trim());
		});

		features.forEach((f,idx)=>{
		    if(!f.style) {
			f.style = $.extend({},style);
		    }
		    let value=this.getFeatureValue(f,rule.property);
		    if(!value) return;
		    styles.forEach(style=>{
			let v = styleMap[style];
			v = v.replace("${value}",value);
			if(v.startsWith('js:')) {
			    v = v.substring(3);
			    try {
				v = eval(v);
			    } catch(err) {
				console.error('Error evaluating style rule:' + v);
			    }
			}
			f.style[style] = v;
		    });
		});
	    });
	}	    

	//Add the map labels at the end after we call checkVisible
	let needToAddMapLabels = false;
	if(Utils.stringDefined(this.getMapLabelsTemplate())) {
	    let maxLength = parseInt(this.getProperty("map.label.maxlength",1000));
	    let maxLineLength = parseInt(this.getProperty("map.label.maxlinelength",1000));
	    needToAddMapLabels = true;
	    this.mapLabels = [];
	    let markerStyle = 	$.extend({},this.style);
	    markerStyle.pointRadius=0;
	    markerStyle.externalGraphic = null;
	    let template = this.getMapLabelsTemplate().replace(/\\n/g,'\n');
	    let macros = Utils.tokenizeMacros(template);
	    features.forEach((feature,idx)=>{
		let pt = feature.geometry.getCentroid(true); 
		let labelStyle = $.extend({},markerStyle);
		let label = this.applyMacros(template, feature.attributes,macros);
		if(label.length>maxLength) {
		    label = label.substring(0,maxLength)+'...';
		}
		if(maxLineLength>0) {
		    let tmp ='';
		    let cnt = 0;
		    let lastChar = '';
		    for(let i=0;i<label.length;i++ ) {
			let nextChar = label[i];
			if(cnt++>maxLineLength) {
			    if(lastChar!=' ' && nextChar!=' ') tmp+='-';
			    tmp+='\n';
			    cnt=0;
			}
			lastChar = nextChar;
			tmp+=nextChar;
		    }
		    label = tmp.trim();
		}
		labelStyle.label = label;
		let mapLabel = MapUtils.createVector(pt,null,labelStyle);
		mapLabel.point = pt;
		feature.mapLabel  = mapLabel;
		this.mapLabels.push(mapLabel);
	    });
	}

	this.visibleFeatures = 0;
	let redrawFeatures = false;
	let max =-1;
	features.forEach((f,idx)=>{
	    let visible = true;
	    rangeFilters.every(filter=>{
		let value=this.getFeatureValue(f,filter.property);
		if(Utils.isDefined(value)) {
		    max = Math.max(max,value);
		    visible = value>=filter.min && value<=filter.max;
		    //		    if(value>1000) console.log(filter.property,value,visible,filter.min,filter.max);
		}
		return visible;
	    });
	    if(visible) {
		stringFilters.every(filter=>{
		    let value=this.getFeatureValue(f,filter.property)??'';
		    if(Utils.isDefined(value)) {
			value= String(value).toLowerCase();
			visible = value.indexOf(filter.stringValue)>=0;
		    }
		    return visible;
		});
	    }
	    if(visible) {
		enumFilters.every(filter=>{
		    let value=this.getFeatureValue(f,filter.property)??'';
		    visible =filter.enumValues.includes(value);
		    return visible;
		});
	    }		

	    if(visible) this.visibleFeatures++;
	    f.isVisible  = visible;
	    MapUtils.setFeatureVisible(f,visible);
	    if(f.mapLabel) {
		redrawFeatures = true;
		f.mapLabel.isFiltered=!visible;
		MapUtils.setFeatureVisible(f.mapLabel,visible);
	    }
	});


	this.jq('filters_count').html('#' + this.visibleFeatures);


	if(this.attrs.fillColors) {
	    //	let ct = Utils.getColorTable('googlecharts',true);
	    let ct = Utils.getColorTable('d3_schemeCategory20',true);	
	    let cidx=0;
	    features.forEach((f,idx)=>{
		f.style = f.style??{};
		cidx++;
		if(cidx>=ct.length) cidx=0;
		f.style.fillColor=ct[cidx]
	    });
	}


	let indexToGroup = {
	};
	this.getStyleGroups().forEach(group=>{
	    group.indices.forEach(index=>{
		indexToGroup[index] = group;
	    });
	});
	features.forEach((f,idx)=>{
	    let group = indexToGroup[idx];
	    if(group) {
		f.style = $.extend({},f.style);
		$.extend(f.style,group.style)
	    }
	});

	this.mapLayer.features.forEach(f=>{
	    if(f.style && f.style.fillPattern && !Utils.stringDefined(f.style.fillColor)) {
		f.style.fillColor='transparent'
	    }
	});
	this.checkVisible();
	if(needToAddMapLabels) {
	    this.display.addFeatures(this.mapLabels);
	}	    
	ImdvUtils.scheduleRedraw(this.mapLayer);
	if(redrawFeatures) {
	    this.display.redraw();
	}
    },

    checkRings:function(points) {
	if(!this.features[0]){
	    console.log("range rings has no features");
	    return
	}

	let style = $.extend({},this.features[0].style);
	style.strokeColor='transparent';
	this.features[0].style = style;

	let pt = this.features[0].geometry.getCentroid();
	let center = this.display.getMap().transformProjPoint(pt)

	if(this.rings) this.display.removeFeatures(this.rings);
	let ringStyle = {};
	if(this.attrs.rangeRingStyle) {
	    //0,fillColor:red,strokeColor:blue
	    Utils.split(this.attrs.rangeRingStyle,'\n',true,true).forEach(line=>{
		let toks = line.split(',');
		ringStyle[toks[0]] = {};
		for(let i=1;i<toks.length;i++) {
		    let toks2 = toks[i].split(':');
		    ringStyle[toks[0]][toks2[0]] = toks2[1];
		}
	    });
	}
	let labels = [];
	if(Utils.stringDefined(this.attrs.rangeRingLabels))
	    labels = Utils.split(this.attrs.rangeRingLabels);
	this.rings = this.display.makeRangeRings(center,this.getRadii(),this.style,this.attrs.rangeRingAngle,ringStyle,labels);
	if(this.rings) {
	    this.rings.forEach(ring=>{
		ring.mapGlyph=this;
		MapUtils.setFeatureVisible(ring,this.isVisible());
	    });
	    this.display.addFeatures(this.rings);
	}		
    },
    applyStyle:function(style) {
	this.style = style;
	this.applyMapStyle();
	if(this.getMapServerLayer()) {
	    if(Utils.isDefined(style.opacity)) {
		this.getMapServerLayer().opacity = +style.opacity;
		this.getMapServerLayer().setVisibility(false);
		this.getMapServerLayer().setVisibility(true);
		ImdvUtils.scheduleRedraw(this.getMapServerLayer());
	    }
	}

	let tmpStyle= this.getStyle(true);
	this.features.forEach(feature=>{
	    if(feature.style && !feature.fixedStyle) {
		$.extend(feature.style,tmpStyle);
	    }
	});	    
	if(this.isFixed()) {
	    this.addFixed();
	}
	if(this.isRings()){
	    this.checkRings();
	}

	this.display.featureChanged(true);
    },
    
    vertexDragged:function(feature,vertex,pixel) {
	this.display.checkSelected(this);
    },
    move:function(dx,dy) {
	let pts = this.attrs.originalPoints;
	if(pts) {
	    //Run through the points, txfm to proj, move, txfm to latlon
	    for(let i=0;i<pts.length;i+=2) {
		let pt = MapUtils.createPoint(pts[i+1], pts[i]);
		pt = this.getMap().transformLLPoint(pt);
		pt.x+=dx; pt.y+=dy;
		pt = this.getMap().transformProjPoint(pt);
		pts[i] = pt.y;
		pts[i+1] = pt.x;
	    }
	}
	if(this.getUseEntryLocation()) {
	    this.setUseEntryLocation(false);
	}
	if(this.rings){
	    this.rings.forEach(feature=>{
		feature.geometry.move(dx,dy);
		feature.layer.drawFeature(feature);
	    });
	}

	this.features.forEach(feature=>{
	    feature.geometry.move(dx,dy);
	    feature.layer.drawFeature(feature);
	});
	if(this.image) {
	    this.image.extent.left+=dx;
	    this.image.extent.right+=dx;	    
	    this.image.extent.top+=dy;
	    this.image.extent.bottom+=dy;
	    this.image.moveTo(this.image.extent,true,true);
	}

	this.display.checkSelected(this);
    },
    removeImage:function() {
	if(this.image) {
	    this.display.getMap().removeLayer(this.image);
	    this.image=null;
	}
    },
    getMap:function() {
	return this.display.getMap();
    },
    setRotation:function(angle) {
	this.style.rotation =angle;
	if(!this.image) return;
	if(this.image && this.image.imageHook) {
	    this.image.imageHook();
	}
	let feature = this.features[0];
	if(!feature) return
	let points =this.getFeaturePoints(feature);
	if(!points) {
	    console.log("MapGlyph.setRotation: no points");
	    return;
	}
	let ext = this.image.extent;
	let c  =ext.getCenterPixel();
	[[ext.left,ext.top],[ext.right,ext.top],[ext.right,ext.bottom],[ext.left,ext.bottom],[ext.left,ext.top]].forEach((tuple,idx)=>{
	    let x = tuple[0];
	    let y = tuple[1];		
	    let r = Utils.rotate(c.x, c.y, x, y, this.style.rotation,true);
	    points[idx].x=r.x;
	    points[idx].y=r.y;	    
	});
	this.display.redraw(this);
    },
    getFeaturePoints:function(feature) {
	if(!feature || !feature.geometry) return null;
	let components = feature.geometry.components;
	if(components[0]&&components[0].components) return components[0].components;
	return components;
    },
    checkImage:function(feature,applyRotationFromFeature) {
	if(this.image) {
	    if(Utils.isDefined(this.image.opacity)) {
		this.style.imageOpacity=this.image.opacity;
	    }
	}
	if(!this.style.imageUrl) {
	    console.log('MapGlyph.checkImage: no image url');
	    return;
	}
	feature = feature??this.features[0];
	if(!feature) {
	    console.log('MapGlyph.checkImage: no feature');
	    return

	}
	let points =this.getFeaturePoints(feature);
	if(!points) {
	    console.log('MapGlyph.checkImage: no points');
	    return
	}
	let bounds = MapUtils.createBoundsFromPoints(points);
	let rotation = Utils.getRotation(points);
	let c  =bounds.getCenterPixel();	
	let rotatedPoints=points.map(p=>{
	    return Utils.rotate(c.x, c.y, p.x,p.y, rotation.angle);
	});
	if(applyRotationFromFeature) this.style.rotation = rotation.angle-180
	bounds= MapUtils.createBoundsFromPoints(rotatedPoints);
	if(this.image) {
	    this.image.extent = bounds;
	    this.image.moveTo(bounds,true,true);
	} else {
	    bounds = this.getMap().transformProjBounds(bounds);
	    this.image=  this.getMap().addImageLayer(this.getName(),this.getName(),"",this.style.imageUrl,true,  bounds.top,bounds.left,bounds.bottom,bounds.right);
	    this.initImageLayer(this.image);
	    if(Utils.isDefined(this.style.imageOpacity)) {
		this.image.setOpacity(this.style.imageOpacity);
	    }
	}
	this.setRotation(this.style.rotation);
    },
    initImageLayer:function(image) {
	this.image = image;
	image.imageHook = (image)=> {
	    let transform='';
	    if(Utils.stringDefined(this.style.transform)) 
		transform = this.style.transform;
	    if(Utils.isDefined(this.style.rotation) && this.style.rotation!=0)
		transform += ' rotate(' + this.style.rotation +'deg)';
	    if(!Utils.stringDefined(transform))  transform=null;
	    let childNodes = this.image.div.childNodes;
	    for(let i = 0, len = childNodes.length; i < len; ++i) {
                let element = childNodes[i].firstChild || childNodes[i];
                let lastChild = childNodes[i].lastChild;
                if (lastChild && lastChild.nodeName.toLowerCase() === "iframe") {
		    element = lastChild.parentNode;
                }
		if(element.style)
		    element.style.transform=transform;
		//                    OpenLayers.Util.modifyDOMElement(element, null, null, null, null, null, null, null);
	    }
	}
    },	
    getDisplayAttrs: function() {
	return this.attrs.displayAttrs;
    },
    applyDisplayAttrs: function(attrs) {
	if(this.displayInfo && this.displayInfo.display) {
	    this.displayInfo.display.deleteDisplay();
	    this.displayInfo.display = null;
	    jqid(this.displayInfo.divId).remove();
	    jqid(this.displayInfo.bottomDivId).remove();			
	}
	this.addData(attrs,false);
    },
    isVisible: function() {
	return this.attrs.visible??true;
    },
    setShowMarkerWhenNotVisible:function(v) {
	this.attrs.showMarkerWhenNotVisible = v;
	return this;
    },
    getZoomOnChange:function() {
	return this.attrs.zoomOnChange;
    },
    setZoomOnChange:function(v) {
	this.attrs.zoomOnChange = v;
	return this;
    },
    getMapPointsRange:function() {
	return this.attrs.mapPointsRange;
    },
    setMapPointsRange:function(v) {
	this.attrs.mapPointsRange  = v;
	return this;
    },    
    getMapLabelsTemplate:function() {
	return this.attrs.mapLabelsTemplate;
    },
    setMapLabelsTemplate:function(v) {
	this.attrs.mapLabelsTemplate = v;
	return this;
    },	   

    getShowMarkerWhenNotVisible:function() {
	return this.attrs.showMarkerWhenNotVisible;
    },
    getVisibleLevelRange:function() {
	return this.attrs.visibleLevelRange;
    },

    setVisible:function(visible,callCheck) {
	this.attrs.visible = visible;
	if(this.children) {
	    this.children.forEach(child=>{
		child.setVisible(visible, callCheck);
	    });
	}


	if(this.rings) {
	    this.rings.forEach(feature=>{
		MapUtils.setFeatureVisible(feature,visible);
	    });
	}

	if(callCheck)
	    this.checkVisible();
	this.checkMapLayer();
	let legend = this.getLegendDiv();
	if(this.getVisible()) 
	    legend.removeClass('imdv-legend-label-invisible');
	else
	    legend.addClass('imdv-legend-label-invisible');			    
    },
    getVisible:function() {
	if(!Utils.isDefined(this.attrs.visible)) this.attrs.visible = true;
	return this.attrs.visible;
    },
    setVisibleLevelRange:function(min,max) {
	let range = this.getVisibleLevelRange();
	let oldMin = range?.min;
	let oldMax = range?.max;	
	if(min===oldMin && max===oldMax) return;
	if(min=="") min = null;
	if(max=="") max = null;	
	this.attrs.visibleLevelRange = {min:min,max:max};
	this.checkVisible();
    },    

    checkVisible: function() {
	let showMarker = this.getShowMarkerWhenNotVisible();
	let range = this.getVisibleLevelRange()??{};
	let displayRange = this.display.getMapProperty('visibleLevelRange');
	if(!range || (displayRange && !Utils.stringDefined(range.min) && !Utils.stringDefined(range.max))) {
	    range = displayRange;
	    showMarker  = this.display.getMapProperty('showMarkerWhenNotVisible');
	}
	let visible=true;
	let level = this.display.getCurrentLevel();
	let min = Utils.stringDefined(range.min)?+range.min:-1;
	let max = Utils.stringDefined(range.max)?+range.max:10000;
	visible =  this.getVisible() && (level>=min && level<=max);

	if(this.getVisible() && showMarker && !visible && !this.showMarkerMarker) {
	    let featuresToUse = this.features;
	    if(!featuresToUse || featuresToUse.length==0) {
		featuresToUse = this.mapLayer?.features
	    }		

	    let bounds = this.display.getMap().getFeaturesBounds(featuresToUse,true);
	    if(bounds) {
		let center = MapUtils.getCenter(bounds);
		this.showMarkerMarker = this.display.getMap().createMarker("", center, this.getIcon(), "",
									   null,null,16,null,null,{});
		this.display.addFeatures([this.showMarkerMarker]);
		this.showMarkerMarker.mapGlyph = this;
	    }
	}

	let setVis = (feature,vis)=>{
	    if(!Utils.isDefined(vis))  vis=visible;
	    MapUtils.setFeatureVisible(feature, vis);
	};

	if(this.features) {
	    this.features.forEach(f=>{setVis(f);});
	}

	if(this.showMarkerMarker) {
	    if(!this.getVisible() || visible) {
		this.display.removeFeatures([this.showMarkerMarker]);
		this.showMarkerMarker = null;
	    }
	}

	if(this.isFixed()) {
	    if(visible)
		jqid(this.getFixedId()).show();
	    else
		jqid(this.getFixedId()).hide();
	}
	if(this.getMapLayer() && !this.imageLayers) {
	    this.getMapLayer().setVisibility(visible);
	}
	if(this.imageLayers) {
	    this.imageLayers.forEach(obj=>{
		let imageVisible = visible && this.isImageLayerVisible(obj);
		if(obj.layer)
		    obj.layer.setVisibility(imageVisible);
	    })
	}
	if(this.getMapServerLayer()) {
	    this.getMapServerLayer().setVisibility(visible);
	}	

	if(this.selectDots && this.selectDots.length>0) {
	    this.selectDots.forEach(dot=>{
		MapUtils.setFeatureVisible(dot,visible);
	    });
	    ImdvUtils.scheduleRedraw(this.display.selectionLayer);
	}
	if(this.image) {
	    this.image.setVisibility(visible);
	}	
	if(this.displayInfo && this.displayInfo.display) {
	    this.displayInfo.display.setVisible(visible);
	}

	if(this.mapLabels && this.mapLoaded) {
	    if(!visible) {
		this.mapLabels.forEach(mapLabel=>{setVis(mapLabel,false);});
	    } else if(Utils.stringDefined(this.getMapPointsRange())) {
		if(level<parseInt(this.getMapPointsRange())) {
		    visible=false;
		    this.mapLabels.forEach(mapLabel=>{setVis(mapLabel,false);});
		}
	    } else {
		//If the label wasn't filtered then turn them all on
		this.mapLabels.forEach(mapLabel=>{
		    if(!mapLabel.isFiltered)
			setVis(mapLabel,true);
		});
		let args ={};
		args.fontSize = this.style.fontSize??'12px';
		if(this.getProperty('map.label.padding'))
		    args.padding = +this.getProperty('map.label.padding');
		if(this.getProperty('map.label.pixelsperline'))
		    args.pixelsPerLine = +this.getProperty('map.label.pixelsperline');
		if(this.getProperty('map.label.pixelspercharacter'))
		    args.pixelsPerCharacter = +this.getProperty('map.label.pixelspercharacter');
		MapUtils.gridFilter(this.getMap(), this.mapLabels,args);
	    }
	}

	if(this.children) {
	    this.children.forEach(child=>{child.checkVisible();});
	}
	ImdvUtils.scheduleRedraw(this.display.myLayer);
    },    
    setImageLayerVisible:function(obj,visible) {
	this.isImageLayerVisible(obj,true,visible);
	if(obj.layer) obj.layer.setVisibility(visible);
    },
    isImageLayerVisible:function(obj,create,visible) {
	if(!this.attrs.imageLayerState) {
	    this.attrs.imageLayerState = {};
	}
	let state=this.attrs.imageLayerState[obj.id];
	if(!state) {
	    state = this.attrs.imageLayerState[obj.id] = {
		visible:true
	    }
	}
	if(Utils.isDefined(visible)) state.visible=visible;
	if(obj.id=='main') {
	    if(this.getMapLayer()) this.getMapLayer().setVisibility(visible);
	    return
	}
	if(state.visible && create)  {
	    obj.layer =this.getMap().addImageLayer(obj.name,obj.name,'',obj.url,true,
						   obj.north,obj.west,obj.south,obj.east);
	    
	    if(Utils.isDefined(this.style.imageOpacity))
		obj.layer.setOpacity(this.style.imageOpacity);
	    this.display.makeLegend();
	}
	return state.visible;
    },
    isShape:function() {
	if(this.getType()==GLYPH_LABEL) {
	    if(!Utils.stringDefined(this.style.externalGraphic)) return true;
	    if(this.style.externalGraphic && this.style.externalGraphic.endsWith("blank.gif")) return true;
	    if(this.style.pointRadius==0) return true;
	}
	return GLYPH_TYPES_SHAPES.includes(this.getType());
    },
    isData:function() {
	return this.type == GLYPH_DATA;
    },
    isFixed:function() {
	return this.type == GLYPH_FIXED;
    },    
    addFixed: function() {
	let style = this.style;
	let line = "solid";
	if(style.strokeDashstyle) {
	    if(['dot','dashdot'].includes(style.strokeDashstyle)) {
		line = "dotted";
	    } else  if(style.strokeDashstyle.indexOf("dash")>=0) {
		line = "dashed";
	    }
	}
	let css = HU.css('padding','5px');
	let color = Utils.stringDefined(style.borderColor)?style.borderColor:"#ccc";
	css+= HU.css('border' , style.borderWidth+"px" + " " + line+ " " +color);
	if(Utils.stringDefined(style.fillColor)) {
	    css+=HU.css("background",style.fillColor);
	}
	css+=HU.css('color',style.fontColor);
	if(Utils.stringDefined(style.fontSize)) {
	    css+=HU.css('font-size',style.fontSize);
	}

	['right','left','bottom','top'].forEach(d=>{
	    if(Utils.stringDefined(style[d])) css+=HU.css(d,HU.getDimension(style[d]));
	});
	let id = this.getFixedId();
	jqid(id).remove();
	let text = this.style.text??"";
	let html = HU.div(['id',id,CLASS,"ramadda-imdv-fixed",'style',css],"");
	this.display.jq(ID_MAP_CONTAINER).append(html);
	let toggleLabel = null;
	if(text.startsWith("toggle:")) {
	    text = text.trim();
	    let regexp = /toggle:(.*)\n/;
	    let match = text.match(regexp);
	    if(match) {
		toggleLabel=match[1];
		text = text.replace(regexp,"").trim();
	    }
	} 

	let initFixed = () =>{
	    let _this=this;
	    let height  = jqid(id).height();
	    jqid(id).draggable({
		containment:this.display.domId(ID_MAP),
		start: function (event, ui) {
                    $(this).css({
			height:(height+10)+'px',
                        right: "auto",
                        top: "auto"
                    });
		},
		stop:function() {
		    let div= $(this);
		    let top = div.position().top;
		    let left = div.position().left;		    
		    let bottom = top+div.height();
		    let right = left+div.width();		    
		    let pw = div.parent().width();
		    let ph = div.parent().height();		    
		    let pos =  _this.style;
		    ['top','bottom','left','right'].forEach(p=>{
			pos[p]='';
		    });
		    let set = (which,v) =>{
			v =  Math.max(0,(parseInt(v)))+'px';
			pos[which] =v;
//			div.css(pos,v);
		    }
		    if(top<ph-bottom) set('top',top);
		    else set('bottom',(ph-bottom));
		    if(left<pw-right) set('left',left);
		    else set('right',pw-right);
		},
		revert: false
	    });
	}
	if(text.startsWith("<wiki>")) {
	    this.display.wikify(text,null,wiki=>{
		if(toggleLabel)
		    wiki = HU.toggleBlock(toggleLabel+SPACE2, wiki,false);
		wiki = HU.div(['style','max-height:300px;overflow-y:auto;'],wiki);
		jqid(id).html(wiki);
		initFixed();
	    });
	} else {
	    text = this.convertText(text);
	    text = text.replace(/\n/g,"<br>");
	    if(toggleLabel)
		text = HU.toggleBlock(toggleLabel+SPACE2, text,false);
	    jqid(id).html(text);
	    initFixed();
	}
    },

    addData:function(displayAttrs,andZoom) {
	displayAttrs = displayAttrs??{};
	displayAttrs.doInitCenter = andZoom??false;
	this.attrs.displayAttrs = displayAttrs;
	let entryId = this.getEntryId();
	let pointDataUrl = displayAttrs.pointDataUrl ||Ramadda.getUrl("/entry/data?max=50000&entryid=" + entryId);
	let pointData = new PointData(this.attrs.name,  null,null,
				      pointDataUrl,
				      {entryId:entryId});
	
	let divId   = HU.getUniqueId("display_");
	let bottomDivId   = HU.getUniqueId("displaybottom_");	    
	this.display.jq(ID_HEADER1).append(HU.div([ID,divId]));
	this.display.jq(ID_BOTTOM).append(HU.div([ID,bottomDivId]));	    
	let attrs = {"externalMap":this.display.getMap(),
		     "isContained":true,
		     "showRecordSelection":true,
		     "showInnerContents":false,
		     "entryIcon":this.attrs.icon,
		     "title":this.attrs.name,
		     "max":"5000",
		     "thisEntryType":this.attrs.entryType,
		     "entryId":entryId,
		     "divid":divId,
		     "bottomDiv":bottomDivId,			 
		     "data":pointData,
		     "fileUrl":Ramadda.getUrl("/entry/get?entryid=" + entryId+"&fileinline=true")};
	$.extend(attrs,displayAttrs);
	attrs = $.extend({},attrs);
	attrs.name=this.getName();
	let display = this.display.getDisplayManager().createDisplay("map",attrs);
	//Not sure why we do this since we can't integrate charts with map record selection
	//	display.setProperty("showRecordSelection",false);

	display.errorMessageHandler = (display,msg) =>{
	    this.display.setErrorMessage(msg,5000);
	};
	this.displayInfo =   {
	    display:display,
	    divId:divId,
	    bottomDivId: bottomDivId
	};
    },
    getDecoration:function(small) {
	let type = this.getType();
	let style = this.style??{};
	let css= ['display','inline-block'];
	let dim = small?'10px':'25px';
	css.push('width',small?'10px':'50px');
	let line = "solid";
	if(style.strokeWidth>0) {
	    if(style.strokeDashstyle) {
		if(['dot','dashdot'].includes(style.strokeDashstyle)) {
		    line = "dotted";
		} else  if(style.strokeDashstyle.indexOf("dash")>=0) {
		    line = "dashed";
		}
	    }
	    css.push('border',(small?Math.min(+style.strokeWidth,1):style.strokeWidth)+"px " + line +" " + style.strokeColor);
	}

	if(style.imageUrl) {
	    if(!small) 
		return HU.toggleBlock(style.imageUrl, HU.image(style.imageUrl,["width","200px"]));
	} else if(type==GLYPH_LABEL) {
	    if(!small)
		return style.label.replace(/\n/g,"<br>");
	} else if(type==GLYPH_MARKER) {
	    if(!small)
		return HU.image(style.externalGraphic,['width','16px']);
	} else if(type==GLYPH_BOX) {
	    if(Utils.stringDefined(style.fillColor)) {
		css.push('background',style.fillColor);
	    }
	    css.push('height',dim);
	    return HU.div(['style',HU.css(css)]);
	} else if(type==GLYPH_HEXAGON) {
	    css=[];
	    if(Utils.stringDefined(style.fillColor)) {
		css.push('background',style.fillColor);
	    }
	    if(Utils.stringDefined(style.strokeColor)) {
		css.push('color',style.strokeColor);
	    }		
	    css.push('font-size',small?'16px':'32px','vertical-align','center');
	    return HU.span(['style',HU.css(css)],"&#x2B22;");
	} else if(type==GLYPH_CIRCLE || type==GLYPH_POINT) {
	    if(Utils.stringDefined(style.fillColor)) {
		css.push('background',style.fillColor);
	    }
	    if(type==GLYPH_POINT) {
		css.push('margin-top','10px','height','10px','width','10px');
	    } else {
		css.push('height',dim);
		css.push('width',dim);
	    }		    
	    return HU.div(['class','ramadda-dot', 'style',HU.css(css)]);
	} else if(type==GLYPH_LINE ||
		  type==GLYPH_POLYLINE ||
		  type==GLYPH_POLYGON ||
		  type==GLYPH_FREEHAND_CLOSED ||
		  type==GLYPH_MAP
		  || type==GLYPH_ROUTE
		  ||  type==GLYPH_FREEHAND) {
	    if(this.isClosed()) {
		if(type==GLYPH_FREEHAND_CLOSED)
		    css.push('border-radius','10px');
		css.push('height','10px');
		//		css.push('margin-top','10px','margin-bottom','10px');
		css.push('background',style.fillColor);
	    } else {
		css.push('margin-bottom','4px','border-bottom', style.borderWidth+"px" + " " + line+ " " +style.strokeColor);
	    }
	    return HU.div(['style',HU.css(css)]);
	}
	return HU.div(['style',HU.css('display','inline-block','border','1px solid transparent','width',small?'10px':'50px')]);
    },
    isClosed: function() {
	return GLYPH_TYPES_CLOSED.includes(this.type);
    },
    isOpenLine:function() {
	return GLYPH_TYPES_LINES_OPEN.includes(this.type);
    },
    isStraightLine:function() {
	return GLYPH_TYPES_LINES_STRAIGHT.includes(this.type);
    },
    addEntries: function(andZoom) {
	let entryId = this.getEntryId();
        let entry =  new Entry({
            id: entryId,
        });
	if(this.features)
	    this.display.removeFeatures(this.features);
	this.features =[];
        let callback = (entries)=>{
	    this.entries = entries;
	    entries.forEach((e,idx)=>{
		if(!e.hasLocation()) return;
		let  pt = MapUtils.createPoint(e.getLongitude(),e.getLatitude());
		pt = this.display.getMap().transformLLPoint(pt);
		let style = $.extend({},this.style);
		style.externalGraphic = e.getIconUrl();
		style.strokeWidth=1;
		style.strokeColor="transparent";
		/*
		  let bgstyle = $.extend({},style);
		  bgstyle = $.extend(bgstyle,{externalGraphic:Ramadda.getUrl("/images/white.png")});
		  bgstyle.label = null;
		  let bgpt = MapUtils.createPoint(pt.x,pt.y);
		  let bg = MapUtils.createVector(bgpt,null,bgstyle);
		  bg.noSelect = true;
		  //		bg.mapGlyph=this;
		  //		bg.entryId = e.getId();
		  this.features.push(bg);
		*/
		if(style.showLabels) {
		    let label  =e.getName();
		    let toks = Utils.split(label," ",true,true);
		    if(toks.length>1) {
			label = "";
			Utils.splitList(toks,3).forEach(l=>{
			    label += Utils.join(l," ");
			    label+="\n";
			})
			label = label.trim();
		    }
		    style.label=label;
		} else {
		    style.label=null;
		}
		let marker = MapUtils.createVector(pt,null,style);


		let attrs = {name:e.getName(),
			     entryglyphs:e.mapglyphs,
			     entryId:e.getId()
			    };
		let mapGlyph = new MapGlyph(this.display,GLYPH_MARKER, attrs, marker,style);
		marker.mapGlyph = this;
		marker.entryId = e.getId();
		if(this.getShowMultiData()) {
		    mapGlyph.applyEntryGlyphs();
		}
		this.features.push(marker);
	    });
	    this.display.addFeatures(this.features);
	    this.checkVisible();
	    if(andZoom)
		this.panMapTo();
	    this.showMultiEntries();
	};
	entry.getChildrenEntries(callback);
    },
    isSelected:function() {
	return this.selected;
    },
    getSelected:function(selected) {
	if(this.isSelected()) {
	    selected.push(this);
	}
	if(this.children) {
	    this.children.forEach(child=>{child.getSelected(selected);});
	}
    },

    select:function(maxPoints,dontRedraw) {
	if(!Utils.isDefined(maxPoints)) maxPoints = 20;
	if(this.isSelected()) {
	    return;
	}
	this.selected = true;
	this.selectDots = [];
	let pointCount = 0;
	let mapLayer = this.getMapLayer();
	if(mapLayer && mapLayer.features) {
	    let style={
		strokeColor:'#000',
		strokeWidth:2,
		fillColor:'transparent'
	    };
	    mapLayer.features.forEach(f=>{
		f.originalStyle = f.style;
		f.style = style;
	    });
	    ImdvUtils.scheduleRedraw(this.mapLayer);
	}	    

	let image = this.getImage();
	if(image) {
	    let ext = image.extent;
	    let c  =ext.getCenterPixel();
	    [[ext.left,ext.top],[ext.right,ext.top],[ext.left,ext.bottom],[ext.right,ext.bottom]].forEach(tuple=>{
		let x = tuple[0];
		let y = tuple[1];		
		//Rotate the dots
		if(this.style.rotation) {
		    let r = Utils.rotate(c.x, c.y, x, y, this.style.rotation,true);
		    x = r.x; y=r.y;
		}
                let pt = MapUtils.createPoint(x,y);
		let dot = MapUtils.createVector(pt,null,this.display.DOT_STYLE);	
		this.selectDots.push(dot);
	    });
	} else {
	    pointCount+=this.display.selectFeatures(this,this.getFeatures(),maxPoints);
	}
	this.display.selectionLayer.addFeatures(this.selectDots,{silent:true});
	if(this.children) {
	    this.children.forEach(child=>{
		pointCount+=child.select(maxPoints, dontRedraw);
	    });
	}
	return pointCount;
    },
    unselect:function() {
	if(!this.isSelected()) return;
	this.selected = false;
	if(this.selectDots) {
	    this.display.selectionLayer.removeFeatures(this.selectDots);
	    this.selectDots= null;
	}

	if(this.mapLayer && this.mapLayer.features) {
	    this.applyMapStyle(true);
	}

	if(this.children) {
	    this.children.forEach(child=>{
		child.unselect();
	    });
	}	

    },
    
    doRemove:function() {
	if(this.isFixed()) {
	    jqid(this.getFixedId()).remove();
	}
	if(this.mapLabels) {
	    this.display.removeFeatures(this.mapLabels);
	}
	if(this.features) {
	    this.display.removeFeatures(this.features);
	}
	if(this.rings) {
	    this.display.removeFeatures(this.rings);
	}
	if(this.showMarkerMarker) {
	    this.display.removeFeatures([this.showMarkerMarker]);
	    this.showMarkerMarker = null;
	}
	if(this.selectDots) {
	    this.display.selectionLayer.removeFeatures(this.selectDots);
	    this.selectDots = null;
	}

	if(this.getMapLayer()) {
	    this.display.getMap().removeLayer(this.getMapLayer());
	    this.image =null;
	    this.setMapLayer(null);
	}

	if(this.getImage()) {
	    this.display.getMap().removeLayer(this.getImage());
	    this.image =null;
	}


	if(this.imageLayers) {
	    this.imageLayers.forEach(obj=>{
		if(obj.layer)
		    this.display.getMap().removeLayer(obj.layer);
	    })
	    this.imageLayers = [];
	}

	if(this.getMapServerLayer()) {
	    this.display.getMap().removeLayer(this.getMapServerLayer());
	    this.setMapServerLayer(null);
	}
	if(this.displayInfo) {
	    jqid(this.displayInfo.divId).remove();
	    jqid(this.displayInfo.bottomDivId).remove();			
	    if(this.displayInfo.display) {
		this.displayInfo.display.deleteDisplay();
	    }
	}

	this.setParentGlyph(null);

	if(this.children) {
	    let tmp = [...this.children];
	    tmp.forEach(child=>{
		child.doRemove();
	    });
	}
    }
}

