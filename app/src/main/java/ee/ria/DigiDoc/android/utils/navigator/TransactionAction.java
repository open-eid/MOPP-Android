package ee.ria.DigiDoc.android.utils.navigator;

public interface TransactionAction<T extends Transaction> {

    T transaction();
}
