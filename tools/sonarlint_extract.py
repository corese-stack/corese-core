#!/usr/bin/env python3
"""
Extract SonarLint diagnostics for Java files by driving the SonarLint
Language Server (the same `sonarlint-ls.jar` bundled with the VS Code
SonarLint extension) directly over stdio, using the Language Server
Protocol (LSP) - no VS Code UI required.

How it works
------------
The VS Code SonarLint extension does not do the Java analysis itself:
it starts `sonarlint-ls.jar` (a standalone LSP server, class
`org.sonarsource.sonarlint.ls.ServerMain`) as a child process and talks
to it over stdio using standard LSP JSON-RPC messages. The extension's
Java support is provided by the `sonarjava.jar` analyzer plugin that
ships in the extension's `analyzers/` folder and is passed to the
server on the command line.

We can reproduce exactly that handshake ourselves:

  1. Launch:
       java -jar sonarlint-ls.jar -stdio -analyzers <sonarjava.jar> ...
     (`-stdio` tells it to use stdio transport instead of a TCP port;
     `-analyzers` is a space separated list of analyzer plugin jars,
     confirmed by inspecting the jar's picocli `@Option` annotations in
     `org.sonarsource.sonarlint.ls.ServerMain`.)

  2. Send the LSP `initialize` request. SonarLint expects some
     SonarLint-specific keys inside `initializationOptions`
     (`productKey`, `productName`, `productVersion`, `workspaceName`,
     `disableTelemetry`, ... - discovered the same way, by inspecting
     the strings/constant pool of the server's compiled classes).

  3. Send `initialized` notification.

  4. For each Java file, send `textDocument/didOpen` with the file
     content. SonarLint analyzes the file asynchronously and reports
     results back to us as `textDocument/publishDiagnostics`
     notifications (standard LSP) - we just listen for those.

  5. Collect all `publishDiagnostics` notifications, wait for the
     server to go quiet (or hit a timeout), then shut down cleanly
     with `shutdown` + `exit`.

This script implements a minimal, dependency-free LSP client
(Content-Length framed JSON-RPC over the subprocess's stdin/stdout)
sufficient to do exactly this.

Usage
-----
    python3 sonarlint_extract.py \
        --src-root src/main/java/fr/inria/corese/core/next \
        --output sonarlint_report.json

Requirements
------------
    - Java (the JRE bundled inside the VS Code extension is reused by
      default; override with --java to point elsewhere).
    - sonarlint-ls.jar and analyzers/sonarjava.jar from the SonarLint
      VS Code extension (paths below have working defaults, override
      with --sonarlint-ls / --analyzer-jar if your extension version
      or install location differs).
"""

from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import threading
import time
from pathlib import Path
from typing import Any, Dict, List, Optional

EXT_DIR = Path.home() / ".vscode/extensions/sonarsource.sonarlint-vscode-5.9.1-linux-x64"
DEFAULT_SONARLINT_LS = EXT_DIR / "server/sonarlint-ls.jar"
DEFAULT_ANALYZER_JARS = [
    EXT_DIR / "analyzers/sonarjava.jar",
    EXT_DIR / "analyzers/sonarjavasymbolicexecution.jar",
]
DEFAULT_JAVA = EXT_DIR / "jre/21.0.12.1-linux-x86_64.tar/bin/java"

# How long (seconds) to wait for diagnostics after opening the LAST file
# before we assume analysis has settled and it's safe to shut down.
DEFAULT_QUIET_PERIOD = 8.0
# Hard cap on total analysis wait time, in case something never quiets down.
DEFAULT_MAX_WAIT = 600.0


