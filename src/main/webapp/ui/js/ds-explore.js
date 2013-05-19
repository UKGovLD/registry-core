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

    var register = function(linkspec) {
        var src = linkspec[0];
        var dest = linkspec[1];
        var name = linkspec[2];
        
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
    
    var registerNode = function(nodeid) {
        nodeState[nodeid] = {element: findNode(nodeid), visible:true};
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

    // Return the public variables/functions for this module
    return {
        register : register,
        registerNode : registerNode,
        initElement : initElement
    };

}(); // "revealing module" pattern


jsPlumb.bind("ready", function() {
    DATASET_EXPLORER.initElement();
});


