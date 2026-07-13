const DISPLAY_PLOTLY_INTERVAL = 'interval';
addGlobalDisplayType({
    type: DISPLAY_PLOTLY_INTERVAL,
    label: 'Interval View',
    category: CATEGORY_CHARTS,
    tooltip: makeDisplayTooltip('Interval View', null)
});


function RamaddaIntervalDisplay(displayManager, id, properties) {
    if(!Utils.isDefined(properties.width)) properties.width=HU.px(600);
    if(!Utils.isDefined(properties.height)) properties.height=HU.px(400);
    let SUPER = new RamaddaPlotlyDisplay(displayManager, id, DISPLAY_PLOTLY_DOTPLOT, properties);
    let myProps = [
	{label:'Interval Display'},
	{p:'fields',ex:''},
	{p:'labelField',ex:''},
	{p:'categoryField',ex:''},
	{p:'colorField',ex:''},	
	{p:'lineColor',d:'rgba(156, 165, 196, 1.0)'},
        {p:"yAxisTitle"},
	{p:"yAxisType",ex:'log'},
	{p:"yAxisShowLine",d:true},
	{p:"yAxisShowGrid",d:false},
	{p:"xAxisTitle"},
	{p:"xAxisType",ex:'log'},
	{p:"xAxisShowGrid",d:false},
	{p:"xAxisShowLine",d:true},
	{p:"marginLeft",d:140},
	{p:"marginRight",d:40},
	{p:"marginBottom",d:50},
	{p:"marginTop",d:20},
	{p:"chartFill"},
	{p:"chartAreaFill"},
	
    ];

    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        getDisplayStyle: function() {
            return "";
        },

        updateUIInner: function() {
	    let _this = this;
            let records = this.filterData();
            if (!records) return;
            let pointData = this.getData();

            if (pointData == null) return;
            let allFields = pointData.getRecordFields();
            let categoryField = this.getFieldById(allFields,this.getCategoryField());
            let colorField = this.getFieldById(allFields,this.getColorField());	    
            let startField = this.getFieldById(allFields,this.getProperty('startField',''));
            let endField = this.getFieldById(allFields,this.getProperty('endField',''));	    
            let labelField = this.getFieldById(allFields,this.getLabelField());
            let textTemplate = this.getProperty('textTemplate');
            if (!startField || !endField) {
                this.displayError("No fields specified");
                return;
            }
            if (!labelField) {
                this.displayError("No label field specified");
                return;
            }	    


            let labelName = "";
            if (labelField) {
                labelName = labelField.getLabel();
            }
	    let color = this.getLineColor();
            let colorBy = this.getColorByInfo(records);

            let plotData = [];
	    let base=[];
            let labels = [];

	    let values = [];
	    let yValues = [];
	    let tickValues = [];
	    let tickLabels = [];


            let colorValues = [];	    
	    let tooltips = null;
	    let text=null;
	    if(textTemplate)  text = [];
	    let tooltip= this.getTooltip(null);


	    let lastCategory=null;
	    let pointCnt = 0;
	    let recordMap={};
	    
	    records.forEach(record=>{
		if(categoryField) {
		    let category = categoryField.getValue(record);
		    if(category!=lastCategory) {
			lastCategory = category;
			values.push(1);
			base.push(1);
			labels.push(HU.b(category));
			if(text) text.push('');
			colorValues.push('transparent');
			if(tooltips) tooltips.push('');
			yValues.push(pointCnt);
			pointCnt++;
		    }
		}
		yValues.push(pointCnt);
		recordMap[pointCnt++] = record;
		let baseValue = startField.getValue(record);
		base.push(baseValue);
		values.push(endField.getValue(record)-baseValue);		
		labels.push(labelField.getValue(record));
		if(textTemplate) {
		    text.push(this.getRecordHtml(record,null,textTemplate));
		}
		if(colorBy.isEnabled()) {
		    let value = record.getData()[colorBy.index];
		    colorValues.push(colorBy.getColor(value, record));
		} else {
		    if(colorField) {
			let c = colorField.getValue(record);
			if(Utils.stringDefined(c)) {
			    colorValues.push(c);
			} else {
			    colorValues.push(color);
			}
		    }
		    colorValues.push(color);
		}
	    });

            plotData.push({
                type: 'bar',
		orientation: 'h',
                mode: 'markers',
                name: startField.getLabel() + ' - ' +endField.getLabel(),
		base:base,
                x: values,
                y: yValues,
		text:text,
		hoverinfo: 'none',
                marker: {
                    color: colorValues,
                    line: {
                        color: 'green',
                        width: 0,
                    },
                }
            });

	    
            let layout = this.makeLayout({
		categoryorder: 'array',
		categoryarray: labels,

                yaxis: {
		    tickvals: yValues,
		    ticktext: labels,
		    autorange: 'reversed' ,
                    title: this.getYAxisTitle(labelName),
		    type:this.getYAxisType(),
                    showline: this.getYAxisShowLine(),
                    showgrid: this.getYAxisShowGrid(),
                    tickfont: {
                        font: {
                            color: 'rgb(102, 102, 102)',
                        }
                    },
                },
                xaxis: {
		    side:'top',
                    title: this.getXAxisTitle(startField.getLabel() + ' - ' +endField.getLabel()),
		    type:this.getXAxisType(),
                    showgrid: this.getXAxisShowGrid(false),
                    showline: this.getXAxisShowLine(true),
                    linecolor: 'rgb(102, 102, 102)',
                    titlefont: {
                        font: {
                            color: 'rgb(204, 204, 204)'
                        }
                    },
                    tickfont: {
                        font: {
                            color: 'rgb(102, 102, 102)'
                        }
                    },
                    autotick: true,
                    ticks: 'outside',
                    tickcolor: 'rgb(102, 102, 102)'
                },
                margin: {
                    l: this.getMarginLeft(),
                    r: this.getMarginRight(40),
                    b: this.getMarginBottom(),
                    t: this.getMarginTop(50),
                },
                legend: {
                    font: {
                        size: 10,
                    },
                    yanchor: 'middle',
                    xanchor: 'right'
                },
                paper_bgcolor: this.getChartFill(this.getProperty("chart.fill",'rgb(254, 247, 234)')),
                plot_bgcolor: this.getChartAreaFill(this.getProperty("chartArea.fill", 'rgb(254, 247, 234)')),
                hovermode: 'closest'
            });

            this.setDimensions(layout, 2);
	    let rowHeight=28;
	    layout.height =   layout.margin.t + layout.margin.b +
		rowHeight * yValues.length; 

	    const maxLabelLength = Math.max(...labels.map(s => s.length));
	    layout.margin.l = Math.min(300,maxLabelLength*8);
            this.makePlot(plotData, layout);
	    this.jq(ID_PLOT).css(CSS_MAX_HEIGHT,this.getProperty('height',HU.px(400))).css(CSS_OVERFLOW_Y,OVERFLOW_AUTO);


	    if(tooltip) {
		setTimeout(()=>{
		    this.tooltipDiv = $("<div>")
			.addClass("display-tooltip")
			.css({
			    position: "fixed",
			    display: "none",
			    zIndex: 10000,
			    pointerEvents: "none"
			})
			.appendTo(document.body);

		    let plotDiv = document.getElementById(this.getDomId(ID_PLOT));
		    plotDiv.on('plotly_unhover', eventData =>{
			this.tooltipDiv.hide();
		    });
		    plotDiv.on('plotly_hover', eventData=> {
			let point = eventData.points && eventData.points[0];
			if (!point) return;
			let record = recordMap[point.pointIndex];
			if(!record) {
			    console.log('null record',point.pointIndex);
			    return;
			}
			let html = this.getRecordHtml(record,null,tooltip);
			html = HU.div([ATTR_CLASS,CLASS_POPUP],html);
			this.tooltipDiv.html(html).css({
			    left: eventData.event.clientX + 12,
			    top: eventData.event.clientY + 12}).show();
		    });
		},1);
	    }



	    if(colorBy.isEnabled()) {
		colorBy.displayColorTable();
	    }

        },
    });
}




