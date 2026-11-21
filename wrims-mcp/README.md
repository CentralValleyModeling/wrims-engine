# wrims-mcp

An [MCP](https://modelcontextprotocol.io) server that lets an AI agent check WRESL+ code and read
structured feedback from the WRIMS engine. It wraps the engine's `ValidateStudy` command
(`wrims-core`, package `debug`), so the agent sees exactly what the engine reports.

## Tool

`wresl_validate(main_file)` compile-checks the study rooted at a main `.wresl` file, including
every file it includes. It does **not** run the study. It returns:

```json
{"ok": false, "errors": 1, "warnings": 0,
 "diagnostics": [{"severity": "error", "file": "...\\goals.wresl", "line": 12,
                  "message": "Operator '<=' is not supported in a goal; use '<' (it already includes equality)."}]}
```

A clean result does not prove the code runs: some problems only appear when the study solves.
Problems running the validator itself (Java missing, wrong version, timeout) come back in the same
format, so an agent handles one shape.

## Requirements

- Python 3.10 or later
- Java 21
- A built engine: `./gradlew :wrims-core:build` from the repository root (no native libraries are
  needed, because nothing is solved)

## Install

From this folder:

```powershell
python -m venv .venv
.venv\Scripts\python -m pip install -e ".[test]"
```

## Configuration

All settings are optional environment variables.

| Variable | Default | Meaning |
| --- | --- | --- |
| `WRIMS_JAVA` | `java` | Java 21 executable. Set this if the `java` on `PATH` is older. |
| `WRIMS_CLASSPATH` | `wrims-core/build/libs/*` and `wrims-core/build/tmp/libs/*` of this checkout | Jars that contain `ValidateStudy` and its dependencies |
| `WRIMS_JAVA_OPTS` | `-Xss1024K -Xmx4096m` | JVM options. The deep thread stack is needed by the parser. |
| `WRIMS_VALIDATE_TIMEOUT_S` | `600` | Seconds before a validation is given up |

## Use with Claude Code

```powershell
claude mcp add wrims-wresl -e WRIMS_JAVA="C:\path\to\jdk-21\bin\java.exe" -- C:\path\to\wrims-engine\wrims-mcp\.venv\Scripts\python.exe -m wrims_mcp
```

Or in a project's `.mcp.json`:

```json
{
  "mcpServers": {
    "wrims-wresl": {
      "command": "C:\\path\\to\\wrims-engine\\wrims-mcp\\.venv\\Scripts\\python.exe",
      "args": ["-m", "wrims_mcp"],
      "env": { "WRIMS_JAVA": "C:\\path\\to\\jdk-21\\bin\\java.exe" }
    }
  }
}
```

## Tests

```powershell
.venv\Scripts\python -m pytest
```

There are three layers. Each one adds something the layer before it cannot show.

| File | Layer | What it proves | Needs the engine |
| --- | --- | --- | --- |
| `test_runner.py`, `test_server.py` | Unit | How the server builds the Java command and handles every way the validator can fail (missing file, Java not installed, timeout, bad exit code, garbage output, oversized stderr). The tool is registered with the right schema and returns the runner's result as JSON text. The process call is stubbed. | No |
| `test_integration.py` | Runner and engine | The runner and the real `ValidateStudy` command agree on the result format, and the engine's checks give the expected file, line and message. | Yes |
| `test_e2e_stdio.py` | End to end | An MCP client can start the server as its own process, complete the handshake, list the tool and call it over stdio, and the answer comes from the real engine. This is the path an agent such as Claude Code uses. | Yes |

The tests marked "Yes" are skipped unless Java 21 and a built `wrims-core` are found (see
`tests/engine_support.py`). To run them:

```powershell
cd ..
.\gradlew :wrims-core:build
cd wrims-mcp
$env:WRIMS_JAVA = "C:\path\to\jdk-21\bin\java.exe"   # only if the java on PATH is older than 21
.venv\Scripts\python -m pytest -v
```

### What the end-to-end tests do

Each test writes a one-model study to a temporary folder, starts `python -m wrims_mcp` as a child
process, and talks to it as an MCP client. They check three things:

1. **A bad goal gives structured feedback.** A study with `goal g {d <= 5}` returns
   `ok: false` with one error. The error names `main.wresl`, line 4, and says to use `<`.
2. **The feedback leads to a fix.** The same study with `<` instead of `<=` returns a clean
   result (`ok: true`, no diagnostics). Together with the first test, this is the loop an agent
   follows: check, read the message, edit, check again.
3. **A bad path is a result, not a crash.** A missing file returns `ok: false` with a
   "File not found" message, and the call is not a protocol error (`is_error` is false), so the
   agent can read the message and carry on.

In all three, a study with errors is a normal result. Only a failure of the protocol itself would
be a tool error.

### What the tests do not cover

- An agent actually writing WRESL: the tests supply the code. To try that, register the server
  with Claude Code (see above) and ask it to write a file.
- Running a study. The server only compile-checks.
- Linux or macOS. The tests have only been run on Windows.

## Side effects

The engine writes a log file named `=WreslCheck_<name>=.log` next to the main file on every check.
