const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");
const vm = require("node:vm");

test("the category disclosure opens and closes the list together with its indicator", () => {
    const attributes = {};
    let start, toggle, marker;
    const elements = {
        category_toggle: {
            addEventListener: (event, callback) => {
                if (event === "click") toggle = callback;
            },
            setAttribute: (key, value) => attributes[key] = value
        },
        category_content: {setAttribute: (key, value) => attributes[key] = value},
        category_indicator: {setInnerText: value => marker = value}
    };
    const document = {
        addEventListener: (event, callback) => {
            if (event === "DOMContentLoaded") start = callback;
        },
        getElementById: id => elements[id]
    };
    const file = path.join(__dirname, "../../main/resources/assets/apricityui/apricity/banira_codex/ui/native-category.js");
    vm.runInNewContext(fs.readFileSync(file, "utf8"), {document});
    start();
    toggle();
    assert.deepEqual(attributes, {"aria-expanded": "true", "aria-hidden": "false"});
    assert.equal(marker, "\u25be");
    toggle();
    assert.deepEqual(attributes, {"aria-expanded": "false", "aria-hidden": "true"});
    assert.equal(marker, "\u25b8");
});
