FROM relateiq/oracle-java8

RUN apt-get -y install libapr1 graphviz
RUN ln -s /usr/bin/dot /usr/local/bin/dot

ADD reka-main.jar reka-main.jar
ADD config config
ADD bundles bundles

CMD ["java", "-jar", "reka-main.jar", "config/main.reka"]
