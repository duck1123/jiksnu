FROM clojure

ENV APP_HOME /app
WORKDIR ${APP_HOME}

# Default application settings
ENV JIKSNU_DB_HOST mongo
ENV HTTP_PORT 8080

# Expose HTTP port
EXPOSE 8080
# Expose nRepl port
EXPOSE 7888
# Expose Karma port
EXPOSE 9876
# Expose Karma Live HTTP reporter
EXPOSE 5060

# Install dependencies
RUN set -x \
    && curl -sL https://deb.nodesource.com/setup_6.x | bash - \
    && curl -sS https://dl.yarnpkg.com/debian/pubkey.gpg | apt-key add - \
    && echo "deb https://dl.yarnpkg.com/debian/ stable main" | tee /etc/apt/sources.list.d/yarn.list \
    && apt-get update \
    && apt-get install -y \
       byobu \
       build-essential \
       curl \
       git \
       jq \
       netcat \
       nodejs \
       yarn \
    && rm -rf /var/lib/apt/lists/*

### Add notify-send proxy
RUN set -x \
    && curl -sL https://github.com/fgrehm/notify-send-http/releases/download/v0.2.0/client-linux_amd64 > /usr/bin/notify-send \
    && chmod +x /usr/bin/notify-send

### Create application user
ARG user=jiksnu
ARG group=jiksnu
ARG uid=1000
ARG gid=1000
RUN groupadd -g ${gid} ${group} \
    && useradd -u ${uid} -g ${gid} --create-home -s /bin/bash ${user} \
    && su ${user} -c "byobu-ctrl-a screen"

# Add base project files
COPY script/gather-dependencies ${APP_HOME}/script/gather-dependencies
COPY package.json project.clj .yarnclean yarn.lock ${APP_HOME}/
RUN script/gather-dependencies

### Add application
COPY . ${APP_HOME}/
RUN script/bootstrap \
    && mv /root/.m2 /home/${user}/ \
    && chown -R ${uid}:${gid} ${APP_HOME} /home/${user}/.m2
USER ${user}

ENTRYPOINT []
CMD [ "script/entrypoint-run-dev" ]
