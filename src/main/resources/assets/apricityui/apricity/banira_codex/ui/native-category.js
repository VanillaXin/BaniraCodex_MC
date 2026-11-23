(function () {
    document.addEventListener("DOMContentLoaded", function () {
        var fold = document.getElementById("category_toggle");
        var marker = document.getElementById("category_indicator");
        var list = document.getElementById("category_content");
        var expanded = false;
        fold.addEventListener("click", function () {
            expanded = !expanded;
            fold.setAttribute("aria-expanded", expanded ? "true" : "false");
            list.setAttribute("aria-hidden", expanded ? "false" : "true");
            marker.setInnerText(expanded ? "\u25be" : "\u25b8");
        });
    });
})();
