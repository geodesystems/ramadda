/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/



//Note: I put all of the chart definitions together at the top so one can see everything that is available here
const DISPLAY_D3_GLIDER_CROSS_SECTION = "GliderCrossSection";
//const DISPLAY_D3_PROFILE = "profile";
const DISPLAY_D3_LINECHART = "D3LineChart";
const DISPLAY_D3_PLOT = "d3plot";
const DISPLAY_SKEWT = "skewt";
const DISPLAY_VENN = "venn";
const DISPLAY_CHERNOFF = "chernoff";
const DISPLAY_D3BUBBLE = "d3bubble";
const DISPLAY_MINIDOTS = "minidots";

//Note: Added requiresData and category
addGlobalDisplayType({
    type: DISPLAY_D3_PLOT,
    forUser: true,
    label: "D3 Plot",
    requiresData: true,
    category: CATEGORY_MISC,
    tooltip: makeDisplayTooltip("D3 Plot",null,'In development. Uses D3.Plot')        
});


addGlobalDisplayType({
    type: DISPLAY_D3_LINECHART,
    forUser: false,
    label: "D3 LineChart",
    requiresData: true,
    category: "Charts"
});
/*
  addGlobalDisplayType({
  type: DISPLAY_D3_PROFILE,
  forUser: false,
  label: "Profile",
  requiresData: true,
  category: "Charts"
  });
*/
addGlobalDisplayType({
    type: DISPLAY_D3_GLIDER_CROSS_SECTION,
    forUser: false,
    label: "Glider cross section",
    requiresData: true,
    category: CATEGORY_MISC
});


addGlobalDisplayType({
    type: DISPLAY_VENN,
    forUser: true,
    label: "Venn Diagram",
    requiresData: true,
    category: CATEGORY_MISC,
    tooltip: makeDisplayTooltip("A Venn diagram","venn.png")    
});

addGlobalDisplayType({
    type: DISPLAY_MINIDOTS,
    forUser: false,
    label: "Mini Dots",
    requiresData: true,
    category: CATEGORY_MISC
});

addGlobalDisplayType({
    type: DISPLAY_CHERNOFF,
    forUser: false,
    label: "Chernoff Faces",
    requiresData: true,
    category: CATEGORY_MISC
});

addGlobalDisplayType({
    type: DISPLAY_D3BUBBLE,
    forUser: true,
    label: "Bubble Chart",
    requiresData: true,
    category: CATEGORY_IMAGES,
    tooltip: makeDisplayTooltip("Animated bubbles showing images","d3bubble.png"),            
});


addGlobalDisplayType({
    type: DISPLAY_SKEWT,
    forUser: false,
    label: "SkewT",
    requiresData: true,
    category: CATEGORY_MISC
});

//Note: define meaningful things as variables not as string literals
const FIELD_TIME = "time";
const FIELD_DEPTH = "depth";
const FIELD_VALUE = "value";
const FIELD_SELECTEDFIELD = "selectedfield";

const TYPE_LATITUDE = "latitude";
const TYPE_LONGITUDE = "longitude";
const TYPE_TIME = "time";
const TYPE_VALUE = "value";
const TYPE_ELEVATION = "elevation";


const FUNC_MOVINGAVERAGE = "movingAverage";

const D3Util = {
    initPlot:function(display,opts) {
	opts = opts??{};
	['caption',
	 ['plotWidth','width'],
	 ['plotHeight','height'],
	 'marginTop','marginRight','marginBottom','marginLeft','grid'].forEach(prop=>{
	     if(!Array.isArray(prop)) prop = [prop,prop];
	     if(!Utils.isDefined(opts[prop[1]])) {
		 opts[prop[1]] = display.getProperty(prop[0]);
	     }
	 });

	/*
	  opts.x = {x:{
	  round: true, nice: d3.utcYear,type:'band',ticks:10,
	  axis: display.getProperty('xAxisPosition','bottom')
	  }};
	*/
	/*
	  opts.x = {grid: display.getProperty('xAxisGrid',false),
	  ticks:10};
	*/


	return opts;
    },
    initMarks:function(display, marks) {
	if(display.getProperty('showFrame')) {
	    marks.push(Plot.frame());
	}
	//	marks.push(Plot.text(["Hello, world!"], {frameAnchor: "left"}));
	Utils.split(display.getProperty("rules",""),",").forEach(rule=>{
            marks.push(Plot.ruleY([parseFloat(rule)]));
	});

	marks.push(Plot.axisX({ticks: d3.utcYear.every(50), tickFormat: "%Y"}));
	//	marks.push(Plot.axisX({anchor: "top",tickSpacing:200,xticks:10}));
	//	marks.push(Plot.gridX({ticks:5, stroke: "#efefef", strokeOpacity: 0.5}));
    },

    createMarks:function(display, fields, records,args) {
	let marks = [];
	args = args??{};
	this.initMarks(display, marks);
	fields.forEach((field,idx)=>{
	    let opts = {};
	    $.extend(opts,args);
            marks.push(D3Util.createMark(display, field,records,idx,opts));
	}); 
	return marks;
    },
    getProperty(display, prefix,prop,dflt) {
	return  display.getProperty(prefix+'.'+prop,display.getProperty(prop))??dflt;
    },
    createMark:function(display, field,records,index,args) {
	args = args??args;
	if(!args.array) {
	    args.array=this.getD3Data(display,field,records,{includeDate:args.includeDate});
	}
	let opts = {x: "Date", y: field.getId()};
	let colors = args.colors?? display.getColorList();
	let fill = args.fill??this.getProperty(display,field.getId(),'fillColor');

	opts.strokeWidth =parseFloat(args.strokeWidth??this.getProperty(display,field.getId(),'strokeWidth',1));
	opts.stroke =  args.stroke??this.getProperty(display,field.getId(), 'strokeColor','#000');
	
	let mark;
	let markType  = display.getProperty(field.getId()+'.markType',display.getProperty('markType','line'));
	if(markType=='bar') {
	    opts.fill = fill;
	    mark= Plot.barY(args.array, opts);
	} else if(markType=='dot') {
	    opts.fill = fill;
	    opts.r = parseFloat(this.getProperty(display,field.getId(),'radius',4));
	    mark= Plot.dot(args.array, opts);	    
	} else if(markType=='line') {
	    mark= Plot.lineY(args.array, opts);
	} else {
	    console.error('Unknown mark type:' + markType);
	    mark= Plot.lineY(args.array, opts);
	}
	return mark;
    },	
    getD3Data:function(display,fields,records,args) {
	if(!Array.isArray(fields)) fields=[fields];
	args = args??{};
	let opts = {
	    includeDate:false
	}
	$.extend(opts,args);
	let d3Array = [];
	records.forEach((record,idx)=>{
	    let obj = {};
	    if(opts.includeDate)
		//		obj.Date = Utils.formatDateYYYYMMDD(record.getDate());
		obj.Date = record.getDate();
	    fields.forEach(field=>{
		obj[field.getId()] = field.getValue(record);  
	    });
	    d3Array.push(obj);
	});    
	return d3Array;
    },
    getAxis: function(axisType, range) {
        var axis;
        if (axisType == FIELD_TIME) {
            axis = d3.time.scale().range(range);
        } else {
            axis = d3.scale.linear().range(range);
        }
        return axis;
    },
    getDataValue: function(axis, record, index) {
        var data;
        if (axis.fieldIdx >= 0) {
            data = record.getData()[axis.fieldIdx];
        } else {
            switch (axis.type) {
            case TYPE_TIME:
                data = new Date(record.getDate());
                break;
            case TYPE_ELEVATION:
                //console.log(record.getElevation());
                data = record.getElevation();
                break;
            case TYPE_LATITUDE:
                data = record.getLatitude();
            case TYPE_LONGITUDE:
                data = record.getLongitude();
            default:
                data = record.getData()[index];
            }
        }


        if (axis.reverse == true) {
            return -1 * data;
        } else {
            return data;
        }
    },
    // This will be the default but we can add more colorscales
    getColorFromColorBar: function(value, range) {
        var colors = ["#00008B", "#0000CD", "#0000FF", "#00FFFF", "#7CFC00", "#FFFF00", "#FFA500", "#FF4500", "#FF0000", "#8B0000"];
        var colorScale = d3.scale.linear()
            .domain([0, colors.length - 1])
            .range(range);

        var colorScaler = d3.scale.linear()
            .range(colors)
            .domain(d3.range(0, colors.length).map(colorScale));

        color = colorScaler(value);
        return color;
    },
    // This is for the path lines the previous function for generic ones. 
    addColorBar: function(svg, colors, colorSpacing, displayWidth) {
        //Note: this originally had this.displayWidth which was undefined
        var colorBar = svg.append("g")
            .attr({
                ATTR_ID: "colorBarG",
                "transform": "translate(" + (displayWidth - 40) + ",0)"
            });

        colorBar.append("g")
            .append("defs")
            .append("linearGradient")
            .attr({
                id: "colorBarGradient",
                x1: "0%",
                y1: "100%",
                x2: "0%",
                y2: "0%"
            })
            .selectAll("stop")
            .data(colors)
            .enter()
            .append("stop")
            .attr({
                "offset": function(d, i) {
                    return colorSpacing * (i) + "%"
                },
                "stop-color": function(d, i) {
                    return colors[i]
                },
                "stop-opacity": 1
            });

        return colorBar;
    }
}


