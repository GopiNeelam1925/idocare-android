package il.co.idocare.views;

import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;
import java.util.List;

import il.co.idocare.handlermessaging.HandlerMessagingMaster;
import il.co.idocare.handlermessaging.HandlerMessagingSlave;

/**
 * This is an abstract implementation of ViewMVC interface which provides some convenience
 * logic specific to the app.
 */
public abstract class AbstractViewMVC implements
        ViewMVC,
        HandlerMessagingMaster,
        HandlerMessagingSlave {

    Handler mInboxHandler;
    final List<Handler> mOutboxHandlers = new ArrayList<Handler>();

    // ---------------------------------------------------------------------------------------------
    //
    // Handler messaging methods


    /**
     * Handle the message received by the inbox Handler
     * @param msg message to handle
     */
    protected abstract void handleMessage(Message msg);

    @Override
    public Handler getInboxHandler() {
        // Since most of the work done in MVC Views consist of manipulations on underlying
        // Android Views, it will be convenient (and less error prone) if MVC View's inbox Handler
        // will be running on UI thread.
        if (mInboxHandler == null) {
            // TODO: review hte warning about possible memory leak due to this handler being inner class
            mInboxHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    AbstractViewMVC.this.handleMessage(msg);
                }
            };
        }
        return mInboxHandler;
    }

    @Override
    public void addOutboxHandler(Handler handler) {
        // Not sure that there will be use case that requires sync, but just as precaution...
        synchronized (mOutboxHandlers) {
            if (!mOutboxHandlers.contains(handler)) {
                mOutboxHandlers.add(handler);
            }
        }
    }

    @Override
    public void removeOutboxHandler(Handler handler) {
        // Not sure that there will be use case that requires sync, but just as precaution...
        synchronized (mOutboxHandlers) {
            mOutboxHandlers.remove(handler);
        }
    }

    @Override
    public void notifyOutboxHandlers(int what, int arg1, int arg2, Object obj) {
        // Not sure that there will be use case that requires sync, but just as precaution...
        synchronized (mOutboxHandlers) {
            for (Handler handler : mOutboxHandlers) {
                Message msg = Message.obtain(handler, what, arg1, arg2, obj);
                msg.sendToTarget();
            }
        }

    }

    // End of Handler messaging methods
    //
    // ---------------------------------------------------------------------------------------------

}
