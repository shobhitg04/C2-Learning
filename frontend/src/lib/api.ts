export type FieldError = { field: string; message: string };

export class ApiError extends Error {
  readonly status: number;
  readonly fieldErrors: FieldError[];

  constructor(status: number, message: string, fieldErrors: FieldError[] = []) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

export type Comment = { id: number; body: string; createdAt: string };

export type Ticket = {
  id: string;
  description: string;
  status: string;
  priority: string;
  assignee: string | null;
  category: string;
  comments: Comment[];
  resolutionNotes: string | null;
};

export type TicketSummary = {
  id: string;
  description: string;
  status: string;
  priority: string;
  assignee: string | null;
  category: string;
};

export type TicketPage = { content: TicketSummary[]; page: number; size: number; total: number };

export type TicketPatch = {
  status?: string;
  assignee?: string | null;
  resolutionNotes?: string | null;
};

export type AskSource = { ticketId: string; title: string; similarity: number };

export type AskResponse = { answer: string; sources: AskSource[]; noRelevantTickets: boolean };

const UNREACHABLE = "The server could not be reached.";

function origin(): string {
  const url = process.env.NEXT_PUBLIC_API_URL;
  if (!url) {
    throw new ApiError(0, UNREACHABLE);
  }
  return url;
}

async function send<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(new URL(path, origin()), init);
  } catch {
    throw new ApiError(0, UNREACHABLE);
  }
  if (!response.ok) {
    throw await failed(response);
  }
  return (await response.json()) as T;
}

async function failed(response: Response): Promise<ApiError> {
  try {
    const body = await response.json();
    if (body && typeof body.message === "string" && Array.isArray(body.fieldErrors)) {
      return new ApiError(typeof body.status === "number" ? body.status : response.status, body.message, body.fieldErrors);
    }
  } catch {
    // The body is not the shared error shape.
  }
  return new ApiError(response.status, UNREACHABLE);
}

function json(method: string, body: unknown): RequestInit {
  return { method, headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) };
}

export function createTicket(ticket: { description: string; priority: string; category: string; assignee?: string | null }) {
  return send<Ticket>("/api/tickets", json("POST", ticket));
}

export function listTickets(query: { page?: number; size?: number; keyword?: string; status?: string } = {}) {
  const params = new URLSearchParams();
  if (query.page !== undefined) params.set("page", String(query.page));
  if (query.size !== undefined) params.set("size", String(query.size));
  if (query.keyword !== undefined) params.set("keyword", query.keyword);
  if (query.status !== undefined) params.set("status", query.status);
  const qs = params.toString();
  return send<TicketPage>("/api/tickets" + (qs ? `?${qs}` : ""));
}

export function getTicket(id: string) {
  return send<Ticket>(`/api/tickets/${encodeURIComponent(id)}`);
}

export function updateTicket(id: string, patch: TicketPatch) {
  const body: TicketPatch = {};
  if ("status" in patch) body.status = patch.status;
  if ("assignee" in patch) body.assignee = patch.assignee;
  if ("resolutionNotes" in patch) body.resolutionNotes = patch.resolutionNotes;
  return send<Ticket>(`/api/tickets/${encodeURIComponent(id)}`, json("PATCH", body));
}

export function addComment(id: string, body: string) {
  return send<Comment>(`/api/tickets/${encodeURIComponent(id)}/comments`, json("POST", { body }));
}

export function ask(question: string) {
  return send<AskResponse>("/api/ai/ask", json("POST", { question }));
}
