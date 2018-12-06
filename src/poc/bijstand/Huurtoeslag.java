package poc.bijstand;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnDecisionRequirementsGraph;
import org.camunda.bpm.dmn.engine.DmnDecisionResult;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Path("/huurtoeslag")
public class Huurtoeslag {

	private static Map<String, Object> results = new HashMap<String, Object>();

	protected static VariableMap prepareVariableMap(JSONObject jsObject) {

		String huishouden = jsObject.getString("huishouden");
		int vermogen = jsObject.getInt("vermogen");
		int leeftijd = jsObject.getInt("leeftijd");

		int aantalpersonen = jsObject.getInt("aantalpersonen");
		double rekeninkomen = jsObject.getDouble("rekeninkomen");
		double rekenhuur = jsObject.getDouble("rekenhuur");
		double kwaliteitskorting = jsObject.getDouble("kwaliteitskorting");
		boolean kind = jsObject.getBoolean("kind");
		boolean handicap = jsObject.getBoolean("handicap");
		// prepare variables for decision evaluation
		// prepare variables for decision evaluation
		VariableMap variables = Variables.putValue("huishouden", huishouden).putValue("vermogen", vermogen)
				.putValue("leeftijd", leeftijd).putValue("aantalpersonen", aantalpersonen)
				.putValue("rekeninkomen", rekeninkomen).putValue("rekenhuur", rekenhuur)
				.putValue("kwaliteitskorting", kwaliteitskorting).putValue("kind", kind).putValue("handicap", handicap);

		return variables;
	}	
	
	protected static void parseAndEvaluateDecision(VariableMap variables, InputStream inputStream) {

		// create a new default DMN engine
		DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();

		// parse the drg from an input stream
		DmnDecisionRequirementsGraph drg = dmnEngine.parseDecisionRequirementsGraph(inputStream);

		// get the keys of all containing decisions
		Set<String> decisionKeys = drg.getDecisionKeys();
		System.out.println(decisionKeys.toString());
		
		// get a containing decision by key
		DmnDecision decision = drg.getDecision("huurtoeslag");
		// get a containing decision by key
		DmnDecision decision1 = drg.getDecision("aftopping");
		DmnDecision decision2 = drg.getDecision("basishuur");
		DmnDecision decision3 = drg.getDecision("huurgrens");
		DmnDecision decision4 = drg.getDecision("huurtoeslagA");
		DmnDecision decision5 = drg.getDecision("huurtoeslagB");
		DmnDecision decision6 = drg.getDecision("huurtoeslagC");

		// evaluate decision
		DmnDecisionResult result = dmnEngine.evaluateDecision(decision, variables);
		DmnDecisionResult result1 = dmnEngine.evaluateDecision(decision1, variables);
		DmnDecisionResult result2 = dmnEngine.evaluateDecision(decision2, variables);
		DmnDecisionResult result3 = dmnEngine.evaluateDecision(decision3, variables);
		DmnDecisionResult result4 = dmnEngine.evaluateDecision(decision4, variables);
		DmnDecisionResult result5 = dmnEngine.evaluateDecision(decision5, variables);
		DmnDecisionResult result6 = dmnEngine.evaluateDecision(decision6, variables);
		
		results.put("aftopping", result1.collectEntries("aftopping").toArray()[0]);
		results.put("basishuur", result2.collectEntries("basishuur").toArray()[0]);
		results.put("huurgrens", result3.collectEntries("huurgrens").toArray()[0]);
		results.put("huurtoeslagA", result4.collectEntries("huurtoeslagA").toArray()[0]);
		results.put("huurtoeslagB", result5.collectEntries("huurtoeslagB").toArray()[0]);
		results.put("huurtoeslagC", result6.collectEntries("huurtoeslagC").toArray()[0]);
		results.put("huurtoeslag", result.collectEntries("huurtoeslag").toArray()[0]);
		
	}
	
	@POST
	@Consumes("application/json")
	@Produces("application/json")
	public Response getJSON(InputStream incomingData) {
		StringBuilder crunchifyBuilder = new StringBuilder();
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(incomingData));
			String line = null;
			while ((line = in.readLine()) != null) {
				crunchifyBuilder.append(line);
			}
		} catch (Exception e) {
			System.out.println("Error Parsing: - ");
		}
		JSONObject jsObject = (JSONObject) new JSONTokener(crunchifyBuilder.toString()).nextValue();

		VariableMap variables = prepareVariableMap(jsObject);

		// parse decision from resource input stream
		InputStream inputStream = Huurtoeslag.class.getResourceAsStream("huurtoeslag.dmn");

		try {
			parseAndEvaluateDecision(variables, inputStream);

		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
			}
		}		

		GsonBuilder gsonMapBuilder = new GsonBuilder();		 
		Gson gsonObject = gsonMapBuilder.create();
 
		String result = gsonObject.toJson(results);
		return Response.status(200).entity(result).build();
	}

}
