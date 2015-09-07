(function () {
    var localRequire = typeof require === 'function' ?
        require :
        function (deps, callback) {
            callback(AJS.$);
        };

    localRequire(['jquery'], function ($) {
        $(document).ready(function () {
            $(".ao-plugin").click(function () {
                $(this).toggleClass("ao-plugin-selected");
            });
        });
    });
})();