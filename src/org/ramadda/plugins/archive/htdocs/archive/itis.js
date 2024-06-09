
var Itis = {
    spacer:'&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;',
    init:function() {
	let name = 'edit_type_archive_bio_common_name';
	this.input= $('[name="'+ name+'"]');
	$(document).ready(()=>{
	    let uid = HU.getUniqueId('');
	    this.input.after(HU.span([ATTR_ID, uid,
				      ATTR_STYLE,
				      'margin-left:5px;height:20px;width:25px;padding:5px;min-width:25px;'],this.spacer));
	    this.progress = jqid(uid);
	});
	let _this=this;
	this.input.on('keypress', (event) =>{
            if (event.which != 13) return;
            event.preventDefault(); 
	    let v = this.input.val();
	    if(!Utils.stringDefined(v)) return;
            let url = 'https://www.itis.gov/ITISWebService/jsonservice/ITISService/searchByCommonName?srchKey=' + encodeURIComponent(v);
	    let success=(data)=>{
		_this.showNamesPopup(data);
	    };
	    this.fetch(url,success);
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
        let dialog= HU.makeDialog({content:html,my:"right top",at:"right bottom",	
			   anchor:this.input,draggable:true,header:true,inPlace:false});
	dialog.find('.ramadda-clickable').click(function(){
	    dialog.remove();
	    let item = _this.items[$(this).attr('index')];
	    if(!item) return;
	    _this.input.val(item.commonName);
	    _this.loadTsn(item);
	});
    },
    loadTsn:function(item) {
	let url = 'https://www.itis.gov/ITISWebService/jsonservice/ITISService/getFullHierarchyFromTSN?tsn='+ item.tsn;
	let success=(data)=>{
	    if(!data.hierarchyList) {
		alert('No results');
		return;
	    }
	    let map = {};
//	    console.dir(item);	    console.dir(data);
	    console.log(this.getInput('tsn_number',true).length);
	    this.getInput('tsn_number',true).val(data.tsn);
	    data.hierarchyList.forEach(h=>{
		let rank = h.rankName.toLowerCase();
//		console.log(rank+'='+h.taxonName +' '+this.getInput(rank).length);
		this.getInput('taxon_'+rank).val(h.taxonName??'');
	    });
	};
	this.fetch(url,success);
    },
    getInput:function(taxa,plain) {
	let name = 'edit_type_archive_bio_'+ taxa+(plain?'':'_plus');
	return $('[name="'+ name+'"]');
    }
}


