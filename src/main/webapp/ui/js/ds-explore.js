/**
 * Data set explorer, based on jsPlumb
 */
var DATASET_EXPLORER = function() {

    var dynamicAnchors = [
                          [0, 0.2, -1, 0], [0, 0.5, -1, 0], [0, 0.8, -1, 0], // left
                          [0.2, 1, 0, 1],  [0.5, 1, 0, 1],  [0.8, 1, 0, 1],  // top
                          [1, 0.2, 1, 0],  [1, 0.5, 1, 0],  [1, 0.8, 1, 0],  // right
                          [0.2, 0, 0, -1], [0.5, 0, 0, -1], [0.8, 0, 0, -1]  // bottom
                          ];

    jsPlumb.importDefaults({
        PaintStyle : {
            lineWidth:2,
            strokeStyle: '#456'
        },
        ConnectionsDetachable : false,
        Connector : [ "Bezier",  { curviness: 40 } ],
        Endpoints : [["Dot", {radius:2}], "Blank"],
        ConnectionOverlays : [ [ "Arrow", { location:1, id:"arrow", length:9, width:8  } ] ],
//        Anchor : dynamicAnchors
        Anchor : "Continuous"
    });

    var nodeFor = function(id) {
        return $(".node[data-id='" + id + "']");
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
        var names = $(".connection-out[data-out='" + src + "'][data-in='" + target + "']").map(function(){ return $(this).text()}).get();
        if (names.length !== 0) {
            var linkName = names.join();
            jsPlumb.connect({
                source: nodeFor( src ),
                target: nodeFor( target ),
                overlays: [ ["Label", {label:linkName, location:0.7, id:"label"}] ]
            });
        }
    };

    var addConnections = function(src, target) {
        addConnection(src, target);
        addConnection(target, src);
    };

    var addNodeFn = function(dir) {
        var idir = dir === "out" ? "in" : "out";
        return function(event) {
            var node = $(event.currentTarget).attr('data-node');
            var linkSet = $(".connection-" + dir + "[data-" + dir + "='" + node + "']");
            var targets = {};
            linkSet.each(function(){ targets[ $(this).attr('data-' + idir) ] = node; });
            $.each(targets, function(linkedDS, src) {
                var targetNode = nodeFor(linkedDS);
                if (targetNode.length === 0) {
                    $.get('/ui/dataset-browse-element?uri=' + linkedDS, function(data){
                        $("#canvas").append(data);
                        initElement();
                        randomPlacement(node, linkedDS);
                        addConnections(node, linkedDS);
                    });
                } else {
                    if (targetNode.is(":hidden")) {
                        targetNode.show();
                        addConnections( node, linkedDS);
                    }
                }
            });
            return false;
        };
    };

    var initElement = function() {
        $(".popinfo").popover();

        jsPlumb.draggable($(".node"));

        $(".add-out").unbind().click( addNodeFn("out") );
        $(".add-in").unbind().click( addNodeFn("in") );

        $(".close-node").unbind().click(function(event){
            var nodeURI = $(event.currentTarget).attr('data-node');
            var node = nodeFor(nodeURI);
            jsPlumb.detachAllConnections(node);
            node.hide();
            return false;
        });
    };

    // Return the public variables/functions for this module
    return {
       initElement : initElement
    };

}(); // "revealing module" pattern


jsPlumb.bind("ready", function() {
    DATASET_EXPLORER.initElement();
});


