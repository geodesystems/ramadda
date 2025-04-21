//see https://mistic100.github.io/jQCloud/
function ramaddaWordCloud(source,target,args) {
    let opts = {
	colorTable:null,
	shape:'elliptic',
	delay:5
    }
    if(args) $.extend(opts,args);

    if(opts.headerId) {
	let handler = (value) =>{
	    ramaddaWordCloudCreate(source,target,opts,value);
	}
	jqid(opts.headerId).css('margin-bottom','0.5em');
	HU.initPageSearch('#'+ source,null,'Find',false,{handler:handler,target:'#'+opts.headerId});
    }
    ramaddaWordCloudCreate(source,target,opts);
}

function ramaddaWordCloudCreate(source,target,opts,match) {
    let cloudId = HU.getUniqueId('cloud');
    jqid(target).html(HU.div([ATTR_ID,cloudId,ATTR_STYLE,HU.css('width','100%','height','100%')]));
    let words = [];
    if(match) match=match.toLowerCase();
    jqid(source).find('.ramadda-word').each(function() {
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
    let steps = 10;
    let colors = null;
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
