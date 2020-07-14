#!/bin/sh

mkdir -p ${uniregistrar_driver_did_btcr_basePath}
mkdir -p ${uniregistrar_driver_did_btcr_walletPathMainnet}
mkdir -p ${uniregistrar_driver_did_btcr_walletPathTestnet}
mkdir -p ${uniregistrar_driver_did_btcr_walletPathRegtestnet}

cd /opt/driver-did-btcr/
mvn jetty:run -P war -Dorg.eclipse.jetty.annotations.maxWait=240
