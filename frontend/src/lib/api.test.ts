import assert from "node:assert/strict";
import { afterEach, beforeEach, describe, test } from "node:test";
import { ApiError, ask, createTicket, listTickets } from "./api";

const ticket = {
  id: "TKT-1001",
  description: "Invoice export fails",
  status: "OPEN",
  priority: "HIGH",
  assignee: null,
  category: "billing",
  comments: [],
  resolutionNotes: null,
};

beforeEach(() => {
  process.env.NEXT_PUBLIC_API_URL = "http://localhost:8088";
});

describe("api", { concurrency: false }, () => {
  afterEach(() => {
    delete (globalThis as { fetch?: typeof fetch }).fetch;
  });

function stub(response: Response | (() => Promise<Response>)) {
  const calls: Request[] = [];
  globalThis.fetch = (async (input: RequestInfo | URL, init?: RequestInit) => {
    calls.push(new Request(input, init));
    return typeof response === "function" ? response() : response;
  }) as typeof fetch;
  return calls;
}

test("create returns the ticket id", async () => {
  stub(Response.json(ticket, { status: 201 }));
  const created = await createTicket({ description: "Invoice export fails", priority: "HIGH", category: "billing" });
  assert.equal(created.id, "TKT-1001");
});

test("list sends page, size, keyword, and status", async () => {
  const calls = stub(Response.json({ content: [], page: 0, size: 20, total: 0 }));
  await listTickets({ page: 0, size: 20, keyword: "export", status: "OPEN" });
  const url = new URL(calls[0].url);
  assert.equal(url.pathname, "/api/tickets");
  assert.equal(url.searchParams.get("page"), "0");
  assert.equal(url.searchParams.get("size"), "20");
  assert.equal(url.searchParams.get("keyword"), "export");
  assert.equal(url.searchParams.get("status"), "OPEN");
});

test("ask returns the answer and source", async () => {
  const calls = stub(
    Response.json({
      answer: "Exports fail. [TKT-1001]",
      sources: [{ ticketId: "TKT-1001", title: "Invoice export fails", similarity: 0.9 }],
      noRelevantTickets: false,
    }),
  );
  const result = await ask("Why do exports fail?");
  assert.equal(result.noRelevantTickets, false);
  assert.equal(result.sources[0].ticketId, "TKT-1001");
  assert.deepEqual(JSON.parse(await calls[0].text()), { question: "Why do exports fail?" });
});

test("validation error keeps the field error", async () => {
  stub(
    Response.json(
      {
        timestamp: "2026-10-23T14:00:00Z",
        status: 400,
        error: "Bad Request",
        message: "Validation failed",
        fieldErrors: [{ field: "description", message: "must not be blank" }],
      },
      { status: 400 },
    ),
  );
  await assert.rejects(createTicket({ description: "", priority: "HIGH", category: "billing" }), (error: unknown) => {
    assert.ok(error instanceof ApiError);
    assert.equal(error.status, 400);
    assert.equal(error.message, "Validation failed");
    assert.deepEqual(error.fieldErrors, [{ field: "description", message: "must not be blank" }]);
    return true;
  });
});

test("index failure keeps the message and an empty fieldErrors", async () => {
  stub(Response.json({ status: 503, message: "Ticket could not be indexed", fieldErrors: [] }, { status: 503 }));
  await assert.rejects(createTicket({ description: "A", priority: "HIGH", category: "billing" }), (error: unknown) => {
    assert.ok(error instanceof ApiError);
    assert.equal(error.status, 503);
    assert.equal(error.message, "Ticket could not be indexed");
    assert.deepEqual(error.fieldErrors, []);
    return true;
  });
});

test("a body that is not the error shape is unreachable", async () => {
  stub(new Response("nope", { status: 500 }));
  await assert.rejects(createTicket({ description: "A", priority: "HIGH", category: "billing" }), (error: unknown) => {
    assert.ok(error instanceof ApiError);
    assert.equal(error.message, "The server could not be reached.");
    return true;
  });
});

test("a network failure is unreachable", async () => {
  globalThis.fetch = (async () => {
    throw new TypeError("offline");
  }) as typeof fetch;
  await assert.rejects(createTicket({ description: "A", priority: "HIGH", category: "billing" }), (error: unknown) => {
    assert.ok(error instanceof ApiError);
    assert.equal(error.message, "The server could not be reached.");
    return true;
  });
});

test("a missing base URL does not call fetch", async () => {
  delete process.env.NEXT_PUBLIC_API_URL;
  let called = false;
  globalThis.fetch = (async () => {
    called = true;
    return Response.json(ticket);
  }) as typeof fetch;
  await assert.rejects(createTicket({ description: "A", priority: "HIGH", category: "billing" }), (error: unknown) => {
    assert.ok(error instanceof ApiError);
    assert.equal(error.message, "The server could not be reached.");
    return true;
  });
  assert.equal(called, false);
});
});
