#!/usr/bin/env python3
"""Copy lint: fail the build on banned user-facing terms.

The contract is `SPECS.md` §0.1 and SK-19. This script scans rendered values, not resource
names, and allows only the reviewed explicit negations ("no balance", "not a wallet", ...).
"""

from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

# A user-facing surface is anything a person can read in the app, the store listing, the install
# page, the release notes or the notices. Developer docs (CONTEXT.md, SPECS.md, docs/PLAN-*.md)
# are excluded: they must be free to *name* the banned terms in order to ban them.
TEXT_SURFACES = [
    ROOT / "README.md",
    ROOT / "PRIVACY.md",
    ROOT / "SECURITY.md",
    ROOT / "docs" / "index.html",
    *sorted((ROOT / "release-notes").glob("*.md")),
    *sorted((ROOT / "fastlane" / "metadata" / "android").rglob("*.txt")),
]
XML_SURFACES = sorted((ROOT / "apps" / "android" / "src" / "main" / "res").glob("values*/strings.xml"))


def _negated(line: str, term: str) -> bool:
    """True when the line contains a reviewed explicit negation of `term`."""
    low = line.lower()
    if term == "balance":
        return any(
            phrase in low
            for phrase in (
                "no balance",
                "not a balance",
                "never a balance",
                "not your balance",
                "no pooled balance",
                "counters, not a balance",
                "no balance was created",
                "no balance, no",
            )
        ) or "BALANCE" in line  # an internal identifier, never rendered
    if term == "wallet":
        return any(
            phrase in low
            for phrase in (
                "no wallet",
                "not a wallet",
                "never a wallet",
                "not a bank, not a wallet",
                "wallet provider",  # a named Transfer Method in CONTEXT §2.17
            )
        )
    if term == "password":
        return any(
            phrase in low
            for phrase in (
                "password-authenticated key exchange",
                "bank password",
                "password manager",
            )
        )
    if term == "confirmed by bank":
        return any(
            phrase in low
            for phrase in (
                "not confirmed by bank",
                "never confirmed by bank",
                "never rendered as",
                "without an adapter",
            )
        )
    if term == "login":
        # "login" is banned as the name of the identity; technical prose may use "log in" as a
        # verb. The banned form is the noun/attribute, so allow the verb spelling only.
        return "log in" in low
    return False


RULES = [
    (re.compile(r"\bdues\b", re.IGNORECASE), "dues"),
    (re.compile(r"\bpayment\s+method\b", re.IGNORECASE), "payment method"),
    (re.compile(r"\btop\s*up\b", re.IGNORECASE), "top up"),
    (re.compile(r"\bwithdraw\s+funds?\b", re.IGNORECASE), "withdraw funds"),
    (re.compile(r"\bdeposit\s+funds?\b", re.IGNORECASE), "deposit funds"),
    (re.compile(r"\bbalance\b", re.IGNORECASE), "balance"),
    (re.compile(r"\bwallet\b", re.IGNORECASE), "wallet"),
    (re.compile(r"\bpassword\b", re.IGNORECASE), "password"),
    (re.compile(r"\blogin\b", re.IGNORECASE), "login"),
    (re.compile(r"confirmed\s+by\s+bank", re.IGNORECASE), "confirmed by bank"),
]

# Biometric sign-in is forbidden for any reason (product rule, SPECS SK-13). The copy may name it
# only to forbid it; the code may never call it. This gate fails the build on the APIs themselves.
BIOMETRIC_SURFACES = [
    *sorted((ROOT / "apps" / "android" / "src" / "main").rglob("*.kt")),
    ROOT / "apps" / "android" / "src" / "main" / "AndroidManifest.xml",
]
BIOMETRIC_RULES = [
    (re.compile(r"BiometricPrompt|BiometricManager|androidx\.biometric"), "Android biometric API"),
    (re.compile(r"CredentialManager|createCredential|GetCredentialRequest"), "passkey/credential API"),
    (re.compile(r"setUserAuthenticationRequired\s*\(\s*true"), "biometric-gated key"),
    (re.compile(r"USE_BIOMETRIC|USE_FINGERPRINT"), "biometric permission"),
    (re.compile(r"KeyguardManager|isDeviceSecure|createConfirmDeviceCredentialIntent"), "device-credential prompt"),
]


def check_biometric_sources(path: Path) -> list[str]:
    findings: list[str] = []
    try:
        lines = path.read_text(encoding="utf-8").splitlines()
    except OSError as exc:
        return [f"{path.relative_to(ROOT)}: cannot read ({exc})"]
    for number, line in enumerate(lines, start=1):
        for pattern, term in BIOMETRIC_RULES:
            if pattern.search(line):
                findings.append(
                    f"{path.relative_to(ROOT)}:{number}: forbidden {term} -> {line.strip()}"
                )
    return findings


def check_text(path: Path) -> list[str]:
    findings: list[str] = []
    try:
        lines = path.read_text(encoding="utf-8").splitlines()
    except OSError as exc:
        return [f"{path.relative_to(ROOT)}: cannot read ({exc})"]
    for number, line in enumerate(lines, start=1):
        for pattern, term in RULES:
            if pattern.search(line) and not _negated(line, term):
                findings.append(f"{path.relative_to(ROOT)}:{number}: banned term '{term}' -> {line.strip()}")
    return findings


def check_strings(path: Path) -> list[str]:
    findings: list[str] = []
    try:
        tree = ET.parse(path)
    except (ET.ParseError, OSError) as exc:
        return [f"{path.relative_to(ROOT)}: cannot parse ({exc})"]
    for element in tree.iter():
        if element.tag not in {"string", "item"}:
            continue
        value = (element.text or "").strip()
        if not value:
            continue
        for pattern, term in RULES:
            if pattern.search(value) and not _negated(value, term):
                name = element.get("name", element.tag)
                findings.append(
                    f"{path.relative_to(ROOT)}: <{element.tag} name=\"{name}\"> "
                    f"banned term '{term}' -> {value}"
                )
    return findings


def main() -> int:
    surfaces = [p for p in TEXT_SURFACES if p.exists()]
    xml_surfaces = [p for p in XML_SURFACES if p.exists()]
    source_surfaces = [p for p in BIOMETRIC_SURFACES if p.exists()]
    findings: list[str] = []
    for path in surfaces:
        findings.extend(check_text(path))
    for path in xml_surfaces:
        findings.extend(check_strings(path))
    for path in source_surfaces:
        findings.extend(check_biometric_sources(path))

    scanned = len(surfaces) + len(xml_surfaces) + len(source_surfaces)
    if findings:
        print(f"copy-lint: {len(findings)} finding(s) on {scanned} surface(s)")
        for finding in findings:
            print(f"  ✗ {finding}")
        print("Fix the copy (SPECS.md §0.1 / SK-19). 'dues' must never be user-facing.")
        print("Biometric/passkey sign-in is forbidden (SK-13); the passphrase is the only door.")
        return 1

    print(
        f"copy-lint: clean — {len(surfaces) + len(xml_surfaces)} copy surface(s), "
        f"{len(source_surfaces)} source surface(s), no banned terms or biometric calls"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
