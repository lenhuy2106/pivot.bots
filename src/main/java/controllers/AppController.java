package controllers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import controllers.services.DatabaseService;
import controllers.services.LearningService;
import controllers.services.LearningService.LearningResult;
import database.Constants;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import models.Learning;
import models.Message;
import ninja.Context;
import ninja.Renderable;
import ninja.Result;
import ninja.Results;
import ninja.params.Param;
import ninja.uploads.DiskFileItemProvider;
import ninja.uploads.FileItem;
import ninja.uploads.FileProvider;
import ninja.utils.ResponseStreams;
import org.eclipse.jetty.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Application Controller.
 * Contains the logic of the application.
 * @author Nhu Huy Le <mail@huy-le.de>
 * @see <a href="http://www.ninjaframework.org/documentation/basic_concepts/controllers.html"></a>
 */
@FileProvider(DiskFileItemProvider.class)
@Singleton
public class AppController {

    private static final Logger LOG = LoggerFactory.getLogger(AppController.class);

    /**
     * The service providing the learning.
     */
    private final LearningService learningService;

    /**
     * the service providing the database.
     */
    private final DatabaseService dbService;

    private final Session session;

    /**
     * Ctor.
     * @param session
     * @throws IOException If database is not accessable.
     */
    @Inject
    public AppController(Session session) throws IOException {
        this.session = session;
        this.learningService = session.getLearningService();
        this.dbService       = session.getDbService();
        session.init();
    }

    /**
     * Index GET.
     * @return website.
     */
    public Result index() {
        return Results.html()
                .render("session", session);
    }

    /**
     * Learning GET.
     * @return website.
     */
    public Result learning() {
        return Results.html()
                .render("session", session);
    }

    /**
     * Learning execution POST.
     * @param context Injected session context.
     * @param learning Injected/parsed learning object.
     * @return website.
     * @throws java.io.IOException If database is not accessable.
     */
    public Result learningStart(Context context, Learning learning) throws IOException {
        LOG.info("learning with: {}", learning);
        session.getCategories().add(learning.getCategory());
        dbService.saveCategories(session.getCategories());
        session.init();

        LearningResult learningResult = learningService.extract(learning);
        LOG.info("questions extracted: " + session.getQuestions().toString());

        dbService.saveLearning(learning, learningResult);

        session.updateVocab();

        return Results.html()
                .render("session", session)
                .render(learning);
    }

    /**
     * Dialogue GET/POST.
     * @param context Injected session context.
     * @param messageOpt Optional message answer.
     * @param skip If question wasn't understood.
     * @return website.
     * @throws java.io.IOException If database is not accessable.
     */
    public Result dialogue(
            Context context,
            Optional<Message> messageOpt,
            @Param("skip") Optional<Boolean> skip) throws IOException {

        String text = messageOpt.isPresent()
                ? messageOpt.get().getText()
                : "";

        // there must be a user message
        if (StringUtil.isNotBlank(text)) {
            session.processMessage(text, skip);
        }

        return Results.html()
                .render("session", session);

    }
    /**
     * Analysis GET.
     * @return website.
     * @throws java.io.IOException If database is not accessable.
     */
    public Result analysis() throws IOException {
        Map<String, Integer> counter = session.getCategories().stream()
                .collect(Collectors.toMap(cat -> cat,
                        cat -> {
                            try {
                                return dbService.loadCount(cat);
                            }
                            catch (IOException ex) {
                                LOG.error("database access failed.", ex);
                                return 0;
                            }
                        }));
        long botPotential =
                session.getWords().values().stream()
                        .flatMap(s -> s.stream())
                        .count()
                + session.getQuestions().values().stream()
                        .flatMap(s -> s.stream())
                        .count();
        long usedPotential =
                session.getWordsUsed().size()
                + session.getQuestionsAnswered().values().stream()
                        .flatMap(s -> s.stream())
                        .count();

        // arithmetic adjustments
        AtomicInteger sum = new AtomicInteger();
        counter.forEach((cat, count) -> {
            counter.put(cat, count < 0
                    ? 0
                    : count);
            if (count > 0) {
                sum.addAndGet(count);
            }
        });
        // no div by zero
        botPotential = botPotential == 0
                ? 1
                : botPotential;
        sum.set(sum.get() < 1
                ? 1
                : sum.get());
        usedPotential = usedPotential > botPotential
                ? botPotential
                : usedPotential;

        return Results.html()
                .render("session", session)
                .render("sum", sum.get())
                .render("counter", counter)
                .render("botPotential", botPotential)
                .render("usedPotential", usedPotential);
    }

    /**
     * Settings GET.
     * @param ctx Session context.
     * @param upUser Optional user to import.
     * @param upBot Optional bot to import.
     * @return website.
     * @throws IOException If database is not accessable.
     */
    public Result settings(
            Context ctx,
            @Param("upUser") Optional<FileItem> upUser,
            @Param("upBot") Optional<FileItem> upBot) throws IOException {

        String changeUser = ctx.getParameter("currentUser");
        String changeBot  = ctx.getParameter("currentBot");

        session.updateWith(upUser, upBot, changeUser, changeBot);

        return Results.html()
                .render("datapath", Constants.DATA_PATH)
                .render("session", session);
    }

    /**
     * Serves exportable database files from the data directory.
     *
     * @return
     */
    public Result serveData() {
        Renderable renderable = (Context context, Result result) -> {
            String requestPath = context.getRequestPath();
            LOG.info("Requesting {}", requestPath);
            // remove trailing slash
            File file = new File(requestPath.substring(1, requestPath.length()));
            if (file.exists()) {
                // downloadable headers
                result.status(200);
                result.contentType("text/plain");
                result.addHeader("Content-Disposition", "attachment");
                ResponseStreams responseStreams = context.finalizeHeadersWithoutFlashAndSessionCookie(result);
                try (OutputStream os = responseStreams.getOutputStream()) {
                    Files.copy(file.toPath(), os);
                    os.flush();
                }
                catch (IOException ex) {
                    LOG.error("data directory access failed.", ex);
                    result.status(404);
                }
            }
        };
        return Results.ok().render(renderable);
    }

}
