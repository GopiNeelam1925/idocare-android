package il.co.idocare.dependencyinjection.contextscope;

import dagger.Subcomponent;
import il.co.idocare.dependencyinjection.controllerscope.ControllerComponent;
import il.co.idocare.dependencyinjection.controllerscope.ControllerModule;
import il.co.idocare.dependencyinjection.serversync.ServerSyncComponent;
import il.co.idocare.dependencyinjection.serversync.ServerSyncModule;

@ContextScope
@Subcomponent(modules = ContextModule.class)
public interface ContextComponent {

    ControllerComponent newControllerComponent(ControllerModule controllerModule);

    ServerSyncComponent newServerSyncComponent(ServerSyncModule serverSyncModule);


}
