
installtcnative:
	@rm -rf repo
	@mkdir repo
	@mvn install:install-file \
		-DlocalRepositoryPath=repo \
		-Dclassifier=linux-x86_64 \
		-Dfile=../../../reka-netty-tcnative/target/netty-tcnative-1.1.30.Fork2-openssl-1.0.1-linux-x86_64.jar
