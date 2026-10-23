# Graph Report - .  (2026-10-23)

## Corpus Check
- Corpus is ~46 words - fits in a single context window. You may not need a graph.

## Summary
- 8 nodes · 10 edges · 3 communities (2 shown, 1 thin omitted)
- Extraction: 90% EXTRACTED · 10% INFERRED · 0% AMBIGUOUS · INFERRED: 1 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Session Capture
- Skill Loading
- Graphify Naming

## God Nodes (most connected - your core abstractions)
1. `grok-4.7` - 4 edges
2. `Enable Graphiphy` - 3 edges
3. `Graphify Skill` - 3 edges
4. `Cursor IDE Session` - 3 edges
5. `Graphiphy` - 2 edges
6. `Graphify` - 2 edges
7. `read_file_v2` - 2 edges
8. `SpecStory` - 1 edges

## Surprising Connections (you probably didn't know these)
- `Enable Graphiphy` --references--> `Graphiphy`  [EXTRACTED]
  .specstory/history/2026-10-23_13-10-10Z-graphiphy-folder-enablement.md → .specstory/history/2026-10-23_13-10-10Z-graphiphy-folder-enablement.md  _Bridges community 2 → community 0_
- `grok-4.7` --references--> `Enable Graphiphy`  [EXTRACTED]
  .specstory/history/2026-10-23_13-10-10Z-graphiphy-folder-enablement.md → .specstory/history/2026-10-23_13-10-10Z-graphiphy-folder-enablement.md  _Bridges community 0 → community 1_
- `Graphify Skill` --references--> `Graphify`  [EXTRACTED]
  .specstory/history/2026-10-23_13-10-10Z-graphiphy-folder-enablement.md → .specstory/history/2026-10-23_13-10-10Z-graphiphy-folder-enablement.md  _Bridges community 2 → community 1_

## Hyperedges (group relationships)
- **Graphiphy Folder Enablement** — specstory_history_2026_10_23_13_10_10z_graphiphy_folder_enablement_enable_graphiphy, specstory_history_2026_10_23_13_10_10z_graphiphy_folder_enablement_graphiphy, specstory_history_2026_10_23_13_10_10z_graphiphy_folder_enablement_graphify_skill, specstory_history_2026_10_23_13_10_10z_graphiphy_folder_enablement_grok_4_7 [EXTRACTED 1.00]

## Communities (3 total, 1 thin omitted)

### Community 0 - "Session Capture"
Cohesion: 0.67
Nodes (3): Cursor IDE Session, Enable Graphiphy, SpecStory

### Community 1 - "Skill Loading"
Cohesion: 1.00
Nodes (3): Graphify Skill, grok-4.7, read_file_v2

## Knowledge Gaps
- **1 isolated node(s):** `SpecStory`
  These have ≤1 connection - possible missing edges or undocumented components.
- **1 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `grok-4.7` connect `Skill Loading` to `Session Capture`?**
  _High betweenness centrality (0.357) - this node is a cross-community bridge._
- **Why does `Cursor IDE Session` connect `Session Capture` to `Skill Loading`?**
  _High betweenness centrality (0.286) - this node is a cross-community bridge._
- **Why does `Enable Graphiphy` connect `Session Capture` to `Skill Loading`, `Graphify Naming`?**
  _High betweenness centrality (0.214) - this node is a cross-community bridge._
- **What connects `SpecStory` to the rest of the system?**
  _1 weakly-connected nodes found - possible documentation gaps or missing edges._