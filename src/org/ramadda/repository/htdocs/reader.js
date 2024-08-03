function RamaddaReader(id,args,data) {
    this.data=data;
    this.args = args;
    let width = args.width??'100%';
    let height = args.height??'70vh';    
    delete args.width;
    delete args.height;
    var  opts  = {
	showToc:true,
	showSearch:true,
	ppi:100,
	ui:'full',
	imagesBaseURL:ramaddaBaseUrl+'/lib/bookreader/images/',
	enableSearch:true,
	data:data
    }
    $.extend(opts, args);
    let sid = HU.getUniqueId('search_');
    let left = '';
    if(opts.showSearch) {
	left +=HU.div([],HU.input('','',[ATTR_ID,sid,'placeholder','Search','size','12']));

    }
    data.forEach((d,idx)=>{
	d = d[0];
	let label = Utils.makeLabel(d.name);
	label = label.replace(/\.[^\.]+$/,'');
	let href =HU.href(RamaddaUtils.getEntryUrl(d.entryid),'#'+(idx+1),['target','entry']);
	label = href +': '+label;
	left+=HU.div([ATTR_STYLE,'padding:2px;white-space:nowrap;',ATTR_CLASS,'ramadda-clickable ramadda-hoverable','data-entryid',d.entryid,'data-index',idx],label);
    });


    let leftId = HU.getUniqueId('left_');
    let w = '150px';
    left = HU.div([ATTR_ID,leftId,
		   ATTR_STYLE,HU.css('padding-right','8px','max-width',w,'width',w,'height',height,'max-height',height,'overflow-y','auto')],left);
    left = HU.div([ATTR_STYLE,HU.css('vertical-align','top','display','table-cell','max-width',w,'width',w)],left);
    if(!opts.showToc) 
	left=HU.div([ATTR_STYLE,HU.css('vertical-align','top','display','table-cell','max-width','1px','width','1px')],'');
    let rid = HU.getUniqueId('reader_');
    let main = HU.div([ATTR_ID,rid,ATTR_STYLE,HU.css('width',width,'height',height)]);
    main = HU.div([ATTR_STYLE,HU.css('display','table-cell','width',width)], main);
    let html = left+main;
    jqid(id).append(html);
    opts.el='#'+rid;
    this.br = new BookReader(opts);
    this.br.init();
    let _this = this;
    let tocItems =  jqid(leftId).find('.ramadda-clickable');
    let map = {};
    tocItems.each(function() {
	map[$(this).attr('data-entryid')] = $(this);
    });

    jqid(sid).keyup(function(event) {
	let v = $(this).val().trim();
	if(v=='') {
	    tocItems.show();
	    return 
	}

    });

    jqid(sid).keypress(function(event) {
        let keycode = (event.keyCode ? event.keyCode : event.which);
        if (keycode != 13) return;
	let v = $(this).val().trim();
	if(v=='') {
	    tocItems.show();
	    return 
	}

        let url = RamaddaUtil.getUrl("/search/suggest?text=" + encodeURIComponent(v));
        url +="&ancestor=" + _this.args.entryid;
        let jqxhr = $.getJSON(url, function(data) {
	    tocItems.hide();
	    if(!data.values) return;
	    data.values.forEach(d=>{
		let ele = map[d.id];
		if(ele) {
		    ele.show();
		}
	    });
	});


    });

    tocItems.click(function() {
	_this.br.jumpToIndex(+$(this).attr('data-index'));
    });
}

RamaddaReader.prototype = {
};
