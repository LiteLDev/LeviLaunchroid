#!/usr/bin/env python3
import json
import sys
from pathlib import Path


current_path = Path(sys.argv[1])
previous_path = Path(sys.argv[2])
message_path = Path(sys.argv[3])
output_path = Path(sys.argv[4])

with current_path.open(encoding="utf-8") as file:
    current = json.load(file)
try:
    with previous_path.open(encoding="utf-8") as file:
        previous = json.load(file)
except (FileNotFoundError, json.JSONDecodeError):
    previous = {"items": []}

previous_ids = {item.get("id") for item in previous.get("items", [])}
new_items = [item for item in current.get("items", []) if item.get("id") not in previous_ids]
if len(new_items) > 1:
    raise SystemExit("Publish one new news item per commit so each push notification is unambiguous.")

should_send = bool(new_items and new_items[0].get("notify", True))
with output_path.open("a", encoding="utf-8") as output:
    output.write(f"should_send={'true' if should_send else 'false'}\n")
if should_send:
    message_path.write_text(json.dumps(new_items[0], ensure_ascii=False), encoding="utf-8")
