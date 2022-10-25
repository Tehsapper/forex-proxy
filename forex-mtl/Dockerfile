FROM mozilla/sbt:8u292_1.5.7 as build

WORKDIR /forex

COPY .scalafmt.conf .
COPY build.sbt .
COPY project project
COPY src src

RUN sbt assembly

FROM azul/zulu-openjdk-alpine:8u292

WORKDIR /forex

COPY --from=build /forex/target/scala-2.13/forex-assembly-1.0.1.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
