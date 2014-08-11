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
