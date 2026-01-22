import java.util.HashMap;
import java.util.Random;

public class LanguageModel {

    // The map of this model.
    // Maps windows to lists of charachter data objects.
    HashMap<String, List> CharDataMap;
    
    // The window length used in this model.
    int windowLength;
    
    // The random number generator used by this model. 
    private Random randomGenerator;

    /** Constructs a language model with the given window length and a given
     * seed value. Generating texts from this model multiple times with the 
     * same seed value will produce the same random texts. Good for debugging. */
    public LanguageModel(int windowLength, int seed) {
        this.windowLength = windowLength;
        randomGenerator = new Random(seed);
        CharDataMap = new HashMap<String, List>();
    }

    /** Constructs a language model with the given window length.
     * Generating texts from this model multiple times will produce
     * different random texts. Good for production. */
    public LanguageModel(int windowLength) {
        this.windowLength = windowLength;
        randomGenerator = new Random();
        CharDataMap = new HashMap<String, List>();
    }

    /** Builds a language model from the text in the given file (the corpus). */
    public void train(String fileName) {
        String window = "";
        char c;
        In in = new In(fileName);
        
        // Reads the first window
        for (int i = 0; i < windowLength; i++) {
            window += in.readChar();
        }
        
        // Processes the rest of the text
        while (!in.isEmpty()) {
            c = in.readChar();
            List probs = CharDataMap.get(window);
            if (probs == null) {
                probs = new List();
                CharDataMap.put(window, probs);
            }
            probs.update(c);
            
            // Advances the window
            window = window.substring(1) + c;
        }
        
        // Computes probabilities for all lists
        for (List probs : CharDataMap.values()) {
            calculateProbabilities(probs);
        }
    }

    // Computes and sets the probabilities (p and cp fields) of all the
    // characters in the given list. */
    public void calculateProbabilities(List probs) {               
        int totalCount = 0;
        ListIterator itr = probs.listIterator(0);
        while (itr.hasNext()) {
            CharData current = itr.next();
            totalCount += current.count;
        }
        
        double cumulativeProb = 0.0;
        itr = probs.listIterator(0);
        while (itr.hasNext()) {
            CharData current = itr.next();
            current.p = (double) current.count / totalCount;
            cumulativeProb += current.p;
            current.cp = cumulativeProb;
        }
    }

    // Returns a random character from the given probabilities list.
    public char getRandomChar(List probs) {
        double r = randomGenerator.nextDouble();
        ListIterator itr = probs.listIterator(0);
        while (itr.hasNext()) {
            CharData current = itr.next();
            if (current.cp > r) {
                return current.chr;
            }
        }
        // Fallback (should not happen usually)
        return probs.get(probs.getSize() - 1).chr;
    }

    /**
     * Generates a random text, based on the probabilities that were learned during training. 
     * @param initialText - text to start with. If initialText's last substring of size numberOfLetters
     * doesn't appear as a key in Map, we generate no text and return only the initial text. 
     * @param textLength - the size of text to generate
     * @return the generated text
     */
    public String generate(String initialText, int textLength) {
        // Validation: If initial text is shorter than window, return it as is.
        if (initialText.length() < windowLength) {
            return initialText;
        }
        
        String generatedText = initialText;
        String window = generatedText.substring(generatedText.length() - windowLength);
        
        // Generate characters until the text reaches the requested length
        while (generatedText.length() < textLength) {
            List probs = CharDataMap.get(window);
            
            // If the current window is not found in the map, stop and return what we have
            if (probs == null) {
                return generatedText;
            }
            
            char nextChar = getRandomChar(probs);
            generatedText += nextChar;
            
            // Update window
            window = generatedText.substring(generatedText.length() - windowLength);
        }
        
        return generatedText;
    }

    /** Returns a string representing the map of this language model. */
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (String key : CharDataMap.keySet()) {
            List keyProbs = CharDataMap.get(key);
            str.append(key + " : " + keyProbs + "\n");
        }
        return str.toString();
    }

    public static void main(String[] args) {
        int windowLength = Integer.parseInt(args[0]);
        String initialText = args[1];
        int generatedTextLength = Integer.parseInt(args[2]);
        boolean randomGeneration = args[3].equals("random");
        String fileName = args[4];
        
        // Create the LanguageModel object
        LanguageModel lm;
        if (randomGeneration)
            lm = new LanguageModel(windowLength);
        else
            lm = new LanguageModel(windowLength, 20);
            
        // Trains the model, creating the map.
        lm.train(fileName);
        
        // Generates text, and prints it.
        System.out.println(lm.generate(initialText, generatedTextLength));
    }
} 