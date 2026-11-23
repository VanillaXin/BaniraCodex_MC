(function () {
    var channel = "banira_picker_script";
    var catalog = [];
    var state = {query: "", only: false, expanded: [], collapsed: [], selected: null};
    var lastClick = null;

    function normalized(value) {
        return String(value).replace(/^[\x00-\x20]+|[\x00-\x20]+$/g, "").toLowerCase();
    }

    function toggle(list, value) {
        var index = list.indexOf(value);
        if (index < 0) list.push(value);
        else list.splice(index, 1);
    }

    function project(query) {
        var order = [];
        var groups = Object.create(null);
        var rows = [];
        for (var i = 0; i < catalog.length; i++) {
            var item = catalog[i];
            if (state.only && !item.selectable) continue;
            if (query && normalized(item.title).indexOf(query) < 0
                && normalized(item.details).indexOf(query) < 0
                && normalized(item.group).indexOf(query) < 0) continue;
            if (!groups[item.group]) {
                groups[item.group] = [];
                order.push(item.group);
            }
            groups[item.group].push(item);
        }
        for (var g = 0; g < order.length; g++) {
            var name = order[g];
            var expanded = query ? state.collapsed.indexOf(name) < 0 : state.expanded.indexOf(name) >= 0;
            rows.push({group: name, expanded: expanded});
            if (expanded) {
                var children = groups[name];
                for (var c = 0; c < children.length; c++) {
                    rows.push({id: children[c].id, selectable: children[c].selectable});
                }
            }
        }
        return rows;
    }

    function available(rows, id) {
        for (var i = 0; i < rows.length; i++) {
            if (rows[i].id === id && rows[i].selectable) return true;
        }
        return false;
    }

    function update(request) {
        if (request.catalog) catalog = request.catalog;
        if (request.init) {
            if (request.state) state = request.state;
            state.only = request.only;
            lastClick = null;
        }
        if (request.query !== state.query) {
            state.query = request.query;
            state.collapsed = [];
            lastClick = null;
        }
        var query = normalized(state.query);
        var rows = project(query);
        var confirmed = null;
        if (state.selected !== null && !available(rows, state.selected)) {
            state.selected = null;
            lastClick = null;
        }
        if (request.action === "filter") {
            state.only = !state.only;
            lastClick = null;
            rows = project(query);
        } else if (request.action === "fold") {
            for (var i = 0; i < rows.length; i++) {
                if (rows[i].group !== request.value) continue;
                toggle(query ? state.collapsed : state.expanded, request.value);
                lastClick = null;
                rows = project(query);
                break;
            }
        } else if (request.action === "select" && available(rows, request.value)) {
            var elapsed = request.now - lastClick;
            if (state.selected === request.value && lastClick !== null && elapsed >= 0 && elapsed <= request.doubleClick) {
                confirmed = request.value;
            }
            state.selected = request.value;
            lastClick = request.now;
        } else if (request.action === "confirm" && available(rows, state.selected)) {
            confirmed = state.selected;
        }
        if (confirmed !== null || !available(rows, state.selected)) {
            state.selected = null;
            lastClick = null;
        }
        for (var j = 0; j < rows.length; j++) rows[j].selected = rows[j].id === state.selected;
        return {state: state, rows: rows, confirmed: confirmed, selected: state.selected !== null, only: state.only};
    }

    document.addEventListener("DOMContentLoaded", function () {
        var endpoint = document.getElementById(channel);
        endpoint.addEventListener(channel, function () {
            var request = JSON.parse(String(endpoint.getAttribute("data-request")));
            endpoint.setAttribute("data-response", JSON.stringify(update(request)));
        });
        endpoint.setAttribute("data-ready", "1");
    });
})();
