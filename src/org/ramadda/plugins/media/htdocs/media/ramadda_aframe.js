var RamaddaAframe = {
    init: function(sceneId,cameraId,args) {
	args = args??{};
	document.addEventListener('DOMContentLoaded', () => {
	    const skyEl = document.querySelector('#' + args.skyId);
	    const sceneEl = document.querySelector('#'+ sceneId);
	    const cameraEl = document.querySelector('#' + cameraId);
            const zoomSpeed = 0.1;
	    sceneEl.addEventListener('loaded', function() {
		if(args.loadingId) {
		    let cnt = 0;
		    let interval = setInterval(()=>{
			//Wait at most 20 seconds
			if(cnt++>10*20) {
			    jqid(args.loadingId).remove();
			    clearInterval(interval);
			    return;
			}
			let skyMesh = skyEl.getObject3D('mesh');
			if (skyMesh && skyMesh.material && skyMesh.material.map && skyMesh.material.map.image) {
			    if(skyMesh.material.map.image.complete) {
				clearInterval(interval);
				jqid(args.loadingId).remove();
			    }
			}
		    },100);
		}
            });
	    if(Utils.isDefined(args.zoom)) {
		let zoom = ()=>{
		    const camera = cameraEl.getObject3D('camera');
		    if(!camera) {
			setTimeout(zoom,100);
			return;
		    }
		    camera.zoom = args.zoom;
		    camera.updateProjectionMatrix();
		};
		setTimeout(zoom,100);
	    }
            sceneEl.addEventListener('wheel', (event) => {
		const camera = cameraEl.getObject3D('camera');
		event.preventDefault(); 
		event.stopPropagation();
		const zoomChange = event.deltaY * zoomSpeed * -0.01;
		const newZoom = THREE.MathUtils.clamp(camera.zoom + zoomChange, 0.5, 5); 
		camera.zoom = newZoom;
		if(event.shiftKey)
		    console.log('zoom',newZoom);
		camera.updateProjectionMatrix();
            });

	    AFRAME.registerComponent('mouse-drag-rotate', {
		init: function () {
		    this.startX = 0;
		    this.startY = 0;
		    this.isDragging = false;
		    this.rotation = new THREE.Euler();
		    if(Utils.isDefined(args.rotateY))
			this.rotation.y=Utils.toRadians(args.rotateY);
		    if(Utils.isDefined(args.rotateX))
			this.rotation.x=Utils.toRadians(args.rotateX);		    
		    this.onMouseDown = this.onMouseDown.bind(this);
		    this.onMouseMove = this.onMouseMove.bind(this);
		    this.onMouseUp = this.onMouseUp.bind(this);
		    window.addEventListener('mousedown', this.onMouseDown);
		    window.addEventListener('mousemove', this.onMouseMove);
		    window.addEventListener('mouseup', this.onMouseUp);
		},

		remove: function () {
		    window.removeEventListener('mousedown', this.onMouseDown);
		    window.removeEventListener('mousemove', this.onMouseMove);
		    window.removeEventListener('mouseup', this.onMouseUp);
		},

		onMouseDown: function (event) {
		    this.isDragging = true;
		    this.startX = event.clientX;
		    this.startY = event.clientY;
		},
		
		onMouseMove: function (event) {
		    if (!this.isDragging) return;
		    const deltaX = event.clientX - this.startX;
		    const deltaY = event.clientY - this.startY;
		    const rotationSpeed = 0.005;
		    this.startX = event.clientX;
		    this.startY = event.clientY;
		    this.rotation.y -= deltaX * rotationSpeed;
		    this.rotation.x -= deltaY * rotationSpeed;
		    if(event.shiftKey) {
			console.log('x:',Utils.toDegrees(this.rotation.x),'y:',Utils.toDegrees(this.rotation.y));
		    }
		    this.el.object3D.rotation.set(
			this.rotation.x,
			this.rotation.y,
			this.rotation.z
		    );
		},

		onMouseUp: function () {
		    this.isDragging = false;
		}

	    });


	});
    }
}
