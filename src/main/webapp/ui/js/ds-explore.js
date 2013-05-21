/**
 * Data set explorer, based on jsPlumb
 */
var DATASET_EXPLORER = function() {

//    var dynamicAnchors = [
//                          [0, 0.2, -1, 0], [0, 0.5, -1, 0], [0, 0.8, -1, 0], // left
//                          [0.2, 1, 0, 1],  [0.5, 1, 0, 1],  [0.8, 1, 0, 1],  // top
//                          [1, 0.2, 1, 0],  [1, 0.5, 1, 0],  [1, 0.8, 1, 0],  // right
//                          [0.2, 0, 0, -1], [0.5, 0, 0, -1], [0.8, 0, 0, -1]  // bottom
//                          ];

    // Structure is {nodeid: {element:jqueryelt, visible:boolean}}
    var nodeState = {};

    // Structure is {srcid : {destid: {names:[name1,...], visible:boolean}}}
    var linkState = {};
    var invLinkState = {};

    jsPlumb.importDefaults({
        PaintStyle : {
            lineWidth:2,
            strokeStyle: '#456'
        },
        ConnectionsDetachable : false,
        Connector : [ "Bezier",  { curviness: 40 } ],
        Endpoints : [["Dot", {radius:2}], "Blank"],
        ConnectionOverlays : [ [ "Arrow", { location:1, id:"arrow", length:9, width:8  } ] ],
        Anchor : "Continuous"
    });


    var linksFor = function(src) {
        var dests = linkState[src];
        if (dests === undefined) {
            dests = {};
            linkState[src] = dests;
        }
        return dests;
    };

    var invLinksFor = function(src) {
        var dests = invLinkState[src];
        if (dests === undefined) {
            dests = {};
            invLinkState[src] = dests;
        }
        return dests;
    };

    var register = function(src, dest, name) {
        var links = linksFor(src);
        var l = links[dest];
        if (l === undefined) {
            l = {names:[name], visible:false};
            links[dest] = l;
        } else if ($.inArray(name, l.names) === -1) {
            l.names.push( name );
        }
        invLinksFor(dest)[src] = l;
    };

    var findNode = function(id) {
        return $(".node[data-id='" + id + "']");
    };

    var registerNode = function(nodeid, label) {
        nodeState[nodeid] = {element: findNode(nodeid), visible: true, label: label};
    };

    var hideNode = function(nodeid) {
        var state = nodeState[nodeid];
        state.element.hide();
        state.visible = false;
    };

    var showNode = function(nodeid) {
        var state = nodeState[nodeid];
        state.element.show();
        state.visible = true;
    };

    var nodeFor = function(id) {
        var state = nodeState[id];
        if (state === undefined) {
            return undefined;
        } else {
            return state.element;
        }
    };

    var randomPlacement = function(current, toplace) {
        var angle = Math.random() * 2 * Math.PI;
        var yoffset = 180 * Math.sin(angle);
        var xoffset = 180 * Math.cos(angle);
        var position = nodeFor(current).offset();
        position.left = position.left + xoffset;
        position.top = position.top + yoffset;
        nodeFor(toplace).offset(position);
    };

    var addConnection = function(src, target) {
        var link = linksFor(src)[target];
        if (link !== undefined && !link.visible) {
            var linkName = link.names.join();
            jsPlumb.connect({
                source: nodeFor( src ),
                target: nodeFor( target ),
                overlays: [ ["Label", {label:linkName, location:0.7, id:"label", cssClass:"link-label"}] ]
            });
            link.visible = true;
        }
    };

    var addConnections = function(src, target) {
        addConnection(src, target);
        addConnection(target, src);
    };

    var addDirectionalConnections = function(outbound, src, target) {
        if (outbound) {
            addConnection(src, target);
        } else {
            addConnection(target, src);
        }
    };

    var removeConnections = function(src) {
        jsPlumb.detachAllConnections( nodeFor(src) );
        $.each(linksFor(src), function(target, state){ state.visible = false;  });
        $.each(invLinksFor(src), function(target, state){ state.visible = false;  });
    };

    var addNodeFn = function(outbound) {
        return function(event) {
            var node = $(event.currentTarget).attr('data-node');
            var links = outbound ? linksFor(node) : invLinksFor(node);
            $.each(links, function(linkedDS, state){
                var targetNode = nodeFor(linkedDS);
                if (targetNode === undefined) {
                    $.get('/ui/dataset-browse-element?uri=' + linkedDS, function(data){
                        $("#canvas").append(data);
                        initElement();
                        randomPlacement(node, linkedDS);
//                        addConnections(node, linkedDS);
                        addDirectionalConnections(outbound, node, linkedDS);
                    });
                } else {
                    showNode(linkedDS);
//                    addConnections( node, linkedDS);
                    addDirectionalConnections(outbound, node, linkedDS);
                }
            });
            return false;
        };
    };

    var initElement = function() {
        $(".popinfo").popover();

        jsPlumb.draggable($(".node"));

        $(".add-out").unbind().click( addNodeFn(true) );
        $(".add-in").unbind().click( addNodeFn(false) );

        $(".close-node").unbind().click(function(event){
            var nodeURI = $(event.currentTarget).attr('data-node');
            removeConnections( nodeURI );
            hideNode(nodeURI);
            return false;
        });
    };

    var scaling = 4;   // Scaling springy coordinates to screen coordinates, no longer used
    var maxIter = 200; // Force termination after this many iterations
    var iterStep = 2;  // 1 + number of iteration steps to skip animating

    var relayout = function() {
        // Set up springy clone of graph
        var graph = new Springy.Graph();
        $.each(nodeState, function(nodeID, state){
            if (state.visible) {
                state.springyNode = graph.newNode(state);
            }
        });
        $.each(linkState, function(src, links){
            $.each(links, function(target, state){
                if (state.visible) {
                    graph.newEdge( nodeState[src].springyNode, nodeState[target].springyNode );
                }
            });
        });

        var width = $("#canvas").width();
        var height = $("#canvas").height();

        var layout = new Springy.Layout.ForceDirected(
                graph,
                400.0, // Spring stiffness
                400.0, // Node repulsion
                0.25  // Damping
              );


        // calculate bounding box of graph layout.. with ease-in
        var currentBB = layout.getBoundingBox();
        var targetBB = {bottomleft: new Springy.Vector(-2, -2), topright: new Springy.Vector(2, 2)};

        // auto adjusting bounding box
        Springy.requestAnimationFrame(function adjust() {
            targetBB = layout.getBoundingBox();
            // current gets 20% closer to target every iteration
            currentBB = {
                bottomleft: currentBB.bottomleft.add( targetBB.bottomleft.subtract(currentBB.bottomleft)
                    .divide(10)),
                topright: currentBB.topright.add( targetBB.topright.subtract(currentBB.topright)
                    .divide(10))
            };

            Springy.requestAnimationFrame(adjust);
        });

        // convert to/from screen coordinates
        var toScreen = function(p) {
            var size = currentBB.topright.subtract(currentBB.bottomleft);
            var sx = p.subtract(currentBB.bottomleft).divide(size.x).x * width;
            var sy = p.subtract(currentBB.bottomleft).divide(size.y).y * height;
            return {left: sx, top: sy};
        };

        var fromScreen = function(s) {
            var size = currentBB.topright.subtract(currentBB.bottomleft);
            var px = (s.x / width) * size.x + currentBB.bottomleft.x;
            var py = (s.y / height) * size.y + currentBB.bottomleft.y;
            return new Springy.Vector(px, py);
        };

        var iter = 0;

        var renderer = new Springy.Renderer(
                layout,
                function clear() {
                    iter += 1;
                    if (iter > maxIter) {
                        renderer.stop();
                    }
                },
                function drawEdge(edge, p1, p2) { },
                function drawNode(node, p) {
                    if (iter % iterStep === 0) {
                        node.data.element.offset( toScreen(p) );
                        jsPlumb.repaint(node.data.element);
                    }
                }
              );
        renderer.start();
    };

    // Return the public variables/functions for this module
    return {
        register : register,
        registerNode : registerNode,
        initElement : initElement,
        relayout : relayout
    };

}(); // "revealing module" pattern


jsPlumb.bind("ready", function() {
    DATASET_EXPLORER.initElement();
    $("#relayout").click( DATASET_EXPLORER.relayout );
});


