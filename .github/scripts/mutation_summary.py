"""Write a Markdown summary of PIT / StrykerJS mutation reports (for $GITHUB_STEP_SUMMARY).

Usage: python3 mutation_summary.py pit <mutations.xml>
       python3 mutation_summary.py stryker <mutation.json>
"""

import collections
import json
import sys
import xml.etree.ElementTree as ET

KILLED = {"KILLED", "TIMED_OUT", "Killed", "Timeout"}
SURVIVED = {"SURVIVED", "Survived"}
NO_COVERAGE = {"NO_COVERAGE", "NoCoverage"}


def load_pit(path):
    rows = []
    for m in ET.parse(path).getroot():
        rows.append(
            (
                m.findtext("sourceFile"),
                m.findtext("mutatedClass").rsplit(".", 1)[-1],
                int(m.findtext("lineNumber")),
                m.findtext("description"),
                m.get("status"),
            )
        )
    return rows


def load_stryker(path):
    rows = []
    for name, f in json.load(open(path, encoding="utf-8"))["files"].items():
        for m in f["mutants"]:
            desc = m["mutatorName"] + (f": {m['replacement']}" if m.get("replacement") else "")
            rows.append((name, name, m["location"]["start"]["line"], desc[:80], m["status"]))
    return rows


def main(kind, path):
    rows = load_pit(path) if kind == "pit" else load_stryker(path)
    per_file = collections.defaultdict(collections.Counter)
    for _, key, _, _, status in rows:
        per_file[key][status] += 1

    def score(c):
        killed = sum(c[s] for s in KILLED)
        detectable = killed + sum(c[s] for s in SURVIVED | NO_COVERAGE)
        return 100.0 * killed / detectable if detectable else 100.0

    total = collections.Counter(s for *_, s in rows)
    print(f"### Mutation score ({'Java / PIT' if kind == 'pit' else 'TypeScript / StrykerJS'}): {score(total):.1f}%\n")
    print(", ".join(f"{k}: {v}" for k, v in sorted(total.items())) + "\n")
    print("| File | Score | Killed | Survived | No coverage |")
    print("|---|---:|---:|---:|---:|")
    ranked = sorted(per_file.items(), key=lambda kv: -sum(kv[1][s] for s in SURVIVED | NO_COVERAGE))
    for key, c in ranked:
        alive = sum(c[s] for s in SURVIVED | NO_COVERAGE)
        if alive:
            print(
                f"| `{key}` | {score(c):.0f}% | {sum(c[s] for s in KILLED)} "
                f"| {sum(c[s] for s in SURVIVED)} | {sum(c[s] for s in NO_COVERAGE)} |"
            )
    print("\nFull report: see the uploaded artifact.")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
