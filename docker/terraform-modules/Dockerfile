FROM alpine:latest AS base

FROM scratch

COPY --from=base /bin/sh /bin/sh
COPY --from=base /bin/ls /bin/ls
COPY --from=base /bin/cp /bin/cp
COPY --from=base /lib/ld-musl-x86_64.so.1 /lib/ld-musl-x86_64.so.1

COPY  modules /modules

VOLUME "/module-share"

CMD ["/bin/cp", "-f", "-r", "/modules/*", "/modules-share"]
