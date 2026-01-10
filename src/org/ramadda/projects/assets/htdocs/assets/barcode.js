
function barcodeDebug() {
    let s = Utils.join(arguments,' ');
    //    jqid('barcodedebug').append(HU.div([],s));
//    console.log(s);
}
function barcodeInfo() {
    let s = Utils.join(arguments,' ');
    jqid('barcodedebug').append(HU.div([],s));
    console.log(s);
}


function barcodeError() {
    console.trace()
    let s = Utils.join(arguments,' ');
    jqid('barcodedebug').append(HU.div([ATTR_STYLE,HU.css(CSS_BACKGROUND,'red')],s));
    console.error(s);
}

window.onerror = function (message, source, lineno, colno, error) {
    barcodeError(`Error: ${message} at ${source}:${lineno}:${colno}`);
};

window.addEventListener('unhandledrejection', event => {
    barcodeError('Unhandled rejection: ' + event.reason);
});

var ID_ASSETS_NAME = 'name';
var ID_ASSETS_CODE = 'barcode';
var ID_ASSETS_ADDGEOLOCATION = 'addgeolocation';
var ID_ASSETS_TYPE='assettype';
var ASSET_TYPES=[['type_assets_vehicle','Vehicle'],
		 ['type_assets_equipment','Equipment'],
		 ['type_assets_equipment_construction','Construction Equipment'],
		 ['type_assets_fixture','Furniture/Fixture'],
		 ['type_assets_it','IT Asset'],
		 ['type_assets_building','Building'],
		];

function BarcodeReader (videoId,callback,args) {
    let videoElement = this.videoElement = document.getElementById(videoId);
    // Force attributes for iOS Safari
    videoElement.autoplay = true;
    videoElement.muted = true;
    videoElement.playsInline = true; // note camelCase property
    videoElement.muted = true;
    videoElement.setAttribute('webkit-playsinline', '');

    //    videoElement.setAttribute('autoplay', '');    videoElement.setAttribute('muted', '');    videoElement.setAttribute('playsinline', '');

    barcodeDebug('making ZXing reader');
    this.startScanner(callback,videoElement,args);
    barcodeDebug('after making ZXing reader');

    this.cancel = ()=>{
	this.codeReader.reset();
    }
}


