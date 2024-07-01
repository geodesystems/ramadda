
var debugDataIcons = false;

var DATAICON_PROPERTIES = ['externalGraphic','pointRadius','label'];
var DEFAULT_DATAICON_PROPS = 'font:50px sans-serif,lineWidth:5,requiredField:${_field},borderColor:#000,fill:#eee';
var DEFAULT_DATAICONS = 'label,pos:nw,dx:80,dy:-ch+20,label:${${_field} decimals=1 suffix=" ${unit}"}\nimage,pos:nw,dx:10,dy:10-ch,width:60,height:60,url:${icon}';
//DEFAULT_DATAICONS='label,pos:nw,dx:10,dy:-ch+20,label:${${_field} decimals=1 suffix=" ${unit}" prefix="${fieldName}"}\nimage,pos:nw,dx:10,dy:10-ch,width:60,height:60,url:${icon}'


var DEFAULT_DATAICON_FIELD='atmp|temp.*|.*temp';
var DEFAULT_DATAICON_FIELDS=DEFAULT_DATAICON_FIELD+',label=Temperature,unit=C\ndewpoint,label=Dewpoint,unit=C\n.*rh|relativehumidity,label=Relative Humidity,unit=%';


var LINETYPE_STRAIGHT='straight';
var LINETYPE_GREATCIRCLE='greatcircle';
var LINETYPE_CURVE='curve';
var LINETYPE_STEPPED='stepped';        

var ID_INMAP_LABEL='inmaplabel';

var ID_ADDDOTS = 'adddots';
var ID_LINETYPE = 'linetype';
var ID_SHOWDATAICONS = 'showdataicons';

var ID_DATAICON_USEENTRY = 'dataicon_useentry';
var ID_DATAICON_MARKERS = 'dataicon_markers';
var ID_DATAICON_LABEL='dataicon_label';
var ID_DATAICON_FIELDS='dataicon_fields';
var ID_DATAICON_INIT_FIELD='dataicon_init_field';
var ID_DATAICON_SELECTED_FIELD='dataicon_selected_field';
var ID_DATAICON_WIDTH='dataicon_width';
var ID_DATAICON_HEIGHT='dataicon_height';
var ID_DATAICON_SIZE='dataicon_size';
var ID_DATAICON_PROPS='dataicon_props';

//attr flags
var ID_DATAICON_SHOWING = 'dataIconShowing';
var ID_DATAICON_ORIGINAL = 'dataIconOriginal';
var ID_LEGEND_TEXT = 'legendText';

