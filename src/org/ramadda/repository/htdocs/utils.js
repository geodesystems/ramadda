/**
 * Copyright (c) 2008-2015 Geode Systems LLC
*/

var ramaddaBaseUrl = "${urlroot}";
var ramaddaBaseEntry = "${baseentry}";
var root = ramaddaBaseUrl;
var urlroot = ramaddaBaseUrl;
var icon_close = ramaddaBaseUrl +"/icons/close.gif";
var icon_rightarrow = ramaddaBaseUrl +"/icons/grayrightarrow.gif";
var icon_downdart =ramaddaBaseUrl +"/icons/downdart.gif";
var icon_rightdart =ramaddaBaseUrl +"/icons/rightdart.gif";
var icon_downdart =ramaddaBaseUrl +"/icons/application_side_contract.png";
var icon_rightdart =ramaddaBaseUrl +"/icons/application_side_expand.png";
var icon_progress = ramaddaBaseUrl +"/icons/progress.gif";
var icon_information = ramaddaBaseUrl +"/icons/information.png";
var icon_folderclosed = ramaddaBaseUrl +"/icons/folderclosed.png";
var icon_folderopen = ramaddaBaseUrl +"/icons/togglearrowdown.gif";

var icon_tree_open = ramaddaBaseUrl +"/icons/togglearrowdown.gif";
var icon_tree_closed = ramaddaBaseUrl +"/icons/togglearrowright.gif";


var icon_menuarrow = ramaddaBaseUrl +"/icons/downdart.gif";
var icon_blank = ramaddaBaseUrl +"/icons/blank.gif";
var uniqueCnt = 0;

function noop() {}

