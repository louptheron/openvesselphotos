SHELL := /bin/bash
.SHELLFLAGS = -ec
.SILENT:
MAKEFLAGS += --silent
.ONESHELL:

.DEFAULT_GOAL: help

.PHONY: help ##OTHER 🛟 To display this prompts. This will list all available targets with their documentation
help:
	echo "❓ Use \`make <target>' where <target> is one of 👇"
	echo ""
	echo -e "\033[1mLocal Development\033[0m:"
	grep -E '^\.PHONY: [a-zA-Z0-9_-]+ .*?##LOCAL' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = "(: |##LOCAL)"}; {printf "\033[36m%-35s\033[0m %s\n", $$2, $$3}'
	echo ""
	echo -e "\033[1mTesting\033[0m:"
	grep -E '^\.PHONY: [a-zA-Z0-9_-]+ .*?##TEST' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = "(: |##TEST)"}; {printf "\033[36m%-35s\033[0m %s\n", $$2, $$3}'
	echo ""
	echo -e "\033[1mOther commands\033[0m:"
	grep -E '^\.PHONY: [a-zA-Z0-9_-]+ .*?##OTHER' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = "(: |##OTHER)"}; {printf "\033[36m%-35s\033[0m %s\n", $$2, $$3}'

################################################################################
# Local Development

.PHONY: install-front ##LOCAL ⬇️  Install frontend dependencies
install-front:
	cd ./frontend && npm i

# Variables come from backend/.env.local.defaults, overridden by an optional .env at the root
.PHONY: run-back ##LOCAL ▶️  Run backend API (and its database)
run-back:
	docker compose up -d --quiet-pull --wait db
	@bash -c 'set -a; source ./backend/.env.local.defaults; if [ -f .env ]; then source .env; fi; ./gradlew :backend:bootRun'

.PHONY: run-front ##LOCAL ▶️  Run frontend for development, on http://localhost:3000
run-front:
	cd ./frontend && npm run dev

.PHONY: lint-back ##LOCAL 🪮 ✨  Lint and format backend code
lint-back:
	./gradlew :backend:ktlintFormat

.PHONY: lint-front ##LOCAL 🪮 ✨  Lint and format frontend code
lint-front:
	cd ./frontend && npm run test:lint:fix

.PHONY: clean ##LOCAL Clean all backend assets and stop docker containers
clean:
	rm -Rf ./backend/build ./frontend/build
	docker compose --profile app down -v

.PHONY: build-app-docker ##LOCAL Build app docker image
build-app-docker:
	docker build -f infra/docker/app/Dockerfile -t openvesselphotos .

.PHONY: run-app-docker ##LOCAL ▶️  Run the app docker image and its database, on http://localhost:8880
run-app-docker:
	docker compose --profile app up --build

################################################################################
# Testing

.PHONY: test ##TEST ✅ Run all tests
test: test-back test-front

.PHONY: test-back ##TEST ✅ Run backend tests (needs Docker) — use class=MyClassITests for a single Kotlin test class
test-back:
	if [ -z "$(class)" ]; then \
		./gradlew :backend:ktlintCheck :backend:test; \
	else \
		./gradlew :backend:test --console plain --tests "$(class)"; \
	fi

.PHONY: test-front ##TEST ✅ Run frontend checks and tests
test-front:
	cd ./frontend && npm run test:lint && npm run test:type && npm run test:format && npm run test:unit
