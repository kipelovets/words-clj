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
	docker-compose stop app
	docker-compose rm -f app

ring:
	docker-compose rm -f ring
	docker-compose up ring