function RamaddaD3plotDisplay(displayManager, id, properties) {
    const SUPER  = new RamaddaDisplay(displayManager, id, DISPLAY_D3_PLOT, properties);
    let myProps = [
        {label:'D3 Plot Attributes'},
        {p:'skewtWidth',ex:'500'},
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        needsData: function() {
            return true;
        },
        updateUI: async function() {
	    if(!window.Plot) {
		if(this.awaiting) return;
		this.awaiting = true;
		await Utils.importJS('https://cdn.jsdelivr.net/npm/@observablehq/plot@0.6.9/dist/plot.umd.min.js')
		this.awaiting = false;
	    }
	    if(this.awaiting) {
		return;
	    }
            let records =  this.filterData();
	    if(!records) {
		return;
	    }
            let fields = this.getSelectedFields([]);
	    let data=D3Util.getD3Data(this,fields,records,{includeDate:true});
	    let marks = D3Util.createMarks(this,fields,records,{includeDate:true});    
	    let opts=D3Util.initPlot(this,{marks:marks});
/*
  color bar example
  https://observablehq.com/@observablehq/plot-warming-stripes
	    let opts2 = {
		x: {round: true},
		color: {scheme: "BuRd"},
		marks: [
		    Plot.lineY(data, {
			x: "Date",
			y: "gcag",
//			interval: "year", // yearly data
//			inset: 0 // no gaps
		    })
		]
		}
		*/
	    let plot = Plot.plot(opts);
	    this.getContents().html(plot);
	}
    });
}