function MapGlyph(display,type,attrs,feature,style,fromJson,json) {
    if(!type) {
	console.log("no type given for MapGlyph");
	console.trace();
	return
    }

    this.display = display;
    this.type = type;


    style = style??{};
    if(style.mapOptions) {
	delete style.mapOptions;
    }
    this.transientProperties = {};

    let glyphType = this.getGlyphType();
    if(attrs.routeProvider)
	this.name = "Route: " + attrs.routeProvider +" - " + attrs.routeType;
    else 
	this.name = attrs.name || glyphType.getName() || type;
    let mapGlyphs = attrs.mapglyphs;
    if(attrs.mapglyphs) delete attrs.mapglyphs;
    if(mapGlyphs) {
//	mapGlyphs = mapGlyphs.replace(/\\n/g,"\n");
	this.putTransientProperty("mapglyphs", mapGlyphs);
    }
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
	    this.loadEntry();
	}

	//And call getBounds so the bounds object gets cached for later use on reload
	this.getBounds();
    }


    if(this.isRings()) {
	this.checkRings();
    }
    this.checkDataIconMenu();
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
	    let items =   ['filter.show=true','label=','filter.first=true','type=enum','filter.top=true']
	    if(info.isNumeric()) {
		items.push('format.decimals=0',
			   'filter.min=0',
			   'filter.max=100',
			   'filter.animate=true',
			   'filter.animate.step=1',
			   'filter.animate.sleep=100',
			   'filter.live=true');

	    }
	    if(info.isEnumeration()) {
		items.push('label.feature_value=');
	    }
	    items.push('colortable.select=true');

	    items.forEach(item=>{
		let label = item.replace('=.*','');
		html+=HU.div([ATTR_STYLE,'margin-left:10px;', ATTR_CLASS,HU.classes('ramadda-menu-item',CLASS_CLICKABLE),'item',item],item);
	    });

	    html = HU.div([ATTR_STYLE,'margin-left:10px;margin-right:10px;'],html);
	    let dialog =  HU.makeDialog({content:html, anchor:$(this)});
	    dialog.find('.' + CLASS_CLICKABLE).click(function() {
		dialog.remove();
		let item = $(this).attr('item');
		let line = info.id+'.' + item+'\n';
		let textComp = GuiUtils.getDomObject(target);
		if(textComp) {
		    WikiUtil.insertAtCursor('', textComp.obj, line);
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
	if(Utils.stringDefined(this.attrs.icon)) {
	    return this.attrs.icon;
	}
	if(this.attrs[ID_DATAICON_ORIGINAL] && Utils.stringDefined(this.attrs[ID_DATAICON_ORIGINAL].externalGraphic)) {
	    return this.attrs[ID_DATAICON_ORIGINAL].externalGraphic;
	}
	return this.style.externalGraphic ??this.getGlyphType().getIcon();
    },
    getGlyphType:function() {
	return  this.display.getGlyphType(this.getType());
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

	//Set the location
	obj.points = this.getPoints(obj);

	if(this.isMultiEntry() && this.children) {
	    let locs = attrs.childrenLocations??{};
	    attrs.childrenLocations=locs;
	    this.children.forEach(child=>{
		if(child.overrideLocation) {
		    locs[child.attrs.entryId] = child.overrideLocation;
		}
	    });
	}
	if(!this.dontSaveChildren && this.haveChildren()) {
	    let childrenJson=[];
	    this.getChildren().forEach(child=>{
		if(child.isEphemeral) return;
		childrenJson.push(child.makeJson());
	    });
	    obj.children = childrenJson;
	}
	return obj;
    },	

    checkLayersAnimationButton:function() {
	if(this.attrs[PROP_LAYERS_ANIMATION_ON]) {
	    this.jq(PROP_LAYERS_ANIMATION_PLAY).html(HU.getIconImage(icon_stop));
	} else {
	    this.jq(PROP_LAYERS_ANIMATION_PLAY).html(HU.getIconImage(icon_play));
	}
    },
    checkLayersAnimation:function(skipCall) {
	if(this.animationTimeout) clearTimeout(this.animationTimeout);
	this.animationTimeout = null;
	if(!this.getProperty(PROP_LAYERS_ANIMATION_SHOW)) {
	    this.attrs[PROP_LAYERS_ANIMATION_ON] = false;
	    return;
	}
	this.checkLayersAnimationButton();
	if(!this.getVisible()) {
	    return;
	}
	if(skipCall) return;
	if(this.attrs[PROP_LAYERS_ANIMATION_ON]) {
	    let pause = this.getProperty(PROP_LAYERS_ANIMATION_DELAY,1000);
	    let stepAnimation = () =>{
		this.toggleLayersVisibility();
		this.checkLayersAnimation(true);
		this.animationTimeout = setTimeout(stepAnimation,pause);
		return;

//		this.jq(PROP_LAYERS_ANIMATION_PLAY).html(HU.getIconImage(icon_stop,null,[ATTR_STYLE,'color:blue;']));
		/*
		setTimeout(()=>{
		    this.jq(PROP_LAYERS_ANIMATION_PLAY).html(HU.getIconImage(icon_stop));
		},300);
*/
	    }
	    stepAnimation();
	}
    },
    toggleLayersAnimation:function() {
	this.attrs[PROP_LAYERS_ANIMATION_ON]	  =  !this.attrs[PROP_LAYERS_ANIMATION_ON];
	if(this.attrs[PROP_LAYERS_ANIMATION_ON] && !this.getVisible()) {
	    this.setVisible(true,null,null,true);
	}
	this.checkLayersAnimation();
    },
    toggleLayersVisibility:function(event) {
	let children = 	this.getChildren();
	if(!children||children.length==0) return;
	children.forEach(child=>{
	    child.highlighted=false;
	});
	event = event??{};
	if(event.shiftKey) {
	    children.forEach(child=>{
		child.setVisible(true,true);
	    });
	    return
	}
	if(event.metaKey) {
	    children.forEach(child=>{
		child.setVisible(false,true);
	    });
	    return
	}	

	let nextIdx=0;
	if(!this.visibleChild) nextIdx=0;
	else {
	    nextIdx = children.indexOf(this.visibleChild);
	    if(nextIdx<0) nextIdx=0;
	    else if(nextIdx==children.length-1) nextIdx=0;
	    else nextIdx++;
	}
	children.forEach((child,idx) =>{
	    if(idx==nextIdx) {
		child.setVisible(true,true,true);
		this.visibleChild = child;
	    }  else {
		child.setVisible(false,true);
	    }
	});
    },
    applyStyleToChildren:function(prop,value) {
	this.applyChildren(child=>{
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
							[ATTR_ID,this.domId('radii'),'size','40'])+' e.g., 1km, 2mi (miles), 100ft') +
		HU.formEntryTop('Rings Labels:',
				HU.hbox([HU.input('',this.attrs.rangeRingLabels??'',
						  [ATTR_ID,this.domId('rangeringlabels'),'size','40']),
					 'Use ${d} macro for the distance e.g.:<br> Label 1 ${d}, ..., Label N ${d}  '])) +

		HU.formEntry('Ring label angle:',
			     HU.input('',Utils.isDefined(this.attrs.rangeRingAngle)?this.attrs.rangeRingAngle:90+45,[
				 ATTR_ID,this.domId('rangeringangle'),'size',4]) +' Leave blank to not show labels') +
		HU.formEntryTop('Ring Styles',
				HU.hbox([HU.textarea('',this.attrs.rangeRingStyle??'',[ATTR_ID,this.domId('rangeringstyle'),'rows',5,'cols', 40]),
					 'Format:<br>ring #,style:value,style:value  e.g.:<br>1,fillColor:red,strokeColor:blue<br>2,strokeDashstyle:dot|dash|dashdot|longdash<br>N,strokeColor:black<br>*,strokeWidth:5<br>even,...<br>odd,...']));
	}
	if(this.isMapServer() && this.getDatacubeVariable()) {
	    return HU.formEntry('Color Table:',HU.div([ATTR_ID,this.domId('colortableproperties')]));
	}	    

	if(this.isStraightLine()) {
	    return HU.formEntry('Line type:',
				HU.select('',[ATTR_ID,this.domId(ID_LINETYPE)],[
				    {value:LINETYPE_STRAIGHT,label:'Straight'},
				    {value:LINETYPE_STEPPED,label:'Stepped'},
				    {value:LINETYPE_CURVE,label:'Curve'},
				    {value:LINETYPE_GREATCIRCLE,label:'Great Circle'}],
					  this.attrs.lineType)) +
		HU.formEntry('',HU.checkbox(this.domId(ID_ADDDOTS),[ATTR_ID,this.domId(ID_ADDDOTS)],this.attrs.addDots,'Add dots'));
	}

	return '';
    },

    addToPropertiesDialog:function(content,style) {
	let html='';
	let layout = (lbl,widget)=>{
	    html+=HU.b(lbl)+'<br>'+widget+'<br>';
	}
	let nameWidget = HU.input('',this.getName(),[ATTR_ID,this.domId('mapglyphname'),'size','40']);
	if(this.isEntry()) {
	    nameWidget+='<br>' +HU.checkbox(this.domId('useentryname'),[],this.getUseEntryName(),'Use name from entry');
	    nameWidget+=HU.space(3) +HU.checkbox(this.domId('useentrylocation'),[],this.getUseEntryLocation(),'Use location from entry');
	}
	html+=HU.formTable();
	html+=HU.formEntry('Name:',nameWidget);
	if(this.isMap() && this.attrs.resourceUrl) {
	    html+=HU.formEntry('Map URL:',HU.input('',this.attrs.resourceUrl,[ATTR_ID,this.domId('resourceurl'),'size','60']));
//	    opts.resourceUrl
	}	    
	html+=HU.formTableClose();


	let level = this.getVisibleLevelRange(true)??{};
	html+= HU.checkbox(this.domId('visible'),[],this.getVisible(),'Visible')
	if(this.getMapLayer()) {
	    html+= HU.space(4)+HU.checkbox(this.domId('canselect'),[],this.getCanSelect(),'Can Select');
	}
	html+='<br>';	
	html+=this.display.getLevelRangeWidget(level,this.getShowMarkerWhenNotVisible());

	let featureInfo = this.getFeatureInfoList();
	let 	lines = Utils.mergeLists(['_name','default'],featureInfo.map(info=>{return info.id;}));

	let makePopup = (id,label)=> {
	    let domId = this.display.domId('glyphedit_' +id);
	    let propsHelp =this.display.makeSideHelp(lines,domId,{prefix:'${',suffix:'}'});
	    let h = HU.leftRightTable(HU.b(label),
				    this.getHelp('#popuptext'));
	    let help = 'Add macro:'+ HU.div([ATTR_CLASS,'imdv-side-help'],propsHelp);
	    h+=  HU.hbox([HU.textarea('',style[id]??'',[ATTR_ID,domId,'rows',4,'cols', 40]),HU.space(2),help]);
	    return h;
	}
	html+=makePopup('popupText','Popup Text:');
	html+=HU.b('Legend Text:') +'<br>' +
	    HU.textarea('',this.attrs[ID_LEGEND_TEXT]??'',
			[ATTR_ID,this.domId(ID_LEGEND_TEXT),'rows',4,'cols', 40]);
	

	content.push({header:'Properties',contents:html});

	html='';
//	html=  this.getHelp('#miscproperties')+'<br>';
	let miscLines =[...IMDV_PROPERTY_HINTS];
	if(this.canHaveChildren()) {
	    miscLines.push(...IMDV_GROUP_PROPERTY_HINTS);
	}
	if(this.isMap()) {
	    miscLines.push('declutter.features=true');
	}

	this.getFeatureInfoList().forEach((info,idx)=>{
	    if(idx==0) {
		miscLines.push({skip:true,line:'<thin_hr></thin_hr><b>Features</b>'});
	    }		
	    miscLines.push({info:info.id,title:info.getLabel()});	    
	});

	let miscHelp =this.display.makeSideHelp(miscLines,this.domId('miscproperties'),{style:'height:350px;max-height:350px;',suffix:'\n'});
	let ex = HU.b('Add property:') + miscHelp

	html += HU.hbox([HU.textarea('',this.attrs.properties??'',[ATTR_ID,this.domId('miscproperties'),'rows',16,'cols', 40]),
			 HU.space(2),ex]);
	content.push({header:'Flags',contents:html});

	if(this.isDataIconCapable()) {
	    let contents ='';
	    let help = this.getHelp('dataicons.html');
	    let dataIconsSelect= HU.b('Show data icons: ')+
		HU.select('',[ATTR_ID,this.domId(ID_SHOWDATAICONS)],
			  ['inherited','yes','no'],
			  this.attrs[ID_SHOWDATAICONS]??'inherited');
			  
	    contents+= HU.leftRightTable(dataIconsSelect,help);
	    contents+='<thin_hr></thin_hr>';
	    if(Utils.stringDefined(this.transientProperties.mapglyphs)) {
		let on = this.getAttribute(ID_DATAICON_USEENTRY);
		let id = this.domId(ID_DATAICON_USEENTRY);
		contents+=  HU.radio(id, id, '', 'true', on) +
		    HU.tag('label',['for', id,ATTR_TITLE,''],  'Use the default data icon specification for the entry');
		contents+='<br>';
		contents+=  HU.radio(id+'_oruse', id, '', 'false', !on) +
		    HU.tag('label',['for', id+'_oruse',ATTR_TITLE,''],  'Use parent group\'s or the below if defined');
		contents+='<br>';
	    }



	    let buttonList = [HU.span([ATTR_ID,this.domId('dataicon_add_default'),ATTR_TITLE,'Set example values'],'Apply Defaults')];
	    if(Utils.stringDefined(this.transientProperties.mapglyphs)) {
		buttonList.push(HU.span([ID_GLYPH_ID,this.getId(),ATTR_CLASS,CLASS_CLICKABLE,ATTR_TITLE,'Apply settings from entry',ATTR_ID,this.domId('applyentrydataicon')],'Apply from Entry'));
	    }
	    buttonList.push(HU.span([ATTR_ID,this.domId('dataicon_clear_default'),ATTR_TITLE,'Clear properties'],'Clear'));

	    let dataIconInfo  =this.getDataIconInfo();
	    contents+=  HU.buttons(buttonList,
				   null,HU.css('text-align','left'));

	    let fields1 = HU.b('Menu Fields:')+'<br>'+
		HU.textarea('',dataIconInfo[ID_DATAICON_FIELDS]??'',
			    ['placeholder','field pattern,label=<label>,unit=<unit>\ne.g.:\n'+
			     DEFAULT_DATAICON_FIELDS,ATTR_ID,this.domId(ID_DATAICON_FIELDS),'rows',4,'cols', 60]);
	    let  fields2= HU.b('Initial field:')+'<br>'+
		HU.input('',dataIconInfo[ID_DATAICON_INIT_FIELD]??'',[ATTR_ID,this.domId(ID_DATAICON_INIT_FIELD),'size','25','placeholder','Initial field']) +'<br>' +
		HU.b('Menu Label:') +'<br>'  +
		HU.input('',dataIconInfo[ID_DATAICON_LABEL]??'',[ATTR_ID,this.domId(ID_DATAICON_LABEL),'size','25']);

	    contents+=HU.table(HU.tr(['valign','top'],HU.td(fields1) +HU.td(HU.div([ATTR_STYLE,'margin-left:8px;'],fields2))));
	    contents+='<p>';
	    contents+=HU.b('Canvas: ') +
		'W: ' + HU.input('',dataIconInfo[ID_DATAICON_WIDTH]??'',[ATTR_ID,this.domId(ID_DATAICON_WIDTH),'size','3']) +
		' H: ' + HU.input('',dataIconInfo[ID_DATAICON_HEIGHT]??'',[ATTR_ID,this.domId(ID_DATAICON_HEIGHT),'size','3']) +
		HU.space(2) + HU.b('Icon Size: ') +
		HU.input('',dataIconInfo[ID_DATAICON_SIZE]??'',[ATTR_ID,this.domId(ID_DATAICON_SIZE),'size','3']);


	    contents+=  HU.div([ATTR_STYLE,'padding-bottom:0.5em;'],
			       HU.b('Properties:') + HU.space(1) +
			       HU.input('',dataIconInfo[ID_DATAICON_PROPS]??'',[ATTR_ID,this.domId(ID_DATAICON_PROPS),'size','80']));
	    contents+=HU.b('Icon Specification:')  +'<br>';
	    contents +=
		HU.textarea('',dataIconInfo[ID_DATAICON_MARKERS]??'',[ATTR_ID,this.domId(ID_DATAICON_MARKERS),'rows',4,'cols', 90]);
	    content.push({
		header:'Data Icons',
		contents: contents});

	}

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

    applyPropertiesDialog: function(style) {
	//Clear out any feature infos
	this.featureInfo=null;

	//Make sure we do this after we set the above style properties
	this.setName(this.jq("mapglyphname").val());
	if(this.isMap()) {
	    let newUrl = this.jq('resourceurl').val();
	    if(newUrl!=this.attrs.resourceUrl) {
		this.attrs.resourceUrl = newUrl;
		setTimeout(()=>{this.checkMapLayer(false,true);},10);
	    }
	}
	this.attrs[ID_LEGEND_TEXT] = this.jq(ID_LEGEND_TEXT).val();
	if(this.isEntry()) {
	    this.setUseEntryName(this.jq("useentryname").is(":checked"));
	    this.setUseEntryLabel(this.jq("useentrylabel").is(":checked"));
	    this.setUseEntryLocation(this.jq("useentrylocation").is(":checked"));
	}
	this.setVisible(this.jq('visible').is(':checked'),true,null,true);
	if(this.jq('canselect').length) {
	    this.attrs.canSelect = this.jq('canselect').is(':checked');
	    if(this.getMapLayer()) this.getMapLayer().canSelect = this.attrs.canSelect;
	}

	this.parsedProperties = null;
	this.attrs.properties = this.jq('miscproperties').val();
	if(this.display.jq(ID_LEVEL_RANGE_CHANGED).val()=='changed') {
	    this.setVisibleLevelRange(this.display.jq(ID_LEVEL_RANGE_MIN).val(),
				      this.display.jq(ID_LEVEL_RANGE_MAX).val());
	} else 	if(this.display.jq(ID_LEVEL_RANGE_CHANGED).val()=='cleared') {
	    this.attrs.visibleLevelRange = null;
	    this.checkVisible();
	}
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


	if(this.isMultiEntry()) {
	    this.applyChildren(child=>{
		let newStyle = $.extend({},style);
		if(!style.showLabels) {
		    newStyle.label=null;
		} else {
		    newStyle.label = child.style.label;
		}
		newStyle.externalGraphic = child.style.externalGraphic;		
		child.applyStyle(newStyle);
	    });
	}


	if(this.isDataIconCapable()) {
	    if(this.jq(ID_SHOWDATAICONS).length) {
		this.setShowDataIcons(this.jq(ID_SHOWDATAICONS).val());
	    }
	    this.setAttribute(ID_DATAICON_USEENTRY,this.jq(ID_DATAICON_USEENTRY).is(':checked'));
	    let dataIconInfo = this.getDataIconInfo();
	    [ID_DATAICON_MARKERS, ID_DATAICON_FIELDS,ID_DATAICON_INIT_FIELD,
	     ID_DATAICON_WIDTH, ID_DATAICON_HEIGHT, ID_DATAICON_SIZE,
	     ID_DATAICON_LABEL, ID_DATAICON_PROPS].forEach(prop=>{
		 dataIconInfo[prop] = this.jq(prop).val();
	     });
	}

	this.applyStyle(style);

	if(this.isDataIconCapable()) {
	    this.applyDataIcon();
	    this.checkDataIconMenu();
	}

	if(this.getImage()) {
	    this.getImage().setOpacity(style.imageOpacity);
	    this.checkImage();
	}

	//If we are a group then this triggers a redraw of any descendent images
	//to apply the inherited image transform
	this.applyChildren(child=>{
	    if(child.getImage()) {
		child.checkImage();
	    }
	},true);



    },

    
    featureSelected:function(feature,layer,event) {
	if(this.selectedStyleGroup) {
	    let indices = this.selectedStyleGroup.indices;
	    if(indices.includes(feature.featureIndex)) {
		this.selectedStyleGroup.indices = Utils.removeItem(indices,feature.featureIndex);
		feature.style =  feature.originalStyle = null;
	    } else {
		this.getStyleGroups().forEach((group,idx)=>{
		    group.indices = Utils.removeItem(group.indices,feature.featureIndex);
		});
		feature.style = feature.originalStyle = $.extend(feature.style??{},this.selectedStyleGroup.style);
		indices.push(feature.featureIndex);
	    }
	    ImdvUtils.scheduleRedraw(layer,feature);
	    this.display.featureChanged(true);	    
	    return
	}
	this.display.getMap().onFeatureSelect(feature.layer,event)
    },
    featureUnselected:function(feature,layer,event) {
	//	this.display.getMap().onFeatureSelect(feature.layer,event)
    },
    glyphCreated:function() {
	this.applyDataIcon();
    },


    isDataIconCapable:function() {
	return this.isEntry() || this.isGroup()  || this.isMultiEntry();
    },
    getDataIconInfo:function(v) {
	let ID = 'dataIconInfo';
	//check for old property
	if(this.attrs.glyphInfo) {
	    if(!this.attrs[ID]) {
		this.attrs[ID] = this.attrs.glyphInfo;
	    }
	    this.attrs.glyphInfo = null;
	}

	if(!this.attrs[ID]) {
	    this.attrs[ID] = {};
	}
	let info =  this.attrs[ID];
	//backwards compat
	if(info.glyphs) {
	    if(!info[ID_DATAICON_MARKERS]) info[ID_DATAICON_MARKERS]= info.glyphs;
	    info.glyphs=null;
	}
	return info;
    },
    getDataIconProperty:function(property,dflt) {
	let debug = false;
//	debug=property==ID_DATAICON_SELECTED_FIELD;
	if(this.getAttribute(ID_DATAICON_USEENTRY)) {
	    return dflt;
	}	    

	if(debug) {
	    console.log("getDataIconProperty:" + this.getName()+' prop='+property);
	}
	let value = this.getDataIconInfo()[property];
	if(Utils.stringDefined(value)) {
	    if(debug) console.log("\tmine:" +value);
	    //If it is the field then make sure it is in the FIELDS
	    if(property==ID_DATAICON_SELECTED_FIELD) {
		let fields = this.getDataIconInfo()[ID_DATAICON_FIELDS];
		if(!Utils.stringDefined(fields) ||
		   (fields && fields.indexOf(value)<0)) {
		       value = null;
		   }
	    }
	    if(value)
		return value;
	}
	if(property==ID_DATAICON_INIT_FIELD) {
	    if(this.dataIconFieldsId) {
		glyphField = jqid(this.dataIconFieldsId).val();
		if(glyphField) return glyphField;
	    }
	}

	if(this.getParentGlyph()) {
	    if(debug)
		console.log("\tasking parent");
	    return this.getParentGlyph().getDataIconProperty(property,dflt);
	}
	return dflt;
    },

    getDataIconMarkers:function() {
	if(this.getAttribute(ID_DATAICON_USEENTRY)) {
	    if(Utils.stringDefined(this.transientProperties.mapglyphs)) {
		return this.transientProperties.mapglyphs;
	    }
	}
	let markers = this.getDataIconProperty(ID_DATAICON_MARKERS);
	if(Utils.stringDefined(markers)) {
	    return markers;
	}
	return this.transientProperties.mapglyphs;
    },


    checkDataIconMenu:function() {
	let _this = this;
	let dataIconInfo = this.getDataIconInfo();
	if(this.dataIconContainer) {
	    jqid(this.dataIconContainer).hide();
	}
	if(!this.getProperty('showGlyphMenu',true,true)) {
	    return
	}

	if(!Utils.stringDefined(dataIconInfo[ID_DATAICON_FIELDS])) {
	    return;
	}	    
	this.dataIconFieldsId = HU.getUniqueId('dataiconfields_');


	if(!this.dataIconContainer || jqid(this.dataIconContainer).length==0) {
	    this.dataIconContainer = HU.getUniqueId('dataiconfieldscontainer_');
	    this.display.jq(ID_HEADER1).append(HU.div([ATTR_STYLE,HU.css('display','inline-block','margin-right','20px'),ATTR_ID,this.dataIconContainer]));
	}
	jqid(this.dataIconContainer).show();


	let items = [];
	Utils.split(dataIconInfo[ID_DATAICON_FIELDS],'\n',true,true).forEach(item=>{
	    let toks = Utils.split(item,',',true,true);
	    let map = {};
	    for(let i=1;i<toks.length;i++) {
		let toks2 = Utils.split(toks[i],"=",true,true);
		if(toks2.length>1) map[toks2[0]] = toks2[1];
	    }
	    items.push({value:toks[0],label:map.label});
	});
	let menu = HU.select('',[ATTR_ID,this.dataIconFieldsId],
			     items,
			     Utils.getStringDefined(dataIconInfo[ID_DATAICON_SELECTED_FIELD],
						    dataIconInfo[ID_DATAICON_INIT_FIELD]));
	let label = Utils.getStringDefined(dataIconInfo[ID_DATAICON_LABEL],'Select field');
	let clazz = '';
	if(!this.isVisible()) {
	    clazz+=' ' + CLASS_LEGEND_LABEL_INVISIBLE;
	}
	let contents = HU.div([ATTR_CLASS,clazz,ATTR_STYLE,HU.css('padding','4px')],HU.b(label)+':'+HU.space(1)+menu);
	jqid(this.dataIconContainer).html(contents);

	jqid(this.dataIconFieldsId).change(function(){
	    _this.getDataIconInfo()[ID_DATAICON_SELECTED_FIELD] = $(this).val();
	    _this.applyDataIcon();
	});

    },


    getStyleForProperties:function(style) {
	return this.resetDataIconOriginal(style);
    },

    resetDataIconOriginal:function(style) {
	style = style??this.style;
	if(this.attrs[ID_DATAICON_ORIGINAL]) {
	    let o = this.attrs[ID_DATAICON_ORIGINAL];
	    DATAICON_PROPERTIES.forEach(prop=>{
		style[prop] = o[prop]??this.style[prop];
	    });
	}
	if(this.isDataIconCapable()) {
	    if(Utils.stringDefined(this.attrs.icon)) {
		style.externalGraphic = this.attrs.icon;
	    }
	}
	return style;
    },
    clearDataIcon: function() {
	if(this.getDataIconShowing()) {
	    this.setDataIconShowing(false);
	    this.resetDataIconOriginal();
	    this.attrs[ID_DATAICON_ORIGINAL] = null;
	    this.applyStyle();
	}
    },

    glyphHasBeenDropped:function() {
	if(this.getShowDataIcons()) {
	    this.applyDataIcon();
	} else {
	    this.clearDataIcon();
	}
    },


    applyDataIcon: function() {
	if(this.isEntry()) {
	    this.makeDataIcon();
	}
	this.applyChildren(child=>{
	    child.applyDataIcon();
	});
    },


    makeDataIcon:function(force) {
	let debug  = false;
//	debug=true;

	if(!this.isVisible())  {
	    return;
	}
	if(!force && !this.getShowDataIcons()) {
	    this.clearDataIcon();
	    return;
	}
	let markersString = this.getDataIconMarkers();
	if(!Utils.stringDefined(markersString)) {
	    return;
	}
	if(debug)	console.log('makeDataIcon',this.getName());
	let opts = {
	    entryId:this.attrs.entryId
	};

	let markerLines = [];
	markersString = markersString.replace(/\\ *\n/g,'');
	let rawLines = Utils.split(markersString,'\n',true,true);
	rawLines.forEach(line=>{
	    line = line.trim();
	    if(line.startsWith("#") || line == "") return;
	    //console.log('\tline:'+line);
	    markerLines.push(line);
	});
	if(markerLines.length==0) {
	    console.log("\tno markers-2");
	    return;
	}
	let markers = [];
	let lines=[];
	let props = {};
	markerLines.forEach(line=>{
	    line = line.trim();
	    if(line.startsWith("#")) return;
	    if(line.startsWith('props:')) {
		this.parseDataIconProps(props,line.substring('props:'.length));
		return;
	    }
	    lines.push(line);
	});
	this.parseDataIconProps(props,this.getDataIconProperty(ID_DATAICON_PROPS));

	let sampleCount = props.sampleCount??1;
	let url = Ramadda.getUrl("/entry/data?record.last="+ sampleCount+"&max=" + sampleCount+"&entryid=" + opts.entryId);
//	console.log('url',url);
	let pointData = new PointData("",  null,null,url, {entryId:opts.entryId});


	let callback = (data)=>{
	    this.makeDataIcons(pointData,data,markers,lines,props);
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
    parseDataIconProps:function(props,line) {
	Utils.split(line??'',',',true,true).forEach(line2=>{
	    let toks = Utils.split(line2,":",true,true);
	    if(toks.length==2) {
		props[toks[0]] = toks[1];
	    }
	});
    },
    getDataIconShowing: function() {
	return this.attrs[ID_DATAICON_SHOWING];
    },
    setDataIconShowing: function(v) {
	this.attrs[ID_DATAICON_SHOWING] =v;
    },    


    makeDataIcons: function(pointData,data,markers,lines,props) {

	let cvrt=(v,dflt)=>{
	    if(!Utils.stringDefined(v)) return dflt;
	    return v;
	};


	//This recurses up the glyph tree
	let size=cvrt(this.getDataIconProperty(ID_DATAICON_SIZE),props.iconSize??100);
	let canvasWidth=parseFloat(cvrt(this.getDataIconProperty(ID_DATAICON_WIDTH),props.canvasWidth??100));
	let canvasHeight=parseFloat(cvrt(this.getDataIconProperty(ID_DATAICON_HEIGHT),props.canvasHeight??100));
	let selectedField=this.getDataIconProperty(ID_DATAICON_SELECTED_FIELD);

	if(!Utils.stringDefined(selectedField)) {
	    selectedField=this.getDataIconProperty(ID_DATAICON_INIT_FIELD);
	}
	let markerFields = this.getDataIconProperty(ID_DATAICON_FIELDS);
	let attrs = {};
	if(debugDataIcons)
	    console.log({size,canvasWidth,canvasHeight});
	if(selectedField && markerFields) {
	    Utils.split(markerFields,'\n',true,true).every(item=>{
		let toks = Utils.split(item,',',true,true);
		if(toks[0]!=selectedField) return true;
		for(let i=1;i<toks.length;i++) {
		    let toks2 = Utils.split(toks[i],"=",true,true);
		    attrs[toks2[0]] = toks2[1]??'';
		}
		return false;
	    });
	}

	lines.forEach(line=>{
	    line = line.trim();
	    if(line.startsWith('#')) return;
	    if(selectedField) {
		line = line.replace(/\${_field}/g,selectedField.replace(/ /g,''));
	    }

	    let extra = '';
	    ['scale','offset1','offset2'].forEach(a=>{
		if(attrs[a]) extra+= ' ' + a +'=' + attrs[a] +' ';
	    });
	    line = line.replace(/\${_extra}/g,extra);
	    let unit = '';
	    Object.keys(attrs).forEach(key=>{
		if(key=='unit') {
		    unit = attrs[key];
		    return;
		}
		if(key=='label') return;
		line = line.replaceAll("\${" + key+"}",attrs[key]);
	    });

	    //In case there wasn't a unit
//	    line = line.replaceAll(/\${unit}/g,'');
	    line = line.replaceAll(/\${icon}/g,this.getIcon());	    
	    props = Utils.clone({},
				props,{
				    glyphField:selectedField,
				    canvasWidth:canvasWidth,
				    canvasHeight: canvasHeight,
				    entryname: this.getName(),
				    unit:unit
				},attrs);
	    if(debugDataIcons)
		console.log('line:'+ line);
	    markers.push(new Glyph(this.display,1.0, data.getRecordFields(),data.getRecords(),props,line));
	});
	let cid = HU.getUniqueId("canvas_");
	let c = HU.tag("canvas",[ATTR_CLASS,"", ATTR_STYLE,"xdisplay:none;", 	
				 ATTR_WIDTH,canvasWidth,ATTR_HEIGHT,canvasHeight,ATTR_ID,cid]);

	let isShown = true;
	markers.forEach(marker=>{
	    if(!marker.okToShow()) {
		isShown=false;
	    }
	});


	$(document.body).append(c);
	let canvas = document.getElementById(cid);
	let ctx = canvas.getContext("2d");
	if(isShown &&props.fill) {
	    ctx.fillStyle=props.fill;
	    ctx.fillRect(0,0,canvasWidth,canvasHeight);
	}
	ctx.strokeStyle ="#000";
	ctx.fillStyle="#000";
	let pending = [];
	let records = data.getRecords();
	let numberCount = 0;
	let missingCount = 0;	
	markers.forEach(marker=>{
	    //if its an image glyph then the image might not be loaded so the call returns a
	    //isReady function that we keep checking until it is ready
	    let isReady =  marker.draw(props, canvas, ctx, 0,canvasHeight,{
		findNonNan:props.findNonNan,
		records:records,
		recordIndex:records.length-1,
		record:records[records.length-1]
	    });
	    if(isReady) pending.push(isReady);
	    //check for missing
	    if(!marker.isImage()) {
		numberCount++;
		if(marker.hadMissingValue()) {
		    missingCount++;
		}
	    }
	});

	if(numberCount>0 && numberCount==missingCount) {
	    isShown=false;
	}

	if(isShown && props.borderColor) {
	    ctx.strokeStyle = props.borderColor;
	    ctx.lineWidth=parseFloat(props.borderWidth??1);
	    let d = 0.5*ctx.lineWidth;
	    ctx.strokeRect(0+d,0+d,canvasWidth-d*2,canvasHeight-d*2);
	    ctx.strokeStyle = null;
	    ctx.lineWidth=1;
	}

	//Save the original style
	if(!this.getDataIconShowing()) {
	    //Check for the case where the style has been set with a data icon when we have the properties dialog up
	    if(!this.style?.externalGraphic?.startsWith('data')) {
		this.attrs[ID_DATAICON_ORIGINAL] = {};
		DATAICON_PROPERTIES.forEach(prop=>{
		    this.attrs[ID_DATAICON_ORIGINAL][prop] = this.style[prop];
		});
	    }
	} 
	this.setDataIconShowing(true);
	let finish = ()=>{
	    //Check for a race condition
	    if(!this.getDataIconShowing())  {
		return;
	    }

	    try {
		let img = canvas.toDataURL();
		if($('#testimg').length) 
		    $("#testimg").html(HU.tag(TAG_IMG,[ATTR_SRC,img]));
		canvas.remove();
		this.style.label=null;
		this.style.pointRadius=size;
		if(!isShown) {
		    this.style.pointRadius=0;
		}
		this.style.externalGraphic=img;
	    } catch(err) {
		console.error('Error',err);
		if(String(err).indexOf('insecure')>=0) {
		    alert('There was an error making the data icon.\nPerhaps one of the images is from an external server');
		
		}
	    }
	    if(records && records.length>0 && this.features&& this.getProperty(PROP_MOVE_TO_LATEST_LOCATION,null,true)) {
		let record = records[records.length-1];
		let p1 = MapUtils.createLonLat(record.getLongitude(),record.getLatitude())
		p1 = this.getMap().transformLLPoint(p1);
		p1= new OpenLayers.Geometry.Point(p1.lon, p1.lat);
		this.features.forEach(feature=>{
		    let geometry = feature.geometry;
		    if(geometry && Utils.isDefined(geometry.x)) {
			geometry.x=p1.x;
			geometry.y=p1.y;			
			geometry.clearBounds();
		    }
		});
	    }

	    this.applyStyle(this.style,true,true);		
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

    setAttribute:function(property,value) {
	this.attrs[property] = value;
    },
    getAttribute:function(property,value,dflt) {
	if(!Utils.isDefined(this.attrs[property])) return dflt;
	return Utils.getProperty(this.attrs[property]);
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
	    link = HU.div([ATTR_STYLE,'white-space:nowrap;max-width:180px;overflow-x:hidden;',ATTR_TITLE,entry.getName()], link);
	    let add = '';
	    if(MAP_TYPES.includes(entry.getType().getId())) {
		add = HU.span([ATTR_CLASS,CLASS_CLICKABLE,ATTR_TITLE,'add map','entryid',entry.getId(),'command',GLYPH_MAP],HU.getIconImage('fas fa-plus'));
	    } else if(entry.isPoint) {
		add = HU.span([ATTR_CLASS,CLASS_CLICKABLE,ATTR_TITLE,'add data','entryid',entry.getId(),'command',GLYPH_DATA],HU.getIconImage('fas fa-plus'));
	    } else if(entry.isGroup) {
		add = HU.span([ATTR_CLASS,CLASS_CLICKABLE,ATTR_TITLE,'add multi entry','entryid',entry.getId(),'command',GLYPH_MULTIENTRY],HU.getIconImage('fas fa-plus'));
	    } else {
	    }		
	    if(add!='') {
		link = HU.leftRightTable(link,add);
	    }

	    html+=HU.div([ATTR_STYLE,HU.css('white-space','nowrap')],link);
	});
	if(html!='') {
	    html = HU.div([ATTR_CLASS,'ramadda-cleanscroll', ATTR_STYLE,'max-height:200px;overflow-y:auto;'], HU.div([ATTR_STYLE,'margin-right:10px;'],html));
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
		this.getMap().toFrontLayer(this.mapServerLayer);
	    else
		this.getMap().toBackLayer(this.mapServerLayer);		    
	    return;		
	}
	if(this.getImage()) {
	    if(toFront)
		this.getMap().toFrontLayer(this.getImage());
	    else
		this.getMap().toBackLayer(this.getImage());		    
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
//	return this.attrs.filterable??true;
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
    redraw:function() {
	if(this.getMapLayer()) {
	    ImdvUtils.scheduleRedraw(this.getMapLayer(),this);
	} else {
	    this.display.redraw(this);
	}
    },
    checkLineType:function(points) {
	if(this.attrs.lineType==LINETYPE_GREATCIRCLE || this.attrs.lineType==LINETYPE_CURVE) {
	    if(!MapUtils.loadTurf(()=>{this.checkLineType(points);})) {
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
	    let line = turf.lineString(tmp);	
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
    getStyleFromTree: function(prop,dflt) {
	if(Utils.stringDefined(this.style[prop])) return this.style[prop];
	if(this.getParentGlyph()) return this.getParentGlyph().getStyleFromTree(prop,dflt);
	return dflt;
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
	let bounds;
	if(this.attrs.bounds) {
	    //wsen
	    let b = this.attrs.bounds;
	    bounds = MapUtils.createBounds(b[0],b[1],b[2],b[3]);
	    bounds = this.getMap().transformLLBounds(bounds);
	}
	if(!bounds) {
	    bounds = this.getBounds();
	}
	if(bounds) {
	    this.getMap().zoomToExtent(bounds);
	    if(bounds.getWidth()==0) andZoomIn = true;
	}
	if(andZoomIn) {
	    //The -1 is a flag to use the singlePointZoom
	    this.getMap().zoomTo(-1,true);
	}
    },
    getBounds: function() {
	let bounds = null;
	if(this.haveChildren()) {
	    this.applyChildren(child=>{bounds =  MapUtils.extendBounds(bounds,child.getBounds());});
	}
	if(this.features && this.features.length) {
	    bounds = MapUtils.extendBounds(bounds,this.getMap().getFeaturesBounds(this.features));
	}
	if(this.extraFeatures) {
	    bounds = MapUtils.extendBounds(bounds, this.getMap().getFeaturesBounds(this.extraFeatures));
	}

	if(this.isMapServer()) {
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
						   this.getMap().getLayerVisibleExtent(obj.layer)||obj.layer.extent);
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

	if(!MapUtils.boundsDefined(bounds) && this.isMultiEntry() && this.entries) {
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
		bounds =  this.display.getMap().transformLLBounds(bounds);
	    }
	}
	//Cache the bounds
	if(MapUtils.boundsDefined(bounds)) {
//	    console.log(this.getName() +' getBounds defined:', bounds);
	    this.attrs.lastBounds = bounds;
	} else if(this.attrs.lastBounds) {
//	    console.log(this.getName() +' using last bounds:',this.attrs.lastBounds);
	    return MapUtils.createBounds(this.attrs.lastBounds);
	} else {
//	    console.log(this.getName() +' no bounds');
	}
	return bounds;
    },


    collectFeatures: function(features) {
	if(this.haveChildren()) {
	    this.applyChildren(child=>{child.collectFeatures(features);});
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
	let text = this.getPopupTextInner();
	if(text) text = text.replace(/\${_name}/g,this.getName());
	return text;
    },
    getPopupTextInner: function() {	
	if(Utils.stringDefined(this.style.popupText)) return this.style.popupText;
	if(this.getParentGlyph()) return  this.getParentGlyph().getPopupTextInner();
	return null;
    },
    getEntryId: function() {
	return this.attrs.entryId;
    },
    hasBounds:function() {
	if(this.isMapServer()) {
	    if(this.attrs.bounds) return true;
	    if(this.getDatacubeVariable() && Utils.isDefined(this.getDatacubeAttr('geospatial_lat_min'))) {
		return true;
	    }
	    return false;
	}

	return  !this.isFixed();
    },

    getLabel:function(opts) {
	opts = opts??{};
	let args = {
	    forLegend:false,
	    addDecorator:false,
	    addIcon:true
	}
	$.extend(args,opts);
	let name = this.getName();
	let label;
	let theLabel;
	if(Utils.stringDefined(name)) {
	    if(!args.forLegend)
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
	let glyphType = this.getGlyphType();
	let right = '';
	if(args.addDecorator) {
	    //For now don't add the decoration (the graphic indicator)
	    //right+=this.getDecoration(true);
	}

	if(glyphType) {
	    let icon = Utils.getStringDefined(this.style.externalGraphic,this.attrs.icon,glyphType.getIcon());
	    if(icon.startsWith('data:')) icon = this.attrs.icon;
	    if(icon && icon.endsWith('blank.gif')) icon = glyphType.getIcon();
	    icon = HU.image(icon,[ATTR_WIDTH,'18px']);
	    if(url && args.forLegend)
		icon = HU.href(url,icon,['target','_entry']);
	    let showZoomTo = args.forLegend && this.hasBounds();
	    if(this.canHaveChildren()) {
		if(this.getProperty(PROP_LAYERS_ANIMATION_SHOW)) {
		    right+=SPACE+HU.span([ATTR_CLASS,CLASS_CLICKABLE,
					  ATTR_TITLE,'Play',
					  ATTR_ID,this.domId(PROP_LAYERS_ANIMATION_PLAY),
					  ID_GLYPH_ID,this.getId(),'buttoncommand',PROP_LAYERS_ANIMATION_PLAY],
					 HU.getIconImage(icon_play,[],BUTTON_IMAGE_ATTRS));
		}

		if(this.getProperty(PROP_LAYERS_STEP_SHOW)) {
		    right+=SPACE+HU.span([ATTR_CLASS,CLASS_CLICKABLE,
					  ATTR_TITLE,'Cycle visibility children. Shift-key: all visible; Meta-key: all hidden',
					  ID_GLYPH_ID,this.getId(),'buttoncommand',PROP_LAYERS_STEP_SHOW],
					 HU.getIconImage('fas fa-arrows-spin',[],BUTTON_IMAGE_ATTRS));
		}
	    }

	    if(showZoomTo) {
		right+=SPACE+
		    HU.span([ATTR_CLASS,HU.classes(CLASS_CLICKABLE, CLASS_LEGEND_ITEM_VIEW),
			     ID_GLYPH_ID,this.getId(),
			     TITLE,'Click:Move to; Shift-click:Zoom in',],
//<i class="fa-regular fa-eye"></i>
			    HU.getIconImage('fas fa-eye',[],LEGEND_IMAGE_ATTRS));
	    }
	    if(args.addIcon) {
		if(args.forLegend && this.getProperty('showLegendBox')) {
		    let boxStyle = this.getLegendStyle(this.style);
		    label = HU.div([ATTR_CLASS,'imdv-legend-box',
				    ATTR_STYLE,boxStyle], '')  + label;
		} else {
		    label = HU.span([ATTR_STYLE,'margin-right:5px;'], icon)  + label;
		}
	    }
	}

	if(args.forLegend) {
	    let extra = this.getProperty('legendTooltip',null);
	    if(extra) extra = HU.div([],extra);
	    let typeLabel =this.getGlyphType().getName();
	    if(this.entry) {
		let type = this.entry.getType();
		if(type) {
		    typeLabel+=HU.div([],"Type:" + type.name);
		}
	    }
	    
	    let title = HU.b(HU.center(theLabel))+
		HU.div([],typeLabel) +
		(extra??'') +
		'Click to toggle visibility<br>Shift-click to select';
	    label = HU.div([ATTR_TITLE,title,ATTR_STYLE,HU.css('overflow-x','hidden','white-space','nowrap')], label);	    
	}
	if(right!='') {
	    right= HU.span([ATTR_STYLE,HU.css('white-space','nowrap')], right);
	}
	if(args.forLegend) {
	    let clazz = CLASS_LEGEND_LABEL;
	    label = HU.div([ATTR_CLASS,HU.classes(CLASS_CLICKABLE, clazz),
			    ID_GLYPH_ID,this.getId()],label);
	    return [label,right];
	}
	return label;
    },


    loadJson:function(jsonObject) {
	if(jsonObject.children) {
	    jsonObject.children.forEach(childJson=>{
		let child = this.display.makeGlyphFromJson(childJson);
		if(child) {
		    this.addChildGlyph(child);
		}
	    });
	}
    },
    removeChild: function(child) {
	if(this.children) {
	    this.children = Utils.removeItem(this.children, child);
	}
    },
    clearChildren:function() {
	if(this.children) {
	    let tmp = [...this.children];
	    tmp.forEach(child=>{
		child.doRemove();
	    });
	    this.children=[];
	}
    },

    applyChildren:function(func,descend) {
	if(!this.haveChildren()) return;
	this.getChildren().forEach(child=>{
	    func(child);
	    if(descend) child.applyChildren(func,descend);
	});
    },
    getChildren: function() {
	if(!this.children) {
	    this.children = [];
	}
	return this.children;
    },
    haveChildren: function() {
	if(this.children && this.children.length>0) {
	    return true;
	}    
	return false;
    },
    findGlyph:function(id) {
	if(id == this.getId()) return this;
	return ImdvUtils.findGlyph(this.getChildren(),id);
    },
    addChildGlyph: function(child) {
//	console.log("add child:" + child.getName());
	this.getChildren().push(child);
	child.setParentGlyph(this);
    },
    getParentGlyph: function() {
	return this.parentGlyph;
    },
    setParentGlyph: function(parent) {
	if(this.parentGlyph) this.parentGlyph.removeChild(this);
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
	return this.jq(ID_GLYPH_LEGEND);
    },
    setLayerLevel:function(level) {
	let setIndex= (layer) =>{
	    if(layer) {
		level++;
		layer.ramaddaLayerIndex=level;
	    }
	}	    
	setIndex(this.getMapLayer());
	if(this.imageLayers) {
	    this.imageLayers.forEach(obj=>{
		setIndex(obj.layer);
	    });
	}
	setIndex(this.mapServerLayer);
	setIndex(this.image);
	if(this.displayInfo?.display?.getMyMapLayers) {
	    this.displayInfo.display.getMyMapLayers().forEach(layer=>{
		setIndex(layer);
	    });
	}

	this.applyChildren(mapGlyph=>{level = mapGlyph.setLayerLevel(level);});
	return level;
    },
    findInLegend:function(clazz) {
	if(clazz.startsWith('imdv-'))  clazz='.'+ clazz;
	let sel = '#' + this.domId(ID_GLYPH_LEGEND) +' '+clazz;
	if(this.topHeaderId)
	    sel+=', #' +this.topHeaderId+ ' ' + clazz;
	return  $(sel);
    },
    

    checkInMapLabel:function() {
	let label= this.jq(ID_INMAP_LABEL);
	if(this.getProperty('showLabelInMapWhenVisible',false)) {
	    if(this.getVisible()) label.show();
	    else label.hide();	    
	}

	if(this.getVisible()) label.removeClass('imdv-inmap-label-invisible');
	else  label.addClass('imdv-inmap-label-invisible');	
    },

    addInMapLabel:function() {
	let label= this.jq(ID_INMAP_LABEL);
	label.remove();
	let showLabel = this.getProperty('showLabelInMap',false);
	if(showLabel || this.getProperty('showLabelInMapWhenVisible',false)) {
	    let clazz = (showLabel?CLASS_CLICKABLE:'')+' imdv-inmap-label';
	    this.display.getLabels().append(HU.div([ATTR_ID,this.domId(ID_INMAP_LABEL),
						    ATTR_CLASS,clazz],
						   this.getProperty('inMapLabel')??this.getName()));
	    if(showLabel) {
		this.jq(ID_INMAP_LABEL).click(()=>{
		    this.setVisible(!this.getVisible(),true);
		});
	    }
	    this.checkInMapLabel();
	}
    },
    makeLegend:function(opts) {
	this.addInMapLabel();
	opts = opts??{};
	let html = '';
	if(!this.display.getShowLegendShapes() && this.isShape()) {
	    return "";
	}
	let label =  this.getLabel({forLegend:true,addDecorator:true});
	let body = HU.div([ATTR_CLASS,CLASS_LEGEND_INNER],this.getLegendBody());

	if(this.imageLayers) {
	    let cbx='';
	    if(this.getMapLayer() && this.getMapLayer().features.length) {
		cbx+=HU.div([],HU.checkbox(Utils.getUniqueId(''),['imageid','main',
								  ATTR_CLASS,'imdv-imagelayer-checkbox'],
					   this.isImageLayerVisible({id:'main'}),
					   'Main Layer'));
	    }
	    this.imageLayerMap = {};
	    this.imageLayers.forEach(obj=>{
		this.imageLayerMap[obj.id] = obj;
		cbx+=HU.div([],HU.checkbox(Utils.getUniqueId(''),['imageid',obj.id,
								  ATTR_CLASS,'imdv-imagelayer-checkbox'],
					   this.isImageLayerVisible(obj),
					   obj.name));
	    });
	    body+=HU.div([ATTR_CLASS,CLASS_LEGEND_OFFSET],cbx);
	}




	if(this.haveChildren()) {
	    if(this.getProperty('showTextSearch',false)) {
		let input =  HU.input('',this.getProperty('searchtext')??'',[ATTR_STYLE,'width:100%',ATTR_PLACEHOLDER,'Search Text',ATTR_ID,this.domId('searchtext')]);
		body+=HU.div([ATTR_STYLE,'margin-bottom:4px;margin-left:8px;margin-right:8px;'],input);
	    }



	    let child="";
	    this.applyChildren(mapGlyph=>{
		let childHtml = mapGlyph.makeLegend(opts);
		if(childHtml) child+=childHtml;
	    });
	    body+=HU.div([ATTR_CLASS,CLASS_LEGEND_OFFSET],child);
	}

	let block = HU.toggleBlockNew("",body,this.getLegendVisible(),
				      {separate:true,headerStyle:'display:inline-block;',
				       extraAttributes:['map-glyph-id',this.getId()]});		
	if(opts.idToGlyph)
	    opts.idToGlyph[this.getId()] = this;
	let clazz = "";
	if(!this.getVisible()) {
	    clazz+=' ' + CLASS_LEGEND_LABEL_INVISIBLE;
	}
	if(this.highlighted) {
	    clazz+= ' ' + CLASS_LEGEND_LABEL_HIGHLIGHT;
	}

	html+=HU.open('div',[ATTR_ID,this.domId(ID_GLYPH_LEGEND),ID_GLYPH_ID,this.getId(),
			     ATTR_CLASS,HU.classes(CLASS_LEGEND_ITEM,clazz)]);
	html+=HU.div([ATTR_STYLE,'display: flex;'],
		     HU.div([ATTR_STYLE,'margin-right:4px;'],block.header)+
		     HU.div([ATTR_STYLE,'width:80%;'], label[0])+
		     HU.div([],label[1]));

	html+=HU.div([ATTR_CLASS,'imdv-legend-body'],block.body);
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
    canEdit: function() {
	return !this.isEphemeral;
    },
    getLegendBody:function() {
	let showInMapLegend=this.getProperty('showLegendInMap',false) && !this.display.getShowLegendInMap();
	let inMapLegend='';
	let body = '';
	
	let debug = this.getName()=='Alerts';
	let buttons = this.display.makeGlyphButtons(this,this.canEdit(),this.getName()=='Alerts');

	if(this.isMap() && this.getProperty('showFeaturesTable',true))  {
	    this.showFeatureTableId = HU.getUniqueId('btn');
	    if(buttons!=null) buttons = HU.space(1)+buttons;
	    buttons =  HU.span([ATTR_ID,this.showFeatureTableId,ATTR_TITLE,'Show features table',
				ATTR_CLASS,CLASS_CLICKABLE],
			       HU.getIconImage('fas fa-table',[],BUTTON_IMAGE_ATTRS)) +buttons;
	}

	if(this.attrs.entryId) {
	    if(buttons!=null) buttons = HU.space(1)+buttons;
	    url = RamaddaUtils.getEntryUrl(this.attrs.entryId);
	    buttons = HU.href(url,HU.getIconImage('fas fa-home',[],BUTTON_IMAGE_ATTRS),['target','_entry',ATTR_TITLE,'View entry',
											ATTR_CLASS,CLASS_CLICKABLE]) +buttons;
	}	
	if((!this.display.canEdit() && !this.getProperty('showButtons',true))) {
	    buttons = '';
	}

	if(buttons!='')
	    body+=HU.div([ATTR_CLASS,CLASS_LEGEND_OFFSET],buttons);
	//	    body+=HU.center(buttons);
	if(Utils.stringDefined(this.attrs[ID_LEGEND_TEXT])) {
	    let text = this.attrs[ID_LEGEND_TEXT].replace(/\n/g,'<br>');
	    body += HU.div([ATTR_CLASS,HU.classes(CLASS_LEGEND_OFFSET,'imdv-legend-text')],text);
	}

	let boxStyle = 'display:inline-block;width:14px;height:14px;margin-right:4px;';
	let legend = '';
	let styleLegend='';
	if(this.isMap()) {
	    if(!this.mapLoaded) {
		if(this.isVisible()) 
		    body += HU.div([ATTR_CLASS,CLASS_LEGEND_INNER],'Loading...');
		return body;
	    }

	    this.getStyleGroups().forEach((group,idx)=>{
		styleLegend+=HU.openTag(TAG_TABLE,[ATTR_WIDTH,'100%']);
		styleLegend+= HU.openTag(TAG_TR,[ATTR_TITLE,this.display.canEdit()?'Select style':'',
					       ATTR_CLASS,HU.classes(CLASS_IMDV_STYLEGROUP,(this.display.canEdit()?CLASS_CLICKABLE:'')),'index',idx]);
		let style = boxStyle + this.getLegendStyle(group.style);
		styleLegend+=HU.tag(TAG_TD,[ATTR_WIDTH,'18px'],
				    HU.div([ATTR_STYLE, style]));
		styleLegend +=HU.tag(TAG_TD,[], group.label);
		styleLegend+='</tr>';
		styleLegend+='</table>'
	    });


	    if(styleLegend!='') {
		styleLegend=HU.div([ATTR_ID,'glyphstyle_' + this.getId()], styleLegend);
		if(showInMapLegend)
		    inMapLegend+=styleLegend;
		else
		    legend+=styleLegend;
	    }

	}
	//true=>forLegend
	let mapStyleRules;
	if((mapStyleRules=this.getMapStyleRules(true)).length>0) {
	    let rulesLegend = '';
	    let lastProperty='';
	    let ruleCnt = 0;
	    mapStyleRules.forEach(rule=>{
		if(rule.type=='use') return;
		if(!Utils.stringDefined(rule.property)) return;
		let propOp = rule.property+rule.type;
		if(lastProperty!=propOp) {
		    if(rulesLegend!='') rulesLegend+='</table>';
		    let type = rule.type;
		    if(type=='==') type='=';
		    type = type.replace(/</g,'&lt;').replace(/>/g,'&gt;');
		    rulesLegend+= HU.b(this.makeLabel(rule.property,true))+' ' +type+'<br><table width=100%>\n';
		}
		lastProperty  = propOp;
		let label = rule.value;
		let info = this.getFeatureInfo(rule.property);
		if(info) label = info.getValueLabel(rule.value);

		label   = HU.span([ATTR_STYLE,'font-size:9pt;'],label);
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
		ruleCnt++;
		let style = boxStyle +this.getLegendStyle(styleObj);
		let legendContents = null;
		//If there is a pointRadius and a graphic then show the graphic in the legend
		if(Utils.isDefined(styleObj.pointRadius) && 
		   Utils.stringDefined(styleObj.externalGraphic??this.style.externalGraphic)) {
		    legendContents =
			HU.image(styleObj.externalGraphic??this.style.externalGraphic,[ATTR_WIDTH,'14px']);
		}
		let div=legendContents??HU.div([ATTR_CLASS,'circles-1',ATTR_STYLE,style]);
		let item = HU.tr([],
				 HU.td([ATTR_WIDTH,'16px'], div) +
				 HU.td([],label));
		rulesLegend+=HU.div([],item);
	    });
	    if(!ruleCnt) rulesLegend=null;
	    if(Utils.stringDefined(rulesLegend)) {
		rulesLegend+= '</table>';
		legend+=rulesLegend;
	    }
	}


	if(legend!='') {
	    if(showInMapLegend)
		inMapLegend+=legend;
	    else
		body+=HU.toggleBlock(HU.span([ATTR_TITLE,'Legend'],HU.getIconImage('fas fa-list',null,[ATTR_STYLE,HU.css("font-size","8pt")])),
				     /*'Legend',*/legend,true);
	}


	let showAnimation = false;
	if(this.isMapServer() && this.getDatacubeVariable() && this.getDatacubeVariable().dims && this.getDatacubeVariable().shape && this.getDatacubeAttr('time_coverage_start')) {
	    let v = this.getDatacubeVariable();
	    body+='Time: ' + HU.span([ATTR_ID,this.domId('time_current')],this.getCurrentTimeStep()??'')+'<br>';
	    let idx=v.dims.indexOf('time');
	    let numTimes = v.shape[idx];
	    let start =new Date(v.attrs.time_coverage_start);
	    let end =new Date(v.attrs.time_coverage_end);
	    let value = end.getTime();
	    if(this.attrs.currentTimeStep) {
		value = new Date(this.attrs.currentTimeStep).getTime();
	    }

	    let slider = 
		HU.div([ATTR_TITLE,'Set time','slider-min',start.getTime(),'slider-max',end.getTime(),'slider-value',value,
			ID,this.domId('time_slider'),ATTR_CLASS,'ramadda-slider',STYLE,HU.css('display','inline-block',ATTR_WIDTH,'90%')],'');

	    let anim = HU.join([
		['Settings','fa-cog','settings'],
		['Go to start','fa-backward','start'],
		['Step backward','fa-step-backward','stepbackward'],
		['Play','fa-play','play'],
		['Step forward','fa-step-forward','stepforward'],
		['Go to end','fa-forward','end']
	    ].map(t=>{
		return HU.span([ATTR_CLASS,HU.classes('imdv-time-anim',CLASS_CLICKABLE),
				ATTR_TITLE,t[0],'action',t[2]],HU.getIconImage(t[1]));
	    }),HU.space(2));

	    if(this.getProperty('showAnimation',true)) {
		showAnimation  = true;
		body+=HU.center(anim);
		body+=slider;
		let fstart = this.formatDate(start);
		let fend = this.formatDate(end);
		body+=HU.leftRightTable(HU.span([ATTR_ID,this.domId('time_min')],fstart),
					HU.span([ATTR_ID,this.domId('time_max')],fend));
	    }
	}



	if(this.isMapServer() || Utils.stringDefined(this.style.imageUrl) || this.imageLayers || this.image) {
	    let v = (this.imageLayers||this.isImage())?this.style.imageOpacity:this.style.opacity;
	    if(!Utils.isDefined(v)) v = 1;
	    if(showAnimation)
		body+='Opacity:';
	    body += 
		HU.center(
		    HU.div([ATTR_TITLE,'Set image opacity','slider-min',0,'slider-max',1,'slider-value',v,
			    ID,this.domId('image_opacity_slider'),ATTR_CLASS,'ramadda-slider',STYLE,HU.css('display','inline-block',ATTR_WIDTH,'90%')],''));
	}

	if(this.display.canEdit() && (this.image || Utils.stringDefined(this.style.imageUrl))) {
/*
	    body+='Rotation:';
	    body += HU.center(
		HU.div([ATTR_TITLE,'Set image rotation','slider-min',-360,'slider-max',360,'slider-value',this.style.rotation??0,
		ID,this.domId('image_rotation_slider'),ATTR_CLASS,'ramadda-slider',STYLE,HU.css('display','inline-block',ATTR_WIDTH,'90%')],''));
		*/
	}

	let item  = (content,checkInMap,addDecoration) => {
	    if(checkInMap && showInMapLegend) {
		if(addDecoration && Utils.stringDefined(content)) {
		    content = HU.hbox([this.getDecoration(true),content]);
		}
		inMapLegend+=HU.div([ATTR_STYLE,'max-width:200px'],content);
	    } else {
		if(Utils.stringDefined(content)) {	
		    body+=HU.div([ATTR_CLASS,'imdv-legend-body-item'], content);
		}
	    }
	};


	let colorTableLegend ='';
	body+=HU.div([ATTR_ID,this.domId('legendcolortableprops')]);
	colorTableLegend+=HU.div([ATTR_ID,this.domId('legendcolortable_fill')]);
	colorTableLegend+=HU.div([ATTR_ID,this.domId('legendcolortable_stroke')]);	
	colorTableLegend+=HU.div([ATTR_ID,this.domId(ID_MAPLEGEND)]);
	if(showInMapLegend)
	    inMapLegend+=colorTableLegend;
	else
	    body+=colorTableLegend;

	//Put the placeholder here for map filters
	body+=HU.div([ATTR_ID,this.domId(ID_MAPFILTERS)]);

	if(this.type==GLYPH_LABEL && this.style.label) {
	    item(this.style.label.replace(/\"/g,"\\"));
	}
	if(this.getProperty('showMeasures',true) && !this.isIsoline()) {
	    let distances = this.display.getDistances(this.getGeometry(),this.getType());
	    item(distances,true,true);
	}
	if(this.isMultiEntry()) {
//	    item(HU.div([ATTR_ID,this.domId('multientry')]));
	}

	if(this.isShape()) {
	    item('',true,true);
	}
	if(Utils.stringDefined(this.style.imageUrl)) {
	    let filter = this.getStyleFromTree('imagefilter');
	    item(HU.center(HU.href(this.style.imageUrl,HU.image(this.style.imageUrl,[ATTR_STYLE,HU.css('margin-bottom','4px','border','1px solid #ccc',ATTR_WIDTH,'150px','filter',filter??'')]),['target','_image'])));
	}
	if(Utils.stringDefined(this.style.legendUrl)) {
	    item(HU.center(HU.href(this.style.legendUrl,HU.image(this.style.legendUrl,[ATTR_STYLE,HU.css('margin-bottom','4px','border','1px solid #ccc',ATTR_WIDTH,'150px')]),['target','_image'])));
	}


	if(this.isRoute()) {
	    if(this.attrs.instructions && this.attrs.instructions.length>0) {
		let instr = '';
		this.attrs.instructions.forEach(step=>{
		    let title = '';
		    let attrs = [];
		    if(step.lat) {
			attrs.push(ATTR_TITLE,'Click to view',ATTR_CLASS,HU.classes('imdv-route-step',CLASS_CLICKABLE),
				   'lat',step.lat,
				   'lon',step.lon);
		    } else {
			attrs.push(ATTR_CLASS,'imdv-route-step');
		    }
		    instr+=HU.div(attrs, step.instr);
		});
		body+=HU.center(HU.b('Directions')) +
		    HU.div([ATTR_STYLE,'max-height:200px;overflow-y:auto;'],instr);
	    }
	}
	




	this.jq('maplegend').remove();
	if(inMapLegend!='') {
	    inMapLegend=
		HU.div([ATTR_TITLE,this.getName(),ATTR_STYLE,'white-space:nowrap;max-width:150px;overflow-x:hidden'],HU.b(this.getName())) +
		inMapLegend;

	    inMapLegend = HU.div([ATTR_STYLE,'border-bottom:var(--basic-border);padding:4px;',ATTR_ID,this.domId('maplegend')], inMapLegend);
	    this.display.addToMapLegend(this,inMapLegend);
	}
	



	return body;
    },
    canDrop: function() {
	if(this.getParentGlyph()) {
	    if(!this.isGroup() && !this.getParentGlyph().isGroup()) {
		return false;
	    }
	} 
	return true;
    },

    canDrag: function() {
	if(this.getParentGlyph() && !this.getParentGlyph().isGroup()) {
	    return false;
	}
	return true;
    },
    
    initLegend:function() {
	let _this = this;
	if(this.canHaveChildren()) {
	    if(this.getProperty(PROP_LAYERS_ANIMATION_SHOW)) {
		this.jq(PROP_LAYERS_ANIMATION_PLAY).click(()=>{
		    this.checkLayersAnimation();
		});
	    }
	    this.checkLayersAnimation();
	}	    


	let steps = this.getLegendDiv().find('.imdv-route-step');
	steps.click(function(){
	    let lon = $(this).attr('lon');
	    let lat = $(this).attr('lat');
	    if(!Utils.isDefined(lat)) return;
	    steps.removeClass('imdv-route-step-on');
	    $(this).addClass('imdv-route-step-on');
	    if(_this.stepMarker) {
		_this.display.removeFeatures([_this.stepMarker]);
	    }

	    _this.getMap().setCenter(MapUtils.createLonLat(lon,lat));
	    _this.stepMarker = _this.display.makeFeature(_this.getMap(),'OpenLayers.Geometry.Point',
							 {externalGraphic:'/emojis/1f699.png',
							  pointRadius:12},
							[lat,lon]);
		
	    _this.display.addFeatures([_this.stepMarker]);
	});


	if(this.imageLayers) {
	    this.getLegendDiv().find('.imdv-imagelayer-checkbox').change(function() {
		let visible = $(this).is(':checked');
		let id = $(this).attr('imageid');
		let obj = _this.imageLayerMap[id]??{id:id};
		_this.setImageLayerVisible(obj,visible);
	    });
	}


	if(this.display.canEdit()) {
	    let label = this.getLegendDiv().find('.' + CLASS_LEGEND_LABEL);
	    //Set the last dropped time so we don't also handle this as a setVisibility click
	    let notify = ()=>{_this.display.setLastDroppedTime(new Date());};
	    if(this.canDrag()) {
		this.getLegendDiv().draggable({
		    handle:label,
		    cursor: "crosshair",
		    start: notify,
		    drag: notify,
		    stop: notify,
		    containment:this.display.domId(ID_LEGEND),
		    revert: true
		});
	    }
	    if(this.canDrop()) {
		this.display.makeLegendDroppable(this,label,notify);
	    } 
	    let items = this.jq(ID_LEGEND).find('.' + CLASS_LEGEND_LABEL);
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
		    $(this).html(HU.getIconImage(icon_play));
		} else {
		    $(this).html(HU.getIconImage(icon_stop));
		    let stepTime = () =>{
			let current = +slider.slider('value');
			current = current+(_this.attrs.timeAnimationStep??1)*step;
			change(current);
			//			console.log("current time: " +new Date(current) +' step:' + _this.attrs.timeAnimationStep);
			if(current>=max) {
			    $(this).html(HU.getIconImage(icon_play));
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
							   [ATTR_ID,_this.domId('timeanimation_pause'),'size','4']) +' (ms)');
		html+=HU.formEntry('Time Step:', HU.input("",_this.attrs.timeAnimationStep??1,
							  [ATTR_ID,_this.domId('timeanimation_step'),'size','4']) +' Time steps to skip');		
		html+='</table>';

		let buttons = HU.buttons([
		    HU.div([ATTR_CLASS,'ramadda-button-ok ramadda-button-apply display-button'], 'Apply'),
		    HU.div([ATTR_CLASS,'ramadda-button-ok display-button'], 'OK'),
		    HU.div([ATTR_CLASS,'ramadda-button-cancel display-button'], 'Cancel')]);
		html+=buttons;
		html = HU.div([ATTR_STYLE,'margin:6px;'], html);
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
	
	if(this.haveChildren()) {
	    this.jq('searchtext').change(function(){
		let text=$(this).val();
		_this.setProperty('searchtext', text);
		_this.applyChildren(child=>{
		    child.applyFeatureFilters();
		},true);
	    });
	}


	if(!this.isGroup()) {
	    this.makeFeatureFilters();
	}
	if(this.isMap() && this.mapLoaded) {
	    let addColor= (obj,prefix, strings) => {
		if(obj && Utils.stringDefined(obj.property)) {
		    let div = this.getColorTableDisplay(obj.colorTable,obj.min,obj.max,true,obj.isEnumeration, strings);
		    let html = HU.b(HU.center(this.makeLabel(obj.property,true)));
		    if(obj.isEnumeration) {
			html+=HU.div([ATTR_STYLE,'max-height:150px;overflow-y:auto;'],div);
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
			    html = HU.div([ATTR_STYLE,'max-height:200px;overflow-y:auto;margin:2px;'], html);
			    let dialog = HU.makeDialog({content:html,title:HU.div([ATTR_STYLE,'margin-left:20px;margin-right:20px;'], _this.makeLabel(obj.property,true)+' Legend'),header:true,my:"left top",at:"left bottom",draggable:true,anchor:$(this)});
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
		let menu = HU.select('',[ATTR_ID,this.domId('colortableproperty')],
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
	this.applyChildren(mapGlyph=>{mapGlyph.initLegend();});
    },

    convertPopupText:function(text) {
	if(this.getImage()) {
	    text = text.replace(/\${image}/g,HU.image(this.style.imageUrl,[ATTR_WIDTH,'200px']));
	}
	return text;
    },
    

    canHaveChildren:function() {
	return this.isGroup() || this.isMultiEntry();
    },
    isMarker:function() {
	return this.getType() ==GLYPH_MARKER;
    },
    isGroup:function() {
	return this.getType() ==GLYPH_GROUP;
    },
    isEntry:function() {
	return this.getType() ==GLYPH_ENTRY;
    },
    loadEntry: function() {
	if(!this.attrs.entryId) return;
	let callback = (entry)=>{
	    this.setEntry(entry);
	};
	getRamadda().getEntry(this.attrs.entryId, callback);
    },

    setEntry:function(entry) {		
	this.entry = entry;
	if(!this.isEntry()) return;
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

	this.applyDataIcon();
	this.applyMapStyle();
	this.display.redraw(this);
	this.display.makeLegend();
	//And call getBounds so the bounds object gets cached for later use on reload
	this.getBounds();
    },
    isImage:function() {
	return this.getType() ==GLYPH_IMAGE;
    },    
    isMap:function() {
	return this.getType()==GLYPH_MAP;
    },
    isIsoline:function() {
	return this.getType()==GLYPH_ISOLINE;
    },    
    isRoute:function() {
	return this.getType()==GLYPH_ROUTE;
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
    getShowDataIcons:function() {
	if(this.attrs[ID_SHOWDATAICONS] ===true)
	    this.attrs[ID_SHOWDATAICONS] = 'yes';
	else if(this.attrs[ID_SHOWDATAICONS] ===false)
	    this.attrs[ID_SHOWDATAICONS] = 'no';	
	if(this.attrs[ID_SHOWDATAICONS] ==='yes') return true;
	if(this.attrs[ID_SHOWDATAICONS] ==='no') return false;	
	if(this.getParentGlyph()) {
	    return this.getParentGlyph().getShowDataIcons();
	}
	return false;
    },
    setShowDataIcons:function(v) {
	this.attrs[ID_SHOWDATAICONS] = v;
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
		if(mapLayer) {
		    this.attrs.name = mapLayer.name;
		    this.style.legendUrl = mapLayer.opts.legend;
		    if(Utils.stringDefined(mapLayer.opts.attribution) &&
		       !Utils.stringDefined(this.attrs[ID_LEGEND_TEXT])) {
			this.attrs[ID_LEGEND_TEXT] = mapLayer.opts.attribution;
		    }
		}
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
    getCanSelect:function() {
	if(!Utils.isDefined(this.attrs.canSelect)) return true;
	return this.attrs.canSelect;
    },
    setMapLayer:function(mapLayer) {
	this.mapLayer = mapLayer;
	if(mapLayer) {
	    mapLayer.canSelect=this.getCanSelect();
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
    checkMapLayer:function(andZoom,force) {
	//Only create the map if we're visible
	if(!this.isMap() || !this.isVisible()) return;
	if(this.mapLayer!=null && force) {
	    this.display.getMap().removeLayer(this.mapLayer);
	    this.mapLayer = null;
	}


	if(this.mapLayer==null) {
	    if(!Utils.isDefined(andZoom)) {
		//Not sure why we do this
		//		andZoom = true;
	    }
	    if(this.attrs.entryType=='geo_imdv') {
		this.dontSaveChildren=true;
		if(this.mapLoaded) return;
		let url =Ramadda.getUrl("/entry/get?entryid=" + this.attrs.entryId+"&fileinline=true");
		let finish = (data)=>{
		    this.mapLoaded = true;
		    this.makeLegend();
		};
		this.display.loadIMDVUrl(url,finish,this);
		return
	    }
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

	    if(!this.mapServerLayer) return;

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

	if(this.isMultiEntry()) {
	    let childIcon = this.style.childIcon;
	    if(Utils.stringDefined(childIcon)) {
		this.applyChildren(child=>{
		    child.style.externalGraphic = childIcon;
		    child.attrs.icon = childIcon;		    
		});
	    }
	}

	if(this.isMap()) {
	    this.attrs.subsetSkip = jqid(this.domId('subsetSkip')).val();
	    this.attrs.subsetReverse = jqid(this.domId('subsetReverse')).is(':checked');
	    this.attrs.subsetSimplify= jqid(this.domId('subsetSimplify')).is(':checked');
	    this.setMapPointsRange(jqid('mappoints_range').val());
	    this.setMapLabelsTemplate(jqid('mappoints_template').val());
	    this.attrs.declutter_labels=this.jq('declutter_labels').is(':checked');
	    ['labels_maxlength','labels_maxlinelength',
	     'declutter_pixelsperline','declutter_pixelspercharacter','declutter_padding'].forEach(id=>{
		 let v=this.jq(id).val();
		 if(v) v=v.trim();
		 this.attrs[id] = v;
	     });


	    
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
	    return  {
		property:this.jq(prefix +'colorby_property').val(),
		min:parseFloat(this.jq(prefix +'colorby_min').val()),
		max:parseFloat(this.jq(prefix +'colorby_max').val()),		
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
	let attrs = [TITLE,id,ATTR_STYLE,'margin-right:4px;',"colortable",id]
	//	if(ct.colors.length>20)   attrs.push(STYLE,HU.css(ATTR_WIDTH,'400px'));
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
		html+=HU.image(url,[ATTR_CLASS,CLASS_CLICKABLE,'colorbar',name, 'height','20px',ATTR_WIDTH,'256px',ATTR_TITLE,name]);
		html+='<br>';
		if(name==currentColorbar) {
		    image = c[1];
		}
	    });
	});
	let url = image?('data:image/png;base64,' + image):null;
	this.jq('colortableproperties').html(HU.div([ATTR_ID,this.domId('colortable'),ATTR_CLASS,CLASS_CLICKABLE,ATTR_TITLE,'Click to select color bar'],
						    url? HU.image(url,['height','20px',ATTR_WIDTH,'256px']):'No image'));

	let _this = this;
	html = HU.div([ATTR_STYLE,'margin:8px;max-height:200px;overflow-y:auto;'], html);
	this.jq('colortable').click(function() {
	    let colorSelect = HU.makeDialog({content:html,
					     my:'left top',
					     at:'left bottom',
					     anchor:$(this)});
	    colorSelect.find(TAG_IMG).click(function() {
		_this.currentColorbar = $(this).attr('colorbar');
		colorSelect.remove();
		_this.initColorTables(_this.currentColorbar);
	    });
	});
    },
    initPropertiesComponent: function(dialog) {
	let props = [
	    [ID_DATAICON_FIELDS,  DEFAULT_DATAICON_FIELDS],
	    [ID_DATAICON_INIT_FIELD,DEFAULT_DATAICON_FIELD],
	    [ID_DATAICON_WIDTH,'300'],
	    [ID_DATAICON_HEIGHT,'100'],	     
	    [ID_DATAICON_SIZE,'40'],
	    [ID_DATAICON_LABEL,''],
	    [ID_DATAICON_PROPS,DEFAULT_DATAICON_PROPS,''],
	    [ID_DATAICON_MARKERS,DEFAULT_DATAICONS]];

	this.jq('applyentrydataicon').button().click(()=>{
	    let propsLine = '';
	    let markers='';
	    Utils.split(this.transientProperties.mapglyphs,'\n',true,true).forEach(line=>{
		if(line.startsWith("#")) return;
		if(line.startsWith('props:')) {
		    propsLine+=line.substring('props:'.length);
		} else {
		    markers+=line+'\n';
		}
	    });
	    let props = {};
	    this.parseDataIconProps(props,propsLine);
	    if(Utils.isDefined(props.canvasWidth)) {
		this.jq(ID_DATAICON_WIDTH).val(props.canvasWidth);
		delete props.canvasWidth;
	    }
	    if(Utils.isDefined(props.canvasHeight)) {
		this.jq(ID_DATAICON_HEIGHT).val(props.canvasHeight);
		delete props.canvasHeight;
	    }
	    if(Utils.isDefined(props.iconSize)) {
		this.jq(ID_DATAICON_SIZE).val(props.iconSize);
		delete props.iconSize;
	    }
	    let tmp = '';
	    Object.keys(props).forEach((key,idx)=>{
		if(idx>0) tmp+=',';
		tmp+=key+':'+props[key];
	    });

	    this.jq(ID_DATAICON_PROPS).val(tmp);
	    this.jq(ID_DATAICON_MARKERS).val(markers);	    
	});
	this.jq('dataicon_add_default').button().click(() =>{
	    props.forEach(tuple=>{	     	      
		 if(!Utils.stringDefined(this.jq(tuple[0]).val()))
		    this.jq(tuple[0]).val(tuple[1]);
	     });
	});
	this.jq('dataicon_clear_default').button().click(() =>{
	    props.forEach(tuple=>{	     	      
		this.jq(tuple[0]).val('');
	     });
	});




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
		wrapper.html(HU.input("",value,[ATTR_ID,'mapvalue_' + index,'size','15']));
		tt=info.min +" - " + info.max;
	    }  else  if(info.samples.length) {
		tt = Utils.join(info.getSamplesLabels(), ", ");
		if(info.isEnumeration()) {
		    wrapper.html(HU.select("",[ATTR_ID,'mapvalue_' + index],info.samples,value,20));
		} else {
		    wrapper.html(HU.input("",value,[ATTR_ID,'mapvalue_' + index,'size','15']));
		}
	    }
	    jqid('mapvalue_' + index).attr(ATTR_TITLE,tt);
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
		table = HU.openTag(TAG_TABLE,[ATTR_ID,id,'table-ordering','true','table-searching','true','table-height','400px',
					    ATTR_CLASS,'stripe rowborder ramadda-table'])
		table+='<thead><tr>';
		stats = [];
		columns.forEach((column,idx)=>{
		    table+=HU.tag('th',[],column.getLabel(true));
		    stats.push({total:0,count:0,min:0,max:0});
		});
		table+=HU.close(TAG_TR,'thead','tbody');
	    }
	    this.featureTableMap[rowIdx] =feature;
	    table+=HU.openTag(TAG_TR,[ATTR_TITLE,'Click to zoom to','featureidx', rowIdx,
				    ATTR_CLASS,HU.classes('imdv-feature-table-row',CLASS_CLICKABLE)]);
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
		table+=HU.tag(TAG_TD,[ATTR_STYLE,isNumber?'text-align:right':'','align',isNumber?'right':'left'],sv);
	    });
	});
	if(stats) {
	    table+='<tfoot><tr>';
	    let fmt = (label,amt) =>{
		return HU.tr(HU.td([ATTR_STYLE,'text-align:right','align','right'],HU.b(label)) +
			     HU.td([ATTR_STYLE,'text-align:right','align','right'],Utils.formatNumberComma(amt)));
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
	    html += HU.div([ATTR_STYLE,'width:200px;margin:10px;'],'No data');
	} else {
	    html +=  
		HU.div([ATTR_STYLE,HU.css('margin','5px','max-width','1000px','overflow-x','scroll')],table);
	    html = HU.div([ATTR_STYLE,'margin:5px;'], html);
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
	let html =  HU.div([ATTR_ID,this.domId('downloadfeatures'),ATTR_CLASS,CLASS_CLICKABLE],'Download Table');
	html+=HU.div([ATTR_ID,this.domId('featurestable')]);
	html = HU.div([ATTR_STYLE,'margin:10px;'], html);
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
	if(!url.startsWith('#')) url = '/userguide/imdv/' + url;
	return HU.href(Ramadda.getUrl(url),HU.getIconImage(icon_help) +' ' +(label??'Help'),['target','_help']);
    },
    getCentroid: function() {
	if(this.features && this.features.length) {
	    return  this.features[0].geometry.getCentroid(true);
	}
	if(this.extraFeatures && this.extraFeatures.length) {
	    return  this.extraFeatures[0].geometry.getCentroid(true);
	}

	return null;
    },
    makeGroupRoute: function() {
	let mode = this.display.jq('routetype').val()??'car';
	let provider = this.display.jq('routeprovider').val();
	let doSequence = this.display.jq('routedosequence').is(':checked');
	let pts = [];
	this.applyChildren(child=>{
	    if(!child.isVisible()) return;
	    let centroid  = child.getCentroid();
	    if(!centroid) return;
	    var lonlat = this.getMap().transformProjPoint(centroid)
	    pts.push(lonlat);
	});

	if(pts.length==0) {
	    alert('No points to make route from');
	    return;
	}
	this.display.createRoute(provider,mode,pts,{
	    doSequence:doSequence});
    },
    getPropertiesComponent: function(content) {
	if(!this.canDoMapStyle()) return;
	let attrs = this.getExampleMapLayer()?.features[0].attributes ?? {};
	let featureInfo = this.featureInfo = this.getFeatureInfoList();
	let keys  = Object.keys(featureInfo);
	let numeric = featureInfo.filter(info=>{return info.isNumeric();});
	let enums = featureInfo.filter(info=>{return info.isEnumeration();});
	let colorBy = '';
	colorBy+=HU.leftRightTable(HU.checkbox(this.domId('fillcolors'),[ATTR_ID,this.domId('fillcolors')],
					       this.attrs.fillColors,'Fill Colors'),
				   this.getHelp('mapfiles.html#mapstylerules'));

	numeric = featureInfo;
	if(numeric.length) {
	    let numericProperties=Utils.mergeLists([['','Select']],numeric.map(info=>{return {value:info.property,label:info.getLabel()};}));
	    let mapComp = (obj,prefix) =>{
		let comp = '';
		comp += HU.div([ATTR_CLASS,'formgroupheader'], 'Map value to ' + prefix +' color')+ HU.formTable();
		comp += HU.formEntry('Property:', HU.select('',[ATTR_ID,this.domId(prefix+'colorby_property')],numericProperties,obj.property) +HU.space(2)+ HU.b('Range: ') + HU.input('',obj.min??'', [ATTR_ID,this.domId(prefix+'colorby_min'),'size','6',ATTR_TITLE,'min value']) +' -- '+    HU.input('',obj.max??'', [ATTR_ID,this.domId(prefix+'colorby_max'),'size','6',ATTR_TITLE,'max value']));
		comp += HU.hidden('',obj.colorTable||'blues',[ATTR_ID,this.domId(prefix+'colorby_colortable')]);
		comp+=HU.formEntry('Color table:', HU.div([ATTR_ID,this.domId(prefix+'colorby_colortable_label')])+
				   Utils.getColorTablePopup(null,null,'Select',true,'prefix',prefix));
		comp+=HU.close(TAG_TABLE);
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
	rulesTable+=HU.tr([],HU.tds([ATTR_STYLE,'font-weight:bold;'],['Property','Operator','Value',ATTR_STYLE]));
	let rules = this.getMapStyleRules(true);
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
		valueInput = HU.select('',[ATTR_ID,'mapvalue_' + index],info.getSamplesForMenu(),value,20); 
		
	    } else {
		valueInput = HU.input('',value,[ATTR_ID,'mapvalue_' + index,'size','15']);
	    }
	    let propSelect =HU.select('',[ATTR_ID,'mapproperty_' + index,'mapproperty_index',index],properties,rule.property);
	    let opSelect =HU.select('',[ATTR_ID,'maptype_' + index],operators,rule.type);	    
	    valueInput =HU.span([ATTR_ID,'mapvaluewrapper_' + index],valueInput);
	    let s = Utils.stringDefined(rule.style)?rule.style:'';
	    let styleInput = HU.textarea('',s,[ATTR_ID,'mapstyle_' + index,'rows','3','cols','30',ATTR_TITLE,styleTitle]);
	    rulesTable+=HU.tr(['valign','top'],HU.tds([],[propSelect,opSelect,valueInput,styleInput]));
	}
	rulesTable += '</table>';
	let table = HU.b('Style Rules')+HU.div([ATTR_CLASS,'imdv-properties-section'], rulesTable);
	content.push({header:'Style Rules', contents:colorBy+table});


	let mapPointsRange = HU.leftRightTable(HU.b('Visiblity limit: ') + HU.select('',[ID,'mappoints_range'],this.display.levels,this.getMapPointsRange()??'',null,true) + ' '+
					       HU.span([ATTR_CLASS,'imdv-currentlevellabel'], '(current level: ' + this.display.getCurrentLevel()+')'),
					       this.getHelp('mapfiles.html#map_labels'));
	let mapPoints = HU.textarea('',this.getMapLabelsTemplate()??'',[ATTR_ID,'mappoints_template','rows','6','cols','40',ATTR_TITLE,'Map points template, e.g., ${code}']);

	let propsHelp =this.display.makeSideHelp(helpLines,'mappoints_template',{prefix:'${',suffix:'}'});
	mapPoints = HU.hbox([mapPoints,HU.space(2),'Add property:' + propsHelp]);


	let styleGroups =this.getStyleGroups();
	let styleGroupsUI = HU.leftRightTable('',
					      this.getHelp('mapfiles.html#adding_a_map'));
	styleGroupsUI+=HU.openTag(TAG_TABLE,[ATTR_WIDTH,'100%']);
	styleGroupsUI+=HU.tr([],HU.tds([],
				       ['Group','Fill','Opacity','Stroke',ATTR_WIDTH,'Pattern','Features']));
	for(let i=0;i<20;i++) {
	    let group = styleGroups[i];
	    let prefix = 'mapstylegroups_' + i;
	    styleGroupsUI+=HU.tr([],HU.tds([],[
		HU.input('',group?.label??'',[ATTR_ID,prefix+'_label','size','10']),
		HU.input('',group?.style.fillColor??'',[ATTR_CLASS,'ramadda-imdv-color',ATTR_ID,prefix+'_fillcolor','size','6']),
		HU.input('',group?.style.fillOpacity??'',[ATTR_TITLE,'0-1',ATTR_ID,prefix+'_fillopacity','size','2']),		
		HU.input('',group?.style.strokeColor??'',[ATTR_CLASS,'ramadda-imdv-color',ATTR_ID,prefix+'_strokecolor','size','6']),
		HU.input('',group?.style.strokeWidth??'',[ATTR_ID,prefix+'_strokewidth','size','6']),
		this.display.getFillPatternSelect(prefix+'_fillpattern',group?.style.fillPattern??''),
		Utils.join(group?.indices??[],',')]));
	}
	styleGroupsUI += HU.closeTag(TAG_TABLE);
	styleGroupsUI = HU.div([ATTR_STYLE,HU.css('max-height','150px','overflow-y','auto')], styleGroupsUI);

	//Don't add style groups if it is a group, just map glyphs
	if(!this.isGroup()) {
	    content.push({header:'Style Groups',contents:styleGroupsUI});
	}


	let input = (id,label,dflt) =>{
	    return SPACE2 + HU.b(label+': ')+
		HU.input('', this.attrs[id]??'', ['placeholder',dflt,ATTR_ID,this.domId(id),ATTR_SIZE,'5']);
	}
	let space =  HU.div([ATTR_STYLE,'margin-top:5px;']);
	let extra = '';
	extra+=input('labels_maxlength','Max Length','100');
	extra+=input('labels_maxlinelength','Max Line Length',15);
	extra+=space;
	extra+= HU.checkbox(this.domId('declutter_labels'),
			    [ATTR_ID,this.domId('declutter_labels')],
			    this.getDeclutterLabels(),'Declutter Labels');
	extra+=space;
	extra+=input('declutter_padding','Padding',2);				
	extra+=input('declutter_pixelsperline','Pixels/Line',10);
	extra+=input('declutter_pixelspercharacter','Pixels/Character',4);
	let labelsHtml =mapPointsRange+ 
	    HU.b('Label Template:')+ '<br>' +    
	    mapPoints +
	    extra;
	    
	content.push({header:'Labels',
		      contents:labelsHtml});

	if(this.isMap()) {
	    let subset = HU.b('Feature Subset')+'<br>';
	    subset += '<table>';
	    subset+=HU.formEntry('Skip:',
 				 HU.input('',this.attrs.subsetSkip??'0',
					  [ATTR_ID,this.domId('subsetSkip'),'size','6'])+
				 ' ' +'Prunes features');
	    subset+=HU.formEntry('',
				 HU.checkbox(this.domId('subsetReverse'),
					     [ATTR_ID,this.domId('subsetReverse')],
					     this.attrs.subsetReverse,'Reverse features'));
	    subset+=HU.formEntry('',
				 HU.checkbox(this.domId('subsetSimplify'),
					     [ATTR_ID,this.domId('subsetSimplify')],
					     this.attrs.subsetSimplify,'Simplify geometry' +
					     ' ' +'(for now this sets feature.components.length=1)'));
	    subset += '</table>';	    
	    content.push({header:'Subset',
			  contents:subset});

	}



	content.push({header:'Sample Values',contents:ex});
    },
    getStyleGroups: function() {
	if(!this.attrs.styleGroups) {
	    this.attrs.styleGroups = [];
	}
	return this.attrs.styleGroups??[];
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
    hasMapFeatures: function() {
	if(!this.isMap() || !this?.getExampleMapLayer()?.features || this.getExampleMapLayer()?.features.length==0) return false;
	return true;
    },
    canDoMapStyle: function() {
	let features = this.getExampleFeatures();
	if(!features || features.length==0) return false;
	if(!features[0].attributes ||   Object.keys(features[0].attributes).length==0) {
	    return false;
	}
	return true;
    },

    getMapStyleRules(justMine) {
	let debug = false;
	if(debug)
	    console.log('getMapStyleRules:' + this.getName());
	let rules =[];
	if(this.attrs.mapStyleRules && this.attrs.mapStyleRules.length>0) {
	    rules = Utils.mergeLists(this.attrs.mapStyleRules);
	}
	if(justMine) {
	    return rules;
	}
	if(this.getParentGlyph()) {
	    if(debug)
		console.log('\tasking parent');
	    rules = Utils.mergeLists(rules,this.getParentGlyph().getMapStyleRules());
	}
	return rules;
    },
    getExampleFeatures: function() {
	return this.getExampleMapLayer()?.features;
    },

    getMapFeatures: function() {
	if(this.mapLayer) {
	    return this.mapLayer.features;
	}
	let children = 	this.getChildren();
	if(!children) {
	    return null;
	}
	let features = null;
	for(let i=0;i<children.length;i++) {
	    let childFeatures = children[i].getMapFeatures();
	    if(childFeatures) features = Utils.mergeLists(features??[],childFeatures);
	}
	return features;
    },

    getExampleMapLayer: function() {
	if(this.mapLayer) return this.mapLayer;
	let children = 	this.getChildren();
	if(!children) return null;
	for(let i=0;i<children.length;i++) {
	    let layer = children[i].getExampleMapLayer();
	    if(layer) return  layer;
	}
	return null;
    },

    getFeatureInfoList:function() {
	if(this.featureInfo) return this.featureInfo;
	let features= this.getMapFeatures();
	if(!features || features.length==0) return [];
	let attrs = features[0].attributes;
	let keys =   Object.keys(attrs);
	let _this = this;
	let first = [];		
	let middle=[];
	let last = [];
	keys.forEach(key=>{
	    let info = new FeatureInfo(this,key);
	    let _c = info.property.toLowerCase();
	    if(_c.indexOf('objectid')>=0) {
		last.push(info);
	    } else if(_c.indexOf('name')>=0) {
		first.push(info);
	    } else {
		middle.push(info);
	    }
	});


	let featureInfo =  Utils.mergeLists(first,middle,last);
	let featureInfoMap = {};

	features.forEach((f,fidx)=>{
	    featureInfo.forEach(info=>{
		info.initValues(f);
	    });
	});


	featureInfo.forEach(info=>{
	    info.finishInit();
	    featureInfoMap[info.property] = info;
	    featureInfoMap[info.id] = info;	    
	});
	this.featureInfoMap =featureInfoMap;	    
	if(!this.isGroup()) {
	    this.featureInfo =featureInfo;
	}

	return featureInfo;
    },
    getFeatureInfo:function(property) {
	this.getFeatureInfoList();
	if(this.featureInfoMap) return this.featureInfoMap[property];
	return null;
    },
    makeLabel:function(l,makeSpan) {
	let info = this.getFeatureInfo(l);
	if(info) {
	    return info.getLabel(makeSpan);
	}
	let id = Utils.makeId(l);
	let label = l;
	if(id=='shapestlength') {
	    label =  'Shape Length';
	} else 	if(this.getProperty(id+'.feature.label')) {
	    label =  this.getProperty(id+'.feature.label');
	} else {
	    label =  this.display.makeLabel(l);
	}
	if(makeSpan) label = HU.span([ATTR_TITLE,'aka:' +id], label);
	return label;
    },

    setProperty:function(key,value) {
	let newLine = key+'='+value;
	if(this.attrs.properties) {
	    let tmp = '';
	    let hadMatch =false;
	    Utils.split(this.attrs.properties,'\n',true,true).forEach(line=>{
		if(line.match('^ *' +key+' *=')) {
		    hadMatch=true;
		    line = newLine;
		}
		tmp+=line+'\n';
	    });
	    if(!hadMatch) tmp+=newLine+'\n';
	    this.attrs.properties = tmp;
	} else {
	    this.attrs.properties = newLine+'\n';
	}
	this.parsedProperties=null;
	this.display.featureChanged(true);	    
    },
    getProperty:function(key,dflt,checkParent) {
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
	if(checkParent && this.getParentGlyph()) {
	    return this.getParentGlyph().getProperty(key,dflt,true);
	}
	return this.display.getMapProperty(key,dflt);
    },
    makeFeatureFilters:function() {
	let _this = this;
	let contents = {
	    first:'',
	    sliders:'',
	    enums:'',
	    strings:'',
	    top:[]
	}
	let showTop;
	let add=(info,type,line)=>{
	    if(showTop)   contents.top.push(line);
	    else if(info.getProperty('filter.first')) contents.first+=line;
	    else contents[type]+=line;
	};
	let filters = this.attrs.featureFilters = this.attrs.featureFilters ??{};
	this.filterInfo = {};
	this.getFeatureInfoList().forEach(info=>{
	    if(!info.showFilter()) {
		return;
	    }
	    //true=> check the  glyph
	    showTop = info.getProperty('filter.top',false,true);
	    this.filterInfo[info.property] = info;
	    this.filterInfo[info.getId()] = info;	    
	    if(!filters[info.property]) filters[info.property]= {};
	    let filter = filters[info.property];
	    if(!Utils.isDefined(filter.min) || isNaN(filter.min)) filter.min = info.min;
	    if(!Utils.isDefined(filter.max) || isNaN(filter.max)) filter.max = info.max;	    
	    filter.property =  info.property;
	    let id = info.getId();
	    let label = HU.span([ATTR_TITLE,info.property],HU.b(info.getLabel()));
	    if(info.isString())  {
		filter.type="string";
		let attrs =['filter-property',info.property,ATTR_CLASS,CLASS_FILTER_STRING,ATTR_ID,this.domId('string_'+ id),'size',20];
		attrs.push('placeholder',this.getProperty(info.property.toLowerCase()+'.filterPlaceholder',''));
		let string=label+":<br>" +
		    HU.input("",filter.stringValue??"",attrs) +"<br>";
		add(info,'strings',string);
		return
	    } 
	    if(info.isEnumeration())  {
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
			HU.select("",[ATTR_STYLE,'width:90%;','filter-property',info.property,ATTR_CLASS,'imdv-filter-enum',ATTR_ID,this.domId('enum_'+ id),'multiple',null,'size',Math.min(info.samples.length,showTop?3:5)],options,filter.enumValues,50)+"<br>";
		    add(info,'enums',line);
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
		    HU.leftRightTable(HU.div([ATTR_ID,this.domId('slider_min_'+ id),
					      ATTR_CLASS,CLASS_FILTER_SLIDER_LABEL],Utils.formatNumber(filter.min??min)),
				      HU.div([ATTR_ID,this.domId('slider_max_'+ id),
					      ATTR_CLASS,CLASS_FILTER_SLIDER_LABEL],Utils.formatNumber(filter.max??max)));
		if(showTop) line = HU.div([ATTR_STYLE,HU.css(ATTR_WIDTH,'120px')], line);
		let slider =  HU.div(['slider-min',min,'slider-max',max,'slider-isint',info.isInt(),
				      'slider-value-min',filter.min??info.min,'slider-value-max',filter.max??info.max,
				      'filter-property',info.property,'feature-id',info.id,
				      ATTR_CLASS,CLASS_FILTER_SLIDER,
				      ATTR_STYLE,HU.css("display","inline-block","width","100%")],"");
		if(info.getProperty('filter.animate',false)) {
		    line+=HU.table([ATTR_WIDTH,'100%'],
				   HU.tr([],
					 HU.td([ATTR_WIDTH,'18px'],HU.span(['feature-id',info.id,
									 ATTR_CLASS,HU.classes(CLASS_FILTER_PLAY,CLASS_CLICKABLE),
									 ATTR_TITLE,'Play'],HU.getIconImage(icon_play))) +
					 HU.td([],slider)));
		} else {
		    line+=slider;
		}

		label = HU.b(label)+':';
		if(showTop)
		    line = HU.hbox([label+HU.space(1),line]);
		else 
		    line =  label+'<br>' +line;
		add(info,'sliders',line);
	    }
	});


	if(contents.sliders!='')
	    contents.sliders = HU.div([ATTR_STYLE,HU.css('margin-left','10px','margin-right','20px')],contents.sliders);
	if(contents.first!='')
	    contents.first = HU.div([ATTR_STYLE,HU.css('margin-left','10px','margin-right','20px')],contents.first);	    

	if(this.topHeaderId) {
	    jqid(this.topHeaderId).html('');
	}	
	if(contents.top.length) {
	    if(!this.topHeaderId) {
		this.topHeaderId = HU.getUniqueId('topheader');
		this.display.appendHeader(HU.div([ATTR_CLASS,'imdv-legend-top',ATTR_STYLE,HU.css('display','inline-block'),ATTR_ID,this.topHeaderId]));
	    }

	    let label =  this.getLabel({addIcon:false,forLegend:true})[0];
	    //HU.b(this.getName()+':'))
	    let top = HU.div([ATTR_STYLE,HU.css('font-weight','bold','text-align','center')], label) +HU.hbox(contents.top.map(c=>{return HU.div([ATTR_STYLE,'margin-right:15px;'], c);}));
	    jqid(this.topHeaderId).html(top);
	}


	let widgets = contents.first+contents.enums+contents.sliders+contents.strings;

	if(Utils.stringDefined(widgets) ||contents.top.length) {
	    let update = () =>{
		this.display.featureHasBeenChanged = true;
		this.applyMapStyle(true);
		if($("#"+this.zoomonchangeid).is(':checked')) {
		    this.panMapTo();
		}
		this.updateFeaturesTable();
	    };

	    let clearAll = HU.span([ATTR_STYLE,'margin-right:5px;',
				    ATTR_CLASS,HU.classes(CLASS_CLICKABLE,'imdv-legend-clearall'),
				    ATTR_TITLE,'Clear Filters'],HU.getIconImage('fas fa-eraser',null,LEGEND_IMAGE_ATTRS));

	    this.zoomonchangeid = HU.getUniqueId("andzoom");

	    widgets = HU.div([ATTR_STYLE,'padding-bottom:5px;max-height:200px;overflow-y:auto;'], widgets);
	    let filtersHeader ='';
	    if(this.getProperty('filter.zoomonchange.show',true)) {
		filtersHeader = HU.checkbox(this.zoomonchangeid,
					    [ATTR_TITLE,'Zoom on change',ATTR_ID,this.zoomonchangeid],
					    this.getZoomOnChange(),
					    HU.span([ATTR_TITLE,'Zoom on change',ATTR_STYLE,'margin-left:12px;'], HU.getIconImage('fas fa-binoculars',[],LEGEND_IMAGE_ATTRS)));
	    }
	    let filtersCount = HU.span([ATTR_ID,this.domId('filters_count')],Utils.isDefined(this.visibleFeatures)?'#'+this.visibleFeatures:'');
	    filtersHeader = HU.leftRightTable(filtersHeader, clearAll);

	    if(this.getProperty('filter.toggle.show',true)) {
		let toggle = HU.toggleBlockNew('Filters ' + filtersCount,filtersHeader + widgets,this.getFiltersVisible(),{separate:true,headerStyle:'display:inline-block;',callback:null});
		this.jq(ID_MAPFILTERS).html(HU.div([ATTR_STYLE,'margin-right:5px;'],toggle.header+toggle.body));
		HU.initToggleBlock(this.jq(ID_MAPFILTERS),(id,visible)=>{this.setFiltersVisible(visible);});
	    } else  {
		filtersHeader+=filtersCount;
		this.jq(ID_MAPFILTERS).html(HU.div([ATTR_STYLE,'margin-right:5px;'],filtersHeader  + 
						   widgets));
		this.setFiltersVisible(true);		    
	    }


	    jqid(this.zoomonchangeid).change(function() {
		_this.setZoomOnChange($(this).is(':checked'));
	    });

	    this.findInLegend('.imdv-legend-clearall').click(()=>{
		this.display.featureChanged();
		this.attrs.featureFilters = {};
		this.applyMapStyle();
		this.updateFeaturesTable();
		if($("#"+this.zoomonchangeid).is(':checked')) {
		    this.panMapTo();
		}
	    });
    
	    this.findInLegend(CLASS_FILTER_STRING).keypress(function(event) {
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
	    this.findInLegend('.imdv-filter-enum').change(function(event) {
		let key = $(this).attr('filter-property');
		let filter = filters[key]??{};
		filter.property = key;
		filter.type='enum';
		filter.enumValues=$(this).val();
		update();
	    });

	    let sliderMap = {};
	    
	    this.findInLegend(CLASS_FILTER_SLIDER).each(function() {
		let theFeatureId = $(this).attr('feature-id');
		let featureInfo = _this.getFeatureInfo(theFeatureId);
		let onSlide = function( event, ui, force) {
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
		step = featureInfo.getStep(step);
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

	    this.findInLegend(CLASS_FILTER_PLAY).click(function() {
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
		    $(this).html(HU.getIconImage(icon_play));
		    $(this).attr('playing',false);
		    if(animation.timeout) {
			clearTimeout(animation.timeout);
			animation.timeout = null;
		    }
		} else {
		    $(this).html(HU.getIconImage(icon_stop));
		    $(this).attr('playing',true);
		    let step = (_values) =>{
			let values = _values?? slider.slider('values');
			if(values[1]>=sliderInfo.max) {
			    $(this).html(HU.getIconImage(icon_play));
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

	if(this.mapLayer) {
	    //Not now
	    //
	    //	    this.addBuffer(this.mapLayer.features);
	}
    },
    addBuffer:function(features) {
	if(!features) return;
	if(!MapUtils.loadTurf(()=>{this.addBuffer(features);})) {
	    return;
	}
	let polygons = [];
	features.forEach((f,idx)=>{
	    if(idx>50) return;
	    let points = [];
	    let obj;
	    let geom = f.geometry;
	    if(false && geom.CLASS_NAME=='OpenLayers.Geometry.LineString') {
//		console.dir(geom);
		geom.components.forEach(point=>{
		    let p = this.getMap().transformProjPoint(point);
		    points.push([p.x,p.y]);
		});
		obj = turf.multiPoint(points);
	    } else {
		let centroid = geom.getCentroid();
		let p = this.getMap().transformProjPoint(centroid);
		obj = turf.point([p.x,p.y]);
	    }
	    if(obj) {
//		console.dir(obj)
		let buffered = turf.buffer(obj, 10, {units: 'meters'});
		polygons.push(buffered);
	    }
	});
	if(polygons.length>0) {
	    let collection = turf.featureCollection(polygons);
	    console.log(collection);
	    this.display.createGeoJsonLayer('Buffer',collection,null,{color:'red'});
	} else {
	    console.log('Could not find features to buffer');
	}
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
	dots.addClass(CLASS_CLICKABLE);
	let select = jqid(_this.domId('enum_'+ Utils.makeId(obj.property)));
	select.find('option').each(function() {
	    if($(this).prop('selected')) {
		let value = $(this).prop(ATTR_TITLE);
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
    getMapFeaturesToGrid: function() {
	if(!this.mapLayer || !this.mapLoaded) {
	    return null;
	}
	let features= this.mapLayer.features;
	return features;
    },

    applyMapStyle:function(skipLegendUI) {
	let debug = false;
	if(debug)   console.log("applyMapStyle:" + this.getName());
    	this.applyChildren(child=>{child.applyMapStyle(skipLegendUI);});
	let _this = this;
	let features = this.getMapFeatures();
	if(!features) {
	    return;
	}
	if(!this.originalFeatures) this.originalFeatures = features;
	else features = this.originalFeatures;

	if(!this.mapLayer) return;
	let changedFeaturesList = false;
	if(this.attrs.subsetSimplify) {
	    this.haveChangedGeometry=true;
//	    console.log('prune geometry');
	    this.mapLayer.removeFeatures(features);
	    this.originalFeatures.forEach(feature=>{
		if(!feature.geometry.originalComponents) {
		    feature.geometry.originalComponents = [...feature.geometry.components];
		}
		feature.geometry.components.length=1;
	    });
	    this.mapLayer.addFeatures(features);	    
	} else if(this.haveChangedGeometry) {
//	    console.log('reset geometry');
	    this.haveChangedGeometry = false;
	    this.mapLayer.removeFeatures(features);
	    features.forEach(feature=>{
		if(feature.geometry.originalComponents) {
		    feature.geometry.components = [...feature.geometry.originalComponents];
		}
	    });
	    this.mapLayer.addFeatures(features);	    	    
	}

	if(Utils.isNumber(this.attrs.subsetSkip) && this.attrs.subsetSkip>0) {
	    let tmp = [];
	    let cnt = 0;
	    features.forEach((feature,idx)=>{
		cnt--;
		if(cnt<0) {
		    tmp.push(feature);
		    cnt = this.attrs.subsetSkip;
		}
	    });
	    changedFeaturesList = true;
	    this.mapLayer.removeFeatures(this.originalFeatures);
	    this.mapLayer.addFeatures(tmp);	    
	    features = this.mapLayer.features;
	} 

	if(this.attrs.subsetReverse) {
	    let tmp = [];
	    features.forEach(feature=>{
		tmp.unshift(feature);
	    });
	    changedFeaturesList = true;
	    this.mapLayer.removeFeatures(features);
	    this.mapLayer.addFeatures(tmp);	    
	    features = tmp;
	}

	if(!changedFeaturesList && this.originalFeatures.length!=features.length) {
	    this.mapLayer.removeFeatures(this.mapLayer.features);
	    this.mapLayer.addFeatures(this.originalFeatures);	    
	    features = this.originalFeatures;
	}


	if(!skipLegendUI && this.canDoMapStyle() && !this.isGroup()) {
	    this.makeFeatureFilters();
	}

	//Check for fillimage
	if(features.length>0 && features[0].attributes?.fillimage) {
	    this.addFillImage(features);
	}
	let style = this.style;
	
	if(Utils.isDefined(style.externalGraphic_cleared)) {
	    if(Utils.isTrue(style.externalGraphic_cleared)) {
		features.forEach(f=>{
		    if(f.style)
			f.style.externalGraphic = null;
		});
	    }
	    delete style['externalGraphic_cleared'];	    
	}
	let rules = this.getMapStyleRules();
//	if(debug) console.dir("\tmapStyleRules",rules);
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

	if(debug)   console.dir(style);
	if(this.mapLayer)
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
		if(debug && idx<3)   console.dir("\tfeature style:",featureStyle);
		ImdvUtils.applyFeatureStyle(f, featureStyle);
		f.originalStyle = Utils.clone(style);			    
	    });
	}


	//Check for any rule based styles
	let attrs = features.length>0?features[0].attributes:{};
	let keys  = Object.keys(attrs);
	if(rules && rules.length>0) {
	    this.mapLayer.style = null;
	    let seen = {};
	    let uniqueRules = rules.filter(rule=>{
		let key = rule.property+'__'+rule.value;
		if(seen[key]) return false;
		seen[key] = true;
		return true;
	    });
	    if(debug) console.dir("\tadding styleMap unique rules",uniqueRules);
	    olDebug = true;
	    this.mapLayer.styleMap = this.display.getMap().getVectorLayerStyleMap(this.mapLayer, style,uniqueRules);
	    features.forEach((f,idx)=>{
		f.fidx=idx;
		f.style=null;
	    });
	} 

	let applyColors = (obj,attr,stringList,debug)=>{
	    if(!obj || !Utils.stringDefined(obj?.property))  return;
	    //make a copy because we can change it later
	    //Maybe not since the state doesn't get set
	    //	    obj  =$.extend({},obj);
	    if(debug)
		console.log('applyColors',attr);
	    let strings  =[]
	    let prop =obj.property;
	    let min =Number.MAX_VALUE;
	    let max =Number.MIN_VALUE;
	    let ct =Utils.getColorTable(obj.colorTable,true);
	    let anyNumber =  false;
	    features.forEach((f,idx)=>{
		let value = this.getFeatureValue(f,prop);
		if(isNaN(parseFloat(value))) {
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
		if(!Utils.isDefined(obj.min))
		    obj.min = min;
		if(!Utils.isDefined(obj.max))
		    obj.max = max;		
	    }
	    strings = strings.sort((a,b)=>{
		return a.localeCompare(b);
	    });

	    //If there was no numbers then we pass back the strings
	    if(!anyNumber) {
		stringList.push(...strings);
	    }
	    let range = obj.max-obj.min;
	    if(debug) console.log('applyColors min:' + obj.min +' max:' + obj.max);
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
		    let percent = (value-obj.min)/range;
		    index = Math.max(0,Math.min(ct.length-1,Math.round(percent*ct.length)));
		}

		if(!f.style)
		    f.style = $.extend({},style);
		f.style[attr]=ct[index];
		if(f.originalStyle) f.originalStyle[attr]=ct[index];		
		if(debug && idx<3) {
		    console.log('\t'+attr+'='+f.style[attr]);
//		    console.dir(f.style);
		}
	    });
	};


	this.fillStrings = [];
	this.strokeStrings = [];		
	applyColors(this.attrs.fillColorBy,'fillColor',this.fillStrings);
	applyColors(this.attrs.strokeColorBy,'strokeColor',this.strokeStrings);	

	if(debug) console.log("\tuseRules:" + useRules?.length);
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
	    let maxLength = parseInt(this.attrs.labels_maxlength??1000);
	    let maxLineLength = parseInt(this.attrs.labels_maxlinelength??1000);
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
	this.applyFeatureFilters(features);


	if(this.attrs.fillColors) {
	    //	let ct = Utils.getColorTable('googlecharts',true);
	    let ct = Utils.getColorTable('d3_schemeCategory20',true);	
	    let cidx=0;
	    features.forEach((f,idx)=>{
		f.style = f.style??{};
		cidx++;
		if(cidx>=ct.length) cidx=0;
		if(f.originalStyle) f.originalStyle.fillColor=ct[cidx];
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
		if(!f.originalStyle)f.originalStyle={};
		$.extend(f.originalStyle,group.style)
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
    },
    applyFeatureFilters:function(features) {
	let debug = false;
//	debug = true;
	let redraw = false;
	if(!features)  {
	    features=this.mapLayer?.features;
	    redraw = true;
	}
	if(!features) return;
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


	let redrawFeatures = false;
	let max =-1;
	let text = this.getProperty('showTextSearch',null,true)?this.getProperty('searchtext',null,true):null;
	if(!Utils.stringDefined(text)) { text=null;}
	else text = text.toLowerCase();
	features.forEach((f,idx)=>{
	    let visible = true;
	    if(debug && idx<5) console.log("feature check filter:");
	    rangeFilters.every(filter=>{
		let value=this.getFeatureValue(f,filter.property);
		if(Utils.isDefined(value)) {
		    max = Math.max(max,value);
		    visible = value>=filter.min && value<=filter.max;
		    if(debug && idx<5) console.log("\trange:",filter,value,visible);
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
			if(debug && idx<5) console.log("\tstring:",filter,value,visible);
		    }
		    return visible;
		});
	    }
	    if(visible && text) {
		if(f.attributes) {
		    let numStrings = 0;
		    let numMatched = 0;
		    let matched  = false;
		    Object.keys(f.attributes).every(key=>{
			let value = f.attributes[key];
			if(value  && typeof value == 'string') {
			    numStrings++;
			    matched = value.toLowerCase().indexOf(text)>=0;
			    if(matched) return false;
			}
			return true;
		    })
		    if(numStrings && !matched) {
			if(debug && idx<5) console.log("\ttext:",text);
			visible=false;
		    }
		}
	    }


	    if(visible) {
		enumFilters.every(filter=>{
		    let value=this.getFeatureValue(f,filter.property)??'';
		    visible =filter.enumValues.includes(value);
		    if(debug && idx<5) console.log("\tenum",filter,value);
		    return visible;
		});
	    }		

	    

	    if(visible) this.visibleFeatures++;
	    f.isVisible  = visible;
	    f.isFiltered=!visible;
	    MapUtils.setFeatureVisible(f,visible);
	    if(f.mapLabel) {
		redrawFeatures = true;
		f.mapLabel.isFiltered=!visible;
		MapUtils.setFeatureVisible(f.mapLabel,visible);
	    }
	});

	this.jq('filters_count').html('#' + this.visibleFeatures);
	if(redraw) {
	    ImdvUtils.scheduleRedraw(this.mapLayer);
	}

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

	if(this.extraFeatures) this.display.removeFeatures(this.extraFeatures);
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
	this.setExtraFeatures(this.display.makeRangeRings(center,this.getRadii(),this.style,this.attrs.rangeRingAngle,ringStyle,labels));
    },
    setExtraFeatures:function(features) {
	this.extraFeatures = features;
	if(this.extraFeatures) {
	    this.extraFeatures.forEach(f=>{
		f.mapGlyph=this;
		MapUtils.setFeatureVisible(f,this.isVisible());
	    });
	    this.display.addFeatures(this.extraFeatures);
	}		
    },
    applyStyle:function(style,skipChangeNotification,isForDataIcon) {
	if(style) {
	    this.style = style;
	}
	//check for the data icon state
	if(this.isDataIconCapable() && !isForDataIcon) {
	    this.resetDataIconOriginal();
	}

	this.applyMapStyle();
	if(this.getMapServerLayer()) {
	    if(Utils.isDefined(this.style.opacity)) {
		this.getMapServerLayer().opacity = +this.style.opacity;
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

	if(!skipChangeNotification)
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
	if(this.extraFeatures){
	    this.extraFeatures.forEach(feature=>{
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

	if(this.getParentGlyph()?.isMultiEntry()) {
	    this.overrideLocation = this.getPoints({});
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
//	    console.log('MapGlyph.checkImage: no image url');
	    return;
	}
	feature = feature??this.features[0];
	if(!feature) {
//	    console.log('MapGlyph.checkImage: no feature');
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
    getImageTransform:function() {
	let transform = '';
	if(Utils.stringDefined(this.style.transform)) 
	    transform = this.style.transform;
	if(Utils.isDefined(this.style.rotation) && this.style.rotation!=0)
	    transform += ' rotate(' + this.style.rotation +'deg)';
	if(!Utils.stringDefined(transform))  transform=null;
	return transform;
    },

    initImageLayer:function(image) {
	this.image = image;
	image.imageHook = (image)=> {
	    let transform=this.getImageTransform();
	    let childNodes = this.image.div.childNodes;
	    for(let i = 0, len = childNodes.length; i < len; ++i) {
                let element = childNodes[i].firstChild || childNodes[i];
                let lastChild = childNodes[i].lastChild;
                if (lastChild && lastChild.nodeName.toLowerCase() === "iframe") {
		    element = lastChild.parentNode;
                }
		if(element.style)
		    element.style.transform=transform;
		element.style['clip-path']=  this.style.clippath;
		if(this.style.imagecss) {
		    Utils.split(this.style.imagecss,'\n',true,true).forEach(line=>{
			let toks = Utils.split(line,'=',true,true);
//			console.log(toks[0]+':'+ toks[1]);
			element.style[toks[0]] = toks[1];
		    });
		}

		element.style.filter = this.getStyleFromTree('imagefilter');
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
    getDeclutterLabels:function() {
	if(Utils.isDefined(this.attrs.declutter_labels))
	    return this.attrs.declutter_labels;
	return this.getProperty('declutter.labels',true);
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
    getVisibleLevelRange:function(skipParent) {
	let r =this.attrs.visibleLevelRange;
	if(!skipParent && (!r || !Utils.isDefined(r.min)) && this.getParentGlyph()) {
	    return this.getParentGlyph().getVisibleLevelRange();
	}
	return this.attrs.visibleLevelRange;
    },

    setVisible:function(visible,callCheck,highlighted,skipChildren) {
	this.highlighted = highlighted;
	if(!visible) {
	    if(this.stepMarker) {
		this.display.removeFeatures([this.stepMarker]);
	    }
	}

	this.attrs.visible = visible;
	this.checkInMapLabel();
	if(!skipChildren) {
    	    this.applyChildren(child=>{child.setVisible(visible, callCheck);});
	}
	Utils.forEach(this.extraFeatures,f=>{MapUtils.setFeatureVisible(f,visible);});

	if(this.canHaveChildren()) {
	    if(!visible) this.attrs[PROP_LAYERS_ANIMATION_ON]=false;
	    this.checkLayersAnimationButton();
	}

	if(callCheck)
	    this.checkVisible();
	this.checkMapLayer();

	let legend = this.getLegendDiv();
	legend.removeClass(CLASS_LEGEND_LABEL_INVISIBLE);
	legend.removeClass(CLASS_LEGEND_LABEL_HIGHLIGHT);
	if(this.getVisible()) {
	    if(this.highlighted) {
		legend.addClass(CLASS_LEGEND_LABEL_HIGHLIGHT);
	    } 
	} else {
	    legend.addClass(CLASS_LEGEND_LABEL_INVISIBLE);
	}

	if(this.topHeaderId) {
	    if(this.getVisible()) {
		jqid(this.topHeaderId).removeClass(CLASS_LEGEND_LABEL_INVISIBLE);
	    } else {
		jqid(this.topHeaderId).addClass(CLASS_LEGEND_LABEL_INVISIBLE);
	    }
	}
	this.checkDataIconMenu();	
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
	let level = this.display.getCurrentLevel();
	let min = Utils.stringDefined(range.min)?+range.min:-1;
	let max = Utils.stringDefined(range.max)?+range.max:10000;
	let visible=  this.getVisible() && (level>=min && level<=max);
//	console.log(this.getName(),range,level,visible);
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

	if(this.features) {
	    this.features.forEach(f=>{
		MapUtils.setFeatureVisible(f, visible);
	    });
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
	if(this.isData()) { 
	    this.checkDataDisplayVisibility();
	}

	this.checkDeclutter(this.mapLabels,visible,true);
	let features = this.getMapFeaturesToGrid();
	if(features) {
	    this.checkDeclutter(features,visible,false);
	    ImdvUtils.scheduleRedraw(this.mapLayer);
	}
    	this.applyChildren(child=>{child.checkVisible();});
	ImdvUtils.scheduleRedraw(this.display.myLayer);
	if(visible && this.isMultiEntry() && !this.haveAddedEntries) {
	    setTimeout(()=>{
		this.addEntries();
	    },1);
	}
	return visible;
    },    
    checkDeclutter:function(features, visible,isLabels) {
	if(!features || !this.mapLoaded) return;
	if(!visible) {
	    features.forEach(feature=>{MapUtils.setFeatureVisible(feature,false);});
	    return;
	} 
	let featuresToGrid = [];
	//If the label wasn't filtered then turn them all on
	features.forEach(feature=>{
	    if(!feature.isFiltered) {
		featuresToGrid.push(feature);
		MapUtils.setFeatureVisible(feature,true);
	    }
	});
	if(Utils.stringDefined(this.getMapPointsRange())) {
	    let level = this.display.getCurrentLevel();
	    if(level<parseInt(this.getMapPointsRange())) {
		visible=false;
		features.forEach(feature=>{MapUtils.setFeatureVisible(feature,false);});
	    }
	    return;
	} 
	if(isLabels) {
	    if(!this.getDeclutterLabels()){
		return;
	    }
	} else {
	    if(!this.getProperty('declutter.features',false)) {
		return;
	    }
	}
	let t1 = new Date();
	MapUtils.declutter(this.getMap(), featuresToGrid,this.getDeclutterArgs());
	//	Utils.displayTimes("gridding #" + features.length,[t1,new Date()],true);
    },
    getDeclutterArgs:function() {
	let args ={};
	args.fontSize = this.style.fontSize??'12px';
	if(Utils.stringDefined(this.attrs.declutter_padding))
	    args.padding = +this.attrs.declutter_padding;
	if(this.attrs.declutter_granularity)
	    args.granularity = +this.attrs.declutter_granularity;
	if(this.attrs.declutter_pixelsperline)
	    args.pixelsPerLine = +this.attrs.declutter_pixelsperline;
	if(this.attrs.declutter_pixelspercharacter)
	    args.pixelsPerCharacter = +this.attrs.declutter_pixelspercharacter;
	return args;
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
	let html = HU.div([ATTR_ID,id,ATTR_CLASS,"ramadda-imdv-fixed",ATTR_STYLE,css],"");
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
		wiki = HU.div([ATTR_STYLE,'max-height:300px;overflow-y:auto;'],wiki);
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
	let outerDivId   = HU.getUniqueId("outerdisplay_");	
	let bottomDivId   = HU.getUniqueId("displaybottom_");	    
	let headerDiv = HU.div([ID,outerDivId],HU.div([ID,divId]));
	this.display.jq(ID_HEADER1).append(headerDiv);
	this.display.jq(ID_BOTTOM).append(HU.div([ID,bottomDivId]));	    
	let attrs = {"externalMap":this.display.getMap(),
		     "externalDisplay":this,
		     "isContained":true,
		     "showRecordSelection":true,
		     "showInnerContents":false,
		     "entryIcon":this.attrs.icon,
		     "title":this.attrs.name,
		     "max":"5000",
		     "thisEntryType":this.attrs.entryType,
		     "entryId":entryId,
		     "divid":divId,
		     "acceptRequestChangeEvent":false,
		     "pointDataCacheOK":false,
		     "bottomDiv":bottomDivId,			 
		     "data":pointData,
		     "fileUrl":Ramadda.getUrl("/entry/get?entryid=" + entryId+"&fileinline=true")};
	$.extend(attrs,displayAttrs);
	attrs = $.extend({},attrs);
	attrs.name=this.getName();
	let display = this.display.getDisplayManager().createDisplay("map",attrs);
	//	this.attrs.name = display.getLogLabel();
	//Not sure why we do this since we can't integrate charts with map record selection
	//	display.setProperty("showRecordSelection",false);

	display.errorMessageHandler = (display,msg) =>{
	    this.display.setErrorMessage(msg,5000);
	};
	this.displayInfo =   {
	    display:display,
	    outerDivId:outerDivId,
	    divId:divId,
	    bottomDivId: bottomDivId
	};
	this.checkDataDisplayVisibility();
    },
    externalDisplayReady:function(display) {
	this.display.checkGlyphLayers();
    },
    checkDataDisplayVisibility:function() {
	if(!this.displayInfo) return;
	let visible = this.isVisible();
	if(this.displayInfo.display) {
	    this.displayInfo.display.setVisible(visible);
	}
	//For now don't toggle the class because if there isn't any thing shown we have a grey bar
	let div = jqid(this.displayInfo.divId);
	let outerDiv = jqid(this.displayInfo.outerDivId);	
	if(visible) {
	    outerDiv.show();
//	    outerDiv.removeClass(CLASS_LEGEND_LABEL_INVISIBLE);
//	    outerDiv.find('input').prop('disabled',false);
	}    else {
	    outerDiv.hide();
//	    outerDiv.addClass(CLASS_LEGEND_LABEL_INVISIBLE);
//	    outerDiv.find('input').prop('disabled',true);
	}
    },
    getDecoration:function(small) {
	let type = this.getType();
	let style = this.style??{};
	let css= ['display','inline-block'];
	let dim = small?'10px':'25px';
	css.push(ATTR_WIDTH,small?'10px':'50px');
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
		return HU.image(style.externalGraphic,[ATTR_WIDTH,'16px']);
	} else if(type==GLYPH_BOX) {
	    if(Utils.stringDefined(style.fillColor)) {
		css.push('background',style.fillColor);
	    }
	    css.push('height',dim);
	    return HU.div([ATTR_STYLE,HU.css(css)]);
	} else if(type==GLYPH_HEXAGON) {
	    css=[];
	    if(Utils.stringDefined(style.fillColor)) {
		css.push('background',style.fillColor);
	    }
	    if(Utils.stringDefined(style.strokeColor)) {
		css.push('color',style.strokeColor);
	    }		
	    css.push('font-size',small?'16px':'32px','vertical-align','center');
	    return HU.span([ATTR_STYLE,HU.css(css)],"&#x2B22;");
	} else if(type==GLYPH_CIRCLE || type==GLYPH_POINT) {
	    if(Utils.stringDefined(style.fillColor)) {
		css.push('background',style.fillColor);
	    }
	    if(type==GLYPH_POINT) {
		css.push('margin-top','10px','height','10px',ATTR_WIDTH,'10px');
	    } else {
		css.push('height',dim);
		css.push(ATTR_WIDTH,dim);
	    }		    
	    return HU.div([ATTR_CLASS,'ramadda-dot', ATTR_STYLE,HU.css(css)]);
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
	    return HU.div([ATTR_STYLE,HU.css(css)]);
	}
	return HU.div([ATTR_STYLE,HU.css('display','inline-block','border','1px solid transparent',ATTR_WIDTH,small?'10px':'50px')]);
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
	if(!this.checkVisible()) return;
	if(this.haveAddedEntries) return;
	this.haveAddedEntries = true;
	let entryId = this.getEntryId();
        let entry =  new Entry({
            id: entryId,
        });
        let callback = (entries)=>{
	    this.clearChildren();
	    this.children = [];
	    this.entries = entries;
	    let someNotLocated = false;
	    entries.forEach((e,idx)=>{
		if(!e.hasLocation()) {
		    console.log("multi entry has no location:" + e.getName());
		    someNotLocated = true;
		    return;
		}
		let overrideLocation;
		if(this.attrs.childrenLocations) {
		    overrideLocation = this.attrs.childrenLocations[e.getId()];
		}
		let latLon;

		if(overrideLocation) {
		    latLon = {latitude:overrideLocation[0],longitude:overrideLocation[1]};
		} else {
		    latLon = {latitude:e.getLatitude(),longitude:e.getLongitude()};
		}
		let pt = MapUtils.createPoint(latLon.longitude,latLon.latitude);
		pt = this.display.getMap().transformLLPoint(pt);
		let style = Utils.clone({},this.style);
		style.externalGraphic = e.getIconUrl();
		if(Utils.stringDefined(this.style.childIcon)) {
		    style.externalGraphic = this.style.childIcon;
		}
		style.strokeWidth=1;
		style.strokeColor="transparent";
		style.fontSize='12px';
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
		
		let attrs = {name:e.getName(),
			     mapglyphs:e.mapglyphs,
			     entryId:e.getId(),
			     icon:style.externalGraphic
			    };
		let points =[latLon.latitude,latLon.longitude];
		let mapGlyph = this.display.createMapMarker(GLYPH_ENTRY,attrs, style,points,false);
		mapGlyph.overrideLocation = overrideLocation;
		mapGlyph.isEphemeral = true;
		this.addChildGlyph(mapGlyph);
		if(this.getShowDataIcons()) {
		    mapGlyph.makeDataIcon(true);
		}
	    });
	    //call getBounds  so they are cached
	    this.getBounds();
	    this.display.makeLegend();
	    this.checkVisible();
	    if(andZoom) {
		this.panMapTo();
	    }
	    this.showMultiEntries();
	};
	let order = 'orderby=' + this.getProperty('orderby','name');
	order+='&ascending=' + this.getProperty('ascending','true');
	entry.getChildrenEntries(callback,order);
    },
    isSelected:function() {
	return this.selected;
    },
    getSelected:function(selected) {
	if(this.isSelected()) {
	    selected.push(this);
	}
	this.applyChildren(child=>{child.getSelected(selected);});
    },

    select:function(maxPoints,dontRedraw) {
	if(!Utils.isDefined(maxPoints)) maxPoints = 20;
	if(this.isSelected()) {
	    return;
	}
	this.findInLegend(CLASS_LEGEND_LABEL).css('font-weight','bold');
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
	this.applyChildren(child=>{pointCount+=child.select(maxPoints, dontRedraw);});
	return pointCount;
    },
    unselect:function() {
	this.findInLegend(CLASS_LEGEND_LABEL).css('font-weight','normal');
	this.applyChildren(child=>{child.unselect();});
	if(!this.isSelected()) {
	    return;
	}	    
	this.selected = false;
	if(this.selectDots) {
	    this.display.selectionLayer.removeFeatures(this.selectDots);
	    this.selectDots= null;
	}

	if(this.mapLayer && this.mapLayer.features) {
	    this.applyMapStyle(true);
	}

    },
    
    doRemove:function() {
	if(this.dataIconContainer) {
	    jqid(this.dataIconContainer).remove();
	    this.dataIconContainer=null;
	}

	if(this.stepMarker) {
	    this.display.removeFeatures([this.stepMarker]);
	}

	if(this.isFixed()) {
	    jqid(this.getFixedId()).remove();
	}
	if(this.mapLabels) {
	    this.display.removeFeatures(this.mapLabels);
	}
	if(this.features) {
	    this.display.removeFeatures(this.features);
	}
	if(this.extraFeatures) {
	    this.display.removeFeatures(this.extraFeatures);
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
	this.clearChildren();
    }
}

function FeatureInfo(mapGlyph,key) {
    $.extend(this,{
	mapGlyph:mapGlyph,
	property:key,
	id:Utils.makeId(key),
	min:NaN,
	max:NaN,
	type:'',
	seen:{},
	samples:[],
    });
}

FeatureInfo.prototype= {
    getId:function() {
	return this.id;
    },
    getProperty:function(prop,dflt,checkGlyph) {
	if(checkGlyph) dflt=this.mapGlyph.getProperty(prop,dflt);
	let v =   this.mapGlyph.getProperty(this.id+'.' + prop,dflt);
	return v;
    },
    show: function() {
	return  this.getProperty('show',this.mapGlyph.getProperty('feature.show',true));
    },
    showFilter: function() {
	let dflt = this.mapGlyph.getProperty('filter.show',this.show());
	let show =   this.getProperty('filter.show',dflt);
	return show;
    },
    showTable: function() {
	return this.getProperty('table.show',this.mapGlyph.getProperty('table.show',this.show()));
    },
    showPopup: function() {
	return this.getProperty('popup.show',this.mapGlyph.getProperty('popup.show',this.show()));
    },				
    isColorTableSelect: function() {
	return this.getProperty('colortable.select',this.mapGlyph.getProperty('colortable.select',false));
    },
    getType:function() {
	return this.getProperty('type',this.type);
    },
    getLabel:function(addSpan) {
	let label  =this.getProperty('label');
	if(!Utils.stringDefined(label)) label  =this.mapGlyph.display.makeLabel(this.property);
	if(addSpan) label = HU.span([ATTR_TITLE,this.property],label);
	return label;
    },
    getValueLabel:function(v) {
	let label = this.getProperty('label.' +v?.toLowerCase(), null);
	if(!label) label = Utils.makeLabel(v);
	return label;
    },
    format:function(value) {
	if(this.isNumeric()) {
	    let decimals = this.getProperty('format.decimals',-1,true);
	    if(decimals>=0&&!isNaN(value)) {
		value = Utils.trimDecimals(value,decimals);
	    }
	}
	return value;
    },
    isNumeric:function(){return this.isInt() || this.getType()=='numeric';},
    isInt:function() {return this.getType()=='int';},
    isString:function() {return this.getType()=='string';},
    isEnumeration:function() {return this.getType()=='enumeration'||this.getType()=='enum';},
    getStep:function(dflt) {
	let s = this.getProperty('step');
	if(Utils.isDefined(s)) return parseFloat(s);
	return dflt;
    },
    getSamplesLabels:function() {
	return this.samples.map(sample=>{return sample.label;});
    },
    getSamplesForMenu:function() {
	return this.samples;
    },
    getSamplesValues:function() {
	return this.samples.map(sample=>{
	    let cnt = this.seen[sample.value];
	    return sample.value +' ('+ cnt+')';
	});
    },
    initValues:function(f) {
	let value= this.mapGlyph.getFeatureValue(f,this.property);
	if(!Utils.isDefined(value)) return;
	let isEnumeration = this.isEnumeration();
	if(isNaN(value) || this.samples.length>0 || isEnumeration) {
	    if(this.samples.length<30) {
		this.type='enumeration';
		if(!this.seen[value]) {
		    this.seen[value] = 0;
		    this.samples.push(value);
		}
		this.seen[value]++;
	    } else {
		this.type='string';
	    }
	} else if(!isNaN(value)) {
	    if(this.type != 'numeric')
		this.type='int';
	    if(Math.round(value)!=value) {
		this.type = 'numeric';
	    }
	    this.min = isNaN(this.min)?value:Math.min(this.min,value);
	    this.max = isNaN(this.max)?value:Math.max(this.max,value);			
	}
    },

    finishInit:function() {
	if(this.samples.length) {
	    let items = this.samples.map(item=>{
		return {value:item,label:this.getValueLabel(item)};
//		return {value:item,label:Utils.makeLabel(item)};
	    });
	    this.samples =  items.sort((a,b)=>{
		return a.label.localeCompare(b.label);
	    });
	}
    }
    
    
}
