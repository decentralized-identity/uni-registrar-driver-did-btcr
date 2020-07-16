## Driver Defaults

```typescript
# Driver
WAITING_TIME = 3     // Default waiting time in seconds when shutdown is triggered
PROPERTIES_FILE_PATH = "/driver.properties"
DEFAULT_BITCOINCLIENT = BITCOIND
MAX_OUTPUT_COUNT_PER_TX = 10 // Max amount of outputs are included in a single transaction, used by UTXOProducer

# Wallet Service
ACTIVATE_UTXO_PRODUCING = false // for every chain
REQUIRED_TX_DEPTH = 1 // Then completion of the document is triggered
TARGET_FUND_AMOUNT = 100000 // satoshis
WALLET_PATH = "/opt/wallets"
SCRIPT_TYPE = P2PKH
TX_FEE = 1000 // satoshis
FUNDING_WAIT_TIME = 24 // Hours. When client asked for a funding with a ticket

// Number of usable UTXOs to keep
UTXO_UPKEEP_MAINNET = 1
UTXO_UPKEEP_TESTNET = 50
UTXO_UPKEEP_REGTEST = 300

//Prefixes
DEFAULT_WALLET_PREFIX_MAINNET = "mainnet"
DEFAULT_WALLET_PREFIX_TESTNET = "testnet"
DEFAULT_WALLET_PREFIX_REGTEST = "regtest"

# BTCR Method
DEFAULT_DID_CONTINUATION = localfile
DEFAULT_METHOD_PREFIX = did:btcr:
DEFAULT_MAINNET_PREFIX = tx1:
DEFAULT_TESTNET_PREFIX = txtest1:
DEFAULT_REGTEST_PREFIX = txtest1:
```