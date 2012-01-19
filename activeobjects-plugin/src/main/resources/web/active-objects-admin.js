(function() {
    var $ = AJS.$;

    /**
     * Creates a confirmation dialog that fires a callback if accepted, does
     * nothing if cancelled
     * 
     * @method createConfirmDialog
     * @return {Dialog} The dialog element
     */
    function showDialog(title, text, successFunction) {
        var dialog = new AJS.Dialog(500, 200, 'show-dialog');

        dialog.addPanel("", "panel1");
        dialog.addHeader(title);

        if (successFunction) {
            dialog.addButton(i18n("i18nAoAdminDeleteButtonConfirm"), function(popup) {
                popup.hide();
                successFunction();
            }, "confirm-button");
        }

        dialog.addButton(i18n("i18nAoAdminDeleteButtonClose"), function(popup) {
            popup.hide();
        }, "close-button");

        dialog.getCurrentPanel().html(text);
        dialog.show();
    }

    $(document).ready(function() {
        $(".ao-plugin-description").click(function() {
            $(this).parent().toggleClass("ao-plugin-selected");
        });

        $("a.delete-action").click(function() {
            var $form = $(this).closest("form");
            var $selected = $("input:checked", $form);
            if ($selected.size() == 0) {
                // Tell the user no table is selected
                showDialog(i18n("i18nAoAdminDeleteNoTableSelectedTitle"), i18n("i18nAoAdminDeleteNoTableSelectedMessage"));
            } else {
                var tableList = [];
                $selected.each(function() {
                    tableList.push($(this).attr("value"));
                });

                // Request confirmation and submit the form
                var confirmationMessage = i18n("i18nAoAdminDeleteConfirmMessage", tableList.join(", "));
                var title = i18n("i18nAoAdminDeleteConfirmTitle");
                showDialog(title, confirmationMessage, function() {
                    $form.submit();
                });
            }

            return false;
        });

    });

    /**
     * Translates 'key' into a message, by searching for the hidden input field with the same
     * id. If present, supplementary arguments are used as parameters.
     */
    function i18n(key) {
        // The first argument (key) is the ID of an input field in list-tables.vm

        var message = $("#" + key).attr("value");
        if (!message) {
            throw "No message for key " + key;
        }
        
        // Build the list of arguments
        var formatArguments = [ message ];
        for ( var i = 1; i < arguments.length; i++) {
            formatArguments.push(arguments[i]);
        }
        return AJS.format.apply(this, formatArguments);
    }
})();