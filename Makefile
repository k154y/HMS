PROJECT_NAME := hotel-management-system
COMPOSE_FILE := docker-compose.dev.yml
API_DIR := apps/api


.PHONY: help
.PHONY: dev-up dev-down dev-restart dev-status dev-logs
.PHONY: backend-run backend-build backend-test backend-check backend-clean


help:
	@echo ""
	@echo "$(PROJECT_NAME)"
	@echo ""
	@echo "Development infrastructure:"
	@echo "  make dev-up        Start PostgreSQL, Redis, and RabbitMQ"
	@echo "  make dev-down      Stop development infrastructure"
	@echo "  make dev-restart   Restart development infrastructure"
	@echo "  make dev-status    Show development containers"
	@echo "  make dev-logs      Follow development container logs"
	@echo ""
	@echo "Backend:"
	@echo "  make backend-run   Run the Spring Boot backend"
	@echo "  make backend-build Build the Spring Boot backend"
	@echo "  make backend-test  Run backend tests"
	@echo "  make backend-check Compile backend source"
	@echo "  make backend-clean Remove Maven build files"
	@echo ""


dev-up:
	docker compose -f $(COMPOSE_FILE) up -d


dev-down:
	docker compose -f $(COMPOSE_FILE) down


dev-restart:
	docker compose -f $(COMPOSE_FILE) down
	docker compose -f $(COMPOSE_FILE) up -d


dev-status:
	docker compose -f $(COMPOSE_FILE) ps


dev-logs:
	docker compose -f $(COMPOSE_FILE) logs -f


backend-run:
	mvn -f $(API_DIR)/pom.xml spring-boot:run


backend-build:
	mvn -f $(API_DIR)/pom.xml clean package


backend-test:
	mvn -f $(API_DIR)/pom.xml test


backend-check:
	mvn -f $(API_DIR)/pom.xml -DskipTests compile


backend-clean:
	mvn -f $(API_DIR)/pom.xml clean