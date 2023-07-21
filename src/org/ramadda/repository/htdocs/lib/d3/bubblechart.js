//
//With some minor modifications this is from https://naustud.io/tech-stack/
//
/*
Copyright  2017 Nau Studio. Created by Thanh Tran. ISC License.
THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE. 
*/

//
/// Based loosely from this D3 bubble graph https://bl.ocks.org/mbostock/4063269
// And this Forced directed diagram https://bl.ocks.org/mbostock/4062045


/*eslint-enable indent*/
/*global d3*/
function BubbleChart(dom,data, args) {
    let options = {
	showSizeLegend:true
    }
    if(args)
	$.extend(options,args);


    let domainLabel1 = options.label1!=null?options.label1:'less';
    let domainLabel2 = options.label2!=null?options.label2:'more';
    let svg = d3.select(dom);
    let width = svg.property('clientWidth'); // get width in pixels
    let height = +svg.attr('height');
    let centerX = width * 0.5;
    let centerY = height * 0.5;
    let strength = 0.05;
    let focusedNode;

    let format = d3.format(',d');

    let c = options.colors!=null?options.colors:d3.schemeCategory10;
    let scaleColor = d3.scaleOrdinal(c);

    // use pack to calculate radius of the circle
    let pack = d3.pack()
	.size([width, height ])
	.padding(1.5);

    let forceCollide = d3.forceCollide(d => d.r + 1);

    // use the force
    let simulation = d3.forceSimulation()
    // .force('link', d3.forceLink().id(d => d.id))
	.force('charge', d3.forceManyBody())
	.force('collide', forceCollide)
    // .force('center', d3.forceCenter(centerX, centerY))
	.force('x', d3.forceX(centerX ).strength(strength))
	.force('y', d3.forceY(centerY ).strength(strength));

    // reduce number of circles on mobile screen due to slow computation
    if ('matchMedia' in window && window.matchMedia('(max-device-width: 767px)').matches) {
	data = data.filter(el => {
	    return el.value >= 50;
	});
    }

    let root = d3.hierarchy({ children: data })
	.sum(d => d.value);

    // we use pack() to automatically calculate radius conveniently only
    // and get only the leaves
    let nodes = pack(root).leaves().map(node => {
	// console.log('node:', node.x, (node.x - centerX) * 2);
	const data = node.data;
	return {
	    x: centerX + (node.x - centerX) * 3, // magnify start position to have transition to center movement
	    y: centerY + (node.y - centerY) * 3,
	    r: 0, // for tweening
	    radius: node.r, //original radius
	    id: data.cat + '.' + (data.name.replace(/\s/g, '-')),
	    cat: data.cat,
	    name: data.name,
	    value: data.value,
	    icon: data.icon,
	    desc: data.desc,
	};
    });
    simulation.nodes(nodes).on('tick', ()=>{BubbleChartTicked(node);});

    svg.style('background-color', '#eee');
    let node = svg.selectAll('.node')
	.data(nodes)
	.enter().append('g')
	.attr('class', 'node')
	.call(d3.drag()
	      .on('start', (event,d) => {
		  if (!event.active) { simulation.alphaTarget(0.2).restart(); }
		  d.fx = d.x;
		  d.fy = d.y;
	      })
	      .on('drag', (event,d) => {
		  d.fx = event.x;
		  d.fy = event.y;
	      })
	      .on('end', (event,d) => {
		  if (!event.active) { simulation.alphaTarget(0); }
		  d.fx = null;
		  d.fy = null;
	      }));

    node.append('circle')
	.attr('id', d => d.id)
	.attr('r', 0)
	.style('fill', d => scaleColor(d.cat))
	.transition().duration(2000).ease(d3.easeElasticOut)
	.tween('circleIn', (d) => {
	    let i = d3.interpolateNumber(0, d.radius);
	    return (t) => {
		d.r = i(t);
		simulation.force('collide', forceCollide);
	    };
	});

    node.append('clipPath')
	.attr('id', d => `clip-${d.id}`)
	.append('use')
	.attr('xlink:href', d => `#${d.id}`);

    // display text as circle icon
    node.filter(d => !String(d.icon).includes('http'))
	.append('text')
	.classed('node-icon', true)
	.attr('clip-path', d => `url(#clip-${d.id})`)
	.selectAll('tspan')
	.data(d => d.icon.split(';'))
	.enter()
	.append('tspan')
	.attr('x', 0)
	.attr('y', (d, i, nodes) => (13 + (i - nodes.length / 2 - 0.5) * 10))
	.text(name => name);

    // display image as circle icon
    node.filter(d => String(d.icon).includes('http'))
	.append('image')
	.classed('node-icon', true)
	.attr('clip-path', d => `url(#clip-${d.id})`)
	.attr('xlink:href', d => d.icon)
	.attr('x', d => -d.radius * 0.7)
	.attr('y', d => -d.radius * 0.7)
	.attr('height', d => d.radius * 2 * 0.7)
	.attr('width', d => d.radius * 2 * 0.7);

    node.append('title')
	.text(d => (d.cat + '::' + d.name + '\n' + format(d.value)));

    let legendOrdinal = d3.legendColor()
	.scale(scaleColor)
	.shape('circle');

    // legend 1
    svg.append('g')
	.classed('legend-color', true)
	.attr('text-anchor', 'start')
	.attr('transform', 'translate(20,30)')
	.style('font-size', '12px')
	.call(legendOrdinal);

    let sizeScale = d3.scaleOrdinal()
	.domain([domainLabel1, domainLabel2])
	.range([5, 10] );

    let legendSize = d3.legendSize()
	.scale(sizeScale)
	.shape('circle')
	.shapePadding(10)
	.labelAlign('end');


    // legend 2
    if(options.showSizeLegend) {
	svg.append('g')
	    .classed('bubblechart-legend-size', true)
	    .attr('text-anchor', 'start')
	    .attr('transform', 'translate(150, 25)')
	    .style('font-size', '12px')
	    .call(legendSize);
    }


    /*
      <foreignObject class="circle-overlay" x="10" y="10" width="100" height="150">
      <div class="circle-overlay__inner">
      <h2 class="circle-overlay__title">ReactJS</h2>
      <p class="circle-overlay__body">Lorem ipsum dolor sit amet, consectetur adipisicing elit. Ullam, sunt, aspernatur. Autem repudiandae, laboriosam. Nulla quidem nihil aperiam dolorem repellendus pariatur, quaerat sed eligendi inventore ipsa natus fugiat soluta doloremque!</p>
      </div>
      </foreignObject>
    */
    let infoBox = node.append('foreignObject')
	.classed('circle-overlay bubblechart-hidden', true)
	.attr('x', -350 * 0.5 * 0.8)
	.attr('y', -350 * 0.5 * 0.8)
	.attr('height', 350 * 0.8)
	.attr('width', 350 * 0.8)
	.append('xhtml:div')
	.classed('circle-overlay__inner', true);

    infoBox.append('h2')
	.classed('circle-overlay__title', true)
	.text(d => d.name);

    infoBox.append('p')
	.classed('circle-overlay__body', true)
	.html(d => d.desc);


    node.on('click', (event,currentNode) => {
	event.stopPropagation();
	let currentTarget = event.currentTarget; // the <g> el

	if (currentNode === focusedNode) {
	    // no focusedNode or same focused node is clicked
	    return;
	}
	let lastNode = focusedNode;
	focusedNode = currentNode;

	simulation.alphaTarget(0.2).restart();
	// hide all circle-overlay
	d3.selectAll('.circle-overlay').classed('bubblechart-hidden', true);
	d3.selectAll('.node-icon').classed('node-icon--faded', false);

	// don't fix last node to center anymore
	if (lastNode) {
	    lastNode.fx = null;
	    lastNode.fy = null;
	    node.filter((d, i) => i === lastNode.index)
		.transition().duration(2000).ease(d3.easePolyOut)
		.tween('circleOut', () => {
		    let irl = d3.interpolateNumber(lastNode.r, lastNode.radius);
		    return (t) => {
			lastNode.r = irl(t);
		    };
		})
		.on('interrupt', () => {
		    lastNode.r = lastNode.radius;
		});
	}

	// if (!event.active) simulation.alphaTarget(0.5).restart();

	d3.transition().duration(2000).ease(d3.easePolyOut)
	    .tween('moveIn', () => {
		let ix = d3.interpolateNumber(currentNode.x, centerX);
		let iy = d3.interpolateNumber(currentNode.y, centerY);
		let ir = d3.interpolateNumber(currentNode.r, centerY * 0.5);
		return function (t) {
		    // console.log('i', ix(t), iy(t));
		    currentNode.fx = ix(t);
		    currentNode.fy = iy(t);
		    currentNode.r = ir(t);
		    simulation.force('collide', forceCollide);
		};
	    })
	    .on('end', () => {
		simulation.alphaTarget(0);
		let $currentGroup = d3.select(currentTarget);
		$currentGroup.select('.circle-overlay')
		    .classed('bubblechart-hidden', false);
		$currentGroup.select('.node-icon')
		    .classed('node-icon--faded', true);

	    })
	    .on('interrupt', () => {
//		console.log('move interrupt', currentNode);
		currentNode.fx = null;
		currentNode.fy = null;
		simulation.alphaTarget(0);
	    });

    });

    // blur
    d3.select(document).on('click', (event) => {
	let target = event.target;
	// check if click on document but not on the circle overlay
	if (!target.closest('#circle-overlay') && focusedNode) {
	    focusedNode.fx = null;
	    focusedNode.fy = null;
	    simulation.alphaTarget(0.2).restart();
	    d3.transition().duration(2000).ease(d3.easePolyOut)
		.tween('moveOut', function () {
//		    console.log('tweenMoveOut', focusedNode);
		    let ir = d3.interpolateNumber(focusedNode.r, focusedNode.radius);
		    return function (t) {
			focusedNode.r = ir(t);
			simulation.force('collide', forceCollide);
		    };
		})
		.on('end', () => {
		    focusedNode = null;
		    simulation.alphaTarget(0);
		})
		.on('interrupt', () => {
		    simulation.alphaTarget(0);
		});

	    // hide all circle-overlay
	    d3.selectAll('.circle-overlay').classed('bubblechart-hidden', true);
	    d3.selectAll('.node-icon').classed('node-icon--faded', false);
	}
    });
}


function BubbleChartTicked(node) {
    node
	.attr('transform', d => `translate(${d.x},${d.y})`)
	.select('circle')
	.attr('r', d => d.r);
}


