# Dockerfile for universalregistrar/driver-did-btcr

FROM maven:3-jdk-11 AS BUILD
MAINTAINER Markus Sabadello <markus@danubetech.com>

ENV PATH $JAVA_HOME/bin:$PATH

# build driver-did-btcr

ADD . /opt/driver-did-btcr
RUN cd /opt/driver-did-btcr && mvn clean install package -P war -N -DskipTests

FROM jetty:9.4-jre11-slim

# variables

ENV uniregistrar_driver_did_btcr_Mainnet=
ENV uniregistrar_driver_did_btcr_Testnet=
ENV uniregistrar_driver_did_btcr_Regtest=

ENV uniregistrar_driver_did_btcr_methodPrefix=
ENV uniregistrar_driver_did_btcr_testnetPrefix=
ENV uniregistrar_driver_did_btcr_mainnetPrefix=
ENV uniregistrar_driver_did_btcr_regtestPrefix=

ENV uniregistrar_driver_did_btcr_basePath=/opt/btcr-continuation
ENV uniregistrar_driver_did_btcr_baseUri=http://localhost

ENV uniregistrar_driver_did_btcr_privateKeyMainnet=
ENV uniregistrar_driver_did_btcr_privateKeyTestnet=
ENV uniregistrar_driver_did_btcr_privateKeyRegtest=

ENV uniregistrar_driver_did_btcr_walletPathMainnet=/opt/wallets
ENV uniregistrar_driver_did_btcr_walletPathTestnet=/opt/wallets
ENV uniregistrar_driver_did_btcr_walletPathRegtest=/opt/wallets

ENV uniregistrar_driver_did_btcr_walletPrefixMainnet=mainnet
ENV uniregistrar_driver_did_btcr_walletPrefixTestnet=testnet
ENV uniregistrar_driver_did_btcr_walletPrefixRegtest=regtestnet

ENV uniregistrar_driver_did_btcr_requiredTxDepth=
ENV uniregistrar_driver_did_btcr_targetFundAmount=

ENV uniregistrar_driver_did_btcr_utxoUpkeepMainnet=
ENV uniregistrar_driver_did_btcr_utxoUpkeepTestnet=
ENV uniregistrar_driver_did_btcr_utxoUpkeepRegtest=

ENV uniregistrar_driver_did_btcr_utxoProducingMainnet=false
ENV uniregistrar_driver_did_btcr_utxoProducingTestnet=true
ENV uniregistrar_driver_did_btcr_utxoProducingRegtest=true

ENV uniregistrar_driver_did_btcr_checkIntervalMainnet=
ENV uniregistrar_driver_did_btcr_checkIntervalTestnet=
ENV uniregistrar_driver_did_btcr_checkIntervalRegtest=

ENV uniregistrar_driver_did_btcr_rpcUrlMainnet=
ENV uniregistrar_driver_did_btcr_rpcUrlTestnet=
ENV uniregistrar_driver_did_btcr_rpcUrlRegtest=

ENV uniregistrar_driver_did_btcr_peerMainnet=
ENV uniregistrar_driver_did_btcr_peerTestnet=
ENV uniregistrar_driver_did_btcr_bitcoinConnection=BTCD

ENV uniregistrar_driver_did_btcr_followDeactivation=true
ENV uniregistrar_driver_did_btcr_overWriteDidDocs=true

ENV uniregistrar_driver_did_btcr_rpcCertMainnet=
ENV uniregistrar_driver_did_btcr_rpcCertTestnet=

# copy from build stage

COPY --from=build /opt/driver-did-btcr/target/*.war /var/lib/jetty/webapps/ROOT.war
COPY --from=build /opt/driver-did-btcr/docker/run-driver-did-btcr.sh /opt

# done

EXPOSE 9080

USER root
RUN chmod a+rx /opt/run-driver-did-btcr.sh
CMD "/opt/run-driver-did-btcr.sh"