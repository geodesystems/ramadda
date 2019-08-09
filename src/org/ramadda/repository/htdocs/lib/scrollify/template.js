$(function() {
	$.scrollify({
		section:".panel",
		    scrollbars:false,
		    before:function(i,panels) {
		    var ref = panels[i].attr("data-section-name");
		    $(".pagination .active").removeClass("active");
		    $(".pagination").find("a[href=\"#" + ref + "\"]").addClass("active");
		},
		    afterRender:function() {
		    var pagination = "<ul class=\"pagination\">";
		    var activeClass = "";
		    $(".panel").each(function(i) {
			    activeClass = "";
			    if(i===0) {
				activeClass = "active";
			    }
			    var name  = $(this).attr("data-section-name");
			    if(!name) name = " ";
			    var nameSlice = "";
			    if(name)
				nameSlice = name.slice(1);

			    pagination += "<li><a class=\"" + activeClass + "\" href=\"#" + name + "\"><span class=\"hover-text\">" + name.charAt(0).toUpperCase() + nameSlice + "</span></a></li>";
			});
		    pagination += "</ul>";
		    $(".panel-first").append(pagination);
		    $(".pagination a").on("click",$.scrollify.move);
		}
	    });
    });