function RamaddaSkewtDisplay(displayManager, id, properties) {
    const ID_SKEWT = "skewt";
    const ID_DATE_LABEL = "skewtdate";
    const SUPER  = new RamaddaDisplay(displayManager, id, DISPLAY_SKEWT, properties);
    let myProps = [
        {label:'Skewt Attributes'},
        {p:'skewtWidth',ex:'500'},
        {p:'skewtHeight',ex:'550'},
        {p:'hodographWidth',ex:'150'},
        {p:'showHodograph',ex:'false'},
        {p:'windStride',ex:'1'},
        {p:'showText',ex:'false'},
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        needsData: function() {
            return true;
        },
        initDisplay:  function() {
            SUPER.initDisplay.call(this);
        },
        handleEventPointDataLoaded: function(source, pointData) {
            //TODO: this results in a double  call to updateUI when first created
            this.updateUI();
        },
        handleEventRecordSelection: function(source, args) {
	    this.skewt.highlightRecord(args.record);
	},
        updateUI: async function() {
            if(!this.loadedResources) {
		if(this.loadingSkewt) return;
		this.loadingSkewt=true;
                await Utils.importCSS(RamaddaUtil.getCdnUrl("/lib/skewt/sounding.css"));
                await Utils.importJS(RamaddaUtil.getCdnUrl("/lib/skewt/d3skewt.js"));
                this.loadedResources = true;
            }

            if(!window["D3Skewt"]) {
                setTimeout(()=>this.updateUI(),100);
                return;
            }
            SUPER.updateUI.call(this);

	    if(!d3.scaleLinear) {
                this.setDisplayMessage("Oops, the wrong version of D3 had been loaded");
		return;
	    }

	    //          console.log("skewt.updateui-1");
            let records =  this.filterData();
            if (!records || records.length==0) {
                this.setDisplayMessage(this.getLoadingMessage());
                return;
            }
	    //          console.log("skewt.updateui-2");
            let skewtId = this.getDomId(ID_SKEWT);
	    let html = '';
            html += HU.div([ATTR_TITLE,'Download skew-t data',ATTR_ID,this.domId('download'),ATTR_CLASS,'ramadda-clickable',ATTR_STYLE,'text-align:right;margin-right:20px;'],HU.getIconImage('fas fa-download'));
	    html += HU.div([ATTR_ID, skewtId], "");
            this.setContents(html);
	    this.jq('download').click(()=>{
		if(!this.dataObject) {
		    alert('No data available');
		    return;
		}		    
		let keys = Object.keys(this.dataObject);
		keys = Utils.removeItem(keys,'records');
		let data = '';
		keys.forEach((key,idx)=>{
		    if(idx>0) data+=',';
		    data+=key
		})
		data+='\n';
		let cnt = this.dataObject[keys[0]].length;
		for(let i=0;i<cnt;i++) {
		    keys.forEach((key,idx)=>{
			if(idx>0) data+=',';
			let v = parseFloat(this.dataObject[key][i]);
			v = Utils.trimDecimals(v,4);
			data+=v;
		    });
		    data+='\n';
		}
		Utils.makeDownloadFile(this.getProperty(ATTR_TITLE,'skewt').replace(/ /g,'_')+'.csv',data);

	    });
            let date = records[0].getDate();
            if(this.jq(ID_DATE_LABEL).length==0) {
                this.jq(ID_TOP_LEFT).append(HtmlUtils.div([ID,this.getDomId(ID_DATE_LABEL)]));
            }
            if(date!=null) {
                this.jq(ID_DATE_LABEL).html("Date: " + this.formatDate(date));
            } else {
                this.jq(ID_DATE_LABEL).html("");
            }
            let options = {};
            if (this.propertyDefined("showHodograph"))
                options.showHodograph = this.getProperty("showHodograph", true);
            if (this.propertyDefined("showText"))
                options.showText = this.getProperty("showText", true);

            if (this.propertyDefined("skewtWidth"))
                options.skewtWidth = parseInt(this.getProperty("skewtWidth"));
            if (this.propertyDefined("skewtHeight"))
                options.skewtHeight = parseInt(this.getProperty("skewtHeight"));
            if (this.propertyDefined("hodographWidth")){
                options.hodographWidth = parseInt(this.getProperty("hodographWidth"));
            }
            if (this.propertyDefined("windStride")){
                options.windStride = parseInt(this.getProperty("windStride"));
            }
            options.showText = this.getProperty("showText",true);
            //            options.hodographWidth = 200;
            let fields = this.getData().getRecordFields();
            let names = [
                {id:'pressure',aliases:['vertCoord','pressure_mb']},
                {id:'height',aliases:['Geopotential_height_isobaric']},
                {id:'temperature',aliases:['Temperature_isobaric','temperature_c']},
                {id:'dewpoint',aliases:[]},
                {id:'rh',aliases:['Relative_humidity_isobaric','relative_humidity']},
                {id:'wind_direction',aliases:['wind_direction_true_deg']},
                {id:'wind_speed',aliases:['wind_speed_m_s']},
                {id:'uwind',aliases:['u-component_of_wind_isobaric','u']},
                {id:'vwind',aliases:['v-component_of_wind_isobaric','v']},
            ];
            //TODO: check for units
            let data ={};
            let dataFields ={};
            for(let i=0;i<names.length;i++) {
                let obj = names[i];
                let id = obj.id;
                let field = this.getFieldById(fields,id,false,true);
                if(field == null) {
                    for(let j=0;j<obj.aliases.length;j++) {
                        field = this.getFieldById(fields,obj.aliases[j],false,true);
                        if(field) break;
                    }
                }
                if(field) {
                    data[id] = this.getColumnValues(records, field).values;
                    dataFields[id]=field;
                }
            }

	    let getFieldIds = () =>{
		return fields.reduce((acc,field)=>{
		    return acc+field.getId()+"<br>";
		},"<br>Fields:<br>");
	    }
	    

            if(!data.pressure) {
                this.displayError("No pressure defined in data." + getFieldIds());
                return;
            }

            if(!data.temperature) {
                this.displayError("No temperature defined in data." + getFieldIds());
                return;
            }

	    data.records = records;
            if(!data.height) {
                let pressures = [
                    1013.25, 954.61, 898.76, 845.59, 795.01, 746.91, 701.21,
                    657.80, 616.6, 577.52, 540.48, 505.39, 472.17, 440.75,
                    411.05, 382.99, 356.51, 331.54, 303.00, 285.85, 264.99,
                    226.99, 193.99, 165.79, 141.70, 121.11, 103.52, 88.497,
                    75.652, 64.674, 55.293, 25.492, 11.970, 5.746, 2.871,
                    1.491, 0.798, 0.220, 0.052, 0.010,];
                let alts = [
                    0, 500, 1000, 1500, 2000, 2500, 3000, 3500, 4000, 4500, 5000,
                    5500, 6000, 6500, 7000, 7500, 8000, 8500, 9000, 9500, 10000,
                    11000, 12000, 13000, 14000, 15000, 16000, 17000, 18000, 19000,
                    20000, 25000, 30000, 35000, 40000, 45000, 50000, 60000, 70000,
                    80000,
                ];
                
                data.height = [];
                for(let i=0;i<data.pressure.length;i++) {
                    let pressure = data.pressure[i];
                    let alt = alts[alts.length-1];
                    for(let j=0;j<pressures.length;j++) {
                        if(pressure>=pressures[j]) {
                            if(j==0) alt = 0;
                            else {
                                let p1 = pressures[j-1];
                                let p2 = pressures[j];
                                let a1 = alts[j-1];
                                let a2 = alts[j];
                                let percent = 1-(pressure-p2)/(p1-p2);
                                alt = (a2-a1)*percent+a1;
                            }
                            break;
                        }
                    }
                    data.height.push(alt);
                }
            }


            if(!data.dewpoint) {
                if(!data.rh) {
                    this.displayError("No dewpoint or rh." + getFieldIds());
                    return;
                }
                data.dewpoint = [];
                for(let i=0;i<data.rh.length;i++) {
                    let rh=data.rh[i];
                    let t=data.temperature[i];
                    let dp = t-(100-rh)/5;
                    data.dewpoint.push(dp);
                }
            }

            if(!data.wind_speed) {
		if(!data.uwind || !data.vwind) {
                    this.displayError("No wind speed defined in data."  + getFieldIds());
                    return;
                }
                data.wind_speed = [];
                data.wind_direction = [];
                for(let i=0;i<data.uwind.length;i++) {
                    let u = data.uwind[i];
                    let v = data.vwind[i];
                    let ws = Math.sqrt(u*u+v*v);
                    let wdir = 180+(180/Math.PI)*Math.atan2(v,u);
                    data.wind_speed.push(ws);
                    data.wind_direction.push(wdir);
                }
            }

            let alldata = data;
            data = {};
            //if any missing then don't include
            for(a  in alldata) data[a] = [];
            alldata[names[0].id].map((v,idx)=>{
                let ok = true;
                for(id in alldata) {
                    if(isNaN(alldata[id][idx])) {
                        ok = false;
                        break;
                    }
                }
                if(ok || id=='records') {
                    for(id in alldata) {
                        data[id].push(alldata[id][idx]);
                    }
                }
            });

            if(data.height.length>1) {
                if(data.height[0]>data.height[1]) {
                    for(name in data)
                        data[name] = Utils.reverseArray(data[name]);
                }
            }
            if(data.temperature.length==0) {
                this.displayError(this.getNoDataMessage());
                return;
            }
	    this.dataObject = data;

            options.myid = this.getId();
	    options.mouseDownListener = (record)=>{
		if(record) {
		    this.propagateEventRecordSelection({highlight:true,record: record});
		}
	    }
            try {
                this.skewt = new D3Skewt(skewtId, options,data);
            } catch(e) {
                this.displayError("An error occurred: " +e);
                console.log("error:" + e.stack);
                return;
            }
            await this.getDisplayEntry((e)=>{
		if(!e) return;
                let q= e.getAttribute("variables");
                if(!q) return;
                q = q.value;
                q = q.replace(/\r\n/g,"\n");
                q = q.replace(/^ *\n/,"");
                q = q.replace(/^ *([^:]+):([^\n].*)$/gm,"<div title='$1' class=display-skewt-index-label>$1</div>: <div title='$2'  class=display-skewt-index>$2</div>");
                q = q.replace(/[[\r\n]/g,"\n");
                q = HtmlUtils.div([ATTR_CLASS, "display-skewt-text"],q);
                $("#" + this.skewt.textBoxId).html(q);
            });
        }
    });
}


