/**
 * Copyright (c) 2008-2019 Geode Systems LLC
 */



var root = ramaddaBaseUrl;
var urlroot = ramaddaBaseUrl;
var icon_close = ramaddaBaseUrl + "/icons/cancel.png";
var icon_rightarrow = ramaddaBaseUrl + "/icons/grayrightarrow.gif";
var icon_downdart = ramaddaBaseUrl + "/icons/downdart.gif";
var icon_rightdart = ramaddaBaseUrl + "/icons/rightdart.gif";
var icon_downdart = ramaddaBaseUrl + "/icons/application_side_contract.png";
var icon_rightdart = ramaddaBaseUrl + "/icons/application_side_expand.png";
var icon_progress = ramaddaBaseUrl + "/icons/progress.gif";
var icon_wait = ramaddaBaseUrl + "/icons/wait.gif";
var icon_information = ramaddaBaseUrl + "/icons/information.png";
var icon_folderclosed = ramaddaBaseUrl + "/icons/folderclosed.png";
var icon_folderopen = ramaddaBaseUrl + "/icons/togglearrowdown.gif";

var icon_tree_open = ramaddaBaseUrl + "/icons/togglearrowdown.gif";
var icon_tree_closed = ramaddaBaseUrl + "/icons/togglearrowright.gif";
var icon_zoom = ramaddaBaseUrl + "/icons/magnifier.png";
var icon_zoom_in = ramaddaBaseUrl + "/icons/magnifier_zoom_in.png";
var icon_zoom_out = ramaddaBaseUrl + "/icons/magnifier_zoom_out.png";

var icon_menuarrow = ramaddaBaseUrl + "/icons/downdart.gif";
var icon_blank = ramaddaBaseUrl + "/icons/blank.gif";
var icon_menu = ramaddaBaseUrl + "/icons/menu.png";

if (!window["uniqueCnt"]) {
    window["uniqueCnt"] = 1;
}

function noop() {}

function addHandler(obj, id) {
    if (window.globalHandlers == null) {
        window.globalHandlers  = {};
    }
    if(!id) id = HtmlUtils.getUniqueId();
    window.globalHandlers[id] = obj;
    return id;
}

function getHandler(id) {
    if (!id || window.globalHandlers == null) {
        return null;
    }
    return window.globalHandlers[id];
}




