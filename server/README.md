# Linux Server

## Docker Magia
To spin the backend and the database run in the root folder
```bash
docker compose up --build
```

For debugging purposes, like self updating code on edit, run:
```bash
docker compose -f docker-compose.yaml -f  docker-compose.override.yaml up --build
```