function RamaddaD3Display(displayManager, id, properties) {
    const ID_SVG = "svg";
    const SUPER = new RamaddaDisplay(displayManager, id, "d3", properties);
    defineDisplay(addRamaddaDisplay(this), SUPER, [], {
        initDisplay: function() {
            this.createUI();
            this.setDisplayTitle(properties.graph.title);

            //Note: use innerHeight/innerWidth wiki attributes
            let width = this.getProperty("innerWidth", 600);
            let height = this.getProperty("innerHeight", 300);
            let margin = {
                top: 20,
                right: 50,
                bottom: 30,
                left: 50
            };
            let divStyle =
                "height:" + height + "px;" +
                "width:" + width + "px;";
            let html = HtmlUtils.div([ATTR_ID, this.getDomId(ID_SVG), ATTR_STYLE, divStyle], "");
            this.setContents(html);

            // To create dynamic size of the div
            this.displayHeight = parseInt((d3.select("#" + this.getDomId(ID_SVG)).style("height")).split("px")[0]) - margin.top - margin.bottom; //this.getProperty("height",300);//d3.select("#"+this.getDomId(ID_SVG)).style("height");//
            this.displayWidth = parseInt((d3.select("#" + this.getDomId(ID_SVG)).style("width")).split("px")[0]) - margin.left - margin.right; //this.getProperty("width",600);//d3.select("#"+this.getDomId(ID_SVG)).style("width");//

            //                console.log("WxH:" + this.displayHeight +" " + this.displayWidth);

            // To solve the problem with the classess within the class
            let myThis = this;
            let zoom = d3.behavior.zoom()
                .on("zoom", function() {
                    myThis.zoomBehaviour()
                });
            this.zoom = zoom;
            this.svg = d3.select("#" + this.getDomId(ID_SVG)).append("svg")
                .attr("width", this.displayWidth + margin.left + margin.right)
                .attr("height", this.displayHeight + margin.top + margin.bottom)
                .attr(ATTR_CLASS, "D3graph")
                .call(zoom)
                .on("click", function(event) {
                    myThis.click(event)
                })
                .on("dblclick", function(event) {
                    myThis.dbclick(event)
                })
                .append("g")
                .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

            // Define the Axis
            // 100 pixels for the legend... lets see if we keep it
            this.x = D3Util.getAxis(properties.graph.axis.x.type, [0, this.displayWidth - 100]);
            this.y = D3Util.getAxis(properties.graph.axis.y.type, [this.displayHeight, 0]);

            this.xAxis = d3.svg.axis()
                .scale(this.x)
                .orient("bottom");

            this.yAxis = d3.svg.axis()
                .scale(this.y)
                .orient("left");

            // Add Axis to the plot
            this.svg.append("g")
                .attr(ATTR_CLASS, "x axis")
                .attr("transform", "translate(0," + this.displayHeight + ")")
                .call(this.xAxis);

            this.svg.append("g")
                .attr(ATTR_CLASS, "y axis")
                .call(this.yAxis);


            // Color Bar
            let colors = ["#00008B", "#0000CD", "#0000FF", "#00FFFF", "#7CFC00", "#FFFF00", "#FFA500", "#FF4500", "#FF0000", "#8B0000"];

            let colorSpacing = 100 / (colors.length - 1);

            let colorBar = D3Util.addColorBar(this.svg, colors, colorSpacing, this.displayWidth);
            this.color = d3.scale.category10();
            this.updateUI();
        },
        needsData: function() {
            return true;
        },
        initDialog: function() {
            this.addFieldsCheckboxes();
        },
        getDialogContents: function() {
            let height = this.getProperty(PROP_HEIGHT, "400");
            let html = HtmlUtils.div([ATTR_ID, this.getDomId(ID_FIELDS), ATTR_CLASS, "display-fields", ]);
            html += SUPER.getDialogContents.apply(this);
            return html;
        },
        fieldSelectionChanged: function() {
            this.updateUI();
        },
        // onlyZoom is not updating the axis
        updateUI: function() {
            //Note: Not sure why onlyZoom was a function param. The pointData gets passes in 
            //when the json is loaded
            //            updateUI: function(onlyZoom) {
            let onlyZoom = false;

            //Note: if we write to the SVG dom element then we lose the svg object that got created in initDisplay
            //Not sure how to show a message to the user
            if (!this.hasData()) {
                return;
            }
            test = this;
            let selectedFields = this.getSelectedFields();
            if (selectedFields.length == 0) {
                //this.writeHtml(ID_SVG, "No fields selected");
                return;
            }
            this.addFieldsCheckboxes();
            pointData = this.getData();
            if (pointData == null) {
                //this.writeHtml(ID_SVG, "No data");
                console.log("no data");
                return;
            }

            let fields = pointData.getNumericFields();
            let records = pointData.getRecords();
            let ranges = RecordUtil.getRanges(fields, records);
            let elevationRange = RecordUtil.getElevationRange(fields, records);
            let offset = (elevationRange[1] - elevationRange[0]) * 0.05;
            // To be used inside a function we can use this.x inside them so we extract as variables. 
            let x = this.x;
            let y = this.y;
            let color = this.color;
            let axis = properties.graph.axis;

            if (onlyZoom) {
                this.zoom.x(this.x);
                this.zoom.y(this.y);
            } else {
                // Update axis for the zoom and other changes
                this.x.domain(d3.extent(records, function(d) {
                    return D3Util.getDataValue(axis.x, d, selectedFields[0].getIndex());
                }));
                // the y domain depends on the first selected element I have to think about it.
                this.y.domain(d3.extent(records, function(d) {
                    return D3Util.getDataValue(axis.y, d, selectedFields[0].getIndex());
                }));

                this.zoom.x(this.x);
                this.zoom.y(this.y);
            }

            this.svg.selectAll(".y.axis").call(this.yAxis);
            this.svg.selectAll(".x.axis").call(this.xAxis);

            // Remove previous lines
            this.svg.selectAll(".line").remove();
            this.svg.selectAll(".legendElement").remove();

            let myThis = this;
            for (let fieldIdx = 0; fieldIdx < selectedFields.length; fieldIdx++) {
                let dataIndex = selectedFields[fieldIdx].getIndex();
                let range = ranges[dataIndex];
                // Plot line for the values
                let line = d3.svg.line()
                    .x(function(d) {
                        return x(D3Util.getDataValue(axis.x, d, selectedFields[fieldIdx].getIndex()));
                    })
                    .y(function(d) {
                        return y(D3Util.getDataValue(axis.y, d, selectedFields[fieldIdx].getIndex()));
                    });

                displayLine = this.svg.append("path")
                    .datum(records)
                    .attr(ATTR_CLASS, "line")
                    .attr("d", line)
                    .on("mousemove", function(event) {
                        myThis.mouseover(event)
                    })
                    .attr("fill", "none")
                    .attr("stroke", function(d) {
                        return color(fieldIdx);
                    })
                    .attr("stroke-width", "0.5px");

                if (properties.graph.axis.z == FIELD_SELECTEDFIELD) {
                    displayLine.attr("stroke", "url(#colorBarGradient)");
                }

                if (properties.graph.derived != null) {
                    let funcs = properties.graph.derived.split(",");
                    for (funcIdx = 0; funcIdx < funcs.length; funcIdx++) {
                        let func = funcs[funcIdx];
                        if (func == FUNC_MOVINGAVERAGE) {
                            // Plot moving average Line
                            let movingAverageLine = d3.svg.line()
                                .x(function(d) {
                                    return x(D3Util.getDataValue(axis.x, d, selectedFields[fieldIdx].getIndex()));
                                })
                                .y(function(d, i) {
                                    if (i == 0) {
                                        return _movingSum = D3Util.getDataValue(axis.y, d, selectedFields[fieldIdx].getIndex());
                                    } else {
                                        _movingSum += D3Util.getDataValue(axis.y, d, selectedFields[fieldIdx].getIndex());
                                    }
                                    return y(_movingSum / i);
                                })
                                .interpolate("basis");
                            this.svg.append("path")
                                .attr(ATTR_CLASS, "line")
                                .attr("d", movingAverageLine(records))
                                .attr("fill", "none")
                                .attr("stroke", function(d) {
                                    return color(fieldIdx);
                                })
                                .attr("stroke-width", "1.5px")
                                .attr("viewBox", "50 50 100 100 ")
                                .style("stroke-dasharray", ("3, 3"));
                        } else {
                            console.log("Error: Unknown derived function:" + func);
                        }

                    }
                }

                // Legend element Maybe create a function or see how we implement the legend
                this.svg.append("svg:rect")
                    .attr(ATTR_CLASS, "legendElement")
                    .attr("x", this.displayWidth - 100)
                    .attr("y", (50 + 50 * fieldIdx))
                    .attr("stroke", function(d) {
                        return color(fieldIdx);
                    })
                    .attr("height", 2)
                    .attr("width", 40);

                this.svg.append("svg:text")
                    .attr(ATTR_CLASS, "legendElement")
                    .attr("x", this.displayWidth - 100 + 40 + 10) // position+color rect+padding
                    .attr("y", (55 + 55 * fieldIdx))
                    .attr("stroke", function(d) {
                        return color(fieldIdx);
                    })
                    .attr(ATTR_STYLE, "font-size:50%")
                    .text(selectedFields[fieldIdx].getLabel());
            }
        },

        zoomBehaviour: function() {
            // Call redraw with only zoom don't update extent of the data.
            this.updateUI(true);
        },
        //this gets called when an event source has selected a record
        handleEventRecordSelection: function(source, args) {},
        mouseover: function(event) {
            // TO DO
            console.log("mouseover");
        },
        click: function(event) {
            // TO DO
            console.log("click:" + event);
        },
        dbclick: function(event) {
            // Unzoom
            this.zoom();
            this.updateUI();
        },
        getSVG: function() {
            return this.svg;
        }
    });
}


