# syntax=docker/dockerfile:1.4
FROM python:3.8

RUN --mount=type=cache,target=/var/cache/apt,sharing=locked \
    --mount=type=cache,target=/var/lib/apt,sharing=locked \
    apt-get update && apt-get install -y --no-install-recommends \
    nginx supervisor mariadb-client default-libmysqlclient-dev \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY requirements.txt .
RUN --mount=type=cache,target=/root/.cache PYTHONDONTWRITEBYTECODE=1 pip install -r requirements.txt
COPY src .

EXPOSE 5000
ENTRYPOINT [ "flask", "run", "--host", "0.0.0.0" ]