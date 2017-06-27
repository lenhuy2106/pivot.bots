package models;

/**
 * Learning POJO.
 * Is parsed from the http post request.
 * @author Nhu Huy Le <mail@huy-le.de>
 */
public class Learning {

    /**
     * Text to learn/extract.
     */
    private String corpus;

    /**
     * Category to learn with.
     */
    private String category;

    public Learning() {
    }

    public String getCorpus() {
        return corpus;
    }

    public void setCorpus(String corpus) {
        this.corpus = corpus;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

}
