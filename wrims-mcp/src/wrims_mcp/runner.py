"""Runs the WRIMS engine's ``ValidateStudy`` command and returns its result as a dict.

The validator runs in its own JVM for every call: the WRESL+ parser keeps static state, so a
fresh process keeps one validation from affecting the next, and a crashed compile cannot take
this server down with it.
"""

from __future__ import annotations

import json
import os
import shlex
import subprocess
from dataclasses import dataclass, field
from pathlib import Path

MAIN_CLASS = "gov.ca.water.wrims.engine.core.debug.ValidateStudy"
DEFAULT_JAVA_OPTS = "-Xss1024K -Xmx4096m"  # the deep thread stack is needed by the WRESL+ parser
DEFAULT_TIMEOUT_S = 600.0
STDERR_TAIL_CHARS = 2000


def default_classpath() -> str:
    """Classpath of a built wrims-engine checkout that contains this package."""
    root = Path(__file__).resolve().parents[3]
    libs = root / "wrims-core" / "build" / "libs"
    deps = root / "wrims-core" / "build" / "tmp" / "libs"
    return os.pathsep.join([str(libs / "*"), str(deps / "*")])


@dataclass(frozen=True)
class ValidatorConfig:
    java: str = "java"
    classpath: str = field(default_factory=default_classpath)
    java_opts: str = DEFAULT_JAVA_OPTS
    timeout_s: float = DEFAULT_TIMEOUT_S

    @classmethod
    def from_env(cls) -> "ValidatorConfig":
        """Reads WRIMS_JAVA, WRIMS_CLASSPATH, WRIMS_JAVA_OPTS and WRIMS_VALIDATE_TIMEOUT_S."""
        env = os.environ
        return cls(
            java=env.get("WRIMS_JAVA", "java"),
            classpath=env.get("WRIMS_CLASSPATH") or default_classpath(),
            java_opts=env.get("WRIMS_JAVA_OPTS", DEFAULT_JAVA_OPTS),
            timeout_s=float(env.get("WRIMS_VALIDATE_TIMEOUT_S", DEFAULT_TIMEOUT_S)),
        )


def _failure(message: str) -> dict:
    """A result in the validator's own format, for when the validator itself could not run."""
    return {
        "ok": False,
        "errors": 1,
        "warnings": 0,
        "diagnostics": [{"severity": "error", "file": None, "line": None, "message": message}],
    }


def _tail(text: str) -> str:
    text = text.strip()
    return text[-STDERR_TAIL_CHARS:]


def validate(main_file: str, config: ValidatorConfig | None = None) -> dict:
    """Compile-checks the WRESL+ study rooted at ``main_file``.

    Returns the validator's JSON object (``ok``, ``errors``, ``warnings``, ``diagnostics``).
    Problems running the validator are reported in the same format, so callers handle one shape.
    """
    config = config or ValidatorConfig.from_env()
    path = Path(main_file).expanduser()

    if path.suffix.lower() != ".wresl":
        return _failure(f"Not a .wresl file: {main_file}")
    if not path.is_file():
        return _failure(f"File not found: {main_file}")

    command = [
        config.java,
        *shlex.split(config.java_opts),
        "-cp",
        config.classpath,
        MAIN_CLASS,
        str(path.resolve()),
    ]

    try:
        completed = subprocess.run(
            command, capture_output=True, text=True, timeout=config.timeout_s, check=False
        )
    except FileNotFoundError:
        return _failure(
            f"Could not start Java ('{config.java}'). Install Java 21 or set WRIMS_JAVA to its java executable."
        )
    except subprocess.TimeoutExpired:
        return _failure(f"Validation did not finish within {config.timeout_s:g} seconds.")

    # Exit code 0 = clean, 1 = the study has errors; both come with the JSON result on stdout.
    if completed.returncode in (0, 1):
        try:
            result = json.loads(completed.stdout)
        except json.JSONDecodeError:
            result = None
        if isinstance(result, dict) and "diagnostics" in result:
            return result

    detail = _tail(completed.stderr) or _tail(completed.stdout) or "no output"
    return _failure(
        f"The validator failed to run (exit code {completed.returncode}). "
        f"Check that WRIMS_CLASSPATH points to a built wrims-core and that Java is version 21. Details: {detail}"
    )
