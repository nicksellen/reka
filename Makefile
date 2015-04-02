-include env.make

dist_dir = dist/reka
packaged = reka-server.tar.gz

dist_modules := $(shell ls | grep reka-module | sed 's/reka-module-//g')

.PHONY: clean test run package upload-s3

all: clean $(packaged)

$(dist_dir): .mvn-build
	@mkdir -p $(dist_dir)
	@cp -r dist-resources/* $(dist_dir)/
	@mkdir -p $(dist_dir)/lib/
	@mkdir -p $(dist_dir)/lib/modules
	@mkdir -p $(dist_dir)/etc/apps
	@cp reka-server/target/reka-server-*.jar $(dist_dir)/lib/reka-server.jar
	@for module in $(dist_modules); do\
		cp reka-module-$$module/target/reka-module-$$module-* $(dist_dir)/lib/modules ; \
	done
	@for module in `ls $(dist_dir)/lib/modules`; do\
		echo "module ../lib/modules/$$module" >> $(dist_dir)/etc/config.reka; \
	done
	@echo "app api @include(apps/api.reka)" >> $(dist_dir)/etc/config.reka

package: $(packaged)

$(packaged): $(dist_dir)
	@cd dist && tar zcvf $(packaged) reka && mv $(packaged) ..
	@echo made $(packaged)

clean:
	@rm -rf $(dist_dir)
	@rm -rf $(packaged)
	@rm -f .mvn-build

.mvn-build:
	@mvn -DskipTests -T 1.5C clean package
	@touch .mvn-build

test:
	@mvn test

run: $(dist_dir)
	@$(dist_dir)/bin/reka-server

upload-s3: $(packaged)
	@aws s3 \
		cp $(packaged) s3://reka/reka-server.tar.gz	\
		--grants \
			read=uri=http://acs.amazonaws.com/groups/global/AllUsers

