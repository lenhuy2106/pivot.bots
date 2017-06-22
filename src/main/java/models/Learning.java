
package models;

/**
 *
 * @author T500
 */
public class Learning {
    
    private String corpus;
    
    private Category category;

    public Learning() {
    }

    public String getCorpus() {
        return corpus;
    }

    public void setCorpus(String corpus) {
        this.corpus = corpus;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    @Override
    public String toString() {
        return "Learning{" + "corpus=" + corpus.substring(0, 50) + "..., category=" + category + '}';
    }
    
}
