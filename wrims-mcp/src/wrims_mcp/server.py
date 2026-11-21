"""MCP server: lets an AI agent compile-check WRESL+ code and read the diagnostics."""

from __future__ import annotations

import anyio
from mcp.server.mcpserver import MCPServer
from mcp.types import ToolAnnotations

from wrims_mcp import runner

INSTRUCTIONS = (
    "Tools for checking WRESL+ (the CalSim / WRIMS operating-rule language). "
    "A clean check does not prove the code runs: some problems only appear when the study solves."
)

mcp = MCPServer("wrims-wresl", instructions=INSTRUCTIONS)


@mcp.tool(
    annotations=ToolAnnotations(
        title="Check WRESL+ study",
        idempotentHint=True,
        destructiveHint=False,
        openWorldHint=False,
    )
)
async def wresl_validate(main_file: str) -> dict:
    """Compile-check a WRESL+ study with the WRIMS engine's pre-run check. Does not run the study.

    Pass the path to the study's main .wresl file; every file it includes is checked. Returns
    ok (true when there are no errors), the error and warning counts, and a list of diagnostics,
    each with severity, file, line and message. Fix every error and re-run until ok is true, and
    treat warnings as likely run-time failures. For example, reading an earlier value of a state
    variable, such as X(-1) on a 'define X {value ...}', is warned about because WRIMS stops the
    run on it; read an alias of the value instead.

    Writes a log file named =WreslCheck_<name>=.log next to the main file.
    """
    return await anyio.to_thread.run_sync(runner.validate, main_file)


def main() -> None:
    mcp.run()


if __name__ == "__main__":
    main()
