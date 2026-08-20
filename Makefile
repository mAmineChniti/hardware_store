SHELL := /bin/bash
.DEFAULT_GOAL := help

# Colors (evaluated once at parse time via printf — works on zsh, bash, sh)
BLUE  := $(shell printf '\033[0;34m')
GREEN := $(shell printf '\033[0;32m')
YELLOW := $(shell printf '\033[0;33m')
NC    := $(shell printf '\033[0m')

.PHONY: help
help: ## Show this help message
	@echo "$(BLUE)Hardware Store - Available Commands:$(NC)"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "$(GREEN)%-20s$(NC) %s\n", $$1, $$2}'

.PHONY: lint
lint: ## Run linting (checkstyle + spotless check + spotbugs)
	@echo "$(BLUE)Running linting...$(NC)"
	./gradlew lint

.PHONY: format
format: ## Format code with spotless
	@echo "$(BLUE)Formatting code...$(NC)"
	./gradlew format

.PHONY: checkstyle
checkstyle: ## Run checkstyle only
	@echo "$(BLUE)Running checkstyle...$(NC)"
	./gradlew checkstyleMain checkstyleTest

.PHONY: spotless
spotless: ## Run spotless check only
	@echo "$(BLUE)Running spotless check...$(NC)"
	./gradlew spotlessJavaCheck

.PHONY: build
build: ## Build the project
	@echo "$(BLUE)Building project...$(NC)"
	./gradlew build

.PHONY: clean
clean: ## Clean build artifacts
	@echo "$(BLUE)Cleaning build artifacts...$(NC)"
	./gradlew clean

.PHONY: clean-build
clean-build: ## Clean and build the project
	@echo "$(BLUE)Cleaning and building project...$(NC)"
	./gradlew clean build

.PHONY: run
run: ## Run the application
	@echo "$(BLUE)Starting application...$(NC)"
	./gradlew bootRun

.PHONY: test
test: ## Run tests
	@echo "$(BLUE)Running tests...$(NC)"
	./gradlew test

.PHONY: test-coverage
test-coverage: ## Run tests with coverage report
	@echo "$(BLUE)Running tests with coverage...$(NC)"
	./gradlew test jacocoTestReport

.PHONY: watch
watch: ## Watch for file changes and auto-build (requires entr)
	@echo "$(BLUE)Watching for file changes...$(NC)"
	@command -v entr >/dev/null 2>&1 || { echo "$(YELLOW)entr is not installed. Install it with: brew install entr (macOS) or apt install entr (Linux)$(NC)"; exit 1; }
	find src -type f \( -name '*.java' -o -name '*.kt' -o -name '*.yml' -o -name '*.yaml' -o -name '*.properties' \) | entr -r ./gradlew build

.PHONY: deps
deps: ## Show dependency tree
	@echo "$(BLUE)Showing dependency tree...$(NC)"
	./gradlew dependencies

.PHONY: deps-updates
deps-updates: ## Check for dependency updates
	@echo "$(BLUE)Checking for dependency updates...$(NC)"
	./gradlew dependencyUpdates

.PHONY: boot-jar
boot-jar: ## Build executable JAR
	@echo "$(BLUE)Building executable JAR...$(NC)"
	./gradlew bootJar

.PHONY: docker-build
docker-build: ## Build Docker image
	@echo "$(BLUE)Building Docker image...$(NC)"
	docker build -t hardware-store:latest .

.PHONY: docker-run
docker-run: ## Run Docker container
	@echo "$(BLUE)Running Docker container...$(NC)"
	docker run -p 8080:8080 hardware-store:latest

.PHONY: docker-up
docker-up: ## Start services with docker compose (build and detached)
	@echo "$(BLUE)Starting services with docker compose...$(NC)"
	docker compose up -d --build
	@echo "$(GREEN)API docs available at: http://localhost:$${APP_PORT:-8080}/swagger-ui/index.html$(NC)"

.PHONY: docker-down
docker-down: ## Stop services with docker compose
	@echo "$(BLUE)Stopping services with docker compose...$(NC)"
	docker compose down

.PHONY: docker-logs
docker-logs: ## View docker compose logs
	@echo "$(BLUE)Viewing docker compose logs...$(NC)"
	docker compose logs -f

.PHONY: docker-ps
docker-ps: ## Show running containers
	@echo "$(BLUE)Showing running containers...$(NC)"
	docker compose ps

.PHONY: docker-restart
docker-restart: ## Restart services with docker compose
	@echo "$(BLUE)Restarting services with docker compose...$(NC)"
	docker compose restart

.PHONY: all
all: clean format lint build ## Run clean, format, lint, and build
	@echo "$(GREEN)All tasks completed successfully!$(NC)"
