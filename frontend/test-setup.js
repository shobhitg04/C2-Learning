import { JSDOM } from "jsdom";

const dom = new JSDOM("<!doctype html><html><body></body></html>", { url: "http://localhost:3000/" });
const globals = globalThis;
globals.window = dom.window;
globals.document = dom.window.document;
globals.navigator = dom.window.navigator;
globals.HTMLElement = dom.window.HTMLElement;
globals.HTMLTextAreaElement = dom.window.HTMLTextAreaElement;
globals.Element = dom.window.Element;
globals.Node = dom.window.Node;
globals.DocumentFragment = dom.window.DocumentFragment;
globals.MutationObserver = dom.window.MutationObserver;
globals.getComputedStyle = dom.window.getComputedStyle.bind(dom.window);
globals.IS_REACT_ACT_ENVIRONMENT = true;