function RamaddaD3LineChartDisplay(displayManager, id, properties) {
    var dfltProperties = {};
    //Note: use json structures to define the props
    dfltProperties.graph = {
        title: "Line chart",
        //Note: changed this to "derived" from "extraLine".
        //This is a comma separated list of functions (for now just one)
        derived: FUNC_MOVINGAVERAGE,
        axis: {
            y: {
                type: TYPE_VALUE,
                fieldname: FIELD_SELECTEDFIELD
            },
            x: {
                type: TYPE_TIME,
                fieldname: FIELD_TIME,
            }
        }
    };

    properties = $.extend(dfltProperties, properties);
    return new RamaddaD3Display(displayManager, id, properties);
}


/*
  function RamaddaProfileDisplay(displayManager, id, properties) {
  var dfltProperties = {};
  //Note: use json structures to define the props
  dfltProperties.graph = {
  title: "Profile chart",
  derived: null,
  axis: {
  y: {
  type: TYPE_ELEVATION,
  fieldname: FIELD_DEPTH,
  fieldIdx: 3,
  reverse: true
  },
  x: {
  type: TYPE_VALUE,
  fieldname: FIELD_VALUE,
  },
  }
  };
  //Note: now set the properties
  properties = $.extend(dfltProperties, properties);
  return new RamaddaD3Display(displayManager, id, properties);
  }
*/



function RamaddaGliderCrossSectionDisplay(displayManager, id, properties) {
    var dfltProperties = {};
    //Note: use json structures to define the props
    dfltProperties.graph = {
        title: "Glider cross section",
        derived: null,
        axis: {
            y: {
                type: TYPE_ELEVATION,
                fieldname: FIELD_DEPTH,
                reverse: true
            },
            x: {
                type: TYPE_TIME,
                fieldname: FIELD_TIME,
            },
            z: FIELD_SELECTEDFIELD,
        }
    };
    properties = $.extend(dfltProperties, properties);

    return new RamaddaD3Display(displayManager, id, properties);
}






