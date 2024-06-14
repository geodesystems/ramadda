
//docs:https://www.itis.gov/ws_tsnApiDescription.html
var Itis = {
    spacer:'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;',
    getUrl:function(service) {
        return  'https://www.itis.gov/ITISWebService/jsonservice/ITISService/' + service;
    },

    init:function() {
	let _this=this;
	let doSearch=()=>{
	    let v = this.input.val();
	    if(!Utils.stringDefined(v)) return;
            let url = this.getUrl('searchByCommonName?srchKey=' + encodeURIComponent(v));
	    let success=(data)=>{
		_this.showNamesPopup(data);
	    };
	    this.fetch(url,success);
	}

	let name = 'edit_type_archive_bio_common_name';
	this.commonNames= $('[name="'+ name+'"]');
//	this.input= $('[name="'+ name+'"]');
	this.input= jqid('entrynameinput');
	
	$(document).ready(()=>{
	    let uid = HU.getUniqueId('');
	    let puid = HU.getUniqueId('');
	    let extra='';
	    extra+=HU.span([ATTR_ID,uid],"Search Taxonomy");
	    extra += HU.span([ATTR_ID, puid, ATTR_STYLE,
				 'margin-left:5px;height:20px;width:25px;padding:5px;min-width:25px;'],this.spacer);
	    this.input.after(HU.div([],extra));
	    this.searchButton =    jqid(uid);
	    this.searchButton.button().click(function() {
		doSearch();
	    });
	    this.progress = jqid(puid);
	});
	this.input.on('keypress', (event) =>{
            if (event.which != 13) return;
            event.preventDefault(); 
	    doSearch();
	    });
    },
    fetch:function(url,callback) {
	let _url = '/repository/proxy?url='+encodeURIComponent(url);
	this.progress.html(HU.image(ramaddaBaseUrl+'/icons/progress.gif'));
	let ok = data=>{
	    this.progress.html(this.spacer);
	    callback(data);
	}
	$.getJSON(_url, ok).fail(data=>{
	    this.progress.html(this.spacer);
	    alert('Call to itis.gov failed:' + data);
	    console.dir(data);
	});
    },
    showNamesPopup:function(data) {
	let _this=this;
	if(!data.commonNames || data.commonNames.length==0 || (
	    data.commonNames.length==1 && !data.commonNames[0])) {
	    alert('No information found');
	    return
	}
	let html=HU.div([ATTR_STYLE,HU.css('text-weight','bold')],'Select an item to set the taxonomy:');
	this.items = {};
	data.commonNames.forEach((item,idx)=>{
	    this.items[idx] = item;
	    html+=HU.div(['index',idx,ATTR_CLASS,'ramadda-clickable'],item.commonName);
	});
        html=HU.div([ATTR_STYLE,HU.css('margin','5px','max-height','400px','overflow-y','auto')],html);
        let dialog= HU.makeDialog({content:html,my:"left top",at:"left bottom",	
			   anchor:this.searchButton,draggable:true,header:true,inPlace:false});
	dialog.find('.ramadda-clickable').click(function(){
	    dialog.remove();
	    let item = _this.items[$(this).attr('index')];
	    if(!item) return;
	    _this.input.val(item.commonName);
	    _this.loadTsn(item);
	});
    },
    color:function(input) {
	input.css('background','yellow');
	input.animate({ backgroundColor: "#fff" }, 2000);
    },
    loadTsn:function(item) {
	let url1 = this.getUrl('getFullHierarchyFromTSN?tsn='+ item.tsn);
	let url2 = this.getUrl('getScientificNameFromTSN?tsn=' + item.tsn);
	let url3 = this.getUrl('getCommonNamesFromTSN?tsn=' + item.tsn);	
	let success1=(data)=>{
	    if(!data.hierarchyList) {
		alert('No results');
		return;
	    }
	    let map = {};
	    let inputs = [this.getInput('tsn_number')];
	    this.getInput('tsn_number',true).val(data.tsn);
	    data.hierarchyList.forEach(h=>{
		let rank = h.rankName.toLowerCase();
		console.log("RANK:"+ rank);
		let input = this.getInput('taxon_'+rank);
		input.val(h.taxonName??'');
		inputs.push(input);
	    });


	    inputs.forEach(input=>{
		this.color(input);
	    });
	};
	let success2=(data)=>{
	    if(data.combinedName) {
		this.getInput('scientific_name',true).val(data.combinedName);
	    }
	};
	let success3=(data)=>{
	    if(!data.commonNames) return
	    let name = this.input.val().trim();
	    let names = [];
	    let seen={};
	    seen[name]=true;
	    Utils.split(this.commonNames.val(),"\n",true,true).forEach(n=>{
		names.push(n);
		seen[n]=true;
	    });
	    data.commonNames.forEach(n=>{
		if(seen[n.commonName]) return;
		seen[n.commonName]=true;
		names.push(n.commonName);
	    });
	    names = Utils.join(names,'\n');
	    this.commonNames.val(names);
	    this.color(this.commonNames);
	};	
	this.fetch(url1,success1);
	this.fetch(url2,success2);
	this.fetch(url3,success3);    	
    },
    getInput:function(taxa,plain) {
	let name = 'edit_type_archive_bio_'+ taxa+(plain?'':'_plus');
	return $('[name="'+ name+'"]');
    }
}


