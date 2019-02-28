
function RamaddaChat(entryId, chatId) {
    var ID_INPUT_TOOLBAR = "inputtoolbar";
    var ID_MENU = "menu";
    var ID_GUTTER = "gutter";
    var ID_OUTPUT = "output";
    var ID_INPUT= "input";
    var ID_INPUT_CONTAINER= "inputcontainer";
    var ID_TOGGLE = "inputtoggle";

    $.extend(this,{
            messageCnt:0,
            entryId: entryId,
                chatId: chatId
                });

    this.chat = $("#"+this.chatId);

    $.extend(this, {
            initChat: function() {
                let _this = this;
                this.handlerId = addHandler(this);
                addHandler(this,this.handlerId+"_entryid");
                addHandler(this,this.handlerId+"_wikilink");
                this.editToolbar = "";
                var html  = 
                    "<table border=0 width=100% cellspacing=0 cellpadding=0><tr valign=top><td rowspan=2 width=18>" +
                    HtmlUtils.div(["id",this.getDomId(ID_GUTTER),"class","ramadda-chat-gutter"],"GG") +
                    "</td><td>" +
                    HtmlUtils.div(["id",this.getDomId(ID_INPUT_CONTAINER),"class","ramadda-chat-input-container"],"") +
                    "</td></tr><tr valign=top><td>" +
                    HtmlUtils.div(["id",this.getDomId(ID_OUTPUT),"class","ramadda-chat-output"],"")+
                    "</td></tr></table>";

                this.chat.html(html);

                this.gutter  = this.jq(ID_GUTTER);
                this.inputContainer  = this.jq(ID_INPUT_CONTAINER);
                this.output  = this.jq(ID_OUTPUT);
                this.showTextArea  = false;
                this.toggleInput();
                var url = ramaddaBaseUrl +"/wikitoolbar?entryid=" + this.entryId +"&handler=" + this.handlerId;
                GuiUtils.loadHtml(url,  h=> {
                        //                        console.log(h);
                        this.editToolbar =h; 
                        this.jq(ID_INPUT_TOOLBAR).html(h);
                    });
                this.connect();
                this.receive();
            },
            jq: function(id) {
                return  $("#"+this.getDomId(id));
            },
            getDomId: function(id) {
                return this.chatId+"_"+id;
            },
           selectClick(type,id, entryId, value) {
                 if(type == "entryid") {
                     this.insertText(entryId);
                 } else {
                     this.insertText("[[" + entryId +"|" + value+"]]");
                 }
                 this.input.focus();
            },
            insertTags: function(tagOpen, tagClose, sampleText) {
                var id = this.getDomId(ID_INPUT);
                var textComp = GuiUtils.getDomObject(id);
                insertTagsInner(id, textComp.obj, tagOpen, tagClose, sampleText);
            },
            insertText: function(value) {
                var id = this.getDomId(ID_INPUT);
                var textComp = GuiUtils.getDomObject(id);
                if (textComp || editor) {
                    insertAtCursor(id, textComp.obj, value);
                }
            },
            toggleInput: function() {
               let _this = this;
                this.showTextArea  = !this.showTextArea;
                var id = this.getDomId(ID_INPUT);
                var gutter = HtmlUtils.image(ramaddaBaseUrl+(this.showTextArea?"/icons/chevron.png":"/icons/chevron-expand.png"),
                                             ["id",this.getDomId(ID_TOGGLE),"title","toggle input"]);
                
                gutter +="\n";
                gutter+="<br>" +  HtmlUtils.image(ramaddaBaseUrl+"/chat/mail-send.png",["class","ramadda-chat-menuitem","title","Send","data-command","send"]);
                gutter +="\n";
                gutter+="<br>" +  HtmlUtils.image(ramaddaBaseUrl+"/icons/eraser.png",["class","ramadda-chat-menuitem","title","Clear","data-command","clear"]);

                if(this.showTextArea) {
                    var text = this.input?this.input.val():"";
                    var inputToolbar = HtmlUtils.div(["id",this.getDomId(ID_INPUT_TOOLBAR)],this.editToolbar);
                    var input =  inputToolbar + HtmlUtils.textarea("input", text||"", ["placeholder","shift-return to send","rows", "8", ATTR_CLASS, "ramadda-chat-input", ATTR_ID, id]);
                    this.inputContainer.html(input);
                    this.input  = $("#"+id);
                    this.input.focus();
                    this.input.keypress(event => {
                            if (event.shiftKey && event.which == 13) {
                                this.send(this.input.val());
                                event.preventDefault();
                                //                                this.input.val("");
                                //                                this.input.focus();
                            }});

                } else {
                    var input = HtmlUtils.input("chatinput","",["placeholder","chat text","id",id,"class","ramadda-chat-input"]);
                    this.inputContainer.html(input);
                    this.input  = $("#"+id);
                    this.input.focus();
                    this.input.keypress(event => {
                            if (event.which == 13) {
                                this.send(this.input.val());
                                this.input.attr("placeholder","");
                                this.input.val("");
                            }});
                }
                this.gutter.html(gutter);
                this.gutter.find(".ramadda-chat-menuitem").click(function() {
                        //For some reason I'm not getting the command here
                        var command = $(this).attr("data-commmand");
                        var title = $(this).attr("title");
                        if(title == "Clear") {
                            _this.output.html("");
                        } else  if(title == "Send") {
                            _this.send(_this.input.val());
                        }
                    });
                this.jq(ID_TOGGLE).click(()=>this.toggleInput());
            },
           processMessages: function(data) {
               if(data.code!="ok") {
                   this.writeError("Error:" + data.message);
               } else {
                   for(var i=0;i<data.messages.length;i++) {
                       var message = data.messages[i];
                       this.write(message.message, message.user);
                   }
               }
           },
           receive: function(wait) {
                if(!Utils.isDefined(wait)) wait = 0;
                let _this = this;
                var url = ramaddaBaseUrl +"/chat/output?entryid=" + this.entryId;
                this.connected = true;
                var jqxhr = $.getJSON(url,  data=> {
                        this.processMessages(data);
                        this.receive();
                    }).fail(() => {
                            _this.connected = false;
                            wait= wait+1000;
                            if(wait<30*1000) {
                                setTimeout(()=>{_this.receive(wait);},wait);
                            } else {
                                this.writeError("Error: connection to server broke");
                            }
                        });

            },
            connect: function() {
                var json = {
                    command:"connect",
                }
                msg  =encodeURIComponent(JSON.stringify(json));
                var url = ramaddaBaseUrl +"/chat/input?entryid=" + this.entryId+"&input=" + msg;
                var jqxhr = $.getJSON(url,  data=> {
                        this.processMessages(data);
                    });

           },
            send: function(msg) {
                if(!this.connected) {
                    this.writeError("Not connected to server");
                    return;
                }
                if(msg == "") msg = ":br";
                var json = {
                    command:"message",
                    message:msg
                }
                msg  =encodeURIComponent(JSON.stringify(json));
                var url = ramaddaBaseUrl +"/chat/input?entryid=" + this.entryId+"&input=" + msg;
                var jqxhr = $.getJSON(url,  data=> {
                        //                        console.log("send result:" + JSON.stringify(data));
                        if(data.code!="ok") {
                            this.writeError("Error:" + data.message);
                        } 
                    });
            },
            writeError:function(msg) {
                 this.write(HtmlUtils.div(["class","ramadda-chat-message ramadda-chat-message-error"], msg),"chat");
            },
            write:function(out, user) {
                if(!user) user = "";
                this.messageCnt++;
                var id = this.getDomId(this.messageCnt);
                var msg = "<table cellpadding=0 cellspacing=0 width=100%><tr valign=top><td width=150px style='border-right:1px #ccc solid;' align=left>" +HtmlUtils.div(["class","ramadda-chat-user"], user) +
                    "</td><td align=left>" +
                    HtmlUtils.div(["class","ramadda-chat-message"], out) +
                    "</td></tr></table>";
                this.output.append(HtmlUtils.div(["id",id,"class","ramadda-chat-message-container"], msg));
                Utils.initContent("#" +id);
                this.output.scrollTop(this.output.prop("scrollHeight"));
                
            }
        });


    this.initChat();

}