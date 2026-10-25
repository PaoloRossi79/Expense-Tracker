# 01 — Using these tasks with Kilo + Ollama / DeepSeek

The tasks are written for a model that is **less capable than a frontier model**: small steps, exact file names, exact signatures, test tables, and an explicit "do not" list. This guide makes that work in practice.

## 1. One-time setup

### Ollama context size (important)

Small default context windows silently truncate long prompts, and the model then "forgets" the task file. Set the context explicitly.

`Modelfile`:

```
FROM <the model you use, e.g. the exact tag shown by `ollama list`>
PARAMETER num_ctx 32768
PARAMETER temperature 0.2
```

```
ollama create coder-32k -f Modelfile
```

Then pick that model in Kilo and set *Context Window Size (num_ctx)* in the provider settings to the same value. Models Kilo's docs suggest for local use: `qwen3-coder:30b` (main choice) or `devstral:24b`; they want roughly 24 GB VRAM or a Mac with 32 GB+. If the model calls a tool wrongly, rephrase the prompt or use the *Enhance Prompt* button; keep sessions short and disable MCP tools. Kilo's own Ollama documentation says you need at least 32k context for decent results (the Ollama default is far smaller and silently truncates the task). 32768 fits one task file + architecture + a reference file + two or three sources. If your GPU runs out of memory, lower to 16384, never below that. Low temperature (0.1–0.3) gives more repeatable code.

### Project rules for Kilo

Create `.kilocode/rules/screenshot-import.md` (Kilo still loads this folder for backward compatibility; newer Kilo versions read `instructions` globs from `kilo.jsonc`, e.g. `.kilo/rules/*.md`; otherwise paste the text into *Custom instructions*) with exactly this:

```
You are implementing one task from docs/screenshot-import/tasks/.
1. Read the task file completely, then docs/screenshot-import/00-ARCHITECTURE.md and docs/APP_CONTEXT.md.
2. Do ONLY what the task says. Do not refactor, rename, reformat or "improve" other files.
3. Write the tests first (they are specified in the task), run them, see them fail, then implement.
4. Use the exact class, function and file names from the task and the architecture file.
5. Never add network calls, logging of OCR text, vendors or amounts, or analytics.
6. Never open, print or paste files from fixtures-private/.
7. Run the exact command in "Done when". If it fails 3 times, STOP and report the failing output; do not keep guessing.
8. When done, print: files created/changed, command run, result. Then stop.
```

### Git

Work on a branch `feature/screenshot-import`. **One commit per task** with the commit message given in the task. If a task goes wrong, `git restore .` and start the session again; this is cheaper than repairing a confused session.

## 2. Per-task routine (copy this every time)

1. **New chat/session for every task.** Long histories make small models worse.
2. Mode: *Code* for implementation. Switch to *Debug* only if tests fail after the implementation is in place. (Mode names can differ between Kilo versions.)
3. Attach/mention only:
   - `docs/screenshot-import/tasks/Txx-….md`
   - `docs/screenshot-import/00-ARCHITECTURE.md`
   - `docs/APP_CONTEXT.md` (exists after T00)
   - the files listed under "Read first" in the task, nothing else.
4. Paste this prompt:

```
Implement task Txx exactly as written in the attached task file.
Follow the project rules. Write the tests first, then the code.
Run the command under "Done when". Stop when it passes and show me the summary.
If something in the task file is unclear or contradicts the code, stop and ask me instead of guessing.
```

5. Review the diff yourself (5 minutes): only the listed files changed? tests meaningful? Then commit.
6. Tick the task in `README.md`.

## 3. Command approvals

Let Kilo auto-approve **reading files** and **editing files inside the project**. Keep **manual approval for terminal commands** (you want to see `./gradlew …`, and nothing that deletes or uploads). Never auto-approve commands that touch folders outside the project.

## 4. When the model struggles

| Symptom | What to do |
|---|---|
| Tests fail 3 times in a row | Stop. Start a new session in *Debug* mode with only: the task file, the failing test output, the one source file under repair |
| Model rewrites other files | `git restore` those files, repeat the prompt and add "Change only these files: …" |
| It invents class names | Point it at `docs/APP_CONTEXT.md` and the architecture file; say "use only names from these files" |
| It produces a huge refactor | Reject. The tasks are intentionally narrow |
| Compose/UI code does not compile | Give it the existing screen file as a pattern (listed in APP_CONTEXT) and ask it to copy that style |
| A bank parser will not reach green | The problem is usually the **profile values** or the **expected file**, not the code: run the diagnostics in the bank task, check the worksheet, or bring the anonymised fixture to me (Claude) |

## 5. Local vs DeepSeek (cloud)

- **Code and task files** contain no personal data: using DeepSeek's API or any cloud model for them is a normal engineering decision.
- **Screenshots, raw OCR dumps and non-anonymised fixtures never go into any chat**, local or cloud. They hold real transactions. Only anonymised fixtures (checklist in `02-…`) may be attached.
- If you want zero code leaving the machine, use the local Ollama model for everything; the tasks are sized for that.
- A mixed strategy works well: local model for the easy tasks (T02–T06, T10, T17, T19), a stronger model for the tricky ones (T07, T08, bank parsers, T22).

## 6. Reference code and escalation

Tasks point to verified code in `reference/` (98+ JVM tests pass). Standard routine: let the model write the tests and code from the task; if it fails twice, give it the reference file and say "adapt this to the project". Do not skip the tests.

## 7. Order and sizing

Do the tasks strictly in the order of `README.md`. Each task is meant to fit in a single session of 10–30 minutes of model time. If a session runs long, the task is probably being over-interpreted: stop, restore, retry with the prompt above.
