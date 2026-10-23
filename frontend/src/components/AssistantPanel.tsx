"use client";

import { useState, type FormEvent } from "react";
import { ApiError, ask, type AskResponse } from "../lib/api";

const UNREACHABLE = "The server could not be reached.";

export function AssistantPanel() {
  const [question, setQuestion] = useState("");
  const [blank, setBlank] = useState(false);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<AskResponse | null>(null);
  const [error, setError] = useState("");

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!question.trim()) {
      setBlank(true);
      return;
    }
    setBlank(false);
    setError("");
    setLoading(true);
    try {
      setResult(await ask(question.trim()));
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : UNREACHABLE);
    } finally {
      setLoading(false);
    }
  }

  return (
    <aside>
      <form onSubmit={submit}>
        <label>
          Question
          <textarea value={question} onChange={(event) => setQuestion(event.target.value)} />
        </label>
        {blank ? <p>must not be blank</p> : null}
        <button type="submit" disabled={loading}>
          Ask
        </button>
      </form>
      {loading ? <p>Asking...</p> : null}
      {error ? <p role="alert">{error}</p> : null}
      {result?.noRelevantTickets ? <p data-region="nomatch">{result.answer}</p> : null}
      {result && !result.noRelevantTickets ? <p data-region="answer">{result.answer}</p> : null}
      {result && !result.noRelevantTickets ? (
        <ul>
          {result.sources.map((source) => (
            <li key={source.ticketId}>
              <a href={`/tickets/${source.ticketId}`}>{source.ticketId}</a> {source.title} {source.similarity}
            </li>
          ))}
        </ul>
      ) : null}
    </aside>
  );
}
