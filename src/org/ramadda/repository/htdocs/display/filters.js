/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/


var FILTER_ALL = "-all-";

//class: BaseFilter
function BaseFilter(display,properties) {
    this.display = display;
    if (properties == null) properties = {};
    RamaddaUtil.defineMembers(this, {
        properties: properties,
	isEnabled: function() {
	    return true;
	},
	prepareToFilter: function() {
	},
        isRecordOk: function(display, record, values) {
            return true;
        },
	getWidget: function() {return ""},
	initWidget: function(inputFunc) {}
    });
}



//class: BoundsFilter
function BoundsFilter(display, properties) {
    RamaddaUtil.inherit(this, new BaseFilter(display, properties));
    $.extend(this, {
	enabled:true,
	getWidget: function() {
	    let id = this.display.getDomId("boundsfilter");
	    return HtmlUtils.span([STYLE,HU.css("margin-left","4px","margin-right","4px"), ID,id,CLASS,"ramadda-clickable", TITLE,"Filter records on map view. Shift-click to clear"], HtmlUtils.getIconImage("fas fa-globe-americas"));
	},
	initWidget: function(inputFunc) {
	    this.inputFunc = inputFunc;
	    let id = this.display.getDomId("boundsfilter");
	    let _this = this;
	    this.display.jq("boundsfilter").click(function(event){
		if (event.shiftKey) {
		    if(!_this.bounds) return;
		    _this.bounds = null;
		} else {
		    _this.bounds = _this.display.getBounds();
		}
		inputFunc($(this),null,_this.bounds);
	    });
	},
	isRecordOk: function(record) {
	    if(!this.bounds) {
		return true;
	    }
	    if(record.hasLocation()) {
		let b = this.bounds;
		let lat = record.getLatitude();
		let lon = record.getLongitude();
		if(lat>b.top || lat<b.bottom || lon <b.left || lon>b.right)
		    return false;
	    }
            return true;
	},
    });
}


