.PHONY: help lint format build clean clean-build run test watch checkstyle spotless deps deps-updates boot-jar docker-build docker-run docker-up docker-down docker-logs docker-ps docker-restart all

# Default target
.DEFAULT_GOAL := help

# Colors for output
BLUE := \033[0;34m
GREEN := \033[0;32m
YELLOW := \033[0;33m
NC := \033[0m # No Color

help: ## Show this help message
	@echo "$(BLUE)Hardware Store - Available Commands:$(NC)"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "$(GREEN)%-20s$(NC) %s\n", $$1, $$2}'

lint: ## Run linting (checkstyle + spotless check)
	@echo "$(BLUE)Running linting...$(NC)"
	./gradlew checkstyleMain checkstyleTest spotlessJavaCheck

format: ## Format code with spotless
	@echo "$(BLUE)Formatting code...$(NC)"
	./gradlew spotlessApply

checkstyle: ## Run checkstyle only
	@echo "$(BLUE)Running checkstyle...$(NC)"
	./gradlew checkstyleMain checkstyleTest

spotless: ## Run spotless check only
	@echo "$(BLUE)Running spotless check...$(NC)"
	./gradlew spotlessJavaCheck

build: ## Build the project
	@echo "$(BLUE)Building project...$(NC)"
	./gradlew build

clean: ## Clean build artifacts
	@echo "$(BLUE)Cleaning build artifacts...$(NC)"
	./gradlew clean

clean-build: ## Clean and build the project
	@echo "$(BLUE)Cleaning and building project...$(NC)"
	./gradlew clean build

run: ## Run the application
	@echo "$(BLUE)Starting application...$(NC)"
	./gradlew bootRun

test: ## Run tests
	@echo "$(BLUE)Running tests...$(NC)"
	./gradlew test

test-coverage: ## Run tests with coverage report
	@echo "$(BLUE)Running tests with coverage...$(NC)"
	./gradlew test jacocoTestReport

watch: ## Watch for file changes and auto-build (requires entr)
	@echo "$(BLUE)Watching for file changes...$(NC)"
	@command -v entr >/dev/null 2>&1 || { echo "$(YELLOW)entr is not installed. Install it with: brew install entr (macOS) or apt install entr (Linux)$(NC)"; exit 1; }
	find src -type f -name '*.java' -o -name '*.kt' | entr -r ./gradlew build

deps: ## Show dependency tree
	@echo "$(BLUE)Showing dependency tree...$(NC)"
	./gradlew dependencies

deps-updates: ## Check for dependency updates
	@echo "$(BLUE)Checking for dependency updates...$(NC)"
	./gradlew dependencyUpdates

boot-jar: ## Build executable JAR
	@echo "$(BLUE)Building executable JAR...$(NC)"
	./gradlew bootJar

docker-build: ## Build Docker image
	@echo "$(BLUE)Building Docker image...$(NC)"
	docker build -t hardware-store:latest .

docker-run: ## Run Docker container
	@echo "$(BLUE)Running Docker container...$(NC)"
	docker run -p 8080:8080 hardware-store:latest

docker-up: ## Start services with docker compose (build and detached)
	@echo "$(BLUE)Starting services with docker compose...$(NC)"
	docker compose up -d --build

docker-down: ## Stop services with docker compose
	@echo "$(BLUE)Stopping services with docker compose...$(NC)"
	docker compose down

docker-logs: ## View docker compose logs
	@echo "$(BLUE)Viewing docker compose logs...$(NC)"
	docker compose logs -f

docker-ps: ## Show running containers
	@echo "$(BLUE)Showing running containers...$(NC)"
	docker compose ps

docker-restart: ## Restart services with docker compose
	@echo "$(BLUE)Restarting services with docker compose...$(NC)"
	docker compose restart

all: clean format lint build ## Run clean, format, lint, and build
	@echo "$(GREEN)All tasks completed successfully!$(NC)"
