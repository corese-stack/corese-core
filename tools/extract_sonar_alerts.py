#!/usr/bin/env python3
"""
Extract SonarLint alerts and diagnostics directly from the VS Code SonarLint storage.
Connects to the H2 database maintained by the SonarLint language server in VS Code.
"""

import argparse
import json
import os
import subprocess
import sys
from collections import Counter
from pathlib import Path

SONARLINT_JAR = Path.home() / ".vscode/extensions/sonarsource.sonarlint-vscode-5.9.1-linux-x64/server/sonarlint-ls.jar"
DB_URL = f"jdbc:h2:{Path.home()}/.sonarlint/storage/h2/sq-ide;AUTO_SERVER=TRUE"


def query_findings(prefix="src/main/java/fr/inria/corese/core/next"):
    sql = f"""SELECT IDE_RELATIVE_FILE_PATH, RULE_KEY, START_LINE, MESSAGE, FINDING_TYPE
             FROM KNOWN_FINDINGS 
             WHERE IDE_RELATIVE_FILE_PATH LIKE '{prefix}%' 
             ORDER BY IDE_RELATIVE_FILE_PATH, START_LINE;"""
    
    cmd = [
        "java", "-cp", str(SONARLINT_JAR),
        "org.h2.tools.Shell",
        "-url", DB_URL,
        "-user", "sa", "-password", "",
        "-sql", sql
    ]
    res = subprocess.run(cmd, capture_output=True, text=True, check=True)
    
    findings = []
    for line in res.stdout.splitlines():
        parts = [p.strip() for p in line.split("|")]
        if len(parts) >= 5 and parts[0].startswith(prefix):
            rel_path = parts[0]
            if os.path.exists(rel_path):
                findings.append({
                    "file": rel_path,
                    "rule": parts[1],
                    "line": int(parts[2]) if parts[2].isdigit() else 0,
                    "message": parts[3],
                    "type": parts[4]
                })
    return findings


def main():
    parser = argparse.ArgumentParser(description="Extract active SonarLint alerts from VS Code H2 database.")
    parser.add_argument("--path", default="src/main/java/fr/inria/corese/core/next", help="Path prefix to filter files")
    parser.add_argument("--json", dest="json_out", help="Path to write JSON output")
    parser.add_argument("--summary", action="store_true", help="Print summary by rule and by file")
    args = parser.parse_args()

    findings = query_findings(args.path)

    files_map = {}
    rule_counter = Counter()
    for f in findings:
        files_map.setdefault(f["file"], []).append(f)
        rule_counter[f["rule"]] += 1

    print(f"SonarLint Analysis Report for: {args.path}")
    print(f"Total findings on active files: {len(findings)} across {len(files_map)} files")

    if args.summary or not args.json_out:
        print("\n--- Summary by Rule ---")
        for rule, count in rule_counter.most_common():
            print(f"  {rule:12s}: {count:3d}")

        print("\n--- Files with Most Findings ---")
        sorted_files = sorted(files_map.items(), key=lambda kv: len(kv[1]), reverse=True)
        for filepath, file_findings in sorted_files[:25]:
            print(f"  {len(file_findings):3d} issues: {filepath}")

    if args.json_out:
        out_data = {
            "total_findings": len(findings),
            "files_count": len(files_map),
            "by_rule": dict(rule_counter),
            "findings": findings
        }
        Path(args.json_out).write_text(json.dumps(out_data, indent=2), encoding="utf-8")
        print(f"\nWrote full JSON findings to {args.json_out}")


if __name__ == "__main__":
    main()
