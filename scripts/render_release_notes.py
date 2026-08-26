#!/usr/bin/env python3
import json
import sys
from pathlib import Path


source = Path(sys.argv[1])
destination = Path(sys.argv[2])
with source.open(encoding="utf-8") as file:
    changelog = json.load(file)

lines = [f"## {changelog['headline']}", "", changelog["summary"], ""]
for section in changelog["sections"]:
    lines.extend([f"### {section['title']}", ""])
    for item in section["items"]:
        label = item["text"]
        url = item.get("url", "")
        lines.append(f"- [{label}]({url})" if url else f"- {label}")
    lines.append("")

destination.write_text("\n".join(lines).rstrip() + "\n", encoding="utf-8")
