// Set of initialization actions fun on page load to make UI features live

$(function() {

    // Query forms run a target query and load the resulting HTML into a data-result element        
    var processQueryForms = function() {
        $(".query-form").each(function() {
            var target = $(this).attr('data-target');
            var queryField = $(this).attr('data-query');
            var resultField = $(this).attr('data-result');
            $(this).submit( function(){
                var url = target + $(queryField).val();
                $(resultField).load(url);
                return false;
            });
        });
    };

    processQueryForms();

    // Anything marked as popinfo will have a popover (data-trigger, data-placement, data-content)
    // enable popups
    $(".popinfo").popover();

    // General ajax forms which reload the page when actioned. Errors are displayed in elts of class "ajax-error". 
    $(".ajax-form").ajaxForm({
      success:
          function(data, status, xhr){
            // $("#status-dialog").modal("hide");
            location.reload();
          },

      error:
        function(xhr, status, error){
           $(".ajax-error").html("<div class='alert'> <button type='button' class='close' data-dismiss='alert'>&times;</button>Action failed: " + error + " - " + xhr.responseText + "</div>");
        }
    });

    // Set up editable fields
    $.fn.editable.defaults.mode = 'inline';
    
    $(".ui-editable").editable();
    
    $(".edit-remove-row").click(function(e){
        var rowid = $(e.target).attr("data-target");
        $(rowid).remove();
    });
    
    var emitResource = function(val) {
        if (val.match(/https?:/)) {
            return "<" + val + ">";
        } else {
            return val;
        }
    };
    
    // Implement edit save-changes functionality
    $("#update-save").click( function(){
        var data = $("#edit-prefixes").text();
        var url = $("#edit-table").attr("data-target");
        data = data + "\n<" + $("#edit-table").attr("data-root") + ">\n";
        $("#edit-table tbody tr").each(function(){
            var row = $(this).find("td").toArray();
            var prop = emitResource($(row[0]).text());
            var value = $(row[1]).text();
            data = data + "    " + prop + " " + value + " ;\n";
        });
        $.ajax({
            type: "PUT",
            url: url,
            data: data,
            contentType: "text/turtle",
            success: function(){
                $("#msg").html("Submitted successfully");
                location.reload();
            },
            error: function(xhr, status, error){
                $("#msg").html("Save failed: " + error + " - " + xhr.responseText);
                $('#msg-alert').removeClass('alert-success').addClass('alert-error').show();
            }
          });
    });
});
