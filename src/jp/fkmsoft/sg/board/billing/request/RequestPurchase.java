package jp.fkmsoft.sg.board.billing.request;

import jp.fkmsoft.sg.board.billing.BillingLogger;
import jp.fkmsoft.sg.board.billing.BillingRequest;
import jp.fkmsoft.sg.board.billing.BillingService;
import jp.fkmsoft.sg.board.billing.ResponseCode;
import jp.fkmsoft.sg.board.billing.ResponseHandler;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;

/**
 * Wrapper class that requests a purchase.
 */
public class RequestPurchase extends BillingRequest {
    private static final String TAG = "Request Purchase";
    public final String mProductId;
    public final String mDeveloperPayload;
    public final String mProductType;

    /** Constructor
     *
     * @param itemId  The ID of the item to be purchased. Will be assumed to be a one-time
     *                purchase.
     * @param itemType  Either Consts.ITEM_TYPE_INAPP or Consts.ITEM_TYPE_SUBSCRIPTION,
     *                  indicating the type of item type support is being checked for.
     * @param developerPayload Optional data.
     */
    public RequestPurchase(BillingService service, String itemId, String itemType, String developerPayload) {
        // This object is never created as a side effect of starting this
        // service so we pass -1 as the startId to indicate that we should
        // not stop this service after executing this request.
        super(service, -1);
        mProductId = itemId;
        mDeveloperPayload = developerPayload;
        mProductType = itemType;
    }

    @Override
    protected long run() throws RemoteException {
        Bundle request = makeRequestBundle("REQUEST_PURCHASE");
        request.putString(Field.BILLING_REQUEST_ITEM_ID, mProductId);
        request.putString(Field.BILLING_REQUEST_ITEM_TYPE, mProductType);
        // Note that the developer payload is optional.
        if (mDeveloperPayload != null) {
            request.putString(Field.BILLING_REQUEST_DEVELOPER_PAYLOAD, mDeveloperPayload);
        }
        Bundle response = mBillingService.sendBillingRequest(request);
        PendingIntent pendingIntent
                = response.getParcelable(Field.BILLING_RESPONSE_PURCHASE_INTENT);
        if (pendingIntent == null) {
            BillingLogger.error(TAG, "Error with requestPurchase");
            return Field.BILLING_RESPONSE_INVALID_REQUEST_ID;
        }

        Intent intent = new Intent();
        ResponseHandler.buyPageIntentResponse(pendingIntent, intent);
        return response.getLong(Field.BILLING_RESPONSE_REQUEST_ID,
                Field.BILLING_RESPONSE_INVALID_REQUEST_ID);
    }

    @Override
    protected void responseCodeReceived(ResponseCode responseCode) {
        ResponseHandler.responseCodeReceived(mBillingService, this, responseCode);
    }
}
