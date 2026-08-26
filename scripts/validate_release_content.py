#!/usr/bin/env python3
import json
import sys
from datetime import datetime
from pathlib import Path
from urllib.parse import urlparse


def fail(message):
    raise ValueError(message)


def load(path):
    try:
        with path.open(encoding="utf-8") as file:
            return json.load(file)
    except Exception as error:
        fail(f"{path}: {error}")


def text(value, field):
    if not isinstance(value, str) or not value.strip():
        fail(f"{field} must be a non-empty string")


def validate_changelog(path):
    data = load(path)
    if data.get("schemaVersion") != 1:
        fail(f"{path}: schemaVersion must be 1")
    text(data.get("headline"), f"{path}: headline")
    text(data.get("summary"), f"{path}: summary")
    sections = data.get("sections")
    if not isinstance(sections, list) or not sections:
        fail(f"{path}: sections must be a non-empty array")
    for index, section in enumerate(sections):
        text(section.get("title"), f"{path}: sections[{index}].title")
        items = section.get("items")
        if not isinstance(items, list) or not items:
            fail(f"{path}: sections[{index}].items must be a non-empty array")
        for item_index, item in enumerate(items):
            prefix = f"{path}: sections[{index}].items[{item_index}]"
            if not isinstance(item, dict):
                fail(f"{prefix} must be an object with a text field")
            text(item.get("text"), f"{prefix}.text")
            url = item.get("url", "")
            if url:
                parsed = urlparse(url)
                if parsed.scheme not in {"http", "https"} or not parsed.netloc:
                    fail(f"{prefix}.url must be an http or https URL")


def parse_date(value, field):
    text(value, field)
    if not value.endswith("Z"):
        fail(f"{field} must use UTC and end with Z")
    try:
        return datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError:
        fail(f"{field} must be an ISO-8601 timestamp such as 2026-08-26T18:00:00Z")


def validate_news(path):
    data = load(path)
    if data.get("schemaVersion") != 1:
        fail(f"{path}: schemaVersion must be 1")
    items = data.get("items")
    if not isinstance(items, list):
        fail(f"{path}: items must be an array")
    seen = set()
    dates = []
    for index, item in enumerate(items):
        prefix = f"{path}: items[{index}]"
        text(item.get("id"), f"{prefix}.id")
        if item["id"] in seen:
            fail(f"{prefix}.id duplicates {item['id']}")
        seen.add(item["id"])
        text(item.get("title"), f"{prefix}.title")
        text(item.get("summary"), f"{prefix}.summary")
        text(item.get("category"), f"{prefix}.category")
        dates.append(parse_date(item.get("publishedAt"), f"{prefix}.publishedAt"))
        url = item.get("url", "")
        if url:
            parsed = urlparse(url)
            if parsed.scheme not in {"http", "https"} or not parsed.netloc:
                fail(f"{prefix}.url must be an http or https URL")
        if "important" in item and not isinstance(item["important"], bool):
            fail(f"{prefix}.important must be true or false")
        if "notify" in item and not isinstance(item["notify"], bool):
            fail(f"{prefix}.notify must be true or false")
    if dates != sorted(dates, reverse=True):
        fail(f"{path}: news items must be ordered newest first")


def main():
    root = Path(sys.argv[1] if len(sys.argv) > 1 else ".").resolve()
    validate_changelog(root / "resources/launcher/changelog.json")
    validate_news(root / "resources/launcher/news.json")
    print("Release content is valid.")


if __name__ == "__main__":
    try:
        main()
    except ValueError as error:
        print(error, file=sys.stderr)
        sys.exit(1)
