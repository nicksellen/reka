bundles = command http jade jdbc mustache smtp

dist_dir = dist/reka

all:
	@mvn clean package 

bundles:
	@cd bundles
	@mvn clean package

main:
	@cd main
	@mvn clean package

clojure:
	@mvn -DskipTests -f pom-clojure.xml clean package

clean:
	@rm -rf dist

dist:
	@mkdir -p $(dist_dir)/lib/bundles
	@mkdir -p $(dist_dir)/etc/apps
	@cp main/reka-main/target/reka-main*.jar $(dist_dir)/lib/reka-server.jar
	@for bundle in $(bundles); do\
		cp bundles/reka-$$bundle/target/reka-$$bundle-*.jar $(dist_dir)/lib/bundles/ ; \
	done
	@for bundle in `ls $(dist_dir)/lib/bundles`; do\
		echo "bundle ../lib/bundles/$$bundle" >> $(dist_dir)/etc/config.reka; \
	done
	@cp docker/config/reka-api/main.reka $(dist_dir)/etc/apps/api.reka
	@echo "app @include(apps/api.reka)" >> $(dist_dir)/etc/config.reka
	@cp -r dist-resources/* $(dist_dir)/
	@cd dist && tar zcvf reka.tar.gz reka
	@echo made dist/reka.tar.gz

run: dist
	@dist/reka/bin/reka-server

s3: dist
	@aws s3 \
		cp dist/reka.tar.gz s3://reka/reka.tar.gz	\
		--grants \
			read=uri=http://acs.amazonaws.com/groups/global/AllUsers
