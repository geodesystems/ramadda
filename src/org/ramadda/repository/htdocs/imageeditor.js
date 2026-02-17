
function RamaddaImageEditor(imageUrl, imageName, numberOfVersions) {
    this.numberOfVersions = numberOfVersions;
    this.imageUrl = imageUrl;
    this.imageName = imageName;
    let args = {
        usageStatistics: false,
        includeUI: {
            initMenu: 'filter',
            menuBarPosition: 'bottom'
        },
        cssMaxWidth: 700,
        cssMaxHeight: 500
    }
    if(imageUrl) {
        args.includeUI.loadImage =  {
            path: this.imageUrl,
            name: this.imageName,
        };
    }

    this.imageEditor = new tui.ImageEditor('#tui-image-editor-container', args);

    window.onresize = () => {
        this.imageEditor.ui.resizeEditor();
    }
}

RamaddaImageEditor.prototype = {
    undoCounts: 0,

    imageEditorMessage:function(msg) {
        this.getMessage().html(msg);
	setTimeout(() =>{
            this.getMessage().html('');
	},3000);
    },


    getMessage:function() {
        return  $("#imageeditor_message");
    },
    getForm:function() {
	return  $("#imageeditform");
    },
    imageEditorSave:function() {
	let imageEditor = this.imageEditor;
        if (!imageEditor) return;
        var form = this.getForm();
        if (form.length == 0) return;
        var contents = $("#imagecontents");
        var entryid = form.children(":input[name=entryid]").val();
        this.imageEditorMessage("Saving image...");
	setTimeout(()=>{this.imageEditorSaveInner()}, 1);
    },

    imageEditorSaveInner:function() {
        let image = this.imageEditor.toDataURL();
	let form = this.getForm();
        let data = new FormData(form[0]);
        data.append("imagecontents", image);
        console.log("saving image");
        $.ajax({
            type: "POST",
            enctype: 'multipart/form-data',
            url: form.attr('action'),
            data: data,
            processData: false,
            contentType: false,
            cache: false,
            timeout: 600000,
            success: (data) => {
                try {
                    var result = JSON.parse(data);
                    if (result.code == 'error')
                        this.imageEditorMessage("<span class='ramadda-error-label'>" + result.message + "</span>");
                    else {
                        this.imageEditorMessage(result.message);
                        this.deltaVersions(1);
                    }
                } catch (e) {
                    console.log(data);
                    this.imageEditorMessage("Error saving image:" + e);
                }
            },
            error: (e) => {
                try {
                    var result = JSON.parse(data.responseText);
                    this.imageEditorMessage(result);
                } catch (e) {
                    console.log("bad result:" + data);
                    this.imageEditorMessage("Error:" + e);
                }
            }
        });
    },

    deltaVersions:    function(d) {
        this.numberOfVersions += d;
        if (this.numberOfVersions < 0)
            this.numberOfVersions = 0;
        if (this.numberOfVersions == 0)
            $("#imageversions").html("");
        else
            $("#imageversions").html(this.numberOfVersions + " version" + (this.numberOfVersions > 1 ? "s " : ""));
    },

    imageEditorUndo:function() {
        var form = $("#imageeditform");
        if (form.length == 0) return;
        var entryid = form.children(":input[name=entryid]").val();
        $.post(form.attr("action"), {
            entryid: entryid,
            image_undo: "true"
        }).done((data) =>{
            try {
                var result = JSON.parse(data);
                if (result.code == 'error')
                    this.imageEditorMessage("<span class='ramadda-error-label'>" + result.message + "</span>");
                else {
                    this.deltaVersions(-1);
                    this.imageEditorMessage("");
                    this.imageEditor.loadImageFromURL(this.imageUrl+'&undo=' + (this.undoCounts++), this.imageName);
                }
            } catch (e) {
                console.log(data);
                this.imageEditorMessage("Error saving image:" + e);
            }
        }).fail(function(data) {
        });
    }

}
