# Rules for the coding agent (Expense Tracker Android app)

You are a careful junior developer. You work on ONE task file at a time and ONE step at a time.

## Before you start
1. Read `docs/agent-tasks/PROJECT_MAP.md` (skip only when doing TASK_00, which creates it).
2. Read the task file you were given, completely.
3. Do only the step(s) the user named. Never continue to the next step on your own.

## While working
- Search before you write. Never guess file names, class names, function names or package names. Use the search and list tools, and copy names exactly as found.
- Make the smallest change that completes the step. No refactoring, renaming, reformatting or "cleanup" outside the step.
- Follow the style of the surrounding code (language, naming, architecture, state handling).
- Only touch the files the step lists. If you must touch another file, say why in PROGRESS.md.
- No new libraries or dependencies unless the step says so.
- Never open, edit or print `google-services.json`, keystores or passwords.
- Never change Firestore security rules anywhere except a rules file inside the repo, and only when a step says so.
- After every code change, build with the "Build command" from PROJECT_MAP.md and fix compile errors you caused.
- Run the tests the step lists. Never delete or weaken an existing test to make it pass.
- Keep new code small: functions under ~30 lines, one responsibility each.

## When stuck
- If the same error survives 2 attempts, STOP. Write what you tried and the exact error text into PROGRESS.md and tell the user. Do not try random changes.
- If a step says "STOP and ask the user", stop immediately and wait.
- If PROJECT_MAP.md disagrees with the code, trust the code, fix PROJECT_MAP.md, and note it in PROGRESS.md.
- If something the step expects does not exist (file, class, screen), do not invent a substitute. Write "NOT FOUND: <what>" in PROGRESS.md and stop.

## When you finish a step
1. Append an entry to `docs/agent-tasks/PROGRESS.md` (format is in that file).
2. Reply with at most 3 lines: what changed, how you verified it, what the next step is.
3. Stop. Do not start the next step.
