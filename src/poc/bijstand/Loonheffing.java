package poc.bijstand;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
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

@Path("/loonheffing")
public class Loonheffing {

	private  Map<String, Object> results = new HashMap<String, Object>();

	public  Map<String, Object> getResults() {
		return results;
	}

	protected VariableMap prepareVariableMap(JSONObject jsObject) {

		double brutoloon = jsObject.getDouble("brutoloon");
		double brutocomponenten = jsObject.getDouble("brutocomponenten");
		double WNpremie = jsObject.getDouble("WNpremie");

		VariableMap variables = Variables.putValue("brutoloon", brutoloon)
				.putValue("brutocomponenten", brutocomponenten).putValue("WNpremie", WNpremie);

		return variables;
	}

	protected void parseAndEvaluateDecision(VariableMap variables, InputStream inputStream) {

		// create a new default DMN engine
		DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();

		// parse the drg from an input stream
		DmnDecisionRequirementsGraph drg = dmnEngine.parseDecisionRequirementsGraph(inputStream);

		// get the keys of all containing decisions
		Set<String> decisionKeys = drg.getDecisionKeys();
		System.out.println(decisionKeys.toString());

		// get a containing decision by key
		DmnDecision decision = drg.getDecision("heffingsloon");
		DmnDecision decision1 = drg.getDecision("belastingschijf");
		DmnDecision decision2 = drg.getDecision("ahk");
		DmnDecision decision3 = drg.getDecision("arbeidskorting");
		DmnDecision decision4 = drg.getDecision("loonheffing");
		
		// evaluate decision
		DmnDecisionResult result = dmnEngine.evaluateDecision(decision, variables);
		DmnDecisionResult result1 = dmnEngine.evaluateDecision(decision1, variables);
		DmnDecisionResult result2 = dmnEngine.evaluateDecision(decision2, variables);
		DmnDecisionResult result3 = dmnEngine.evaluateDecision(decision3, variables);
		DmnDecisionResult result4 = dmnEngine.evaluateDecision(decision4, variables);

		results.put("heffingsloon", result.collectEntries("heffingsloon").toArray()[0]);
		results.put("belastingschijf", result1.collectEntries("belastingschijf").toArray()[0]);
		results.put("belastingpercentage", result1.collectEntries("belastingpercentage").toArray()[0]);
		results.put("algemene heffingskorting", result2.collectEntries("ahk").toArray()[0]);
		results.put("arbeidskorting", result3.collectEntries("arbeidskorting").toArray()[0]);
		results.put("loonheffing", result4.collectEntries("loonheffing").toArray()[0]);
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
		InputStream inputStream = Loonheffing.class.getResourceAsStream("loonheffing.dmn");

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