//class: RecordFilter
function RecordFilter(display,filterFieldId, properties) {
    const ID_TEXT = "_text_";
    this.id = filterFieldId;
    this.isText = (this.id == ID_TEXT);
    let fields;
    if(this.isText) {
	let f = display.getProperty("textFilterFields");
	if(f) {
	    fields = display.getFieldsByIds(null,f);
	} else {
	    fields = display.getFieldsByType(null, "string");
	}
    } else {
	let filterField = display.getFieldById(null, filterFieldId);
	if(filterField)
	    fields = [filterField];
	else {
	    console.warn("Could not find filter field:" + filterFieldId);
	    this.disabled = true;
	    fields = [];
	}
    }
    $.extend(this, new BaseFilter(display, properties));
    this.getId = function() {
	return this.id;
    }
    let getAttr = (suffix,dflt)=>{
	let key = this.getId()+"." + suffix;
	let v = display.getProperty(key);
	if(!Utils.isDefined(v)) v = display.getProperty(suffix,dflt);
	return v;
    };
    let label = "";
    if(this.isText)  {
	label = getAttr("filterLabel","Search");
    } else  {
	label = getAttr("filterLabel",fields.length>0?fields[0].getLabel():"");
    }
    $.extend(this, {
	fields: fields,
	values:[],
	hideFilterWidget: display.getProperty("hideFilterWidget",false, true),
	displayType:getAttr("filterDisplay","menu"),
	label:   label,
	depends: getAttr("filterDepends",null),
	dateIds: [],
	prefix:display.getProperty(this.getId() +".filterPrefix",""),
	suffix:display.getProperty(this.getId() +".filterSuffix",""),
	startsWith:display.getProperty(this.getId() +".filterStartsWith",false),
	ops:Utils.split(display.getProperty(this.getId() +".filterOps"),";",true,true),
	labelField:display.getFieldById(null,display.getProperty(this.getId() +".labelField"))
    });



    if(this.ops) {
	let tmp = [];
	this.ops.forEach(tok=>{
	    let tuple  = tok.split(",");
	    tmp.push({
		op: tuple[0],
		value: tuple[1],
		label: tuple[2]||this.id+tuple[0] +tuple[1]
	    });
	});
	this.ops = tmp;
    }

    $.extend(this, {
	toString:function() {
	    return "filter:" + this.getId();
	},
	getField: function() {
	    return this.fields[0];
	},
	getFieldId: function() {
	    if(this.fields.length==0) return '';
	    return this.fields[0].getId();
	},	
	getLabel: function() {
	    return this.label;
	},

	getValue: function(record) {
	    if(this.fields.length==1) {
		return record.getValue(this.fields[0].getIndex());
	    } else {
		let v = this.fields.reduce((acc,field)=>{
		    return acc+=" " + record.getValue(field.getIndex());
		},"");
		return v;
	    }
	},
	isFieldNumeric:function() {
	    if(this.isText) return false;
	    if(this.disabled) return false;
	    return this.getField().isNumeric();
	},
	isFieldBoolean:function() {
	    if(this.isText) return false;
	    if(this.disabled) return false;
	    return this.getField().isFieldBoolean();
	},	
	isFieldEnumeration: function() {
	    if(this.isText) return false;
	    if(this.disabled) return false;
	    return this.getField().isFieldEnumeration();
	},
	isFieldDate: function() {
	    if(this.disabled) return false;
	    return this.getFieldType()=="date";
	},	
	isFieldMultiEnumeration: function() {
	    if(this.disabled) return false;
	    return this.getField().isFieldMultiEnumeration();
	},
	fieldType:null,
	getFieldType: function() {
	    if(this.disabled) return '';
	    if(this.getField() && !this.fieldType) {
		this.fieldType =  this.display.getProperty(this.getField().getId()+".type",this.getField().getType());
	    }
	    return this.fieldType;
	},
	getFilterId: function(id) {
	    return  this.display.getDomId("filterby_" + (id||this.getId()));
	},
	isEnabled: function() {
	    if(this.disabled) return false;
	    return this.isText || this.getField()!=null;
	},
	recordOk: function(display, record, values) {
            return true;
        },
	propertyCache:{},
	getProperty: function(key, dflt,dontCheckCache) {
	    if(!dontCheckCache) {
		let value = this.propertyCache[key];
		if(value) return value.value;
	    }
	    let v = this.display.getProperty(key, dflt);
	    if(!dontCheckCache) {
		this.propertyCache[key] = {value:v};
	    }
	    return v;
	},
	getPropertyFromUrl: function(key, dflt) {
	    return this.display.getPropertyFromUrl(key, dflt,true);
	},	
	prepareToFilter: function() {
//	    console.log(this+" prepareToFilter");
	    this.mySearch = null;
	    if(this.filterIDependOn) {
		this.checkDependency();
	    }

	    if(!this.isEnabled()) {
		return;
	    }
	    let value=null;
	    let _values =[];
	    let values=null;
	    let matchers =[];
	    if(this.ops) {
		let v = $("#" + this.getFilterId(this.getId())).val();
		if(v==FILTER_ALL) {
		    this.mySearch = null;
		    return;
		}
		this.mySearch =  {
		    index: parseFloat(v)
		};
		return;
	    } else  if(this.isFieldNumeric()) {
		let minField = $("#" + this.display.getDomId("filterby_" + this.getId()+"_min"));
		let maxField = $("#" + this.display.getDomId("filterby_" + this.getId()+"_max"));
		if(!minField.val() || !maxField.val()) return;
		let minValue = parseFloat(minField.val().trim());
		let maxValue = parseFloat(maxField.val().trim());
		let dfltMinValue = parseFloat(minField.attr("data-min"));
		let dfltMaxValue = parseFloat(maxField.attr("data-max"));
		if(minValue!= dfltMinValue || maxValue!= dfltMaxValue) {
		    value = [minValue,maxValue];
		}
 	    } else if(this.isFieldDate()){
		let date1 = $("#" + this.display.getDomId("filterby_" + this.getId()+"_date_from")).val();
		let date2 = $("#" + this.display.getDomId("filterby_" + this.getId()+"_date_to")).val();
		if(date1!=null && date1.trim()!="") 
		    date1 =  Utils.parseDate(date1);
		else
		    date1=null;
		if(date2!=null && date2.trim()!="") 
		    date2 =  Utils.parseDate(date2);
		else
		    date2=null;
		if(date1!=null || date2!=null)
		    value = [date1,date2]; 
	    }  else {
		values = this.getFieldValues();
		if(!values) {
		    return;
		}
		if(!Array.isArray(values)) values = [values];
		if(values.length==0) {
		    return;
		}
		values = values.map(v=>{
		    return v.replace(/_comma_/g,",");
		});
		values.forEach(v=>{
		    _values.push((""+v).toLowerCase());
		    try {
			matchers.push(new TextMatcher(v));
		    } catch(skipIt){}
		});
	    }
	    let anyValues = value!=null;
	    if(!anyValues && values) {
		values.forEach(v=>{if(v.length>0 && v!= FILTER_ALL)anyValues = true});
	    }
	    //console.log("\t",this+" any values:" + anyValues);
	    if(anyValues) {
		this.mySearch =  {
		    value:value,
		    values:values,
		    matchers:matchers,
		    _values:_values,
		    anyValues:anyValues,
		};
	    } else {
		this.mySearch = null;
	    }
//	    console.log(this +"prepare:" + JSON.stringify(this.mySearch));
	},
	isRecordOk:function(record,debug) {
	    let ok = true;
	    if(!this.isEnabled() || !this.mySearch) {
		if(debug) {
		    if(!this.isEnabled())
			console.log("\t"+ this+"  not enabled");
		    if(!this.mySearch)
			console.log("\t" + this+"  no mySearch");
		}
		return ok;
	    }
	    let rowValue = this.getValue(record);
	    if(this.ops) {
		let op = this.ops[this.mySearch.index];
		if(op.op=="<") ok =  rowValue<op.value;
		else if(op.op=="<=") ok = rowValue<=op.value;
		else if(op.op==">") ok= rowValue>op.value;
		else if(op.op==">=") ok= rowValue>=op.value;
		else if(op.op=="==") ok= rowValue==op.value;				
	    } else   if(this.isFieldBoolean()) {
		rowValue=String(rowValue);
		ok = this.mySearch.values.includes(rowValue);
	    } else   if(this.isFieldEnumeration()) {
		rowValue=String(rowValue);
		if(this.isFieldMultiEnumeration()) {
 		    ok = false;
		    let values = rowValue.split(",");
		    values.forEach(value=>{
			value=value.trim();
			if(this.mySearch.values.includes(value)) ok = true;
		    });
		} else {
		    ok = this.mySearch.values.includes(rowValue);
		}
	    } else if(this.isFieldNumeric()) {
		if(isNaN(this.mySearch.value[0]) && isNaN(this.mySearch.value[1])) return ok;
		if(isNaN(rowValue) || rowValue=="")  ok =false;
		else if(!isNaN(this.mySearch.value[0]) && rowValue<this.mySearch.value[0]) ok = false;
		else if(!isNaN(this.mySearch.value[1]) && rowValue>this.mySearch.value[1]) ok = false;
	    } else if(this.isFieldDate()){
		if(this.mySearch.value &&  Array.isArray(this.mySearch.value)) {
		    if(rowValue == null) {
			ok = false;
		    }  else  {
			let date1 = this.mySearch.value[0];
			let date2 = this.mySearch.value[1];
			if(!rowValue.getTime) {
			    ok = false;
			}  else {
			    let dttm = rowValue.getTime();
			    if(isNaN(dttm)) ok = false;
			    else if(date1 && dttm<date1.getTime())
				ok = false;
			    else if(date2 && dttm>date2.getTime())
				ok = false;
			}
		    }
		}
	    } else {
		let startsWith = this.startsWith;
		ok = false;
		rowValue  = String(rowValue).toLowerCase();
		for(let j=0;j<this.mySearch._values.length;j++) {
		    let fv = this.mySearch._values[j];
		    if(startsWith) {
			if(rowValue.toString().startsWith(fv)) {
			    ok = true;
			    break;
			}
		    } else  if(rowValue.toString().indexOf(fv)>=0) {
			ok = true;
			break;
		    }
		}


		if(!ok && !startsWith) {
		    for(ri=0;ri<this.mySearch.matchers.length;ri++) {
			let matcher = this.mySearch.matchers[ri];
			if(matcher.matches(rowValue.toString())) {
			    ok = true;
			    break;
			}
		    }
		}
	    }
	    return ok;
	},

	doTags:function() {
	    if(!this.getProperty(this.getId()+".showFilterTags",true)) {
		return false;
	    }
	    let tags =  this.display.getShowFilterTags();
	    return tags;
	},
	doTagsColor:function() {
	    if(!this.getProperty(this.getId()+".colorFilterTags",true)) return false;
	    let tags =  this.getProperty(this.getId()+".colorFilterTags", true) || this.getProperty("colorFilterTags");
	    return tags;
	},

	getFieldValues: function() {
	    if(this.isFieldEnumeration()) {
		if(this.doTags()) {
//		    console.log("\tselected tags: " + this.selectedTags);
		    return this.selectedTags ||[];
		}
	    }
	    let element =$("#" + this.display.getDomId("filterby_" + this.getId()));
	    let value=null;
	    if(element.attr("isCheckbox")) {
		if(element.is(':checked')) {
		    value = element.attr("onValue");
		} else {
		    value = element.attr("offValue");
		}
	    } else if(element.attr("isButton")) {
		value = element.attr("data-value");
	    } else {
		value = element.val();
	    }
	    if(!value) {
		if(this.defaultValue) value = this.defaultValue;
		else value = FILTER_ALL;
	    }
	    if(!Array.isArray(value)) {
		if(!this.isFieldEnumeration()) {
		    value = value.split(",");
		} else {
		    value = [value];
		}
	    }
	    let tmp = [];
	    value.forEach(v=>tmp.push(v.trim()));
	    value = tmp;
	    return value;
	},
	toggleTag:function(value,on,cbx, propagateEvent) {
	    let _this = this;
	    let type  = this.getFilterId();
	    let tagId = Utils.makeId(type +"_" +  value);

	    if(on) {
		if(this.selectedTags.includes(value)) return;
		this.selectedTags = Utils.addUnique(this.selectedTags,value);
		let tagGroup = this.display.jq(ID_TAGBAR).find(".tag-group" +HU.attrSelect("tag-type",this.getFilterId()));
		if(tagGroup.length==0) {
		    let bar;
		    if(this.display.getProperty("tagDiv"))
			bar= $("#"+this.display.getProperty("tagDiv"));
		    else
			bar= this.display.jq(ID_TAGBAR);
		    tagGroup = $(HU.div([STYLE,HU.css("display","inline-block"), CLASS,"tag-group","tag-type",this.getFilterId()])).appendTo(bar);
		}
		
		let tag = $(HU.div(["metadata-type",type,"metadata-value",value,TITLE,value, STYLE, HU.css("background", Utils.getEnumColor(this.getFieldId())),CLASS,"display-search-tag", ID,tagId],value+SPACE +HU.getIconImage("fas fa-times"))).appendTo(tagGroup);
		tag.click(function(){
		    _this.selectedTags = Utils.removeElement(_this.selectedTags,value);
		    if(cbx)
			cbx.prop('checked',false);
		    $(this).remove();
		    _this.inputFunc(_this.fakeInput,null,_this.selectedTags);
		});
	    } else {
		this.selectedTags = Utils.removeElement(this.selectedTags,value);
		$("#" + tagId).remove();
	    }
	    if(propagateEvent && this.inputFunc) {
		this.inputFunc(this.fakeInput,null,this.selectedTags);
	    }
	},
	    
	initWidget: function(inputFunc) {
	    let _this= this;
	    if(!this.isEnabled()) return;
	    this.inputFunc = inputFunc;
	    this.fakeInput  = {
		attr:function(key) {
		    return this[key];
		},
		val: function() {return null},
		id: this.getId(),
		fieldId:this.getFieldId()
	    };

	    if(!this.hideFilterWidget && this.getProperty(this.getId()+".filterLive",this.getProperty('filterLive',false))) {
		let widgetId = this.getFilterId(this.getId());
		let widget = $("#" + widgetId);
		if(widget.length) {
		    widget.keyup(function() {
			inputFunc($(this),null,$(this).val());
		    });
		}
	    }
	    if(this.isFieldEnumeration() && this.getProperty(this.getId() +".filterMultiple",this.getProperty('filterMultiple'))) {
		let widgetId = this.getFilterId(this.getId());
		HU.makeSelectTagPopup(jqid(widgetId),{
		    wrap:"<span class='ramadda-hoverable;' style='display:inline-block;margin-bottom:0px;'>${widget}</span>",
		    makeButton:false,
		    hide:false,after:true,buttonLabel:HU.getIconImage('fa-solid fa-list-check')});
	    }

	    if(!this.hideFilterWidget && this.getProperty(this.getId()+".filterSuggest",false)) {
		let widgetId = this.getFilterId(this.getId());
		let widget = $("#" + widgetId);
		if(widget.length) {
		    widget.keyup(function(e) {
			if(_this.suggestDialog)
			    _this.suggestDialog.remove();
			let input = $(this);
			let v = input.val().toLowerCase();
			let html = '';
			let seen = {};
			_this.records.forEach(r=>{
			    let rv=r.getValueFromField(_this.getId());
			    if(!rv) return;
			    rv =String(rv);
			    if(seen[rv]) return;
			    seen[rv] = true;
			    let _rv = rv.toLowerCase();
			    if(_rv.indexOf(v)>0) {
				html+=HU.div([ATTR_CLASS,'ramadda-clickable',
					      ATTR_STYLE,HU.css('white-space','nowrap','max-width','400px','overflow-x','hidden')],
					      rv);
			    }
			});
			if(html!='') {
			    html = HU.div([ATTR_STYLE,HU.css('max-height','300px','overflow-y','auto','padding','5px')], html);
			    _this.suggestDialog =
				HU.makeDialog({content:html,my:'left top',at:'left bottom',anchor:input});
			    _this.suggestDialog.find('.ramadda-clickable').click(function() {
				_this.suggestDialog.remove();
				_this.suggestDialog=null;
				input.val($(this).html());
				input.focus();
			    });
			}			    

		    });
		}
	    }	    

	    this.initDateWidget(inputFunc);
	    let processDateSelect = (v)=>{
		let now=new Date();
		let from = now;
		let to;
		let widgetId = _this.widgetId;
		if(v=='') {
		    jqid(widgetId+'_date_from').val('');
		    jqid(widgetId+'_date_to').val('');		    
		    inputFunc(jqid(widgetId+'_date_from'),null,{from:'',to:'',select:''});
		    return;
		}
		if(v=='thisyear') {
		    let year  = now.getFullYear();
		    from = new Date(year, 0, 1);
		    to = new Date(year, 11, 31);			
		} else    if(v.startsWith('year_')) {
		    let year = parseInt(v.substring('year_'.length));
		    from = new Date(year, 0, 1);
		    to = new Date(year, 11, 31);			
		} else    if(v=='ytd') {
		    let year  = now.getFullYear();
		    from = new Date(year, 0, 1);
		    to = now;
		} else {
		    let date =Utils.createDateInner(v,now);
		    if(date.getTime()<now.getTime()) {
			from = date; to = now;
		    } else {
			from = now; to = date;
		    }
		}
		from = Utils.formatDateYYYYMMDD(from);
		to = Utils.formatDateYYYYMMDD(to);		    
		jqid(widgetId+'_date_from').val(from);
		jqid(widgetId+'_date_to').val(to);		    
		inputFunc(jqid(widgetId+'_date_from'),null,{from:from,to:to,select:v});
	    };
	    
	    if(this.dateRadiosId) {
		jqid(this.dateRadiosId).find('input:radio').change(function() {
		    processDateSelect($(this).attr('value'));
		});
	    }

	    if(this.dateSelectId) {
		jqid(this.dateSelectId).change(function() {
		    processDateSelect($(this).val());
		});
	    }
	    //	HtmlUtils.initSelect($("#" + this.widgetId));
	    if(this.tagCbxs) {
		let _this = this;
		let cbxChange = function() {
        	    let cbx = $(this);
	            let on = cbx.is(':checked');
		    let value  = $(this).attr("metadata-value");
		    _this.toggleTag(value,on,cbx,true);
		}
		let clickId = this.getFilterId()+"_popup";
		$("#" + clickId).click(()=>{
		    let dialog = this.display.createTagDialog(this.tagCbxs, $("#" + clickId), cbxChange, this.getFilterId(),this.getLabel());
		    dialog.find(".metadata-cbx").each(function() {
			let value = $(this).attr('metadata-value');
			$(this).prop('checked',_this.selectedTags.includes(value));
		    });
		});
	    }

	},
	initDateWidget: function(inputFunc) {
	    if(!this.hideFilterWidget) {
		for(let i=0;i<this.dateIds.length;i++) {
		    let id = this.dateIds[i];
		    HtmlUtils.datePickerInit(id);
		    $("#" + id).change(function(){
			inputFunc($(this));
		    });
		}
	    }
	},
	checkDependency: function() {
	    if(!this.filterIDependOn || !this.records) {
		return;
	    }


	    /*
	    if(!this.dependMySearch || !this.filterIDependOn.mySearch || !this.dependMySearch.values || !this.filterIDependOn.mySearch.values) {
		console.log("checkDependency: not ready:" +  " my search:" + this.dependMySearch);
		return;
	    }

	    let v1 = this.dependMySearch.values;
	    let v2 = this.filterIDependOn.mySearch.values;
	    if(v1.length == v2.length) {
		let equals = true;
		for(let i=0;i<v1.length && equals;i++)
		    equals = v1[i] == v2[i];
		if(equals) return;
	    }
*/
            let enums = this.getEnums(this.records);
	    let widgetId = this.getFilterId(this.getId());
	    let tmp = [];
	    enums.forEach(e=>tmp.push(e.value));
	    this.display.ignoreFilterChange = true;
	    let widget = $("#" + widgetId);
	    let val = widget.val();
	    if(!val) val  = 	widget.attr("data-value");
	    widget.html(HU.makeOptions(tmp,val));
	    this.display.ignoreFilterChange = false;
	},
	handleEventPropertyChanged:function(prop) {
	    if(this.isFieldEnumeration() && this.doTags()) {
		if(this.selectedTags) {
		    let type  = this.getFilterId();
		    this.selectedTags.forEach(value=>{
			let tagId = Utils.makeId(type +"_" +  value);	
			$("#" + tagId).remove();
		    });
		}
		this.selectedTags = [];
		let values = prop.values?prop.values:null;
		if(!values) {
		    if(prop.value) {
			if(Array.isArray(prop.value)) values= prop.value;
			else values[prop.value];
		    } else {
			values=[];
		    }
		}

		prop.value.forEach(value=>{
		    if(value) {
			this.toggleTag(value,true);
		    }
		});
		return;
	    }


	    let id = this.widgetId;
	    if(this.isFieldDate() && prop?.value?.select) {
		jqid(id+'_date_from').val(prop.value.from);
		jqid(id+'_date_to').val(prop.value.to);
		jqid(id+'_date_select').val(prop.value.select);				
		return
	    }
	    if(prop.id && prop.id.endsWith("date_from")) {
		id+="_date_from";
	    } else 	if(prop.id && prop.id.endsWith("date_to")) {
		id+="_date_to";
	    }


	    let widget = $("#"+id);
	    if(widget.attr("isCheckbox")) {
		let on = widget.attr("onValue");
		widget.prop('checked',prop.value.includes(on));
	    } else {
		widget.val(prop.value);
	    }
	    widget.attr("data-value",prop.value);
	    if(widget.attr("isButton")) {
		widget.find(".display-filter-button").removeClass("display-filter-button-selected");
		widget.find("[value='" + prop.value +"']").addClass("display-filter-button-selected");
	    }
	},
	getIncludeAll:function() {
	    return this.getProperty(this.getId() +".includeAll",
			     this.getProperty(this.getId() +".filterIncludeAll",
					      this.getProperty("filterIncludeAll", 
							       this.getProperty("filter.includeAll", true))));
	},
	getWidget: function(fieldMap, bottom,records, vertical) {
	    let labelVertical =   this.getProperty("filterLabelVertical",this.getProperty(this.getId()+".filterLabelVertical",vertical));
	    this.records = records;
	    let debug = false;
	    if(debug) console.log(this.id +".getWidget");
	    if(!this.isEnabled()) {
		if(debug) console.log("\tnot enabled");
		return "";
	    }
	    let widgetStyle = "";
	    if(this.hideFilterWidget)
		widgetStyle = "display:none;";
	    fieldMap[this.getId()] = {
		field: this.fields[0],
		values:[],
	    };
	    let showLabel = true;
            let widget;
	    let widgetId = this.widgetId = this.getFilterId(this.getId());
	    let widgetLabel =   this.getProperty(this.getId()+".filterLabel",this.getLabel());
	    let includeAll = this.getIncludeAll();

            if(this.ops) {
		let labels =[];
		this.ops.forEach((op,idx)=>{
		    labels.push([String(idx),op.label]);
		});

		let selected = this.getPropertyFromUrl(this.getId() +".filterValue",FILTER_ALL);
		let showLabel = this.getProperty(this.getId() +".showFilterLabel",this.getProperty("showFilterLabel",true));
		let allName = this.getProperty(this.getId() +".allName",!showLabel?this.getLabel():"All");
		let enums = Utils.mergeLists([[FILTER_ALL,allName]],labels);
		let attrs= [STYLE,widgetStyle, ID,widgetId,"fieldId",this.getId()];
		widget = HU.select("",attrs,enums,selected);
	    } else   if(this.isFieldBoolean()) {
		let attrs= [STYLE,widgetStyle, ID,widgetId,"fieldId",this.getId()];
		let filterValues = this.getProperty(this.getId()+".filterValues");
                let enums = [];
		let allName = this.getProperty(this.getId() +".allName","-");
		enums.push(['',allName]);
		if(filterValues) {
		    filterValues.split(",").forEach(tok=>{
			let toks = tok.split(":");
			enums.push(toks);
		    });
		} else {
		    enums.push('true','false');
		}

//		let values = filterValues?filterValues.split(","):["-","true","false"];
		widget = HU.select("",attrs,enums,this.dflt);
	    } else   if(this.isFieldEnumeration()) {
		if(debug) console.log("\tis enumeration");
		let dfltValue = this.defaultValue =
		    this.getPropertyFromUrl(this.getId() +".filterValue",FILTER_ALL);
                let enums = this.getEnums(records);
		let attrs= ["style",widgetStyle, "id",widgetId,"fieldId",this.getId()];
		if(this.getProperty(this.getId() +".filterMultiple",this.getProperty('filterMultiple'))) {
		    attrs.push("multiple");
		    attrs.push("");
		    attrs.push("size");
		    attrs.push(this.getProperty(this.getId() +".filterMultipleSize",
						this.getProperty('filterMultipleSize','3')));
		    dfltValue = dfltValue.split(",");
		}

		if(this.displayType!="menu") {
		    if(debug) console.log("\tnot menu");
		    if(!includeAll && dfltValue == FILTER_ALL) dfltValue = enums[0].value;
		    let buttons = "";
		    let colorMap = Utils.parseMap(this.getProperty(this.getId() +".filterColorByMap"));
		    let useImage = this.displayType == "image";
		    let useButton = this.displayType == "button";
		    let imageAttrs = [];
		    let imageMap = Utils.getNameValue(this.getProperty(this.getId() +".filterImages"));
		    if(useImage) {
			let w = this.getProperty(this.getId() +".filterImageWidth");
			let h = this.getProperty(this.getId() +".filterImageHeight");
			if(h) {
			    imageAttrs.push("height");
			    imageAttrs.push(h);
			}
			if(w) {
			    imageAttrs.push("width");
			    imageAttrs.push(w);
			}
			if(!h && !w) {
			    imageAttrs.push("width");
			    imageAttrs.push("50");
			}
			
			imageAttrs.push("style");
			imageAttrs.push(this.getProperty(this.getId() +".filterImageStyle","border-radius:50%;"));
		    }
		    for(let j=0;j<enums.length;j++) {
			let extra = "";
			let v = enums[j].value;
			let color = colorMap?colorMap[v]:null;
			let label;
			if(Array.isArray(v)) {
			    label = v[1];
			    v = v[0];
			} else {
			    label = v;
			}

			let style = this.getProperty(this.getId() +".filterItemStyle","");
			if(color) {
			    style += " background-color:" + color +"; ";
			} else {
			    style += " border:1px solid #ccc; "
			}
			
			let clazz = " ramadda-hoverable ramadda-clickable display-filter-item display-filter-item-" + this.displayType +" ";
			if(useButton) clazz+=" ramadda-button ";
			if(v == dfltValue) {
			    clazz+=  " display-filter-item-" + this.displayType +"-selected ";
			}
			if(v == FILTER_ALL) {
			    extra = " display-filter-item-all ";
			}
			if(useImage) {
			    let image=null;
			    if(imageMap) image = imageMap[v];
			    if(!image || image=="") image = enums[j].image;
			    if(image) {
				buttons+=HtmlUtils.div(["fieldId",this.getId(),"class",clazz,"style",style, "data-value",v,"title",label],
						       HtmlUtils.image(image,imageAttrs));
			    } else {
				buttons+=HtmlUtils.div(["fieldId",this.getId(),"class",clazz,"style",style,"data-value",v,"title",label],label);
			    }
			} else {
			    buttons+=HtmlUtils.div(["fieldId",this.getId(),"class",clazz, "style",style,"data-value",v],label);
			}
			buttons+="\n";
		    }


		    if(useImage && this.getProperty(this.getId() +".filterShowButtonsLabel")) {
			buttons+=HtmlUtils.div(["class","display-filter-item-label","id",this.display.getDomId("filterby_" + this.getId() +"_label")],"&nbsp;");
		    }
		    bottom[0]+= this.prefix + 
			HtmlUtils.div(["data-value",dfltValue,"class","display-filter-items","id",widgetId,"isButton","true", "fieldId",
				       this.getId()], buttons);
		    if(debug) console.log("\treturn 1");
		    return "";
		} else if(this.getProperty(this.getId() +".filterCheckbox")) {
		    if(debug) console.log("\tis checkbox");
		    attrs.push("isCheckbox");
		    attrs.push(true);
		    let tmp = [];
		    enums.map(e=>tmp.push(e.value));
		    let checked = tmp.includes(dfltValue);
		    if(tmp.length>0) {
			attrs.push("onValue");
			attrs.push(tmp[0]);
		    }
		    if(tmp.length>1) {
			attrs.push("offValue");
			attrs.push(tmp[1]);
		    }
		    widget = HtmlUtils.checkbox("",attrs,checked);
		    //			    console.log(widget);
		} else if(this.doTags()) {
		    let doColor = this.doTagsColor();
		    showLabel  =false;
		    let cbxs = [];
		    this.tagToCbx = {};
		    this.selectedTags = [];
		    enums.map((e,idx)=>{
			let count  = e.count;
			let value = e.value;
			let label = value;
			if(Array.isArray(value)) {
			    value = e.value[0];
			    label = e.value[1];
			    if(value === "")
				label = "-blank-";
			}
			let showCount  = true;
			if(count) label = label +(showCount?" (" + count+")":"");
			let cbxId = this.getFilterId() +"_cbx_" + idx;
			this.tagToCbx[value] = cbxId;
			let cbx = HU.checkbox("",[CLASS,"metadata-cbx",ID,cbxId,"metadata-type",this.getFilterId(),"metadata-value",value],false) +" " + HU.tag( "label",  [CLASS,"ramadda-noselect ramadda-clickable","for",cbxId],label);
			cbx = HU.span([CLASS,'display-search-tag','tag',label,STYLE, HU.css("background", Utils.getEnumColor(this.getFieldId()))], cbx);
			cbxs.push(cbx);
		    }); 
		    this.tagCbxs  = cbxs;
		    let clickId = this.getFilterId()+"_popup";
		    let label = " " +this.getLabel()+" ("+ cbxs.length+")";
		    label = label.replace(/ /g,"&nbsp;");
		    let style = HU.css("white-space","nowrap", "line-height","1.5em",  "margin-top","6px","padding-right","5px");
		    if(doColor)
			style+=HU.css("border","1px solid #ccc","background", Utils.getEnumColor(this.getFieldId()));
		    else
			style+=HU.css();
		    widget= HU.div([STYLE, style, TITLE,"Click to select tag", ID,clickId,CLASS,"ramadda-clickable entry-toggleblock-label"], HU.makeToggleImage("fa-solid fa-plus","font-size:8pt;") +label);   
		} else {
		    if(debug) console.log("\tis select");
		    let tmp = [];
		    let showCount = this.getProperty(this.getId()+".filterShowCount",this.getProperty("filterShowCount",true));
		    enums.map(e=>{
			let count  = e.count;
			let v = e.value;
			let label = v;
			if(Array.isArray(v)) {
			    v = e.value[0];
			    label = e.value[1];
			    if(v === "")
				label = "-blank-";
			}
			if(count) label = label +(showCount?" (" + count+")":"");
			tmp.push([v,label]);
		    }); 
                    widget = HtmlUtils.select("",attrs,tmp,dfltValue);
		}
	    } else if(this.isFieldNumeric()) {
		if(debug) console.log("\tis numeric");
		let min=0;
		let max=0;
		let cnt=0;
		records.map(record=>{
		    let value = this.getValue(record);
		    if(isNaN(value))return;
		    if(cnt==0) {min=value;max=value;}
		    else {
			min = Math.min(min, value);
			max = Math.max(max, value);
		    }
		    cnt++;
		});
		let tmpMin = this.getPropertyFromUrl(this.getId() +".filterValueMin",this.getProperty("filterValueMin"));
		let tmpMax = this.getPropertyFromUrl(this.getId() +".filterValueMax",this.getProperty("filterValueMax"));		
		let minStyle = "";
		let maxStyle = "";
		let dfltValueMin = min;
		let dfltValueMax = max;
		if(Utils.isDefined(tmpMin)) {
		    minStyle = "background:" + TEXT_HIGHLIGHT_COLOR+";";
		    dfltValueMin = parseFloat(tmpMin);
		}
		if(Utils.isDefined(tmpMax)) {
		    maxStyle = "background:" + TEXT_HIGHLIGHT_COLOR+";";
		    dfltValueMax = parseFloat(tmpMax);
		}

		let size = this.getProperty(this.getId()+'.filterWidgetSize',
					    this.getProperty('filterWidgetSize', '60px'));
		minStyle+=HU.css('width',size);
		maxStyle+=HU.css('width',size);
                widget = HtmlUtils.input('',dfltValueMin,[STYLE,minStyle,'data-type',this.getFieldType(),'data-min',min,'class','display-filter-range display-filter-input', 'id',widgetId+'_min','xsize',3,'fieldId',this.getId()]);
		widget += '-';
                widget += HtmlUtils.input('',dfltValueMax,[STYLE,maxStyle,'data-type',this.getFieldType(),'data-max',max,'class','display-filter-range display-filter-input', 'id',widgetId+'_max','xsize',3,'fieldId',this.getId()]);
	    } else if(this.getFieldType() == 'date') {
                widget =HtmlUtils.datePicker('','',['class','display-filter-input','style',widgetStyle, 'id',widgetId+'_date_from','fieldId',this.getId()]) +'-' +
		    HtmlUtils.datePicker('','',['class','display-filter-input','style',widgetStyle, 'id',widgetId+'_date_to','fieldId',this.getId()]);
		this.dateIds.push(widgetId+'_date_from');
		this.dateIds.push(widgetId+'_date_to');

		let selects = this.getProperty(this.getId()+'.filterDateSelects');

		if(selects) {
		    if(!this.getProperty(this.getId()+'.filterDateShowRange',true)) {
			widget  = HU.span([ATTR_STYLE,'display:none;'], widget);
		    }
		    this.dateSelectId = widgetId+'_date_select';
		    selects = selects.split(",").map(o=>{
			let toks = Utils.split(o,':',true,true);
			if(toks.length==2) return {value:toks[0],label:toks[1]};
			return {value:o,label:o};
		    });
		    let select;
		    if(this.getProperty(this.getId()+'.filterDateShowRadio',false)) {
			this.dateRadiosId = widgetId+'_date_radios';
			let name = HU.getUniqueId('radios_');
			selects = Utils.mergeLists([{value:'',label:'All dates'}],selects);
			select = HU.div([ATTR_ID,this.dateRadiosId,
					 ATTR_STYLE,HU.css('text-align','left')], HU.radioGroup(name, selects));
		    } else {
			selects = Utils.mergeLists([{value:'',label:'Select date'}],selects);
			select= HU.select('',[ATTR_ID,widgetId+'_date_select','ignore',true],selects);
		    }
		    if(labelVertical)
			widget += select;
		    else
			widget=HU.span([],widget)+HU.span([ATTR_STYLE,'margin-left:5px'],select);
		}

            } else {
		let dfltValue = this.getPropertyFromUrl(this.getId() +".filterValue","");
		let width = this.getProperty(this.getId() +".filterWidth","150px");		
		let attrs =[STYLE,widgetStyle+"width:" + HU.getDimension(width), "id",widgetId,"fieldId",this.getId(),"class","display-filter-input"];
		let placeholder = this.getProperty(this.getId() +".filterPlaceholder");
		attrs.push("width");
		attrs.push(width);
		if(placeholder) {
		    attrs.push("placeholder");
		    attrs.push(placeholder);
		} else {
		    showLabel = false;
		    attrs.push("placeholder");
		    attrs.push(widgetLabel);
		}

		attrs.push("istext",this.isText);
                widget =HU.input("",dfltValue,attrs);
		let values=fieldMap[this.getId()].values;
		let seen = {};
		records.map(record=>{
		    let value = this.getValue(record);
		    if(!seen[value]) {
			seen[value] = true;
			values.push(value);
		    }	
		});
            }
	    if(!this.hideFilterWidget) {
		let tt = widgetLabel;
		if(Utils.stringDefined(this.getField().getDescription())) {
		    tt = tt+HU.getTitleBr() +
			this.getField().getDescription();
		}
		if(widgetLabel.length>50) widgetLabel = widgetLabel.substring(0,49)+"...";
		if(!this.getProperty(this.getId() +".showFilterLabel",this.getProperty("showFilterLabel",true))) {
		    widgetLabel = "";
		}
		else {
		    if(!Utils.stringDefined(widgetLabel)) widgetLabel = "";
		    else widgetLabel = widgetLabel+": ";
		}
		widgetLabel = this.display.makeFilterLabel(widgetLabel,tt,labelVertical);
		if(labelVertical) widgetLabel = widgetLabel+"<br>";
		if(vertical) {
		    widget = HtmlUtils.div([],(showLabel?widgetLabel:"") + widget+this.suffix);
		} else {
		    widget = HtmlUtils.div(["style","display:inline-block;"],(showLabel?widgetLabel:"") + widget+this.suffix);
		}
	    }
	    if(!vertical)
		widget= widget +(this.hideFilterWidget?"":"&nbsp;&nbsp;");
	    if(this.prefix) widget = this.prefix+widget;

	    let show = this.getProperty(this.getId() +".filterShow",this.getProperty("filterShow",true));
	    if(!show) widget=HU.div([ATTR_STYLE,'display:none;'], widget);

	    return widget;
	},
	getWidgetId:function() {
	    return this.widgetId;
	},
	getEnums: function(records) {
	    let counts = {};
	    let isMulti  = this.isFieldMultiEnumeration();
	    records.forEach((record,idx)=>{
		let value = this.getValue(record);
		if(value ===null) return;
		value = String(value);
		let values = isMulti?value.split(","):[value];
		values.forEach(v=>{
		    v =v.trim ();
		    if(!counts[v]) counts[v]=1;
		    else   counts[v]++;
		});
	    });

	    let enums = null;
	    let filterValues = this.getProperty(this.getId()+".filterValues");
	    let showLabel = this.getProperty(this.getId() +".showFilterLabel",this.getProperty("showFilterLabel",true));

	    if (filterValues) {
		let toks;
		if ((typeof filterValues) == "string") {
		    filterValues = Utils.getMacro(filterValues);
		    toks = filterValues.split(",");
		} else {
		    toks = filterValues;
		}
		enums=[];
		toks.map(tok=>{
		    let tmp = tok.split(":");
		    if(tmp.length>1) {
			tok = [tmp[0],tmp[1]];
		    } else if(tok == FILTER_ALL) {
			let allName = this.getProperty(this.getId() +".allName",!showLabel?this.getLabel():"All");
			tok = [tmp[0],allName];
		    } else {
			tok = [tok,tok];
		    }
		    let count = counts[tok[0]];
		    enums.push({value:tok,count:count});
		})
	    }
	    if(enums == null) {
		let depends = this.getProperty(this.getId() +".depends");
		this.filterIDependOn = !depends?null:this.display.getRecordFilter(depends);
		let allName = this.getProperty(this.getId() +".allName",!showLabel?this.getLabel():"All");
		allName+=' (' + records.length+')'
		enums = [];
		let includeAll = this.getIncludeAll();
//		if(includeAll && !this.getProperty(this.getId() +".filterLabel",null,true)) {
		if(includeAll) {
		    enums.push({value:[FILTER_ALL,allName]});
		}
		let seen = {};
		let dflt = this.getField().getEnumeratedValues();
		if(dflt) {
		    for(let v in dflt) {
			seen[v] = true;
			let count = counts[v];
			enums.push({value:[v,dflt[v]],count:count});
		    }
		}
		let enumValues = [];
		let imageField=this.display.getFieldByType(null, "image");
		let valuesAreNumbers = true;

		if(this.filterIDependOn) {
		    this.filterIDependOn.prepareToFilter();
//		    console.log('getEnums: dependencys search info:',this.filterIDependOn.mySearch)
		}

		records.forEach((record,idx)=>{
		    if(this.filterIDependOn) {
			if(!this.filterIDependOn.isRecordOk(record)) return;
		    }
		    let value =this.getValue(record);
		    let values;
		    if(isMulti) {
			values = value.split(",").map(v=>{return v.trim();});
		    } else {
			values = [value];
		    }

		    values.forEach(value=>{
			if(seen[value]) return;
			seen[value]  = true;
			let obj = {};
			if(imageField)
			    obj.image = this.display.getDataValues(record)[imageField.getIndex()];
			if((+value+"") != value) valuesAreNumbers = false;
			let label = value;
			if(label.length>30) {
			    label=  label.substring(0,29)+"...";
			}
			if(this.labelField) {
			    label += " - " + this.labelField.getValue(record);
			    console.log("l:" + label);
			}


			if(typeof value == "string")
			    value = value.replace(/\'/g,"&apos;");
			let tuple = [value, label];
			obj.value = tuple;
			obj.count =  counts[value];

			enumValues.push(obj);
		    });
		});


		if(this.getProperty(this.getId() +".filterSortCount") ||
		   this.getProperty(this.getId() +".filterSort",this.getProperty('filterSort',true))) {
		    let sort = this.getProperty(this.getId() +".filterSort",this.getProperty('filterSort',false));
		    let sortCount = this.getProperty(this.getId() +".filterSortCount",!sort);
		    enumValues.forEach(e=>{
			if(!Utils.isDefined(e.count)) e.count=0;
		    });

		    enumValues.sort((a,b)  =>{
			if(sortCount) {
			    if(b.count!=a.count) {
				return b.count-a.count;
			    }				
			}
			a= a.value;
			b = b.value;
			if(valuesAreNumbers) {
			    return +a - +b;
			}
			return (""+a[1]).localeCompare(""+b[1]);
		    });
		}
		for(let j=0;j<enumValues.length;j++) {
		    let v = enumValues[j];
		    enums.push(v);
		}
	    }
	    return enums;
	}
	
    });
}

RecordFilter.prototype = {
    toString:function() {
	return 'RecordFilter:' + this.id;
    }
}


//class: MonthFilter
function MonthFilter(param) {
    RamaddaUtil.inherit(this, new BaseFilter());
    RamaddaUtil.defineMembers(this, {
        months: param.split(","),

        recordOk: function(display, record, values) {
            for (i in this.months) {
                let month = this.months[i];
                let date = record.getDate();
                if (date == null) return false;
                if (date.getMonth == null) {
                    //console.log("bad date:" + date);
                    return false;
                }
                if (date.getMonth() == month) return true;
            }
            return false;
        }
    });
}


function TextMatcher (pattern,myId) {
    this.myId = myId;
    this.regexps=[];
    if(pattern) {
        pattern = pattern.trim();
    }
    if(pattern&& pattern.length>0) {
        pattern = pattern.replace(/\./g,"\\.");
        if(pattern.startsWith('"') && pattern.endsWith('"')) {
            pattern  = pattern.replace(/^"/,"");
            pattern  = pattern.replace(/"$/,"");
            this.regexps.push(new RegExp("(" + pattern + ")","ig"));
        } else {
            pattern.split(" ").map(p=>{
                p = p.trim();
                this.regexps.push(new RegExp("(" + p + ")","ig"));
            });
        }
    }   
    $.extend(this, {
        pattern: pattern,
        hasPattern: function() {
            return this.regexps.length>0;
        },
        highlight: function(text,id) {
	    if(id && this.myId && id!=this.myId) return text;
            for(var i=0;i<this.regexps.length;i++) {
                text  =  text.replace(this.regexps[i], "<span style=background:yellow;>$1</span>");
            }
            return text;
        },
        matches: function(text) {
            if(this.regexps.length==0) return true;
            text  = text.toLowerCase();
            for(var i=0;i<this.regexps.length;i++) {
                if(!text.match(this.regexps[i])) return false;
            }
            return true;
        }
    });

}
