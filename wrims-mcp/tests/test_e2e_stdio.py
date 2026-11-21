"""End to end: an MCP client talks to the server over stdio, and the server runs the real engine.

This is the path an agent such as Claude Code uses. It starts the server as a separate process
(``python -m wrims_mcp``), performs the MCP handshake, lists the tools and calls ``wresl_validate``.
Skipped unless Java 21 and a built wrims-core are available; see engine_support.py.
"""

import json
import os
import sys
from pathlib import Path

import anyio
from engine_support import CONFIG, GOAL_WITH_LESS_OR_EQUAL, requires_engine, write_study
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

pytestmark = requires_engine

SRC = str(Path(__file__).resolve().parents[1] / "src")


async def _call_validate(main_file: str):
    """Starts the server, calls wresl_validate and returns (tool names, parsed result, is_error)."""
    server = StdioServerParameters(
        command=sys.executable,
        args=["-m", "wrims_mcp"],
        env={
            **os.environ,
            "PYTHONPATH": SRC,
            "WRIMS_JAVA": CONFIG.java,
            "WRIMS_CLASSPATH": CONFIG.classpath,
        },
    )
    async with stdio_client(server) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            tools = [t.name for t in (await session.list_tools()).tools]
            response = await session.call_tool("wresl_validate", {"main_file": main_file})
            return tools, json.loads(response.content[0].text), response.is_error


def test_agent_sees_structured_error_for_a_bad_goal(tmp_path):
    tools, result, is_error = anyio.run(_call_validate, write_study(tmp_path, GOAL_WITH_LESS_OR_EQUAL))

    assert tools == ["wresl_validate"]
    assert is_error is False  # study errors are results, not tool failures
    assert result["ok"] is False and result["errors"] == 1
    error = result["diagnostics"][0]
    assert error["file"].endswith("main.wresl") and error["line"] == 4
    assert "use '<'" in error["message"]


def test_agent_sees_clean_result_after_fixing_the_goal(tmp_path):
    fixed = GOAL_WITH_LESS_OR_EQUAL.replace("<=", "<")
    _, result, is_error = anyio.run(_call_validate, write_study(tmp_path, fixed))

    assert is_error is False
    assert result == {"ok": True, "errors": 0, "warnings": 0, "diagnostics": []}


def test_missing_file_is_a_result_not_a_crash(tmp_path):
    _, result, is_error = anyio.run(_call_validate, str(tmp_path / "nope.wresl"))

    assert is_error is False
    assert result["ok"] is False
    assert "File not found" in result["diagnostics"][0]["message"]
