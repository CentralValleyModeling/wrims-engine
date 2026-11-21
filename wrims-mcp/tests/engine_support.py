"""Shared setup for the tests that run the real WRIMS engine.

Those tests are skipped unless Java 21 and a built wrims-core are found. Build the engine with
``./gradlew :wrims-core:build`` and, if the java on PATH is older than 21, set WRIMS_JAVA to a
Java 21 executable.
"""

import glob
import os
import re
import subprocess
from pathlib import Path

import pytest

from wrims_mcp.runner import ValidatorConfig


def _java_major(java: str) -> int | None:
    try:
        out = subprocess.run([java, "-version"], capture_output=True, text=True, timeout=30)
    except (OSError, subprocess.TimeoutExpired):
        return None
    match = re.search(r'version "(\d+)', out.stderr + out.stdout)
    return int(match.group(1)) if match else None


CONFIG = ValidatorConfig.from_env()
_major = _java_major(CONFIG.java)
_jars_built = all(glob.glob(part) for part in CONFIG.classpath.split(os.pathsep))

requires_engine = pytest.mark.skipif(
    _major is None or _major < 21 or not _jars_built,
    reason="needs Java 21 (set WRIMS_JAVA) and a built wrims-core (./gradlew :wrims-core:build)",
)

GOAL_WITH_LESS_OR_EQUAL = "define d {std kind 'FLOW' units 'CFS'}\ngoal g {d <= 5}"


def write_study(directory: Path, model_body: str) -> str:
    """Writes a one-model study and returns the path of its main file."""
    main = directory / "main.wresl"
    main.write_text("sequence s1 { model m1 order 1 }\nmodel m1 {\n" + model_body + "\n}\n")
    return str(main)
