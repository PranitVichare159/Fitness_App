package com.fitness.aiservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendations;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class ActivityAiService {
    private  final GeminiService  geminiService;

    public Recommendations genrateRecommendation(Activity activity){
        String prompt = createPromptForActivity(activity);
        String aiResponse=geminiService.getRecommendations(prompt);
        log.info("Response from ai {}",aiResponse);
        return processAiResponse(activity,aiResponse);
    }

    private Recommendations processAiResponse(Activity activity, String aiResponse) {
        try{
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(aiResponse);
            JsonNode textNode = rootNode.get("candidates")
                    .get(0)
                    .path("content")
                    .get("parts")
                    .get(0)
                    . path("text");

            String jsonContent = textNode.asText()
                    .replaceAll("```json\\n","")
                    .replaceAll("\\n```","")
                    .trim();

            JsonNode analysisJson = mapper.readTree(jsonContent);
            JsonNode analysisNode =analysisJson.path("analysis");

            StringBuilder fullAnalysis = new StringBuilder();
            addAnalysisSection(fullAnalysis,analysisNode,"overall","Overall:");
            addAnalysisSection(fullAnalysis,analysisNode,"pace","Pace:");
            addAnalysisSection(fullAnalysis,analysisNode,"heartRate","Heart  Rate:");
            addAnalysisSection(fullAnalysis,analysisNode,"caloriesBurned","Calories:");

            List<String> improvements = extractImprovements(analysisJson.path("improvements"));
            List<String> suggestions = extractSuggestions(analysisJson.path("suggestions"));
            List<String> safety = extractSafetyGuidelines(analysisJson.path("safety"));

            return Recommendations.builder()
                    .activityId(activity.getId())
                    .userId(activity.getUserId())
                    .type(activity.getType().toString())
                    .recommendation(fullAnalysis.toString().trim())
                    .improvements(improvements)
                    .suggestions(suggestions)
                    .safety(safety)
                    .createdDate(LocalDateTime.now())
                    .build();

        }catch(Exception e){
            e.printStackTrace();
            return createDefaultRecommendation(activity);
        }

    }

    private Recommendations createDefaultRecommendation(Activity activity) {
        return Recommendations.builder()
                .activityId(activity.getId())
                .userId(activity.getUserId())
                .type(activity.getType().toString())
                .recommendation("Unable to generate Detailed analysis")
                .improvements(Collections.singletonList("Contionue with your current routine"))
                .suggestions(Collections.singletonList("Consider consulting a fitness consultant "))
                .safety(Arrays.asList(
                        "Always warm up before excercise",
                        "Stay Hydrated",
                        "Listen to your Body"
                ))
                .createdDate(LocalDateTime.now())
                .build();
    }

    private List<String> extractSafetyGuidelines(JsonNode safetyNode) {
        List<String> safety = new ArrayList<>();
        if(safetyNode.isArray()){
            safetyNode.forEach(item ->safety.add(item.asText()));
        }
        return safety.isEmpty() ? Collections.singletonList("Follow general Safety Guidelines") : safety;

    }

    private List<String> extractSuggestions(JsonNode suggestionsNode) {
        List<String> suggestions = new ArrayList<>();
        if(suggestionsNode.isArray()){
            suggestionsNode.forEach(suggestion -> {
                String workout = suggestion.path("workout").asText();
                String description = suggestion.path("description").asText();
                suggestions.add(String.format("%s: %s", workout, description));

            });
        }
        return suggestions.isEmpty() ? Collections.singletonList("No Specific suggestions Provided") :
                suggestions;
    }

    private List<String> extractImprovements(JsonNode improvementsNode) {
        List<String> improvements = new ArrayList<>();
        if(improvementsNode.isArray()){
            improvementsNode.forEach(improvement -> {
                String area = improvement.path("area").asText();
                String details = improvement.path("recommendation").asText();
                improvements.add(String.format("%s: %s", area, details));

            });
        }
        return improvements.isEmpty() ? Collections.singletonList("No Specific improvement Provided") :
                improvements;

    }

    private void addAnalysisSection(StringBuilder fullAnalysis, JsonNode analysisNode, String key, String prefix) {
        if(!analysisNode.path(key).isMissingNode()){
            fullAnalysis.append(prefix)
                    .append(analysisNode.path(key).asText())
                    .append("\n\n");
        }
    }


    private String createPromptForActivity(Activity activity) {
        return  String.format("""
                Analyze this fitness activity and provide detailed recommendations inthe following EXACT JSON format:
                { 
                    "analysis": {
                        "overall": "Overall analysis here", 
                        "pace": "Pace analysis here", 
                        "heartRate": "Heart rate analysis here", 
                        "caloriesBurned": "Calories analysis here" 
                    },
                    "improvements": [ 
                      { 
                        "area": "Area name",
                        "recommendation": "Detailed recommendation" 
                      }
                    ],
                    "suggestions": [
                      {
                        "workout": "Workout name",
                        "description": "Detailed workout description"
                      }
                    ],
                    "safety": [
                       "Safety point 1",
                       "Safety point 2"
                    ]
                }
                Analyze this activity:
                Activity Type: %s
                Duration: %d minutes
                Calories Burned:%d    
                Additional Metrics: %s
                
                Provide detailed analysis focusing on performance, improvements, next workout suggestions, and safety guidelines.
                Ensure the response follows the EXACT JSON format shown above.
                 """,

                        activity.getType(),
                        activity.getDuration(),
                        activity.getCaloriesBurned(),
                        activity.getAdditionalMetrics()


                );
    }
}
