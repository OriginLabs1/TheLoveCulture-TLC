# TLC — one command per obligation (SPECS §B-3, §C-1).
# Everything here is reproducible on a clean machine with JDK 21 only; Android targets need the SDK.

SHELL := /bin/bash
GRADLE ?= ./gradlew
ANDROID_HOME ?= $(HOME)/Android/Sdk
VERSION := $(shell sed -nE 's/^versionName=//p' version.properties)

.DEFAULT_GOAL := help

.PHONY: help doctor verify test ledger-verify demo check-foss android-build android-test lint security release-assets clean

help: ## Show targets
	@grep -hE '^[a-z-]+:.*?## ' $(MAKEFILE_LIST) | awk 'BEGIN{FS=":.*?## "}{printf "  %-16s %s\n", $$1, $$2}'

doctor: ## Verify toolchain and secrets (never prints secret values)
	@echo "— TLC doctor —"
	@test -n "$(JAVA_HOME)" && echo "✓ JAVA_HOME set" || echo "! JAVA_HOME not set (JDK 21 required)"
	@java -version 2>&1 | head -1 || echo "✗ java not found"
	@echo "✓ pwd: $$(pwd)"
	@test -f CONTEXT.md -a -f SPECS.md && echo "✓ doctrine present" || echo "✗ CONTEXT.md/SPECS.md missing"
	@test -f LICENSE && echo "✓ licence present (GPL-3.0-or-later)" || echo "✗ LICENSE missing"
	@if [ -z "$$CLOUDFLARE_API_TOKEN" ]; then echo "! CLOUDFLARE_API_TOKEN not set (deploys disabled)"; else echo "✓ CLOUDFLARE_API_TOKEN set"; fi
	@if [ -z "$$ANDROID_HOME" ]; then echo "! ANDROID_HOME not set (android targets disabled)"; else echo "✓ ANDROID_HOME=$$ANDROID_HOME"; fi
	@for s in ANDROID_KEYSTORE_BASE64 ANDROID_KEYSTORE_PASSWORD ANDROID_KEY_ALIAS ANDROID_KEY_PASSWORD; do \
		if [ -z "$${!s}" ]; then echo "! $$s not set (release signing disabled)"; else echo "✓ $$s set"; fi; done

verify: ## Compile + unit tests for the core (no Android SDK needed) — the merge gate
	$(GRADLE) --console=plain verify

test: verify ## Alias for the core gate

ledger-verify: ## Re-verify the hash chain, and prove tampering/truncation is caught
	$(GRADLE) --console=plain :tools:ledger-verify:installDist -q
	./tools/ledger-verify/build/install/ledger-verify/bin/ledger-verify --demo

check-foss: ## Prove the foss channel has no Play Services / Firebase / tracking
	ANDROID_HOME=$(ANDROID_HOME) ./scripts/check-foss.sh

android-build: ## Assemble the foss debug APK (requires ANDROID_HOME + x86_64 AAPT2)
	ANDROID_HOME=$(ANDROID_HOME) $(GRADLE) --console=plain :apps:android:assembleFossDebug

shell-check: ## Type-check the Android shell without AAPT2 (works on arm64 hosts)
	ANDROID_HOME=$(ANDROID_HOME) ./scripts/check-shell.sh

android-test: ## Run foss unit tests (requires ANDROID_HOME)
	ANDROID_HOME=$(ANDROID_HOME) $(GRADLE) --console=plain :apps:android:testFossDebugUnitTest

lint: ## Static analysis across core and the foss shell
	$(GRADLE) --console=plain :core:domain:build :core:ledger:build
	ANDROID_HOME=$(ANDROID_HOME) $(GRADLE) --console=plain :apps:android:lintFossDebug

security: ## Secret scan (fails closed if gitleaks is present)
	@if command -v gitleaks >/dev/null 2>&1; then gitleaks detect --no-banner --redact; else echo "! gitleaks not installed — CI runs it"; fi

release-assets: ## Show what the release pipeline will publish for the current version
	@echo "TLC $(VERSION)"
	@echo "  versionCode: $$(sed -nE 's/^versionCode=//p' version.properties)"
	@echo "  tag:         v$(VERSION)"
	@echo "  asset:       tlc-$(VERSION)-foss.apk (+ .dependencies.txt, .signature.txt, SHA256SUMS)"
	@echo "  channels:    GitHub Releases · F-Droid · Orion Store · Uptodown (same signed APK)"

clean: ## Remove build outputs
	$(GRADLE) --console=plain clean
