"""Runner against the real ValidateStudy command (no MCP protocol involved).

Skipped unless Java 21 and a built wrims-core are available; see engine_support.py.
"""

from engine_support import CONFIG, GOAL_WITH_LESS_OR_EQUAL, requires_engine, write_study

from wrims_mcp.runner import validate

pytestmark = requires_engine


def test_clean_study(tmp_path):
    result = validate(write_study(tmp_path, "define d {std kind 'FLOW' units 'CFS'}\ngoal g {d < 5}"), CONFIG)
    assert result["ok"] is True, result
    assert result["diagnostics"] == []


def test_goal_with_less_or_equal_is_an_error_with_file_and_line(tmp_path):
    result = validate(write_study(tmp_path, GOAL_WITH_LESS_OR_EQUAL), CONFIG)
    assert result["ok"] is False
    error = result["diagnostics"][0]
    assert error["severity"] == "error" and error["line"] == 4
    assert error["file"].endswith("main.wresl")
    assert "'<='" in error["message"]


def test_lagged_state_variable_is_a_warning(tmp_path):
    body = "define x {value 1.0}\ndefine d {std kind 'FLOW' units 'CFS'}\ndefine y {value x(-1) + 2.0}\ngoal g {d < y}"
    result = validate(write_study(tmp_path, body), CONFIG)
    assert result["ok"] is True and result["warnings"] == 1, result
    assert "state variable [x]" in result["diagnostics"][0]["message"]
