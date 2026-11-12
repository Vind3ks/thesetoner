# Build a single runnable jar
FROM sbtscala/scala-sbt:eclipse-temurin-21.0.12.1_1_1.13.1_2.13.18 AS build
WORKDIR /build
COPY build.sbt ./
COPY project/build.properties project/plugins.sbt project/
RUN sbt -batch update
COPY src src
RUN sbt -batch assembly

FROM eclipse-temurin:21.0.13_9-jre-noble
WORKDIR /app
COPY --from=build /build/target/scala-2.13/thesetoner.jar /app/thesetoner.jar
# Starting copies of the bot's lists (quotes, taunts, admins...). On first start they are
# copied into /data, which should be a persistent volume; after that the bot's own copies win.
COPY TheSetoner-*.txt /app/seed/
COPY docker/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh && mkdir -p /data && chown 1000:1000 /data

# Settings come from environment variables:
# IRC_SERVER, IRC_USER, IRC_PASS, IRC_CHANNELS (same names as QAI)
ENV DATA_DIR=/data
USER 1000:1000
VOLUME /data
ENTRYPOINT ["/app/entrypoint.sh"]