var loadedVenn = false;
function RamaddaVennDisplay(displayManager, id, properties) {
    const ID_VENN = "venn";
    const SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_VENN, properties);
    defineDisplay(addRamaddaDisplay(this), SUPER, [], {
        getContentsStyle: function() {
            return "";
        },
        checkLayout: function() {
            this.updateUIInner();
        },
        updateUI: function() {
	    if(!loadedVenn) {
		loadedVenn = true;
		var includes = "<script src='" + RamaddaUtil.getCdnUrl("/lib/venn.js")+"'></script>";
		this.writeHtml(ID_DISPLAY_TOP, includes);
	    }
	    let _this = this;
	    var func = function() {
		if(!window["venn"]) {
		    setTimeout(func, 1000);
		} else {
                    _this.updateUIInner();
		}
            };
	    func();
        },
        updateUIInner: function() {
            let records = this.filterData();
            if (!records) {
                return;
            }
            var allFields = this.getData().getRecordFields();
            var fields = this.getSelectedFields(allFields);
            if (fields.length == 0)
                fields = allFields;
            var strings = this.getFieldsByType(fields, "string");
            if (strings.length == 0) {
                this.displayError("No string fields specified");
                return;
            }
            /*
              var sets = [{sets : [0], label : 'SE', size : 28,}, 
              {sets : [1], label : 'Treat', size: 35},
              {sets : [2], label : 'Anti-CCP', size : 108}, 
              {sets : [3], label : 'DAS28', size:106},
              {sets : [0,1], size:1},
              {sets : [0,2], size:1},
              {sets : [0,3], size:14},
            */
            var setInfos = {};
            var setCnt = 0;
            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                var row = this.getDataValues(records[rowIdx]);
                var keys = [];
                var key = "";
                for (var fieldIdx = 0; fieldIdx < strings.length; fieldIdx++) {
                    var field = strings[fieldIdx];
                    var value = row[field.getIndex()];
                    var setKey = field.getId() + "--" + value;
                    keys.push(setKey);
                    key += "--" + setKey;
                    if (!Utils.isDefined(setInfos[setKey])) {
                        setInfos[setKey] = {
                            count: 0,
                            setIds: [setCnt],
                            label: value,
                        };
                        setCnt++;
                    }
                    setInfos[setKey].count++;
                }
                var ids = [];
                if (!Utils.isDefined(setInfos[key])) {
                    for (var i = 0; i < keys.length; i++) {
                        ids.push(setInfos[keys[i]].setIds[0]);
                    }
                    setInfos[key] = {
                        count: 0,
                        setIds: ids,
                        label: null,
                    };
                }
                setInfos[key].count++;
            }

            var sets = [];
            for (var a in setInfos) {
                var setInfo = setInfos[a];
                var obj = {
                    sets: setInfo.setIds,
                    size: setInfo.count
                };
                if (setInfo.label)
                    obj.label = setInfo.label;
                sets.push(obj);
            }
            this.setContents(HtmlUtils.div([ATTR_ID, this.getDomId(ID_VENN), ATTR_STYLE, "height:300px;"], ""));
            var chart = venn.VennDiagram()
                .width(600)
                .height(400);
            var id = "#" + this.getDomId(ID_VENN);
            var strokeColors = this.getColorTable(true, "strokeColors", "nice");
            var fillColors = this.getColorTable(true, "fillColors", "nice");
            var textColors = this.getColorTable(true, "textColors");
            if (!textColors)
                textColors = strokeColors;
            d3.select(id).datum(sets).call(chart);
            d3.selectAll(id + " .venn-circle path")
                .style("fill-opacity", parseFloat(this.getProperty("fillOpacity", 0.5)))
                .style("stroke-width", parseInt(this.getProperty("strokeWidth", 1)))
                .style("stroke-opacity", parseFloat(this.getProperty("strokeOpacity", 0.5)))
                .style("stroke", function(d, i) {
                    return i < strokeColors.length ? strokeColors[i] : strokeColors[i % strokeColors.length];
                })
                .style("fill", function(d, i) {
                    return i < fillColors.length ? fillColors[i] : fillColors[i % fillColors.length];
                })
            d3.selectAll(id + " .venn-circle text")
                .style("fill", function(d, i) {
                    return i < textColors.length ? textColors[i] : textColors[i % textColors.length]; 
		})
                .style("font-size", this.getProperty("fontSize", "16px"))
                .style("font-weight", this.getProperty("fontWeight", "100"));

        }
    });
}


function RamaddaMinidotsDisplay(displayManager, id, properties) {
    const ID_MINIDOTS = "minidots";
    const SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_MINIDOTS, properties);
    let myProps = [
	{label:'Minidots Properties'},
	{p:'dateField',ex:''},
	{p:'valueField',ex:''},
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, [], {
        getContentsStyle: function() {
            return "";
        },
        checkLayout: function() {
            this.updateUI();
        },
        updateUI: function() {
            let records = this.filterData();
            if (!records) {
                return;
            }
	    let dotsWidth = +this.getProperty("dotsWidth",500);
	    let dotsHeight = +this.getProperty("dotsHeight",200);	    
	    let valueField = this.getFieldById(null,this.getPropertyValueField());
	    let dateField = this.getFieldById(null,this.getPropertyDateField());
	    let groupByField = this.getFieldById(null,this.getProperty("groupBy"));
	    let divisor = +this.getProperty("divisor",1);
	    if(!valueField) {
                this.displayError("No value field specified");
		return;
	    }
	    if(!dateField) dateField = this.getFieldByType(null,"date");
	    let dateToCount = {};
	    let minDate=null, maxDate = null;
	    let groups = {};

	    records.forEach(record=>{
		let date = dateField?record.getValue(dateField.getIndex()):record.getDate();
		minDate = minDate!=null?Math.min(minDate,date.getTime()):date;
		maxDate = maxDate!=null?Math.max(maxDate,date.getTime()):date;
	    });
	    records.forEach(record=>{
		let groupByValue = groupByField?record.getValue(groupByField.getIndex()):"all";
		let date = dateField?record.getValue(dateField.getIndex()):record.getDate();
		minDate = minDate!=null?Math.min(minDate,date.getTime()):date;
		maxDate = maxDate!=null?Math.max(maxDate,date.getTime()):date;
		let data = groups[groupByValue];
		if(!data) {
		    groups[groupByValue] = data = {
			records:[],
			total:0,
			list:[],
			seen:{}
		    }
		}
		data.records.push(record);
		let value = record.getValue(valueField.getIndex());
		data.total+=value;
		value = value/divisor;
		if(data.list.length>5000) return;
		for(let i=0;i<value;i++) {
		    data.list.push({x:date.getTime(),
				    y:Math.random(),
				    record:record});
		}
	    });
	    let range = {
		minx:minDate,
		maxx:maxDate,
		miny:0,
		maxy:1
	    }
	    let groupList = Object.keys(groups).sort();
	    if(!groupByField) {
		let data = groups["all"];
		this.setContents(HtmlUtils.div([CLASS,"display-minidots-dots", ID, this.getDomId(ID_MINIDOTS), STYLE, HU.css(HEIGHT,HU.getDimension(dotsHeight),WIDTH,HU.getDimension(dotsWidth))], ""));
		drawDots(this,"#"+ this.getDomId(ID_MINIDOTS),dotsWidth,dotsHeight,data.list,range,null/*colorBy*/);
	    } else {
		let container = this.jq(ID_MINIDOTS);
		let table = "<table border=1>";
		groupList.forEach((key,idx)=>{
		    let data = groups[key];
		    table+="<tr>";
		    table += HU.td([],key +" (" + data.total+")");
		    let id = this.getDomId(ID_MINIDOTS+"_"+idx);
		    table += HU.td([],HtmlUtils.div([CLASS,"display-minidots-dots", ID, id, STYLE, HU.css(HEIGHT,HU.getDimension(dotsHeight),WIDTH,HU.getDimension(dotsWidth))], ""));
		    table+="</tr>\n";
		});
		this.setContents(table);
		groupList.forEach((key,idx)=>{
		    let data = groups[key];
		    let id = this.getDomId(ID_MINIDOTS+"_"+idx);
		    drawDots(this,"#"+ id,dotsWidth,dotsHeight,data.list,range,null/*colorBy*/);
		});
	    }

        }
    });
}



