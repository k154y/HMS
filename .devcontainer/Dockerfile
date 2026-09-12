FROM mcr.microsoft.com/devcontainers/java:21-bookworm

USER root

# Development utilities used across the HMS backend,
# database, infrastructure, and debugging workflows.
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        make \
        curl \
        jq \
        postgresql-client \
        redis-tools \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

USER vscode

WORKDIR /workspace