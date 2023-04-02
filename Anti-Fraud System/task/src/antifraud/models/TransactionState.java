package antifraud.models;

public enum TransactionState {
    NOT_ALLOWED("NOT ALLOWED"),
    ALLOWED("ALLOWED"),
    MANUAL_PROCESSING("MANUAL_PROCESSING"),
    PROHIBITED("PROHIBITED"),
    NONE(""),
    ;

    private final String value;

    TransactionState(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static boolean isTransactionState(String state) {
        for (TransactionState transactionState : values()) {
            if (transactionState.value.equalsIgnoreCase(state)) return true;
        }
        return false;
    }
}