BarcodeReader.prototype = {
    startScanner:async function(callback,videoElement,args) {
	let opts = {};
	if(args) opts = $.extend(opts,args);

	try {
	    barcodeDebug('startScanner');
	    const devices = await navigator.mediaDevices.enumerateDevices();
	    const videoInputDevices = devices.filter(d => d.kind === 'videoinput');
	    barcodeDebug('video devices:' + videoInputDevices.length);
	    if (videoInputDevices.length === 0) {
		barcodeError('No video input devices found.');
		return;
	    }

	    navigator.mediaDevices.getUserMedia({
		video: { width: { ideal: 1280 }, height: { ideal: 720 } }
	    });


	    //	    Object.keys(ZXing.BarcodeFormat).forEach(key=>{onsole.log(key);});

	    const hints = new Map();
	    //	    hints.set(ZXing.DecodeHintType.POSSIBLE_FORMATS, [ZXing.BarcodeFormat.EAN_13]);
	    videoInputDevices.forEach((device,index)=>{
		//		console.dir(device);
		//		barcodeInfo('video index:',index, ' kind:',device.kind, 'id:',device.deviceId);
	    });
	    const selectedDeviceId = videoInputDevices[0].deviceId;
	    const codeReader = this.codeReader = new ZXing.BrowserMultiFormatReader();
	    let readerDevices = await codeReader.listVideoInputDevices();
	    readerDevices.forEach((device, index) => {
		//		barcodeInfo('index:',index, ' label:',device.label, 'id:',device.deviceId,'kind:',device.kind);
	    });

	    const constraints = {
		video: { facingMode: { exact: 'environment' }, width: { ideal: 1280 }, height: { ideal: 720 } }
	    };

	    let cnt = 0;
	    let seen = {};
	    //	    this.codeReader.decodeFromVideoDevice(selectedDeviceId, videoElement, (result, error, controls) => {
	    this.firstAttempt = performance.now();
	    this.lastAttempt = performance.now();	    
	    let handleResponse = (result, error, controls) => {
		if (result) {
		    //		    barcodeInfo('result',result);
		    if(!callback(result,controls)) {
			//		controls.stop();
		    }
		} else if(error) {
		    if (error instanceof ZXing.NotFoundException) {
			const now = performance.now();
			const diff= now - this.lastAttempt;
			if (diff > 500) {
			    if(opts.statusCallback) {
				opts.statusCallback('Still scanning...',now-this.firstAttempt);
			    }
			    this.lastAttempt = now;
			}
		    }
		    let s = error+'';
		    if(!seen[s]) {
			//			barcodeInfo('barcode: ', error);
			seen[s] = true;
		    }
		} else {
		    
		}
	    };
	    try {
		await this.codeReader.decodeFromConstraints(constraints,videoElement, handleResponse,hints);
	    } catch(err) {
//		console.log('error calling decodeFromConstraints. trying default camera',err);
		this.codeReader.decodeFromVideoDevice(selectedDeviceId, videoElement, handleResponse,hints);
	    }
	} catch(err) {
	    barcodeError('Barcode error starting scanner: '+ err);
	}
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
	try {
	    this.initEditMode();
	} catch(err) {
	    barcodeError('Barcode error: initEditMode:'+ err);
	}
	return;
    }

    this.id = id;
    this.buttonId= this.id+'_open';
    this.contentsId= this.id+'_contents';    
    this.headerId= this.id+'_header';    
    if(this.opts.scanMode) {
	try {
	    this.initScanMode();
	} catch(err) {
	    barcodeError('Barcode error - initScanMode: '+ err);
	}
	return;
    }

    jqid(this.id).append(HU.div([ATTR_ID,this.buttonId],'Scan bar code'));
    this.videoOpen=false;
    jqid(this.buttonId).button().click(()=>{
	if(this.videoOpen) {
	    jqid(this.buttonId).html('Scan bar code');
	    jqid(this.contentsId).hide(500);
	    if(this.dialog) this.dialog.remove();
	    this.dialog=null;
	} else {
	    jqid(this.buttonId).html('Close');
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
    showStatus:function(message,ms) {
	console.log(message);
	if(this.statusId) {
	    let seconds = parseInt(ms/1000);
	    message = HU.getIconImage('fa-solid fa-spinner fa-spin') + SPACE +' scanning';
	    jqid(this.statusId).html(message);
	}
    },
    initEditMode: function() {
	let name = 'edit_type_assets_base_asset_id';
        this.assetIdInput= HU.jqname(name);
	let buttonId= HU.getUniqueId('editbutton');
	this.id = HU.getUniqueId('editmode');
	this.videoId= this.id+'_video';
	this.assetIdInput.after(SPACE2+
				HU.span([ATTR_ID,buttonId],'Scan Bar Code'));
	jqid(buttonId).button().click(()=>{
	    let html='';
	    html=HU.center(HU.div([ATTR_STYLE,HU.css(CSS_TEXT_ALIGN,ALIGN_CENTER,
						     CSS_MARGIN,HU.px(5)),
				   ATTR_ID,this.id+'_cancel'],LABEL_CANCEL));
	    html+=HU.center(HU.tag(TAG_VIDEO,
				   [ATTR_CLASS,'ramadda-barcode-video-editmode',
				    ATTR_ID,this.videoId,
				    'autoplay',null,
				    'muted',null,
				    'playsinline']));
	    html+=HU.div([ATTR_CLASS,'ramadda-barcode-status',
			  ATTR_STYLE,HU.css(CSS_PADDING,HU.px(10)),
			  ATTR_ID,this.statusId=this.id+'_status'],'&nbsp;');
	    let dialog = this.dialog = HU.makeDialog({content:html,
						      anchor:jqid(buttonId),
						      at:POS_LEFT_TOP,
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
		barcodeInfo('Scanned:', result.getText(), 'Format:', result.getBarcodeFormat());
		cancel();
		this.assetIdInput.val(result.getText());
		this.assetIdInput.css(CSS_BACKGROUND,'yellow');
		setTimeout(()=>{
		    this.assetIdInput.css(CSS_BACKGROUND,'#fff');
		},3000);
		
	    };
	    this.barcodeReader = new BarcodeReader(this.videoId,callback,
						   {statusCallback:(message,ms)=>this.showStatus(message,ms)}
);
	});
    },
    initScanMode:function() {
	let header =  'Scan a bar code to search for asset or ' +
	    HU.span([ATTR_ID,'searchasset',
		     ATTR_CLASS,'assets-create-link'],'search');
	let status = HU.span([ATTR_CLASS,'ramadda-barcode-status',
			     ATTR_ID,this.statusId=this.id+'_status'],'&nbsp;');
	header= header  + HU.space(4)+status;
	this.makeVideo({header:header});
	jqid('searchasset').click(()=>{
	    return this.handleBarcode('');
	});

    },
    handleScanMode:function(code) {
	var ID_SCAN_RESULTS ='scanresults';
	let rows =[];
	rows.push(HU.boldLabel('Barcode') +
		  HU.input('',this.opts.code,[ATTR_STYLE,
					      HU.css(CSS_WIDTH,HU.px(300)),
					      ATTR_ID,ID_ASSETS_CODE,
					      ATTR_PLACEHOLDER,'Enter bar code']));
	let buttons  =HU.div([ATTR_CLASS,HU.classes(CLASS_BUTTON_OK,'display-button')], 'Find Asset') +
	    SPACE2 +
	    HU.div([ATTR_CLASS,HU.classes(CLASS_BUTTON_CANCEL,'display-button')], LABEL_CANCEL);
	rows.push(HU.center(buttons));
	rows.push(HU.div([ATTR_ID,ID_SCAN_RESULTS]));
	let html = Utils.wrap(rows,'<div style="margin-bottom:8px;">',HU.close(TAG_DIV));
        html=HU.div([ATTR_STYLE,HU.css(CSS_MARGIN,HU.px(8))],html);
	let anchor = jqid(this.videoId);
	let title = 'Find Asset';
	let dialog = this.dialog = HU.makeDialog({content:html,
						  draggable:true,
						  anchor:anchor,
						  at:'center top',
						  my:'center top',						  
						  showCloseIcon:false,
						  title:title,header:true});	

	const input = document.getElementById(ID_ASSETS_CODE);
	setTimeout(() => {
	    input.focus();
	    input.scrollIntoView({ block: 'center' });
	}, 300);

	//	this.hideVideo();
	document.body.style.position = 'static';
	let doSearch = () =>{
	    let code = jqid(ID_ASSETS_CODE).val();
	    if(!Utils.stringDefined(code)) {
		alert('Please enter a bar code');
		return;
	    }
	    let url = HU.url(RamaddaUtil.getUrl(URL_SEARCH_DO),
			     'forsearch',true,
			     ARG_OUTPUT,'json',
			     ARG_TYPE,'type_assets_base',
			     'max',100,'skip',0,
			     'search.type_assets_base.asset_id',code);
	    jqid(ID_SCAN_RESULTS).html('');
	    let callback = {
		entryListChanged:(list) => {
		    let entries = list.getEntries();
		    if(entries.length==0) {
			let msg = HU.div([ATTR_STYLE,
					  HU.css(CSS_BORDER_TOP,
						 'var(--basic-border)',
						 CSS_PADDING,HU.px(5))],
					 'No assets found. Do you want to create a new asset?');
			msg+=HU.center(HU.div([ATTR_ID,'newasset'],'Yes'));
			jqid(ID_SCAN_RESULTS).html(msg);
			jqid('newasset').button().click(()=>{
			    this.dialog.remove();
			    this.dialog=null;
			    this.makeNewDialog(code);
			});
			return;
		    }
		    let html = '';
		    entries.forEach(entry=>{
			html+=HU.div([],entry.getLink(null,true,[ATTR_CLASS,'ramadda-decor ramadda-clickable']));
		    });
		    html = HU.center(HU.b('Results')) +
			HU.div([ATTR_STYLE,HU.css(CSS_MAX_HEIGHT,HU.px(300),
						  CSS_OVERFLOW_Y,OVERFLOW_AUTO)],
			       html);
		    jqid(ID_SCAN_RESULTS).html(HU.div([ATTR_STYLE,
						       HU.css(CSS_BORDER_TOP,
							      'var(--basic-border)',
							      CSS_PADDING,HU.px(5))],html));
		}
	    }
	    let entryList = new EntryList(getGlobalRamadda(), url,callback,true);

	}

	jqid(ID_ASSETS_CODE).keydown(e=>{
            if(Utils.isReturnKey(e)) {
		doSearch();
            }
        });


	dialog.find(HU.dotClass(CLASS_BUTTON_OK)).button().click(()=>{
	    doSearch();
	});
        dialog.find(HU.dotClass(CLASS_BUTTON_CANCEL)).button().click(()=>{
	    this.showVideo();
            this.dialog.remove();
	    this.dialog=null;
        });	
    },	
    makeVideo:function(args) {
	let html = HU.center(HU.div([ATTR_ID,this.headerId]));
	this.videoId= this.id+'_video';
	html+=HU.center("<video class=ramadda-barcode-video id='" + this.videoId+ "'  autoplay muted playsinline></video>\n");
	jqid(this.id).append(HU.div([ATTR_ID,this.contentsId],html));
	try {
	    this.initVideo(args);
	} catch(err) {
	    barcodeError('Barcode error initVideo:' + err);
	}
    },	

    initVideo:function(args) {
	args = args??{};
	let title = 'create an asset';
	if(this.opts.defaultTypeLabel)
	    title = 'create a ' + this.opts.defaultTypeLabel;
	let header = args.header;
	if(!header) {
	    let status = HU.span([ATTR_CLASS,'ramadda-barcode-status',
				  ATTR_ID,this.statusId=this.id+'_status'],'&nbsp;');
	    header =
		'Scan a bar code or directly ' + HU.span([ATTR_ID,'create',
							  ATTR_CLASS,'assets-create-link'],title);
	    header= header  + HU.space(4)+status;
	}
	
	jqid(this.headerId).html(header);
	jqid('create').click(()=>{
	    return this.handleBarcode('');
	});
	let callback = args.callback;
	if(!callback) callback= (result,controls)=>{
	    if(!jqid(this.videoId).is(':visible')) return;
	    return this.handleBarcode(result.getText(),controls);
	}
	this.barcodeReader = new BarcodeReader(this.videoId,callback,
					       {statusCallback:(message,ms)=>this.showStatus(message,ms)}
					      );
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
    handleBarcode:function(code,controls) {
	if(this.dialog) {
	    this.dialog.remove();
	}
	this.opts.code = code;
	if(this.opts.scanMode) {
	    this.handleScanMode(code,controls);
	    return;
	}
	this.makeNewDialog(code,controls);
    },

    makeNewDialog:function(code) {
	this.opts.code = code;
	let rows =[];
	rows.push(HU.b('Barcode: ') +
		  HU.input('',code,[ATTR_STYLE,HU.css(CSS_WIDTH,HU.px(300)),
				    ATTR_ID,ID_ASSETS_CODE,
				    ATTR_PLACEHOLDER,'Enter bar code']));

	if(!this.opts.defaultType) {
	    rows.push(HU.div([],HU.b('Asset Type: ') +
			     HU.select('',[ATTR_ID,ID_ASSETS_TYPE],
				       ASSET_TYPES)));
	    
	}

	rows.push(HU.input('','',[ATTR_STYLE,HU.css(CSS_WIDTH,HU.px(300)),
				  ATTR_ID,ID_ASSETS_NAME,
				  ATTR_PLACEHOLDER,'Enter name']));
	rows.push(HU.checkbox('',[ATTR_ID,ID_ASSETS_ADDGEOLOCATION],true,'Add map location'));
	let buttons  =HU.div([ATTR_CLASS,HU.classes(CLASS_BUTTON_OK,'display-button')], 'Create Asset') + SPACE2 +
	    HU.div([ATTR_CLASS,HU.classes(CLASS_BUTTON_CANCEL,'display-button')], LABEL_CANCEL);
	rows.push(HU.center(buttons));
	let html = Utils.wrap(rows,'<div style="margin-bottom:8px;">',HU.close(TAG_DIV));
        html=HU.div([ATTR_STYLE,HU.css(CSS_MARGIN,HU.px(8))],html);
	let anchor = jqid(this.videoId);
	let title = 'Create Asset';
	if(this.opts.defaultTypeLabel)
	    title = 'Create '+ this.opts.defaultTypeLabel;
	let dialog = this.dialog = HU.makeDialog({content:html,
						  anchor:anchor,
						  draggable:true,
						  at:'center top',
						  my:'center top',
						  showCloseIcon:false,
						  title:title,header:true});	

	const input = document.getElementById(ID_ASSETS_NAME);
	setTimeout(() => {
	    input.focus();
	    input.scrollIntoView({ block: 'center' });
	}, 300);

	this.hideVideo();
	document.body.style.position = 'static';
	dialog.find(HU.dotClass(CLASS_BUTTON_OK)).button().click(()=>{
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
        dialog.find(HU.dotClass(CLASS_BUTTON_CANCEL)).button().click(()=>{
	    this.showVideo();
            this.dialog.remove();
	    this.dialog=null;

        });
    },

    createEntry:function() {
	let args =[ARG_GROUP,this.opts.entryid,
		   'defaultgroup',this.opts.entryid,
		   ARG_TYPE,this.opts.type??this.opts.defaultType,
		   'asset_id',this.opts.code];
	if(Utils.stringDefined(this.opts.name)) {
	    args.push(ID_ASSETS_NAME,this.opts.name);
	}
	if(this.opts.geolocation) {
	    args.push('latitude',Utils.trimDecimals(this.opts.geolocation.latitude,5),
		      'longitude',Utils.trimDecimals(this.opts.geolocation.longitude,5));				 
	}
	let url = RamaddaUtil.getUrl('/entry/form');
	url = HU.url(url,args);
	window.location.href=url;

    }
}
