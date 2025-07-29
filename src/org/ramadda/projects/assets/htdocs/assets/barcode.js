
var ID_ASSETS_NAME = 'name';
var ID_ASSETS_CODE = 'barcode';
var ID_ASSETS_ADDGEOLOCATION = 'addgeolocation';
var ID_ASSETS_TYPE='assettype';
var ASSET_TYPES=[['type_assets_building','Building'],
		 ['type_assets_vehicle','Vehicle'],
		 ['type_assets_equipment','Equipment'],
		 ['type_assets_it','IT Asset']];

function BarcodeReader (videoId,callback,args) {
    let opts = {
    }
    /*
    window.onerror = function (message, source, lineno, colno, error) {
	const errBox = document.createElement('pre');
	errBox.style.color = 'red';
	errBox.textContent = `JS Error: ${message} at ${source}:${lineno}:${colno}`;
	document.body.appendChild(errBox);
    };*/
    let videoElement = this.videoElement = document.getElementById(videoId);
    // Force attributes for iOS Safari
    videoElement.setAttribute('autoplay', '');
    videoElement.setAttribute('muted', '');
    videoElement.setAttribute('playsinline', '');

    const codeReader = this.codeReader = new ZXing.BrowserMultiFormatReader();
    codeReader.decodeFromVideoDevice(null, videoElement, (result, error, controls) => {
	if (result) {
	    if(!callback(result.getText(),controls)) {
//		controls.stop();
	    }
	} else if(error) {
//	    console.log(error);
	}
    });

    this.cancel = ()=>{
	this.codeReader.reset();
    }
}


function AssetHandler (id,args) {
    let _this = this;
    this.args = args;
    this.opts = {
	defaultType:null,
	defaultTypeLabel:null,
	editMode:false,
	scanMode:false
    }
    this.opts = $.extend(this.opts,args??{});
    if(this.opts.editMode) {
	this.initEditMode();
	return;
    }

    this.id = id;
    this.buttonId= this.id+'_open';
    this.contentsId= this.id+'_contents';    
    this.headerId= this.id+'_header';    
    if(this.opts.scanMode) {
	this.initScanMode();
	return;
    }


    jqid(this.id).append(HU.div([ATTR_ID,this.buttonId],"Scan bar code"));
    this.videoOpen=false;
    jqid(this.buttonId).button().click(()=>{
	if(this.videoOpen) {
	    jqid(this.buttonId).html("Scan bar code");
	    jqid(this.contentsId).hide(500);
	    if(this.dialog) this.dialog.remove();
	    this.dialog=null;
	} else {
	    jqid(this.buttonId).html("Close");
	    if(this.barcodeReader) {
		jqid(this.contentsId).show(500);
		this.showVideo();
	    } else {
		this.makeVideo();
	    }
	}
	this.videoOpen=!this.videoOpen;
    })


}

