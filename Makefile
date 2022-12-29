.EXPORT_ALL_VARIABLES:


PGPORT=5432
PGHOST=localhost
PGUSER=postgres
PGPASSWORD=postgres
PGDATABASE=postgres
APP_ENV=local

.EXPORT_ALL_VARIABLES:

npm:
	npm install

backend: up
	clojure -X user/run


repl:
	clj -A:shadow:dev:test watch app

server:
	clj -A:server

build:
	clj -A:shadow release app

up:
	docker-compose up -d

down:
	docker-compose down

libox:
	mkdir -p jars && cd jars && gsutil cp gs://libox/stable/aidbox.jar libox.jar
