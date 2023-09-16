

$.getJSON('https://api.earthsystemdatalab.net/api/colorbars', data=>{
    let cid = HU.getUniqueId("canvas_");
    let w = 256;
    let h=20;
    let c =  HU.tag("canvas",[CLASS,"", STYLE,"border:1px solid red;", 	
			      WIDTH,w,HEIGHT,h,ID,cid]);
    $(document.body).append(c);
    let canvas = document.getElementById(cid);
    let ctx = canvas.getContext("2d");
    let cts=[];
    let cat;
    let images= [];
    data.forEach(coll=>{
	cat=coll[0];
	coll[2].forEach(c=>{
	    if(c[0].indexOf('alpha')>0) return;
	    images.push({cat:cat,name:c[0],image:c[1]});
	})});
			
    let idx=0;
    let js = 'let esdlColorTables=[\n';
    cat='';
    let makeImage = ()=>{
	let image=images[idx++];
	if(!image) {
	    js+='];\nUtils.addColorTables(esdlColorTables);\n';
	    Utils.makeDownloadFile('esdlcolortables.js',js);
	    return;
	}
	if(image.cat!=cat) {
	    cat = image.cat;
	    let id = Utils.makeId(cat);
	    js+= "{category:\"" + cat+"\"},\n";
	}
	var img = new Image();
	let url = 'data:image/png;base64,' + image.image;
	img.onload=()=>{
	    if(Utils.ColorTables[image.name]) {
		console.log('exists '+image.name);
		setTimeout(makeImage,1);
		return;
	    }
//	    console.log(image.name);
	    ctx.drawImage(img, 0, 0,w,h);
	    var data = ctx.getImageData(0, 0, w, 1).data;
	    let colors = [];
	    for(let i=0;i<data.length;i+=4) {
		colors.push(data[i],data[i+1],data[i+2]);
//		console.log(c);
	    }
	    js+='{id:\'' +image.name+'\',colors: Utils.makeRGBColortable(' +Utils.join(colors,',')+')},\n';
	    setTimeout(makeImage,1);
	}
	img.src=url;
    }
    makeImage();
});
