/**
 * Data set explorer, based on jsPlumb
 */
var DATASET_EXPLORER = function() {

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
    
    var initElement = function() {
        $(".popinfo").popover();
        
        jsPlumb.draggable($(".node"));
        
        $(".add-out").click(function(event){
            var node = $(event.currentTarget).attr('data-node');
            var linkSet = $(".connection-out[data-out='" + node + "']")
                            .map( function() { return $(this).attr('data-in'); } )
                            .get();
            $.each(linkSet, function(index, linkedDS){
                var alreadyRequested = false;
                for (var i = 0; i < index-1; i++) {
                    if (linkSet[i] === linkedDS) {
                        alreadyRequested = true;
                        break;
                    }
                }
                if (!alreadyRequested && nodeFor(linkedDS).length === 0) {
                    $.get('/ui/dataset-browse-element?uri=' + linkedDS, function(data){
                        $("#canvas").append(data);
                        initElement();
                        randomPlacement(node, linkedDS);
                    });
                }
            });
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


