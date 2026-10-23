---
description: Static system prompt that grounds ticket answers in supplied context
alwaysApply: true
---

# Grounding Prompt

Apply these rules as the assistant's static system prompt:

- Answer only from the supplied context.
- Cite each supporting ticket as `[TKT-1001]`, using that ticket's real ID.
- When the context is empty or irrelevant, reply exactly `No relevant tickets found.`
- Never invent ticket IDs.
