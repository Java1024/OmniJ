package org.mastercoin.test

import com.google.bitcoin.core.Address
import com.google.bitcoin.core.Sha256Hash
import com.google.bitcoin.core.Transaction
import com.msgilligan.bitcoin.BTC
import org.mastercoin.MPRegTestParams
import org.mastercoin.rpc.MastercoinClientDelegate
import static org.mastercoin.CurrencyID.*

import java.security.SecureRandom

trait TestSupport implements MastercoinClientDelegate {
    final BigDecimal stdTxFee = BTC.satoshisToBTC(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE)

    String createNewAccount() {
        def random = new SecureRandom();
        def accountName = "msc-" + new BigInteger(130, random).toString(32)
        return accountName
    }

    Address createFaucetAddress(String account, BigDecimal requestedBTC, BigDecimal requestedMSC) {
        def btcForMSC = requestedMSC / 100
        def startBTC = requestedBTC + btcForMSC + stdTxFee

        // Generate blocks until we have the requested amount of BTC
        while (getBalance() < startBTC) {
            generateBlock()
        }

        // Create a BTC address to hold the requested BTC and MSC
        Address address = getAccountAddress(account)
        // and send it BTC
        Sha256Hash txid = sendToAddress(address, startBTC)

        generateBlock()
        def tx = getTransaction(txid)
        assert tx.confirmations == 1

        // Make sure we got the correct amount of BTC
        BigDecimal btcBalance = getBalance(account)
        assert btcBalance == startBTC

        // Send BTC to get MSC (and TMSC)
        def amounts = [(MPRegTestParams.MoneyManAddress): btcForMSC,
                       (address): startBTC - btcForMSC ]
        txid = sendMany(account, amounts)

        generateBlock()
        tx = getTransaction(txid)
        assert tx.confirmations == 1

        // Verify correct amounts received
        btcBalance = getBalance(account)
        BigDecimal mscBalance = getbalance_MP(address, MSC)
        BigDecimal tmscBalance = getbalance_MP(address, TMSC)

        assert btcBalance == requestedBTC
        assert mscBalance == requestedMSC
        assert tmscBalance == requestedMSC

        return address
    }
}
