import json
import subprocess

import pytest

from wrims_mcp import runner
from wrims_mcp.runner import ValidatorConfig, validate

CONFIG = ValidatorConfig(java="java21", classpath="a/*:b/*", java_opts="-Xss1M -Xmx1G", timeout_s=5)

OK = {"ok": True, "errors": 0, "warnings": 0, "diagnostics": []}
BAD = {
    "ok": False,
    "errors": 1,
    "warnings": 0,
    "diagnostics": [{"severity": "error", "file": "x.wresl", "line": 4, "message": "Operator '<=' is not supported"}],
}


@pytest.fixture
def wresl(tmp_path):
    f = tmp_path / "main.wresl"
    f.write_text("sequence s1 { model m1 order 1 }")
    return f


def fake_run(monkeypatch, *, stdout="", stderr="", returncode=0, raises=None):
    calls = []

    def run(command, **kwargs):
        calls.append((command, kwargs))
        if raises:
            raise raises
        return subprocess.CompletedProcess(command, returncode, stdout=stdout, stderr=stderr)

    monkeypatch.setattr(runner.subprocess, "run", run)
    return calls


def only_message(result):
    assert result["ok"] is False and result["errors"] == 1
    return result["diagnostics"][0]["message"]


def test_clean_study_returns_validator_json(monkeypatch, wresl):
    fake_run(monkeypatch, stdout=json.dumps(OK), returncode=0)
    assert validate(str(wresl), CONFIG) == OK


def test_study_errors_are_returned_unchanged(monkeypatch, wresl):
    fake_run(monkeypatch, stdout=json.dumps(BAD), returncode=1)
    assert validate(str(wresl), CONFIG) == BAD


def test_command_is_a_list_with_absolute_path_and_configured_jvm(monkeypatch, wresl):
    calls = fake_run(monkeypatch, stdout=json.dumps(OK))
    validate(str(wresl), CONFIG)
    command, kwargs = calls[0]
    assert command == [
        "java21", "-Xss1M", "-Xmx1G", "-cp", "a/*:b/*", runner.MAIN_CLASS, str(wresl.resolve())
    ]
    assert kwargs.get("shell", False) is False
    assert kwargs["timeout"] == 5


def test_missing_file_is_reported_without_starting_java(monkeypatch, tmp_path):
    calls = fake_run(monkeypatch, stdout=json.dumps(OK))
    result = validate(str(tmp_path / "nope.wresl"), CONFIG)
    assert "File not found" in only_message(result)
    assert calls == []


def test_non_wresl_file_is_rejected_without_starting_java(monkeypatch, tmp_path):
    other = tmp_path / "study.config"
    other.write_text("x")
    calls = fake_run(monkeypatch, stdout=json.dumps(OK))
    assert "Not a .wresl file" in only_message(validate(str(other), CONFIG))
    assert calls == []


def test_java_not_installed(monkeypatch, wresl):
    fake_run(monkeypatch, raises=FileNotFoundError())
    assert "WRIMS_JAVA" in only_message(validate(str(wresl), CONFIG))


def test_timeout(monkeypatch, wresl):
    fake_run(monkeypatch, raises=subprocess.TimeoutExpired("java", 5))
    assert "within 5 seconds" in only_message(validate(str(wresl), CONFIG))


def test_usage_exit_code_reports_stderr(monkeypatch, wresl):
    fake_run(monkeypatch, stderr="Usage: ValidateStudy <path>", returncode=2)
    message = only_message(validate(str(wresl), CONFIG))
    assert "exit code 2" in message and "Usage: ValidateStudy" in message


def test_zero_exit_with_garbage_stdout_is_not_reported_clean(monkeypatch, wresl):
    fake_run(monkeypatch, stdout="not json at all", stderr="UnsupportedClassVersionError", returncode=0)
    message = only_message(validate(str(wresl), CONFIG))
    assert "UnsupportedClassVersionError" in message


def test_json_without_diagnostics_is_not_trusted(monkeypatch, wresl):
    fake_run(monkeypatch, stdout='{"ok": true}', returncode=0)
    assert validate(str(wresl), CONFIG)["ok"] is False


def test_long_stderr_is_truncated_to_its_tail(monkeypatch, wresl):
    fake_run(monkeypatch, stderr="x" * 10_000 + "THE END", returncode=3)
    message = only_message(validate(str(wresl), CONFIG))
    assert "THE END" in message and len(message) < 3_000


def test_config_from_env(monkeypatch):
    monkeypatch.setenv("WRIMS_JAVA", "/opt/jdk/bin/java")
    monkeypatch.setenv("WRIMS_CLASSPATH", "/jars/*")
    monkeypatch.setenv("WRIMS_JAVA_OPTS", "-Xmx2G")
    monkeypatch.setenv("WRIMS_VALIDATE_TIMEOUT_S", "30")
    assert ValidatorConfig.from_env() == ValidatorConfig("/opt/jdk/bin/java", "/jars/*", "-Xmx2G", 30.0)


def test_default_classpath_points_into_the_built_engine():
    classpath = runner.default_classpath()
    assert "wrims-core" in classpath and classpath.count("*") == 2
