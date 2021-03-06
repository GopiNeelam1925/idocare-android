package il.co.idocare.screens.navigationdrawer;

import androidx.annotation.UiThread;

/**
 * Implementations of this interface can be used in order to manipulate the
 * state of NavigationDrawer
 */
@UiThread
public interface NavigationDrawerDelegate {
    void openDrawer();
    void closeDrawer();
}