var Utils = {
    parseDate: function(s,roundUp, rel) {
        if(s ==null) return null;
        s = s.trim();
        if(s == "") return null;
        var regexpStr = "(rel|now|today)(\\+|-)(.*)"
        var regex = new RegExp(regexpStr , 'i');
        var match = s.match(regex);
        var offset = 0;

        if(match!=null) {
            offset = parseFloat(match[3]);
            if(match[2] == "-") offset = -offset;
            s = match[1];
        }
        var date = null;
        if(s == "now") {
        } else if(s == "rel") {
            if(rel == null) return null;
            date  = new Date(rel.getTime());
        } else if(s == "today") {
            date = new Date(Date.now());
            date.setMilliseconds(0);
            date.setSeconds(0);
            date.setMinutes(0);
            if(roundUp) 
                date.setHours(24);
        }  else {
            date = new Date(Date.parse(s));
        }
        if(offset!=0) {
            date.setDate(date.getDate()+offset);
        }
        return date;
    },
    isDefined: function(v) {
        return  !(typeof v=== 'undefined');
    },
    stringDefined: function(v) {
        if(!Utils.isDefined(v)) return false;
        if(v == null || v =="") return false;
        return true;
    },
    formatNumber: function(number) {
        if(number>1000) {
            return number_format(number,0);
        } else if (number>100) {
            return number_format(number,1);
        } else if (number>10) {
            return number_format(number,2);
        } else if (number>1) {
            return number_format(number,3);
        } 
        return number;
    },
    isNumber: function(value) {
        if ((undefined === value) || (null === value)) {
            return false;
        }
        if (typeof value == 'number') {
            return true;
        }
        return !isNaN(value - 0);
    },
    isType: function(value,typeName) {
        if(value && (typeof value == typeName)) {
            return true;
        }
        return false;
    },
    getDefined: function(v1, v2) {
        if(Utils.isDefined(v1)) return v1;
        return v2;
    },
    cleanId: function(id) {
        id = id.replace(/:/g,"_").replace(/\./g,"_").replace(/=/g,"_").replace(/\//g,"_");
        return id;
    },
    isMobile: function() {
        return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
    },
    isFalse: function(value) {
        if (!(typeof value == 'undefined')) {
            return value == false;
        }
        return false;
    },
    toFloat:function(s, dflt) {
        if(s == null || s.trim() == "") {
            if (!(typeof dflt == 'undefined')) {
                return dflt;
            }
            return NaN;
        }
        return parseFloat(s);
    },
    getUrlArgs: function(qs) {
        //http://stackoverflow.com/questions/979975/how-to-get-the-value-from-the-url-parameter
        qs = qs.split('+').join(' ');
        var params = {},
        tokens,
        re = /[?&]?([^=]+)=([^&]*)/g;

        while (tokens = re.exec(qs)) {
            params[decodeURIComponent(tokens[1])] = decodeURIComponent(tokens[2]);
        }
        return params;
    },
    initPage: function() {
        //Buttonize
        $(':submit').button().click(function(event){});
        //menuize
        /*
        $(".ramadda-pulldown").selectBoxIt({});
        */
        /* for select menus with icons */
        $(".ramadda-pulldown-with-icons").iconselectmenu().iconselectmenu("menuWidget").addClass( "ui-menu-icons ramadda-select-icon");
    },
    ColorTables: {blues: ["rgb(255,255,255)","rgb(246,246,255)","rgb(237,237,255)","rgb(228,228,255)","rgb(219,219,255)","rgb(211,211,255)","rgb(202,202,255)","rgb(193,193,255)","rgb(184,184,255)","rgb(175,175,255)","rgb(167,167,255)","rgb(158,158,255)","rgb(149,149,255)","rgb(140,140,255)","rgb(131,131,255)","rgb(123,123,255)","rgb(114,114,255)","rgb(105,105,255)","rgb(96,96,255)","rgb(87,87,255)","rgb(79,79,255)","rgb(70,70,255)","rgb(61,61,255)","rgb(52,52,255)","rgb(43,43,255)","rgb(35,35,255)","rgb(26,26,255)","rgb(17,17,255)","rgb(8,8,255)","rgb(0,0,255)",],
                  blue_green_red:["rgb(0,0,255)",
"rgb(8,17,246)",
"rgb(17,35,237)",
"rgb(26,52,228)",
"rgb(35,70,219)",
"rgb(43,87,211)",
"rgb(52,105,202)",
"rgb(61,123,193)",
"rgb(70,140,184)",
"rgb(79,158,175)",
"rgb(87,175,167)",
"rgb(96,193,158)",
"rgb(105,211,149)",
"rgb(114,228,140)",
"rgb(123,246,131)",
"rgb(131,246,123)",
"rgb(140,228,114)",
"rgb(149,211,105)",
"rgb(158,193,96)",
"rgb(167,175,87)",
"rgb(175,158,79)",
"rgb(184,140,70)",
"rgb(193,123,61)",
"rgb(202,105,52)",
"rgb(211,87,43)",
"rgb(219,70,35)",
"rgb(228,52,26)",
"rgb(237,35,17)",
"rgb(246,17,8)",
"rgb(255,0,0)",
                                  ],
                  white_blue:["rgb(244,252,254)","rgb(101,239,255)","rgb(50,227,255)","rgb(0,169,204)","rgb(0,122,153)"],
                  blue_red:["rgb(26,12,234)","rgb(26,12,234)","rgb(26,12,234)","rgb(234,12,48)","rgb(234,12,48)","rgb(234,12,48)"],
                  red_white_blue:["rgb(254,9,9)","rgb(254,11,11)","rgb(254,13,13)","rgb(254,15,15)","rgb(254,17,17)","rgb(254,19,19)","rgb(254,21,21)","rgb(254,23,23)","rgb(254,25,25)","rgb(254,27,27)","rgb(254,29,29)","rgb(254,31,31)","rgb(254,33,33)","rgb(254,35,35)","rgb(254,37,37)","rgb(254,39,39)","rgb(254,41,41)","rgb(254,43,43)","rgb(254,45,45)","rgb(254,47,47)","rgb(254,49,49)","rgb(254,51,51)","rgb(254,53,53)","rgb(254,55,55)","rgb(254,56,57)","rgb(254,58,59)","rgb(254,60,61)","rgb(254,62,63)","rgb(254,64,65)","rgb(254,66,67)","rgb(254,68,69)","rgb(254,70,71)","rgb(254,72,73)","rgb(254,74,75)","rgb(254,76,77)","rgb(254,78,79)","rgb(254,80,81)","rgb(254,82,83)","rgb(254,84,85)","rgb(254,86,87)","rgb(254,88,89)","rgb(253,90,91)","rgb(253,92,93)","rgb(253,94,95)","rgb(253,96,97)","rgb(253,98,99)","rgb(253,100,101)","rgb(253,102,103)","rgb(253,104,105)","rgb(253,105,107)","rgb(253,107,109)","rgb(253,109,111)","rgb(253,111,113)","rgb(253,113,115)","rgb(253,115,117)","rgb(253,117,119)","rgb(253,119,121)","rgb(253,121,123)","rgb(253,123,125)","rgb(253,125,127)","rgb(253,127,129)","rgb(253,129,131)","rgb(253,131,132)","rgb(253,133,134)","rgb(253,135,136)","rgb(253,137,138)","rgb(253,139,140)","rgb(253,141,142)","rgb(253,143,144)","rgb(253,145,146)","rgb(253,147,148)","rgb(253,149,150)","rgb(253,151,152)","rgb(253,153,154)","rgb(253,154,156)","rgb(253,156,158)","rgb(253,158,160)","rgb(253,160,162)","rgb(253,162,164)","rgb(253,164,166)","rgb(253,166,168)","rgb(253,168,170)","rgb(253,170,172)","rgb(252,172,174)","rgb(252,174,176)","rgb(252,176,178)","rgb(252,178,180)","rgb(252,180,182)","rgb(252,182,184)","rgb(252,184,186)","rgb(252,186,188)","rgb(252,188,190)","rgb(252,190,192)","rgb(252,192,194)","rgb(252,194,196)","rgb(252,196,198)","rgb(252,198,200)","rgb(252,200,202)","rgb(252,202,204)","rgb(252,203,206)","rgb(252,205,208)","rgb(252,207,210)","rgb(252,209,212)","rgb(252,211,214)","rgb(252,213,216)","rgb(252,215,218)","rgb(252,217,220)","rgb(252,219,222)","rgb(252,221,224)","rgb(252,223,226)","rgb(252,225,228)","rgb(252,227,230)","rgb(252,229,232)","rgb(252,231,234)","rgb(252,233,236)","rgb(252,235,238)","rgb(252,237,240)","rgb(252,239,242)","rgb(252,241,244)","rgb(252,243,246)","rgb(252,245,248)","rgb(252,247,250)","rgb(252,249,252)","rgb(252,251,254)","rgb(252,252,255)","rgb(250,250,255)","rgb(248,248,255)","rgb(246,246,255)","rgb(244,244,255)","rgb(242,242,255)","rgb(240,240,255)","rgb(238,238,255)","rgb(236,236,255)","rgb(234,234,255)","rgb(232,232,255)","rgb(230,230,255)","rgb(228,228,255)","rgb(226,226,255)","rgb(224,224,255)","rgb(222,222,255)","rgb(220,220,255)","rgb(218,218,255)","rgb(216,216,255)","rgb(214,214,255)","rgb(212,212,255)","rgb(210,210,255)","rgb(208,208,255)","rgb(206,206,255)","rgb(204,204,255)","rgb(202,202,255)","rgb(200,200,255)","rgb(198,198,255)","rgb(196,196,255)","rgb(194,194,255)","rgb(192,192,255)","rgb(190,190,255)","rgb(188,188,255)","rgb(186,186,255)","rgb(184,184,255)","rgb(182,182,255)","rgb(180,180,255)","rgb(178,178,255)","rgb(176,176,255)","rgb(174,174,255)","rgb(172,172,255)","rgb(170,170,255)","rgb(168,168,255)","rgb(166,166,255)","rgb(164,164,255)","rgb(162,162,255)","rgb(160,160,255)","rgb(158,158,255)","rgb(156,156,255)","rgb(154,154,255)","rgb(152,152,255)","rgb(150,150,255)","rgb(148,148,255)","rgb(146,146,255)","rgb(144,144,255)","rgb(142,142,255)","rgb(140,140,255)","rgb(138,138,255)","rgb(136,136,255)","rgb(134,134,255)","rgb(132,132,255)","rgb(130,130,255)","rgb(128,128,255)","rgb(126,126,255)","rgb(124,124,255)","rgb(122,122,255)","rgb(120,120,255)","rgb(118,118,255)","rgb(116,116,255)","rgb(114,114,255)","rgb(112,112,255)","rgb(110,110,255)","rgb(108,108,255)","rgb(106,106,255)","rgb(104,104,255)","rgb(102,102,255)","rgb(100,100,255)","rgb(98,98,255)","rgb(96,96,255)","rgb(94,94,255)","rgb(92,92,255)","rgb(90,90,255)","rgb(88,88,255)","rgb(86,86,255)","rgb(84,84,255)","rgb(82,82,255)","rgb(80,80,255)","rgb(78,78,255)","rgb(76,76,255)","rgb(74,74,255)","rgb(72,72,255)","rgb(70,70,255)","rgb(68,68,255)","rgb(66,66,255)","rgb(64,64,255)","rgb(62,62,255)","rgb(60,60,255)","rgb(58,58,255)","rgb(56,56,255)","rgb(54,54,255)","rgb(52,52,255)","rgb(50,50,255)","rgb(48,48,255)","rgb(46,46,255)","rgb(44,44,255)","rgb(42,42,255)","rgb(40,40,255)","rgb(38,38,255)","rgb(36,36,255)","rgb(34,34,255)","rgb(32,32,255)","rgb(30,30,255)","rgb(28,28,255)","rgb(26,26,255)","rgb(24,24,255)","rgb(22,22,255)","rgb(20,20,255)","rgb(18,18,255)","rgb(16,16,255)","rgb(14,14,255)","rgb(12,12,255)","rgb(10,10,255)","rgb(8,8,255)","rgb(6,6,255)","rgb(4,4,255)","rgb(2,2,255)","rgb(0,0,255)"],
                  blue_white_red: ["rgb(0,0,255)","rgb(2,2,255)","rgb(4,4,255)","rgb(6,6,255)","rgb(8,8,255)","rgb(10,10,255)","rgb(12,12,255)","rgb(14,14,255)","rgb(16,16,255)","rgb(18,18,255)","rgb(20,20,255)","rgb(22,22,255)","rgb(24,24,255)","rgb(26,26,255)","rgb(28,28,255)","rgb(30,30,255)","rgb(32,32,255)","rgb(34,34,255)","rgb(36,36,255)","rgb(38,38,255)","rgb(40,40,255)","rgb(42,42,255)","rgb(44,44,255)","rgb(46,46,255)","rgb(48,48,255)","rgb(50,50,255)","rgb(52,52,255)","rgb(54,54,255)","rgb(56,56,255)","rgb(58,58,255)","rgb(60,60,255)","rgb(62,62,255)","rgb(64,64,255)","rgb(66,66,255)","rgb(68,68,255)","rgb(70,70,255)","rgb(72,72,255)","rgb(74,74,255)","rgb(76,76,255)","rgb(78,78,255)","rgb(80,80,255)","rgb(82,82,255)","rgb(84,84,255)","rgb(86,86,255)","rgb(88,88,255)","rgb(90,90,255)","rgb(92,92,255)","rgb(94,94,255)","rgb(96,96,255)","rgb(98,98,255)","rgb(100,100,255)","rgb(102,102,255)","rgb(104,104,255)","rgb(106,106,255)","rgb(108,108,255)","rgb(110,110,255)","rgb(112,112,255)","rgb(114,114,255)","rgb(116,116,255)","rgb(118,118,255)","rgb(120,120,255)","rgb(122,122,255)","rgb(124,124,255)","rgb(126,126,255)","rgb(128,128,255)","rgb(130,130,255)","rgb(132,132,255)","rgb(134,134,255)","rgb(136,136,255)","rgb(138,138,255)","rgb(140,140,255)","rgb(142,142,255)","rgb(144,144,255)","rgb(146,146,255)","rgb(148,148,255)","rgb(150,150,255)","rgb(152,152,255)","rgb(154,154,255)","rgb(156,156,255)","rgb(158,158,255)","rgb(160,160,255)","rgb(162,162,255)","rgb(164,164,255)","rgb(166,166,255)","rgb(168,168,255)","rgb(170,170,255)","rgb(172,172,255)","rgb(174,174,255)","rgb(176,176,255)","rgb(178,178,255)","rgb(180,180,255)","rgb(182,182,255)","rgb(184,184,255)","rgb(186,186,255)","rgb(188,188,255)","rgb(190,190,255)","rgb(192,192,255)","rgb(194,194,255)","rgb(196,196,255)","rgb(198,198,255)","rgb(200,200,255)","rgb(202,202,255)","rgb(204,204,255)","rgb(206,206,255)","rgb(208,208,255)","rgb(210,210,255)","rgb(212,212,255)","rgb(214,214,255)","rgb(216,216,255)","rgb(218,218,255)","rgb(220,220,255)","rgb(222,222,255)","rgb(224,224,255)","rgb(226,226,255)","rgb(228,228,255)","rgb(230,230,255)","rgb(232,232,255)","rgb(234,234,255)","rgb(236,236,255)","rgb(238,238,255)","rgb(240,240,255)","rgb(242,242,255)","rgb(244,244,255)","rgb(246,246,255)","rgb(248,248,255)","rgb(250,250,255)","rgb(252,252,255)","rgb(252,251,254)","rgb(252,249,252)","rgb(252,247,250)","rgb(252,245,248)","rgb(252,243,246)","rgb(252,241,244)","rgb(252,239,242)","rgb(252,237,240)","rgb(252,235,238)","rgb(252,233,236)","rgb(252,231,234)","rgb(252,229,232)","rgb(252,227,230)","rgb(252,225,228)","rgb(252,223,226)","rgb(252,221,224)","rgb(252,219,222)","rgb(252,217,220)","rgb(252,215,218)","rgb(252,213,216)","rgb(252,211,214)","rgb(252,209,212)","rgb(252,207,210)","rgb(252,205,208)","rgb(252,203,206)","rgb(252,202,204)","rgb(252,200,202)","rgb(252,198,200)","rgb(252,196,198)","rgb(252,194,196)","rgb(252,192,194)","rgb(252,190,192)","rgb(252,188,190)","rgb(252,186,188)","rgb(252,184,186)","rgb(252,182,184)","rgb(252,180,182)","rgb(252,178,180)","rgb(252,176,178)","rgb(252,174,176)","rgb(252,172,174)","rgb(253,170,172)","rgb(253,168,170)","rgb(253,166,168)","rgb(253,164,166)","rgb(253,162,164)","rgb(253,160,162)","rgb(253,158,160)","rgb(253,156,158)","rgb(253,154,156)","rgb(253,153,154)","rgb(253,151,152)","rgb(253,149,150)","rgb(253,147,148)","rgb(253,145,146)","rgb(253,143,144)","rgb(253,141,142)","rgb(253,139,140)","rgb(253,137,138)","rgb(253,135,136)","rgb(253,133,134)","rgb(253,131,132)","rgb(253,129,131)","rgb(253,127,129)","rgb(253,125,127)","rgb(253,123,125)","rgb(253,121,123)","rgb(253,119,121)","rgb(253,117,119)","rgb(253,115,117)","rgb(253,113,115)","rgb(253,111,113)","rgb(253,109,111)","rgb(253,107,109)","rgb(253,105,107)","rgb(253,104,105)","rgb(253,102,103)","rgb(253,100,101)","rgb(253,98,99)","rgb(253,96,97)","rgb(253,94,95)","rgb(253,92,93)","rgb(253,90,91)","rgb(254,88,89)","rgb(254,86,87)","rgb(254,84,85)","rgb(254,82,83)","rgb(254,80,81)","rgb(254,78,79)","rgb(254,76,77)","rgb(254,74,75)","rgb(254,72,73)","rgb(254,70,71)","rgb(254,68,69)","rgb(254,66,67)","rgb(254,64,65)","rgb(254,62,63)","rgb(254,60,61)","rgb(254,58,59)","rgb(254,56,57)","rgb(254,55,55)","rgb(254,53,53)","rgb(254,51,51)","rgb(254,49,49)","rgb(254,47,47)","rgb(254,45,45)","rgb(254,43,43)","rgb(254,41,41)","rgb(254,39,39)","rgb(254,37,37)","rgb(254,35,35)","rgb(254,33,33)","rgb(254,31,31)","rgb(254,29,29)","rgb(254,27,27)","rgb(254,25,25)","rgb(254,23,23)","rgb(254,21,21)","rgb(254,19,19)","rgb(254,17,17)","rgb(254,15,15)","rgb(254,13,13)","rgb(254,11,11)","rgb(254,9,9)"],
                  grayscale:["#000","#111","#222","#333","#444","#555","#666","#777","#888","#999","#aaa","#bbb","#ccc","#ddd","#eee","#fff"],
                  inversegrayscale: ["#fff","#eee","#ddd","#ccc","#bbb","#aaa","#999","#888","#777","#666","#555","#444","#333","#222","#111","#000"]},
    rainbow: ["red","orange","yellow","green","blue","indigo","violet"],
    


};

var GuiUtils = {
    getProxyUrl: function(url) {
        var base = ramaddaBaseUrl + "/proxy?trim=true&url=";
        return base + encodeURIComponent(url);
    },

    showingError: false,
    pageUnloading: false,
    handleError: function(error, extra, showAlert) {
        if(this.pageUnloading) {
            return;
        }
        console.log(error);
        if(extra) {
            console.log(extra);
        }

        if(this.showingError) {
            return;
        }
        if(showAlert) {
            this.showingError = true;
            //            alert(error);
            this.showingError = false;
        }
        closeFormLoadingDialog ();
    },
    isJsonError: function(data) {
        if(data == null) {
            this.handleError("Null JSON data", null, false);
            return true;
        }
        if(data.error!=null) {
            var code = data.errorcode;
            if(code == null) code = "error";
            this.handleError("Error in Utils.isJsonError:" + data.error, null, false);
            return true;
        }
        return false;
    },
    loadXML: function (url, callback,arg) {
        var req = false;
        if(window.XMLHttpRequest) {
            try {
                req = new XMLHttpRequest();
            } catch(e) {
                req = false;
            }
        } else if(window.ActiveXObject)  {
            try {
                req = new ActiveXObject("Msxml2.XMLHTTP");
            } catch(e) {
                try {
                    req = new ActiveXObject("Microsoft.XMLHTTP");
                } catch(e) {
                    req = false;
                }
            }
        }
        if(req) {
            req.onreadystatechange = function () { 
                if (req.readyState == 4 && req.status == 200)   {
                    callback(req,arg); 
                }
            };
            req.open("GET", url, true);
            req.send("");
        }
    },
    loadHtml: function (url, callback) {
        var jqxhr = $.get(url, function(data) {
                alert( "success" );
                console.log(data);
            })
        .done(function() {})
        .fail(function() {
                console.log("Failed to load url: "+ url);
            });
    },
    loadUrl: function (url, callback,arg) {
        var req = false;
        if(window.XMLHttpRequest) {
            try {
                req = new XMLHttpRequest();
            } catch(e) {
                req = false;
            }
        } else if(window.ActiveXObject)  {
            try {
                req = new ActiveXObject("Msxml2.XMLHTTP");
            } catch(e) {
                try {
                    req = new ActiveXObject("Microsoft.XMLHTTP");
                } catch(e) {
                    req = false;
                }
            }
        }
        if(req) {
            req.onreadystatechange = function () { 
                if (req.readyState == 4 && req.status == 200)   {
                    callback(req,arg); 
                }
            };
            req.open("GET", url, true);
            req.send("");
        }
    },
    getUrlArg: function( name, dflt ) {
        name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
        var regexS = "[\\?&]"+name+"=([^&#]*)";
        var regex = new RegExp( regexS );
        var results = regex.exec( window.location.href );
        if( results == null || results=="" )
            return dflt;
        else
            return results[1];
    },
    setCursor: function(c) {
        var cursor = document.cursor;
        if(!cursor && document.getElementById) {
            cursor =  document.getElementById('cursor');
        }
        if(!cursor) {
            document.body.style.cursor = c;
        }
    },
    getDomObject : function(name) {
        obj = new DomObject(name);
        if(obj.obj) return obj;
        return null;
    },
    getEvent: function (event) {
        if(event) return event;
        return window.event;
    },
    getEventX:    function (event) {
        if (event.pageX) {
            return  event.pageX;
        }
        return  event.clientX + document.body.scrollLeft
        + document.documentElement.scrollLeft;
    },
    getEventY :function (event) {
        if (event.pageY) {
            return  event.pageY;
        }
        return  event.clientY + document.body.scrollTop
        + document.documentElement.scrollTop;

    },

    getTop : function (obj) {
        if(!obj) return 0;
        return obj.offsetTop+this.getTop(obj.offsetParent);
    },


    getBottom : function (obj) {
        if(!obj) return 0;
        return this.getTop(obj) + obj.offsetHeight;
    },


    setPosition : function(obj,x,y) {
        obj.style.top = y;
        obj.style.left = x;
    },

    getLeft :  function(obj) {
        if(!obj) return 0;
        return obj.offsetLeft+this.getLeft(obj.offsetParent);
    },
    getRight :  function(obj) {
        if(!obj) return 0;
        return obj.offsetRight+this.getRight(obj.offsetParent);
    },

    getStyle : function(obj) {
        if(obj.style) return obj.style;
        if (document.layers)  { 		
            return   document.layers[obj.name];
        }        
        return null;
    },
    //from http://snipplr.com/view.php?codeview&id=5949
    size_format: function (filesize) {
        if (filesize >= 1073741824) {
            filesize = number_format(filesize / 1073741824, 2, '.', '') + ' Gb';
        } else { 
            if (filesize >= 1048576) {
                filesize = number_format(filesize / 1048576, 2, '.', '') + ' Mb';
            } else { 
                if (filesize >= 1024) {
                    filesize = number_format(filesize / 1024, 0) + ' Kb';
                } else {
                    filesize = number_format(filesize, 0) + ' bytes';
                };
            };
        };
        return filesize;
    },
    inputLengthOk: function(domId, length, doMin) {
        var value = $("#"+ domId).val();
        if(value == null) return true;
        if(doMin) {
            if(value.length<length) {
                closeFormLoadingDialog ();
                return false;
            }
        } else {
            if(value.length>length) {
                closeFormLoadingDialog ();
                return false;
            }
        }
        return true;
    },
    inputValueOk: function (domId, rangeValue, min) {
        var value = $("#"+ domId).val();
        if(value == null) return true;
        if(min && value<rangeValue) {
            closeFormLoadingDialog ();
            return false;
        }
        if(!min && value>rangeValue) {
            closeFormLoadingDialog ();
            return false;
        }
        return true;
    },
    inputIsRequired: function (domId, rangeValue, min) {
        var value = $("#"+ domId).val();
        if(value == null || value.trim().length==0) {
            closeFormLoadingDialog ();
            return false;
        }
        return true;
    }

    

};


var TAG_A = "a";
var TAG_B = "b";
var TAG_DIV = "div";
var TAG_IMG = "img";
var TAG_INPUT = "input";
var TAG_LI = "li";
var TAG_SELECT = "select";
var TAG_OPTION = "option";
var TAG_TABLE = "table";
var TAG_TR = "tr";
var TAG_TD = "td";
var TAG_UL= "ul";
var TAG_OL= "ol";

var ATTR_WIDTH = "width";
var ATTR_HREF = "href";
var ATTR_BORDER = "border";
var ATTR_VALUE = "value";
var ATTR_TITLE = "title";
var ATTR_ALT = "alt";
var ATTR_ID = "id";
var ATTR_CLASS = "class";
var ATTR_SIZE = "size";
var ATTR_STYLE = "style";
var ATTR_ALIGN = "align";
var ATTR_VALIGN = "valign";


var HtmlUtil =  {
    makeBreadcrumbs: function(id) {
        jQuery("#"+id).jBreadCrumb({
                previewWidth: 10, 
                easing:'swing',
                //(sic)
                    beginingElementsToLeaveOpen: 0,
                    endElementsToLeaveOpen: 4
            }
            );
    },
    tooltipInit: function(openerId,id) {
        $("#" + id).dialog({
                autoOpen: false,
                //                draggable: true,
                minWidth:650,
                /* use the default
                show: {
                    effect: "blind",
                    duration: 250
                },
                */
                hide: {
                    effect: "blind",
                    duration: 250
                }
            });

        $( "#" + openerId).click(function() {
                $( "#" + id).dialog( "open" );
            });
    },
    join : function (items,separator) {
        var html = "";
        for(var i =0;i<items.length;i++) {
            if(i>0 & separator!=null) html+= separator;
            html += items[i];
        }
        return html;
    },
    qt : function (value) {
        return "\"" + value +"\"";
    },
    sqt : function (value) {
        return "\'" + value +"\'";
    },
    getUniqueId: function() {
        return "id_" + (uniqueCnt++);
    },
    inset : function(html, top, left, bottom, right) {
        var attrs = [];
        var style = "";
        if(top) {
            style += "padding-top: "  + top + "px; ";
        }
        if(left) {
            style += "padding-left: "  + left + "px; ";
        }
        if(bottom) {
            style += "padding-bottom: "  + bottom + "px; ";
        }
        if(right) {
            style += "padding-right: "  + right + "px; ";
        }
        return HtmlUtil.div(["style", style], html);

    },
    makeAccordian: function(id, args) {
        if(!Utils.isDefined(args)) args = {};
        if(!Utils.isDefined(args.active)) args.active = 0;
        $(function() {
                //We initially hide the accordian contents
                //Show all contents
                var contents = $(".ramadda-accordian-contents");
                contents.css("display","block");
                var ctorArgs = {
                           autoHeight: false, 
                           navigation: true, 
                           collapsible: true, 
                           heightStyle: "content",
                           active: args.active
                       }
                $(id).accordion(ctorArgs);
            }
            );
    },
    hbox : function() {
        var row = HtmlUtil.openTag("tr",["valign","top"]);
        row += "<td>";
        row += HtmlUtil.join(arguments,"</td><td>");
        row += "</td></tr>";
        return this.tag("table",["border","0", "cellspacing","0","cellpadding","0"],
                        row);
    },
    leftRight : function(left,right,leftWeight, rightWeight) {
        if(leftWeight==null) leftWeight = "6";
        if(rightWeight==null) rightWeight = "6";
        return this.div(["class","row"],
                        this.div(["class", "col-md-" + leftWeight], left) +
                        this.div([ "class", "col-md-" + rightWeight,"style","text-align:right;"], right));
    },
    leftCenterRight : function(left,center, right,leftWidth, centerWidth, rightWidth) {
        if(leftWidth==null) leftWidth = "33%";
        if(centerWidth==null) centerWidth = "33%";
        if(rightWidth==null) rightWidth = "33%";

        return this.tag("table",["border","0", "width","100%","cellspacing","0","cellpadding","0"],
                        this.tr(["valign","top"],
                                this.td(["align","center","width",leftWidth],left) +
                                this.td(["align","center","width",centerWidth],center) +
                                this.td(["align","center","width", rightWidth],right)));
    },

    div : function(attrs, inner) {
        return this.tag("div", attrs, inner);
    },
    center : function(inner) {
        return this.tag("center", attrs, inner);
    },
    span : function(attrs, inner) {
        return this.tag("span", attrs, inner);
    },
    image : function(path, attrs) {
        return  "<img " + this.attrs(["src", path,"border","0"]) +" " + this.attrs(attrs) +">";
    },
    tr : function(attrs, inner) {
        return this.tag("tr", attrs, inner);
    },
    td : function(attrs, inner) {
        return this.tag("td", attrs, inner);
    },
    tds : function(attrs, cols) {
        var html = "";
        for(var i=0;i<cols.length;i++) {
            html+= this.td(attrs,cols[i]);
        }
        return html;
    },
    th : function(attrs, inner) {
        return this.tag("th", attrs, inner);
    },
    setFormValue: function(id,val) {
        $("#"+ id).val(val);
    },
    setHtml: function(id,val) {
        $("#"+ id).html(val);
    }, 
   formTable : function() {
        return  this.openTag("table",["class","formtable","cellspacing","0","cellspacing","0"]);
    },
    formEntryTop : function(label, value) {
        return this.tag("tr", ["valign","top"],
                        this.tag("td",["class","formlabel","align","right"],
                                 label) +
                        this.tag("td",[],
                                 value));

    },
    formEntry : function(label, value) {
        return this.tag("tr", [],
                        this.tag("td",["class","formlabel","align","right"],
                                 label) +
                        this.tag("td",[],
                                 value));

    },
    appendArg: function(url,arg,value) {
        if(url.indexOf("?")<0) {
            url += "?";
        } else {
            url += "&";
        }
        url += HtmlUtil.urlArg(arg,value);
        return url;
    },
    getUrl: function(path,args) {
        if(args.length>0) {
            path +="?";
        }
        for(var i=0;i<args.length;i+=2) {
            if(i>0) {
                path +="&";
            }
            path += this.urlArg(args[i], args[i+1]);
        }
        return path;
    },
    b : function(inner) {
        return this.tag("b", [], inner);
    },

    td : function(attrs, inner) {
        return this.tag("td", attrs, inner);
    },

    tag : function(tagName, attrs, inner) {
        var html = "<" + tagName +" " + this.attrs(attrs) +">";
        if(inner!=null) {
            html += inner;
        }
        html += "</" + tagName +">";
        return html;
    },
    openTag : function(tagName, attrs) {
        var html = "<" + tagName +" " + this.attrs(attrs) +">";
        return html;
    },
    openRow : function() {
        return this.openTag("div",["class","row"]);
    },
    closeRow : function() {
        return this.closeTag("div");
    },

    openDiv : function(attrs) {
        return this.openTag("div",attrs);
    },
    closeDiv : function() {
        return this.closeTag("div");
    },

    closeTag : function(tagName) {
        return  "</" + tagName +">\n";
    },
    urlArg: function(name, value) {
        return name  + "=" + encodeURIComponent(value);
    },
    attr : function(name, value) {
        return " " + name +"=" + this.qt(value) +" ";
    },

    attrs : function(list) {
        var html = "";
        if(list == null) return html;
        for(var i=0;i<list.length;i+=2) {
            var name = list[i];
            var value = list[i+1];
            if(value == null) {
                html += name;
            } else {
                html += this.attr(name,value);
            }

        }
        return html;
    },
    styleAttr : function(s) {
        return this.attr("style", s);
    },

    classAttr : function(s) {
        return this.attr("class", s);
    },

    idAttr : function(s) {
        return this.attr("id", s);
    },
    href: function(url, label, attrs) {
        if (attrs == null) attrs = [];
        attrs.push("href");
        attrs.push(url);
        if(!Utils.isDefined(label) || label == "") label = url;
        return this.tag("a", attrs, label);
    },

    onClick : function(call, html, attrs) {
        var myAttrs = ["onclick", call, "style","color:black;   cursor:pointer;"];
        if(attrs!=null) {
            for(var i=0;i<attrs.length;i++) {
                myAttrs.push(attrs[i]);
            }
        }
        return this.tag("a", myAttrs, html);
    },

    checkbox:  function(id,attrs,checked) {
        attrs.push("id");
        attrs.push(id);
        attrs.push("type");
        attrs.push("checkbox");
        attrs.push("value");
        attrs.push("true");
        if(checked) {
            attrs.push("checked");
            attrs.push(null);
        }
        return this.tag("input", attrs);
    },

    radio:  function(id, name, radioclass, value, checked) {
        var html = "<input id=\"" + id +"\"  class=\"" + radioclass +"\" name=\"" + name +"\" type=radio value=\"" + value +"\" ";
        if(checked) {
            html+= " checked ";
        }
        html += "/>";
        return html;
    },


    handleFormChangeShowUrl: function(formId, outputId, skip) {
        if(skip == null) {
            skip = [".*OpenLayers_Control.*","authtoken"];
        }
        var url = $("#" + formId).attr("action")+"?";
        var inputs = $("#" + formId +" :input");
        var cnt = 0;
        inputs.each(function (i, item) {
                if(item.name == "" || item.value == null || item.value == "") return;
                if(item.type == "checkbox") {
                    if(!item.checked) {
                        return;
                    }
                } 
                if(skip!=null) {
                    for(var i=0;i<skip.length;i++) {
                        var pattern = skip[i];
                        if(item.name.match(pattern)) {
                            return;
                        }
                    }
                }
                if(cnt>0) url += "&";
                cnt++;
                url += encodeURIComponent(item.name) + "=" + encodeURIComponent(item.value);
                //                console.log(item.name +"=" + item.value);
            });

        var base = window.location.protocol+ "//" + window.location.host;
        //        console.log("protocol:" + window.location.protocol);
        //        console.log("base:" + base);
        //        console.log("url:" + url);
        url = base + url;                        
        //        console.log("final:" + url);
        $("#" + outputId).html(HtmlUtil.div(["class","ramadda-form-url"],  HtmlUtil.href(url, HtmlUtil.image(ramaddaBaseUrl +"/icons/link.png")) +" " + url));
    },
    makeUrlShowingForm: function(formId, outputId, skip) {
        $("#" + formId +" :input").change(function() {
                HtmlUtil.handleFormChangeShowUrl(formId, outputId, skip);
            });
        HtmlUtil.handleFormChangeShowUrl(formId, outputId, skip);
    },
    input :   function(name, value, attrs) {
        return "<input " + HtmlUtil.attrs(attrs) + HtmlUtil.attrs(["name", name, "value",value]) +">";
    },
    textarea :   function(name, value, attrs) {
        return "<textarea " + HtmlUtil.attrs(attrs) + HtmlUtil.attrs(["name", name]) +">"+value +"</textarea>";
    },
    initSelect: function(s) {
        $(s).selectBoxIt({});
    },
    valueDefined: function(value) {
        if(value != "" && value.indexOf("--") != 0) {
            return true;
        }
        return false;
    }, 


    squote: function(s) {return "'" + s +"'";},
    toggleBlock: function(label, contents, visible) {
        var id = "block_" + (uniqueCnt++);
        var imgid = id +"_img";

        var img1=ramaddaBaseUrl + "/icons/togglearrowdown.gif";
        var img2=ramaddaBaseUrl + "/icons/togglearrowright.gif";
        var args = HtmlUtil.join([HtmlUtil.squote(id),HtmlUtil.squote(imgid), HtmlUtil.squote(img1), HtmlUtil.squote(img2)],",");
        var click = "toggleBlockVisibility(" + args +");";

        var header = HtmlUtil.div(["class","entry-toggleblock-label","onClick", click],
                                  HtmlUtil.image((visible?img1:img2),  ["align","bottom","id",imgid]) +
                                  " " + label);
        var style = (visible?"display:block;visibility:visible":"display:none;");
        var body = HtmlUtil.div(["class","hideshowblock", "id",id, "style", style],
                                contents);
        return header + body;
    }
}


var StringUtil = {
    endsWith : function(str,suffix) {
        return (str.length >= suffix.length) && 
               (str.lastIndexOf(suffix) + suffix.length == str.length);
    },
    startsWith : function(str,prefix) {
        return (str.length >= prefix.length) && 
               (str.lastIndexOf(prefix, 0) === 0);
    }
}






var blockCnt=0;
function DomObject(name) {
    this.obj = null;
    // DOM level 1 browsers: IE 5+, NN 6+
    if (document.getElementById)	{    	
        this.obj = document.getElementById(name);
        if(this.obj) 
            this.style = this.obj.style;
    }
    // IE 4
    else if (document.all)	{  			
        this.obj = document.all[name];
        if(this.obj) 
            this.style = this.obj.style;
    }
    // NN 4
    else if (document.layers)  { 		
        this.obj = document.layers[name];
        this.style = document.layers[name];
    }
   if(this.obj) {
      this.id = this.obj.id;
      if(!this.id) {
	this.id = "obj"+ (blockCnt++);
      }
   } 
}


var RamaddaUtil = {
    //applies extend to the given object
    //and sets a super member to the original object
    //you can call original super class methods with:
    //this.super.<method>.call(this,...);
    inherit: function(object, parent) {
        $.extend(object, parent);
        object.super = parent;
        return object;
    },
    //Just a wrapper around extend. We use this so it is easy to find 
    //class definitions
    initMembers: function(object, members) {
        $.extend(object, members);
        return object;
    },
    //Just a wrapper around extend. We use this so it is easy to find 
    //class definitions
    defineMembers: function(object, members) {
        $.extend(object, members);
        return object;
    }
}


//Set a flag so we know not to show error dialogs above
/*
$(window).on('beforeunload', function(){
        GuiUtils.pageUnloading = true;
        return null;
    });
*/


function pageIsUnloading() {
    GuiUtils.pageUnloading = true;
    //Gotta do this for IE (uggh)
    window.onbeforeunload=confirmExit;
}

function confirmExit()  {
    return null;
}

$.widget( "custom.iconselectmenu", $.ui.selectmenu, {
  _renderItem: function( ul, item ) {
    var li = $( "<li>" ),
      //wrapper = $( "<div>", { text: item.label } );
      wrapper = $( "<span>");

    if ( item.disabled ) {
      li.addClass( "ui-state-disabled" );
    }

    $( "<img>", {
      style: item.element.attr( "data-style" ),
      "class": "ui-icon " + item.element.attr( "data-class" ),
      "src": item.element.attr( "img-src" )
    }).appendTo(wrapper);

    wrapper.append(item.label);

    return li.append( wrapper ).appendTo( ul );
  }
});


window.onbeforeunload = pageIsUnloading;




               /*
msg = "red_white_blue:["
    for(i=Utils.ColorTables.blue_white_red.length-1;i>=0;i--) {
    msg +=Utils.ColorTables.blue_white_red[i]+",";
}
msg+="]"
    //    console.log(msg);
    */
