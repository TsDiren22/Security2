import java.util.ArrayList;
import java.util.Collection;

public class TxHandler {

	/* Creates a public ledger whose current UTXOPool (collection of unspent 
	 * transaction outputs) is utxoPool. This should make a defensive copy of 
	 * utxoPool by using the UTXOPool(UTXOPool uPool) constructor.
	 */
	public Collection<UTXOPool> collection;
	private ArrayList<byte[]> txs;
	private boolean isUnique = true;

	public TxHandler(UTXOPool utxoPool) {
		collection = new ArrayList<>();
		txs = new ArrayList<>();
		for (var utxo : utxoPool.getAllUTXO()) {
			if (utxoPool.contains(utxo)) {
				collection.add(utxoPool);
				if (!txs.contains(utxo.getTxHash())) {
					txs.add(utxo.getTxHash());
				} else {
					isUnique = false;
				}
			}
		}
	}

	/* Returns true if 
	 * (1) all outputs claimed by tx are in the current UTXO pool, V
	 * (2) the signatures on each input of tx are valid, ?V
	 * (3) no UTXO is claimed multiple times by tx, ?V
	 * (4) all of tx’s output values are non-negative, and v
	 * (5) the sum of tx’s input values is greater than or equal to the sum of   
	        its output values; 
	   and false otherwise. ?
	 */

	public boolean isValidTx(Transaction tx) {
		Boolean isValid = true;
		int sumOutput = 0;
		int sumInput = 0;

		for(var output : tx.getOutputs()) {
			if (!collection.contains(output) || output.value < 0) {
				isValid = false;
			}
			sumOutput += output.value;
		}

		for(var input : tx.getInputs()) {
			if (input.signature == null) {
				isValid = false;
			}

			for (UTXOPool c : collection) {
				if (c.contains(new UTXO(input.prevTxHash, input.outputIndex))) {
					sumInput += c.getTxOutput(new UTXO(input.prevTxHash, input.outputIndex)).value;
				} else {
					isValid = false;
				}
			}
		}
		return isValid && isUnique && sumInput >= sumOutput;
	}

	/* Handles each epoch by receiving an unordered array of proposed 
	 * transactions, checking each transaction for correctness, 
	 * returning a mutually valid array of accepted transactions, 
	 * and updating the current UTXO pool as appropriate.
	 */
	public Transaction[] handleTxs(Transaction[] possibleTxs) {
		ArrayList<Transaction> validTxs = new ArrayList<>();
		for (Transaction transaction : possibleTxs) {
			if (isValidTx(transaction)) {
				validTxs.add(transaction);
			}
		}
		return validTxs.toArray(new Transaction[validTxs.size()]);
	}
} 