function RamaddaChernoffDisplay(displayManager, id, properties) {
    const ID_VENN = "venn";
    const SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_VENN, properties);
    defineDisplay(addRamaddaDisplay(this), SUPER, [], {
        getContentsStyle: function() {
            return "";
        },
        checkLayout: function() {
            this.updateUIInner();
        },
        timeout: 100,
        written: false,
        updateUI: function() {
            if (!this.written) {
                this.written = true;
                var includes = "<script src='" + RamaddaUtil.getCdnUrl("/lib/chernoff.js")+"'></script>";
                this.writeHtml(ID_DISPLAY_TOP, includes);
            }
            this.updateUIInner();
        },
        updateUIInner: function() {
            let _this = this;
            if (!Utils.isDefined(d3.chernoff)) {
                this.timeout = this.timeout * 2;
                var func = function() {
                    _this.updateUIInner();
                };
                setTimeout(func, this.timeout);
                return;
            }
            let records = this.filterData();
            if (!records) {
                return;
            }
            var allFields = this.getData().getRecordFields();
            var fields = this.getSelectedFields(allFields);
            var numericFields = this.getFieldsByType(fields, "numeric");
            if (numericFields.length == 0) {
                this.displayError("No numeric fields specified");
                return;
            }
            if (fields.length == 0)
                fields = allFields;
            var string = this.getFieldByType(fields, "string");
            var legend = "";
            var colorField = this.getFieldById(allFields, this.getProperty("colorBy"));
            var colorscale;
            if (colorField) {
                var colors = this.getColorTable(true, null, "blue_white_red");
                var colorValues = this.getColumnValues(records, colorField);
                colorscale = [];
                var min = parseFloat(this.getProperty("colorByMin", colorValues.min));
                var max = parseFloat(this.getProperty("colorByMax", colorValues.max));
                var range = max - min;
                //                console.log("range:" +  max +" " +min +" #colors:" + colors.length);
                for (var i = 0; i < colorValues.values.length; i++) {
                    var value = colorValues.values[i];
                    var percent = (value - min) / range;
                    var idx = Math.round(percent * (colors.length - 1));
                    //                    console.log(idx+" " +colors[idx] +" " + value + " " + percent);
                    colorscale.push(colors[idx]);
                }
                this.displayColorTable(colors, ID_DISPLAY_BOTTOM, min, max);
                legend += "<b>Colored by</b>: " + colorField.getLabel() + "&nbsp;&nbsp;";
            }
            var attrs = [{
                label: "Face width",
                name: "face",
                key: "f",
                min: 0,
                max: 1
            }, {
                label: "Hair",
                name: "hair",
                key: "h",
                min: -1,
                max: 1
            }, {
                label: "Mouth",
                name: "mouth",
                key: "m",
                min: -1,
                max: 1
            }, {
                label: "Nose height",
                name: "noseHeight",
                key: "nh",
                min: 0,
                max: 1
            }, {
                label: "Nose width",
                name: "noseWidth",
                key: "nw",
                min: 0,
                max: 1
            }, {
                label: "Eyes height",
                name: "eyesHeight",
                key: "eh",
                min: 0,
                max: 1
            }, {
                label: "Eyes width",
                name: "eyesWidth",
                key: "ew",
                min: 0,
                max: 1
            }, {
                label: "Brow",
                name: "brow",
                key: "b",
                min: -1,
                max: 1
            }];
            var html = "";
            var showHelp = this.getProperty("showHelp", false);
            if (showHelp) {
                html += "Settings:<br><table class=ramadda-table><thead><tr><th>Attribute&nbsp;</th><th>&nbsp;Default range&nbsp;</th><th>&nbsp;Set field&nbsp;</th><th>&nbsp;Set min&nbsp;</th><th>&nbsp;Set max&nbsp;</th></tr></thead><tbody>";
            }

            for (a in attrs) {
                var attr = attrs[a];
                if (showHelp) {
                    html += "<tr><td>" + attr.label + "</td><td align=center>" + attr.min + " - " + attr.max + "</td><td>" + attr.name + "Field=&lt;field_id&gt;" + "</td><td>" + attr.name + "Min=&lt;min_value&gt;" + "</td><td>" + attr.name + "Max=&lt;max_value&gt;" + "</td></tr>";
                }
                attr.field = this.getFieldById(allFields, this.getProperty(attr.name + "Field"));
                if (attr.field) {
                    legend += "<b>" + attr.label + "</b>: " + attr.field.getLabel() + "&nbsp;&nbsp;";
                    if (Utils.isDefined(this.getProperty(attr.name + "Min"))) {
                        attr.min = parseFloat(this.getProperty(attr.name + "Min"));
                    }
                    if (Utils.isDefined(this.getProperty(attr.name + "Max"))) {
                        attr.max = parseFloat(this.getProperty(attr.name + "Max"));
                    }
                    attr.column = this.getColumnValues(records, attr.field);
                }
            }
            if (showHelp) {
                html += "</tbody></table>";
            }

            var sortField = this.getFieldById(allFields, this.getProperty("sortBy"));

            var rows = [];
            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                var blob = {
                    values: this.getDataValues(records[rowIdx])
                };
                if (colorscale) blob.color = colorscale[rowIdx]
                rows.push(blob);
            }

            if (sortField) {
                rows.sort(function(a, b) {
                    var v1 = a.values[sortField.getIndex()];
                    var v2 = b.values[sortField.getIndex()];
                    if (v1 < v2) return 1;
                    if (v1 > v2) return -1;
                    return 0;
                });
            }

            var data = [];
            for (var rowIdx = 0; rowIdx < rows.length; rowIdx++) {
                var blob = rows[rowIdx];
                var row = blob.values;
                var color = blob.color;
                var faceData = {
                    f: 0.5, //0-1
                    h: 0, //-1-1
                    m: 0, //-1-1
                    nh: 0.5, //0-1
                    nw: 0.5, //0-1
                    eh: 0.5, //0-1
                    ew: 0.5, //0-1
                    b: 0 //-1-1
                };
                data.push({
                    faceData: faceData,
                    color: color
                });
                var tt = "";
                for (a in attrs) {
                    var attr = attrs[a];
                    var field = attr.field;
                    if (!field) {
                        faceData[attr.key] = attr.min + (attr.max - attr.min) / 2;
                    } else {
                        var value = row[field.getIndex()];
                        var min = attr.column.min;
                        var max = attr.column.max;
                        tt += field.getLabel() + ": " + value + " range: " + min + " - " + max + " (" + attr.label + ")\n";
                        if (max != min) {
                            var percent = (value - min) / (max - min);
                            var adjValue = attr.min + (attr.max - attr.min) * percent;
                            //                            console.log(" %:" + percent + " v:" + value +" min:" + min +" max:" + max +" adj:" + adjValue);
                            faceData[attr.key] = adjValue;
                        }
                    }
                }
                var label = (string ? row[string.getIndex()] : "Row: " + rowIdx);
                var labelValue = (string ? row[string.getIndex()] : "");
                label = HtmlUtils.div([ATTR_CLASS, "display-chernoff-label"], label);
                var div = HtmlUtils.div([ATTR_ID, this.getDomId("chernoff") + "_" + rowIdx, ATTR_CLASS, "display-chernoff-face"], "");
                html += HtmlUtils.div([ATTR_TITLE, tt, ATTR_CLASS, "display-chernoff-wrapper ramadda-div-link", "value", labelValue], div + label);
            }
            legend = HtmlUtils.div([ATTR_CLASS, "display-chernoff-legend"], legend);
            var height = this.getProperty("height", "400px");
            if (!height.endsWith("px")) height += "px";
            this.setContents(legend + HtmlUtils.div([ATTR_STYLE, "height:" + height + ";", ATTR_CLASS, "display-chernoff-container", ATTR_ID, this.getDomId("chernoff")], html));
            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                var div = "#" + this.getDomId("chernoff") + "_" + rowIdx;
                this.makeFace(div, data[rowIdx].faceData, data[rowIdx].color);
            }

            if (string) {
                this.find(".ramadda-div-link").click(function() {
                    var value = $(this).attr("value");
                    _this.propagateEvent("fieldValueSelected", {
                        field: string,
                        value: value
                    });
                });
            }
        },
        makeFace: function(div, faceData, color) {
            function chernoffFace() {
                var width = 400,
                    height = 200;
                var chernoff = d3.chernoff()
                    .face(function(d) {
                        return d.f;
                    })
                    .hair(function(d) {
                        return d.h;
                    })
                    .mouth(function(d) {
                        return d.m;
                    })
                    .nosew(function(d) {
                        return d.nw;
                    })
                    .noseh(function(d) {
                        return d.nh;
                    })
                    .eyew(function(d) {
                        return d.ew;
                    })
                    .eyeh(function(d) {
                        return d.eh;
                    })
                    .brow(function(d) {
                        return d.b;
                    });

                function data() {
                    return [faceData];
                }

                function drawFace(selection) {
                    var svg = selection.append("svg")
                        .attr("width", width)
                        .attr("height", height);
                    var face = svg.selectAll("g.chernoff")
                        .data(data())
                        .enter().append("g")
                        .attr(ATTR_CLASS, "chernoff")
                        .call(chernoff);
                    if (color)
                        face.attr(ATTR_STYLE, "fill:" + color);
                }

                function draw(selection) {
                    selection.call(drawFace);
                }
                return draw;
            }
            d3.select(div)
                .call(chernoffFace());
        }





    });
}



