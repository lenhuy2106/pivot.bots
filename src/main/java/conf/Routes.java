package conf;

import controllers.AppController;
import ninja.AssetsController;
import ninja.Router;
import ninja.application.ApplicationRoutes;

/**
 * Internal routing of the http requests.
 * Defines the access points of the user/browser.
 * @author Nhu Huy Le <mail@huy-le.de>
 * @see <a href"http://www.ninjaframework.org/documentation/basic_concepts/routing.html">http://www.ninjaframework.org/documentation/basic_concepts/routing.html</a>
 */
public class Routes implements ApplicationRoutes {

    /**
     * Route the http requests.
     * @param router Router to configure.
     */
    @Override
    public void init(Router router) {
        router.GET().route("/").with(AppController::index);
        router.GET().route("/learning").with(AppController::learning);
        router.POST().route("/learningStart").with(AppController::learningStart);
        router.GET().route("/dialogue").with(AppController::dialogue);
        router.POST().route("/dialogue").with(AppController::dialogue);
        router.GET().route("/analysis").with(AppController::analysis);
        router.GET().route("/settings").with(AppController::settings);
        router.POST().route("/settings").with(AppController::settings);

        // Assets (pictures / javascript)
        router.GET().route("/assets/webjars/{fileName: .*}").with(AssetsController::serveWebJars);
        router.GET().route("/assets/{fileName: .*}").with(AssetsController::serveStatic);

        // Index / Catchall shows index page
        router.GET().route("/.*").with(AppController::index);
    }

}
