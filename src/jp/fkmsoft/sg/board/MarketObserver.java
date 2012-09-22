package jp.fkmsoft.sg.board;

import android.app.Activity;
import android.os.Handler;
import android.widget.TextView;
import jp.fkmsoft.sg.board.billing.PurchaseObserver;
import jp.fkmsoft.sg.board.billing.PurchaseState;
import jp.fkmsoft.sg.board.billing.ResponseCode;
import jp.fkmsoft.sg.board.billing.request.RequestPurchase;
import jp.fkmsoft.sg.board.billing.request.RestoreTransactions;

public class MarketObserver extends PurchaseObserver {

    private TextView logText;
    
    public MarketObserver(Activity activity, Handler handler) {
        super(activity, handler);
        logText = (TextView) activity.findViewById(R.id.observer_out);
    }

    @Override
    public void onBillingSupported(boolean supported, String type) {
        addLog("Billing supported[" + type + "] = " + supported);

    }

    @Override
    public void onPurchaseStateChange(PurchaseState purchaseState,
            String itemId, int quantity, long purchaseTime,
            String developerPayload) {
        addLog("Purchase State Change state=" + purchaseState.ordinal());

    }

    @Override
    public void onRequestPurchaseResponse(RequestPurchase request,
            ResponseCode responseCode) {
        addLog("Request Purchase Response code=" + responseCode.ordinal());
        addLog("productID=" + request.mProductId);
    }

    @Override
    public void onRestoreTransactionsResponse(RestoreTransactions request,
            ResponseCode responseCode) {
        addLog("Restore Transaction Response code=" + responseCode);
    }
    
    private void addLog(String msg) {
        logText.setText(logText.getText().toString() + "\n" + msg);
    }

}
