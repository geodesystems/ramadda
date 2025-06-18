function NwsAlerts(url,div,opts) {
    $.extend(this,{
	url:url,
	div:div
    });
    if(opts) $.extend(this,opts);
    this.init();
  
}

NwsAlerts.prototype = {
    init:function() {
        $.getJSON(this.url), data=>{
	}).fail(data=>{
	    console.error('NWS Alerts: failed to read alert:' + this.url);
	    console.dir(data);
	});
    }
}


