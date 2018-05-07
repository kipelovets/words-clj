.PHONY:

run: stop
	docker-compose up -d app

quick-run:
	docker-compose run --rm app

repl:
	docker-compose run --rm app lein repl

tests:
	docker-compose run --rm app lein test

redis-cli:
	docker-compose run --rm redis-cli

stop:
	docker-compose stop app ring
	docker-compose rm -f app ring

ring:
	docker-compose rm -f ring
	docker-compose up -d ring

build:
	docker-compose run --rm app lein with-profile facebook uberjar

build-image:
	cp target/uberjar/words-0.1.0-SNAPSHOT-standalone.jar docker/ring/
	docker build docker/ring -t kipelovets/words-ring
	docker push kipelovets/words-ring

telegram: run

facebook: ring