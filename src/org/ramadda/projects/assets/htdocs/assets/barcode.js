
var ID_ASSETS_NAME = 'name';
var ID_ASSETS_CODE = 'barcode';
var ID_ASSETS_ADDGEOLOCATION = 'addgeolocation';
var ID_ASSETS_TYPE='assettype';
var ASSET_TYPES=[['type_assets_building','Building'],
		 ['type_assets_vehicle','Vehicle']];

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


function AssetCreator (id,args) {
    let _this = this;
    this.args = args;
    this.opts = {
	defaultType:null,
	defaultTypeLabel:null,
	editMode:false
    }
    this.opts = $.extend(this.opts,args??{});
    if(this.opts.editMode) {
	this.initEditMode();
	return;
    }

    this.id = id;
    this.buttonId= this.id+'_open';
    this.videoId= this.id+'_video';
    this.contentsId= this.id+'_contents';    
    this.headerId= this.id+'_header';    
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
		let html = HU.center(HU.div([ATTR_ID,this.headerId]));
		html+=HU.center("<video class=assets_barcode_video id='" + this.videoId+ "'  autoplay muted playsinline></video>\n");
		
		jqid(this.id).append(HU.div([ATTR_ID,this.contentsId],html));
		this.initVideo();
	    }
	}
	this.videoOpen=!this.videoOpen;
    })


}

AssetCreator.prototype = {
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
		dialog.remove();
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
    initVideo:function() {
	let title = 'create an asset';
	if(this.opts.defaultTypeLabel)
	    title = 'create a ' + this.opts.defaultTypeLabel;
	let header = 'Scan a bar code or directly ' + HU.span([ATTR_ID,'create',
							       ATTR_CLASS,'assets-create-link'],title);
	jqid(this.headerId).html(header);
	jqid('create').click(()=>{
	    return this.handleBarcode('');
	});
	let callback = (code)=>{
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
	let rows =[];
	rows.push(HU.b('Barcode: ') +
		  HU.input('',this.opts.code,[ATTR_STYLE,HU.css('width','300px'),
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
						  anchor:anchor,at:'left top',
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
//	this.createEntry();
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
	console.log(url);
	window.location.href=url;

    }
}
