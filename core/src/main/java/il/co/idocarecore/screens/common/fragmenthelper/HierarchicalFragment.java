package il.co.idocarecore.screens.common.fragmenthelper;


import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public interface HierarchicalFragment {
    @Nullable
    Fragment getHierarchicalParentFragment();
}
