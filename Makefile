.PHONY: install dev-db dev-back dev-front test lint format build docker-build docker-up

DEV_DATABASE_URL ?= postgres://ovp:ovp@localhost:$${DB_PORT:-5432}/ovp
DEV_ADMIN_PASSWORD ?= admin

install:
	cd frontend && npm ci

# Development: run each in its own terminal, then open http://localhost:3000
dev-db:
	docker compose up -d db

dev-back:
	DATABASE_URL=$(DEV_DATABASE_URL) ADMIN_PASSWORD=$(DEV_ADMIN_PASSWORD) ./gradlew :backend:bootRun

dev-front:
	cd frontend && npm run dev

# Checks (the backend tests need Docker, for Testcontainers)
test:
	./gradlew :backend:test
	cd frontend && npm run test:unit

lint:
	./gradlew :backend:ktlintCheck
	cd frontend && npm run test:lint && npm run test:type && npm run test:format

format:
	./gradlew :backend:ktlintFormat
	cd frontend && npm run format

# Production build: the jar serves the API and the built frontend
build:
	cd frontend && npm ci && npm run build
	./gradlew stage

docker-build:
	docker compose build app

docker-up:
	docker compose --profile app up --build