AssetHandler.prototype = {
    initEditMode: function() {
	let name = 'edit_type_assets_base_asset_id';
        this.assetIdInput= HU.jqname(name);
	let buttonId= HU.getUniqueId('editbutton');
	this.id = HU.getUniqueId('editmode');
	this.videoId= this.id+'_video';
	this.assetIdInput.after(HU.space(2)+HU.span([ATTR_ID,buttonId],'Scan Bar Code'));
	jqid(buttonId).button().click(()=>{
	    let html='';
	    html=HU.div([ATTR_STYLE,HU.css('text-align','center','margin','5px'),ATTR_ID,this.id+'_cancel'],'Cancel');
	    html+=HU.center("<video class=assets_editmode_barcode_video id='" + this.videoId+ "'  autoplay muted playsinline></video>\n");
	    let dialog = this.dialog = HU.makeDialog({content:html,
						      anchor:jqid(buttonId),at:'left top',
						      draggable:true,
						      showCloseIcon:true,
						      title:'Scan bar code',header:true});	

	    let cancel = (controls)=> {
		if(controls)
		    controls.stop();
		this.barcodeReader.cancel();		
		this.barcodeReader = null;
		if(this.dialog) {
		    this.dialog.remove();
		    this.dialog=null;
		}
	    }
	    jqid(this.id+'_cancel').button().click(()=>{
		cancel();
	    });
	    let callback = (result,controls) =>{
		cancel();
		this.assetIdInput.val(result);
	    };
	    this.barcodeReader = new BarcodeReader(this.videoId,callback);
	});
    },
    initScanMode:function() {
	let header =  'Scan a bar code to search for asset or ' +
	    HU.span([ATTR_ID,'search',
		     ATTR_CLASS,'assets-create-link'],'search');
	this.makeVideo({header:header});
	jqid('search').click(()=>{
	    return this.handleBarcode('');
	});

    },
    handleScanMode:function(code) {
	var ID_SCAN_RESULTS ='scanresults';
	let rows =[];
	rows.push(HU.b('Barcode: ') +
		  HU.input('',this.opts.code,[ATTR_STYLE,HU.css('width','300px'),
					      ATTR_ID,ID_ASSETS_CODE,
					      ATTR_PLACEHOLDER,'Enter bar code']));
	let buttons  =HU.div([ATTR_CLASS,'ramadda-button-ok display-button'], 'Find Asset') + SPACE2 +
	    HU.div([ATTR_CLASS,'ramadda-button-cancel display-button'], 'Cancel');
	rows.push(HU.center(buttons));
	rows.push(HU.div([ATTR_ID,ID_SCAN_RESULTS]));
	let html = Utils.wrap(rows,'<div style="margin-bottom:8px;">','</div>');
        html=HU.div([ATTR_STYLE,HU.css('margin','8px')],html);
	let anchor = jqid(this.videoId);
	let title = 'Find Asset';
	let dialog = this.dialog = HU.makeDialog({content:html,
						  anchor:anchor,
						  at:'center top',
						  my:'center top',						  
						  showCloseIcon:false,
						  title:title,header:true});	

	const input = document.getElementById(ID_ASSETS_CODE);
	setTimeout(() => {
	    input.focus();
	    input.scrollIntoView({ block: "center" });
	    }, 300);

//	this.hideVideo();
	document.body.style.position = 'static';
	let doSearch = () =>{
	    let code = jqid(ID_ASSETS_CODE).val();
	    if(!Utils.stringDefined(code)) {
		alert('Please enter a bar code');
		return;
	    }
	    let url = RamaddaUtil.getUrl('/search/do?forsearch=true&output=json&type=type_assets_base&max=100&skip=0&search.type_assets_base.asset_id='+ code);
	    jqid(ID_SCAN_RESULTS).html('');
	    let callback = {
		entryListChanged:(list) => {
		    let entries = list.getEntries();
		    if(entries.length==0) {
			let msg = HU.div([],'No assets found. Do you want to create a new asset?');
			msg+=HU.div([ATTR_ID,"newasset"],"Yes");
			jqid(ID_SCAN_RESULTS).html(msg);
			jqid("newasset").button().click(()=>{
			    this.dialog.remove();
			    this.dialog=null;
			    this.makeNewDialog(code);
			});
			return;
		    }
		    let html = HU.center(HU.b('Results'));
		    entries.forEach(entry=>{
			html+=HU.div([],entry.getLink(null,true,[ATTR_CLASS,'ramadda-decor ramadda-clickable']));
		    });
		    jqid(ID_SCAN_RESULTS).html(HU.div([ATTR_STYLE,HU.css('border-top','var(--basic-border)','padding','5px')],html));
		}
	    }
	    let entryList = new EntryList(getGlobalRamadda(), url,callback,true);

	}

	jqid(ID_ASSETS_CODE).keydown(e=>{
            if(Utils.isReturnKey(e)) {
		doSearch();
            }
        });


	dialog.find('.ramadda-button-ok').button().click(()=>{
	    doSearch();
	});
        dialog.find('.ramadda-button-cancel').button().click(()=>{
	    this.showVideo();
            this.dialog.remove();
	    this.dialog=null;
        });
    },	
    makeVideo:function(args) {
	let html = HU.center(HU.div([ATTR_ID,this.headerId]));
	this.videoId= this.id+'_video';
	html+=HU.center("<video class=assets_barcode_video id='" + this.videoId+ "'  autoplay muted playsinline></video>\n");
		
	jqid(this.id).append(HU.div([ATTR_ID,this.contentsId],html));
	this.initVideo(args);
    },	

    initVideo:function(args) {
	args = args??{};
	let title = 'create an asset';
	if(this.opts.defaultTypeLabel)
	    title = 'create a ' + this.opts.defaultTypeLabel;
	let header = args.header ?? 'Scan a bar code or directly ' + HU.span([ATTR_ID,'create',
							       ATTR_CLASS,'assets-create-link'],title);
	jqid(this.headerId).html(header);
	jqid('create').click(()=>{
	    return this.handleBarcode('');
	});
	let callback = args.callback;
	if(callback) callback= (code)=>{
	    if(!jqid(this.videoId).is(':visible')) return;
	    return this.handleBarcode(code);
	}
	this.barcodeReader = new BarcodeReader(this.videoId,callback);
	if(this.opts.debug) {
	    setTimeout(()=>{this.handleBarcode('example-bar-code');},3000);
	}
    },


    hideVideo:function() {
	$(this.barcodeReader.videoElement).hide();
    },
    showVideo:function() {
	$(this.barcodeReader.videoElement).show();
    },    
    handleBarcode:function(code) {
	if(this.dialog) return;
	this.opts.code = code;
	if(this.opts.scanMode) {
	    this.handleScanMode(code);
	    return;
	}

	this.makeNewDialog(code);
    },

    makeNewDialog:function(code) {
	this.opts.code = code;
	let rows =[];
	rows.push(HU.b('Barcode: ') +
		  HU.input('',code,[ATTR_STYLE,HU.css('width','300px'),
				    ATTR_ID,ID_ASSETS_CODE,
				    ATTR_PLACEHOLDER,'Enter bar code']));

	if(!this.opts.defaultType) {
	    rows.push(HU.div([],HU.b('Asset Type: ') +
			     HU.select('',[ATTR_ID,ID_ASSETS_TYPE],
				       ASSET_TYPES)));
	    
	}

	rows.push(HU.input('','',[ATTR_STYLE,HU.css('width','300px'),
				  ATTR_ID,ID_ASSETS_NAME,
				  ATTR_PLACEHOLDER,'Enter name']));
	rows.push(HU.checkbox('',[ATTR_ID,ID_ASSETS_ADDGEOLOCATION],true,'Add map location'));
	let buttons  =HU.div([ATTR_CLASS,'ramadda-button-ok display-button'], 'Create Asset') + SPACE2 +
	    HU.div([ATTR_CLASS,'ramadda-button-cancel display-button'], 'Cancel');
	rows.push(HU.center(buttons));
	let html = Utils.wrap(rows,'<div style="margin-bottom:8px;">','</div>');
        html=HU.div([ATTR_STYLE,HU.css('margin','8px')],html);
	let anchor = jqid(this.videoId);
	let title = 'Create Asset';
	if(this.opts.defaultTypeLabel)
	    title = 'Create '+ this.opts.defaultTypeLabel;
	let dialog = this.dialog = HU.makeDialog({content:html,
						  anchor:anchor,
						  at:'center top',
						  my:'center top',
						  showCloseIcon:false,
						  title:title,header:true});	

	const input = document.getElementById(ID_ASSETS_NAME);
	setTimeout(() => {
	    input.focus();
	    input.scrollIntoView({ block: "center" });
	    }, 300);

	this.hideVideo();
	document.body.style.position = 'static';
	dialog.find('.ramadda-button-ok').button().click(()=>{
	    if(!this.opts.defaultType) {
		this.opts.type  = jqid(ID_ASSETS_TYPE).val();
	    }

	    this.opts.code = jqid(ID_ASSETS_CODE).val();
	    this.opts.name = jqid(ID_ASSETS_NAME).val();
	    if(jqid(ID_ASSETS_ADDGEOLOCATION).is(':checked')) {
		dialog.hide();
		Utils.getGeoLocation((result)=>{
		    this.opts.geolocation = result;
		    this.createEntry();
		});
		return;
	    }
	    this.dialog.remove();
	    this.dialog=null;
	    this.createEntry();
	});
        dialog.find('.ramadda-button-cancel').button().click(()=>{
	    this.showVideo();
            this.dialog.remove();
	    this.dialog=null;

        });
    },

    createEntry:function() {
	let args =['group',this.opts.entryid,
		   'type',this.opts.type??this.opts.defaultType,
		   'asset_id',this.opts.code];
	if(Utils.stringDefined(this.opts.name)) {
	    args.push(ID_ASSETS_NAME,this.opts.name);
	}
	if(this.opts.geolocation) {
	    args.push('latitude',Utils.trimDecimals(this.opts.geolocation.latitude,5),
		      'longitude',Utils.trimDecimals(this.opts.geolocation.longitude,5));				 
	}
	let url = '/repository/entry/form';
	url = HU.url(url,args);
	window.location.href=url;

    }
}
