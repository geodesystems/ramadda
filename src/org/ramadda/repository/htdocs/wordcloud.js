//see https://mistic100.github.io/jQCloud/

function ramaddaWordCloud(source,target,args) {
    let opts = {
	colorTable:null,
	shape:'elliptic',
	delay:5,
	type:null
    }
    if(args) $.extend(opts,args);
    this.source = source;
    this.target=target;
    this.opts = opts;

    if(this.opts.types) {
	this.opts.types=Utils.split(this.opts.types,",",true,true).map(t=>{
	    let toks = t.split(":");
	    if(toks.length==1) return {
		type:t,
		label:t
	    }
	    return {
		type:toks[0],
		label:toks[1]
	    }
	});
    }
    if(!Utils.stringDefined(this.opts.type)) {
	this.opts.type = this.opts.types?this.opts.types[0].type:null;
    }
    this.loadJson();

    let _this = this;
    if(this.opts.headerId) {
	let menuId;
	if(this.opts.types &&this.opts.types.length>1) {
	    let  options = this.opts.types.map(t=>{
		return {value:t.type,label:t.label};
	    });
	    menuId = HU.getUniqueId('menu');
	    jqid(this.opts.headerId).append(HU.div([ATTR_STYLE,HU.css('margin-bottom','0.5em')],
						   HU.select('',[ATTR_ID,menuId], options)));
	    jqid(menuId).change(function() {
		_this.opts.type=$(this).val();
		_this.loadJson();
	    });
	}
	let handler = (value) =>{
	    this.create(value);
	}
	let search =HU.getUniqueId('search');
	jqid(this.opts.headerId).append(HU.div([ATTR_ID,search]));
	jqid(this.opts.headerId).css('margin-bottom','0.5em');
	HU.initPageSearch(null,null,'Find',false,{handler:handler,target:'#'+search});
    }
//    this.create();
}

ramaddaWordCloud.prototype = {
    loadJson:function() {
	let icon = RamaddaUtils.getUrl('/icons/mapprogress.gif');
	jqid(this.target).html(HU.center(HU.image(icon,[ATTR_STYLE,HU.css('margin-top','50px'),
							ATTR_WIDTH,'150px'])));
	let jsonUrl = RamaddaUtils.getUrl('/metadata/list?response=json&metadata_type=' + this.opts.type);
	$.getJSON(jsonUrl, data=>{
	    this.metadata = data;
	    this.create();
	}).fail(data=>{
	    console.log("Failed to load json:");
	});
    },
    create:function(match) {
	let cloudId = HU.getUniqueId('cloud');
	jqid(this.target).html(HU.div([ATTR_ID,cloudId,ATTR_STYLE,HU.css('width','100%','height','100%')]));
	let words = [];
	if(match) match=match.toLowerCase();
	if(this.metadata) {
	    let element=null;
	    if(this.metadata.elements) {
		this.metadata.elements.every(e=>{
		    if(e.values) {
			element = e;
			return false;
		    }
		    return true;
		});
	    }
	    if(element==null || element.values.length==0) {
		jqid(this.target).html(HU.center('No metadata available'));
		return
	    }
	    element.values.forEach(v=>{
		let url = RamaddaUtil.getUrl('/search/do?metadata_attr' + element.index+'_' + this.opts.type+'=' +
				   encodeURIComponent(v.value));
		let handlers = null;
		if(url)
		    handlers ={click: function() {window.open(url, "_word");}};

		if(match) {
		    let _word = v.label.toLowerCase();
		    if(_word.indexOf(match)<0) return;
		}


		let title = "Count: " + v.count +" Click to search";
		words.push({
		    text: v.label,	
		    html:{style:"cursor:pointer;",title:title},
		    weight:v.count,
		    handlers:handlers,
		});

		
	    });
	} else {
	    jqid(this.source).find('.ramadda-word').each(function() {
		let word = $(this).html();
		if(match) {
		    let _word = word.toLowerCase();
		    if(_word.indexOf(match)<0) return;
		}
		let weight = $(this).attr('word-weight');
		let url = $(this).attr('word-url');	
		let title = $(this).attr('word-title');	
		let handlers = null;
		if(url)
		    handlers ={click: function() {window.open(url, "_word");}};
		words.push({
		    text: word,
		    title:title,
		    html:{style:"cursor:pointer;",title:title},
		    weight:weight,
		    handlers:handlers,
		});
	    });
	}
	let steps = 10;
	let colors = null;
	let opts = this.opts;
	if(opts.colorTable) {
	    let ct = Utils.ColorTables[opts.colorTable];
	    if(ct) {
		//sample based on steps
		colors=[];
		for(let i=0;i<ct.colors.length;i+=steps) {
		    colors.push(ct.colors[i]);
		}
	    }
	}
	if(opts.color)    colors=[opts.color];
	let sizes = [];
	for(let i=60;i>6;i-=6) sizes.push(i+'px');
	let options  = {
	    steps:steps,
	    removeOverflowing:false,
	    delay:+opts.delay,
	    autoResize:true,
            colors:colors,
            classPattern: null,
            shape:opts.shape,
	    fontSize:sizes,
            xfontSize : {
		from: 0.1,
		to: 0.01
	    }
	};

	jqid(cloudId).jQCloud(words, options);
    }
};