function RamaddaD3bubbleDisplay(displayManager, id, properties) {
    const ID_BUBBLES = "bubbles";
    const SUPER = new RamaddaDisplay(displayManager, id, DISPLAY_D3BUBBLE, properties);
    if(!window["BubbleChart"]) {
	Utils.importJS(RamaddaUtil.getCdnUrl("/lib/d3/d3-legend.min.js"));
	Utils.importJS(RamaddaUtil.getCdnUrl("/lib/d3/bubblechart.js"));
    }
    let myProps = [
	{label:'Bubble Chart'},
	{p:'labelField',ex:''},
	{p:'colorBy',ex:''},
	{p:'valueField',ex:''},
	{p:'descriptionField',ex:''},
	{p:'imageField',ex:''},
	{p:'sizeLabel1',ex:''},
	{p:'sizeLabel2',ex:''},
	{p:'showSizeLegend',ex:'false'}
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        needsData: function() {
            return true;
        },
	callbackWaiting:false,
        displayData: function() {
	    this.updateUI();
	},
        updateUI: function() {
            if(!window["BubbleChart"]) {
		if(!this.callbackWaiting) {
		    this.callbackWaiting = true;
                    setTimeout(()=>{
			this.callbackWaiting = false;
			this.updateUI()
		    },100);
		}
                return;
            }
	    //If the width is 0 then there is an error in the d3
	    if(this.getContents().width() ==0)  {
		this.setContents("...");
		return;
	    }
	    let records = this.filterData();
	    if (!records) {
                return;
	    }  
            let colorByField = this.getFieldById(null, this.getProperty("colorBy","category"));
	    let valueField = this.getFieldById(null, this.getProperty("valueField"));
	    if(colorByField)
		this.setProperty("sortFields",colorByField.getId());
	    let html = HtmlUtil.tag("svg", [ATTR_ID, this.getDomId(ID_BUBBLES),
					    "width","100%","height","700", "font-family","sans-serif","font-size","10", "text-anchor","middle"])
	    this.setContents(html);
	    let values;
	    let min = 0;
	    let max = 0;
	    if(valueField) {
		values =  this.getColumnValues(records, valueField).values;
		values.map((v,idx)=>{
		    min  = idx==0?v:Math.min(v,min);
		    max  = idx==0?v:Math.max(v,max);
		});
	    }

	    let labelField = this.getFieldById(null, this.getProperty("labelField","name"));
	    let descField = this.getFieldById(null, this.getProperty("descriptionField"));	    
	    let imageField = this.getFieldById(null, this.getProperty("imageField"));	    
	    let template = this.getProperty("template","${default}");
	    if(!labelField) {
                this.setContents(this.getMessage("No label field found"));
		return
	    }

	    let data =[];
	    records.map(r=>{
		let desc =  descField?r.getValue(descField.getIndex()):this.getRecordHtml(r,null, template);
		let label = r.getValue(labelField.getIndex());
		let obj = {
		    name: label,
		    icon:label,
		    desc:desc,
		    cat:"",
		    value:10,
		}
		if(colorByField)
		    obj.cat = r.getValue(colorByField.getIndex());
		if(valueField)
		    obj.value = r.getValue(valueField.getIndex());
		if(imageField)
		    obj.icon = r.getValue(imageField.getIndex());
		data.push(obj);
	    });
	    if(data.length==0) {
		this.setContents(this.getMessage(this.getNoDataMessage()));
		return;
	    }
	    let colors =  this.getColorTable(true);
	    new BubbleChart("#"+this.domId(ID_BUBBLES),data,{
		label1:this.getProperty("sizeLabel1"),
		label2: this.getProperty("sizeLabel2"), 
		colors:colors,
		showSizeLegend: this.getProperty("showSizeLegend",valueField!=null)
	    });
	}
    })
}
