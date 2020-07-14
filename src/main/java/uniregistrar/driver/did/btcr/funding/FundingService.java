package uniregistrar.driver.did.btcr.funding;

import java.util.Map;

import javax.annotation.Nullable;

import org.bitcoinj.core.ECKey;

public interface FundingService {

	/**
	 * Used for server-side funded registration operations
	 * 
	 * @return new {@link BtcrFund} with required transactional information to spend
	 *         targeted output, including its private key
	 * @throws FundingException Funding exception indicator
	 */
	BtcrFund getServerSideFund() throws FundingException;

	/**
	 *
	 * @param txid      Bitcoin Transaction ID
	 * @param privKey   Private-Key
	 * @param outIndex  Index of the spendable transaction output
	 * @param rotateKey When true, new random ECKey is created for change, funding
	 *                  key is used for the change key otherwise
	 * @return new {@link BtcrFund} with required transactional information to spend
	 *         targeted output
	 * @throws FundingException Funding exception indicator
	 */
	BtcrFund getSelfFund(String txid, ECKey privKey, @Nullable Integer outIndex, boolean rotateKey)
			throws FundingException;

	/**
	 * 
	 * @param fundingTicket UUID in string identifies the ticket
	 * @param rotateKey     When true, new random ECKey is created for change,
	 *                      funding * key is used for the change key otherwise
	 * @return new {@link BtcrFund} with required transactional information to spend
	 * @throws FundingException If given address has no spendable transaction, an
	 *                          exception FundingException will be thrown
	 */
	BtcrFund getExpectedFund(String fundingTicket, boolean rotateKey) throws FundingException;

	/**
	 * Checks if server has UTXO for funding registration process
	 * 
	 * @implNote Relies on non-removed imported key size of the UTXOWallet
	 * @return True if wallet has UTXOs, false otherwise
	 */
	boolean canServerFund();

	/**
	 *
	 * @return Address to fund with funding a funding ticket (UUID)
	 */
	Map<String, String> askForFunding();

	/**
	 *
	 * @return Address to fund with funding a funding ticket (UUID)
	 */
	String askForFundingString();
}
