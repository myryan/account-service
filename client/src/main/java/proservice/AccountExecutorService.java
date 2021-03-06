package proservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Mirian Dzhachvadze
 */
public class AccountExecutorService {

    private final static Logger LOGGER = LoggerFactory.getLogger(AccountExecutorService.class);

    private final HttpClientWrapper httpClientWrapper;
    private final int rCount;
    private final int wCount;
    private final List<Integer> idList;
    private final Random rand;
    private final ExecutorService executor;
    private CompletionService<Long> completionService;

    /**
     * @param httpClientWrapper HttpClient wrapper
     * @param rCount readers count
     * @param wCount writers count
     * @param idList id list
     * @param activeThreads simultaneously worked thread
     */
    public AccountExecutorService(HttpClientWrapper httpClientWrapper, int rCount, int wCount,
            List<Integer> idList, int activeThreads) {
        this.httpClientWrapper = httpClientWrapper;
        this.rCount = rCount;
        this.wCount = wCount;
        this.idList = idList;
        this.rand = new Random();
        if (activeThreads == -1) {
            activeThreads = Integer.MAX_VALUE;
        }

        this.executor = Executors.newFixedThreadPool(activeThreads);
        this.completionService = new ExecutorCompletionService<Long>(executor);
    }

    public void start() {
        startWriters();
        startReaders();

        for (int i = 0; i < rCount + wCount; i++) {
            try {
                completionService.take();
            } catch (InterruptedException e) {
                LOGGER.error("AccountExecutorService.start - Failed to get thread result");
                Thread.currentThread().interrupt();
            }
        }

        executor.shutdown();
    }

    private void startReaders() {
        for (int i = 0; i < rCount; i++) {
            completionService.submit(new Reader(httpClientWrapper, getRandomId()));
        }
    }

    private void startWriters() {
        for (int i = 0; i < wCount; i++) {
            Integer id = getRandomId();
            Long value = (long)rand.nextInt(20);

            Client.registerBalanceValue(id, value);

            completionService.submit(new Writer(httpClientWrapper, id, value));
        }
    }

    public Integer getRandomId() {
        if (!idList.isEmpty() && idList.size() == 1) {
            return idList.get(0);
        }

        return idList.get(rand.nextInt(idList.size() - 1));
    }

    public void shutDown() {
        executor.shutdown();
    }
}
