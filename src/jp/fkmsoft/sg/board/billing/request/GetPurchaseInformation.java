package jp.fkmsoft.sg.board.billing.request;

import jp.fkmsoft.sg.board.billing.BillingRequest;
import jp.fkmsoft.sg.board.billing.BillingService;
import jp.fkmsoft.sg.board.billing.Security;
import android.os.Bundle;
import android.os.RemoteException;

/**
 * Wrapper class that sends a GET_PURCHASE_INFORMATION message to the server.
 */
public class GetPurchaseInformation extends BillingRequest {
    long mNonce;
    final String[] mNotifyIds;

    public GetPurchaseInformation(BillingService service, int startId, String[] notifyIds) {
        super(service, startId);
        mNotifyIds = notifyIds;
    }

    @Override
    protected long run() throws RemoteException {
        mNonce = Security.generateNonce();

        Bundle request = makeRequestBundle("GET_PURCHASE_INFORMATION");
        request.putLong(Field.BILLING_REQUEST_NONCE, mNonce);
        request.putStringArray(Field.BILLING_REQUEST_NOTIFY_IDS, mNotifyIds);
        Bundle response = mBillingService.sendBillingRequest(request);
        logResponseCode("getPurchaseInformation", response);
        return response.getLong(Field.BILLING_RESPONSE_REQUEST_ID,
                Field.BILLING_RESPONSE_INVALID_REQUEST_ID);
    }

    @Override
    protected void onRemoteException(RemoteException e) {
        super.onRemoteException(e);
        Security.removeNonce(mNonce);
    }
}