class LspClient:
    """A minimal LSP client that speaks Content-Length-framed JSON-RPC
    over a subprocess's stdin/stdout, similar to what VS Code itself
    does when talking to sonarlint-ls."""

    def __init__(self, proc: subprocess.Popen, java_config: Optional[dict] = None):
        self.proc = proc
        self.java_config = java_config
        self._id = 0
        self._lock = threading.Lock()
        self._pending: Dict[int, threading.Event] = {}
        self._results: Dict[int, Any] = {}
        self.diagnostics: Dict[str, List[dict]] = {}
        self._diag_lock = threading.Lock()
        self._last_diagnostic_time = time.time()
        self._activity_lock = threading.Lock()
        self.last_activity_time = time.time()
        self._stop_reader = False
        self._reader_thread = threading.Thread(target=self._read_loop, daemon=True)
        self._reader_thread.start()
        self._stderr_thread = threading.Thread(target=self._drain_stderr, daemon=True)
        self._stderr_thread.start()

    # ---- low level framing -------------------------------------------------
    def _write(self, payload: dict) -> None:
        body = json.dumps(payload).encode("utf-8")
        header = f"Content-Length: {len(body)}\r\n\r\n".encode("ascii")
        assert self.proc.stdin is not None
        self.proc.stdin.write(header + body)
        self.proc.stdin.flush()

    def _drain_stderr(self) -> None:
        assert self.proc.stderr is not None
        debug = os.environ.get("SONARLINT_DEBUG")
        for line in self.proc.stderr:
            if debug:
                sys.stderr.write("[sonarlint-ls] " + line.decode(errors="replace"))

    def _read_loop(self) -> None:
        stdout = self.proc.stdout
        assert stdout is not None
        while not self._stop_reader:
            line = stdout.readline()
            if not line:
                break
            if line.strip() == b"":
                continue
            if line.lower().startswith(b"content-length:"):
                length = int(line.split(b":", 1)[1].strip())
                # consume remaining headers until blank line
                while True:
                    hline = stdout.readline()
                    if hline in (b"\r\n", b"\n", b""):
                        break
                body = stdout.read(length)
                try:
                    msg = json.loads(body.decode("utf-8"))
                except json.JSONDecodeError:
                    continue
                self._handle_message(msg)

    def _handle_message(self, msg: dict) -> None:
        # Any message received from the server (log, diagnostics, progress,
        # requests, responses, ...) counts as activity, so the quiet-period
        # detection in the main loop can rely on a single, reliable signal
        # instead of guessing from specific message contents.
        with self._activity_lock:
            self.last_activity_time = time.time()

        if "id" in msg and ("result" in msg or "error" in msg):
            # response to one of our requests
            msg_id = msg["id"]
            with self._lock:
                ev = self._pending.get(msg_id)
                self._results[msg_id] = msg
            if ev:
                ev.set()
            return

        method = msg.get("method")
        if method == "textDocument/publishDiagnostics":
            params = msg.get("params", {})
            uri = params.get("uri", "")
            diags = params.get("diagnostics", [])
            with self._diag_lock:
                self.diagnostics[uri] = diags
                self._last_diagnostic_time = time.time()
            sys.stderr.write(f"[diagnostics] {uri}: {len(diags)} issues\n")
        elif method and msg.get("id") is not None:
            req_id = msg["id"]
            if method == "workspace/configuration":
                items = msg.get("params", {}).get("items", [])
                self._write({"jsonrpc": "2.0", "id": req_id, "result": [{}] * len(items)})
            elif method == "sonarlint/isOpenInEditor":
                self._write({"jsonrpc": "2.0", "id": req_id, "result": True})
            elif method == "sonarlint/listFilesInFolder":
                self._write({"jsonrpc": "2.0", "id": req_id, "result": {"foundFiles": []}})
            elif method == "sonarlint/filterOutExcludedFiles":
                params = msg.get("params", {})
                uris = params.get("fileUris", []) if isinstance(params, dict) else params
                self._write({"jsonrpc": "2.0", "id": req_id, "result": {"fileUris": uris}})
            elif method == "sonarlint/hasJoinedIdeLabs":
                self._write({"jsonrpc": "2.0", "id": req_id, "result": False})
            elif method == "sonarlint/getJavaConfig":
                self._write({"jsonrpc": "2.0", "id": req_id, "result": self.java_config})
            else:
                self._write({"jsonrpc": "2.0", "id": req_id, "result": None})
        elif method == "window/logMessage":
            msg_text = msg.get("params", {}).get("message", "")
            sys.stderr.write(f"[log] {msg_text}\n")
            # Note: we no longer treat a single "Analysis detected" (or similar)
            # log line as a reliable "analysis complete" signal - SonarLint can
            # emit several of these across batches. Completion is instead
            # decided by the quiet-period check on `last_activity_time` in the
            # main loop, which is updated above for every message received.
        # Otherwise: ignore other notifications (progress, telemetry, etc.) -
        # `last_activity_time` was already refreshed above regardless of type.

    # ---- JSON-RPC helpers ---------------------------------------------------
    def request(self, method: str, params: Optional[dict] = None, timeout: float = 30.0) -> dict:
        with self._lock:
            self._id += 1
            msg_id = self._id
            ev = threading.Event()
            self._pending[msg_id] = ev
        self._write({"jsonrpc": "2.0", "id": msg_id, "method": method, "params": params or {}})
        if not ev.wait(timeout):
            raise TimeoutError(f"Timed out waiting for response to {method!r}")
        with self._lock:
            result = self._results.pop(msg_id)
            self._pending.pop(msg_id, None)
        if "error" in result:
            raise RuntimeError(f"{method} failed: {result['error']}")
        return result.get("result")

    def notify(self, method: str, params: Optional[dict] = None) -> None:
        self._write({"jsonrpc": "2.0", "method": method, "params": params or {}})

    def seconds_since_last_diagnostic(self) -> float:
        with self._diag_lock:
            return time.time() - self._last_diagnostic_time

    def close(self) -> None:
        self._stop_reader = True


