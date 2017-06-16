
package models;

/**
 *
 * @author T500
 */
public class Training {
    
    private String corpus;
    
    private Category category;

    public Training() {
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
        return "Training{" + "corpus=" + corpus.substring(0, 50) + "..., category=" + category + '}';
    }
    
}
