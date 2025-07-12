
var ID_ASSETS_NAME = 'name';
var ID_ASSETS_CODE = 'barcode';
var ID_ASSETS_ADDGEOLOCATION = 'addgeolocation';
var ID_ASSETS_TYPE='assettype';
var ASSET_TYPES=[['type_assets_building','Building'],
		 ['type_assets_vehicle','Vehicle']];

function BarcodeReader (videoId,callback,args) {
    let opts = {
    }
    if(args) {
    }
    window.onerror = function (message, source, lineno, colno, error) {
	const errBox = document.createElement('pre');
	errBox.style.color = 'red';
	errBox.textContent = `JS Error: ${message} at ${source}:${lineno}:${colno}`;
	document.body.appendChild(errBox);
    };
    let videoElement = this.videoElement = document.getElementById(videoId);
    // Force attributes for iOS Safari
    videoElement.setAttribute('autoplay', '');
    videoElement.setAttribute('muted', '');
    videoElement.setAttribute('playsinline', '');

    const codeReader = new ZXing.BrowserMultiFormatReader();
    codeReader.decodeFromVideoDevice(null, videoElement, (result, error, controls) => {
	if (result) {
	    if(!callback(result.getText())) {
//		controls.stop();
	    }
	} else if(error) {
//	    console.log(error);
	}
    });

}


function AssetCreator (id,args) {
    this.args = args;
    this.id = id;
    let videoId= this.id+'_video';
    let headerId= this.id+'_header';    
    this.opts = {
	defaultType:null,
	videoId:videoId,
    }

    let header = 'Scan a bar code or directly ' + HU.span([ATTR_ID,'create',
						  ATTR_CLASS,'assets-create-link'],'create an asset');
    jqid(headerId).html(header);
    jqid('create').click(()=>{
	return this.handleBarcode('');
    });
    this.opts = $.extend(this.opts,args);
    let callback = (code)=>{
	if(!jqid(this.videoId).is(':visible')) return;
	return this.handleBarcode(code);
    }

    this.barcodeReader = new BarcodeReader(this.opts.videoId,callback);
    if(this.opts.debug) {
	setTimeout(()=>{this.handleBarcode('example-bar-code');},3000);
    }
}

AssetCreator.prototype = {
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
	rows.push(buttons);
	let html = Utils.wrap(rows,'<div style="margin-bottom:8px;">','</div>');
        html=HU.div([ATTR_STYLE,HU.css('margin','8px')],html);
	let anchor = jqid(this.opts.videoId);
	let dialog = this.dialog = HU.makeDialog({content:html,
						  anchor:anchor,at:'left top',
						  showCloseIcon:false,
						  title:'Create Asset',header:true});	

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