def start_server(java: Path, sonarlint_ls: Path, analyzer_jars: List[Path], work_dir: Path) -> subprocess.Popen:
    cmd = [str(java), "-jar", str(sonarlint_ls), "-stdio", "-analyzers"] + [str(j) for j in analyzer_jars]
    return subprocess.Popen(
        cmd,
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        cwd=str(work_dir),
    )


def to_file_uri(path: Path) -> str:
    return path.resolve().as_uri()


def load_java_config(project_root: Path) -> Optional[Dict[str, Any]]:
    """Build the `sonarlint/getJavaConfig` response from the compile
    classpath resolved by the `sonarResolver` Gradle task.

    Gradle writes a JSON document (despite the plain `properties`
    filename) to `build/sonar-resolver/properties` containing a
    `compileClasspath` list. If that file doesn't exist yet, run
    `./gradlew sonarResolver` to generate it. Returns None (and lets
    the server fall back to its own project detection) if the
    classpath still can't be determined.
    """
    resolver_file = project_root / "build" / "sonar-resolver" / "properties"
    if not resolver_file.exists():
        print(f"{resolver_file} not found; running './gradlew sonarResolver' to generate it...",
              file=sys.stderr)
        try:
            subprocess.run(
                ["./gradlew", "sonarResolver"],
                cwd=str(project_root),
                check=True,
            )
        except (OSError, subprocess.CalledProcessError) as e:
            print(f"warning: failed to run './gradlew sonarResolver': {e}", file=sys.stderr)
            return None

    if not resolver_file.exists():
        print(f"warning: {resolver_file} still not found after running sonarResolver",
              file=sys.stderr)
        return None

    try:
        data = json.loads(resolver_file.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as e:
        print(f"warning: could not parse {resolver_file}: {e}", file=sys.stderr)
        return None

    compile_classpath = data.get("compileClasspath") or []
    classes_dir = project_root / "build" / "classes" / "java" / "main"
    classpath = [str(classes_dir)] + list(compile_classpath)

    return {
        "projectRoot": str(project_root),
        "sourceLevel": "21",
        "classpath": classpath,
        "isTest": False,
        "vmLocation": "",
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Extract SonarLint diagnostics for Java files via the SonarLint Language Server (LSP over stdio).")
    parser.add_argument("--src-root", default="src/main/java/fr/inria/corese/core/next",
                         help="Directory tree to scan for *.java files (default: %(default)s)")
    parser.add_argument("--project-root", default=".",
                         help="Project/workspace root passed to SonarLint as the workspace folder (default: current directory)")
    parser.add_argument("--files", nargs="*", default=None,
                        help="Specific .java files to scan instead of an entire tree")
    parser.add_argument("--output", default="sonarlint_report.json", help="Where to write the JSON report")
    parser.add_argument("--sonarlint-ls", default=str(DEFAULT_SONARLINT_LS), help="Path to sonarlint-ls.jar")
    parser.add_argument("--analyzer-jar", action="append", default=None,
                         help="Analyzer jar to load (repeatable). Defaults to sonarjava.jar + symbolic execution plugin.")
    parser.add_argument("--java", default=str(DEFAULT_JAVA), help="Path to java executable")
    parser.add_argument("--quiet-period", type=float, default=DEFAULT_QUIET_PERIOD,
                         help="Seconds of no new diagnostics before considering analysis complete (default: %(default)s)")
    parser.add_argument("--max-wait", type=float, default=DEFAULT_MAX_WAIT,
                         help="Absolute cap in seconds on total time spent waiting for diagnostics")
    parser.add_argument("--batch-size", type=int, default=25,
                         help="Number of files to open concurrently per batch (avoids opening 700 files at once)")
    args = parser.parse_args()

    project_root = Path(args.project_root).resolve()
    src_root = Path(args.src_root)
    if not src_root.is_absolute():
        src_root = project_root / src_root
    if not src_root.is_dir():
        print(f"error: src root not found: {src_root}", file=sys.stderr)
        return 1

    java = Path(args.java)
    if not java.exists():
        java = Path("java")  # fall back to whatever is on PATH

    sonarlint_ls = Path(args.sonarlint_ls)
    if not sonarlint_ls.exists():
        print(f"error: sonarlint-ls.jar not found at {sonarlint_ls}", file=sys.stderr)
        return 1

    analyzer_jars = [Path(p) for p in (args.analyzer_jar or DEFAULT_ANALYZER_JARS)]
    for jar in analyzer_jars:
        if not jar.exists():
            print(f"error: analyzer jar not found: {jar}", file=sys.stderr)
            return 1

    if args.files:
        java_files = [Path(f).resolve() for f in args.files]
    else:
        java_files = sorted(src_root.rglob("*.java"))
    if not java_files:
        print(f"error: no .java files found", file=sys.stderr)
        return 1
    print(f"Found {len(java_files)} Java files to analyze")

    java_config = load_java_config(project_root)

    extra_properties: Dict[str, str] = {}
    if java_config and java_config.get("classpath"):
        extra_properties["sonar.java.binaries"] = str(project_root / "build" / "classes" / "java" / "main")

    print(f"Starting sonarlint-ls: {java} -jar {sonarlint_ls} -stdio -analyzers "
          f"{' '.join(str(j) for j in analyzer_jars)}")
    proc = start_server(java, sonarlint_ls, analyzer_jars, project_root)
    client = LspClient(proc, java_config=java_config)

    try:
        init_result = client.request("initialize", {
            "processId": os.getpid(),
            "rootUri": to_file_uri(project_root),
            "capabilities": {
                "textDocument": {
                    "publishDiagnostics": {
                        "relatedInformation": True,
                        "versionSupport": False,
                        "tagSupport": {"valueSet": [1, 2]},
                        "codeDescriptionSupport": True,
                        "dataSupport": True,
                    }
                },
                "workspace": {
                    "configuration": True,
                }
            },
            "workspaceFolders": [
                {"uri": to_file_uri(project_root), "name": project_root.name}
            ],
            "initializationOptions": {
                "productKey": "standalone",
                "productName": "SonarLint CLI Extractor",
                "productVersion": "1.0.0",
                "workspaceName": project_root.name,
                "firstSecretDetected": True,
                "showVerboseLogs": True,
                "additionalAttributes": {},
                "disableTelemetry": True,
                "automaticAnalysis": True,
                "connections": {"sonarqube": [], "sonarcloud": []},
                "rules": {},
                "extraProperties": extra_properties,
            },
        }, timeout=60)
        print("Server initialized:",
              init_result.get("serverInfo") if isinstance(init_result, dict) else init_result)
        client.notify("initialized", {})

        # Give the server a moment to finish standalone-engine bootstrap
        # (it lazily starts the Java analysis engine on first use).
        time.sleep(1.0)

        opened_uris: List[str] = []
        batch_size = max(1, args.batch_size)
        for start in range(0, len(java_files), batch_size):
            batch = java_files[start:start + batch_size]
            for f in batch:
                uri = to_file_uri(f)
                try:
                    text = f.read_text(encoding="utf-8", errors="replace")
                except OSError as e:
                    print(f"warning: could not read {f}: {e}", file=sys.stderr)
                    continue
                client.notify("textDocument/didOpen", {
                    "textDocument": {
                        "uri": uri,
                        "languageId": "java",
                        "version": 1,
                        "text": text,
                    }
                })
                opened_uris.append(uri)
            print(f"Opened {min(start + batch_size, len(java_files))}/{len(java_files)} files...")
            # Small pause between batches so we don't overwhelm the analyzer queue.
            time.sleep(0.2)

        print("All files opened. Waiting for SonarLint analysis to settle "
              f"(quiet period: {args.quiet_period}s, max wait: {args.max_wait}s)...")
        wait_start = time.time()
        last_progress_print = 0.0
        progress_interval = 5.0
        while True:
            now = time.time()
            with client._activity_lock:
                idle_for = now - client.last_activity_time
            elapsed = now - wait_start

            if now - last_progress_print >= progress_interval:
                files_with_diagnostics = len(client.diagnostics)
                print(f"  ...{elapsed:.0f}s elapsed, {files_with_diagnostics}/{len(opened_uris)} "
                      f"files have received diagnostics, idle for {idle_for:.1f}s")
                last_progress_print = now

            files_with_diagnostics = len(client.diagnostics)
            required_quiet = args.quiet_period if files_with_diagnostics >= len(opened_uris) else max(args.quiet_period, 15.0)
            if idle_for >= required_quiet:
                print(f"No server activity for {idle_for:.1f}s (>= required quiet period of "
                      f"{required_quiet:.1f}s). Assuming analysis has settled.")
                break
            if elapsed >= args.max_wait:
                print(f"Reached max wait of {args.max_wait}s. Proceeding with whatever "
                      "diagnostics have been received so far.")
                break
            time.sleep(0.5)

        # Some files may never receive a publishDiagnostics message if
        # SonarLint found zero issues in them (LSP servers are not
        # required to publish an empty diagnostics list). Fill those
        # in explicitly so the report always reflects every scanned file.
        for uri in opened_uris:
            client.diagnostics.setdefault(uri, [])

        report = build_report(client.diagnostics, project_root)
        Path(args.output).write_text(json.dumps(report, indent=2), encoding="utf-8")
        print(f"Wrote report with {report['summary']['total_issues']} issue(s) "
              f"across {report['summary']['files_with_issues']} file(s) to {args.output}")
        print_summary(report)
        return 0
    finally:
        try:
            client.request("shutdown", {}, timeout=10)
            client.notify("exit", {})
        except Exception:
            pass
        client.close()
        try:
            proc.wait(timeout=10)
        except Exception:
            proc.kill()


def build_report(diagnostics: Dict[str, List[dict]], project_root: Path) -> dict:
    files = []
    total_issues = 0
    files_with_issues = 0
    severity_counts: Dict[str, int] = {}
    rule_counts: Dict[str, int] = {}

    for uri, diags in sorted(diagnostics.items()):
        rel_path = uri
        if uri.startswith("file://"):
            abs_path = Path(uri[len("file://"):])
            try:
                rel_path = str(abs_path.relative_to(project_root))
            except ValueError:
                rel_path = str(abs_path)

        issues = []
        for d in diags:
            severity = d.get("severity")
            code = d.get("code")
            rule = code if isinstance(code, str) else (code.get("value") if isinstance(code, dict) else code)
            rng = d.get("range", {})
            issues.append({
                "rule": rule,
                "message": d.get("message"),
                "severity": severity,
                "start_line": rng.get("start", {}).get("line", 0) + 1,
                "start_character": rng.get("start", {}).get("character", 0),
                "end_line": rng.get("end", {}).get("line", 0) + 1,
                "end_character": rng.get("end", {}).get("character", 0),
                "source": d.get("source"),
            })
            total_issues += 1
            if rule:
                rule_counts[str(rule)] = rule_counts.get(str(rule), 0) + 1
            severity_counts[str(severity)] = severity_counts.get(str(severity), 0) + 1

        if issues:
            files_with_issues += 1
        files.append({"file": rel_path, "issue_count": len(issues), "issues": issues})

    return {
        "summary": {
            "total_files_scanned": len(files),
            "files_with_issues": files_with_issues,
            "total_issues": total_issues,
            "by_severity": severity_counts,
            "by_rule": dict(sorted(rule_counts.items(), key=lambda kv: -kv[1])),
        },
        "files": files,
    }


def print_summary(report: dict) -> None:
    top_rules = list(report["summary"]["by_rule"].items())[:15]
    if top_rules:
        print("\nTop rules triggered:")
        for rule, count in top_rules:
            print(f"  {count:4d}  {rule}")
    top_files = sorted(report["files"], key=lambda f: -f["issue_count"])[:15]
    if top_files and top_files[0]["issue_count"] > 0:
        print("\nFiles with the most issues:")
        for f in top_files:
            if f["issue_count"] == 0:
                break
            print(f"  {f['issue_count']:4d}  {f['file']}")


if __name__ == "__main__":
    raise SystemExit(main())
