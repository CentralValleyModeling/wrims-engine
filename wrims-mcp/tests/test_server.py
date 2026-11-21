import json

import anyio

from wrims_mcp import runner, server


def test_exposes_exactly_one_validate_tool_taking_a_main_file():
    async def go():
        return await server.mcp.list_tools()

    tools = anyio.run(go)
    assert [t.name for t in tools] == ["wresl_validate"]
    assert tools[0].input_schema["required"] == ["main_file"]
    assert "Does not run the study" in tools[0].description


def test_tool_returns_the_runner_result_as_json_text(monkeypatch):
    result = {
        "ok": False,
        "errors": 1,
        "warnings": 0,
        "diagnostics": [{"severity": "error", "file": "a.wresl", "line": 3, "message": "boom"}],
    }
    seen = []
    monkeypatch.setattr(runner, "validate", lambda main_file: seen.append(main_file) or result)

    async def go():
        return await server.mcp.call_tool("wresl_validate", {"main_file": "C:/study/main.wresl"})

    response = anyio.run(go)
    assert response.is_error is False
    assert json.loads(response.content[0].text) == result
    assert seen == ["C:/study/main.wresl"]
