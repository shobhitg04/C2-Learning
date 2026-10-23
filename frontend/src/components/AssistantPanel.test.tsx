import assert from "node:assert/strict";
import { afterEach, beforeEach, describe, test } from "node:test";
import { act, createElement, type ReactNode } from "react";
import { createRoot, type Root } from "react-dom/client";
import { renderToStaticMarkup } from "react-dom/server";
import RootLayout from "../app/layout";
import { AssistantPanel } from "./AssistantPanel";

let root: Root | undefined;

beforeEach(() => {
  process.env.NEXT_PUBLIC_API_URL = "http://localhost:8088";
  document.body.replaceChildren();
});

afterEach(() => {
  act(() => root?.unmount());
  root = undefined;
});

function render(node: ReactNode) {
  const host = document.createElement("div");
  document.body.appendChild(host);
  root = createRoot(host);
  act(() => root!.render(node));
  return host;
}

async function typeQuestion(host: HTMLElement, value: string) {
  const box = host.querySelector("textarea")!;
  const set = Object.getOwnPropertyDescriptor(window.HTMLTextAreaElement.prototype, "value")!.set!;
  await act(async () => {
    set.call(box, value);
    box.dispatchEvent(new window.Event("input", { bubbles: true }));
  });
}

function askButton(host: HTMLElement) {
  return host.querySelector("button")!;
}

describe("assistant panel", { concurrency: false }, () => {
  test("the layout mounts the panel", () => {
    const html = renderToStaticMarkup(createElement(RootLayout, null, "page"));
    assert.equal(html.includes("<main>page</main>"), true);
    assert.equal(html.includes(">Ask<"), true);
  });

  test("a blank question does not call ask", async () => {
    let called = false;
    globalThis.fetch = (async () => {
      called = true;
      return Response.json({});
    }) as typeof fetch;
    const host = render(createElement(AssistantPanel));
    await typeQuestion(host, "   ");
    await act(async () => {
      askButton(host).click();
    });
    assert.equal(host.textContent!.includes("must not be blank"), true);
    assert.equal(called, false);
  });

  test("ask sends only the question", async () => {
    const calls: Request[] = [];
    globalThis.fetch = (async (input: RequestInfo | URL, init?: RequestInit) => {
      calls.push(new Request(input, init));
      return Response.json({ answer: "Exports fail. [TKT-1001]", sources: [], noRelevantTickets: false });
    }) as typeof fetch;
    const host = render(createElement(AssistantPanel));
    await typeQuestion(host, "Why do quarterly invoice exports fail?");
    await act(async () => {
      askButton(host).click();
    });
    assert.deepEqual(JSON.parse(await calls[0].text()), { question: "Why do quarterly invoice exports fail?" });
    assert.equal(host.querySelector("[data-region=answer]")!.textContent, "Exports fail. [TKT-1001]");
  });

  test("a source links to the ticket", async () => {
    globalThis.fetch = (async () =>
      Response.json({
        answer: "Exports fail. [TKT-1001]",
        sources: [{ ticketId: "TKT-1001", title: "Invoice export fails", similarity: 0.91 }],
        noRelevantTickets: false,
      })) as typeof fetch;
    const host = render(createElement(AssistantPanel));
    await typeQuestion(host, "Why do exports fail?");
    await act(async () => {
      askButton(host).click();
    });
    const link = host.querySelector("a")!;
    assert.equal(link.getAttribute("href"), "/tickets/TKT-1001");
    assert.equal(link.textContent, "TKT-1001");
    assert.equal(host.textContent!.includes("Invoice export fails"), true);
    assert.equal(host.textContent!.includes("0.91"), true);
  });

  test("no match is not an empty answer or an error", async () => {
    globalThis.fetch = (async () =>
      Response.json({ answer: "No relevant tickets found.", sources: [], noRelevantTickets: true })) as typeof fetch;
    const host = render(createElement(AssistantPanel));
    await typeQuestion(host, "Nothing matches");
    await act(async () => {
      askButton(host).click();
    });
    assert.equal(host.querySelector("[data-region=nomatch]")!.textContent, "No relevant tickets found.");
    assert.equal(host.querySelector("[data-region=answer]"), null);
    assert.equal(host.querySelector("a"), null);
    assert.equal(host.querySelector("[role=alert]"), null);
  });

  test("a failed second ask keeps the first answer", async () => {
    const responses = [
      Response.json({
        answer: "Exports fail. [TKT-1001]",
        sources: [{ ticketId: "TKT-1001", title: "Invoice export fails", similarity: 0.91 }],
        noRelevantTickets: false,
      }),
      Response.json({ status: 503, message: "Question could not be answered", fieldErrors: [] }, { status: 503 }),
    ];
    globalThis.fetch = (async () => responses.shift()!) as typeof fetch;
    const host = render(createElement(AssistantPanel));
    await typeQuestion(host, "Why do exports fail?");
    await act(async () => {
      askButton(host).click();
    });
    await typeQuestion(host, "Ask again");
    await act(async () => {
      askButton(host).click();
    });
    assert.equal(host.querySelector("[role=alert]")!.textContent, "Question could not be answered");
    assert.equal(host.querySelector("[data-region=answer]")!.textContent, "Exports fail. [TKT-1001]");
  });

  test("a network failure keeps the previous answer", async () => {
    let first = true;
    globalThis.fetch = (async () => {
      if (first) {
        first = false;
        return Response.json({ answer: "Exports fail. [TKT-1001]", sources: [], noRelevantTickets: false });
      }
      throw new TypeError("offline");
    }) as typeof fetch;
    const host = render(createElement(AssistantPanel));
    await typeQuestion(host, "Why do exports fail?");
    await act(async () => {
      askButton(host).click();
    });
    await typeQuestion(host, "Ask again");
    await act(async () => {
      askButton(host).click();
    });
    assert.equal(host.querySelector("[role=alert]")!.textContent, "The server could not be reached.");
    assert.equal(host.querySelector("[data-region=answer]")!.textContent, "Exports fail. [TKT-1001]");
  });

  test("ask shows Asking and disables the button", async () => {
    let release: (response: Response) => void = () => {};
    globalThis.fetch = (() => new Promise<Response>((resolve) => {
      release = resolve;
    })) as typeof fetch;
    const host = render(createElement(AssistantPanel));
    await typeQuestion(host, "Why do exports fail?");
    await act(async () => {
      askButton(host).click();
      await new Promise((resolve) => setTimeout(resolve, 0));
      assert.equal(host.textContent!.includes("Asking..."), true);
      assert.equal(askButton(host).disabled, true);
      release(Response.json({ answer: "Exports fail.", sources: [], noRelevantTickets: false }));
    });
  });
});
