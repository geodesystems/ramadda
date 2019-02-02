
<style type="text/css">
label {
     text-indent:0px;
 }
</style>


<div style="width:100%;height:800px;">
<div id="tui-image-editor-container" style="width:100%;height:500px;"></div>
</div>

<script type="text/javascript" src="https://cdnjs.cloudflare.com/ajax/libs/fabric.js/1.6.7/fabric.js"></script>
<script type="text/javascript" src="https://uicdn.toast.com/tui.code-snippet/v1.5.0/tui-code-snippet.min.js"></script>
<script type="text/javascript" src="https://cdnjs.cloudflare.com/ajax/libs/FileSaver.js/1.3.3/FileSaver.min.js"></script>
<script type="text/javascript" src="https://uicdn.toast.com/tui-color-picker/v2.2.0/tui-color-picker.js"></script>

<link rel='stylesheet' href='https://uicdn.toast.com/tui-color-picker/v2.2.0/tui-color-picker.css'>
<link rel='stylesheet' href='https://uicdn.toast.com/tui-image-editor/latest/tui-image-editor.css'>
<script src='https://uicdn.toast.com/tui-image-editor/latest/tui-image-editor.js'></script>
     
     

<script LANGUAGE="JavaScript">
         // Image editor
         var imageEditor = new tui.ImageEditor('#tui-image-editor-container', {
             includeUI: {
                 loadImage: {
                     path: '${imageurl}',
                     name: '${imagename}',
                 },
                 initMenu: 'filter',
                 menuBarPosition: 'bottom'
             },
             cssMaxWidth: 700,
             cssMaxHeight: 500
         });

         window.onresize = function() {
             imageEditor.ui.resizeEditor();
         }


        function imageEditorSave()  {
            var form = $("#imageeditform");
            var contents = $("#imagecontents");
            var entryid =form.children(":input[name=entryid]").val();
            message.html("Saving image...");
            var image = imageEditor.toDataURL();
            var message  = $("#imageeditor_message");
            var data = new FormData(form[0]);
            data.append("imagecontents", image);
            $.ajax({
                    type: "POST",
                        enctype: 'multipart/form-data',
                        url: form.attr('action'),
                        data: data,
                        processData: false,
                        contentType: false,
                        cache: false,
                        timeout: 600000,
                        success: function (data) {
                    try {
                        var result = JSON.parse(data);
                        if(result.code == 'error') 
                            message.html("<span class='ramadda-error-label'>" + result.message+"</span>");
                        else
                            message.html(result.message);
                    } catch(e) {
                           console.log(data);
                           message.html("Error saving image:"+ e);
                    }
                    },
                        error: function (e) {
                       try {
                           var result = JSON.parse(data.responseText);
                           message.html(result);
                       } catch(e) {
                           console.log("bad result:" + data);
                           message.html("Error:"+ e);
                       }
                    }
                });
            return;
        }


        function imageEditorUndo()  {
            var form = $("#imageeditform");
            var entryid =form.children(":input[name=entryid]").val();
            var message  = $("#imageeditor_message");
            console.log(entryid +" " + form.attr('action') +" " + message.size());
            $.post(form.attr("action"), { entryid: entryid, image_undo: "true"} ) .done(function(data) {
                    try {
                        var result = JSON.parse(data);
                        if(result.code == 'error') 
                            message.html("<span class='ramadda-error-label'>" + result.message+"</span>");
                        else {
                            window.location.href = form.attr('action')+"?entryid=" + entryid;
                            return;
                        }
                        imageEditor.loadImageFromURL('${imageurl}','${imagename}');
                    } catch(e) {
                           console.log(data);
                           message.html("Error saving image:"+ e);
                       }
                })
                .fail(function(data) {

                    });
        }


</script>