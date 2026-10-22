FROM fcr.ford.com/devenablement/jre17-dynatrace:latest

ARG ARCHIVE=build/libs/grid-v2.jar
ARG SPRING_PROFILES_ACTIVE=dev
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}

ENV JAVA_TOOL_OPTIONS="-Xshare:off -Djava.net.preferIPv4Stack=true -agentpath:/opt/dynatrace/oneagent/agent/lib64/liboneagentloader.so"

ARG HTTP_PROXY=http://internet.gcp.ford.com:83
ARG HTTPS_PROXY=http://internet.gcp.ford.com:83

EXPOSE 8080

ADD ${ARCHIVE} grid-v2.jar

ENTRYPOINT ["sh", "-c", "\
  echo \"JAVA_TOOL_OPTIONS = ${JAVA_TOOL_OPTIONS}\";                                                   \
  echo \"Starting application with JAVA_OPTS = ${JAVA_OPTS}\";                                         \
  exec java ${JAVA_TOOL_OPTIONS} -jar grid-v2.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE}              \
"]
