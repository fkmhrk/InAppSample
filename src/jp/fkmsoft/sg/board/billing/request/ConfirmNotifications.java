package jp.fkmsoft.sg.board.billing.request;

import jp.fkmsoft.sg.board.billing.BillingRequest;
import jp.fkmsoft.sg.board.billing.BillingService;
import android.os.Bundle;
import android.os.RemoteException;

/**
 * Wrapper class that confirms a list of notifications to the server.
 */
public class ConfirmNotifications extends BillingRequest {
    final String[] mNotifyIds;

    public ConfirmNotifications(BillingService service, int startId, String[] notifyIds) {
        super(service, startId);
        mNotifyIds = notifyIds;
    }

    @Override
    protected long run() throws RemoteException {
        Bundle request = makeRequestBundle("CONFIRM_NOTIFICATIONS");
        request.putStringArray(Field.BILLING_REQUEST_NOTIFY_IDS, mNotifyIds);
        Bundle response = mBillingService.sendBillingRequest(request);
        logResponseCode("confirmNotifications", response);
        return response.getLong(Field.BILLING_RESPONSE_REQUEST_ID,
                Field.BILLING_RESPONSE_INVALID_REQUEST_ID);
    }
}

