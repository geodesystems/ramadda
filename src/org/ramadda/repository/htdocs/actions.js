//"use strict";
/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/



function RamaddaActionManager(id,url,args) {
    args = args??{};
    $.extend(this,{
	id:id,
	display: jqid(id),
	running:true,
	url:url
    });
    $.extend(this,args);
    this.init();
}

RamaddaActionManager.prototype = {
    init: function() {
	if(this.cancelUrl) {
	    this.cancelButton =    jqid(this.id+'_cancel');
	    if(this.cancelButton.length) {
		let button = $('<button>Cancel</button>');
		this.cancelButton.append(button);
		button.button();
		button.click(()=>{
		    if(this.cancelUrl && this.running) {
			this.cancelled = true;
			this.running =false;
			$.get(this.cancelUrl);
			this.display.html('');
			this.cancelButton.html('Cancelled');
		    }
		});
	    }
	}
	this.handleActionResults();
    },

    finished:function (msg) {
	this.running=false;
	this.cancelButton.html(msg);
    },
    handleActionResults: function() {
        setTimeout(() =>{
            let success=json=>{
		console.log(json.status,'running',this.running);
		if(!this.running) return;
                let msg = "";
                if(json.message) msg= json.message.replace(/\n/g,HU.br());
		this.display.html(msg);
		this.display.scrollTop(this.display[0].scrollHeight);
		if(json.heading) {
		    jqid(this.id+'_heading').html(json.heading);
		}

                if(json.status=='running') {
                    this.handleActionResults();
                } else {
		    this.finished('Done processing');
		}
            };
            let fail=json=>{
		console.dir('fail',json);
		this.finished('Error');
		let msg;
		if(json.responseText) msg = json.responseText;
		else msg =   json+'';
		this.display.html("Error: " + msg);
            };             
            $.getJSON(this.url, success).fail(fail);
        },1000);
    }
}

