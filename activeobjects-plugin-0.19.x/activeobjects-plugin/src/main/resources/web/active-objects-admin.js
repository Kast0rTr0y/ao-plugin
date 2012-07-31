(function () {
    var $ = AJS.$;

    $(document).ready(function ()
    {
       $(".ao-plugin").click(function()
       {
           $(this).toggleClass("ao-plugin-selected");
       });
    });
})();