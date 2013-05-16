/**
 * Data set explorer, based on jsPlumb
 */


jsPlumb.bind("ready", function() {
    jsPlumb.draggable($(".node"));
});

//$(function() {
//    $(".node").draggable();
//});