var Utils = {
    pageLoaded: false,
    getIcon: function(icon) {
        return ramaddaBaseUrl +"/icons/" + icon;
    },
    imports:{},
    importJS: async function(path,callback,err,noCache) {
        var key = "js:" + path;
        if(!noCache)
            if(this.imports[key]) return Utils.call(callback);
        try {
            await $.getScript( path).done(()=>{
                    if(!noCache)
                        this.imports[key] = true;
                    Utils.call(callback);
                })
                .fail(err);
        } catch(e) {}
       },
    waitOn: async function(obj,callback) {
        if(window[obj]) return;
    },
    importCSS: async function(path,callback,err,noCache) {
        var key = "css:" + path;
        if(!noCache)
            if(this.imports[key]) return Utils.call(callback);
       try {
        await $.ajax({
                url: path,
                dataType: 'text',
                success: (data) =>{
                    if(!noCache)
                        this.imports[key] = true;
                    $('<style type="text/css">\n' + data + '</style>').appendTo("head");                    
                    Utils.call(callback);
                }                  
            }).fail(err);
        } catch(e) {
        }
    },
    importText: async function(path,callback,err) {
        try {
            await $.ajax({
                    url: path,
                    dataType: 'text',
                    success: function(data) {
                        Utils.call(callback,data);
                    }                  
                }).fail(err);
        } catch(e) {
        }
    },

    padLeft: function(s, length, pad) {
        s = "" + s;
        if (!pad) pad = " ";
        while (s.length < length)
            s = pad + s;
        return s;
    },
    join: function(l, delimiter,offset) {
        if((typeof offset) ==  "undefined") offset=0;
        var s = "";
        for (var i = offset; i < l.length; i++) {
            if (i > offset) s += delimiter;
            s += l[i];
        }
        return s;
    },

    getUniqueValues: function(l) {
        var u = [];
        var map = {};
        for (var i = 0; i < l.length; i++) {
            var value = l[i];
            if (!this.isDefined(map[value])) {
                map[value] = true;
                u.push(value);
            }
        }
        return u;
    },
    getMacro: function(v) {
        if (v == "${states}")
            return "Alabama,Alaska,Arizona,Arkansas,California,Colorado,Connecticut,Delaware,District of Columbia,Florida,Georgia,Hawaii,Idaho,Illinois,Indiana,Iowa,Kansas,Kentucky,Louisiana,Maine,Maryland,Massachusetts,Michigan,Minnesota,Mississippi,Missouri,Montana,Nebraska,Nevada,New Hampshire,New Jersey,New Mexico,New York,North Carolina,North Dakota,Ohio,Oklahoma,Oregon,Origin State,Pennsylvania,Rhode Island,South Carolina,South Dakota,Tennessee,Texas,Utah,Vermont,Virginia,Washington,West Virginia,Wisconsin,Wyoming";
        return v;
    },
    formatDateYYYYMMDD: function(date, options, args) {
        return date.getUTCFullYear() + "-" + (date.getUTCMonth() + 1) + "-" + date.getUTCDate();
    },
    formatDateYYYY: function(date, options, args) {
        return date.getUTCFullYear();
    },
    formatDate: function(date, options, args) {
        if (!args) args = {};
        if (!options) {
            options = {
                weekday: 'long',
                year: 'numeric',
                month: 'long',
                day: 'numeric',
                hour: 'numeric',
                minute: 'numeric'
            };
        }
        var suffix = args.suffix;
        if (!suffix) suffix = "";
        else suffix = " " + suffix;
        if (args.timeZone) {
            options.timeZone = args.timeZone;
        } else {
            options.timeZone = "UTC";
        }
        return date.toLocaleDateString("en-US", options) + suffix;
    },
    incrementYear: function(date, by) {
        if (by == null) by = 1;
        date.setUTCFullYear(date.getUTCFullYear() + by);
        return date;
    },
    incrementMonth: function(date, by) {
        if (by == null) by = 1;
        while (by > 0) {
            if (date.getUTCMonth() == 11) {
                date.setUTCFullYear(date.getUTCFullYear() + 1);
                date.setUTCMonth(0);
            } else {
                date.setUTCMonth(date.getUTCMonth() + 1);
            }
            by--;
        }
        return date;
    },
    incrementDay: function(date, by) {
        if (by == null) by = 1;
        date.setTime(date.getTime() + (by * 1000 * 3600 * 24));
        return date;
    },
    getDayInYear: function(date) {
        return (Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()) - Date.UTC(date.getUTCFullYear(), 0, 0)) / 24 / 60 / 60 / 1000;
    },
    monthShortNames: ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"],
    getMonthShortNames: function() {
        return this.monthShortNames;
    },
    parseDate: function(s, roundUp, rel) {
        if (s == null) return null;
        s = s.trim();
        if (s == "") return null;
        var regexpStr = "(rel|now|today)(\\+|-)(.*)"
        var regex = new RegExp(regexpStr, 'i');
        var match = s.match(regex);
        var offset = 0;

        if (match != null) {
            offset = parseFloat(match[3]);
            if (match[2] == "-") offset = -offset;
            s = match[1];
        }
        var date = null;
        if (s == "now") {} else if (s == "rel") {
            if (rel == null) return null;
            date = new Date(rel.getTime());
        } else if (s == "today") {
            date = new Date(Date.now());
            date.setMilliseconds(0);
            date.setSeconds(0);
            date.setMinutes(0);
            if (roundUp)
                date.setHours(24);
        } else {
            date = new Date(Date.parse(s));
        }
        if (offset != 0) {
            date.setDate(date.getDate() + offset);
        }
        return date;
    },
    makeDownloadFile: function(filename, text) {
        var element = document.createElement('a');
        element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(text));
        element.setAttribute('download', filename);
        element.style.display = 'none';
        document.body.appendChild(element);
        element.click();
        document.body.removeChild(element);
    },
    later: function(callback) {
        if(callback) {
            setTimeout(callback,1);
        }
    },
    call: function(callback,arg1,arg2,arg3,arg4) {
        if(callback) {
            var args = [];
            if(Utils.isDefined(arg1)) args.push(arg1);
            if(Utils.isDefined(arg2)) args.push(arg2);
            if(Utils.isDefined(arg3)) args.push(arg3);
            if(Utils.isDefined(arg4)) args.push(arg4);
            callback.apply(this,args);
        }
        return arg1;
    },
    isDefined: function(v) {
        return !(typeof v === 'undefined');
    },
    camelCase: function(s) {
        if (!s) return s;
        var r = "";
        toks = s.split(" ");
        for (var i = 0; i < toks.length; i++) {
            tok = toks[i];
            converted = tok.substring(0, 1).toUpperCase();
            if (tok.length > 1)
                converted += tok.substring(1).toLowerCase();
            if (r != "") r += " ";
            r += converted;
        }
        return r;
    },
    stripTags: function(s) {
        var regex = /(<([^>]+)>)/ig;
        return s.replace(regex, " ");
    },
    articles: "that,this,they,those,with,them,their,have,than,there,when,more,much,many,will,were".split(","),
    stopWords: ["not", "no", "will", "must", "just", "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are", "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but", "by", "could", "did", "do", "does", "doing", "down", "during", "each", "few", "for", "from", "further", "had", "has", "have", "having", "he", "he'd", "he'll", "he's", "her", "here", "here's", "hers", "herself", "him", "himself", "his", "how", "how's", "i", "i'd", "i'll", "i'm", "i've", "if", "in", "into", "is", "it", "it's", "its", "itself", "let's", "me", "more", "most", "my", "myself", "nor", "of", "on", "once", "only", "or", "other", "ought", "our", "ours", "ourselves", "out", "over", "own", "same", "she", "she'd", "she'll", "she's", "should", "so", "some", "such", "than", "that", "that's", "the", "their", "theirs", "them", "themselves", "then", "there", "there's", "these", "they", "they'd", "they'll", "they're", "they've", "this", "those", "through", "to", "too", "under", "until", "up", "very", "was", "we", "we'd", "we'll", "we're", "we've", "were", "what", "what's", "when", "when's", "where", "where's", "which", "while", "who", "who's", "whom", "why", "why's", "with", "would", "you", "you'd", "you'll", "you're", "you've", "your", "yours", "yourself", "yourselves"],
    tokenizeWords: function(s, stopWords, extraStopWords, removeArticles) {
        if (!stopWords) {
            stopWords = this.stopWords;
        }
        var words = [];
        var toks = s.split(/[ ,\(\)\.:\;\n\?\-!\*]/);
        for (var i = 0; i < toks.length; i++) {
            var word = toks[i].trim();
            if (word == "") continue;
            var _word = word.toLowerCase();
            if (stopWords.includes(_word)) continue;
            if (extraStopWords && extraStopWords.includes(_word)) continue;
            if (removeArticles && this.articles.includes(_word)) continue;
            if (word.match(/^[0-9]+$/)) continue;
            words.push(word);
        }
        return words;
    },
    stringDefined: function(v) {
        if (!Utils.isDefined(v)) return false;
        if (v == null || v == "") return false;
        return true;
    },
    formatNumber: function(number, toFloat) {
        var s = this.formatNumberInner(number);
        if (toFloat) return parseFloat(s);
        return s;
    },

    formatNumberInner: function(number) {
        var anumber = number < 0 ? -number : number;
        if (anumber == Math.floor(anumber)) return number;
        if (anumber > 1000) {
            return number_format(number, 0);
        } else if (anumber > 100) {
            return number_format(number, 1);
        } else if (anumber > 10) {
            return number_format(number, 2);
        } else if (anumber > 1) {
            return number_format(number, 3);
        } else {
            var decimals = "" + (number - Math.floor(number));
            var s = number_format(number, Math.min(decimals.length - 2, 5));
            return s;
        }
    },
    isRealNumber: function(value) {
        return !(value == Number.POSITIVE_INFINITY || isNaN(value) || !Utils.isNumber(value) || !Utils.isDefined(value) || value == null);
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
    isType: function(value, typeName) {
        if (value && (typeof value == typeName)) {
            return true;
        }
        return false;
    },
    getDefined: function(v1, v2) {
        if (Utils.isDefined(v1)) return v1;
        return v2;
    },
    cleanId: function(id) {
        id = id.replace(/:/g, "_").replace(/\./g, "_").replace(/=/g, "_").replace(/\//g, "_");
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
    toFloat: function(s, dflt) {
        if (s == null || s.trim() == "") {
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
    getPageLoaded: function() {
        return this.pageLoaded;
    },

    initContent: function(parent) {
        if (!parent) parent = "";
        else parent = parent + " ";
        //tableize
        HtmlUtils.formatTable(parent + ".ramadda-table");
        var snippets = $(parent + ".ramadda-snippet-hover");
        snippets.each(function() {
            let snippet = $(this);
            snippet.parent().hover(function() {
                    var parent = $(this);
                    var offset = parent.height();
                    //Check for max-height on element
                    if (offset > parent.parent().height()) {
                        offset = parent.parent().height();
                    }
                    var popup = getTooltip();
                    popup.html(HtmlUtils.div(["class", "ramadda-popup-inner ramadda-snippet-popup"], snippet.html()));
                    popup.show();
                    popup.position({
                        of: parent,
                        my: "left top",
                        at: "left top+" + (offset + 1),
                        collision: "fit fit"
                    });
                },
                function() {
                    getTooltip().hide();
                }
            )
        });

        //Buttonize
        $(parent + ':submit').button().click(function(event) {});
        $(parent + '.ramadda-button').button().click(function(event) {});
        //menuize
        /*
        $(".ramadda-pulldown").selectBoxIt({});
        */
        /* for select menus with icons */
        $(parent + ".ramadda-pulldown-with-icons").iconselectmenu().iconselectmenu("menuWidget").addClass("ui-menu-icons ramadda-select-icon");
    },

    initPage: function() {
        this.initContent();
        this.pageLoaded = true;

        if (window["initRamaddaDisplays"]) {
            initRamaddaDisplays();
        }

        //allow for tabs to be added to text areas
        $(document).delegate('textarea', 'keydown', function(e) {
            var keyCode = e.keyCode || e.which;
            if (keyCode == 9) {
                e.preventDefault();
                var start = this.selectionStart;
                var end = this.selectionEnd;
                $(this).val($(this).val().substring(0, start) +
                    "\t" +
                    $(this).val().substring(end));
                this.selectionStart = this.selectionEnd = start + 1;
            }
        });
    },
    getColorTable: function(name, justColors) {
        var ct = this.ColorTables[name];
        if (ct && justColors) return ct.colors;
        return ct;
    },
    displayAllColorTables: function(domId) {
        var cnt = 0;
        var html = "";
        for (a in this.ColorTables) {
            html += HtmlUtils.b(a);
            html += HtmlUtils.div(["id", domId + "_" + cnt, "style", "width:100%;"], "");
            html += "<br>";
            cnt++;
        }
        $("#" + domId).html(html);
        cnt = 0;
        for (a in this.ColorTables) {
            this.displayColorTable(this.ColorTables[a], domId + "_" + cnt, 0, 1, {
                showRange: false,
                height: "20px"
            });
            cnt++;
        }
    },

    displayColorTable: function(ct, domId, min, max, args) {
        if (!ct) return;
        if (ct.colors) ct = ct.colors;
        var options = {
            height: "15px",
            showRange: true
        }
        if (args) $.extend(options, args);
        var stringValues = options.stringValues;
        if(stringValues && stringValues.length) 
            options.showRange = false;
        min = parseFloat(min);
        max = parseFloat(max);
        var html = HtmlUtils.openTag("div", ["class", "display-colortable"]) + "<table cellpadding=0 cellspacing=0 width=100% border=0><tr>";
        if (options.showRange)
            html += "<td width=1%>" + this.formatNumber(min) + "&nbsp;</td>";
        var step = (max - min) / ct.length;
        for (var i = 0; i < ct.length; i++) {
            var extra = "";
            var attrs = ["style", "background:" + ct[i] + ";" + "width:100%;height:" + options.height + ";min-width:1px;"];
            if (options.showRange) {
                attrs.push("title");
                attrs.push(this.formatNumber(min + step * i));
            }
            html += HtmlUtils.td(["class", "display-colortable-slice", "style", "background:" + ct[i] + ";", "width", "1"], HtmlUtils.div(attrs, ""));
        }
        if (options.showRange) {
            html += "<td width=1%>&nbsp;" + this.formatNumber(max) + "</td>";
        }
        html += "</tr></table>";
        html += HtmlUtils.closeTag("div");
        html += HtmlUtils.openTag("div", ["class", "display-colortable-extra"]);
        if(stringValues && stringValues.length) {
            var tdw = 100/stringValues.length+"%";
            html+="<table width=100%><tr>";
            for(var i=0;i<stringValues.length;i++) {
                html+="<td align=center width='" + tdw+"'>" + stringValues[i]+"</td>";
            }
            html+="</tr></table>"
        }
        html += HtmlUtils.closeTag("div");

        $("#" + domId).html(html);
    },
    ColorTables: {
        blues: {
            colors: ["rgb(255,255,255)", "rgb(246,246,255)", "rgb(237,237,255)", "rgb(228,228,255)", "rgb(219,219,255)", "rgb(211,211,255)", "rgb(202,202,255)", "rgb(193,193,255)", "rgb(184,184,255)", "rgb(175,175,255)", "rgb(167,167,255)", "rgb(158,158,255)", "rgb(149,149,255)", "rgb(140,140,255)", "rgb(131,131,255)", "rgb(123,123,255)", "rgb(114,114,255)", "rgb(105,105,255)", "rgb(96,96,255)", "rgb(87,87,255)", "rgb(79,79,255)", "rgb(70,70,255)", "rgb(61,61,255)", "rgb(52,52,255)", "rgb(43,43,255)", "rgb(35,35,255)", "rgb(26,26,255)", "rgb(17,17,255)", "rgb(8,8,255)", "rgb(0,0,255)", ]
        },
        blue_green_red: {
            colors: ["rgb(0,0,255)", "rgb(8,17,246)", "rgb(17,35,237)", "rgb(26,52,228)", "rgb(35,70,219)", "rgb(43,87,211)", "rgb(52,105,202)", "rgb(61,123,193)", "rgb(70,140,184)", "rgb(79,158,175)", "rgb(87,175,167)", "rgb(96,193,158)", "rgb(105,211,149)", "rgb(114,228,140)", "rgb(123,246,131)", "rgb(131,246,123)", "rgb(140,228,114)", "rgb(149,211,105)", "rgb(158,193,96)", "rgb(167,175,87)", "rgb(175,158,79)", "rgb(184,140,70)", "rgb(193,123,61)", "rgb(202,105,52)", "rgb(211,87,43)", "rgb(219,70,35)", "rgb(228,52,26)", "rgb(237,35,17)", "rgb(246,17,8)", "rgb(255,0,0)", ]
        },
        white_blue: {
            colors: ["rgb(244,252,254)", "rgb(101,239,255)", "rgb(50,227,255)", "rgb(0,169,204)", "rgb(0,122,153)"]
        },
        blue_red: {
            colors: ["rgb(26,12,234)", "rgb(26,12,234)", "rgb(26,12,234)", "rgb(234,12,48)", "rgb(234,12,48)", "rgb(234,12,48)"]
        },
        red_white_blue: {
            colors: ["rgb(254,9,9)", "rgb(254,11,11)", "rgb(254,13,13)", "rgb(254,15,15)", "rgb(254,17,17)", "rgb(254,19,19)", "rgb(254,21,21)", "rgb(254,23,23)", "rgb(254,25,25)", "rgb(254,27,27)", "rgb(254,29,29)", "rgb(254,31,31)", "rgb(254,33,33)", "rgb(254,35,35)", "rgb(254,37,37)", "rgb(254,39,39)", "rgb(254,41,41)", "rgb(254,43,43)", "rgb(254,45,45)", "rgb(254,47,47)", "rgb(254,49,49)", "rgb(254,51,51)", "rgb(254,53,53)", "rgb(254,55,55)", "rgb(254,56,57)", "rgb(254,58,59)", "rgb(254,60,61)", "rgb(254,62,63)", "rgb(254,64,65)", "rgb(254,66,67)", "rgb(254,68,69)", "rgb(254,70,71)", "rgb(254,72,73)", "rgb(254,74,75)", "rgb(254,76,77)", "rgb(254,78,79)", "rgb(254,80,81)", "rgb(254,82,83)", "rgb(254,84,85)", "rgb(254,86,87)", "rgb(254,88,89)", "rgb(253,90,91)", "rgb(253,92,93)", "rgb(253,94,95)", "rgb(253,96,97)", "rgb(253,98,99)", "rgb(253,100,101)", "rgb(253,102,103)", "rgb(253,104,105)", "rgb(253,105,107)", "rgb(253,107,109)", "rgb(253,109,111)", "rgb(253,111,113)", "rgb(253,113,115)", "rgb(253,115,117)", "rgb(253,117,119)", "rgb(253,119,121)", "rgb(253,121,123)", "rgb(253,123,125)", "rgb(253,125,127)", "rgb(253,127,129)", "rgb(253,129,131)", "rgb(253,131,132)", "rgb(253,133,134)", "rgb(253,135,136)", "rgb(253,137,138)", "rgb(253,139,140)", "rgb(253,141,142)", "rgb(253,143,144)", "rgb(253,145,146)", "rgb(253,147,148)", "rgb(253,149,150)", "rgb(253,151,152)", "rgb(253,153,154)", "rgb(253,154,156)", "rgb(253,156,158)", "rgb(253,158,160)", "rgb(253,160,162)", "rgb(253,162,164)", "rgb(253,164,166)", "rgb(253,166,168)", "rgb(253,168,170)", "rgb(253,170,172)", "rgb(252,172,174)", "rgb(252,174,176)", "rgb(252,176,178)", "rgb(252,178,180)", "rgb(252,180,182)", "rgb(252,182,184)", "rgb(252,184,186)", "rgb(252,186,188)", "rgb(252,188,190)", "rgb(252,190,192)", "rgb(252,192,194)", "rgb(252,194,196)", "rgb(252,196,198)", "rgb(252,198,200)", "rgb(252,200,202)", "rgb(252,202,204)", "rgb(252,203,206)", "rgb(252,205,208)", "rgb(252,207,210)", "rgb(252,209,212)", "rgb(252,211,214)", "rgb(252,213,216)", "rgb(252,215,218)", "rgb(252,217,220)", "rgb(252,219,222)", "rgb(252,221,224)", "rgb(252,223,226)", "rgb(252,225,228)", "rgb(252,227,230)", "rgb(252,229,232)", "rgb(252,231,234)", "rgb(252,233,236)", "rgb(252,235,238)", "rgb(252,237,240)", "rgb(252,239,242)", "rgb(252,241,244)", "rgb(252,243,246)", "rgb(252,245,248)", "rgb(252,247,250)", "rgb(252,249,252)", "rgb(252,251,254)", "rgb(252,252,255)", "rgb(250,250,255)", "rgb(248,248,255)", "rgb(246,246,255)", "rgb(244,244,255)", "rgb(242,242,255)", "rgb(240,240,255)", "rgb(238,238,255)", "rgb(236,236,255)", "rgb(234,234,255)", "rgb(232,232,255)", "rgb(230,230,255)", "rgb(228,228,255)", "rgb(226,226,255)", "rgb(224,224,255)", "rgb(222,222,255)", "rgb(220,220,255)", "rgb(218,218,255)", "rgb(216,216,255)", "rgb(214,214,255)", "rgb(212,212,255)", "rgb(210,210,255)", "rgb(208,208,255)", "rgb(206,206,255)", "rgb(204,204,255)", "rgb(202,202,255)", "rgb(200,200,255)", "rgb(198,198,255)", "rgb(196,196,255)", "rgb(194,194,255)", "rgb(192,192,255)", "rgb(190,190,255)", "rgb(188,188,255)", "rgb(186,186,255)", "rgb(184,184,255)", "rgb(182,182,255)", "rgb(180,180,255)", "rgb(178,178,255)", "rgb(176,176,255)", "rgb(174,174,255)", "rgb(172,172,255)", "rgb(170,170,255)", "rgb(168,168,255)", "rgb(166,166,255)", "rgb(164,164,255)", "rgb(162,162,255)", "rgb(160,160,255)", "rgb(158,158,255)", "rgb(156,156,255)", "rgb(154,154,255)", "rgb(152,152,255)", "rgb(150,150,255)", "rgb(148,148,255)", "rgb(146,146,255)", "rgb(144,144,255)", "rgb(142,142,255)", "rgb(140,140,255)", "rgb(138,138,255)", "rgb(136,136,255)", "rgb(134,134,255)", "rgb(132,132,255)", "rgb(130,130,255)", "rgb(128,128,255)", "rgb(126,126,255)", "rgb(124,124,255)", "rgb(122,122,255)", "rgb(120,120,255)", "rgb(118,118,255)", "rgb(116,116,255)", "rgb(114,114,255)", "rgb(112,112,255)", "rgb(110,110,255)", "rgb(108,108,255)", "rgb(106,106,255)", "rgb(104,104,255)", "rgb(102,102,255)", "rgb(100,100,255)", "rgb(98,98,255)", "rgb(96,96,255)", "rgb(94,94,255)", "rgb(92,92,255)", "rgb(90,90,255)", "rgb(88,88,255)", "rgb(86,86,255)", "rgb(84,84,255)", "rgb(82,82,255)", "rgb(80,80,255)", "rgb(78,78,255)", "rgb(76,76,255)", "rgb(74,74,255)", "rgb(72,72,255)", "rgb(70,70,255)", "rgb(68,68,255)", "rgb(66,66,255)", "rgb(64,64,255)", "rgb(62,62,255)", "rgb(60,60,255)", "rgb(58,58,255)", "rgb(56,56,255)", "rgb(54,54,255)", "rgb(52,52,255)", "rgb(50,50,255)", "rgb(48,48,255)", "rgb(46,46,255)", "rgb(44,44,255)", "rgb(42,42,255)", "rgb(40,40,255)", "rgb(38,38,255)", "rgb(36,36,255)", "rgb(34,34,255)", "rgb(32,32,255)", "rgb(30,30,255)", "rgb(28,28,255)", "rgb(26,26,255)", "rgb(24,24,255)", "rgb(22,22,255)", "rgb(20,20,255)", "rgb(18,18,255)", "rgb(16,16,255)", "rgb(14,14,255)", "rgb(12,12,255)", "rgb(10,10,255)", "rgb(8,8,255)", "rgb(6,6,255)", "rgb(4,4,255)", "rgb(2,2,255)", "rgb(0,0,255)"]
        },
        blue_white_red: {
            colors: ["rgb(0,0,255)", "rgb(2,2,255)", "rgb(4,4,255)", "rgb(6,6,255)", "rgb(8,8,255)", "rgb(10,10,255)", "rgb(12,12,255)", "rgb(14,14,255)", "rgb(16,16,255)", "rgb(18,18,255)", "rgb(20,20,255)", "rgb(22,22,255)", "rgb(24,24,255)", "rgb(26,26,255)", "rgb(28,28,255)", "rgb(30,30,255)", "rgb(32,32,255)", "rgb(34,34,255)", "rgb(36,36,255)", "rgb(38,38,255)", "rgb(40,40,255)", "rgb(42,42,255)", "rgb(44,44,255)", "rgb(46,46,255)", "rgb(48,48,255)", "rgb(50,50,255)", "rgb(52,52,255)", "rgb(54,54,255)", "rgb(56,56,255)", "rgb(58,58,255)", "rgb(60,60,255)", "rgb(62,62,255)", "rgb(64,64,255)", "rgb(66,66,255)", "rgb(68,68,255)", "rgb(70,70,255)", "rgb(72,72,255)", "rgb(74,74,255)", "rgb(76,76,255)", "rgb(78,78,255)", "rgb(80,80,255)", "rgb(82,82,255)", "rgb(84,84,255)", "rgb(86,86,255)", "rgb(88,88,255)", "rgb(90,90,255)", "rgb(92,92,255)", "rgb(94,94,255)", "rgb(96,96,255)", "rgb(98,98,255)", "rgb(100,100,255)", "rgb(102,102,255)", "rgb(104,104,255)", "rgb(106,106,255)", "rgb(108,108,255)", "rgb(110,110,255)", "rgb(112,112,255)", "rgb(114,114,255)", "rgb(116,116,255)", "rgb(118,118,255)", "rgb(120,120,255)", "rgb(122,122,255)", "rgb(124,124,255)", "rgb(126,126,255)", "rgb(128,128,255)", "rgb(130,130,255)", "rgb(132,132,255)", "rgb(134,134,255)", "rgb(136,136,255)", "rgb(138,138,255)", "rgb(140,140,255)", "rgb(142,142,255)", "rgb(144,144,255)", "rgb(146,146,255)", "rgb(148,148,255)", "rgb(150,150,255)", "rgb(152,152,255)", "rgb(154,154,255)", "rgb(156,156,255)", "rgb(158,158,255)", "rgb(160,160,255)", "rgb(162,162,255)", "rgb(164,164,255)", "rgb(166,166,255)", "rgb(168,168,255)", "rgb(170,170,255)", "rgb(172,172,255)", "rgb(174,174,255)", "rgb(176,176,255)", "rgb(178,178,255)", "rgb(180,180,255)", "rgb(182,182,255)", "rgb(184,184,255)", "rgb(186,186,255)", "rgb(188,188,255)", "rgb(190,190,255)", "rgb(192,192,255)", "rgb(194,194,255)", "rgb(196,196,255)", "rgb(198,198,255)", "rgb(200,200,255)", "rgb(202,202,255)", "rgb(204,204,255)", "rgb(206,206,255)", "rgb(208,208,255)", "rgb(210,210,255)", "rgb(212,212,255)", "rgb(214,214,255)", "rgb(216,216,255)", "rgb(218,218,255)", "rgb(220,220,255)", "rgb(222,222,255)", "rgb(224,224,255)", "rgb(226,226,255)", "rgb(228,228,255)", "rgb(230,230,255)", "rgb(232,232,255)", "rgb(234,234,255)", "rgb(236,236,255)", "rgb(238,238,255)", "rgb(240,240,255)", "rgb(242,242,255)", "rgb(244,244,255)", "rgb(246,246,255)", "rgb(248,248,255)", "rgb(250,250,255)", "rgb(252,252,255)", "rgb(252,251,254)", "rgb(252,249,252)", "rgb(252,247,250)", "rgb(252,245,248)", "rgb(252,243,246)", "rgb(252,241,244)", "rgb(252,239,242)", "rgb(252,237,240)", "rgb(252,235,238)", "rgb(252,233,236)", "rgb(252,231,234)", "rgb(252,229,232)", "rgb(252,227,230)", "rgb(252,225,228)", "rgb(252,223,226)", "rgb(252,221,224)", "rgb(252,219,222)", "rgb(252,217,220)", "rgb(252,215,218)", "rgb(252,213,216)", "rgb(252,211,214)", "rgb(252,209,212)", "rgb(252,207,210)", "rgb(252,205,208)", "rgb(252,203,206)", "rgb(252,202,204)", "rgb(252,200,202)", "rgb(252,198,200)", "rgb(252,196,198)", "rgb(252,194,196)", "rgb(252,192,194)", "rgb(252,190,192)", "rgb(252,188,190)", "rgb(252,186,188)", "rgb(252,184,186)", "rgb(252,182,184)", "rgb(252,180,182)", "rgb(252,178,180)", "rgb(252,176,178)", "rgb(252,174,176)", "rgb(252,172,174)", "rgb(253,170,172)", "rgb(253,168,170)", "rgb(253,166,168)", "rgb(253,164,166)", "rgb(253,162,164)", "rgb(253,160,162)", "rgb(253,158,160)", "rgb(253,156,158)", "rgb(253,154,156)", "rgb(253,153,154)", "rgb(253,151,152)", "rgb(253,149,150)", "rgb(253,147,148)", "rgb(253,145,146)", "rgb(253,143,144)", "rgb(253,141,142)", "rgb(253,139,140)", "rgb(253,137,138)", "rgb(253,135,136)", "rgb(253,133,134)", "rgb(253,131,132)", "rgb(253,129,131)", "rgb(253,127,129)", "rgb(253,125,127)", "rgb(253,123,125)", "rgb(253,121,123)", "rgb(253,119,121)", "rgb(253,117,119)", "rgb(253,115,117)", "rgb(253,113,115)", "rgb(253,111,113)", "rgb(253,109,111)", "rgb(253,107,109)", "rgb(253,105,107)", "rgb(253,104,105)", "rgb(253,102,103)", "rgb(253,100,101)", "rgb(253,98,99)", "rgb(253,96,97)", "rgb(253,94,95)", "rgb(253,92,93)", "rgb(253,90,91)", "rgb(254,88,89)", "rgb(254,86,87)", "rgb(254,84,85)", "rgb(254,82,83)", "rgb(254,80,81)", "rgb(254,78,79)", "rgb(254,76,77)", "rgb(254,74,75)", "rgb(254,72,73)", "rgb(254,70,71)", "rgb(254,68,69)", "rgb(254,66,67)", "rgb(254,64,65)", "rgb(254,62,63)", "rgb(254,60,61)", "rgb(254,58,59)", "rgb(254,56,57)", "rgb(254,55,55)", "rgb(254,53,53)", "rgb(254,51,51)", "rgb(254,49,49)", "rgb(254,47,47)", "rgb(254,45,45)", "rgb(254,43,43)", "rgb(254,41,41)", "rgb(254,39,39)", "rgb(254,37,37)", "rgb(254,35,35)", "rgb(254,33,33)", "rgb(254,31,31)", "rgb(254,29,29)", "rgb(254,27,27)", "rgb(254,25,25)", "rgb(254,23,23)", "rgb(254,21,21)", "rgb(254,19,19)", "rgb(254,17,17)", "rgb(254,15,15)", "rgb(254,13,13)", "rgb(254,11,11)", "rgb(254,9,9)"]
        },
        grayscale: {
            colors: ["#000", "#111", "#222", "#333", "#444", "#555", "#666", "#777", "#888", "#999", "#aaa", "#bbb", "#ccc", "#ddd", "#eee", "#fff"]
        },
        inversegrayscale: {
            colors: ["#fff", "#eee", "#ddd", "#ccc", "#bbb", "#aaa", "#999", "#888", "#777", "#666", "#555", "#444", "#333", "#222", "#111", "#000"]
        },
        rainbow: {
            colors: ["red", "orange", "yellow", "green", "blue", "indigo", "violet"]
        },
        nice: {
            colors: ["#1f77b4", "#ff7f0e", "#2ca02c", "#d62728", "#9467bd", "#8c564b", "#e377c2", "#7f7f7f", "#bcbd22", "#17becf"]
        },
        blues: {
            colors: ['rgb(255,255,255)', 'rgb(246,246,255)', 'rgb(237,237,255)', 'rgb(228,228,255)', 'rgb(219,219,255)', 'rgb(211,211,255)', 'rgb(202,202,255)', 'rgb(193,193,255)', 'rgb(184,184,255)', 'rgb(175,175,255)', 'rgb(167,167,255)', 'rgb(158,158,255)', 'rgb(149,149,255)', 'rgb(140,140,255)', 'rgb(131,131,255)', 'rgb(123,123,255)', 'rgb(114,114,255)', 'rgb(105,105,255)', 'rgb(96,96,255)', 'rgb(87,87,255)', 'rgb(79,79,255)', 'rgb(70,70,255)', 'rgb(61,61,255)', 'rgb(52,52,255)', 'rgb(43,43,255)', 'rgb(35,35,255)', 'rgb(26,26,255)', 'rgb(17,17,255)', 'rgb(8,8,255)', 'rgb(0,0,255)', ]
        },

        gray_scale: {
            colors: ['rgb(0,0,0)', 'rgb(1,1,1)', 'rgb(2,2,2)', 'rgb(3,3,3)', 'rgb(4,4,4)', 'rgb(5,5,5)', 'rgb(6,6,6)', 'rgb(7,7,7)', 'rgb(8,8,8)', 'rgb(9,9,9)', 'rgb(10,10,10)', 'rgb(11,11,11)', 'rgb(12,12,12)', 'rgb(13,13,13)', 'rgb(14,14,14)', 'rgb(15,15,15)', 'rgb(16,16,16)', 'rgb(17,17,17)', 'rgb(18,18,18)', 'rgb(19,19,19)', 'rgb(20,20,20)', 'rgb(21,21,21)', 'rgb(22,22,22)', 'rgb(23,23,23)', 'rgb(24,24,24)', 'rgb(25,25,25)', 'rgb(26,26,26)', 'rgb(27,27,27)', 'rgb(28,28,28)', 'rgb(29,29,29)', 'rgb(30,30,30)', 'rgb(31,31,31)', 'rgb(32,32,32)', 'rgb(33,33,33)', 'rgb(34,34,34)', 'rgb(35,35,35)', 'rgb(36,36,36)', 'rgb(37,37,37)', 'rgb(38,38,38)', 'rgb(39,39,39)', 'rgb(40,40,40)', 'rgb(41,41,41)', 'rgb(42,42,42)', 'rgb(43,43,43)', 'rgb(44,44,44)', 'rgb(45,45,45)', 'rgb(46,46,46)', 'rgb(47,47,47)', 'rgb(48,48,48)', 'rgb(49,49,49)', 'rgb(50,50,50)', 'rgb(51,51,51)', 'rgb(52,52,52)', 'rgb(53,53,53)', 'rgb(54,54,54)', 'rgb(55,55,55)', 'rgb(56,56,56)', 'rgb(57,57,57)', 'rgb(58,58,58)', 'rgb(59,59,59)', 'rgb(60,60,60)', 'rgb(61,61,61)', 'rgb(62,62,62)', 'rgb(63,63,63)', 'rgb(64,64,64)', 'rgb(65,65,65)', 'rgb(66,66,66)', 'rgb(67,67,67)', 'rgb(68,68,68)', 'rgb(69,69,69)', 'rgb(70,70,70)', 'rgb(71,71,71)', 'rgb(72,72,72)', 'rgb(73,73,73)', 'rgb(74,74,74)', 'rgb(75,75,75)', 'rgb(76,76,76)', 'rgb(77,77,77)', 'rgb(78,78,78)', 'rgb(79,79,79)', 'rgb(80,80,80)', 'rgb(81,81,81)', 'rgb(82,82,82)', 'rgb(83,83,83)', 'rgb(84,84,84)', 'rgb(85,85,85)', 'rgb(86,86,86)', 'rgb(87,87,87)', 'rgb(88,88,88)', 'rgb(89,89,89)', 'rgb(90,90,90)', 'rgb(91,91,91)', 'rgb(92,92,92)', 'rgb(93,93,93)', 'rgb(94,94,94)', 'rgb(95,95,95)', 'rgb(96,96,96)', 'rgb(97,97,97)', 'rgb(98,98,98)', 'rgb(99,99,99)', 'rgb(100,100,100)', 'rgb(101,101,101)', 'rgb(102,102,102)', 'rgb(103,103,103)', 'rgb(104,104,104)', 'rgb(105,105,105)', 'rgb(106,106,106)', 'rgb(107,107,107)', 'rgb(108,108,108)', 'rgb(109,109,109)', 'rgb(110,110,110)', 'rgb(111,111,111)', 'rgb(112,112,112)', 'rgb(113,113,113)', 'rgb(114,114,114)', 'rgb(115,115,115)', 'rgb(116,116,116)', 'rgb(117,117,117)', 'rgb(118,118,118)', 'rgb(119,119,119)', 'rgb(120,120,120)', 'rgb(121,121,121)', 'rgb(122,122,122)', 'rgb(123,123,123)', 'rgb(124,124,124)', 'rgb(125,125,125)', 'rgb(126,126,126)', 'rgb(127,127,127)', 'rgb(128,128,128)', 'rgb(129,129,129)', 'rgb(130,130,130)', 'rgb(131,131,131)', 'rgb(132,132,132)', 'rgb(133,133,133)', 'rgb(134,134,134)', 'rgb(135,135,135)', 'rgb(136,136,136)', 'rgb(137,137,137)', 'rgb(138,138,138)', 'rgb(139,139,139)', 'rgb(140,140,140)', 'rgb(141,141,141)', 'rgb(142,142,142)', 'rgb(143,143,143)', 'rgb(144,144,144)', 'rgb(145,145,145)', 'rgb(146,146,146)', 'rgb(147,147,147)', 'rgb(148,148,148)', 'rgb(149,149,149)', 'rgb(150,150,150)', 'rgb(151,151,151)', 'rgb(152,152,152)', 'rgb(153,153,153)', 'rgb(154,154,154)', 'rgb(155,155,155)', 'rgb(156,156,156)', 'rgb(157,157,157)', 'rgb(158,158,158)', 'rgb(159,159,159)', 'rgb(160,160,160)', 'rgb(161,161,161)', 'rgb(162,162,162)', 'rgb(163,163,163)', 'rgb(164,164,164)', 'rgb(165,165,165)', 'rgb(166,166,166)', 'rgb(167,167,167)', 'rgb(168,168,168)', 'rgb(169,169,169)', 'rgb(170,170,170)', 'rgb(171,171,171)', 'rgb(172,172,172)', 'rgb(173,173,173)', 'rgb(174,174,174)', 'rgb(175,175,175)', 'rgb(176,176,176)', 'rgb(177,177,177)', 'rgb(178,178,178)', 'rgb(179,179,179)', 'rgb(180,180,180)', 'rgb(181,181,181)', 'rgb(182,182,182)', 'rgb(183,183,183)', 'rgb(184,184,184)', 'rgb(185,185,185)', 'rgb(186,186,186)', 'rgb(187,187,187)', 'rgb(188,188,188)', 'rgb(189,189,189)', 'rgb(190,190,190)', 'rgb(191,191,191)', 'rgb(192,192,192)', 'rgb(193,193,193)', 'rgb(194,194,194)', 'rgb(195,195,195)', 'rgb(196,196,196)', 'rgb(197,197,197)', 'rgb(198,198,198)', 'rgb(199,199,199)', 'rgb(200,200,200)', 'rgb(201,201,201)', 'rgb(202,202,202)', 'rgb(203,203,203)', 'rgb(204,204,204)', 'rgb(205,205,205)', 'rgb(206,206,206)', 'rgb(207,207,207)', 'rgb(208,208,208)', 'rgb(209,209,209)', 'rgb(210,210,210)', 'rgb(211,211,211)', 'rgb(212,212,212)', 'rgb(213,213,213)', 'rgb(214,214,214)', 'rgb(215,215,215)', 'rgb(216,216,216)', 'rgb(217,217,217)', 'rgb(218,218,218)', 'rgb(219,219,219)', 'rgb(220,220,220)', 'rgb(221,221,221)', 'rgb(222,222,222)', 'rgb(223,223,223)', 'rgb(224,224,224)', 'rgb(225,225,225)', 'rgb(226,226,226)', 'rgb(227,227,227)', 'rgb(228,228,228)', 'rgb(229,229,229)', 'rgb(230,230,230)', 'rgb(231,231,231)', 'rgb(232,232,232)', 'rgb(233,233,233)', 'rgb(234,234,234)', 'rgb(235,235,235)', 'rgb(236,236,236)', 'rgb(237,237,237)', 'rgb(238,238,238)', 'rgb(239,239,239)', 'rgb(240,240,240)', 'rgb(241,241,241)', 'rgb(242,242,242)', 'rgb(243,243,243)', 'rgb(244,244,244)', 'rgb(245,245,245)', 'rgb(246,246,246)', 'rgb(247,247,247)', 'rgb(248,248,248)', 'rgb(249,249,249)', 'rgb(250,250,250)', 'rgb(251,251,251)', 'rgb(252,252,252)', 'rgb(253,253,253)', 'rgb(254,254,254)', 'rgb(255,255,255)', ]
        },
        inverse_gray_shade: {
            colors: ['rgb(255,255,255)', 'rgb(255,255,255)', 'rgb(254,254,254)', 'rgb(253,253,253)', 'rgb(252,252,252)', 'rgb(251,251,251)', 'rgb(250,250,250)', 'rgb(249,249,249)', 'rgb(248,248,248)', 'rgb(247,247,247)', 'rgb(246,246,246)', 'rgb(245,245,245)', 'rgb(244,244,244)', 'rgb(243,243,243)', 'rgb(242,242,242)', 'rgb(241,241,241)', 'rgb(240,240,240)', 'rgb(239,239,239)', 'rgb(238,238,238)', 'rgb(237,237,237)', 'rgb(236,236,236)', 'rgb(235,235,235)', 'rgb(234,234,234)', 'rgb(233,233,233)', 'rgb(232,232,232)', 'rgb(231,231,231)', 'rgb(230,230,230)', 'rgb(229,229,229)', 'rgb(228,228,228)', 'rgb(227,227,227)', 'rgb(226,226,226)', 'rgb(225,225,225)', 'rgb(224,224,224)', 'rgb(223,223,223)', 'rgb(222,222,222)', 'rgb(221,221,221)', 'rgb(220,220,220)', 'rgb(219,219,219)', 'rgb(218,218,218)', 'rgb(217,217,217)', 'rgb(216,216,216)', 'rgb(215,215,215)', 'rgb(214,214,214)', 'rgb(213,213,213)', 'rgb(212,212,212)', 'rgb(211,211,211)', 'rgb(210,210,210)', 'rgb(209,209,209)', 'rgb(208,208,208)', 'rgb(207,207,207)', 'rgb(206,206,206)', 'rgb(205,205,205)', 'rgb(204,204,204)', 'rgb(203,203,203)', 'rgb(202,202,202)', 'rgb(201,201,201)', 'rgb(200,200,200)', 'rgb(199,199,199)', 'rgb(198,198,198)', 'rgb(197,197,197)', 'rgb(196,196,196)', 'rgb(195,195,195)', 'rgb(194,194,194)', 'rgb(193,193,193)', 'rgb(192,192,192)', 'rgb(191,191,191)', 'rgb(190,190,190)', 'rgb(189,189,189)', 'rgb(188,188,188)', 'rgb(187,187,187)', 'rgb(186,186,186)', 'rgb(185,185,185)', 'rgb(184,184,184)', 'rgb(183,183,183)', 'rgb(182,182,182)', 'rgb(181,181,181)', 'rgb(180,180,180)', 'rgb(179,179,179)', 'rgb(178,178,178)', 'rgb(177,177,177)', 'rgb(176,176,176)', 'rgb(175,175,175)', 'rgb(174,174,174)', 'rgb(173,173,173)', 'rgb(172,172,172)', 'rgb(171,171,171)', 'rgb(170,170,170)', 'rgb(169,169,169)', 'rgb(168,168,168)', 'rgb(167,167,167)', 'rgb(166,166,166)', 'rgb(165,165,165)', 'rgb(164,164,164)', 'rgb(163,163,163)', 'rgb(162,162,162)', 'rgb(161,161,161)', 'rgb(160,160,160)', 'rgb(159,159,159)', 'rgb(158,158,158)', 'rgb(157,157,157)', 'rgb(156,156,156)', 'rgb(155,155,155)', 'rgb(154,154,154)', 'rgb(153,153,153)', 'rgb(152,152,152)', 'rgb(151,151,151)', 'rgb(150,150,150)', 'rgb(149,149,149)', 'rgb(148,148,148)', 'rgb(147,147,147)', 'rgb(146,146,146)', 'rgb(145,145,145)', 'rgb(144,144,144)', 'rgb(143,143,143)', 'rgb(142,142,142)', 'rgb(141,141,141)', 'rgb(140,140,140)', 'rgb(139,139,139)', 'rgb(138,138,138)', 'rgb(137,137,137)', 'rgb(136,136,136)', 'rgb(135,135,135)', 'rgb(134,134,134)', 'rgb(133,133,133)', 'rgb(132,132,132)', 'rgb(131,131,131)', 'rgb(130,130,130)', 'rgb(129,129,129)', 'rgb(128,128,128)', 'rgb(127,127,127)', 'rgb(126,126,126)', 'rgb(125,125,125)', 'rgb(124,124,124)', 'rgb(123,123,123)', 'rgb(122,122,122)', 'rgb(121,121,121)', 'rgb(120,120,120)', 'rgb(119,119,119)', 'rgb(118,118,118)', 'rgb(117,117,117)', 'rgb(116,116,116)', 'rgb(115,115,115)', 'rgb(114,114,114)', 'rgb(113,113,113)', 'rgb(112,112,112)', 'rgb(111,111,111)', 'rgb(110,110,110)', 'rgb(109,109,109)', 'rgb(108,108,108)', 'rgb(107,107,107)', 'rgb(106,106,106)', 'rgb(105,105,105)', 'rgb(104,104,104)', 'rgb(103,103,103)', 'rgb(102,102,102)', 'rgb(101,101,101)', 'rgb(100,100,100)', 'rgb(99,99,99)', 'rgb(98,98,98)', 'rgb(97,97,97)', 'rgb(96,96,96)', 'rgb(95,95,95)', 'rgb(94,94,94)', 'rgb(93,93,93)', 'rgb(92,92,92)', 'rgb(91,91,91)', 'rgb(90,90,90)', 'rgb(89,89,89)', 'rgb(88,88,88)', 'rgb(87,87,87)', 'rgb(86,86,86)', 'rgb(85,85,85)', 'rgb(84,84,84)', 'rgb(83,83,83)', 'rgb(82,82,82)', 'rgb(81,81,81)', 'rgb(80,80,80)', 'rgb(79,79,79)', 'rgb(78,78,78)', 'rgb(77,77,77)', 'rgb(76,76,76)', 'rgb(75,75,75)', 'rgb(74,74,74)', 'rgb(73,73,73)', 'rgb(72,72,72)', 'rgb(71,71,71)', 'rgb(70,70,70)', 'rgb(69,69,69)', 'rgb(68,68,68)', 'rgb(67,67,67)', 'rgb(66,66,66)', 'rgb(65,65,65)', 'rgb(64,64,64)', 'rgb(63,63,63)', 'rgb(62,62,62)', 'rgb(61,61,61)', 'rgb(60,60,60)', 'rgb(59,59,59)', 'rgb(58,58,58)', 'rgb(57,57,57)', 'rgb(56,56,56)', 'rgb(55,55,55)', 'rgb(54,54,54)', 'rgb(53,53,53)', 'rgb(52,52,52)', 'rgb(51,51,51)', 'rgb(50,50,50)', 'rgb(49,49,49)', 'rgb(48,48,48)', 'rgb(47,47,47)', 'rgb(46,46,46)', 'rgb(45,45,45)', 'rgb(44,44,44)', 'rgb(43,43,43)', 'rgb(42,42,42)', 'rgb(41,41,41)', 'rgb(40,40,40)', 'rgb(39,39,39)', 'rgb(38,38,38)', 'rgb(37,37,37)', 'rgb(36,36,36)', 'rgb(35,35,35)', 'rgb(34,34,34)', 'rgb(33,33,33)', 'rgb(32,32,32)', 'rgb(31,31,31)', 'rgb(30,30,30)', 'rgb(29,29,29)', 'rgb(28,28,28)', 'rgb(27,27,27)', 'rgb(26,26,26)', 'rgb(25,25,25)', 'rgb(24,24,24)', 'rgb(23,23,23)', 'rgb(22,22,22)', 'rgb(21,21,21)', 'rgb(20,20,20)', 'rgb(19,19,19)', 'rgb(18,18,18)', 'rgb(17,17,17)', 'rgb(16,16,16)', 'rgb(15,15,15)', 'rgb(14,14,14)', 'rgb(13,13,13)', 'rgb(12,12,12)', 'rgb(11,11,11)', 'rgb(10,10,10)', 'rgb(9,9,9)', 'rgb(8,8,8)', 'rgb(7,7,7)', 'rgb(6,6,6)', 'rgb(5,5,5)', 'rgb(4,4,4)', 'rgb(3,3,3)', 'rgb(2,2,2)', 'rgb(1,1,1)', ]
        },
        light_gray_scale: {
            colors: ['rgb(62,62,62)', 'rgb(69,69,69)', 'rgb(75,75,75)', 'rgb(82,82,82)', 'rgb(88,88,88)', 'rgb(95,95,95)', 'rgb(102,102,102)', 'rgb(108,108,108)', 'rgb(115,115,115)', 'rgb(121,121,121)', 'rgb(128,128,128)', 'rgb(135,135,135)', 'rgb(141,141,141)', 'rgb(148,148,148)', 'rgb(155,155,155)', 'rgb(161,161,161)', 'rgb(168,168,168)', 'rgb(174,174,174)', 'rgb(181,181,181)', 'rgb(188,188,188)', 'rgb(194,194,194)', 'rgb(201,201,201)', 'rgb(207,207,207)', 'rgb(214,214,214)', 'rgb(221,221,221)', 'rgb(227,227,227)', 'rgb(234,234,234)', 'rgb(240,240,240)', 'rgb(247,247,247)', 'rgb(254,254,254)', ]
        },
        blue_green: {
            colors: ['#e5f5f9', '#99d8c9', '#2ca25f']
        },
        blue_purple: {
            colors: ['#e0ecf4', '#9ebcda', '#8856a7']
        },
        green_blue: {
            colors: ['#e0f3db', '#a8ddb5', '#43a2ca']
        },
        orange_red: {
            colors: ['#fee8c8', '#fdbb84', '#e34a33']
        },
        purple_blue: {
            colors: ['#ece7f2', '#a6bddb', '#2b8cbe']
        },
        purple_blue_green: {
            colors: ['#ece2f0', '#a6bddb', '#1c9099']
        },
        purple_red: {
            colors: ['#e7e1ef', '#c994c7', '#dd1c77']
        },
        red_purple: {
            colors: ['#fde0dd', '#fa9fb5', '#c51b8a']
        },
        yellow_green: {
            colors: ['#f7fcb9', '#addd8e', '#31a354']
        },
        yellow_green_blue: {
            colors: ['#edf8b1', '#7fcdbb', '#2c7fb8']
        },
        yellow_orange_brown: {
            colors: ['#fff7bc', '#fec44f', '#d95f0e']
        },
        yellow_orange_red: {
            colors: ['#ffeda0', '#feb24c', '#f03b20']
        },
        oranges: {
            colors: ['#fee6ce', '#fdae6b', '#e6550d']
        },
        purples: {
            colors: ['#efedf5', '#bcbddc', '#756bb1']
        },
        reds: {
            colors: ['#fee0d2', '#fc9272', '#de2d26']
        },
        greens: {
            colors: ['#e5f5e0', '#a1d99b', '#31a354']
        },
        bright38: {
            colors: ['rgb(254,0,225)', 'rgb(188,0,254)', 'rgb(165,0,254)', 'rgb(134,0,254)', 'rgb(111,0,254)', 'rgb(81,0,254)', 'rgb(58,0,254)', 'rgb(28,0,254)', 'rgb(0,2,254)', 'rgb(0,33,254)', 'rgb(0,56,254)', 'rgb(0,78,254)', 'rgb(0,139,254)', 'rgb(0,169,254)', 'rgb(0,208,254)', 'rgb(0,231,254)', 'rgb(0,254,231)', 'rgb(0,254,200)', 'rgb(0,254,169)', 'rgb(0,254,139)', 'rgb(0,254,109)', 'rgb(0,254,79)', 'rgb(0,254,39)', 'rgb(0,254,0)', 'rgb(42,254,0)', 'rgb(88,254,0)', 'rgb(126,254,0)', 'rgb(164,254,0)', 'rgb(195,254,0)', 'rgb(226,254,0)', 'rgb(254,243,0)', 'rgb(254,199,0)', 'rgb(254,167,0)', 'rgb(254,137,0)', 'rgb(254,106,0)', 'rgb(254,68,0)', 'rgb(254,30,0)', 'rgb(254,0,0)', ]
        },
        precipitation: {
            colors: ['rgb(255,255,255)', 'rgb(6,13,255)', 'rgb(13,26,255)', 'rgb(20,40,255)', 'rgb(26,53,255)', 'rgb(33,67,255)', 'rgb(40,80,255)', 'rgb(46,93,255)', 'rgb(53,107,255)', 'rgb(60,120,255)', 'rgb(67,134,255)', 'rgb(73,147,255)', 'rgb(80,161,255)', 'rgb(87,174,255)', 'rgb(93,187,255)', 'rgb(100,201,255)', 'rgb(107,214,255)', 'rgb(114,228,255)', 'rgb(120,241,255)', 'rgb(127,255,255)', 'rgb(127,255,229)', 'rgb(129,253,223)', 'rgb(130,251,216)', 'rgb(132,249,210)', 'rgb(133,247,203)', 'rgb(135,245,197)', 'rgb(137,243,190)', 'rgb(138,241,184)', 'rgb(140,239,177)', 'rgb(142,237,171)', 'rgb(143,235,164)', 'rgb(145,233,158)', 'rgb(146,231,152)', 'rgb(148,229,145)', 'rgb(150,227,139)', 'rgb(151,225,132)', 'rgb(153,224,126)', 'rgb(154,222,119)', 'rgb(156,220,113)', 'rgb(158,218,106)', 'rgb(159,216,100)', 'rgb(161,214,93)', 'rgb(163,212,87)', 'rgb(164,210,81)', 'rgb(166,208,74)', 'rgb(167,206,68)', 'rgb(169,204,61)', 'rgb(171,202,55)', 'rgb(172,200,48)', 'rgb(174,198,42)', 'rgb(175,196,35)', 'rgb(177,194,29)', 'rgb(179,193,22)', 'rgb(180,191,16)', 'rgb(182,189,10)', 'rgb(183,187,3)', 'rgb(185,185,0)', 'rgb(187,183,0)', 'rgb(188,181,0)', 'rgb(190,179,0)', 'rgb(192,177,0)', 'rgb(193,175,0)', 'rgb(195,173,0)', 'rgb(196,171,0)', 'rgb(198,169,0)', 'rgb(200,167,0)', 'rgb(201,165,0)', 'rgb(203,163,0)', 'rgb(204,162,0)', 'rgb(206,160,0)', 'rgb(208,158,0)', 'rgb(209,156,0)', 'rgb(211,154,0)', 'rgb(213,152,0)', 'rgb(214,150,0)', 'rgb(216,148,0)', 'rgb(217,146,0)', 'rgb(219,144,0)', 'rgb(221,142,0)', 'rgb(222,140,0)', 'rgb(224,138,0)', 'rgb(225,136,0)', 'rgb(227,134,0)', 'rgb(229,132,0)', 'rgb(230,131,0)', 'rgb(232,129,0)', 'rgb(234,127,0)', 'rgb(235,125,0)', 'rgb(237,123,0)', 'rgb(238,121,0)', 'rgb(240,119,0)', 'rgb(242,117,0)', 'rgb(243,115,0)', 'rgb(245,113,0)', 'rgb(246,111,0)', 'rgb(248,109,0)', 'rgb(250,107,0)', 'rgb(251,105,0)', 'rgb(253,103,0)', 'rgb(255,101,0)', ]
        },
        humidity: {
            colors: ['rgb(255,255,0)', 'rgb(228,255,0)', 'rgb(201,255,0)', 'rgb(174,255,0)', 'rgb(147,255,0)', 'rgb(120,255,0)', 'rgb(93,255,0)', 'rgb(67,255,0)', 'rgb(40,255,0)', 'rgb(13,255,0)', 'rgb(0,248,13)', 'rgb(0,234,40)', 'rgb(0,221,67)', 'rgb(0,208,93)', 'rgb(0,194,120)', 'rgb(0,181,147)', 'rgb(0,167,174)', 'rgb(0,154,201)', 'rgb(0,140,228)', 'rgb(0,127,255)', ]
        },
        temperature: {
            min: -90,
            max: 45,
            colors: ['rgb(0,0,250)', 'rgb(0,2,250)', 'rgb(0,5,250)', 'rgb(0,8,250)', 'rgb(0,11,250)', 'rgb(0,14,250)', 'rgb(0,16,250)', 'rgb(0,19,250)', 'rgb(0,22,250)', 'rgb(0,25,250)', 'rgb(0,28,250)', 'rgb(0,30,250)', 'rgb(0,33,250)', 'rgb(0,36,250)', 'rgb(0,39,250)', 'rgb(0,42,250)', 'rgb(0,44,250)', 'rgb(0,47,250)', 'rgb(0,50,250)', 'rgb(0,53,250)', 'rgb(0,56,250)', 'rgb(0,58,250)', 'rgb(0,61,250)', 'rgb(0,64,250)', 'rgb(0,67,250)', 'rgb(0,70,250)', 'rgb(0,73,250)', 'rgb(0,75,250)', 'rgb(0,78,250)', 'rgb(0,81,250)', 'rgb(0,84,250)', 'rgb(0,87,250)', 'rgb(0,89,250)', 'rgb(0,92,250)', 'rgb(0,95,250)', 'rgb(0,98,250)', 'rgb(0,101,250)', 'rgb(0,103,250)', 'rgb(0,106,250)', 'rgb(0,109,250)', 'rgb(0,112,250)', 'rgb(0,115,250)', 'rgb(0,117,250)', 'rgb(0,120,250)', 'rgb(0,123,250)', 'rgb(0,126,250)', 'rgb(0,129,250)', 'rgb(0,132,250)', 'rgb(0,134,250)', 'rgb(0,137,250)', 'rgb(0,140,250)', 'rgb(0,143,250)', 'rgb(0,146,250)', 'rgb(0,148,250)', 'rgb(0,151,250)', 'rgb(0,154,250)', 'rgb(0,157,250)', 'rgb(0,160,250)', 'rgb(0,162,250)', 'rgb(0,165,250)', 'rgb(0,168,250)', 'rgb(0,171,250)', 'rgb(0,174,250)', 'rgb(0,176,250)', 'rgb(0,179,250)', 'rgb(0,182,250)', 'rgb(0,185,250)', 'rgb(0,188,250)', 'rgb(0,191,250)', 'rgb(0,193,250)', 'rgb(0,196,250)', 'rgb(0,199,250)', 'rgb(0,202,250)', 'rgb(0,205,250)', 'rgb(0,207,250)', 'rgb(0,210,250)', 'rgb(0,213,250)', 'rgb(0,216,250)', 'rgb(0,219,250)', 'rgb(0,221,250)', 'rgb(0,224,250)', 'rgb(0,227,250)', 'rgb(0,230,250)', 'rgb(0,233,250)', 'rgb(0,235,250)', 'rgb(0,238,250)', 'rgb(0,241,250)', 'rgb(0,244,250)', 'rgb(0,247,250)', 'rgb(0,255,255)', 'rgb(0,255,203)', 'rgb(0,255,152)', 'rgb(0,255,101)', 'rgb(0,255,50)', 'rgb(0,255,0)', 'rgb(22,255,0)', 'rgb(45,255,0)', 'rgb(68,255,0)', 'rgb(91,255,0)', 'rgb(113,255,0)', 'rgb(136,255,0)', 'rgb(159,255,0)', 'rgb(182,255,0)', 'rgb(205,255,0)', 'rgb(255,255,0)', 'rgb(255,247,0)', 'rgb(255,240,0)', 'rgb(255,232,0)', 'rgb(255,225,0)', 'rgb(255,217,0)', 'rgb(255,210,0)', 'rgb(255,202,0)', 'rgb(255,195,0)', 'rgb(255,187,0)', 'rgb(255,180,0)', 'rgb(255,172,0)', 'rgb(255,165,0)', 'rgb(255,157,0)', 'rgb(255,150,0)', 'rgb(255,142,0)', 'rgb(255,135,0)', 'rgb(255,127,0)', 'rgb(255,120,0)', 'rgb(255,112,0)', 'rgb(255,105,0)', 'rgb(255,97,0)', 'rgb(255,90,0)', 'rgb(255,82,0)', 'rgb(255,75,0)', 'rgb(255,67,0)', 'rgb(255,60,0)', 'rgb(255,52,0)', 'rgb(255,45,0)', 'rgb(255,37,0)', 'rgb(255,30,0)', ]
        },
        visad: {
            colors: ['rgb(0,0,255)', 'rgb(8,17,246)', 'rgb(17,35,237)', 'rgb(26,52,228)', 'rgb(35,70,219)', 'rgb(43,87,211)', 'rgb(52,105,202)', 'rgb(61,123,193)', 'rgb(70,140,184)', 'rgb(79,158,175)', 'rgb(87,175,167)', 'rgb(96,193,158)', 'rgb(105,211,149)', 'rgb(114,228,140)', 'rgb(123,246,131)', 'rgb(131,246,123)', 'rgb(140,228,114)', 'rgb(149,211,105)', 'rgb(158,193,96)', 'rgb(167,175,87)', 'rgb(175,158,79)', 'rgb(184,140,70)', 'rgb(193,123,61)', 'rgb(202,105,52)', 'rgb(211,87,43)', 'rgb(219,70,35)', 'rgb(228,52,26)', 'rgb(237,35,17)', 'rgb(246,17,8)', 'rgb(255,0,0)', ]
        },
        inverse_visad: {
            colors: ['rgb(255,0,0)', 'rgb(246,17,8)', 'rgb(237,35,17)', 'rgb(228,52,26)', 'rgb(219,70,35)', 'rgb(211,87,43)', 'rgb(202,105,52)', 'rgb(193,123,61)', 'rgb(184,140,70)', 'rgb(175,158,79)', 'rgb(167,175,87)', 'rgb(158,193,96)', 'rgb(149,211,105)', 'rgb(140,228,114)', 'rgb(131,246,123)', 'rgb(123,246,131)', 'rgb(114,228,140)', 'rgb(105,211,149)', 'rgb(96,193,158)', 'rgb(87,175,167)', 'rgb(79,158,175)', 'rgb(70,140,184)', 'rgb(61,123,193)', 'rgb(52,105,202)', 'rgb(43,87,211)', 'rgb(35,70,219)', 'rgb(26,52,228)', 'rgb(17,35,237)', 'rgb(8,17,246)', 'rgb(0,0,255)', ]
        },
        wind_comps: {
            colors: ['rgb(0,0,179)', 'rgb(10,24,187)', 'rgb(20,48,194)', 'rgb(31,73,202)', 'rgb(41,97,210)', 'rgb(52,122,218)', 'rgb(62,146,226)', 'rgb(73,170,234)', 'rgb(83,195,242)', 'rgb(94,219,249)', 'rgb(64,207,243)', 'rgb(64,212,222)', 'rgb(64,218,201)', 'rgb(64,223,180)', 'rgb(64,228,159)', 'rgb(64,233,138)', 'rgb(64,238,117)', 'rgb(64,243,96)', 'rgb(64,248,75)', 'rgb(64,254,54)', ]
        },
        windspeed: {
            colors: ['rgb(0,0,250)', 'rgb(0,12,250)', 'rgb(0,25,250)', 'rgb(0,37,250)', 'rgb(0,50,250)', 'rgb(0,62,250)', 'rgb(0,75,250)', 'rgb(0,87,250)', 'rgb(0,100,250)', 'rgb(0,112,250)', 'rgb(0,125,250)', 'rgb(0,137,250)', 'rgb(0,150,250)', 'rgb(0,162,250)', 'rgb(0,175,250)', 'rgb(0,187,250)', 'rgb(0,200,250)', 'rgb(0,212,250)', 'rgb(0,225,250)', 'rgb(0,237,250)', 'rgb(0,255,255)', 'rgb(0,255,234)', 'rgb(0,255,214)', 'rgb(0,255,193)', 'rgb(0,255,173)', 'rgb(0,255,152)', 'rgb(0,255,132)', 'rgb(0,255,111)', 'rgb(0,255,91)', 'rgb(0,255,70)', 'rgb(0,255,0)', 'rgb(20,255,0)', 'rgb(41,255,0)', 'rgb(61,255,0)', 'rgb(82,255,0)', 'rgb(102,255,0)', 'rgb(123,255,0)', 'rgb(143,255,0)', 'rgb(164,255,0)', 'rgb(184,255,0)', 'rgb(255,255,0)', 'rgb(255,247,0)', 'rgb(255,240,0)', 'rgb(255,232,0)', 'rgb(255,225,0)', 'rgb(255,217,0)', 'rgb(255,210,0)', 'rgb(255,202,0)', 'rgb(255,195,0)', 'rgb(255,187,0)', 'rgb(255,180,0)', 'rgb(255,172,0)', 'rgb(255,165,0)', 'rgb(255,157,0)', 'rgb(255,150,0)', 'rgb(255,142,0)', 'rgb(255,135,0)', 'rgb(255,127,0)', 'rgb(255,120,0)', 'rgb(255,112,0)', 'rgb(255,105,0)', 'rgb(255,97,0)', 'rgb(255,90,0)', 'rgb(255,82,0)', 'rgb(255,75,0)', 'rgb(255,67,0)', 'rgb(255,60,0)', 'rgb(255,52,0)', 'rgb(255,45,0)', 'rgb(255,37,0)', ]
        },
        dbz: {
            colors: ['rgb(1,57,255)', 'rgb(0,140,255)', 'rgb(1,209,255)', 'rgb(1,255,232)', 'rgb(1,255,171)', 'rgb(1,255,79)', 'rgb(43,255,0)', 'rgb(166,255,2)', 'rgb(227,255,1)', 'rgb(255,198,0)', 'rgb(255,168,1)', 'rgb(255,145,1)', 'rgb(255,130,1)', 'rgb(255,107,0)', 'rgb(255,84,0)', 'rgb(255,7,0)', ]
        },
        dbz_nws: {
            colors: ['rgb(0,0,0)', 'rgb(0,255,255)', 'rgb(135,206,235)', 'rgb(0,0,255)', 'rgb(0,255,0)', 'rgb(50,205,50)', 'rgb(34,139,34)', 'rgb(238,238,0)', 'rgb(238,220,130)', 'rgb(238,118,33)', 'rgb(255,48,48)', 'rgb(176,48,96)', 'rgb(176,48,96)', 'rgb(186,85,211)', 'rgb(255,0,255)', 'rgb(255,255,255)', ]
        },
        topographic: {
            colors: ['rgb(25,25,255)', 'rgb(20,170,42)', 'rgb(20,170,42)', 'rgb(27,174,35)', 'rgb(35,179,28)', 'rgb(43,184,22)', 'rgb(51,188,15)', 'rgb(59,193,9)', 'rgb(67,198,2)', 'rgb(70,200,0)', 'rgb(71,199,0)', 'rgb(72,199,1)', 'rgb(73,198,1)', 'rgb(74,198,2)', 'rgb(75,197,2)', 'rgb(76,197,3)', 'rgb(78,197,3)', 'rgb(79,196,4)', 'rgb(80,196,4)', 'rgb(81,195,5)', 'rgb(82,195,5)', 'rgb(83,194,6)', 'rgb(85,194,6)', 'rgb(86,194,7)', 'rgb(87,193,7)', 'rgb(88,193,8)', 'rgb(89,192,8)', 'rgb(90,192,9)', 'rgb(92,191,9)', 'rgb(93,191,10)', 'rgb(94,191,10)', 'rgb(95,190,11)', 'rgb(96,190,11)', 'rgb(97,189,12)', 'rgb(98,189,12)', 'rgb(100,188,13)', 'rgb(101,188,13)', 'rgb(102,188,14)', 'rgb(103,187,14)', 'rgb(104,187,15)', 'rgb(105,186,15)', 'rgb(107,186,16)', 'rgb(108,185,16)', 'rgb(109,185,17)', 'rgb(110,185,17)', 'rgb(111,184,18)', 'rgb(112,184,18)', 'rgb(114,183,19)', 'rgb(115,183,19)', 'rgb(116,182,20)', 'rgb(117,182,21)', 'rgb(118,182,21)', 'rgb(119,181,22)', 'rgb(120,181,22)', 'rgb(122,180,23)', 'rgb(123,180,23)', 'rgb(124,179,24)', 'rgb(125,179,24)', 'rgb(126,179,25)', 'rgb(127,178,25)', 'rgb(129,178,26)', 'rgb(130,177,26)', 'rgb(131,177,27)', 'rgb(132,176,27)', 'rgb(133,176,28)', 'rgb(134,176,28)', 'rgb(136,175,29)', 'rgb(137,175,29)', 'rgb(138,174,30)', 'rgb(139,174,30)', 'rgb(140,173,31)', 'rgb(141,173,31)', 'rgb(143,173,32)', 'rgb(144,172,32)', 'rgb(145,172,33)', 'rgb(146,171,33)', 'rgb(147,171,34)', 'rgb(148,170,34)', 'rgb(149,170,35)', 'rgb(151,170,35)', 'rgb(152,169,36)', 'rgb(153,169,36)', 'rgb(154,168,37)', 'rgb(155,168,37)', 'rgb(156,167,38)', 'rgb(158,167,38)', 'rgb(159,167,39)', 'rgb(160,166,39)', 'rgb(161,166,40)', 'rgb(162,165,40)', 'rgb(163,165,41)', 'rgb(165,165,42)', 'rgb(165,165,42)', 'rgb(165,165,43)', 'rgb(165,165,44)', 'rgb(165,165,45)', 'rgb(166,166,46)', 'rgb(166,166,47)', 'rgb(166,166,48)', 'rgb(166,166,49)', 'rgb(166,166,50)', 'rgb(167,167,51)', 'rgb(167,167,52)', 'rgb(167,167,53)', 'rgb(167,167,54)', 'rgb(167,167,55)', 'rgb(168,168,56)', 'rgb(168,168,57)', 'rgb(168,168,58)', 'rgb(168,168,59)', 'rgb(169,169,60)', 'rgb(169,169,61)', 'rgb(169,169,62)', 'rgb(169,169,63)', 'rgb(169,169,64)', 'rgb(170,170,65)', 'rgb(170,170,66)', 'rgb(170,170,67)', 'rgb(170,170,68)', 'rgb(170,170,68)', 'rgb(171,171,69)', 'rgb(171,171,70)', 'rgb(171,171,71)', 'rgb(171,171,72)', 'rgb(172,172,73)', 'rgb(172,172,74)', 'rgb(172,172,75)', 'rgb(172,172,76)', 'rgb(172,172,77)', 'rgb(173,173,78)', 'rgb(173,173,79)', 'rgb(173,173,80)', 'rgb(173,173,81)', 'rgb(173,173,82)', 'rgb(174,174,83)', 'rgb(174,174,84)', 'rgb(174,174,85)', 'rgb(174,174,86)', 'rgb(175,175,87)', 'rgb(175,175,88)', 'rgb(175,175,89)', 'rgb(175,175,90)', 'rgb(175,175,91)', 'rgb(176,176,92)', 'rgb(176,176,93)', 'rgb(176,176,94)', 'rgb(176,176,95)', 'rgb(176,176,95)', 'rgb(177,177,96)', 'rgb(177,177,97)', 'rgb(177,177,98)', 'rgb(177,177,99)', 'rgb(178,178,100)', 'rgb(178,178,101)', 'rgb(178,178,102)', 'rgb(178,178,103)', 'rgb(178,178,104)', 'rgb(179,179,105)', 'rgb(179,179,106)', 'rgb(179,179,107)', 'rgb(179,179,108)', 'rgb(179,179,109)', 'rgb(180,180,110)', 'rgb(180,180,111)', 'rgb(180,180,112)', 'rgb(180,180,113)', 'rgb(181,181,114)', 'rgb(181,181,115)', 'rgb(181,181,116)', 'rgb(181,181,117)', 'rgb(181,181,118)', 'rgb(182,182,119)', 'rgb(182,182,120)', 'rgb(182,182,121)', 'rgb(182,182,121)', 'rgb(182,182,122)', 'rgb(183,183,123)', 'rgb(183,183,124)', 'rgb(183,183,125)', 'rgb(183,183,126)', 'rgb(184,184,127)', 'rgb(184,184,128)', 'rgb(184,184,129)', 'rgb(184,184,130)', 'rgb(184,184,131)', 'rgb(185,185,132)', 'rgb(185,185,133)', 'rgb(185,185,134)', 'rgb(185,185,135)', 'rgb(185,185,136)', 'rgb(186,186,137)', 'rgb(186,186,138)', 'rgb(186,186,139)', 'rgb(186,186,140)', 'rgb(186,186,141)', 'rgb(187,187,142)', 'rgb(187,187,143)', 'rgb(187,187,144)', 'rgb(187,187,145)', 'rgb(188,188,146)', 'rgb(188,188,147)', 'rgb(188,188,148)', 'rgb(188,188,148)', 'rgb(188,188,149)', 'rgb(189,189,150)', 'rgb(189,189,151)', 'rgb(189,189,152)', 'rgb(189,189,153)', 'rgb(189,189,154)', 'rgb(190,190,155)', 'rgb(190,190,156)', 'rgb(190,190,157)', 'rgb(190,190,158)', 'rgb(191,191,159)', 'rgb(191,191,160)', 'rgb(191,191,161)', 'rgb(191,191,162)', 'rgb(191,191,163)', 'rgb(192,192,164)', 'rgb(192,192,165)', 'rgb(192,192,166)', 'rgb(192,192,167)', 'rgb(192,192,168)', 'rgb(193,193,169)', 'rgb(193,193,170)', 'rgb(193,193,171)', 'rgb(193,193,172)', 'rgb(194,194,173)', 'rgb(194,194,174)', 'rgb(194,194,175)', 'rgb(194,194,175)', 'rgb(194,194,176)', 'rgb(195,195,177)', 'rgb(195,195,178)', 'rgb(195,195,179)', 'rgb(195,195,180)', 'rgb(195,195,181)', 'rgb(196,196,182)', 'rgb(196,196,183)', 'rgb(196,196,184)', 'rgb(196,196,185)', 'rgb(197,197,186)', 'rgb(197,197,187)', 'rgb(197,197,188)', 'rgb(197,197,189)', 'rgb(197,197,190)', 'rgb(198,198,191)', 'rgb(198,198,192)', 'rgb(198,198,193)', 'rgb(198,198,194)', 'rgb(198,198,195)', 'rgb(199,199,196)', 'rgb(199,199,197)', 'rgb(199,199,198)', 'rgb(199,199,199)', 'rgb(255,255,255)', ]
        },

    }


};

var GuiUtils = {
    getProxyUrl: function(url) {
        var base = ramaddaBaseUrl + "/proxy?trim=true&url=";
        return base + encodeURIComponent(url);
    },

    showingError: false,
    pageUnloading: false,
    handleError: function(error, extra, showAlert) {
        if (this.pageUnloading) {
            return;
        }
        console.log(error);
        if (extra) {
            console.log(extra);
        }

        if (this.showingError) {
            return;
        }
        if (showAlert) {
            this.showingError = true;
            //            alert(error);
            this.showingError = false;
        }
        closeFormLoadingDialog();
    },
    isJsonError: function(data) {
        if (data == null) {
            this.handleError("Null JSON data", null, false);
            return true;
        }
        if (data.error != null) {
            var code = data.errorcode;
            if (code == null) code = "error";
            this.handleError("Error in Utils.isJsonError:" + data.error, null, false);
            return true;
        }
        return false;
    },
    loadXML: function(url, callback, arg) {
        var req = false;
        if (window.XMLHttpRequest) {
            try {
                req = new XMLHttpRequest();
            } catch (e) {
                req = false;
            }
        } else if (window.ActiveXObject) {
            try {
                req = new ActiveXObject("Msxml2.XMLHTTP");
            } catch (e) {
                try {
                    req = new ActiveXObject("Microsoft.XMLHTTP");
                } catch (e) {
                    req = false;
                }
            }
        }
        if (req) {
            req.onreadystatechange = function() {
                if (req.readyState == 4 && req.status == 200) {
                    callback(req, arg);
                }
            };
            req.open("GET", url, true);
            req.send("");
        }
    },
    loadHtml: async function(url, callback) {
        await $.get(url, function(data) {
                callback(data);
            })
            .done(function() {})
            .fail(function() {
                console.log("Failed to load url: " + url);
            });
    },
    loadUrl: function(url, callback, arg) {
        var req = false;
        if (window.XMLHttpRequest) {
            try {
                req = new XMLHttpRequest();
            } catch (e) {
                req = false;
            }
        } else if (window.ActiveXObject) {
            try {
                req = new ActiveXObject("Msxml2.XMLHTTP");
            } catch (e) {
                try {
                    req = new ActiveXObject("Microsoft.XMLHTTP");
                } catch (e) {
                    req = false;
                }
            }
        }
        if (req) {
            req.onreadystatechange = function() {
                if (req.readyState == 4 && req.status == 200) {
                    callback(req, arg);
                }
            };
            req.open("GET", url, true);
            req.send("");
        }
    },
    getUrlArg: function(name, dflt) {
        name = name.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
        var regexS = "[\\?&]" + name + "=([^&#]*)";
        var regex = new RegExp(regexS);
        var results = regex.exec(window.location.href);
        if (results == null || results == "")
            return dflt;
        else
            return results[1];
    },
    setCursor: function(c) {
        var cursor = document.cursor;
        if (!cursor && document.getElementById) {
            cursor = document.getElementById('cursor');
        }
        if (!cursor) {
            document.body.style.cursor = c;
        }
    },
    getDomObject: function(name) {
        obj = new DomObject(name);
        if (obj.obj) return obj;
        return null;
    },
    getEvent: function(event) {
        if (event) return event;
        return window.event;
    },
    getEventX: function(event) {
        if (event.pageX) {
            return event.pageX;
        }
        return event.clientX + document.body.scrollLeft +
            document.documentElement.scrollLeft;
    },
    getEventY: function(event) {
        if (event.pageY) {
            return event.pageY;
        }
        return event.clientY + document.body.scrollTop +
            document.documentElement.scrollTop;

    },

    getTop: function(obj) {
        if (!obj) return 0;
        return obj.offsetTop + this.getTop(obj.offsetParent);
    },


    getBottom: function(obj) {
        if (!obj) return 0;
        return this.getTop(obj) + obj.offsetHeight;
    },


    setPosition: function(obj, x, y) {
        obj.style.top = y;
        obj.style.left = x;
    },

    getLeft: function(obj) {
        if (!obj) return 0;
        return obj.offsetLeft + this.getLeft(obj.offsetParent);
    },
    getRight: function(obj) {
        if (!obj) return 0;
        return obj.offsetRight + this.getRight(obj.offsetParent);
    },

    getStyle: function(obj) {
        if (obj.style) return obj.style;
        if (document.layers) {
            return document.layers[obj.name];
        }
        return null;
    },
    //from http://snipplr.com/view.php?codeview&id=5949
    size_format: function(filesize) {
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
        var value = $("#" + domId).val();
        if (value == null) return true;
        if (doMin) {
            if (value.length < length) {
                closeFormLoadingDialog();
                return false;
            }
        } else {
            if (value.length > length) {
                closeFormLoadingDialog();
                return false;
            }
        }
        return true;
    },
    inputValueOk: function(domId, rangeValue, min) {
        var value = $("#" + domId).val();
        if (value == null) return true;
        if (min && value < rangeValue) {
            closeFormLoadingDialog();
            return false;
        }
        if (!min && value > rangeValue) {
            closeFormLoadingDialog();
            return false;
        }
        return true;
    },
    inputIsRequired: function(domId, rangeValue, min) {
        var value = $("#" + domId).val();
        if (value == null || value.trim().length == 0) {
            closeFormLoadingDialog();
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
var TAG_UL = "ul";
var TAG_OL = "ol";

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


var HtmlUtils = {
    tabLoaded: function(event, ui) {
        if (window["ramaddaDisplayCheckLayout"]) {
            ramaddaDisplayCheckLayout();
        }
        if(window["ramaddaMapCheckLayout"]) {
            ramaddaMapCheckLayout();
        }
    },
    getErrorDialog: function(msg) {
        return "<div class=\"ramadda-message\"><table><tr valign=top><td><div class=\"ramadda-message-link\"><img border=\"0\"  src=\"/repository/icons/error.png\"  /></div></td><td><div class=\"ramadda-message-inner\">" + msg + "</div></td></tr></table></div>";
    },
    getAceEditor: function(id) {
        if (!this.aceEditors) return null;
        var info = this.aceEditors[id];
        if (!info) return null;
        return info.editor;
    },
    handleAceEditorSubmit: function() {
        if (!this.aceEditors) return;
        for (a in this.aceEditors) {
            var info = this.aceEditors[a];
            console.log("hidden:" + info.hidden + " " + $("#" + info.hidden).size());
            $("#" + info.hidden).val(info.editor.getValue());
        }
    },
    initAceEditor: function(formId, id, hidden) {
        if (!this.aceEditors) {
            this.aceEditors = {};
        }
        var info = {};
        this.aceEditors[id] = info;
        info.editor = ace.edit(id);
        info.formId = formId;
        info.hidden = hidden;
        info.editor.setKeyboardHandler("emacs");
        info.editor.setShowPrintMargin(false);
        info.editor.getSession().setUseWrapMode(true);
        info.editor.setOptions({
            autoScrollEditorIntoView: true,
            copyWithEmptySelection: true,
        });
        info.editor.session.setMode("ace/mode/ramadda");
        return info.editor;
    },
    makeBreadcrumbs: function(id) {
        jQuery("#" + id).jBreadCrumb({
            previewWidth: 10,
            easing: 'swing',
            //(sic)
            beginingElementsToLeaveOpen: 0,
            endElementsToLeaveOpen: 4
        });
    },
    tooltipInit: function(openerId, id) {
        $("#" + id).dialog({
            autoOpen: false,
            //                draggable: true,
            minWidth: 650,
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

        $("#" + openerId).click(function() {
            $("#" + id).dialog("open");
        });
    },
    join: function(items, separator) {
        var html = "";
        for (var i = 0; i < items.length; i++) {
            if (i > 0 & separator != null) html += separator;
            html += items[i];
        }
        return html;
    },
    qt: function(value) {
        return "\"" + value + "\"";
    },
    sqt: function(value) {
        return "\'" + value + "\'";
    },
    getUniqueId: function() {
        var cnt = window["uniqueCnt"]++;
        return "id_" + cnt;
    },
    inset: function(html, top, left, bottom, right) {
        var attrs = [];
        var style = "";
        if (top) {
            style += "padding-top: " + top + "px; ";
        }
        if (left) {
            style += "padding-left: " + left + "px; ";
        }
        if (bottom) {
            style += "padding-bottom: " + bottom + "px; ";
        }
        if (right) {
            style += "padding-right: " + right + "px; ";
        }
        return HtmlUtils.div(["style", style], html);

    },
    makeAccordian: function(id, args) {
        if (!Utils.isDefined(args)) args = {};
        if (!Utils.isDefined(args.active)) args.active = 0;
        $(function() {
            //We initially hide the accordian contents
            //Show all contents
            var contents = $(".ramadda-accordian-contents");
            contents.css("display", "block");
            var ctorArgs = {
                autoHeight: false,
                navigation: true,
                collapsible: true,
                heightStyle: "content",
                active: args.active
            }
            $(id).accordion(ctorArgs);
        });
    },
    hbox: function() {
        var row = HtmlUtils.openTag("tr", ["valign", "top"]);
        row += "<td>";
        row += HtmlUtils.join(arguments, "</td><td>");
        row += "</td></tr>";
        return this.tag("table", ["border", "0", "cellspacing", "0", "cellpadding", "0"],
            row);
    },
    leftRight: function(left, right, leftWeight, rightWeight) {
        if (leftWeight == null) leftWeight = "6";
        if (rightWeight == null) rightWeight = "6";
        return this.div(["class", "row"],
            this.div(["class", "col-md-" + leftWeight], left) +
            this.div(["class", "col-md-" + rightWeight, "style", "text-align:right;"], right));
    },
    leftCenterRight: function(left, center, right, leftWidth, centerWidth, rightWidth, attrs) {
        if (!attrs) attrs = {};
        if (!attrs.valign) attrs.valign = "top";
        if (leftWidth == null) leftWidth = "33%";
        if (centerWidth == null) centerWidth = "33%";
        if (rightWidth == null) rightWidth = "33%";

        return this.tag("table", ["border", "0", "width", "100%", "cellspacing", "0", "cellpadding", "0"],
            this.tr(["valign", attrs.valign],
                this.td(["align", "left", "width", leftWidth], left) +
                this.td(["align", "center", "width", centerWidth], center) +
                this.td(["align", "right", "width", rightWidth], right)));
    },

    leftRightTable: function(left, right, leftWidth, rightWidth, attrs) {
        if (!attrs) attrs = {};
        if (!attrs.valign) attrs.valign = "top";
        var leftAttrs = ["align", "left"];
        var rightAttrs = ["align", "right"];
        if (leftWidth) {
            leftAttrs.push("width");
            leftAttrs.push(leftWidth);
        }
        if (rightWidth) {
            rightAttrs.push("width");
            rightAttrs.push(rightWidth);
        }
        return this.tag("table", ["border", "0", "width", "100%", "cellspacing", "0", "cellpadding", "0"],
            this.tr(["valign", attrs.valign],
                this.td(leftAttrs, left) +
                this.td(rightAttrs, right)));
    },

    heading: function(html) {
        return this.tag("h3",[], html);
    },
    bootstrapClasses:["col-md-12","col-md-6","col-md-4", "col-md-4","col-md-4","col-md-2"],
    getBootstrapClass: function(cols) {
        cols -= 1;
        cols = Math.max(cols,0);
        if(cols<this.bootstrapClasses.length) {
            return this.bootstrapClasses[cols];
        }
        return "col-md-1";
    },
    pre: function(attrs, inner) {
        return this.tag("pre", attrs, inner);
    },
    div: function(attrs, inner) {
        return this.tag("div", attrs, inner);
    },
    center: function(inner) {
        return this.tag("center", attrs, inner);
    },
    span: function(attrs, inner) {
        return this.tag("span", attrs, inner);
    },
    image: function(path, attrs) {
        return "<img " + this.attrs(["src", path, "border", "0"]) + " " + this.attrs(attrs) + ">";
    },
    tr: function(attrs, inner) {
        return this.tag("tr", attrs, inner);
    },
    td: function(attrs, inner) {
        return this.tag("td", attrs, inner);
    },
    tds: function(attrs, cols) {
        var html = "";
        for (var i = 0; i < cols.length; i++) {
            html += this.td(attrs, cols[i]);
        }
        return html;
    },
    formatTable: function(id, args) {
        $(id).each(function() {
            var options = {
                paging: false,
                ordering: false,
                info: false,
                searching: false,
                scrollCollapse: true,
            };
            if (args)
                $.extend(options, args);
            var height = $(this).attr("table-height");
            if (height)
                options.scrollY = height;
            var ordering = $(this).attr("table-ordering");
            if (ordering)
                options.ordering = (ordering == "true");
            var searching = $(this).attr("table-searching");
            if (searching)
                options.searching = (searching == "true");
            var paging = $(this).attr("table-paging");
            if (paging)
                options.paging = (paging == "true");
            if (Utils.isDefined(options.scrollY)) {
                var sh = "" + options.scrollY;
                if (!sh.endsWith("px")) options.scrollY += "px";
            }
            $(this).DataTable(options);
        });
    },
    th: function(attrs, inner) {
        return this.tag("th", attrs, inner);
    },
    setFormValue: function(id, val) {
        $("#" + id).val(val);
    },
    setHtml: function(id, val) {
        $("#" + id).html(val);
    },
    formTable: function() {
        return this.openTag("table", ["class", "formtable", "cellspacing", "0", "cellspacing", "0"]);
    },
    formTableClose: function() {
        return this.closeTag("table");
    },
    formEntryTop: function(label, value) {
        return this.tag("tr", ["valign", "top"],
            this.tag("td", ["class", "formlabel", "align", "right"],
                label) +
            this.tag("td", [],
                value));

    },
    formEntry: function(label, value) {
        return this.tag("tr", [],
            this.tag("td", ["class", "formlabel", "align", "right"],
                label) +
            this.tag("td", [],
                value));

    },
    appendArg: function(url, arg, value) {
        if (url.indexOf("?") < 0) {
            url += "?";
        } else {
            url += "&";
        }
        url += HtmlUtils.urlArg(arg, value);
        return url;
    },
    getUrl: function(path, args) {
        if (args.length > 0) {
            path += "?";
        }
        for (var i = 0; i < args.length; i += 2) {
            if (i > 0) {
                path += "&";
            }
            path += this.urlArg(args[i], args[i + 1]);
        }
        return path;
    },
    b: function(inner) {
        return this.tag("b", [], inner);
    },

    td: function(attrs, inner) {
        return this.tag("td", attrs, inner);
    },

    tag: function(tagName, attrs, inner) {
        if (!inner && (typeof attrs) == "string") {
            inner = attrs;
            attrs = null;
        }
        var html = "<" + tagName + " " + this.attrs(attrs) + ">";
        if (inner != null) {
            html += inner;
        }
        html += "</" + tagName + ">";
        return html;
    },
    openTag: function(tagName, attrs) {
        var html = "<" + tagName + " " + this.attrs(attrs) + ">";
        return html;
    },
    openRow: function() {
        return this.openTag("div", ["class", "row"]);
    },
    closeRow: function() {
        return this.closeTag("div");
    },

    openDiv: function(attrs) {
        return this.openTag("div", attrs);
    },
    closeDiv: function() {
        return this.closeTag("div");
    },

    closeTag: function(tagName) {
        return "</" + tagName + ">\n";
    },
    urlArg: function(name, value) {
        return name + "=" + encodeURIComponent(value);
    },
    attr: function(name, value) {
        return " " + name + "=" + this.qt(value) + " ";
    },

    attrs: function(list) {
        if (!list) return "";
        var html = "";
        if (list == null) return html;
        if (list.length == 1) return list[0];
        for (var i = 0; i < list.length; i += 2) {
            var name = list[i];
            if (!name) continue;
            var value = list[i + 1];
            if (value == null) {
                html += " " + name +" ";
            } else {
                html += this.attr(name, value);
            }

        }
        return html;
    },
    styleAttr: function(s) {
        return this.attr("style", s);
    },

    classAttr: function(s) {
        return this.attr("class", s);
    },

    loadedGoogleCharts: false,
    loadGoogleCharts: function() {
        if (this.loadedGoogleCharts) {
            return;
        }
        this.loadedGoogleCharts = true;

        google.charts.load("43", {
            packages: ['corechart', 'calendar', 'table', 'bar', 'treemap', 'sankey', 'wordtree', 'timeline', 'gauge']
        });
    },

    idAttr: function(s) {
        return this.attr("id", s);
    },
    link: function(url, label, attrs) {
        if (attrs == null) attrs = [];
        var a = [];
        for (i in attrs)
            a.push(attrs[i]);
        attrs = a;
        attrs.push("url");
        attrs.push(url);
        attrs.push("class");
        attrs.push("ramadda-link");
        if (attrs.style) attrs.style += "display:inline-block;";
        else attrs.style = "style='display:inline-block;'";
        return this.div(attrs, label);
    },
    getEntryImage: function(entryId, tag) {
        var index = tag.indexOf("::");
        if(index>=0) {
            var toks = tag.split("::");
            if(toks[0].trim().length>0) {
                entryId = toks[0].trim();
            }
            var attach = toks[1].trim();
            return ramaddaBaseUrl +"/metadata/view/" + attach +"?entryid=" + entryId;
        }
        return tag;
    },
    href: function(url, label, attrs) {
        if (attrs == null) attrs = [];
        var a = [];
        for (i in attrs)
            a.push(attrs[i]);
        attrs = a;
        attrs.push("href");
        attrs.push(url);
        if (!Utils.isDefined(label) || label == "") label = url;
        return this.tag("a", attrs, label);
    },

    onClick: function(call, html, attrs) {
        var myAttrs = ["onclick", call, "style", "color:black;   cursor:pointer;"];
        if (attrs != null) {
            for (var i = 0; i < attrs.length; i++) {
                myAttrs.push(attrs[i]);
            }
        }
        return this.tag("a", myAttrs, html);
    },

    checkbox: function(id, attrs, checked) {
        attrs.push("id");
        attrs.push(id);
        attrs.push("type");
        attrs.push("checkbox");
        attrs.push("value");
        attrs.push("true");
        if (checked) {
            attrs.push("checked");
            attrs.push(null);
        }
        return this.tag("input", attrs);
    },

    radio: function(id, name, radioclass, value, checked, extra) {
        if(!extra) extra = "";
        var html = "<input id='" + id + "'  class='" + radioclass + "' name='" + name + "' type=radio value='" + value + "' ";
        if (checked) {
            html += " checked ";
        }
        html+=" " + extra
        html += "/>";
        return html;
    },


    handleFormChangeShowUrl: function(entryid, formId, outputId, skip, hook) {
        if (skip == null) {
            skip = [".*OpenLayers_Control.*", "authtoken"];
        }
        var url = $("#" + formId).attr("action") + "?";
        var inputs = $("#" + formId + " :input");
        var cnt = 0;
        var seen = {};
        var pairs = [];
        inputs.each(function(i, item) {
            if (item.name == "" || item.value == null || item.value == "") return;
            if (item.value == "-all-") return;
            if (item.type == "submit") {
                if (item.value == "Cancel") {
                    return
                }
            }

            if (item.type == "checkbox") {
                if (!item.checked) {
                    return;
                }
            }
            if (item.type == "radio") {
                if (!item.checked) {
                    return;
                }
            }
            if (item.attributes && item.attributes["default"]) {
                if (item.attributes["default"].value == item.value) {
                    return;
                }
            }
            if (item.name && seen[item.name]) {
                return;
            }
            if (item.name) {
                seen[item.name] = true;
            }

            if (skip != null) {
                for (var i = 0; i < skip.length; i++) {
                    var pattern = skip[i];
                    if (item.name.match(pattern)) {
                        return;
                    }
                }
            }
            //                console.log("item:"   + item.id +" type:" +item.type + " value:" + item.value);
            var values = [];
            if (item.type == "select-multiple" && item.selectedOptions) {
                for (a in item.selectedOptions) {
                    option = item.selectedOptions[a];
                    if (Utils.isDefined(option.value)) {
                        values.push(option.value);
                    }
                }
            } else {
                values.push(item.value);
            }

            for (v in values) {
                if (cnt > 0) url += "&";
                cnt++;
                value = values[v];
                pairs.push({
                    item: item,
                    value: value
                });
                url += encodeURIComponent(item.name) + "=" + encodeURIComponent(value);
            }
        });

        var base = window.location.protocol + "//" + window.location.host;
        url = base + url;
        var input = HtmlUtils.input("formurl", url, ["size", "80"]);
        var html = HtmlUtils.div(["class", "ramadda-form-url"], HtmlUtils.href(url, HtmlUtils.image(ramaddaBaseUrl + "/icons/link.png")) + " " + input);
        if (hook) {
            html += hook({
                entryId: entryid,
                formId: formId,
                inputs: inputs,
                itemValuePairs: pairs
            });
        }
        $("#" + outputId).html(html);
    },
    makeUrlShowingForm: function(entryId, formId, outputId, skip, hook) {
        $("#" + formId + " :input").change(function() {
            HtmlUtils.handleFormChangeShowUrl(entryId, formId, outputId, skip, hook);
        });
        HtmlUtils.handleFormChangeShowUrl(entryId, formId, outputId, skip, hook);
    },
    input: function(name, value, attrs) {
        return "<input " + HtmlUtils.attrs(attrs) + HtmlUtils.attrs(["name", name, "value", value]) + ">";
    },
    textarea: function(name, value, attrs) {
        return "<textarea " + HtmlUtils.attrs(attrs) + HtmlUtils.attrs(["name", name]) + ">" + value + "</textarea>";
    },
    initSelect: function(s) {
        $(s).selectBoxIt({});
    },
    valueDefined: function(value) {
        if (value != "" && value.indexOf("--") != 0) {
            return true;
        }
        return false;
    },


    squote: function(s) {
        return "'" + s + "'";
    },
    toggleBlock: function(label, contents, visible) {
        var id = "block_" + (uniqueCnt++);
        var imgid = id + "_img";

        var img1 = ramaddaBaseUrl + "/icons/togglearrowdown.gif";
        var img2 = ramaddaBaseUrl + "/icons/togglearrowright.gif";
        var args = HtmlUtils.join([HtmlUtils.squote(id), HtmlUtils.squote(imgid), HtmlUtils.squote(img1), HtmlUtils.squote(img2)], ",");
        var click = "toggleBlockVisibility(" + args + ");";

        var header = HtmlUtils.div(["class", "entry-toggleblock-label", "onClick", click],
            HtmlUtils.image((visible ? img1 : img2), ["align", "bottom", "id", imgid]) +
            " " + label);
        var style = (visible ? "display:block;visibility:visible" : "display:none;");
        var body = HtmlUtils.div(["class", "hideshowblock", "id", id, "style", style],
            contents);
        return header + body;
    }
}


var StringUtil = {
    endsWith: function(str, suffix) {
        return (str.length >= suffix.length) &&
            (str.lastIndexOf(suffix) + suffix.length == str.length);
    },
    startsWith: function(str, prefix) {
        return (str.length >= prefix.length) &&
            (str.lastIndexOf(prefix, 0) === 0);
    }
}






var blockCnt = 0;

function DomObject(name) {
    this.obj = null;
    // DOM level 1 browsers: IE 5+, NN 6+
    if (document.getElementById) {
        this.obj = document.getElementById(name);
        if (this.obj)
            this.style = this.obj.style;
    }
    // IE 4
    else if (document.all) {
        this.obj = document.all[name];
        if (this.obj)
            this.style = this.obj.style;
    }
    // NN 4
    else if (document.layers) {
        this.obj = document.layers[name];
        this.style = document.layers[name];
    }
    if (this.obj) {
        this.id = this.obj.id;
        if (!this.id) {
            this.id = "obj" + (blockCnt++);
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
        parent.getThis = function() {
            return object;
        }
        object.getThis = function() {
            return object;
        }
        object.mysuper = parent;
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
    window.onbeforeunload = confirmExit;
}

function confirmExit() {
    return null;
}

$.widget("custom.iconselectmenu", $.ui.selectmenu, {
    _renderItem: function(ul, item) {
        var li = $("<li>"),
            //wrapper = $( "<div>", { text: item.label } );
            wrapper = $("<span>");

        if (item.disabled) {
            li.addClass("ui-state-disabled");
        }

        $("<img>", {
            style: item.element.attr("data-style"),
            "class": "ui-icon " + item.element.attr("data-class"),
            "src": item.element.attr("img-src")
        }).appendTo(wrapper);

        wrapper.append(item.label);

        return li.append(wrapper).appendTo(ul);
    }
});


window.onbeforeunload = pageIsUnloading;

var HtmlUtil = HtmlUtils;


/*
msg = "red_white_blue:["
    for(i=Utils.ColorTables.blue_white_red.length-1;i>=0;i--) {
    msg +=Utils.ColorTables.blue_white_red[i]+",";
}
msg+="]"
    //    console.log(msg);
    */


function Div(contents) {
    this.id = HtmlUtils.getUniqueId();
    this.contents  = contents || "";
    this.extra = "";
    this.toString = function() {
        return HtmlUtils.div(["id",this.id],this.contents);
    }
    this.set = function(html, attrs) {
        if(attrs) this.extra = HtmlUtils.attrs(attrs);
        this.contents = html;
        $("#"+this.id).html(html);
        return this;
    }
    this.append = function(html) {
        if(!this.content) this.content = html;
        else this.content += html;
        $("#"+this.id).append(html);
    }
    this.msg = function(msg) {
        return this.set(HtmlUtils.div([ATTR_CLASS, "display-message"], msg));
    }

}


(function ($, undefined) {
    $.fn.getCursorPosition = function() {
        var el = $(this).get(0);
        var pos = 0;
        if('selectionStart' in el) {
            pos = el.selectionStart;
        } else if('selection' in document) {
            el.focus();
            var Sel = document.selection.createRange();
            var SelLength = document.selection.createRange().text.length;
            Sel.moveStart('character', -el.value.length);
            pos = Sel.text.length - SelLength;
        }
        return pos;
    }
})(jQuery);

