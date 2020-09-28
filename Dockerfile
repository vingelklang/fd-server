FROM openjdk:8-alpine

COPY target/uberjar/fd_server.jar /fd_server/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/fd_server/app.jar"]
