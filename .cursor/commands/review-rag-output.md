# Review RAG output

Given a question, the retrieved ticket chunks, and the assistant answer, check every claim in the answer against the chunks.

## Steps

1. Read the question, the retrieved ticket chunks, and the assistant answer.
2. Split the answer into claims.
3. Match each claim to a ticket ID in the retrieved chunks, or mark the support `NONE`.
4. Mark each claim `grounded`, `partially grounded`, or `hallucinated`.
5. Flag any ticket ID in the answer that is not in the retrieved chunks.
6. If the context was empty, verify the answer is exactly `No relevant tickets found.`
7. End with an overall grounded yes or no. Use yes only when every claim is grounded and no ticket ID is missing from the chunks.

## Output format

| claim | supporting ticket ID or NONE | verdict |
|---|---|---|
| claim text | TKT-1001 or NONE | grounded, partially grounded, or hallucinated |

### Ticket IDs not in the retrieved chunks

- ID — or `none`

### Empty context

- Context empty: yes or no
- Answer is exactly `No relevant tickets found.`: yes, no, or n/a

Overall grounded: yes or no
