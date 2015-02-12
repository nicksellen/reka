name=reka
image=nicksellen/${name}

docker_modules = command net jade mustache smtp less jsx common postgres h2

runlocal: copy

	@rm -rf data

	@REKA_ENV=development java \
		-jar reka.jar \
		config/main.reka

runlocaldebug: copy

	@rm -rf data

	@REKA_ENV=development java \
		-Dlog4j.configurationFile=../main/reka-main/log4j2-debug.xml \
		-jar reka.jar \
		config/main.reka

runlocalnolog: copy

	@rm -rf data

	@REKA_ENV=development java \
		-Dlog4j.configurationFile=../main/reka-main/log4j2-errors-only.xml \
		-jar reka.jar \
		config/main.reka

copy: 

	@echo "Removing existing modules dir"
	@sudo rm -rf config/modules

	@mkdir config/modules

	@for module in $(docker_modules); do\
		cp ../dist/reka/lib/modules/reka-$$module-* config/modules ; \
	done

	@rm -f config/modules.reka

	@for module in `ls config/modules`; do\
		echo "module modules/$$module" >> config/modules.reka; \
	done

	@cp ../build/reka-main*.jar reka.jar

stop: 
	@-docker kill ${name} >/dev/null
	@-docker rm ${name} >/dev/null

build: 
	@docker build -t ${image} .

run: 
	@docker run -d \
		--name reka \
		-e "CONTAINER=docker" \
		-p 80:80 \
		-p 443:443 \
		-p 15099:15099 \
		-p 15100:15100 \
		${image}

dockerenv: copy build stop run
	@docker run \
		-t \
		-e "CONTAINER=docker" \
		${image}
		env

relaunch: copy build stop run

