#!/usr/bin/env sh

chown -R jetty /opt/wallets
chown -R jetty /opt/btcr-continuation

runuser -u jetty -- java -Djetty.http.port=9080 -jar /usr/local/jetty/start.jar