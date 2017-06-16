
package conf;


import ninja.AssetsController;
import ninja.Router;
import ninja.application.ApplicationRoutes;
import controllers.AppController;

public class Routes implements ApplicationRoutes {

    @Override
    public void init(Router router) {  
        
        router.GET().route("/").with(AppController::index);
        router.GET().route("/training").with(AppController::training);
        router.POST().route("/trainingExec").with(AppController::trainingExec);
        router.GET().route("/dialogue").with(AppController::dialogue);
        router.POST().route("/dialogue").with(AppController::dialogue);
        router.GET().route("/analysis").with(AppController::analysis);
 
        ///////////////////////////////////////////////////////////////////////
        // Assets (pictures / javascript)
        ///////////////////////////////////////////////////////////////////////    
        router.GET().route("/assets/webjars/{fileName: .*}").with(AssetsController::serveWebJars);
        router.GET().route("/assets/{fileName: .*}").with(AssetsController::serveStatic);
        
        ///////////////////////////////////////////////////////////////////////
        // Index / Catchall shows index page
        ///////////////////////////////////////////////////////////////////////
        router.GET().route("/.*").with(AppController::index);
    }

}
