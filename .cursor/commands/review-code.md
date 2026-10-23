# Review code

Review the currently selected or changed files against `.cursor/rules`.

## Steps

1. Read every rule in `.cursor/rules`.
2. Review the selected files. If nothing is selected, review the changed files.
3. Record each violation with the file, line, severity, problem, and a suggested fix.
4. Use severity `blocker`, `major`, or `minor` only.
5. End with one verdict: `pass` when there are no blocker or major findings, otherwise `fail`.

## Output format

| file | line | severity | problem | suggested fix |
|---|---|---|---|---|
| path | n | blocker, major, or minor | what is wrong | what to change |

Verdict: pass or fail
