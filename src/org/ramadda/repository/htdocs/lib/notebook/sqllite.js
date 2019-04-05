
var SqlLite = {
    loaded:false,
    dbs: {},
    isReady: function() {
        return window.SQL;
    },
    evaluate: function(sql, chunk) {
        var dbid = chunk.props["db"]||"defaultdb";
        if(!this.dbs[dbid]) {
            this.dbs[dbid] = new SQL.Database();
        }
        
        var db = this.dbs[dbid];
        var res = db.exec(sql);
        if(!res || !Array.isArray(res) || res.length==0) return null;
        var columns = res[0].columns;
        var values = res[0].values;
        if(!columns || !values) return null;

        var id = HtmlUtils.getUniqueId();
        var table = HtmlUtils.openTag("table", ["id",id, "width", "100%", "class", "stripe hover "]) + HtmlUtils.openTag("thead", []);
        columns.map(v=>table+="<th>"+Utils.makeLabel(v)+"</th>");
        table+"</tr>";
        table += HtmlUtils.closeTag("thead");
        table += HtmlUtils.openTag("tbody");
        values.map(row=>{
                table+="<tr>";
                row.map(v=>table+="<td>"+v+"</td>");
                table+="</tr>";
            });
        table+"</tr>";
        table += HtmlUtils.closeTag("tbody");
        table += HtmlUtils.closeTag("table");
        notebook.write(table);
        //        HtmlUtils.formatTable("#" + id, {scrollY: "300"});
        var options = {};
        if(values.length>10) options.scrollY = 300;
        HtmlUtils.formatTable("#" + id, options);

        return null;
    }
}
    Utils.importJS("/repository/lib/notebook/sql.js",null, (jqxhr, settings, exc)=>console.log("error loading sql.js:" + exc));

