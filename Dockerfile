# =====================================================================
# Imagem da api-contas (usada pelo Render e por qualquer hospedagem com Docker)
#
# Etapa 1 compila o projeto; etapa 2 só roda o .jar (imagem menor e mais segura).
# Nenhuma senha fica aqui: tudo vem das variáveis de ambiente da hospedagem.
# =====================================================================

# ---------- Etapa 1: compilar ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Primeiro só o que define as dependências (o Docker reaproveita essa etapa
# quando só o código muda, deixando o build mais rápido)
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q package -DskipTests

# ---------- Etapa 2: rodar ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Não roda como administrador (root). A pasta "dados" (currículos) precisa
# pertencer a esse usuário, senão a API não consegue criá-la ao ligar.
RUN useradd --system --uid 1001 app \
    && mkdir -p /app/dados/curriculos \
    && chown -R app /app/dados
COPY --from=build /app/target/*.jar app.jar
USER app

# Ajustes para caber em máquinas pequenas (ex.: plano grátis com 512 MB):
# usa até 75% da memória, coletor de lixo simples e inicialização mais rápida
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
