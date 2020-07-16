# Driver Variables

**${chain}** = mainnet | testnet | regtest

**${Chain}** = Mainnet | Testnet | Regtest 

| Description                                                  | Environment                                        | Property                     | Default                          |
| ------------------------------------------------------------ | -------------------------------------------------- | ---------------------------- | -------------------------------- |
| **Flag for activating mainnet**: <true,false>                | uniregistrar_driver_did_btcr_Mainnet               | chain.mainnet                | false                            |
| **Flag for activating testnet**: <true,false>                | uniregistrar_driver_did_btcr_Testnet               | chain.testnet                | false                            |
| **Flag for activating regtestnet**: <true,false>             | uniregistrar_driver_did_btcr_Regtest               | chain.regtest                | false                            |
| **BTCR Method Prefix:** *// It should be permanent, it is only there in case another Bitcoin related needs to be supported* | uniregistrar_driver_did_btcr_methodPrefix          | btcr.method.prefix           | did:btcr:                        |
| **BTCR Chain Prefixes:** ${chain} = mainnet \| testnet \| regtest | uniregistrar_driver_did_btcr_${chain}Prefix        | btcr.${chain}.prefix         | tx1: \| txtest1: \| txtest1:     |
| **BTCR Base Path:** For the DIDDocContinuation               | uniregistrar_driver_did_btcr_basePath              | base.path                    |                                  |
| **BTCR Base URI:** For the DIDDocContinuation                | uniregistrar_driver_did_btcr_baseUri               | base.uri                     |                                  |
| **BitcoinJ Wallet Path:**                                    | uniregistrar_driver_did_btcr_walletPath${Chain}    | wallet.path.${chain}         | /opt/wallets                     |
| **BitcoinJ Wallet Prefix:**                                  | uniregistrar_driver_did_btcr_walletPrefix${Chain}  | wallet.prefix.${chain}       | ${chain}-wallet                  |
| **Target Amount of UTXOs to upkeep:**                        | uniregistrar_driver_did_btcr_utxoUpkeep${Chain}    | wallet.utxo.upkeep.${chain}  | regtest=300 testnet=50 mainnet=1 |
| **Wallet Initialization Key:**                               | uniregistrar_driver_did_btcr_privateKey${Chain}    | wallet.${chain}.key          |                                  |
| **RPC Connection to Bitcoin full node:**                     | uniregistrar_driver_did_btcr_rpcUrl${Chain}        | conn.rpc.url.${chain}        |                                  |
| **Driver-side fund amount to use per registration:**         | uniregistrar_driver_did_btcr_targetFundAmount      | btcr.target.fund.amount      | 10000 Satoshis                   |
| **Required tx depth to consider transactions immutable:**    | uniregistrar_driver_did_btcr_requiredTxDepth       | btcr.required.tx.depth       | 1                                |
| **Creation of the new UTXOs:**                               | uniregistrar_driver_did_btcr_utxoProducing${Chain} | wallet.produce.utxo.${chain} | false                            |
| **Follow the deactivation TXs:**                             | uniregistrar_driver_did_btcr_walletScriptType      | driver.follow.deactivation   | true                             |
| **Peers**: Increase the number for multiple peers            | uniregistrar_driver_did_btcr_peer${Chain}          | conn.${chain}.peer.001       | Optional                         |