"""Runs the real ValidateStudy command. Skipped unless a built wrims-core and Java 21 are available.

Build the engine with ``./gradlew :wrims-core:build`` and, if the java on PATH is older than 21,
set WRIMS_JAVA to a Java 21 executable.
"""

import glob
import os
import re
import subprocess

import pytest

from wrims_mcp.runner import ValidatorConfig, validate


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

pytestmark = pytest.mark.skipif(
    _major is None or _major < 21 or not _jars_built,
    reason="needs Java 21 (set WRIMS_JAVA) and a built wrims-core (./gradlew :wrims-core:build)",
)


def _study(tmp_path, model_body):
    main = tmp_path / "main.wresl"
    main.write_text("sequence s1 { model m1 order 1 }\nmodel m1 {\n" + model_body + "\n}\n")
    return str(main)


def test_clean_study(tmp_path):
    result = validate(_study(tmp_path, "define d {std kind 'FLOW' units 'CFS'}\ngoal g {d < 5}"), CONFIG)
    assert result["ok"] is True, result
    assert result["diagnostics"] == []


def test_goal_with_less_or_equal_is_an_error_with_file_and_line(tmp_path):
    result = validate(_study(tmp_path, "define d {std kind 'FLOW' units 'CFS'}\ngoal g {d <= 5}"), CONFIG)
    assert result["ok"] is False
    error = result["diagnostics"][0]
    assert error["severity"] == "error" and error["line"] == 4
    assert error["file"].endswith("main.wresl")
    assert "'<='" in error["message"]


def test_lagged_state_variable_is_a_warning(tmp_path):
    body = "define x {value 1.0}\ndefine d {std kind 'FLOW' units 'CFS'}\ndefine y {value x(-1) + 2.0}\ngoal g {d < y}"
    result = validate(_study(tmp_path, body), CONFIG)
    assert result["ok"] is True and result["warnings"] == 1, result
    assert "state variable [x]" in result["diagnostics"][0]["message"]
