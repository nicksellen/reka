FROM relateiq/oracle-java8

RUN apt-get -y install libapr1 graphviz
RUN ln -s /usr/bin/dot /usr/local/bin/dot

ADD reka-server.jar reka-server.jar
ADD apps apps
ADD bundles bundles
ADD config.reka config.reka

CMD ["java", "-jar", "reka-server.jar", "config.reka"]
