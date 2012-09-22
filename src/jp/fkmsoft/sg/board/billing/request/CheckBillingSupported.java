package jp.fkmsoft.sg.board.billing.request;

import jp.fkmsoft.sg.board.billing.BillingLogger;
import jp.fkmsoft.sg.board.billing.BillingRequest;
import jp.fkmsoft.sg.board.billing.BillingService;
import jp.fkmsoft.sg.board.billing.ResponseCode;
import jp.fkmsoft.sg.board.billing.ResponseHandler;
import android.os.Bundle;
import android.os.RemoteException;

/**
 * Wrapper class that checks if in-app billing is supported.
 *
 * Note: Support for subscriptions implies support for one-time purchases. However, the opposite
 * is not true.
 *
 * Developers may want to perform two checks if both one-time and subscription products are
 * available.
 */
public class CheckBillingSupported extends BillingRequest {
    private static final String TAG = "Check Billing Supported";
    public String mProductType = null;

    /** Constructor
     *
     * Note: Support for subscriptions implies support for one-time purchases. However, the
     * opposite is not true.
     *
     * Developers may want to perform two checks if both one-time and subscription products are
     * available.
     *
     * @pram itemType Either Consts.ITEM_TYPE_INAPP or Consts.ITEM_TYPE_SUBSCRIPTION, indicating
     * the type of item support is being checked for.
     */
    public CheckBillingSupported(BillingService service, String itemType) {
        super(service, -1);
        mProductType = itemType;
    }
    
    @Override
    protected long run() throws RemoteException {
        Bundle request = makeRequestBundle("CHECK_BILLING_SUPPORTED");
        if (mProductType != null) {
            request.putString(Field.BILLING_REQUEST_ITEM_TYPE, mProductType);
        }
        Bundle response = mBillingService.sendBillingRequest(request);
        int responseCode = response.getInt(Field.BILLING_RESPONSE_RESPONSE_CODE);
        if (BillingLogger.isDebugEnabled()) {
            BillingLogger.info(TAG, "CheckBillingSupported response code: " +
                    ResponseCode.valueOf(responseCode));
        }
        boolean billingSupported = (responseCode == ResponseCode.RESULT_OK.ordinal());
        ResponseHandler.checkBillingSupportedResponse(billingSupported, mProductType);
        return Field.BILLING_RESPONSE_INVALID_REQUEST_ID;
    }
}

