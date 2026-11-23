const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");
const vm = require("node:vm");

const file = path.join(__dirname, "../../main/resources/assets/apricityui/apricity/banira_codex/ui/quick-screen-picker.js");
const channel = "banira_picker_script";
const catalog = [
    {id: "one.Options", title: "Options", details: "one.gui.Options", group: "One", selectable: true},
    {id: "one.Other", title: "Other", details: "one.gui.Other", group: "One", selectable: true},
    {id: "two.Inventory", title: "Inventory", details: "two.gui.Inventory", group: "Two", selectable: false}
];

function page() {
    const listeners = new Map();
    const attributes = new Map();
    const document = {
        addEventListener: (event, listener) => listeners.set(event, listener),
        getElementById: id => id === channel ? {
            addEventListener: (event, listener) => listeners.set(channel + ":" + event, listener),
            getAttribute: key => attributes.get(key),
            setAttribute: (key, value) => attributes.set(key, value)
        } : null
    };
    vm.runInNewContext(fs.readFileSync(file, "utf8"), {document}, {filename: file});
    listeners.get("DOMContentLoaded")();
    assert.equal(attributes.get("data-ready"), "1");
    return (action, fields = {}) => {
        attributes.set("data-request", JSON.stringify({action, query: "", now: 1000, doubleClick: 250, ...fields}));
        listeners.get(channel + ":" + channel)();
        return JSON.parse(attributes.get("data-response"));
    };
}

test("groups start collapsed and each group toggles independently", () => {
    const send = page();
    assert.deepEqual(send("sync", {catalog}).rows.map(r => r.group), ["One", "Two"]);
    assert.deepEqual(send("fold", {value: "One"}).rows.map(r => r.id || r.group), ["One", "one.Options", "one.Other", "Two"]);
    const both = send("fold", {value: "Two"}).rows;
    assert.ok(both.some(r => r.id === "two.Inventory" && !r.selectable));
    assert.deepEqual(send("fold", {value: "One"}).rows.map(r => r.id || r.group), ["One", "Two", "two.Inventory"]);
});

test("search is case insensitive, matches full IDs, and preserves manual expansion", () => {
    const send = page();
    send("sync", {catalog});
    send("fold", {value: "Two"});
    const query = " ONE.GUI.OPT ";
    assert.deepEqual(send("sync", {query}).rows.map(r => r.id || r.group), ["One", "one.Options"]);
    assert.deepEqual(send("fold", {query, value: "One"}).rows.map(r => r.id || r.group), ["One"]);
    assert.deepEqual(send("sync", {query: ""}).rows.map(r => r.id || r.group), ["One", "Two", "two.Inventory"]);
});

test("availability filter drops empty groups and ignores unavailable or unknown actions", () => {
    const send = page();
    send("sync", {catalog});
    send("fold", {value: "Two"});
    assert.equal(send("select", {value: "two.Inventory"}).selected, false);
    assert.equal(send("select", {value: "not-in-catalog"}).selected, false);
    const filtered = send("filter");
    assert.equal(filtered.only, true);
    assert.deepEqual(filtered.rows.map(r => r.group), ["One"]);
    assert.ok(send("filter").rows.some(r => r.id === "two.Inventory"));
});

test("single click highlights and explicit confirmation consumes the selection once", () => {
    const send = page();
    send("sync", {catalog});
    send("fold", {value: "One"});
    const selected = send("select", {value: "one.Options"});
    assert.equal(selected.confirmed, null);
    assert.deepEqual(selected.rows.filter(r => r.selected).map(r => r.id), ["one.Options"]);
    assert.equal(send("confirm").confirmed, "one.Options");
    assert.equal(send("confirm").confirmed, null);
});

test("only a second quick click on the same visible entry confirms", () => {
    const send = page();
    send("sync", {catalog});
    send("fold", {value: "One"});
    send("select", {value: "one.Options"});
    assert.equal(send("select", {value: "one.Options", now: 1400}).confirmed, null);
    assert.equal(send("select", {value: "one.Other", now: 1405}).confirmed, null);
    assert.equal(send("select", {value: "one.Other", now: 1500}).confirmed, "one.Other");
});

test("a collapse, changed query, or removed catalog entry clears a hidden selection", () => {
    const send = page();
    send("sync", {catalog});
    send("fold", {value: "One"});
    send("select", {value: "one.Options"});
    assert.equal(send("fold", {value: "One"}).selected, false);
    send("select", {query: "options", value: "one.Options"});
    assert.equal(send("confirm", {query: "inventory"}).confirmed, null);
    send("select", {query: "options", value: "one.Options"});
    assert.equal(send("confirm", {query: "options", catalog: catalog.slice(1)}).confirmed, null);
});

test("document reopen restores view state but does not carry over a pending double click", () => {
    const send = page();
    send("sync", {catalog});
    send("fold", {value: "One"});
    send("filter");
    const saved = send("select", {value: "one.Options"}).state;
    const reopened = page();
    assert.equal(reopened("sync", {catalog, init: true, state: saved, only: true}).selected, true);
    assert.equal(reopened("select", {value: "one.Options", now: 1100}).confirmed, null);
    assert.equal(reopened("confirm").confirmed, "one.Options");
});
