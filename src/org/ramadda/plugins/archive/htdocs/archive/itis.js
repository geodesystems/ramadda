
//docs:https://www.itis.gov/ws_tsnApiDescription.html
var Itis = {
    spacer:'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;',
    getUrl:function(service) {
        return  'https://www.itis.gov/ITISWebService/jsonservice/ITISService/' + service;
    },

    doSearch:function(fromNameInput,v) {
	let _this=this;
	if(!v) v = this.nameInput.val();
	if(!Utils.stringDefined(v)) return;
        let url = this.getUrl('searchByCommonName?srchKey=' + encodeURIComponent(v));
	let success=(data)=>{
	    _this.showNamesPopup(fromNameInput,data,v);
	};
	this.fetch(url,success);
    },
    init:function() {
	let _this=this;
	let doSearch=(fromNameInput,v)=>{
	    this.doSearch(fromNameInput,v);
	}
	$(document).ready(()=>{
	    let name = 'edit_type_archive_taxonomy_common_name';
	    this.commonNames= HU.jqname(name);
	    this.birdCodeFour=HU.jqname('edit_type_archive_ornithology_specimen_alpha_code_four');
	    this.birdCodeSix=HU.jqname('edit_type_archive_ornithology_specimen_alpha_code_six');	    
	    if(this.birdCodeFour.length>0) {
		let url = RamaddaUtil.getUrl('/archive/birdcodes.json');
		$.getJSON(url, json=>{_this.birdCodes=json;}).fail(data=>{console.error('could not load birdcodes.json')});
	    }
	    if(this.commonNames.length==0) {
		console.error('Could not find common names widget:'+'[name="'+ name+'"]');
	    }
	    this.nameInput= jqid('entrynameinput');
	    this.nameInput.on('keypress', (event) =>{
		if (event.which != 13) return;
		event.preventDefault(); 
		doSearch(true);
	    });

	    

	    let _this = this;
	    let uid = HU.getUniqueId('');
	    let puid = HU.getUniqueId('');
	    let extra='';
	    extra+=HU.span([ATTR_ID,uid],"Search Taxonomy");
	    extra += " From <a target=_other href=https://www.itis.gov/>Integrated Taxonomic Information System</a>";
	    extra += HU.span([ATTR_ID, puid, ATTR_STYLE,
				 'margin-left:5px;height:20px;width:25px;padding:5px;min-width:25px;'],this.spacer);
	    this.nameInput.after(HU.div([ATTR_STYLE,HU.css('margin-top','4px')],extra));
	    this.searchButton =    jqid(uid);
	    this.searchButton.button().click(() =>{
		let v = prompt("Search term:",this.nameInput.val());
		if(v) {
		    doSearch(false,v);
		}
	    });
	    this.progress = jqid(puid);
	});
    },
    fetch:function(url,callback) {
	let _url = RamaddaUtil.getUrl('/proxy?url='+encodeURIComponent(url));
	this.progress.html(HU.image(ramaddaBaseUrl+'/icons/progress.gif'));
	let ok = data=>{
	    this.progress.html(this.spacer);
	    callback(data);
	}
	$.getJSON(_url, ok).fail(data=>{
	    this.progress.html(this.spacer);
	    let msg = '';
	    if(data.responseJSON) {
		msg = data.responseJSON;
	    } else if(data.responseText) {
		msg = data.responseText;
	    } else {
		msg = data;
	    }
	    alert('Call to itis.gov failed:' + msg);
	});
    },
    showNamesPopup:function(fromNameInput,data,v) {
	let _this=this;
	if(!data.commonNames || data.commonNames.length==0 || (
	    data.commonNames.length==1 && !data.commonNames[0])) {
	    v = prompt("No information found. Search term:",v);
	    if(v) {
		this.doSearch(fromNameInput,v);
	    }
	    return
	}
	let html=HU.div([ATTR_STYLE,HU.css('text-weight','bold')],'Select an item to set the taxonomy:');
	this.items = {};
	data.commonNames.forEach((item,idx)=>{
	    this.items[idx] = item;
	    let name = item.commonName;
	    let link = HU.href('https://www.itis.gov/servlet/SingleRpt/SingleRpt?search_topic=TSN&search_value=' + item.tsn,
			       HU.image(RamaddaUtils.getUrl('/archive/itis.png'),[ATTR_WIDTH,'24px']),
			       [ATTR_TITLE,
				'View record for TSN ' + item.tsn +' at itis.gov','target','itis']);

	    html+=HU.div([ATTR_CLASS,'ramadda-hoverable'],
			 HU.leftRightTable(HU.div(['index',idx,ATTR_CLASS,'ramadda-clickable'],name),link,null,'30px'));
	});
        html=HU.div([ATTR_STYLE,HU.css('margin','5px','max-height','400px','overflow-y','auto')],html);
        let dialog= HU.makeDialog({content:html,my:"left top",at:"left bottom",	
			   anchor:this.searchButton,draggable:true,header:true,inPlace:false});
	dialog.find('.ramadda-clickable').click(function(){
	    dialog.remove();
	    let item = _this.items[$(this).attr('index')];
	    if(!item) return;
	    if(fromNameInput) {
		_this.nameInput.val(item.commonName);
	    }
	    _this.loadTsn(item);
	});
    },
    color:function(input) {
	input.css('background','yellow');
	input.animate({ backgroundColor: "#fff" }, 2000);
    },
    loadTsn:function(item) {
	let handleFullHierarchy=(data)=>{
//	    console.dir('hierarchy',data);
	    if(!data.hierarchyList) {
		alert('No results');
		return;
	    }
	    let map = {};
	    let inputs = [this.getInput('tsn_number')];
	    this.getInput('tsn_number',true).val(data.tsn);
	    data.hierarchyList.forEach(hierarchyElement=>{
		if(!hierarchyElement) return;

		let rank = hierarchyElement.rankName.toLowerCase();
		let input = this.getInput('taxon_'+rank);
		if(input.length==0) {
//		    console.error('could not find input for:' + 'taxon_' + rank);
		    return;
		}
		input.val(hierarchyElement.taxonName??'');
		inputs.push(input);
	    });


	    inputs.forEach(input=>{
		this.color(input);
	    });
	};
	let handleScientificName=(data)=>{
//	    console.dir('sciname',data);
	    if(Utils.stringDefined(data.combinedName)) {
		this.getInput('scientific_name',true).val(data.combinedName);
		this.applyBirdCodes(data.combinedName,null);
	    }
	};
	let handleCommonNames=(data)=>{
//	    console.dir('commonNames',data);
	    if(!data.commonNames) return
	    let name = this.nameInput.val().trim();
	    let names = [];
	    let seen={};
	    let firstName =null;
	    seen[name]=true;
	    let value = this.commonNames.val();
	    if(value===null) {
		alert('No common names value found');
		if(this.commonNames.length==0) {
		    console.error('Could not find the common names widget');
		}
		return
	    }
	    Utils.split(value,"\n",true,true).forEach(n=>{
		if(seen[n]) return;
		names.push(n);
		seen[n]=true;
	    });
	    let commonNames = []
	    data.commonNames.forEach(n=>{
		if(firstName==null) firstName = n.commonName;
		if(seen[n.commonName]) return;
		seen[n.commonName]=true;
		names.push(n.commonName);
		commonNames.push(n.commonName);
	    });
	    if(!Utils.stringDefined(name) && firstName) {
		this.nameInput.val(firstName);
	    }
	    this.applyBirdCodes(null,commonNames);
	    names = Utils.join(names,'\n');
	    this.commonNames.val(names);
	    this.color(this.commonNames);
	};	
	
	this.fetch(this.getUrl('getFullHierarchyFromTSN?tsn='+ item.tsn),handleFullHierarchy);
	this.fetch(this.getUrl('getScientificNameFromTSN?tsn=' + item.tsn),handleScientificName);
	this.fetch(this.getUrl('getCommonNamesFromTSN?tsn=' + item.tsn),handleCommonNames);    	
    },
    applyBirdCodes(sciname,commonNames) {
	if(!this.birdCodes) return;
	let match = (code,name)=>{
	    if(code.sciname && code.sciname==name) return true;
	    return false;
	}
//	if(sciname) console.dir('sciname:',sciname);
	let theCode = null;
	if(sciname) {
	    this.birdCodes.every(code=>{
		if(match(code,sciname)) {
		    theCode = code;
		    return false;
		}
		return true;
	    })
	}
	if(!theCode && commonNames && commonNames.length>0) {
//	    console.dir('common:',commonNames);	
	    commonNames.every(name=>{
		this.birdCodes.every(code=>{
		    if(match(code,name)) {
			theCode = code;
			return false;
		    }
		    return true;
		});
		if(theCode) return false;
		return true;
	    });
	}
	if(theCode) {
	    Utils.setMenuValue(this.birdCodeFour,theCode.code4);
	    Utils.setMenuValue(this.birdCodeSix,theCode.code6);	    
	}

    },
    getInput:function(taxa,plain) {
	let name = 'edit_type_archive_taxonomy_'+ taxa+(plain?'':'_plus');
	let widget= $('[name="'+ name+'"]');
	return widget;
    }
}


