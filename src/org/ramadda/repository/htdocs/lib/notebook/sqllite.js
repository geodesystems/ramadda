
var SqlLite = {
    loaded:false,
    dbs: {},
    isPluginReady: function() {
        return window.SQL;
    },
    evaluate: function(sql, chunk) {
        var dbid = chunk.props["db"]||"defaultdb";
        if(!this.dbs[dbid]) {
            this.dbs[dbid] = new window.SQL.Database();
        }
        
        var db = this.dbs[dbid];
        var res = db.exec(sql);
        if(!res || !Array.isArray(res) || res.length==0) return null;
        var columns = res[0].columns;
        var values = res[0].values;
        if(!columns || !values) return null;

        var id = HU.getUniqueId();
        var table = HU.open(TAG_TABLE,
			    [ATTR_ID,id, ATTR_WIDTH, "100%", ATTR_CLASS, "stripe hover "]) +
	    HU.openTag(TAG_THEAD, []);
        columns.forEach(v=>{table+=HU.tag(TAG_TH,[],Utils.makeLabel(v))});
        table += HU.close(TAG_TR,TAG_THEAD);
        table += HU.open(TAG_TBODY);
        values.forEach(row=>{
            table+=HU.open(TAG_TR);
            row.forEach(v=>{table+=HU.tag(TAG_TD,[],v)});
            table+=HU.close(TAG_TR);
        });
        table+HU.close(TAG_TR);
        table += HU.close(TAG_TBODY, TAG_TABLE);
        notebook.write(table);
        //        HU.formatTable("#" + id, {scrollY: "300"});
        var options = {};
        if(values.length>10) options.scrollY = 300;
        HU.formatTable("#" + id, options);

        return null;
    }
}
Utils.importJS("/repository/lib/notebook/sql.js",null, (jqxhr, settings, exc)=>console.log("error loading sql.js:" + exc));

