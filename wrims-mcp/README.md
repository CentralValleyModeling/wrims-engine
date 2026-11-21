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

Unit tests run anywhere. The three integration tests run the real engine and are skipped unless
Java 21 and a built `wrims-core` are found.

## Side effects

The engine writes a log file named `=WreslCheck_<name>=.log` next to the main file on every check.
