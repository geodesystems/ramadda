function RamaddaReader(id,args,data) {
    this.data=data;
    let width = args.width??'100%';
    let height = args.height??'70vh';    
    delete args.width;
    delete args.height;
    var  opts  = {
	showToc:true,
	ppi:100,
	ui:'full',
	imagesBaseURL:ramaddaBaseUrl+'/lib/bookreader/images/',
	enableSearch:true,
	data:data
    }
    $.extend(opts, args);
    let left = '';
    data.forEach((d,idx)=>{
	d = d[0];
	let label = '#'+(idx+1) +': ' +Utils.makeLabel(d.name);
	label = label.replace(/\.[^\.]+$/,'');
	left+=HU.div([ATTR_STYLE,'white-space:nowrap;',ATTR_CLASS,'ramadda-clickable','data-entryid',d.entryid,'data-index',idx],label);
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
    jqid(leftId).find('.ramadda-clickable').click(function() {
	_this.br.jumpToIndex(+$(this).attr('data-index'));
    });
}

RamaddaReader.prototype = {
};
