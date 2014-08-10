name=reka
image=nicksellen/${name}

runlocal: copy

	@rm -rf data

	@REKA_ENV=development java \
		-jar reka-main.jar \
		config/main.reka

runlocalnolog: copy

	@rm -rf data

	@REKA_ENV=development java \
		-Dlog4j.configurationFile=../main/reka-main/log4j2-errors-only.xml \
		-jar reka-main.jar \
		config/main.reka

copy: 

	@echo "Removing existing bundles dir"
	@sudo rm -rf config/bundles

	@mkdir config/bundles

	@echo "Copying built jars into local dir"
	@cp ../bundles/reka-*/target/reka-*.jar config/bundles/
	@rm config/bundles/reka-chronicle*

	@cp ../main/reka-main/target/reka-main-*.jar reka-main.jar

	@echo "Making bundles.reka"

	@for b in `find config/bundles -type f -name '*.jar'`; do echo bundle bundles/`basename $$b`; done > config/bundles.reka

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

relaunch: copy build stop run

