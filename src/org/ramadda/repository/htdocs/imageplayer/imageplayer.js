



function PlayerImage (url,label, dttm) {
    this.url = url;
    this.label = label;
    this.image = null;
    this.date = dttm;
    this.ok  =1;
}

PlayerImage.prototype = {
    getOk: function(){
	return this.ok;
    },
    setOk: function(v){
	return this.ok= v;
    }    
}




function ImagePlayer(args)  {
    const ID_LOOP_MODE='loop_mode';
    const ID_STARTSTOP = 'startstop';
    const dwell_step = 1;
    const delay_step = 100;
    const delay_max = 6000;
    const delay_min = 100;
    const dwell_multipler = 1;

    this.id = args.id;
    this.DIR_FORWARD = 0;
    this.DIR_BACKWARD = 1;
    this.MODE_NORMAL=0;
    this.MODE_ROCKING=1;

    this.images= [];
    this.delay = 500;
    this.end_dwell_multipler   = dwell_multipler;
    this.start_dwell_multipler = dwell_multipler;
    this.direction =this.DIR_FORWARD;
    this.running = false;
    this.play_mode = this.MODE_NORMAL;  
    
    if(args.images) {
	args.images.forEach(image=>{
	    let myImage = new PlayerImage(image.url,image.label,image.date);
            this.images.push(myImage);
	});
	delete args.images;
    }

    this.properties = $.extend({},args??{});
    this.currentImage = 0;
    if(Utils.stringDefined(this.properties.currentImage)) {
	if(this.properties.currentImage=='last') {
	    this.currentImage = this.images.length-1;
	} else {
	    this.currentImage = parseInt(this.properties.currentImage);
	}
    }
    if(this.properties.delay)
	this.delay = parseFloat(this.properties.delay);

    this.getProperty = function(name,dflt) {
	let v =this.properties[name];
	if(v==='true') return true;
	if(v==='false') return false;
	if(!Utils.isDefined(v)) return dflt;
	return v;
    }

    this.getId = function(name) {
	return this.id+ name;
    }

    this.jq = function(name) {
	return $("#"+this.id+name);
    }

    this.startStop = function() {
        if(this.running) {
	    this.stop();
        } else {
	    this.start();
        }
    }


    this.makeAnimation=()=>{
	let _this = this;
	let html = '&nbsp;';
	let lazy = this.getProperty('lazyLoading',this.images.length>10);
	//For now always be lazy
	lazy=true;
	this.images.forEach((image,idx)=>{
	    let attrs =    ['id',this.getId('image_' + idx),
			    'style', HU.css('position','absolute',
					    'top','0px','left','0px',
					    'width','100%',
					    'display',
					    idx==0?'block':'none')];
	    if(lazy)    attrs.push('loading','lazy');
	    html+=HU.image(image.url,attrs);
	    html+='\n';
	});

	let height= this.getProperty('imageHeight',null);
	let wrapperCss = HU.css('position','relative',  'width','100%')
	if(height) {
	    wrapperCss+=HU.css('height',HU.getDimension(height));
	}
	html = HU.div(['id',this.getId('imagewrapper'),'style',wrapperCss],
		      html);

	this.jq("animation").html(html);
	let imageWrapper = this.jq('imagewrapper');
	//As the window changes size check the size of the wrapper
	$(window).resize(()=>{
            if(this.pendingResizeTimeout) {
                clearTimeout(this.pendingResizeTimeout);
                this.pendingResizeTimeout = null;
	    }
	    let func = ()=>{
		let imageHeight=100;
		this.images.forEach((image,idx)=>{
		    if(!image.domElement) return;
		    let height = image.domElement.height();
		    if(height>imageHeight) {
			imageHeight=height;
			imageWrapper.css('height',height+'px');
		    }
		});
	    }
	    this.pendingResizeTimeout = setTimeout(func, 500);
	});
	let imageHeight=100;
	this.images.forEach((image,idx)=>{
	    image.domElement = this.jq('image_'+idx);
	    //Listen for load and set the height of the wrapper
	    let _this = this;
	    image.domElement.on('load',function(){
		image.loaded= true;
		if(_this.imageToHide) {
		    _this.imageToHide.domElement.hide();
		    _this.imageToHide=null;
		}

		let height = $(this).height();
		if(height>imageHeight) {
		    imageHeight=height;
		    imageWrapper.css('height',height+'px');
		}
	    });
	});
    }

    this.makeHeader=()=>{
	let compact = this.getProperty('compact',false);
	let buttons = HU.span(['id',this.getId('buttons')]) +HU.span(['id',this.getId('buttonsSuffix')]);
	let header = '';
	if(compact) {
	    header  = HU.leftRightTable(buttons,HU.div(['id',this.getId('date')]));
	} else {
	    header = buttons;
	}
	

	this.jq('header').html(HU.div(['style',HU.css('border-bottom','var(--basic-border)','padding-bottom','0em','margin-bottom','4px')],
				      header));
	let html = HU.leftRightTable(HU.div(['id',this.getId('label')]),
				     compact?'':HU.div(['id',this.getId('date')]));

	this.jq('imageheader').html(html);

	let boxesPosition = this.getProperty('boxesPosition',compact?'bottom':'top');
	let boxesStyle = '';
	if(boxesPosition=='top') {
	    boxesStyle+=HU.css('display','inline');
	} else 	if(boxesPosition=='bottom') {
	    boxesStyle+=HU.css('margin-top','2px');
	}
	let boxesWrapper = HU.div(['style', boxesStyle,'class','imageplayer-boxes','id',this.getId('boxes')])
	if(boxesPosition=='bottom') {
	    this.jq('footer').append(boxesWrapper)
	} else if(boxesPosition=='top') {
	    this.jq('buttonsSuffix').append(boxesWrapper)
	} else if(boxesPosition=='none') {
	} else {
	    console.log('unknown box position:' + boxesPosition);
	}
	

    }

    this.makeButtons = () =>{
	if(!this.getProperty('showButtons',true)) return;
	let clazz='ramadda-clickable'
	let compact = this.getProperty('compact',false);
	if(!this.getProperty('smallButtons',compact))
	    clazz += ' ramadda-button';
	let html = HU.open('div',['class','imageplayer-buttons']);
	[['Go to first frame','firstframe','fas fa-backward-step'],
	 ['One step back','stepback','fas fa-backward'],
	 ['Start/stop',ID_STARTSTOP,'fas fa-play'],
	 ['One step forward','stepforward','fas fa-forward'],
	 ['Go to last frame','lastframe','fas fa-forward-step']].forEach(tuple=>{
	     html+=HU.span(['id',this.getId(tuple[1]),
			    'style','margin-right:8px;',
			    'action',tuple[1],'class',clazz,'title',tuple[0]],
			   HU.getIconImage(tuple[2],'',['style','color:#000;']));
	 });
	html+=HU.close('div');
	this.jq('buttons').html(html);
	let _this = this;
	this.jq('buttons').find('.ramadda-clickable').click(function() {
	    let action = $(this).attr('action');
	    if(action=='firstframe')
		_this.setImage(0,1);
	    else if(action=='stepback')
		_this.setImage(_this.currentImage-1,-1);
	    else if(action=='startstop')
		_this.startStop();
	    else if(action=='stepforward') {
		_this.setImage(_this.currentImage+1,1)
	    }   else if(action=='lastframe')
		_this.setImage(-1,-1);
	    else
		console.log('Unknown action:' + action);
	});
    }

    this.makeControls=() =>{
	if(!this.getProperty('showControls',true)) return;
	let _this = this;
	let html = '';
	let item = (title,id,icon)=>{
	    html+=HU.span(['action',id,'title',title,'id',this.getId(id),'class','ramadda-clickable'],
			  HU.getIconImage(icon)) + HU.space(1);
	}

	let label=label=>{
	    html+=HU.space(3);
	    html+=label+' ';
	};
	html+='Loop Mode: ';
	item('Change loop mode',ID_LOOP_MODE, RamaddaUtil.getUrl('/imageplayer/button_norm.gif'));
	label('Adjust Speed:');
	item('Decrease speed','loop_decreasespeed','fas fa-caret-down');
	item('Increase speed','loop_increasespeed','fas fa-caret-up');	
	label('Start Dwell:');
	item('Descrease start dwell','loop_decreasestartdwell','fas fa-caret-down');
	item('Increase start dwell','loop_increasestartdwell','fas fa-caret-up');
	html+=HU.space(3);
	label('End Dwell:');
	item('Descrease end dwell','loop_decreaseenddwell','fas fa-caret-down');
	item('Increase end dwell','loop_increaseenddwell','fas fa-caret-up');
	html = HU.div(['id',this.getId('controls'),'style','border-top:var(--basic-border);margin-top:0.5em;'], html);
	this.jq('footer').append(html);
	this.jq('controls').find('.ramadda-clickable').click(function() {
	    let action = $(this).attr('action');
	    if(action==ID_LOOP_MODE)
		_this.toggleMode();
	    else if(action=='loop_decreasespeed')
		_this.changeSpeed(delay_step)
	    else if(action=='loop_increasespeed')
		_this.changeSpeed(-delay_step)	    
	    else if(action=='loop_decreasestartdwell')
		_this.changeStartDwell(-dwell_step)
	    else if(action=='loop_increasestartdwell')
		_this.changeStartDwell(dwell_step)
	    else if(action=='loop_decreaseenddwell')
		_this.changeEndDwell(-dwell_step)
	    else if(action=='loop_increaseenddwell')
		_this.changeEndDwell(dwell_step)	    
	    else
		console.log('Unknown action:'+ action);
	});
    }


    this.start = function() {
        this.running=true;
        HU.addToDocumentUrl("autoPlay", this.running);
	this.jq(ID_STARTSTOP).html(HU.getIconImage('fas fa-stop',null,['style','color:#000;']));
	if(this.timeout) 
	    clearTimeout(this.timeout);
        this.timeout =  setTimeout(()=>{
	    this.timeout = null;
	    this.play()}, this.delay);
    }

    this.stop = function() {
        this.running = false;
        HU.addToDocumentUrl("autoPlay", this.running);
	if(this.timeout) 
	    clearTimeout(this.timeout);
	this.jq(ID_STARTSTOP).html(HU.getIconImage('fas fa-play',null,['style','color:#000;']));
    }
    

    this.play = function() {
        if(this.direction == this.DIR_FORWARD) {
	    this.currentImage++;  
        } else {
	    this.currentImage--;
        }

        if (this.currentImage >= this.images.length) {
	    if (this.play_mode == this.MODE_NORMAL) {      
                this.currentImage = 0;
	    } else {
                //rocking
                this.currentImage = this.images.length-1;
                this.direction=this.DIR_BACKWARD;
	    }
        } else if (this.currentImage < 0) {
	    if (this.play_mode == this.MODE_NORMAL) {      
                this.currentImage = 0;
	    } else {
                this.currentImage = 0;
                this.direction=this.DIR_FORWARD;
	    }
        }


        has_selected = false;
        first_selected_image = this.images.length-1;
        last_selected_image = 0;
        for (let i = 0; i < this.images.length; i++) {
	    if(this.images[i].getOk()) { 
                has_selected = true;
                first_selected_image = i < first_selected_image ? i : first_selected_image;  
                last_selected_image = i > last_selected_image ? i : last_selected_image;  
	    }
        }

        if(has_selected) {
	    while (this.images[this.currentImage].getOk() == false) {
                this.currentImage++;
                if (this.currentImage >= this.images.length) {
		    if (this.play_mode == this.MODE_NORMAL) {
                        this.currentImage = 0;
		    } else {
                        this.currentImage = this.images.length-1;
                        this.direction=this.DIR_BACKWARD;
		    }
                }
	    }
        } else {
	    this.currentImage--;
        }
        this.delay_time = this.delay;
        if (this.currentImage == first_selected_image) {
	    this.delay_time = this.start_dwell_multipler*this.delay;
        }
        if (this.currentImage == last_selected_image) {
	    this.delay_time = this.end_dwell_multipler*this.delay;
        }

        this.displayImage();
	if(this.timeout) 
	    clearTimeout(this.timeout);
        this.timeout = setTimeout(()=>{this.play()}, this.delay_time);
    }


    this.changeSpeed = function(dv) {
        this.delay+=dv;
        if(this.delay > this.delay_max) this.delay = this.delay_max;
        if(this.delay < this.delay_min) this.delay = this.delay_min;
    }
    
    this.changeEndDwell = function(dv) {
        this.end_dwell_multipler+=dv;
        if ( this.end_dwell_multipler < 1 ) this.end_dwell_multipler = 0;
    }
    
    this.changeStartDwell = function(dv) {
        this.start_dwell_multipler+=dv;
        if ( this.start_dwell_multipler < 1 ) this.start_dwell_multipler = 0;
    }
    

    this.setImage = function(number,dir) {
	number = parseInt(number);
        this.stop();
        if (number < 0) number = this.images.length-1;
        if (number >= this.images.length) number = 0;
        let hasSelected = false;
	this.images.forEach(image=>{
	    if(image.getOk()) {
		hasSelected = true;
	    }
        });
        if(!hasSelected) {
	    return
	};

        while (this.images[number] && this.images[number].getOk() == false) {
	    number+=dir;
	    if (number < 0) number = this.images.length-1;
	    else if(number>=this.images.length) number = 0;
        }
        this.currentImage = number;
        this.displayImage();
    }


    this.toggleMode = function()  {
        if(this.play_mode==this.MODE_NORMAL) this.changeMode(this.MODE_ROCKING);
        else this.changeMode(this.MODE_NORMAL);
    }
    

    this.changeMode = function(mode) {
	let modeNormalImage =  RamaddaUtil.getUrl('/imageplayer/button_norm.gif');
	let modeRockingImage =  RamaddaUtil.getUrl('/imageplayer/button_sweep.gif');
        this.play_mode = mode;
        let url;
        if(this.play_mode == 0) {
	    url= modeNormalImage;
        } else {
	    url = modeRockingImage;
        }
        this.jq(ID_LOOP_MODE).html(HU.image(url));
    }
    
    this.setBoxes = function() {
	if(!this.getProperty('showBoxes',true)) return;
        let boxes="";
	let boxHeight = this.getProperty('boxHeight','1.5em');
        for (let i = 0; i < this.images.length; i++) {
	    let title = "Date:  " + this.images[i].date; 
	    let hidecolor = "blue";
	    let color = "blue";
	    let isActiveImage = false;

	    if(this.images[i].getOk()==0) {
                hidealt=HU.attr('title','Use image  ' + (i+1) +' in animation'); 
                color = "red";
                hidecolor = "red";
	    } else {
                hidealt=HU.attr('title',"Don't use image  " + (i+1) +" in animation"); 
                if(i == this.currentImage) {
		    color = "green";
		    isActiveImage = true;
                }
	    }
	    let width=14;
	    while(width>1 && this.images.length*width >400) {
                width--;
	    }
	    let imgAttrs =  HU.attr("class","imageplayer-filler") + HU.attr('title',title);
	    let boxStyle = HU.css("width" ,width+"px");
	    if(this.images.length>400)
		boxStyle += HU.css('margin-left','0px');
	    else  if(this.images.length>200)
		boxStyle += HU.css('margin-left','1px');

	    boxStyle+=HU.css('height',boxHeight);
	    let filler = HtmlUtil.div(["image-index",i,
				       "title", title, "class", "ramadda-clickable imageplayer-box imageplayer-box-" + (isActiveImage?"on":"off"),"style",boxStyle]);
	    boxes+= filler;
        }
        this.jq("boxes").html(boxes);
	let _this = this;
	this.jq("boxes").find(".imageplayer-box").click(function() {
	    _this.setImage($(this).attr("image-index"),-1);
	});
	
    }

    this.checkImage = function(status,i) {
        this.images[i].setOk(status);
        this.setBoxes();
    }

    this.toggleImageOk = function(i) {
        if(this.images[i].getOk()==0) this.images[i].setOk(1);
        else this.images[i].setOk(0);
        this.setBoxes();
    }

    this.goToImage = function(frame_num) {
        this.stop();
        this.currentImage = frame_num;
        this.displayImage();
    }

    this.displayImage = function() {
        if(this.images.length==0) {
	    return
        }
        HU.addToDocumentUrl("currentImage", this.currentImage);
	let image = this.images[this.currentImage];
	if(!image) return;
	let loaded = image.loaded;
	this.images.forEach((image,idx)=>{
	    if(idx!=this.currentImage) {
		if(image.domElement.is(':visible')) {
		    if(!loaded) {
			this.imageToHide = image;
		    } else {
			image.domElement.hide();
		    }
		}
	    }
	});
	image.domElement.show();
        this.setBoxes();
	if(this.getProperty('showLabel',true))
            this.jq("label").html(image.label);
	if(this.getProperty('showDate',true))
            this.jq("date").html(image.date);	
        this.jq("framenumber").attr("value",  (this.currentImage+1));
    }


    this.makeHeader();
    this.makeButtons();
    this.makeControls();    
    setTimeout(()=>{
	this.makeAnimation();
	this.displayImage();
    },1);
    if(this.getProperty('autoPlay')) {  
	this.start();
    }

}

