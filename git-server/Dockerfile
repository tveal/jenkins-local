FROM bash:latest

RUN apk add --no-cache git git-daemon
RUN adduser -D -u 1000 alpine
RUN mkdir -p /git/remote \
    && mkdir -p /git/repos \
    && mkdir -p /git/seed-repos \
    && chown -R 1000 /git

USER 1000

WORKDIR /home/alpine

RUN git config --global alias.quickserve "daemon --verbose --export-all --base-path=/git/remote --reuseaddr --enable=receive-pack" \
    && git config --global daemon.receivepack true \
    && git config --global daemon.uploadarch true \
    && git config --global user.name 'git-server-user' \
    && git config --global user.email '<>'

COPY ./start.sh /home/alpine/start.sh

ENTRYPOINT [ "./start.sh" ]
