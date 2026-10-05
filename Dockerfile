# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline || true
COPY src src
RUN mvn -q -B package -DskipTests

# ---------- runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /deployments

# Par de chaves do JWT gerado na imagem (nunca vai para o git).
# Cada nova publicação gera chaves novas: quem estiver logado só precisa entrar de novo.
RUN mkdir -p /deployments/keys \
 && openssl genrsa -out /deployments/keys/privateKey.pem 2048 \
 && openssl rsa -in /deployments/keys/privateKey.pem -pubout -out /deployments/keys/publicKey.pem

COPY --from=build /app/target/quarkus-app/lib/ ./lib/
COPY --from=build /app/target/quarkus-app/*.jar ./
COPY --from=build /app/target/quarkus-app/app/ ./app/
COPY --from=build /app/target/quarkus-app/quarkus/ ./quarkus/

ENV JWT_PRIVATE_KEY_LOCATION=/deployments/keys/privateKey.pem \
    JWT_PUBLIC_KEY_LOCATION=/deployments/keys/publicKey.pem \
    TZ=America/Araguaina \
    JAVA_OPTS="-Duser.timezone=America/Araguaina -XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

EXPOSE 8080
CMD ["sh", "-c", "java $JAVA_OPTS -jar quarkus-run.jar"]
