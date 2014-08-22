FROM relateiq/oracle-java8

RUN apt-get -y install libapr1 graphviz
RUN ln -s /usr/bin/dot /usr/local/bin/dot

ADD reka.jar reka.jar
ADD config config

CMD ["java", "-jar", "reka.jar", "config/main.reka"]
