import java.security.interfaces.RSAKey;
import java.util.ArrayList;
import java.util.Arrays;

public class TxHandler {

	/* Creates a public ledger whose current UTXOPool (collection of unspent 
	 * transaction outputs) is utxoPool. This should make a defensive copy of 
	 * utxoPool by using the UTXOPool(UTXOPool uPool) constructor.
	 */
	private UTXOPool utxoPool;

	public TxHandler(UTXOPool utxoPool) {
		this.utxoPool = new UTXOPool(utxoPool);
	}

	/* Returns true if 
	 * (1) all outputs claimed by tx are in the current UTXO pool, //Voorkomt double spending
	 * (2) the signatures on each input of tx are valid, ?V
	 * (3) no UTXO is claimed multiple times by tx, ?V
	 * (4) all of tx’s output values are non-negative, and v
	 * (5) the sum of tx’s input values is greater than or equal to the sum of   
	        its output values; 
	   and false otherwise. ?
	 */

	public boolean isValidTx(Transaction tx) {
		ArrayList<UTXO> usedUTXOs = new ArrayList<>();
		double sumInput = 0.0;
		double sumOutput = 0.0;
		//Hash en index om de output te vinden voor punt 5. 

		//Voor punt 2 alle input signatures checken. Gebruik de jar verify
		//GetRawDataToSign om de data te krijgen die je moet checken met inputs

		for (Transaction.Input input : tx.getInputs()) {
			//1
			UTXO curUtxo = new UTXO(input.prevTxHash, input.outputIndex);
			
			if (!utxoPool.contains(curUtxo)) {
                return false;
            }
			
			//3
			if(usedUTXOs.contains(curUtxo)){
				return false;
			} else{
				usedUTXOs.add(curUtxo);
			}

			//2
			
			byte[] data = tx.getRawDataToSign(input.outputIndex);
			System.out.println("Data: " + data);
			System.out.println("Index output: " + input.outputIndex);
			System.out.println("Address: " + utxoPool.getTxOutput(curUtxo).address);

			if(data == null || !utxoPool.getTxOutput(curUtxo).address.verifySignature(data, input.signature)){
				return false;
			}

			//5
			sumInput += utxoPool.getTxOutput(curUtxo).value;
		}

		
		for(var output : tx.getOutputs()) {
			//4
			if (output.value < 0) {
				return false;
			}
			//5
			sumOutput += output.value;
		}

		//5
		if(sumInput < sumOutput){
			return false;
		}

		return true;
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
				for (Transaction.Input input : transaction.getInputs()) {
					utxoPool.removeUTXO(new UTXO(input.prevTxHash, input.outputIndex));
				}
				validTxs.add(transaction);
			}
		}
		return validTxs.toArray(new Transaction[validTxs.size()]);
	}
} 
