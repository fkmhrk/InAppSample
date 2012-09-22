package jp.fkmsoft.sg.board.billing.request;

import jp.fkmsoft.sg.board.billing.BillingRequest;
import jp.fkmsoft.sg.board.billing.BillingService;
import jp.fkmsoft.sg.board.billing.ResponseCode;
import jp.fkmsoft.sg.board.billing.ResponseHandler;
import jp.fkmsoft.sg.board.billing.Security;
import android.os.Bundle;
import android.os.RemoteException;

/**
 * Wrapper class that sends a RESTORE_TRANSACTIONS message to the server.
 */
public class RestoreTransactions extends BillingRequest {
    long mNonce;

    public RestoreTransactions(BillingService service) {
        // This object is never created as a side effect of starting this
        // service so we pass -1 as the startId to indicate that we should
        // not stop this service after executing this request.
        super(service, -1);
    }

    @Override
    protected long run() throws RemoteException {
        mNonce = Security.generateNonce();

        Bundle request = makeRequestBundle("RESTORE_TRANSACTIONS");
        request.putLong(Field.BILLING_REQUEST_NONCE, mNonce);
        Bundle response = mBillingService.sendBillingRequest(request);
        logResponseCode("restoreTransactions", response);
        return response.getLong(Field.BILLING_RESPONSE_REQUEST_ID,
                Field.BILLING_RESPONSE_INVALID_REQUEST_ID);
    }

    @Override
    protected void onRemoteException(RemoteException e) {
        super.onRemoteException(e);
        Security.removeNonce(mNonce);
    }

    @Override
    protected void responseCodeReceived(ResponseCode responseCode) {
        ResponseHandler.responseCodeReceived(mBillingService, this, responseCode);
    }
}

