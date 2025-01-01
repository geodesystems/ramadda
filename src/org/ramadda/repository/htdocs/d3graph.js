/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/


function D3Graph(div, nodes, links, width, height) {

    this.findNode = function(value, field) {
        if (!field) {
            field = "nodeid";
        }
        var nodes = this.force.nodes();
        for (var i in nodes) {
            if (nodes[i][field] === value) {
                return nodes[i];
            }
        }
        return null;
    }

    this.getForce = function() {
        return this.force;
    }
    this.getSvg = function() {
        return this.svg;
    }
    this.getNodes = function() {
        return this.getForce().nodes();
    }
    this.getLinks = function() {
        return this.getForce().links();
    }

    this.update = function() {
        var theGraph = this;
        var svg = this.getSvg();
        var force = this.getForce();

        svg.append("svg:defs").selectAll("marker")
            .data(["arrow"])
            .enter().append("svg:marker")
            .attr("id", String)
            .attr("viewBox", "0 -5 10 10")
            .attr("refX", 15)
            .attr("refY", -1.5)
            .attr("class", "graph-arrow")
            .attr("markerWidth", 8)
            .attr("markerHeight", 10)
            .attr("orient", "auto")
            .append("svg:path")
            .attr("d", "M0,-5L10,0L0,5");

        var doLine = true;
        if (doLine) {
            var link = svg.selectAll("line.graph-link").
            data(force.links(), function(d) {
                return d.source.id + "-" + d.target.id;
            });

            var linkEnter = link.enter();
            linkEnter.insert("line").attr("class", "graph-link")
                .attr("marker-end", function(d) {
                    return "url(#arrow)";
                });
            link.exit().remove();
        } else {
            //            var path = svg.append("svg:g").selectAll("path").data(force.links(),  function(d) { return d.source.id + "-" + d.target.id; });
            var path = svg.append("svg:g").selectAll("path").data(force.links(), function(d) {
                return d.source.id + "-" + d.target.id;
            });
            var pathEnter = path.enter();
            pathEnter.append("svg:path")
                .attr("class", function(d) {
                    return "graph-path"
                })
                .attr("marker-end", function(d) {
                    return "url(#arrow)";
                });
            path.exit().remove();
        }


        var node = svg.selectAll("g.graph-node").
        data(force.nodes(), function(d) {
            return d.id;
        });

        var nodeEnter = node.enter().append("g")
            .attr("class", "graph-node")
            .call(force.drag);

        nodeEnter.append("image")
            .attr("class", "graph-circle")
            .attr("xlink:href", function(d) {
                return theGraph.getNodeIcon(d);
            })
            .attr("x", "-8px")
            .attr("y", "-8px")
            .attr("width", "16px")
            .attr("height", "16px");


        nodeEnter.append("text")
            .attr("class", function(d) {
                if (d.graphurl) return "graph-node-text-unvisited";
                else return "graph-node-text-visited";
            })
            .attr("dx", 12)
            .attr("dy", ".35em")
            .text(function(d) {
                return d.name
            });

        nodeEnter.append("text")
            .attr("class", function(d) {
                if (d.graphurl) return "graph-node-text-unvisited";
                else return "graph-node-text-visited";
            })
            .attr("dx", -16)
            .attr("dy", "-1em")
            .text(function(d) {
                if (d.label) return d.label;
                return "";
            });


        node.exit().remove();
        node.on("click", function(d) {
            theGraph.nodeClicked(d);
        }, true);


        force.on("tick", function() {
            if (doLine) {
                link.attr("x1", function(d) {
                        return d.source.x;
                    })
                    .attr("y1", function(d) {
                        return d.source.y;
                    })
                    .attr("x2", function(d) {
                        return d.target.x;
                    })
                    .attr("y2", function(d) {
                        return d.target.y;
                    });
            } else {
                path.attr("d", function(d) {
                    var dx = d.target.x - d.source.x,
                        dy = d.target.y - d.source.y,
                        dr = Math.sqrt(dx * dx + dy * dy);
                    //                    return "M" + d.source.x + "," + d.source.y + "L" + dr + "," + dr + " 0 0,1 " + d.target.x + "," + d.target.y;
                    return "M" + d.source.x + "," + d.source.y + "L" + d.target.x + " " + d.target.y;
                });
            }

            node.attr("transform", function(d) {
                return "translate(" + d.x + "," + d.y + ")";
            });
        });

        var allNodes = svg.selectAll("text");
        allNodes.attr("class", function(d) {
            if (d.graphurl) return "graph-node-text-unvisited";
            else return "graph-node-text-visited";
        });

        // Restart the force layout.
        force.start();
    }

    this.nodeClicked = function(event,d) {
        if (event.altKey && d.url) {
            window.location = d.url;
            return;
        }
        if (!d.graphurl) return;
        var theGraph = this;
        var url = d.graphurl;
        d.graphurl = null;
        d.visited = true;
        d3.json(url, function(json) {
            if (json.nodes) {
                for (var i in json.nodes) {
                    theGraph.addNode(json.nodes[i]);
                }
            }
            if (json.links) {
                for (var i in json.links) {
                    theGraph.addLink(json.links[i]);
                }
            }
            theGraph.update();
        });
    }

    this.getNodeIcon = function(d) {
        if (d.icon) return d.icon;
        return "http://ramadda.org/repository/icons/folderclosed.png";
    }

    this.addNode = function(node) {
        if (this.nodeMap[node.nodeid]) {
            //            alert("already in graph:" + node.nodeid);
            return;
        }
        //        alert("adding in graph:" + node.nodeid);
        this.nodeMap[node.nodeid] = node;
        this.force.nodes().push(node);
    }

    this.addLink = function(link) {
        if (link.source_id) {
            link.source = this.findNode(link.source_id);
            if (!link.source) {
                alert("could not find source:" + link.source_id);
                return;
            }
        }
        if (link.target_id) {
            link.target = this.findNode(link.target_id);
            if (!link.target) {
                alert("could not find target:" + link.target_id);
                return;
            }
        }
        var linkId = link.source.nodeid + "-" + link.target.nodeid;
        if (this.linkMap[linkId]) {
            return
        }
        this.linkMap[linkId] = link;
        this.force.links().push(link);
    }

    this.nodeMap = {};
    this.linkMap = {};

    var msg = ""

    for (var idx in nodes) {
        var node = nodes[idx];
        this.nodeMap[node.nodeid] = node;
    }


    if (!width) width = 960;
    if (!height) height = 500;

    this.svg = d3.select(div).append("svg");

    this.svg.attr("width", width).attr("height", height);

    this.force = d3.layout.force()
        .gravity(.05)
        .distance(150)
        .charge(-100)
        .size([width, height]);

    this.force.nodes(nodes);
    for (var i in links) {
        var link = links[i];
        if (link.source_id) {
            link.source = this.findNode(link.source_id);
            if (!link.source) alert("could not find source:" + link.source_id);
        }
        if (link.target_id) {
            link.target = this.findNode(link.target_id);
            if (!link.target) alert("could not find target:" + link.target_id);
        }
    }
    this.force.links(links);
    this.update();
}
