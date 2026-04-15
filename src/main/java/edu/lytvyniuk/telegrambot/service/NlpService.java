package edu.lytvyniuk.telegrambot.service;

import edu.lytvyniuk.telegrambot.DTO.NlpResult;
import edu.lytvyniuk.telegrambot.DTO.UserIntent;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class NlpService {
    private TokenizerME    tokenizer;
    private SentenceDetectorME sentenceDetector;
    private POSTaggerME    posTagger;
    private static final Map<UserIntent, List<String>> INTENT_KEYWORDS = Map.of(
            UserIntent.WEATHER,  List.of(
                    "weather", "temperature", "forecast", "rain", "snow", "cold", "warm", "humid",
                    "погода", "температура", "прогноз", "дощ", "сніг", "холодно", "тепло"
            ),
            UserIntent.CURRENCY, List.of(
                    "currency", "exchange", "rate", "dollar", "euro", "usd", "eur", "uah", "gbp",
                    "курс", "валюта", "долар", "євро", "гривня", "обмін"
            ),
            UserIntent.NEWS,     List.of(
                    "news", "headlines", "latest", "updates", "articles",
                    "новини", "новина", "останні", "що нового"
            ),
            UserIntent.HELP,     List.of(
                    "help", "commands", "what can you do",
                    "допомога", "команди", "що ти вмієш"
            )
    );

    private static final Pattern CURRENCY_PATTERN =
            Pattern.compile("\\b([A-Z]{3})\\b");
    private static final List<String> PROPER_NOUN_TAGS = List.of("NNP", "NNPS", "PROPN");
    private static final List<String> STOPWORDS = List.of(
            "i", "the", "a", "an", "is", "are", "what", "tell", "show",
            "give", "check", "get", "please", "hi", "hello", "hey",
            "me", "my", "your", "its", "in", "at", "for", "of", "on", "with",
            "weather", "news", "rate", "currency", "forecast", "temperature",
            "я", "ти", "він", "вона", "це", "є", "що", "скажи", "покажи",
            "дай", "будь", "ласка", "привіт", "мене", "мій", "твій", "його",
            "в", "у", "на", "для", "про", "погода", "новини", "курс", "валюта", "прогноз", "температура"
    );
    @PostConstruct
    public void init() {
        tokenizer        = loadTokenizer();
        sentenceDetector = loadSentenceDetector();
        posTagger        = loadPosTagger();
    }

    private TokenizerME loadTokenizer() {
        try (InputStream is = new ClassPathResource(
                "models/opennlp-en-ud-ewt-tokens-1.3-2.5.4.bin").getInputStream()) {
            TokenizerME t = new TokenizerME(new TokenizerModel(is));
            log.info("OpenNLP Tokenizer loaded");
            return t;
        } catch (Exception e) {
            log.error("Tokenizer load failed: {}", e.getMessage());
            return null;
        }
    }

    private SentenceDetectorME loadSentenceDetector() {
        try (InputStream is = new ClassPathResource(
                "models/opennlp-en-ud-ewt-sentence-1.3-2.5.4.bin").getInputStream()) {
            SentenceDetectorME sd = new SentenceDetectorME(new SentenceModel(is));
            log.info("OpenNLP Sentence Detector loaded");
            return sd;
        } catch (Exception e) {
            log.error("SentenceDetector load failed: {}", e.getMessage());
            return null;
        }
    }

    private POSTaggerME loadPosTagger() {
        try (InputStream is = new ClassPathResource(
                "models/opennlp-en-ud-ewt-pos-1.3-2.5.4.bin").getInputStream()) {
            POSTaggerME pos = new POSTaggerME(new POSModel(is));
            log.info("OpenNLP POS Tagger loaded");
            return pos;
        } catch (Exception e) {
            log.error("POS Tagger load failed: {}", e.getMessage());
            return null;
        }
    }
    public NlpResult analyze(String text) {
        String lower = text.toLowerCase();

        UserIntent intent      = detectIntent(lower);
        String     location    = extractLocation(text);
        String     currency    = extractCurrency(text);
        String     newsCategory = extractNewsCategory(lower);

        log.info("NLP result → intent={}, location='{}', currency={}, news={}",
                intent, location, currency, newsCategory);

        return NlpResult.builder()
                .intent(intent)
                .location(location)
                .currencyCode(currency)
                .newsCategory(newsCategory)
                .build();
    }

    private UserIntent detectIntent(String lower) {
        for (Map.Entry<UserIntent, List<String>> entry : INTENT_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lower.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }
        return UserIntent.UNKNOWN;
    }

    public String extractLocation(String text) {
        if (tokenizer != null && posTagger != null) {
            try {
                String[] tokens = tokenizer.tokenize(text);
                String[] tags   = posTagger.tag(tokens);

                List<String> properNouns = new ArrayList<>();
                for (int i = 0; i < tokens.length; i++) {
                    String cleanToken = tokens[i].replaceAll("[^a-zA-Zа-яА-ЯіїєґІЇЄҐ]", "");
                    if (PROPER_NOUN_TAGS.contains(tags[i])
                            && !STOPWORDS.contains(tokens[i].toLowerCase())
                            && !cleanToken.isEmpty()) {
                        properNouns.add(tokens[i]);
                    }
                }
                if (!properNouns.isEmpty()) {
                    String location = String.join(" ", properNouns);
                    log.debug("POS extracted proper nouns as location: '{}'", location);
                    return location;
                }
            } catch (Exception e) {
                log.warn("POS location extraction failed: {}", e.getMessage());
            }
        }

        return extractLocationFallback(text.toLowerCase(), text);
    }

    private String extractLocationFallback(String lower, String original) {
        List<String> patterns = List.of(
                "weather in", "weather for", "weather at", "weather",
                "forecast for", "forecast in", "forecast", "temperature in", "temperature",
                "погода в", "погода у", "погода для", "погода",
                "температура в", "температура у", "температура",
                "прогноз для", "прогноз в", "прогноз у", "прогноз"
        );
        for (String kw : patterns) {
            int idx = lower.indexOf(kw);
            if (idx >= 0) {
                // Check what's after the keyword
                String after = original.substring(idx + kw.length()).trim();
                after = after.replaceFirst("(?i)^(the|a|an|in|at|for|to|of|on|v|u|na)\\s+", "").trim();
                after = after.split("[,?!.]")[0].trim();
                
                if (!after.isEmpty() && !STOPWORDS.contains(after.toLowerCase())) {
                    return after;
                }

                // If nothing after, check if location was before (e.g. "Paris weather")
                if (idx > 0) {
                    String before = original.substring(0, idx).trim();
                    String[] parts = before.split("\\s+");
                    if (parts.length > 0) {
                        String potential = parts[parts.length - 1];
                        if (!STOPWORDS.contains(potential.toLowerCase())) {
                            return potential;
                        }
                    }
                }
            }
        }
        return "";
    }

    private String extractCurrency(String text) {
        Matcher m = CURRENCY_PATTERN.matcher(text);
        while (m.find()) {
            String code = m.group(1);
            if (List.of("USD", "EUR", "UAH", "GBP", "PLN",
                    "CZK", "CHF", "JPY", "CAD").contains(code)) {
                return code;
            }
        }
        String lower = text.toLowerCase();
        if (lower.contains("dollar") || lower.contains("долар"))   return "USD";
        if (lower.contains("euro")   || lower.contains("євро"))    return "EUR";
        if (lower.contains("hryvnia")|| lower.contains("гривня"))  return "UAH";
        if (lower.contains("pound")  || lower.contains("фунт"))    return "GBP";
        if (lower.contains("zloty")  || lower.contains("злотий"))  return "PLN";

        return "USD"; // default
    }
    private String extractNewsCategory(String lower) {
        if (lower.contains("tech")       || lower.contains("технолог")) return "technology";
        if (lower.contains("sport")      || lower.contains("спорт"))    return "sports";
        if (lower.contains("business")   || lower.contains("бізнес"))   return "business";
        if (lower.contains("science")    || lower.contains("наука"))    return "science";
        if (lower.contains("health")     || lower.contains("здоров"))   return "health";
        if (lower.contains("entertain")  || lower.contains("розваг"))   return "entertainment";
        return "general";
    }
}